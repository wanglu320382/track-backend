package com.track.controller;

import com.track.common.Result;
import com.track.dto.ColumnInfo;
import com.track.dto.TableInfo;
import com.track.service.MetadataService;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * 元数据控制器（REST API 入口）。
 * <p>
 * 提供库/表/列结构、注释信息及 Redis keys 查询接口。
 * </p>
 */
@RestController
@RequestMapping("/api/metadata")
public class MetadataController {

    private final MetadataService metadataService;

    public MetadataController(MetadataService metadataService) {
        this.metadataService = metadataService;
    }

    /**
     * 获取数据库/模式列表（支持 MySQL、OceanBase、Oracle）。
     *
     * @param datasourceId 数据源 ID
     * @return 模式名列表
     */
    @GetMapping("/schemas/{datasourceId}")
    public Result<List<String>> getSchemas(@PathVariable Long datasourceId) {
        return Result.success(metadataService.getSchemas(datasourceId));
    }

    /**
     * 获取指定数据源下的表列表，可按 schema 过滤。
     *
     * @param datasourceId 数据源 ID
     * @param schema       模式名（可选）
     * @return 表信息列表（含表名等）
     */
    @GetMapping("/tables/{datasourceId}")
    public Result<List<Map<String, Object>>> getTables(@PathVariable Long datasourceId,
                                                       @RequestParam(required = false) String schema) {
        String s = (schema == null || schema.trim().isEmpty()) ? null : schema;
        return Result.success(metadataService.getTables(datasourceId, s));
    }

    /**
     * 获取表结构详情（字段名、类型、注释、主键等）。
     *
     * @param datasourceId 数据源 ID
     * @param tableName    表名
     * @param schema       模式名（可选）
     * @return 表信息（含列列表）
     */
    @GetMapping("/columns/{datasourceId}")
    public Result<TableInfo> getTableStructure(@PathVariable Long datasourceId,
                                               @RequestParam String tableName,
                                               @RequestParam(required = false) String schema) {
        String s = (schema == null || schema.trim().isEmpty()) ? null : schema;
        List<Map<String, Object>> cols = metadataService.getTableColumns(datasourceId, s, tableName);
        List<ColumnInfo> columns = cols.stream().map(m -> {
            ColumnInfo c = new ColumnInfo();
            c.setColumnName((String) m.get("columnName"));
            c.setDataType((String) m.get("dataType"));
            c.setColumnType((String) m.getOrDefault("columnType", m.get("dataType")));
            c.setColumnComment((String) m.getOrDefault("comment", ""));
            c.setColumnKey((String) m.getOrDefault("columnKey", ""));
            c.setNullable((String) m.getOrDefault("nullable", ""));
            c.setPrimaryKey("PRI".equals(m.get("columnKey")));
            return c;
        }).collect(Collectors.toList());
        TableInfo info = new TableInfo(tableName, "", columns);
        return Result.success(info);
    }

    /**
     * Redis：按模式获取 key 列表（如 pattern=user:*）。
     *
     * @param datasourceId 数据源 ID（需为 Redis 类型）
     * @param pattern      key 匹配模式（可选）
     * @return key 列表
     */
    @GetMapping("/redis/keys/{datasourceId}")
    public Result<List<String>> getRedisKeys(@PathVariable Long datasourceId,
                                             @RequestParam(required = false) String pattern) {
        return Result.success(metadataService.getRedisKeys(datasourceId, pattern));
    }

    /**
     * Redis：获取指定 key 的类型、TTL 等详情。
     *
     * @param datasourceId 数据源 ID（需为 Redis 类型）
     * @param key          Redis key
     * @return key 详情（类型、TTL 等）
     */
    @GetMapping("/redis/key/{datasourceId}")
    public Result<Map<String, Object>> getRedisKeyInfo(@PathVariable Long datasourceId,
                                                      @RequestParam String key) {
        return Result.success(metadataService.getRedisKeyInfo(datasourceId, key));
    }
}
