/*
 * DatasourceConfigService.java
 * 问题溯源系统 - 数据源配置服务接口（CRUD、测试连接、JDBC URL 构建）
 */
package com.track.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.track.entity.DatasourceConfig;

import java.util.List;

/**
 * 数据源配置服务接口。
 * <p>
 * 负责数据源 CRUD、测试连接及 JDBC URL 构建，支持 Oracle/MySQL/OceanBase/Redis 等类型。
 * </p>
 *
 * @see com.track.service.impl.DatasourceConfigServiceImpl
 */
public interface DatasourceConfigService extends IService<DatasourceConfig> {

    /**
     * 测试指定数据源连接是否可用。
     *
     * @param id 数据源配置主键 ID
     * @return 连接成功返回 true，失败或数据源不存在返回 false
     */
    boolean testConnection(Long id);

    /**
     * 根据配置构建 JDBC URL（MySQL/Oracle/OceanBase 等）。
     *
     * @param config 数据源配置
     * @return 完整 JDBC URL 字符串
     * @throws IllegalArgumentException 不支持的数据库类型时抛出
     */
    String buildJdbcUrl(DatasourceConfig config);
}
