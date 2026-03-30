package com.track.util;

import com.track.entity.DatasourceConfig;
import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;

import javax.sql.DataSource;
import javax.crypto.Cipher;
import javax.crypto.spec.IvParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.sql.Connection;
import java.util.ArrayList;
import java.util.Base64;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

/**
 * 根据数据源配置创建 DataSource 连接
 */
public class DataSourceUtil {

    private static final String ENC_PREFIX = "ENC(";
    private static final String ENC_SUFFIX = ")";
    private static final int AES_128_BYTES = 16;
    private static final String AES_ALGORITHM = "AES";
    private static final String AES_TRANSFORMATION = "AES/CBC/PKCS5Padding";
    private static final String DB_AES_KEY_ENV = "TRACK_DB_AES_KEY";
    private static final String DB_AES_IV_ENV = "TRACK_DB_AES_IV";

    /**
     * 根据配置创建 HikariDataSource（适用于 Oracle、MySQL、OceanBase）
     */
    public static DataSource createDataSource(DatasourceConfig config) {
        return createHikariDataSource(config);
    }

    private static HikariDataSource createHikariDataSource(DatasourceConfig config) {
        String type = normalizeDbType(config.getType());
        if ("REDIS".equals(type)) {
            throw new IllegalArgumentException("Redis 不支持 JDBC 连接，请使用 Redis 专用接口");
        }

        HikariConfig hikariConfig = new HikariConfig();
        hikariConfig.setJdbcUrl(buildJdbcUrl(config, type));
        hikariConfig.setUsername(config.getUsername());
        hikariConfig.setPassword(resolveSecret(config.getPassword()));
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
        String host = requireSafeHost(config.getHost());
        int port = config.getPort() != null ? config.getPort() : 3306;
        String extra = sanitizeExtraParams(type, config.getExtraParams());
        if ("MYSQL".equals(type) || "OCEANBASE".equals(type)) {
            String db = requireSafeDbName(config.getDatabaseName());
            return String.format(
                    "jdbc:mysql://%s:%d/%s?characterEncoding=utf-8&useSSL=false&serverTimezone=GMT%%2B8%s",
                    host, port, db, extra.isEmpty() ? "" : "&" + extra);
        }
        if ("ORACLE".equals(type)) {
            int oraclePort = config.getPort() != null ? config.getPort() : 1521;
            String mode = normalizeOracleConnectMode(config.getOracleConnectMode());
            String db = requireSafeOracleIdentifier(config.getDatabaseName(), mode);
            if (db.isEmpty()) {
                db = "orcl";
            }
            if ("SERVICE_NAME".equals(mode)) {
                return String.format("jdbc:oracle:thin:@//%s:%d/%s", host, oraclePort, db);
            }
            return String.format("jdbc:oracle:thin:@%s:%d:%s", host, oraclePort, db);
        }
        // OceanBase Oracle 模式必须用官方驱动和 jdbc:oceanbase://，不能用 ojdbc8
        if ("OCEANBASE_ORACLE".equals(type)) {
            String db = requireSafeDbName(config.getDatabaseName());
            return String.format(
                    "jdbc:oceanbase://%s:%d/%s%s",
                    host, port, db.isEmpty() ? "orcl" : db, extra.isEmpty() ? "" : "?" + extra);
        }
        throw new IllegalArgumentException("不支持的数据库类型: " + type);
    }

    /**
     * Oracle：未配置或空视为 SID（与历史行为一致）。
     */
    private static String normalizeOracleConnectMode(String raw) {
        if (raw == null || raw.trim().isEmpty()) {
            return "SID";
        }
        String m = raw.trim().toUpperCase(Locale.ROOT);
        if ("SERVICE_NAME".equals(m) || "SERVICE".equals(m)) {
            return "SERVICE_NAME";
        }
        return "SID";
    }

    /**
     * SID 与 Service Name 允许的字符集不同（Service Name 常含点号）。
     */
    private static String requireSafeOracleIdentifier(String raw, String mode) {
        if (raw == null) {
            return "";
        }
        String v = raw.trim();
        if (v.isEmpty()) {
            return "";
        }
        String pattern = "SERVICE_NAME".equals(mode) ? "^[a-zA-Z0-9_.$-]+$" : "^[a-zA-Z0-9_\\-]+$";
        if (!v.matches(pattern)) {
            throw new IllegalArgumentException("databaseName 非法");
        }
        return v;
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
        try (HikariDataSource ds = createHikariDataSource(config);
             Connection conn = ds.getConnection()) {
            return conn.isValid(5);
        } catch (Exception e) {
            return false;
        }
    }

