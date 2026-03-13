/*
 * DataQueryService.java
 * 问题溯源系统 - 数据查询服务接口（SQL 查询、表数据、Redis 值，仅读）
 */
package com.track.service;

import java.util.List;
import java.util.Map;

/**
 * 数据查询服务接口。
 * <p>
 * 提供 SQL 查询（仅 SELECT）、按表查询及 Redis 值查询，支持分页；禁止写操作。
 * </p>
 *
 * @see com.track.service.impl.DataQueryServiceImpl
 */
public interface DataQueryService {

    /**
     * 执行 SQL 查询（仅允许 SELECT），支持分页。
     *
     * @param datasourceId 数据源 ID
     * @param schema       库名或 schema，可为空
     * @param sql          SELECT 语句
     * @param page         页码，从 1 开始
     * @param size         每页条数
     * @return 含 columns、rows、total、page、size 的 Map
     */
    Map<String, Object> executeQuery(Long datasourceId, String schema, String sql, int page, int size);

    /**
     * 按表名查询数据（分页），可带 WHERE 条件。
     *
     * @param datasourceId 数据源 ID
     * @param schema       库名或 schema
     * @param tableName    表名
     * @param whereClause  WHERE 条件（不含 WHERE 关键字），可为空
     * @param page         页码
     * @param size         每页条数
     * @return 含 columns、rows、total、page、size 的 Map
     */
    Map<String, Object> queryTableData(Long datasourceId, String schema, String tableName, String whereClause, int page, int size);

    /**
     * Redis：获取指定 key 的值（按类型返回 string/hash/list/set/zset 等）。
     *
     * @param datasourceId 数据源 ID（须为 Redis）
     * @param key          key 名称
     * @return 对应类型的值，未知类型返回 null
     */
    Object getRedisValue(Long datasourceId, String key);
}
