package com.track.controller;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.track.common.Result;
import com.track.entity.CommonStat;
import com.track.entity.SysUser;
import com.track.service.CommonStatService;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 常用查询管理控制器（REST API 入口）。
 */
@RestController
@RequestMapping("/api/commonStat")
@RequiredArgsConstructor
public class CommonStatController {

    private final CommonStatService commonStatService;

    @GetMapping("/list")
    public Result<List<CommonStat>> list(@RequestAttribute("currentUser") SysUser currentUser,
                                        @RequestParam(required = false) Long datasourceId) {
        LambdaQueryWrapper<CommonStat> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(CommonStat::getUserId, currentUser.getId());
        if (datasourceId != null) {
            wrapper.eq(CommonStat::getDatasourceId, datasourceId);
        }
        wrapper.orderByDesc(CommonStat::getUpdateTime);
        return Result.success(commonStatService.list(wrapper));
    }

    /**
     * 分页查询常用查询列表。
     *
     * @param pageNum 页码，默认 1
     * @param pageSize 每页条数，默认 10
     * @param keyword 关键字（标题/描述/语句模糊匹配）
     */
    @GetMapping("/page")
    public Result<Page<CommonStat>> page(
            @RequestAttribute("currentUser") SysUser currentUser,
            @RequestParam(defaultValue = "1") Integer pageNum,
            @RequestParam(defaultValue = "10") Integer pageSize,
            @RequestParam(required = false) Long datasourceId,
            @RequestParam(required = false) String keyword
    ) {
        int safePageNum = pageNum == null ? 1 : Math.max(pageNum, 1);
        int safePageSize = pageSize == null ? 10 : Math.min(Math.max(pageSize, 1), 100);

        LambdaQueryWrapper<CommonStat> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(CommonStat::getUserId, currentUser.getId());

        if (datasourceId != null) {
            wrapper.eq(CommonStat::getDatasourceId, datasourceId);
        }

        if (keyword != null && !keyword.trim().isEmpty()) {
            String kw = keyword.trim();
            // 把 keyword 条件放进括号，避免与 datasourceId/userId 条件的逻辑混淆
            wrapper.and(w -> w.like(CommonStat::getTitle, kw)
                    .or().like(CommonStat::getDescription, kw)
                    .or().like(CommonStat::getStatText, kw));
        }

        wrapper.orderByDesc(CommonStat::getUpdateTime);
        return Result.success(commonStatService.page(new Page<>(safePageNum, safePageSize), wrapper));
    }

    @PostMapping
    public Result<Long> create(@RequestBody CommonStatRequest request,
                               @RequestAttribute("currentUser") SysUser currentUser) {
        CommonStat entity = new CommonStat();
        entity.setUserId(currentUser.getId());
        entity.setDatasourceId(request.getDatasourceId());
        entity.setTitle(request.getTitle());
        entity.setStatText(request.getStatText());
        entity.setDescription(request.getDescription());
        commonStatService.save(entity);
        return Result.success(entity.getId());
    }

    @PutMapping("/{id}")
    public Result<Void> update(@PathVariable Long id,
                               @RequestBody CommonStatRequest request,
                               @RequestAttribute("currentUser") SysUser currentUser) {
        CommonStat entity = commonStatService.getById(id);
        if (entity == null || !entity.getUserId().equals(currentUser.getId())) {
            throw new IllegalArgumentException("记录不存在或无权限");
        }
        if (request.getDatasourceId() != null) {
            entity.setDatasourceId(request.getDatasourceId());
        }
        entity.setTitle(request.getTitle());
        entity.setStatText(request.getStatText());
        entity.setDescription(request.getDescription());
        commonStatService.updateById(entity);
        return Result.success(null);
    }

    @DeleteMapping("/{id}")
    public Result<Void> delete(@PathVariable Long id,
                               @RequestAttribute("currentUser") SysUser currentUser) {
        CommonStat entity = commonStatService.getById(id);
        if (entity != null && entity.getUserId().equals(currentUser.getId())) {
            commonStatService.removeById(id);
        }
        return Result.success(null);
    }

    @Data
    public static class CommonStatRequest {
        private Long datasourceId;
        private String title;
        private String statText;
        private String description;
    }
}
