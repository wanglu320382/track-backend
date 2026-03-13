package com.track.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.track.common.Result;
import com.track.entity.CommonSql;
import com.track.entity.SysUser;
import com.track.service.CommonSqlService;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 常用 SQL 管理控制器（REST API 入口）。
 * <p>
 * 提供当前用户常用 SQL 的增删改查，按用户与数据源隔离。
 * </p>
 */
@RestController
@RequestMapping("/api/commonSql")
@RequiredArgsConstructor
public class CommonSqlController {

    private final CommonSqlService commonSqlService;

    /**
     * 获取当前登录用户的常用 SQL 列表，可按数据源 ID 筛选，按更新时间倒序。
     *
     * @param currentUser  当前用户（由拦截器注入）
     * @param datasourceId 数据源 ID（可选，不传则查全部）
     * @return 常用 SQL 列表
     */
    @GetMapping("/list")
    public Result<List<CommonSql>> list(@RequestAttribute("currentUser") SysUser currentUser,
                                        @RequestParam(required = false) Long datasourceId) {
        LambdaQueryWrapper<CommonSql> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(CommonSql::getUserId, currentUser.getId());
        if (datasourceId != null) {
            wrapper.eq(CommonSql::getDatasourceId, datasourceId);
        }
        wrapper.orderByDesc(CommonSql::getUpdateTime);
        return Result.success(commonSqlService.list(wrapper));
    }

    /**
     * 新增一条常用 SQL（归属当前用户）。
     *
     * @param request     标题、SQL 文本、描述、数据源 ID
     * @param currentUser 当前用户（由拦截器注入）
     * @return 新增记录的 ID
     */
    @PostMapping
    public Result<Long> create(@RequestBody CommonSqlRequest request,
                               @RequestAttribute("currentUser") SysUser currentUser) {
        CommonSql entity = new CommonSql();
        entity.setUserId(currentUser.getId());
        entity.setDatasourceId(request.getDatasourceId());
        entity.setTitle(request.getTitle());
        entity.setSqlText(request.getSqlText());
        entity.setDescription(request.getDescription());
        commonSqlService.save(entity);
        return Result.success(entity.getId());
    }

    /**
     * 修改常用 SQL（仅允许修改本人记录）。
     *
     * @param id          常用 SQL ID
     * @param request     可更新的字段：标题、SQL 文本、描述、数据源 ID
     * @param currentUser 当前用户（由拦截器注入）
     * @return 无内容
     */
    @PutMapping("/{id}")
    public Result<Void> update(@PathVariable Long id,
                               @RequestBody CommonSqlRequest request,
                               @RequestAttribute("currentUser") SysUser currentUser) {
        CommonSql entity = commonSqlService.getById(id);
        if (entity == null || !entity.getUserId().equals(currentUser.getId())) {
            throw new IllegalArgumentException("常用 SQL 不存在或无权限");
        }
        if (request.getDatasourceId() != null) {
            entity.setDatasourceId(request.getDatasourceId());
        }
        entity.setTitle(request.getTitle());
        entity.setSqlText(request.getSqlText());
        entity.setDescription(request.getDescription());
        commonSqlService.updateById(entity);
        return Result.success(null);
    }

    /**
     * 删除常用 SQL（仅允许删除本人记录）。
     *
     * @param id          常用 SQL ID
     * @param currentUser 当前用户（由拦截器注入）
     * @return 无内容
     */
    @DeleteMapping("/{id}")
    public Result<Void> delete(@PathVariable Long id,
                               @RequestAttribute("currentUser") SysUser currentUser) {
        CommonSql entity = commonSqlService.getById(id);
        if (entity != null && entity.getUserId().equals(currentUser.getId())) {
            commonSqlService.removeById(id);
        }
        return Result.success(null);
    }

    /** 常用 SQL 新增/修改请求体 */
    @Data
    public static class CommonSqlRequest {
        private Long datasourceId;
        private String title;
        private String sqlText;
        private String description;
    }
}

