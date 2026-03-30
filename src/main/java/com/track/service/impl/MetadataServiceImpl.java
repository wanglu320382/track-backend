package com.track.service.impl;

import com.track.common.ExceptionDetailUtil;
import com.track.entity.DatasourceConfig;
import com.track.service.DatasourceConfigService;
import com.track.service.MetadataService;
import com.track.util.DataSourceUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import redis.clients.jedis.Jedis;
import redis.clients.jedis.ScanParams;
import redis.clients.jedis.ScanResult;
import redis.clients.jedis.exceptions.JedisMovedDataException;

import javax.sql.DataSource;
import java.sql.*;
import java.util.*;

/**
 * 元数据服务实现：MySQL/OceanBase 使用 information_schema，Oracle 使用系统表
 */
@Service
public class MetadataServiceImpl implements MetadataService {

    private static final Logger log = LoggerFactory.getLogger(MetadataServiceImpl.class);
    private final DatasourceConfigService datasourceConfigService;

    public MetadataServiceImpl(DatasourceConfigService datasourceConfigService) {
        this.datasourceConfigService = datasourceConfigService;
    }

    private boolean isBlank(String s) {
        return s == null || s.trim().isEmpty();
    }

    @Override
    public List<String> getSchemas(Long datasourceId) {
        DatasourceConfig config = datasourceConfigService.getById(datasourceId);
        if (config == null) throw new IllegalArgumentException("数据源不存在");
        String type = DataSourceUtil.normalizeDbType(config.getType());

        if ("REDIS".equals(type)) {
            return Collections.singletonList("default");
        }

        DataSource ds = null;
        Connection conn = null;
        try {
            ds = DataSourceUtil.createDataSource(config);
            conn = ds.getConnection();
            if ("ORACLE".equals(type) || "OCEANBASE_ORACLE".equals(type)) {
                return getOracleSchemas(conn);
            }
            return getMysqlSchemas(conn);
        } catch (SQLException e) {
            log.error("获取库列表失败 datasourceId={}, type={}", datasourceId, type, e);
            throw new RuntimeException(ExceptionDetailUtil.formatSqlException(e));
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

    private List<String> getMysqlSchemas(Connection conn) throws SQLException {
        List<String> list = new ArrayList<>();
        try (ResultSet rs = conn.getMetaData().getCatalogs()) {
            while (rs.next()) {
                list.add(rs.getString("TABLE_CAT"));
            }
        }
        return list;
    }

    private List<String> getOracleSchemas(Connection conn) throws SQLException {
        List<String> list = new ArrayList<>();
        try (PreparedStatement ps = conn.prepareStatement(
                "SELECT DISTINCT OWNER FROM ALL_TABLES WHERE OWNER NOT IN ('SYS','SYSTEM') ORDER BY OWNER");
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                list.add(rs.getString("OWNER"));
            }
        }
        return list;
    }

    @Override
    public List<Map<String, Object>> getObjects(Long datasourceId, String schema) {
        DatasourceConfig config = datasourceConfigService.getById(datasourceId);
        if (config == null) throw new IllegalArgumentException("数据源不存在");
        String type = DataSourceUtil.normalizeDbType(config.getType());

        if ("REDIS".equals(type)) {
            Map<String, Object> m = new HashMap<String, Object>();
            m.put("name", "keys");
            m.put("comment", "Redis Keys");
            return Collections.<Map<String, Object>>singletonList(m);
        }

        DataSource ds = null;
        Connection conn = null;
        try {
            ds = DataSourceUtil.createDataSource(config);
            conn = ds.getConnection();

            if ("ORACLE".equals(type) || "OCEANBASE_ORACLE".equals(type)) {
                return getOracleObjects(conn, !isBlank(schema) ? schema : config.getUsername());
            }

            String db = !isBlank(schema) ? schema : config.getDatabaseName();
            if (isBlank(db)) {
                List<String> schemas = getMysqlSchemas(conn);
                db = schemas.isEmpty() ? null : schemas.get(0);
            }
            return getMysqlObjects(conn, db);
        } catch (SQLException e) {
            log.error("获取表列表失败 datasourceId={}, type={}, schema={}", datasourceId, type, schema, e);
            throw new RuntimeException(ExceptionDetailUtil.formatSqlException(e));
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

    private List<Map<String, Object>> getMysqlObjects(Connection conn, String schema) throws SQLException {
        List<Map<String, Object>> list = new ArrayList<>();
        String stat = "SELECT TABLE_NAME as name, TABLE_COMMENT as comment "
                + "FROM information_schema.TABLES "
                + "WHERE TABLE_SCHEMA = ? "
                + "ORDER BY TABLE_NAME";
        try (PreparedStatement ps = conn.prepareStatement(stat)) {
            ps.setString(1, schema);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    Map<String, Object> m = new HashMap<String, Object>();
                    m.put("name", rs.getString("name"));
                    m.put("comment", rs.getString("comment") != null ? rs.getString("comment") : "");
                    list.add(m);
                }
            }
        }
        return list;
    }

    private List<Map<String, Object>> getOracleObjects(Connection conn, String schema) throws SQLException {
        List<Map<String, Object>> list = new ArrayList<>();
        String stat = "SELECT t.TABLE_NAME as name, c.COMMENTS as table_comment "
                + "FROM ALL_TABLES t "
                + "LEFT JOIN ALL_TAB_COMMENTS c ON t.OWNER = c.OWNER AND t.TABLE_NAME = c.TABLE_NAME "
                + "WHERE t.OWNER = ? "
                + "ORDER BY t.TABLE_NAME";
        try (PreparedStatement ps = conn.prepareStatement(stat)) {
            ps.setString(1, schema);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    String objectComment = rs.getString("table_comment");
                    Map<String, Object> m = new HashMap<String, Object>();
                    m.put("name", rs.getString("name"));
                    m.put("comment", objectComment != null ? objectComment : "");
                    list.add(m);
                }
            }
        }
        return list;
    }

    @Override
    public List<Map<String, Object>> getObjectColumns(Long datasourceId, String schema, String objectName) {
        DatasourceConfig config = datasourceConfigService.getById(datasourceId);
        if (config == null) throw new IllegalArgumentException("数据源不存在");
        String type = DataSourceUtil.normalizeDbType(config.getType());

        if ("REDIS".equals(type)) {
            return Collections.emptyList();
        }

        DataSource ds = null;
        Connection conn = null;
        try {
            ds = DataSourceUtil.createDataSource(config);
            conn = ds.getConnection();

            if ("ORACLE".equals(type) || "OCEANBASE_ORACLE".equals(type)) {
                return getOracleColumns(conn, schema, objectName);
            }
            return getMysqlColumns(conn, schema, objectName);
        } catch (SQLException e) {
            log.error("获取表结构失败 datasourceId={}, type={}, schema={}, objectName={}", datasourceId, type, schema, objectName, e);
            throw new RuntimeException(ExceptionDetailUtil.formatSqlException(e));
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

    private List<Map<String, Object>> getMysqlColumns(Connection conn, String schema, String objectName) throws SQLException {
        List<Map<String, Object>> list = new ArrayList<>();
        String stat = "SELECT COLUMN_NAME, DATA_TYPE, COLUMN_TYPE, COLUMN_COMMENT, COLUMN_KEY, IS_NULLABLE "
                + "FROM information_schema.COLUMNS "
                + "WHERE TABLE_SCHEMA = ? AND TABLE_NAME = ? "
                + "ORDER BY ORDINAL_POSITION";
        try (PreparedStatement ps = conn.prepareStatement(stat)) {
            ps.setString(1, schema);
            ps.setString(2, objectName);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    Map<String, Object> m = new LinkedHashMap<>();
                    m.put("columnName", rs.getString("COLUMN_NAME"));
                    m.put("dataType", rs.getString("DATA_TYPE"));
                    m.put("columnType", rs.getString("COLUMN_TYPE"));
                    m.put("comment", rs.getString("COLUMN_COMMENT") != null ? rs.getString("COLUMN_COMMENT") : "");
                    m.put("columnKey", rs.getString("COLUMN_KEY") != null ? rs.getString("COLUMN_KEY") : "");
                    m.put("nullable", rs.getString("IS_NULLABLE"));
                    list.add(m);
                }
            }
        }
        return list;
    }

    private List<Map<String, Object>> getOracleColumns(Connection conn, String schema, String objectName) throws SQLException {
        List<Map<String, Object>> list = new ArrayList<>();
        String stat = "SELECT c.COLUMN_NAME, c.DATA_TYPE, c.DATA_LENGTH, c.NULLABLE, cc.COMMENTS "
                + "FROM ALL_TAB_COLUMNS c "
                + "LEFT JOIN ALL_COL_COMMENTS cc ON c.OWNER = cc.OWNER AND c.TABLE_NAME = cc.TABLE_NAME AND c.COLUMN_NAME = cc.COLUMN_NAME "
                + "WHERE c.OWNER = ? AND c.TABLE_NAME = ? "
                + "ORDER BY c.COLUMN_ID";
        try (PreparedStatement ps = conn.prepareStatement(stat)) {
            ps.setString(1, schema);
            ps.setString(2, objectName);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    Map<String, Object> m = new LinkedHashMap<>();
                    m.put("columnName", rs.getString("COLUMN_NAME"));
                    m.put("dataType", rs.getString("DATA_TYPE"));
                    m.put("columnType", rs.getString("DATA_TYPE") + "(" + rs.getInt("DATA_LENGTH") + ")");
                    m.put("comment", rs.getString("COMMENTS") != null ? rs.getString("COMMENTS") : "");
                    m.put("columnKey", "");
                    m.put("nullable", rs.getString("NULLABLE"));
                    list.add(m);
                }
            }
        }
        return list;
    }

    @Override
    public List<String> getRedisKeys(Long datasourceId, String pattern) {
        DatasourceConfig config = datasourceConfigService.getById(datasourceId);
        if (config == null) throw new IllegalArgumentException("数据源不存在");
        if (!"REDIS".equals(config.getType().toUpperCase())) {
            throw new IllegalArgumentException("仅 Redis 数据源支持此操作");
        }
        return executeWithRedirect(config, jedis -> {
            String p = (pattern == null || pattern.isEmpty()) ? "*" : pattern;
            int limit = 2000;
            ScanParams scanParams = new ScanParams().match(p).count(500);
            List<String> keys = new ArrayList<>();
            String cursor = ScanParams.SCAN_POINTER_START;
            do {
                ScanResult<String> r = jedis.scan(cursor, scanParams);
                cursor = r.getCursor();
                List<String> batch = r.getResult();
                if (batch != null && !batch.isEmpty()) {
                    for (String k : batch) {
                        keys.add(k);
                        if (keys.size() >= limit) {
                            return keys;
                        }
                    }
                }
            } while (!ScanParams.SCAN_POINTER_START.equals(cursor));
            return keys;
        });
    }

    @Override
    public Map<String, Object> getRedisKeyInfo(Long datasourceId, String key) {
        DatasourceConfig config = datasourceConfigService.getById(datasourceId);
        if (config == null) throw new IllegalArgumentException("数据源不存在");
        return executeWithRedirect(config, jedis -> {
            String type = jedis.type(key);
            Object value;
            if ("string".equals(type)) {
                value = jedis.get(key);
            } else if ("hash".equals(type)) {
                value = jedis.hgetAll(key);
            } else if ("list".equals(type)) {
                value = jedis.lrange(key, 0, -1);
            } else if ("set".equals(type)) {
                value = jedis.smembers(key);
            } else if ("zset".equals(type)) {
                value = jedis.zrangeWithScores(key, 0, -1);
            } else {
                value = null;
            }
            Map<String, Object> r = new HashMap<String, Object>();
            r.put("type", type);
            r.put("value", value != null ? value : "");
            return r;
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
