package com.mogu.data.feature.controller;

import com.mogu.data.common.result.Result;
import com.mogu.data.feature.entity.FeatureDefinition;
import com.mogu.data.feature.entity.FeatureView;
import com.mogu.data.feature.service.FeatureComputeService;
import com.mogu.data.feature.service.FeatureRegistryService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

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
    public Result<List<FeatureView>> listFeatureViews() {
        List<FeatureView> featureViews = featureRegistryService.listFeatureViews();
        return Result.success(featureViews);
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
                                         @RequestParam String inputPath,
                                         @RequestParam String outputPath) {
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
     * 物化特征到Redis
     */
    @PostMapping("/materialize/{featureViewName}")
    public Result<Void> materializeFeatures(@PathVariable String featureViewName) {
        featureComputeService.materializeFeatures(featureViewName);
        return Result.success();
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
}