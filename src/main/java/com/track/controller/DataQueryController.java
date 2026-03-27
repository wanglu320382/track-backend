package com.track.controller;

import com.track.common.Result;
import com.track.service.DataQueryService;
import com.track.util.StatCryptoUtil;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

/**
 * 数据查询控制器（REST API 入口）。
 */
@RestController
@RequestMapping("/api/query")
public class DataQueryController {

    private final DataQueryService dataQueryService;
    private final StatCryptoUtil statCryptoUtil;

    public DataQueryController(DataQueryService dataQueryService, StatCryptoUtil statCryptoUtil) {
        this.dataQueryService = dataQueryService;
        this.statCryptoUtil = statCryptoUtil;
    }

    /**
     * 执行查询（仅支持 SELECT，支持分页；支持明文 stat 或加密 encryptedStat）。
     */
    @PostMapping("/stat/{datasourceId}")
    public Result<Map<String, Object>> executeStat(@PathVariable Long datasourceId,
                                                  @RequestBody Map<String, Object> params) {
        String stat = (String) params.get("stat");
        String encryptedStat = (String) params.get("encryptedStat");

        if (encryptedStat != null && !encryptedStat.isEmpty()) {
            stat = statCryptoUtil.decrypt(encryptedStat);
        }

        Integer page = params.get("page") != null ? (Integer) params.get("page") : 1;
        Integer size = params.get("size") != null ? (Integer) params.get("size") : 100;
        String schema = (String) params.get("schema");
        return Result.success(dataQueryService.executeQuery(datasourceId, schema, stat, page, size));
    }

    /**
     * 根据表名快捷查询表数据，支持 schema、where 条件及分页。
     */
    @GetMapping("/object/{datasourceId}")
    public Result<Map<String, Object>> queryObject(@PathVariable Long datasourceId,
                                                  @RequestParam String objectName,
                                                  @RequestParam(required = false) String schema,
                                                  @RequestParam(required = false) String where,
                                                  @RequestParam(defaultValue = "1") Integer page,
                                                  @RequestParam(defaultValue = "100") Integer size) {
        return Result.success(dataQueryService.queryObjectData(datasourceId, schema, objectName, where, page, size));
    }

    @GetMapping("/redis/{datasourceId}")
    public Result<Object> getRedisValue(@PathVariable Long datasourceId,
                                       @RequestParam String key) {
        return Result.success(dataQueryService.getRedisValue(datasourceId, key));
    }
}
