package com.mogu.data.serving.service;

import com.mogu.data.common.exception.BusinessException;
import com.mogu.data.common.serving.ModelPredictor;
import com.mogu.data.serving.entity.PredictionRequest;
import com.mogu.data.serving.entity.PredictionResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 预测服务
 * 负责模型推理和结果返回
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class PredictorService {

    private final ModelLoaderService modelLoaderService;
    private final FeatureClientService featureClientService;

    // 预测器映射（按框架类型）
    private final java.util.Map<String, ModelPredictor> predictorMap;

    /**
     * 单条预测
     * @param request 预测请求
     * @return 预测响应
     */
    public PredictionResponse predict(PredictionRequest request) {
        long startTime = System.currentTimeMillis();

        log.info("开始预测: model={}, entityType={}, entityId={}",
                request.getModelName(), request.getEntityType(), request.getEntityId());

        try {
            // 1. 加载模型
            Object model;
            if (request.getModelVersion() != null) {
                model = modelLoaderService.loadModel(request.getModelName(), request.getModelVersion());
            } else {
                model = modelLoaderService.loadProductionModel(request.getModelName());
            }

            // 2. 获取特征
            Map<String, Object> features;
            if (request.getFeatures() != null && !request.getFeatures().isEmpty()) {
                // 使用请求中的特征
                features = request.getFeatures();
            } else {
                // 从在线特征存储获取
                features = featureClientService.getOnlineFeatures(
                        request.getEntityType(),
                        request.getEntityId(),
                        Collections.emptyList() // 这里需要根据模型获取所需的特征列表
                );
            }

            // 3. 预测
            List<Double> predictions = getPredictor(request.getModelName()).predict(model, features);
            Double prediction = predictions.get(0);

            // 4. 构建响应
            long endTime = System.currentTimeMillis();
            PredictionResponse response = new PredictionResponse();
            response.setEntityType(request.getEntityType());
            response.setEntityId(request.getEntityId());
            response.setPrediction(prediction);
            response.setLatency(endTime - startTime);
            response.setTimestamp(Instant.now().toEpochMilli());

            // 模型信息
            PredictionResponse.ModelInfo modelInfo = new PredictionResponse.ModelInfo();
            modelInfo.setModelName(request.getModelName());
            modelInfo.setModelVersion(request.getModelVersion());
            modelInfo.setModelType("unknown"); // 这里需要从模型配置中获取
            response.setModelInfo(modelInfo);

            // 特征信息
            if (request.getIncludeDetails()) {
                PredictionResponse.FeatureInfo featureInfo = new PredictionResponse.FeatureInfo();
                featureInfo.setFeatures(features);
                featureInfo.setFeatureCount(features.size());
                featureInfo.setFeatureSources(new HashMap<>());
                response.setFeatureInfo(featureInfo);
            }

            log.info("预测完成: prediction={}, latency={}ms", prediction, response.getLatency());
            return response;

        } catch (Exception e) {
            log.error("预测失败: {}", e.getMessage(), e);
            throw new BusinessException("预测失败: " + e.getMessage());
        }
    }

    /**
     * 批量预测
     * @param requests 预测请求列表
     * @return 预测响应列表
     */
    public List<PredictionResponse> batchPredict(List<PredictionRequest> requests) {
        log.info("开始批量预测, 数量: {}", requests.size());

        try {
            return requests.stream()
                    .map(this::predict)
                    .collect(java.util.stream.Collectors.toList());

        } catch (Exception e) {
            log.error("批量预测失败: {}", e.getMessage(), e);
            throw new BusinessException("批量预测失败: " + e.getMessage());
        }
    }

    /**
     * A/B测试预测
     * @param modelNameA 模型A名称
     * @param modelNameB 模型B名称
     * @param trafficRatio 流量分配比例（模型A的比例）
     * @param request 预测请求
     * @return 预测响应
     */
    public PredictionResponse abTestPredict(String modelNameA, String modelNameB, double trafficRatio, PredictionRequest request) {
        log.info("A/B测试预测: modelA={}, modelB={}, ratio={}", modelNameA, modelNameB, trafficRatio);

        try {
            // 根据流量分配选择模型
            String selectedModel;
            if (Math.random() < trafficRatio) {
                selectedModel = modelNameA;
                log.debug("选择模型A: {}", modelNameA);
            } else {
                selectedModel = modelNameB;
                log.debug("选择模型B: {}", modelNameB);
            }

            // 设置模型名称并预测
            request.setModelName(selectedModel);
            return predict(request);

        } catch (Exception e) {
            log.error("A/B测试预测失败: {}", e.getMessage(), e);
            throw new BusinessException("A/B测试预测失败: " + e.getMessage());
        }
    }

    /**
     * 获取预测器
     */
    private ModelPredictor getPredictor(String modelName) {
        // 根据模型名称推断框架类型，实际应从模型元数据获取
        String frameworkType = modelName.toLowerCase().contains("xgb") ? "xgboost" : "lightgbm";
        String predictorName = frameworkType.equals("xgboost") ? "xgBoostPredictor" : "lightGBMPredictor";
        ModelPredictor predictor = predictorMap.get(predictorName);
        if (predictor == null) {
            throw new BusinessException("找不到对应的预测器: " + predictorName);
        }
        return predictor;
    }

    /**
     * 获取模型版本信息
     * @param modelName 模型名称
     * @return 版本信息
     */
    public Map<String, Object> getModelVersionInfo(String modelName) {
        log.info("获取模型版本信息: {}", modelName);

        try {
            Map<String, Object> versionInfo = new HashMap<>();
            versionInfo.put("modelName", modelName);
            versionInfo.put("cacheInfo", modelLoaderService.getModelCacheInfo());

            return versionInfo;

        } catch (Exception e) {
            log.error("获取模型版本信息失败: {}", e.getMessage(), e);
            throw new BusinessException("获取模型版本信息失败: " + e.getMessage());
        }
    }

    /**
     * 健康检查
     * @return 健康状态
     */
    public Map<String, Object> healthCheck() {
        Map<String, Object> health = new HashMap<>();
        health.put("status", "healthy");
        health.put("modelCache", modelLoaderService.getModelCacheInfo());
        health.put("timestamp", Instant.now().toEpochMilli());
        return health;
    }
}