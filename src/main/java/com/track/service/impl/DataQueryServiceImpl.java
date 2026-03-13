package com.track.service.impl;

import com.track.entity.DatasourceConfig;
import com.track.service.DataQueryService;
import com.track.service.DatasourceConfigService;
import com.track.util.DataSourceUtil;
import org.springframework.stereotype.Service;
import redis.clients.jedis.Jedis;
import redis.clients.jedis.exceptions.JedisMovedDataException;

import javax.sql.DataSource;
import java.sql.*;
import java.util.*;
import java.util.regex.Pattern;

/**
 * 数据查询服务实现：仅支持 SELECT，禁止写操作
 */
@Service
public class DataQueryServiceImpl implements DataQueryService {

    private static final Pattern WRITE_PATTERN = Pattern.compile(
        "\\b(INSERT|UPDATE|DELETE|DROP|CREATE|ALTER|TRUNCATE|REPLACE)\\b",
        Pattern.CASE_INSENSITIVE
    );
    private static final int MAX_ROWS = 1000;

    private final DatasourceConfigService datasourceConfigService;

    public DataQueryServiceImpl(DatasourceConfigService datasourceConfigService) {
        this.datasourceConfigService = datasourceConfigService;
    }

    @Override
    public Map<String, Object> executeQuery(Long datasourceId, String schema, String sql, int page, int size) {
        if (WRITE_PATTERN.matcher(sql).find()) {
            throw new IllegalArgumentException("仅支持 SELECT 查询，禁止写操作");
        }
        if (!sql.trim().toUpperCase().startsWith("SELECT")) {
            throw new IllegalArgumentException("仅支持 SELECT 语句");
        }

        DatasourceConfig config = datasourceConfigService.getById(datasourceId);
        if (config == null) throw new IllegalArgumentException("数据源不存在");
        String type = DataSourceUtil.normalizeDbType(config.getType());
        if ("REDIS".equals(type)) {
            throw new IllegalArgumentException("Redis 请使用专用查询接口");
        }

        DataSource ds = null;
        Connection conn = null;
        try {
            ds = DataSourceUtil.createDataSource(config);
            conn = ds.getConnection();

            if (("ORACLE".equals(type) || "OCEANBASE_ORACLE".equals(type)) && schema != null && !schema.isEmpty()) {
                try (Statement st = conn.createStatement()) {
                    st.execute("ALTER SESSION SET CURRENT_SCHEMA = " + schema);
                }
            } else if (("MYSQL".equals(type) || "OCEANBASE".equals(type)) && schema != null && !schema.isEmpty()) {
                conn.setCatalog(schema);
            }

            int offset = (page - 1) * size;
            int limit = Math.min(size, MAX_ROWS);
            String pageSql = addLimit(sql, limit, offset, type);

            List<Map<String, Object>> rows = new ArrayList<>();
            List<String> columns = new ArrayList<>();
            int total = 0;

            try (PreparedStatement ps = conn.prepareStatement(pageSql);
                 ResultSet rs = ps.executeQuery()) {
                ResultSetMetaData meta = rs.getMetaData();
                int colCount = meta.getColumnCount();
                for (int i = 1; i <= colCount; i++) {
                    columns.add(meta.getColumnLabel(i));
                }
                while (rs.next() && rows.size() < limit) {
                    Map<String, Object> row = new LinkedHashMap<>();
                    for (int i = 1; i <= colCount; i++) {
                        Object val = rs.getObject(i);
                        row.put(meta.getColumnLabel(i), val);
                    }
                    rows.add(row);
                }
            }

            // 简单 count（仅对简单查询有效）
            try {
                String countSql = "SELECT COUNT(*) FROM (" + sql + ") _cnt";
                try (PreparedStatement ps = conn.prepareStatement(countSql);
                     ResultSet rs = ps.executeQuery()) {
                    if (rs.next()) total = rs.getInt(1);
                }
            } catch (Exception e) {
                total = rows.size();
            }

            Map<String, Object> r = new HashMap<String, Object>();
            r.put("columns", columns);
            r.put("rows", rows);
            r.put("total", total);
            r.put("page", page);
            r.put("size", size);
            return r;
        } catch (SQLException e) {
            throw new RuntimeException("查询执行失败: " + e.getMessage(), e);
        } finally {
            if (conn != null) {
                try {
                    conn.close();
                } catch (SQLException ignored) {
                }
            }
            DataSourceUtil.closeDataSource(ds);
        }
    }

