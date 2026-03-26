/*
 * MetadataService.java
 * 问题溯源系统 - 元数据服务接口（库/表/列结构、Redis keys 信息）
 */
package com.track.service;

import java.util.List;
import java.util.Map;

/**
 * 元数据服务接口。
 * <p>
 * 获取库/表/列结构及注释信息；对 Redis 数据源提供 key 列表与 key 信息查询。
 * 支持 MySQL、Oracle、OceanBase、Redis 等。
 * </p>
 *
 * @see com.track.service.impl.MetadataServiceImpl
 */
public interface MetadataService {

    /**
     * 获取数据库/模式列表（MySQL 为库名，Oracle 为 schema/owner）。
     *
     * @param datasourceId 数据源 ID
     * @return 库/模式名称列表；Redis 返回单元素 "default"
     */
    List<String> getSchemas(Long datasourceId);

    /**
     * 获取指定库/模式下的表列表（含表名、注释）。
     *
     * @param datasourceId 数据源 ID
     * @param schema       库名或 schema，可为空
     * @return 表信息列表，每项含 name、comment
     */
    List<Map<String, Object>> getObjects(Long datasourceId, String schema);

    /**
     * 获取表结构详情（字段名、类型、注释、主键、是否可空等）。
     *
     * @param datasourceId 数据源 ID
     * @param schema       库名或 schema
     * @param objectName   表名
     * @return 列信息列表，每项含 columnName、dataType、columnType、comment、columnKey、nullable
     */
    List<Map<String, Object>> getObjectColumns(Long datasourceId, String schema, String objectName);

    /**
     * Redis：按模式扫描 key 列表。
     *
     * @param datasourceId 数据源 ID（须为 Redis）
     * @param pattern      匹配模式，如 "*"、"user:*"，空则视为 "*"
     * @return 匹配的 key 列表
     */
    List<String> getRedisKeys(Long datasourceId, String pattern);

    /**
     * Redis：获取指定 key 的类型和值。
     *
     * @param datasourceId 数据源 ID（须为 Redis）
     * @param key          key 名称
     * @return 含 type、value 的 Map
     */
    Map<String, Object> getRedisKeyInfo(Long datasourceId, String key);
}
