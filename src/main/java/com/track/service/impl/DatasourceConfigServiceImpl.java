package com.track.service.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.track.entity.DatasourceConfig;
import com.track.mapper.DatasourceConfigMapper;
import com.track.service.DatasourceConfigService;
import com.track.util.DataSourceUtil;
import org.springframework.stereotype.Service;

/**
 * 数据源配置服务实现
 */
@Service
public class DatasourceConfigServiceImpl extends ServiceImpl<DatasourceConfigMapper, DatasourceConfig>
        implements DatasourceConfigService {

    @Override
    public boolean testConnection(Long id) {
        DatasourceConfig config = getById(id);
        if (config == null) return false;
        return DataSourceUtil.testConnection(config);
    }

    @Override
    public String buildJdbcUrl(DatasourceConfig config) {
        String type = DataSourceUtil.normalizeDbType(config.getType());
        switch (type) {
            case "MYSQL":
            case "OCEANBASE":
                return String.format("jdbc:mysql://%s:%d/%s?characterEncoding=utf-8&useSSL=false&serverTimezone=GMT%%2B8%s",
                        config.getHost(), config.getPort(), config.getDatabaseName(),
                        config.getExtraParams() != null ? "&" + config.getExtraParams() : "");
            case "ORACLE":
                return String.format("jdbc:oracle:thin:@%s:%d:%s%s",
                        config.getHost(), config.getPort(), config.getDatabaseName(),
                        config.getExtraParams() != null ? "?" + config.getExtraParams() : "");
            case "OCEANBASE_ORACLE":
                return String.format("jdbc:oceanbase://%s:%d/%s%s",
                        config.getHost(), config.getPort(), config.getDatabaseName(),
                        config.getExtraParams() != null ? "?" + config.getExtraParams() : "");
            default:
                throw new IllegalArgumentException("不支持的数据库类型: " + type);
        }
    }
}