    private String addLimit(String sql, int limit, int offset, String type) {
        sql = sql.trim();
        // Oracle/OceanBase Oracle 不支持 LIMIT，必须先去掉用户 SQL 末尾的 LIMIT/OFFSET，再套 ROWNUM 分页
        if ("ORACLE".equals(type) || "OCEANBASE_ORACLE".equals(type)) {
            sql = sql.replaceAll("(?i)\\s+LIMIT\\s+\\d+(\\s+OFFSET\\s+\\d+)?\\s*$", "");
            return "SELECT * FROM (SELECT ROWNUM rn, t.* FROM (" + sql + ") t WHERE ROWNUM <= " + (offset + limit) + ") WHERE rn > " + offset;
        }
        // 去掉用户 SQL 末尾已有的 LIMIT/OFFSET，避免出现 "LIMIT 100 LIMIT 20 OFFSET 0" 语法错误
        sql = sql.replaceAll("(?i)\\s+LIMIT\\s+\\d+(\\s+OFFSET\\s+\\d+)?\\s*$", "");
        return sql + " LIMIT " + limit + " OFFSET " + offset;
    }

    @Override
    public Map<String, Object> queryTableData(Long datasourceId, String schema, String tableName, String whereClause, int page, int size) {
        String safeTable = tableName.replaceAll("[^a-zA-Z0-9_]", "");
        String sql = "SELECT * FROM " + safeTable;
        if (whereClause != null && !whereClause.trim().isEmpty()) {
            sql += " WHERE " + whereClause;
        }
        return executeQuery(datasourceId, schema, sql, page, size);
    }

    @Override
    public Object getRedisValue(Long datasourceId, String key) {
        DatasourceConfig config = datasourceConfigService.getById(datasourceId);
        if (config == null) {
            throw new IllegalArgumentException("数据源不存在");
        }
        return executeWithRedirect(config, jedis -> {
            String type = jedis.type(key);
            if ("string".equals(type)) {
                return jedis.get(key);
            }
            if ("hash".equals(type)) {
                return jedis.hgetAll(key);
            }
            if ("list".equals(type)) {
                return jedis.lrange(key, 0, -1);
            }
            if ("set".equals(type)) {
                return jedis.smembers(key);
            }
            if ("zset".equals(type)) {
                return jedis.zrangeWithScores(key, 0, -1);
            }
            return null;
        });
    }

    @FunctionalInterface
    private interface RedisCallback<T> {
        T doInRedis(Jedis jedis);
    }

    /**
     * 执行 Redis 操作，自动跟随 MOVED 重定向（适配 Redis Cluster）。
     * 允许最多 5 次重定向，以避免错误配置导致死循环。
     */
    private <T> T executeWithRedirect(DatasourceConfig config, RedisCallback<T> callback) {
        String host = config.getHost();
        int port = config.getPort() != null ? config.getPort() : 6379;
        String password = config.getPassword();
        int maxRedirects = 5;

        for (int i = 0; i < maxRedirects; i++) {
            try (Jedis jedis = new Jedis(host, port)) {
                if (password != null && !password.isEmpty()) {
                    jedis.auth(password);
                }
                return callback.doInRedis(jedis);
            } catch (JedisMovedDataException moved) {
                host = moved.getTargetNode().getHost();
                port = moved.getTargetNode().getPort();
            }
        }
        throw new RuntimeException("Redis MOVED 重定向次数过多，请检查集群配置");
    }
}
