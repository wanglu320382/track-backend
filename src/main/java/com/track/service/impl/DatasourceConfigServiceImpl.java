package com.track.service.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.track.entity.DatasourceConfig;
import com.track.mapper.DatasourceConfigMapper;
import com.track.service.DatasourceConfigService;
import com.track.util.DataSourceUtil;
import org.springframework.stereotype.Service;

import java.util.Arrays;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;
import java.util.regex.Pattern;

/**
 * 数据源配置服务实现
 */
@Service
public class DatasourceConfigServiceImpl extends ServiceImpl<DatasourceConfigMapper, DatasourceConfig>
        implements DatasourceConfigService {
    private static final Pattern HOST_PATTERN = Pattern.compile("^[A-Za-z0-9.-]+$");
    private static final Pattern DB_NAME_PATTERN = Pattern.compile("^[A-Za-z0-9_.$-]*$");
    private static final Pattern PARAM_KEY_PATTERN = Pattern.compile("^[A-Za-z0-9._-]+$");
    private static final Pattern PARAM_VALUE_PATTERN = Pattern.compile("^[A-Za-z0-9._,:%+\\-/*]*$");

    private static final Set<String> MYSQL_ALLOWED_PARAMS = Collections.unmodifiableSet(new LinkedHashSet<>(
            Arrays.asList(
                    "allowPublicKeyRetrieval",
                    "allowMultiQueries",
                    "autoReconnect",
                    "connectTimeout",
                    "socketTimeout",
                    "useUnicode",
                    "characterEncoding",
                    "serverTimezone",
                    "useSSL",
                    "rewriteBatchedStatements",
                    "zeroDateTimeBehavior"
            )
    ));

    private static final Set<String> ORACLE_ALLOWED_PARAMS = Collections.unmodifiableSet(new LinkedHashSet<>(
            Arrays.asList(
                    "oracle.net.CONNECT_TIMEOUT",
                    "oracle.jdbc.ReadTimeout",
                    "defaultRowPrefetch"
            )
    ));

    @Override
    public boolean testConnection(Long id) {
        DatasourceConfig config = getById(id);
        if (config == null) return false;
        return DataSourceUtil.testConnection(config);
    }

    @Override
    public String buildJdbcUrl(DatasourceConfig config) {
        String type = DataSourceUtil.normalizeDbType(config.getType());
        String host = sanitizeHost(config.getHost());
        int port = config.getPort() != null ? config.getPort() : 3306;
        String db = sanitizeDatabaseName(config.getDatabaseName());
        Map<String, String> safeExtraParams = parseAndValidateExtraParams(config.getExtraParams());

        switch (type) {
            case "MYSQL":
            case "OCEANBASE": {
                String base = String.format(
                        "jdbc:mysql://%s:%d/%s?characterEncoding=utf-8&useSSL=false&serverTimezone=GMT%%2B8",
                        host, port, db
                );
                return appendParams(base, safeExtraParams, MYSQL_ALLOWED_PARAMS, "&");
            }
            case "ORACLE": {
                int oraclePort = config.getPort() != null ? config.getPort() : 1521;
                String sid = db.isEmpty() ? "orcl" : db;
                String base = String.format("jdbc:oracle:thin:@%s:%d:%s", host, oraclePort, sid);
                return appendParams(base, safeExtraParams, ORACLE_ALLOWED_PARAMS, "?");
            }
            case "OCEANBASE_ORACLE": {
                int obPort = config.getPort() != null ? config.getPort() : 1521;
                String sid = db.isEmpty() ? "orcl" : db;
                String base = String.format("jdbc:oceanbase://%s:%d/%s", host, obPort, sid);
                return appendParams(base, safeExtraParams, ORACLE_ALLOWED_PARAMS, "?");
            }
            default:
                throw new IllegalArgumentException("不支持的数据库类型");
        }
    }

    private String sanitizeHost(String host) {
        if (host == null || host.trim().isEmpty() || !HOST_PATTERN.matcher(host.trim()).matches()) {
            throw new IllegalArgumentException("非法数据源配置");
        }
        return host.trim();
    }

    private String sanitizeDatabaseName(String databaseName) {
        if (databaseName == null) {
            return "";
        }
        String db = databaseName.trim();
        if (!DB_NAME_PATTERN.matcher(db).matches()) {
            throw new IllegalArgumentException("非法数据源配置");
        }
        return db;
    }

    private Map<String, String> parseAndValidateExtraParams(String extraParams) {
        if (extraParams == null || extraParams.trim().isEmpty()) {
            return Collections.emptyMap();
        }
        Map<String, String> params = new LinkedHashMap<>();
        String[] pairs = extraParams.split("&");
        for (String pair : pairs) {
            if (pair == null || pair.trim().isEmpty()) {
                continue;
            }
            String[] kv = pair.split("=", 2);
            String key = kv[0].trim();
            String value = kv.length == 2 ? kv[1].trim() : "";
            if (!PARAM_KEY_PATTERN.matcher(key).matches() || !PARAM_VALUE_PATTERN.matcher(value).matches()) {
                throw new IllegalArgumentException("非法数据源配置");
            }
            params.put(key, value);
        }
        return params;
    }

    private String appendParams(String baseUrl, Map<String, String> params, Set<String> allowedParamKeys, String separator) {
        if (params.isEmpty()) {
            return baseUrl;
        }
        StringBuilder builder = new StringBuilder(baseUrl);
        boolean first = true;
        for (Map.Entry<String, String> entry : params.entrySet()) {
            if (!allowedParamKeys.contains(entry.getKey())) {
                throw new IllegalArgumentException("非法数据源配置");
            }
            if (first) {
                builder.append(separator);
                first = false;
            } else {
                builder.append("&");
            }
            builder.append(entry.getKey()).append("=").append(entry.getValue());
        }
        return builder.toString();
    }
}
