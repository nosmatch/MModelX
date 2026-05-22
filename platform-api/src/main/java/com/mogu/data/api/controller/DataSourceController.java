package com.mogu.data.api.controller;

import com.mogu.data.common.entity.DataSource;
import com.mogu.data.common.entity.FeatureView;
import com.mogu.data.common.result.Result;
import com.mogu.data.common.service.DataSourceService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

import javax.validation.Valid;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * 数据源管理 Controller
 *
 * 提供数据源的 REST API 接口：
 * - CRUD 操作
 * - 连接测试
 * - 使用统计
 *
 * @author MModelX Team
 * @since 2026-05-21
 */
@RestController
@RequestMapping("/api/v1/datasources")
@RequiredArgsConstructor
@Slf4j
public class DataSourceController {

    private final DataSourceService dataSourceService;

    /**
     * 获取所有数据源
     *
     * GET /api/v1/datasources
     *
     * @return 所有激活状态的数据源列表
     */
    @GetMapping
    public Result<List<DataSource>> listDataSources() {
        log.info("Listing all datasources");
        List<DataSource> datasources = dataSourceService.getActiveDataSources();
        return Result.success(datasources);
    }

    /**
     * 根据类型获取数据源
     *
     * GET /api/v1/datasources/type/{type}
     *
     * @param type 数据源类型（postgresql, mysql, redis, kafka, minio, api, local_file）
     * @return 该类型的所有数据源
     */
    @GetMapping("/type/{type}")
    public Result<List<DataSource>> getDataSourcesByType(@PathVariable String type) {
        log.info("Listing datasources by type: {}", type);
        List<DataSource> datasources = dataSourceService.getDataSourcesByType(type);
        return Result.success(datasources);
    }

    /**
     * 搜索数据源
     *
     * GET /api/v1/datasources/search?keyword=xxx
     *
     * @param keyword 搜索关键词
     * @return 匹配的数据源列表
     */
    @GetMapping("/search")
    public Result<List<DataSource>> searchDataSources(@RequestParam String keyword) {
        log.info("Searching datasources with keyword: {}", keyword);
        List<DataSource> datasources = dataSourceService.searchDataSources(keyword);
        return Result.success(datasources);
    }

    /**
     * 获取数据源详情
     *
     * GET /api/v1/datasources/{id}
     *
     * @param id 数据源ID
     * @return 数据源详情（不包含密码）
     */
    @GetMapping("/{id}")
    public Result<DataSource> getDataSource(@PathVariable Long id) {
        log.info("Getting datasource detail: {}", id);
        DataSource datasource = dataSourceService.getDataSource(id);

        // 不返回密码给前端
        datasource.setPasswordEncrypted(null);

        return Result.success(datasource);
    }

    /**
     * 创建数据源
     *
     * POST /api/v1/datasources
     *
     * @param dataSource 数据源对象
     * @return 创建后的数据源ID
     */
    @PostMapping
    public Result<Long> createDataSource(@Valid @RequestBody DataSource dataSource) {
        log.info("Creating datasource: {}", dataSource.getName());
        Long id = dataSourceService.createDataSource(dataSource);
        return Result.success(id, "数据源创建成功");
    }

    /**
     * 更新数据源
     *
     * PUT /api/v1/datasources/{id}
     *
     * @param id 数据源ID
     * @param updates 更新的数据
     * @return 成功消息
     */
    @PutMapping("/{id}")
    public Result<Void> updateDataSource(
        @PathVariable Long id,
        @Valid @RequestBody DataSource updates
    ) {
        log.info("Updating datasource: {}", id);
        dataSourceService.updateDataSource(id, updates);
        return Result.<Void>success(null, "数据源更新成功");
    }

    /**
     * 删除数据源
     *
     * DELETE /api/v1/datasources/{id}
     *
     * @param id 数据源ID
     * @return 成功消息
     */
    @DeleteMapping("/{id}")
    public Result<Void> deleteDataSource(@PathVariable Long id) {
        log.info("Deleting datasource: {}", id);
        dataSourceService.deleteDataSource(id);
        return Result.<Void>success(null, "数据源删除成功");
    }

    /**
     * 测试数据源连接
     *
     * POST /api/v1/datasources/{id}/test
     *
     * @param id 数据源ID
     * @return 测试结果
     */
    @PostMapping("/{id}/test")
    public Result<Boolean> testConnection(@PathVariable Long id) {
        log.info("Testing connection for datasource: {}", id);
        boolean result = dataSourceService.testConnection(id);

        if (result) {
            return Result.success(result, "连接测试成功");
        } else {
            return Result.error("连接测试失败");
        }
    }

    /**
     * 启用数据源
     *
     * POST /api/v1/datasources/{id}/enable
     *
     * @param id 数据源ID
     * @return 成功消息
     */
    @PostMapping("/{id}/enable")
    public Result<Void> enableDataSource(@PathVariable Long id) {
        log.info("Enabling datasource: {}", id);
        dataSourceService.enableDataSource(id);
        return Result.<Void>success(null, "数据源已启用");
    }

    /**
     * 禁用数据源
     *
     * POST /api/v1/datasources/{id}/disable
     *
     * @param id 数据源ID
     * @return 成功消息
     */
    @PostMapping("/{id}/disable")
    public Result<Void> disableDataSource(@PathVariable Long id) {
        log.info("Disabling datasource: {}", id);
        dataSourceService.disableDataSource(id);
        return Result.<Void>success(null, "数据源已禁用");
    }

    /**
     * 获取数据源使用统计
     *
     * GET /api/v1/datasources/{id}/usage
     *
     * @param id 数据源ID
     * @return 使用该数据源的特征视图列表
     */
    @GetMapping("/{id}/usage")
    public Result<List<Map<String, Object>>> getDataSourceUsage(@PathVariable Long id) {
        log.info("Getting datasource usage: {}", id);

        long count = dataSourceService.getUsageCount(id);
        List<FeatureView> featureViews = dataSourceService.getUsedByFeatureViews(id);

        // 构建返回结果
        List<Map<String, Object>> result = featureViews.stream()
            .map(fv -> {
                try {
                    Map<String, Object> map = new HashMap<>();
                    map.put("id", fv.getId());
                    map.put("name", fv.getName());
                    map.put("entity", fv.getEntity());
                    map.put("description", fv.getDescription());
                    return map;
                } catch (Exception e) {
                    Map<String, Object> map = new HashMap<>();
                    map.put("id", fv.getId());
                    map.put("name", fv.getName());
                    return map;
                }
            })
            .collect(Collectors.toList());

        return Result.success(result, "该数据源被 " + count + " 个特征视图使用");
    }

    /**
     * 处理 BindException（数据验证错误）
     */
    @ExceptionHandler(org.springframework.validation.BindException.class)
    public Result<Void> handleBindException(org.springframework.validation.BindException ex) {
        StringBuilder errors = new StringBuilder("数据验证失败：");
        ex.getBindingResult().getFieldErrors().forEach(error -> {
            errors.append(error.getField())
                .append(" ")
                .append(error.getDefaultMessage())
                .append("; ");
        });
        return Result.error(errors.toString());
    }
}
