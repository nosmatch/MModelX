package com.mogu.data.serving.controller;

import com.mogu.data.common.result.Result;
import com.mogu.data.serving.entity.PredictionRequest;
import com.mogu.data.serving.entity.PredictionResponse;
import com.mogu.data.serving.service.ModelLoaderService;
import com.mogu.data.serving.service.PredictorService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

/**
 * 推理服务控制器
 */
@Slf4j
@RestController
@RequestMapping("/api/serving")
@RequiredArgsConstructor
public class ServingController {

    private final PredictorService predictorService;
    private final ModelLoaderService modelLoaderService;

    /**
     * 单条预测
     */
    @PostMapping("/predict")
    public Result<PredictionResponse> predict(@RequestBody PredictionRequest request) {
        log.info("收到预测请求: {}", request);

        PredictionResponse response = predictorService.predict(request);
        return Result.success(response);
    }

    /**
     * 批量预测
     */
    @PostMapping("/predict/batch")
    public Result<List<PredictionResponse>> batchPredict(@RequestBody List<PredictionRequest> requests) {
        log.info("收到批量预测请求, 数量: {}", requests.size());

        List<PredictionResponse> responses = predictorService.batchPredict(requests);
        return Result.success(responses);
    }

    /**
     * 快速预测（简化版）
     */
    @GetMapping("/predict")
    public Result<PredictionResponse> quickPredict(@RequestParam String modelName,
                                                    @RequestParam String entityType,
                                                    @RequestParam String entityId) {
        log.info("收到快速预测请求: model={}, entityType={}, entityId={}",
                modelName, entityType, entityId);

        PredictionRequest request = new PredictionRequest();
        request.setModelName(modelName);
        request.setEntityType(entityType);
        request.setEntityId(entityId);
        request.setIncludeDetails(false);

        PredictionResponse response = predictorService.predict(request);
        return Result.success(response);
    }

    /**
     * A/B测试预测
     */
    @PostMapping("/predict/abtest")
    public Result<PredictionResponse> abTestPredict(@RequestParam String modelA,
                                                     @RequestParam String modelB,
                                                     @RequestParam(defaultValue = "0.5") double ratio,
                                                     @RequestBody PredictionRequest request) {
        log.info("收到A/B测试预测请求: modelA={}, modelB={}, ratio={}", modelA, modelB, ratio);

        PredictionResponse response = predictorService.abTestPredict(modelA, modelB, ratio, request);
        return Result.success(response);
    }

    /**
     * 获取模型版本信息
     */
    @GetMapping("/models/{modelName}/info")
    public Result<Map<String, Object>> getModelVersionInfo(@PathVariable String modelName) {
        Map<String, Object> info = predictorService.getModelVersionInfo(modelName);
        return Result.success(info);
    }

    /**
     * 热加载模型
     */
    @PostMapping("/models/{modelName}/{version}/reload")
    public Result<Void> reloadModel(@PathVariable String modelName, @PathVariable String version) {
        modelLoaderService.hotReloadModel(modelName, version);
        return Result.success();
    }

    /**
     * 卸载模型
     */
    @DeleteMapping("/models/{modelName}/{version}")
    public Result<Void> unloadModel(@PathVariable String modelName, @PathVariable String version) {
        modelLoaderService.unloadModel(modelName, version);
        return Result.success();
    }

    /**
     * 清空模型缓存
     */
    @DeleteMapping("/models/cache")
    public Result<Void> clearModelCache() {
        modelLoaderService.clearModelCache();
        return Result.success();
    }

    /**
     * 健康检查
     */
    @GetMapping("/health")
    public Result<Map<String, Object>> healthCheck() {
        Map<String, Object> health = predictorService.healthCheck();
        return Result.success(health);
    }

    /**
     * 服务状态
     */
    @GetMapping("/status")
    public Result<Map<String, Object>> getStatus() {
        Map<String, Object> status = new java.util.HashMap<>();
        status.put("service", "MModelX Serving Service");
        status.put("version", "1.0.0");
        status.put("status", "running");
        status.put("modelCache", modelLoaderService.getModelCacheInfo());
        status.put("timestamp", System.currentTimeMillis());
        return Result.success(status);
    }
}