    private static boolean testRedisConnection(DatasourceConfig config) {
        String host = requireSafeHost(config.getHost());
        int port = config.getPort() != null ? config.getPort() : 6379;
        try (redis.clients.jedis.Jedis jedis = new redis.clients.jedis.Jedis(host, port)) {
            String password = resolveSecret(config.getPassword());
            if (password != null && !password.isEmpty()) {
                jedis.auth(password);
            }
            jedis.ping();
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

    private static String resolveSecret(String raw) {
        if (raw == null) {
            return null;
        }
        String v = raw.trim();
        if (v.isEmpty()) {
            return "";
        }
        if (v.startsWith(ENC_PREFIX) && v.endsWith(ENC_SUFFIX) && v.length() > (ENC_PREFIX.length() + ENC_SUFFIX.length())) {
            String cipherText = v.substring(ENC_PREFIX.length(), v.length() - ENC_SUFFIX.length()).trim();
            return decryptAesBase64(cipherText, DB_AES_KEY_ENV, DB_AES_IV_ENV);
        }
        return v;
    }

    private static String decryptAesBase64(String cipherTextBase64, String keyEnv, String ivEnv) {
        try {
            byte[] keyBytes = require16BytesUtf8(keyEnv);
            byte[] ivBytes = require16BytesUtf8(ivEnv);

            SecretKeySpec keySpec = new SecretKeySpec(keyBytes, AES_ALGORITHM);
            IvParameterSpec ivSpec = new IvParameterSpec(ivBytes);

            Cipher cipher = Cipher.getInstance(AES_TRANSFORMATION);
            cipher.init(Cipher.DECRYPT_MODE, keySpec, ivSpec);

            byte[] decoded = Base64.getDecoder().decode(cipherTextBase64);
            byte[] decrypted = cipher.doFinal(decoded);
            return new String(decrypted, StandardCharsets.UTF_8);
        } catch (Exception e) {
            throw new RuntimeException("解密配置失败");
        }
    }

    private static byte[] require16BytesUtf8(String envKey) {
        String v = System.getenv(envKey);
        if (v == null || v.trim().isEmpty()) {
            v = System.getProperty(envKey);
        }
        if (v == null || v.trim().isEmpty()) {
            throw new IllegalStateException("缺少必要配置：" + envKey);
        }
        byte[] bytes = v.getBytes(StandardCharsets.UTF_8);
        if (bytes.length != AES_128_BYTES) {
            throw new IllegalStateException(envKey + " 长度必须为 16 字节(UTF-8)");
        }
        return bytes;
    }

    private static String requireSafeHost(String rawHost) {
        if (rawHost == null) {
            throw new IllegalArgumentException("host 不能为空");
        }
        String host = rawHost.trim();
        if (host.isEmpty()) {
            throw new IllegalArgumentException("host 不能为空");
        }
        // 禁止携带协议/路径/查询串等，避免注入到 JDBC URL 结构中
        if (host.contains("://") || host.contains("/") || host.contains("?") || host.contains("#") || host.contains("&")) {
            throw new IllegalArgumentException("host 非法");
        }
        // 允许 hostname / IPv4 / 简单 IPv6（带冒号）/ 带方括号 IPv6
        if (!host.matches("^[a-zA-Z0-9.\\-\\[\\]:]+$")) {
            throw new IllegalArgumentException("host 非法");
        }
        return host;
    }

    private static String requireSafeDbName(String rawDb) {
        if (rawDb == null) {
            return "";
        }
        String db = rawDb.trim();
        if (db.isEmpty()) {
            return "";
        }
        // 对于 mysql 的 path segment，严格限制，避免注入 "/" 或查询串等结构
        if (!db.matches("^[a-zA-Z0-9_\\-]+$")) {
            throw new IllegalArgumentException("databaseName 非法");
        }
        return db;
    }

    private static String sanitizeExtraParams(String type, String rawExtraParams) {
        if (rawExtraParams == null) {
            return "";
        }
        String extra = rawExtraParams.trim();
        if (extra.isEmpty()) {
            return "";
        }

        Set<String> allow = allowedParamsFor(type);
        Map<String, String> parsed = parseQueryLike(extra);
        if (parsed.isEmpty()) {
            return "";
        }

        List<String> parts = new ArrayList<>();
        for (Map.Entry<String, String> e : parsed.entrySet()) {
            String key = e.getKey();
            if (!allow.contains(key)) {
                continue;
            }
            String value = e.getValue();
            if (value == null || value.isEmpty()) {
                parts.add(urlEncode(key));
            } else {
                parts.add(urlEncode(key) + "=" + urlEncode(value));
            }
        }
        return String.join("&", parts);
    }

    private static Set<String> allowedParamsFor(String type) {
        String t = type == null ? "" : type.toUpperCase(Locale.ROOT);
        Set<String> set = new LinkedHashSet<>();
        if ("MYSQL".equals(t) || "OCEANBASE".equals(t)) {
            set.add("characterEncoding");
            set.add("useSSL");
            set.add("serverTimezone");
            set.add("connectTimeout");
            set.add("socketTimeout");
            set.add("useUnicode");
            set.add("allowPublicKeyRetrieval");
            set.add("zeroDateTimeBehavior");
        } else if ("ORACLE".equals(t) || "OCEANBASE_ORACLE".equals(t)) {
            set.add("oracle.net.CONNECT_TIMEOUT");
            set.add("oracle.jdbc.ReadTimeout");
            set.add("defaultRowPrefetch");
        }
        return set;
    }

    private static Map<String, String> parseQueryLike(String raw) {
        String s = raw;
        if (s.startsWith("?")) {
            s = s.substring(1);
        }
        Map<String, String> map = new LinkedHashMap<>();
        for (String pair : s.split("&")) {
            String p = pair == null ? "" : pair.trim();
            if (p.isEmpty()) {
                continue;
            }
            int idx = p.indexOf('=');
            if (idx < 0) {
                map.put(p, "");
            } else {
                String k = p.substring(0, idx).trim();
                String v = p.substring(idx + 1).trim();
                if (!k.isEmpty()) {
                    map.put(k, v);
                }
            }
        }
        return map;
    }

    private static String urlEncode(String s) {
        try {
            return URLEncoder.encode(s, StandardCharsets.UTF_8.name());
        } catch (UnsupportedEncodingException e) {
            // UTF-8 is required by JVM spec; keep a safe fallback.
            throw new IllegalStateException("UTF-8 编码不可用", e);
        }
    }
}
