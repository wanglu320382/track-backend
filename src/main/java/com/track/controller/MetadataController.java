package com.track.controller;

import com.track.common.Result;
import com.track.dto.ColumnInfo;
import com.track.dto.ObjectInfo;
import com.track.service.MetadataService;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * 元数据控制器（REST API 入口）。
 */
@RestController
@RequestMapping("/api/metadata")
public class MetadataController {

    private final MetadataService metadataService;

    public MetadataController(MetadataService metadataService) {
        this.metadataService = metadataService;
    }

    @GetMapping("/schemas/{datasourceId}")
    public Result<List<String>> getSchemas(@PathVariable Long datasourceId) {
        return Result.success(metadataService.getSchemas(datasourceId));
    }

    @GetMapping("/objects/{datasourceId}")
    public Result<List<Map<String, Object>>> getObjects(@PathVariable Long datasourceId,
                                                       @RequestParam(required = false) String schema) {
        String s = (schema == null || schema.trim().isEmpty()) ? null : schema;
        return Result.success(metadataService.getObjects(datasourceId, s));
    }

    @GetMapping("/columns/{datasourceId}")
    public Result<ObjectInfo> getObjectStructure(@PathVariable Long datasourceId,
                                               @RequestParam String objectName,
                                               @RequestParam(required = false) String schema) {
        String s = (schema == null || schema.trim().isEmpty()) ? null : schema;
        List<Map<String, Object>> cols = metadataService.getObjectColumns(datasourceId, s, objectName);
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
        ObjectInfo info = new ObjectInfo(objectName, "", columns);
        return Result.success(info);
    }

    @GetMapping("/redis/keys/{datasourceId}")
    public Result<List<String>> getRedisKeys(@PathVariable Long datasourceId,
                                             @RequestParam(required = false) String pattern) {
        return Result.success(metadataService.getRedisKeys(datasourceId, pattern));
    }

    @GetMapping("/redis/key/{datasourceId}")
    public Result<Map<String, Object>> getRedisKeyInfo(@PathVariable Long datasourceId,
                                                      @RequestParam String key) {
        return Result.success(metadataService.getRedisKeyInfo(datasourceId, key));
    }
}
