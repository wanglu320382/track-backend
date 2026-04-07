package com.track.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.track.common.Result;
import com.track.entity.OptimizationSuggestion;
import com.track.entity.SysUser;
import com.track.service.OptimizationSuggestionService;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

/**
 * 优化建议
 */
@RestController
@RequestMapping("/api/optimizationSuggestion")
@RequiredArgsConstructor
public class OptimizationSuggestionController {

    private static final String DEFAULT_STATUS = "审核中";
    /** Java 8 无 Set.of，使用不可变集合等价实现 */
    private static final Set<String> ALLOWED_STATUS = Collections.unmodifiableSet(
            new HashSet<>(Arrays.asList("审核中", "开发中", "已上线", "不优化")));

    private final OptimizationSuggestionService optimizationSuggestionService;

    @GetMapping("/page")
    public Result<Page<OptimizationSuggestion>> page(
            @RequestAttribute("currentUser") SysUser currentUser,
            @RequestParam(defaultValue = "1") Integer pageNum,
            @RequestParam(defaultValue = "10") Integer pageSize,
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) String status
    ) {
        int safePageNum = pageNum == null ? 1 : Math.max(pageNum, 1);
        int safePageSize = pageSize == null ? 10 : Math.min(Math.max(pageSize, 1), 100);

        LambdaQueryWrapper<OptimizationSuggestion> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(OptimizationSuggestion::getUserId, currentUser.getId());

        if (status != null && !status.trim().isEmpty()) {
            if (!ALLOWED_STATUS.contains(status.trim())) {
                throw new IllegalArgumentException("状态不合法");
            }
            wrapper.eq(OptimizationSuggestion::getStatus, status.trim());
        }

        if (keyword != null && !keyword.trim().isEmpty()) {
            String kw = keyword.trim();
            wrapper.and(w -> w.like(OptimizationSuggestion::getTitle, kw)
                    .or().like(OptimizationSuggestion::getPainPoint, kw)
                    .or().like(OptimizationSuggestion::getImprovementSuggestion, kw)
                    .or().like(OptimizationSuggestion::getProposer, kw));
        }

        wrapper.orderByDesc(OptimizationSuggestion::getCreateTime);
        return Result.success(optimizationSuggestionService.page(new Page<>(safePageNum, safePageSize), wrapper));
    }

    @PostMapping
    public Result<Long> create(@RequestBody OptimizationSuggestionRequest request,
                               @RequestAttribute("currentUser") SysUser currentUser) {
        String title = request.getTitle() == null ? "" : request.getTitle().trim();
        String pain = request.getPainPoint() == null ? "" : request.getPainPoint().trim();
        String improve = request.getImprovementSuggestion() == null ? "" : request.getImprovementSuggestion().trim();
        String proposer = request.getProposer() == null ? "" : request.getProposer().trim();
        if (title.isEmpty() || pain.isEmpty() || improve.isEmpty() || proposer.isEmpty()) {
            throw new IllegalArgumentException("请填写标题、痛点、改善建议与提出人");
        }

        OptimizationSuggestion entity = new OptimizationSuggestion();
        entity.setUserId(currentUser.getId());
        entity.setTitle(title);
        entity.setPainPoint(pain);
        entity.setImprovementSuggestion(improve);
        entity.setProposer(proposer);
        entity.setStatus(DEFAULT_STATUS);
        optimizationSuggestionService.save(entity);
        return Result.success(entity.getId());
    }

    @PutMapping("/{id}")
    public Result<Void> update(@PathVariable Long id,
                               @RequestBody OptimizationSuggestionRequest request,
                               @RequestAttribute("currentUser") SysUser currentUser) {
        OptimizationSuggestion entity = optimizationSuggestionService.getById(id);
        if (entity == null || !entity.getUserId().equals(currentUser.getId())) {
            throw new IllegalArgumentException("记录不存在或无权限");
        }

        String title = request.getTitle() == null ? "" : request.getTitle().trim();
        String pain = request.getPainPoint() == null ? "" : request.getPainPoint().trim();
        String improve = request.getImprovementSuggestion() == null ? "" : request.getImprovementSuggestion().trim();
        String proposer = request.getProposer() == null ? "" : request.getProposer().trim();
        if (title.isEmpty() || pain.isEmpty() || improve.isEmpty() || proposer.isEmpty()) {
            throw new IllegalArgumentException("请填写标题、痛点、改善建议与提出人");
        }
        String st = request.getStatus() == null ? "" : request.getStatus().trim();
        if (st.isEmpty() || !ALLOWED_STATUS.contains(st)) {
            throw new IllegalArgumentException("状态不合法");
        }

        entity.setTitle(title);
        entity.setPainPoint(pain);
        entity.setImprovementSuggestion(improve);
        entity.setProposer(proposer);
        entity.setStatus(st);
        optimizationSuggestionService.updateById(entity);
        return Result.success(null);
    }

    @DeleteMapping("/{id}")
    public Result<Void> delete(@PathVariable Long id,
                               @RequestAttribute("currentUser") SysUser currentUser) {
        OptimizationSuggestion entity = optimizationSuggestionService.getById(id);
        if (entity != null && entity.getUserId().equals(currentUser.getId())) {
            optimizationSuggestionService.removeById(id);
        }
        return Result.success(null);
    }

    @Data
    public static class OptimizationSuggestionRequest {
        private String title;
        private String painPoint;
        private String improvementSuggestion;
        private String proposer;
        /** 新建时忽略，仅更新时可改 */
        private String status;
    }
}
