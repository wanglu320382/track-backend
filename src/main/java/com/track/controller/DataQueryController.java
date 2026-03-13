package com.track.controller;

import com.track.common.Result;
import com.track.service.DataQueryService;
import com.track.util.SqlCryptoUtil;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

/**
 * 数据查询控制器（REST API 入口）。
 * <p>
 * 提供 SQL 查询、表数据快捷查询及 Redis 值查询接口。
 * </p>
 */
@RestController
@RequestMapping("/api/query")
public class DataQueryController {

    private final DataQueryService dataQueryService;

    public DataQueryController(DataQueryService dataQueryService) {
        this.dataQueryService = dataQueryService;
    }

    /**
     * 执行 SQL 查询（仅支持 SELECT，支持分页；支持明文 sql 或加密 encryptedSql）。
     *
     * @param datasourceId 数据源 ID
     * @param params       请求体：sql 或 encryptedSql，page、size、schema（可选）
     * @return 查询结果（含 rows、total 等）
     */
    @PostMapping("/sql/{datasourceId}")
    public Result<Map<String, Object>> executeSql(@PathVariable Long datasourceId,
                                                  @RequestBody Map<String, Object> params) {
        String sql = (String) params.get("sql");
        String encryptedSql = (String) params.get("encryptedSql");

        if (encryptedSql != null && !encryptedSql.isEmpty()) {
            sql = SqlCryptoUtil.decrypt(encryptedSql);
        }

        Integer page = params.get("page") != null ? (Integer) params.get("page") : 1;
        Integer size = params.get("size") != null ? (Integer) params.get("size") : 100;
        String schema = (String) params.get("schema");
        return Result.success(dataQueryService.executeQuery(datasourceId, schema, sql, page, size));
    }

    /**
     * 根据表名快捷查询表数据，支持 schema、where 条件及分页。
     *
     * @param datasourceId 数据源 ID
     * @param tableName    表名
     * @param schema       模式名（可选）
     * @param where        WHERE 条件（可选）
     * @param page         页码，默认 1
     * @param size         每页条数，默认 100
     * @return 表数据（含 rows、total）
     */
    @GetMapping("/table/{datasourceId}")
    public Result<Map<String, Object>> queryTable(@PathVariable Long datasourceId,
                                                  @RequestParam String tableName,
                                                  @RequestParam(required = false) String schema,
                                                  @RequestParam(required = false) String where,
                                                  @RequestParam(defaultValue = "1") Integer page,
                                                  @RequestParam(defaultValue = "100") Integer size) {
        return Result.success(dataQueryService.queryTableData(datasourceId, schema, tableName, where, page, size));
    }

    /**
     * Redis：获取指定 key 的值。
     *
     * @param datasourceId 数据源 ID（需为 Redis 类型）
     * @param key          Redis key
     * @return key 对应的值（类型依实际存储）
     */
    @GetMapping("/redis/{datasourceId}")
    public Result<Object> getRedisValue(@PathVariable Long datasourceId,
                                       @RequestParam String key) {
        return Result.success(dataQueryService.getRedisValue(datasourceId, key));
    }
}
