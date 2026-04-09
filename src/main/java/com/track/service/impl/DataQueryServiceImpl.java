package com.track.service.impl;

import com.track.common.ExceptionDetailUtil;
import com.track.entity.DatasourceConfig;
import com.track.service.DataQueryService;
import com.track.service.DatasourceConfigService;
import com.track.util.DataSourceUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
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

    private static final Logger log = LoggerFactory.getLogger(DataQueryServiceImpl.class);
    private static final Pattern WRITE_PATTERN = Pattern.compile(
        "\\b(INSERT|UPDATE|DELETE|DROP|CREATE|ALTER|TRUNCATE|REPLACE)\\b",
        Pattern.CASE_INSENSITIVE
    );
    private static final Pattern DANGEROUS_STAT_PATTERN = Pattern.compile(
        "(--|/\\*|\\*/|;)",
        Pattern.CASE_INSENSITIVE
    );
    private static final Pattern IDENTIFIER_PATTERN = Pattern.compile("^[A-Za-z][A-Za-z0-9_]*$");
    private static final int MAX_ROWS = 1000;

    private final DatasourceConfigService datasourceConfigService;

    public DataQueryServiceImpl(DatasourceConfigService datasourceConfigService) {
        this.datasourceConfigService = datasourceConfigService;
    }

    @Override
    public Map<String, Object> executeQuery(Long datasourceId, String schema, String stat, int page, int size) {
        if (stat == null || stat.trim().isEmpty()) {
            throw new IllegalArgumentException("查询语句不能为空");
        }
        stat = stat.replaceAll(";","");
        if (page < 1) {
            throw new IllegalArgumentException("page 必须大于等于 1");
        }
        if (size < 1) {
            throw new IllegalArgumentException("size 必须大于等于 1");
        }
        String trimmedStat = stat.trim();
        if (DANGEROUS_STAT_PATTERN.matcher(trimmedStat).find()) {
            throw new IllegalArgumentException("查询语句包含不安全内容，仅支持单条纯 SELECT 查询");
        }
        if (WRITE_PATTERN.matcher(stat).find()) {
            throw new IllegalArgumentException("仅支持 SELECT 查询，禁止写操作");
        }
        if (!trimmedStat.toUpperCase(Locale.ROOT).startsWith("SELECT")) {
            throw new IllegalArgumentException("仅支持 SELECT 语句");
        }

        DatasourceConfig config = datasourceConfigService.getById(datasourceId);
        if (config == null) throw new IllegalArgumentException("数据源不存在");
        String type = DataSourceUtil.normalizeDbType(config.getType());
        String validatedSchema = validateSchema(schema, config, type);
        if ("REDIS".equals(type)) {
            throw new IllegalArgumentException("Redis 请使用专用查询接口");
        }

        DataSource ds = null;
        Connection conn = null;
        try {
            ds = DataSourceUtil.createDataSource(config);
            conn = ds.getConnection();

            if (("ORACLE".equals(type) || "OCEANBASE_ORACLE".equals(type)) && validatedSchema != null) {
                conn.setSchema(validatedSchema);
            } else if (("MYSQL".equals(type) || "OCEANBASE".equals(type)) && validatedSchema != null) {
                conn.setCatalog(validatedSchema);
            }

            int offset = (page - 1) * size;
            int limit = Math.min(size, MAX_ROWS);
            String baseStat = stripTrailingLimitClause(trimmedStat, type);
            String countStat = buildCountQuery(baseStat, type);
            long total = 0L;
            try (PreparedStatement countPs = conn.prepareStatement(countStat);
                 ResultSet countRs = countPs.executeQuery()) {
                if (countRs.next()) {
                    total = countRs.getLong(1);
                }
            }

            String pageStat = addLimit(trimmedStat, limit, offset, type);

            List<Map<String, Object>> rows = new ArrayList<>();
            List<String> columns = new ArrayList<>();

            try (PreparedStatement ps = conn.prepareStatement(pageStat);
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

            Map<String, Object> r = new HashMap<String, Object>();
            r.put("columns", columns);
            r.put("rows", rows);
            r.put("total", total);
            r.put("page", page);
            r.put("size", size);
            return r;
        } catch (SQLException e) {
            log.error("查询执行失败 datasourceId={}, type={}, schema={}", datasourceId, type, schema, e);
            throw new IllegalArgumentException(ExceptionDetailUtil.formatSqlException(e));
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

    /**
     * 与 {@link #addLimit} 一致：去掉用户语句末尾的 LIMIT/OFFSET，便于 COUNT 与分页基于同一套 SELECT。
     */
    private String stripTrailingLimitClause(String stat, String type) {
        stat = stat.trim();
        stat = stat.replaceAll("(?i)\\s+LIMIT\\s+\\d+(\\s+OFFSET\\s+\\d+)?\\s*$", "");
        return stat;
    }

    /**
     * 将 SELECT 包在 COUNT 子查询中，得到符合当前过滤条件的总行数（用于分页 total）。
     * 内层 SQL 已通过上层校验，仅由服务端拼接固定包装，不拼接外部标识符。
     */
    private String buildCountQuery(String innerSelect, String type) {
        if ("ORACLE".equals(type) || "OCEANBASE_ORACLE".equals(type)) {
            return "SELECT COUNT(*) FROM (" + innerSelect + ") track_cnt_sq";
        }
        return "SELECT COUNT(*) FROM (" + innerSelect + ") AS track_cnt_sq";
    }

    private String addLimit(String stat, int limit, int offset, String type) {
        stat = stat.trim();
        if ("ORACLE".equals(type) || "OCEANBASE_ORACLE".equals(type)) {
            stat = stat.replaceAll("(?i)\\s+LIMIT\\s+\\d+(\\s+OFFSET\\s+\\d+)?\\s*$", "");
            return "SELECT * FROM (SELECT ROWNUM rn, t.* FROM (" + stat + ") t WHERE ROWNUM <= " + (offset + limit) + ") WHERE rn > " + offset;
        }
        stat = stat.replaceAll("(?i)\\s+LIMIT\\s+\\d+(\\s+OFFSET\\s+\\d+)?\\s*$", "");
        return stat + " LIMIT " + limit + " OFFSET " + offset;
    }

    private String validateSchema(String schema, DatasourceConfig config, String dbType) {
        if (schema == null || schema.trim().isEmpty()) {
            return null;
        }
        String trimmed = schema.trim();
        if (!IDENTIFIER_PATTERN.matcher(trimmed).matches()) {
            throw new IllegalArgumentException("schema 参数不合法");
        }
        if ("ORACLE".equals(dbType) || "OCEANBASE_ORACLE".equals(dbType)) {
            return trimmed;
        }

        String allowedSchema = config.getDatabaseName();
        if (allowedSchema == null || allowedSchema.trim().isEmpty()) {
            throw new IllegalArgumentException("当前数据源未配置可用 schema");
        }
//        if (!trimmed.equalsIgnoreCase(allowedSchema.trim())) {
//            throw new IllegalArgumentException("schema 不在允许范围内");
//        }
        return trimmed;
    }

    @Override
    public Map<String, Object> queryObjectData(Long datasourceId, String schema, String objectName, String whereClause, int page, int size) {
        if (objectName == null || !IDENTIFIER_PATTERN.matcher(objectName).matches()) {
            throw new IllegalArgumentException("objectName 参数不合法");
        }
        if (whereClause != null && !whereClause.trim().isEmpty()) {
            throw new IllegalArgumentException("暂不支持自定义 whereClause 条件，请改用查询语句接口");
        }
        String stat = "SELECT * FROM " + objectName;
        return executeQuery(datasourceId, schema, stat, page, size);
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
            return "";
        });
    }

    @FunctionalInterface
    private interface RedisCallback<T> {
        T doInRedis(Jedis jedis);
    }

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
