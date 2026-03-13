package com.track.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.track.common.Result;
import com.track.entity.DatasourceConfig;
import com.track.service.DatasourceConfigService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 数据源管理控制器（REST API 入口）。
 * <p>
 * 提供数据源 CRUD、测试连接等接口。
 * </p>
 */
@RestController
@RequestMapping("/api/datasource")
@RequiredArgsConstructor
public class DatasourceController {

    private final DatasourceConfigService datasourceConfigService;

    /**
     * 获取全部数据源列表（不分页）。
     *
     * @return 数据源列表
     */
    @GetMapping("/list")
    public Result<List<DatasourceConfig>> list() {
        return Result.success(datasourceConfigService.list());
    }

    /**
     * 分页查询数据源，支持按名称模糊搜索。
     *
     * @param pageNum  页码，默认 1
     * @param pageSize 每页条数，默认 10
     * @param name     数据源名称（可选，模糊匹配）
     * @return 分页结果
     */
    @GetMapping("/page")
    public Result<Page<DatasourceConfig>> page(
            @RequestParam(defaultValue = "1") Integer pageNum,
            @RequestParam(defaultValue = "10") Integer pageSize,
            @RequestParam(required = false) String name) {
        LambdaQueryWrapper<DatasourceConfig> wrapper = new LambdaQueryWrapper<>();
        if (name != null && !name.trim().isEmpty()) {
            wrapper.like(DatasourceConfig::getName, name);
        }
        return Result.success(datasourceConfigService.page(new Page<>(pageNum, pageSize), wrapper));
    }

    /**
     * 根据 ID 获取单个数据源详情。
     *
     * @param id 数据源 ID
     * @return 数据源配置
     */
    @GetMapping("/{id}")
    public Result<DatasourceConfig> getById(@PathVariable Long id) {
        return Result.success(datasourceConfigService.getById(id));
    }

    /**
     * 新增数据源配置。
     *
     * @param config 数据源配置（JSON 请求体）
     * @return 新增后的数据源 ID
     */
    @PostMapping
    public Result<Long> save(@RequestBody DatasourceConfig config) {
        datasourceConfigService.save(config);
        return Result.success(config.getId());
    }

    /**
     * 更新数据源配置。
     *
     * @param config 数据源配置（需包含 id）
     * @return 无内容
     */
    @PutMapping
    public Result<Void> update(@RequestBody DatasourceConfig config) {
        datasourceConfigService.updateById(config);
        return Result.success(null);
    }

    /**
     * 根据 ID 删除数据源。
     *
     * @param id 数据源 ID
     * @return 无内容
     */
    @DeleteMapping("/{id}")
    public Result<Void> delete(@PathVariable Long id) {
        datasourceConfigService.removeById(id);
        return Result.success(null);
    }

    /**
     * 测试指定数据源连接是否可用。
     *
     * @param id 数据源 ID
     * @return 连接是否成功
     */
    @PostMapping("/test/{id}")
    public Result<Boolean> test(@PathVariable Long id) {
        return Result.success(datasourceConfigService.testConnection(id));
    }
}
