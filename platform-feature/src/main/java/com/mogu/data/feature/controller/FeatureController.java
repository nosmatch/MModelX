package com.mogu.data.feature.controller;

import com.mogu.data.common.result.Result;
import com.mogu.data.feature.entity.FeatureDefinition;
import com.mogu.data.feature.entity.FeatureView;
import com.mogu.data.feature.service.FeatureComputeService;
import com.mogu.data.feature.service.FeatureRegistryService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 特征工程控制器
 */
@Slf4j
@RestController
@RequestMapping("/api/v1/features")
@RequiredArgsConstructor
public class FeatureController {

    private final FeatureRegistryService featureRegistryService;
    private final FeatureComputeService featureComputeService;

    /**
     * 注册特征视图
     */
    @PostMapping("/views")
    public Result<Long> registerFeatureView(@RequestBody FeatureView featureView) {
        Long id = featureRegistryService.registerFeatureView(featureView);
        return Result.success(id);
    }

    /**
     * 获取特征视图
     */
    @GetMapping("/views/{name}")
    public Result<FeatureView> getFeatureView(@PathVariable String name) {
        FeatureView featureView = featureRegistryService.getFeatureView(name);
        return Result.success(featureView);
    }

    /**
     * 列出所有特征视图
     */
    @GetMapping("/views")
    public Result<Map<String, Object>> listFeatureViews() {
        List<FeatureView> featureViews = featureRegistryService.listFeatureViews();
        Map<String, Object> result = new HashMap<>();
        result.put("items", featureViews);
        result.put("total", featureViews.size());
        return Result.success(result);
    }

    /**
     * 更新特征视图
     */
    @PutMapping("/views/{name}")
    public Result<Void> updateFeatureView(@PathVariable String name, @RequestBody FeatureView featureView) {
        featureRegistryService.updateFeatureView(name, featureView);
        return Result.success();
    }

    /**
     * 删除特征视图
     */
    @DeleteMapping("/views/{name}")
    public Result<Void> deleteFeatureView(@PathVariable String name) {
        featureRegistryService.deleteFeatureView(name);
        return Result.success();
    }

    /**
     * 注册特征定义
     */
    @PostMapping("/definitions")
    public Result<Void> registerFeatureDefinition(@RequestBody FeatureDefinition definition) {
        featureRegistryService.registerFeatureDefinition(definition);
        return Result.success();
    }

    /**
     * 计算特征
     */
    @PostMapping("/compute")
    public Result<Void> computeFeatures(@RequestBody FeatureDefinition definition,
                                        @RequestParam(required = false) String inputPath,
                                        @RequestParam(required = false) String outputPath) {
        featureComputeService.computeFeatures(definition, inputPath, outputPath);
        return Result.success();
    }

    /**
     * 批量计算特征
     */
    @PostMapping("/compute/batch")
    public Result<Void> batchComputeFeatures(@RequestBody List<FeatureDefinition> definitions) {
        featureComputeService.batchComputeFeatures(definitions);
        return Result.success();
    }

    /**
     * 获取特征视图的特征定义列表
     */
    @GetMapping("/views/{name}/definitions")
    public Result<List<com.mogu.data.feature.entity.FeatureDefinition.FeatureSpec>> getFeatureDefinitions(
            @PathVariable String name) {
        List<com.mogu.data.feature.entity.FeatureDefinition.FeatureSpec> definitions =
                featureRegistryService.getFeatureDefinitions(name);
        return Result.success(definitions);
    }

    /**
     * 更新特征定义
     */
    @PutMapping("/views/{name}/definitions/{featureName}")
    public Result<Void> updateFeatureDefinition(
            @PathVariable String name,
            @PathVariable String featureName,
            @RequestBody com.mogu.data.feature.entity.FeatureDefinition.FeatureSpec spec) {
        featureRegistryService.updateFeatureDefinition(name, featureName, spec);
        return Result.success();
    }

    /**
     * 删除特征定义
     */
    @DeleteMapping("/views/{name}/definitions/{featureName}")
    public Result<Void> deleteFeatureDefinition(
            @PathVariable String name,
            @PathVariable String featureName) {
        featureRegistryService.deleteFeatureDefinition(name, featureName);
        return Result.success();
    }

    /**
     * 物化特征到Redis
     */
    @PostMapping("/materialize/{featureViewName}")
    public Result<Void> materializeFeatures(@PathVariable String featureViewName) {
        featureComputeService.materializeFeatures(featureViewName);
        return Result.success();
    }

    /**
     * 预览特征数据（从MinIO读取）
     */
    @GetMapping("/preview/{featureViewName}")
    public Result<List<Map<String, Object>>> previewFeatures(
            @PathVariable String featureViewName,
            @RequestParam(defaultValue = "10") int limit) {
        List<Map<String, Object>> preview = featureComputeService.previewFeatures(featureViewName, limit);
        return Result.success(preview);
    }

    /**
     * 获取在线特征
     */
    @GetMapping("/online")
    public Result<Map<String, Object>> getOnlineFeatures(@RequestParam String entityType,
                                                         @RequestParam String entityId,
                                                         @RequestParam List<String> featureNames) {
        Map<String, Object> features = featureComputeService.getOnlineFeatures(entityType, entityId, featureNames);
        return Result.success(features);
    }

    /**
     * 获取Redis状态
     */
    @GetMapping("/redis/status")
    public Result<Map<String, Object>> getRedisStatus() {
        Map<String, Object> status = featureComputeService.getRedisStatus();
        return Result.success(status);
    }

    /**
     * 搜索Redis Keys
     */
    @GetMapping("/redis/keys")
    public Result<List<String>> searchRedisKeys(@RequestParam String pattern) {
        List<String> keys = featureComputeService.searchRedisKeys(pattern);
        return Result.success(keys);
    }

    /**
     * 获取Redis Key的值
     */
    @GetMapping("/redis/keys/value")
    public Result<Object> getRedisKeyValue(@RequestParam String key) {
        Object value = featureComputeService.getRedisKeyValue(key);
        return Result.success(value);
    }

    /**
     * 获取物化历史
     */
    @GetMapping("/materialize/history")
    public Result<List<com.mogu.data.common.entity.MaterializationHistory>> getMaterializeHistory(
            @RequestParam(required = false) String featureViewName) {
        List<com.mogu.data.common.entity.MaterializationHistory> history =
                featureComputeService.getMaterializeHistory(featureViewName);
        return Result.success(history);
    }
}