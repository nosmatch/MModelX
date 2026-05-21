package com.mogu.data.serving.service;

import com.mogu.data.common.exception.BusinessException;
import com.mogu.data.common.storage.RedisService;
import com.mogu.data.feature.service.FeatureComputeService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 在线特征获取客户端
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class FeatureClientService {

    private final RedisService redisService;
    private final FeatureComputeService featureComputeService;

    /**
     * 获取在线特征
     * @param entityType 实体类型
     * @param entityId 实体ID
     * @param featureNames 特征名称列表
     * @return 特征值Map
     */
    public Map<String, Object> getOnlineFeatures(String entityType, String entityId, List<String> featureNames) {
        log.info("获取在线特征: entityType={}, entityId={}, features={}",
                entityType, entityId, featureNames);

        try {
            // 从Redis获取在线特征
            Map<String, Object> features = featureComputeService.getOnlineFeatures(entityType, entityId, featureNames);

            // 检查是否有缺失的特征
            List<String> missingFeatures = featureNames.stream()
                    .filter(name -> !features.containsKey(name))
                    .collect(java.util.stream.Collectors.toList());

            if (!missingFeatures.isEmpty()) {
                log.warn("缺失的特征: {}", missingFeatures);
                // 可以触发特征计算或使用默认值
            }

            return features;

        } catch (Exception e) {
            log.error("获取在线特征失败: {}", e.getMessage(), e);
            throw new BusinessException("获取在线特征失败: " + e.getMessage());
        }
    }

    /**
     * 批量获取在线特征
     * @param entityType 实体类型
     * @param entityIds 实体ID列表
     * @param featureNames 特征名称列表
     * @return 特征值Map（key为entityId，value为特征Map）
     */
    public Map<String, Map<String, Object>> batchGetOnlineFeatures(String entityType, List<String> entityIds, List<String> featureNames) {
        log.info("批量获取在线特征: entityType={}, count={}, features={}",
                entityType, entityIds.size(), featureNames.size());

        try {
            Map<String, Map<String, Object>> batchFeatures = new HashMap<>();

            for (String entityId : entityIds) {
                Map<String, Object> features = getOnlineFeatures(entityType, entityId, featureNames);
                batchFeatures.put(entityId, features);
            }

            return batchFeatures;

        } catch (Exception e) {
            log.error("批量获取在线特征失败: {}", e.getMessage(), e);
            throw new BusinessException("批量获取在线特征失败: " + e.getMessage());
        }
    }

    /**
     * 获取特征并填充默认值
     * @param entityType 实体类型
     * @param entityId 实体ID
     * @param featureNames 特征名称列表
     * @param defaultValues 默认值Map
     * @return 特征值Map
     */
    public Map<String, Object> getOnlineFeaturesWithDefaults(String entityType, String entityId, List<String> featureNames, Map<String, Object> defaultValues) {
        log.info("获取在线特征（带默认值）: entityType={}, entityId={}", entityType, entityId);

        try {
            Map<String, Object> features = getOnlineFeatures(entityType, entityId, featureNames);

            // 填充默认值
            for (Map.Entry<String, Object> entry : defaultValues.entrySet()) {
                if (!features.containsKey(entry.getKey())) {
                    features.put(entry.getKey(), entry.getValue());
                }
            }

            return features;

        } catch (Exception e) {
            log.error("获取在线特征失败: {}", e.getMessage(), e);
            throw new BusinessException("获取在线特征失败: " + e.getMessage());
        }
    }

    /**
     * 刷新特征缓存
     * @param entityType 实体类型
     * @param entityId 实体ID
     * @param featureNames 特征名称列表
     */
    public void refreshFeatureCache(String entityType, String entityId, List<String> featureNames) {
        log.info("刷新特征缓存: entityType={}, entityId={}", entityType, entityId);

        try {
            // 删除缓存的特征
            String featurePrefix = "feature:" + entityType + ":" + entityId + ":";
            for (String featureName : featureNames) {
                String key = featurePrefix + featureName;
                redisService.delete(key);
            }

            log.info("特征缓存刷新完成");

        } catch (Exception e) {
            log.error("刷新特征缓存失败: {}", e.getMessage(), e);
            throw new BusinessException("刷新特征缓存失败: " + e.getMessage());
        }
    }

    /**
     * 预热特征缓存
     * @param entityType 实体类型
     * @param entityIds 实体ID列表
     * @param featureNames 特征名称列表
     */
    public void warmUpFeatureCache(String entityType, List<String> entityIds, List<String> featureNames) {
        log.info("预热特征缓存: entityType={}, count={}", entityType, entityIds.size());

        try {
            // 批量获取特征以预热缓存
            batchGetOnlineFeatures(entityType, entityIds, featureNames);

            log.info("特征缓存预热完成");

        } catch (Exception e) {
            log.error("预热特征缓存失败: {}", e.getMessage(), e);
            throw new BusinessException("预热特征缓存失败: " + e.getMessage());
        }
    }
}