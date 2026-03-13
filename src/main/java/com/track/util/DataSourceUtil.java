package com.track.util;

import com.track.entity.DatasourceConfig;
import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.SQLException;

/**
 * 根据数据源配置创建 DataSource 连接
 */
public class DataSourceUtil {

    /**
     * 根据配置创建 HikariDataSource（适用于 Oracle、MySQL、OceanBase）
     */
    public static DataSource createDataSource(DatasourceConfig config) {
        String type = normalizeDbType(config.getType());
        if ("REDIS".equals(type)) {
            throw new IllegalArgumentException("Redis 不支持 JDBC 连接，请使用 Redis 专用接口");
        }

        HikariConfig hikariConfig = new HikariConfig();
        hikariConfig.setJdbcUrl(buildJdbcUrl(config, type));
        hikariConfig.setUsername(config.getUsername());
        hikariConfig.setPassword(config.getPassword());
        hikariConfig.setDriverClassName(getDriverClassName(type));
        hikariConfig.setMaximumPoolSize(2);
        hikariConfig.setConnectionTimeout(10000);

        return new HikariDataSource(hikariConfig);
    }

    /**
     * 统一规范数据库类型，兼容历史写法。
     * OCEANBASE_ORACLE 使用 Oracle 驱动；OCEANBASE_MYSQL/OCEANBASE 使用 MySQL 驱动。
     */
    public static String normalizeDbType(String rawType) {
        if (rawType == null) {
            return "";
        }
        String type = rawType.toUpperCase();
        if (type.contains("ORACLE") && (type.startsWith("OCEANBASE") || type.startsWith("OCEANB"))) {
            return "OCEANBASE_ORACLE";
        }
        if (type.startsWith("OCEANBASE") || type.startsWith("OCEANB")) {
            return "OCEANBASE";
        }
        return type;
    }

    private static String buildJdbcUrl(DatasourceConfig config, String type) {
        String host = config.getHost();
        int port = config.getPort() != null ? config.getPort() : 3306;
        String db = config.getDatabaseName() != null ? config.getDatabaseName() : "";
        String extra = config.getExtraParams() != null ? config.getExtraParams() : "";
        if ("MYSQL".equals(type) || "OCEANBASE".equals(type)) {
            return String.format(
                    "jdbc:mysql://%s:%d/%s?characterEncoding=utf-8&useSSL=false&serverTimezone=GMT%%2B8%s",
                    host, port, db, extra.isEmpty() ? "" : "&" + extra);
        }
        if ("ORACLE".equals(type)) {
            return String.format(
                    "jdbc:oracle:thin:@%s:%d:%s",
                    host, port, db.isEmpty() ? "orcl" : db);
        }
        // OceanBase Oracle 模式必须用官方驱动和 jdbc:oceanbase://，不能用 ojdbc8
        if ("OCEANBASE_ORACLE".equals(type)) {
            return String.format(
                    "jdbc:oceanbase://%s:%d/%s%s",
                    host, port, db.isEmpty() ? "orcl" : db, extra.isEmpty() ? "" : "?" + extra);
        }
        throw new IllegalArgumentException("不支持的数据库类型: " + type);
    }

    private static String getDriverClassName(String type) {
        if ("MYSQL".equals(type) || "OCEANBASE".equals(type)) {
            return "com.mysql.cj.jdbc.Driver";
        }
        if ("ORACLE".equals(type)) {
            return "oracle.jdbc.OracleDriver";
        }
        if ("OCEANBASE_ORACLE".equals(type)) {
            return "com.oceanbase.jdbc.Driver";
        }
        throw new IllegalArgumentException("不支持的数据库类型: " + type);
    }

    /**
     * 测试连接是否可用
     */
    public static boolean testConnection(DatasourceConfig config) {
        if ("REDIS".equals(normalizeDbType(config.getType()))) {
            return testRedisConnection(config);
        }
        DataSource ds = null;
        try {
            ds = createDataSource(config);
            Connection conn = null;
            try {
                conn = ds.getConnection();
                return conn.isValid(5);
            } finally {
                if (conn != null) {
                    conn.close();
                }
            }
        } catch (Exception e) {
            return false;
        } finally {
            closeDataSource(ds);
        }
    }

    private static boolean testRedisConnection(DatasourceConfig config) {
        try {
            redis.clients.jedis.Jedis jedis = new redis.clients.jedis.Jedis(
                config.getHost(),
                config.getPort() != null ? config.getPort() : 6379
            );
            if (config.getPassword() != null && !config.getPassword().isEmpty()) {
                jedis.auth(config.getPassword());
            }
            jedis.ping();
            jedis.close();
            return true;
        } catch (Exception e) {
            return false;
        }
    }

    /**
     * 关闭 DataSource
     */
    public static void closeDataSource(DataSource ds) {
        if (ds instanceof HikariDataSource) {
            ((HikariDataSource) ds).close();
        }
    }
}
