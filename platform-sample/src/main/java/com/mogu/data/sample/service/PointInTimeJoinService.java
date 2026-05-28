package com.mogu.data.sample.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.mogu.data.common.entity.FeatureView;
import com.mogu.data.common.exception.BusinessException;
import com.mogu.data.common.logger.Logger;
import com.mogu.data.common.storage.MinioService;
import com.mogu.data.feature.entity.FeatureDefinition;
import com.mogu.data.feature.service.FeatureRegistryService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.*;

/**
 * Point-in-time Join 服务（基于已计算的特征文件）
 *
 * 核心功能：防止数据穿越（Data Leakage）
 * 从 MinIO 中读取已计算好的特征文件，按 (entity, timestamp) 组合获取该时间点之前的最新特征值。
 *
 * 特征文件路径格式：features/{featureViewName}/{dateStr}/features_{dateStr}.json
 * 与 FeatureComputeService.buildOutputPath() 保持一致。
 *
 * @author MModelX Team
 * @since 2026-05-26
 */
@Service
@RequiredArgsConstructor
public class PointInTimeJoinService {

    private static final Logger log = Logger.getLogger(PointInTimeJoinService.class);
    private static final String FEATURES_BUCKET = "features";
    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("yyyyMMdd");

    private final FeatureRegistryService featureRegistryService;
    private final MinioService minioService;
    private final ObjectMapper objectMapper;

    /**
     * 执行 Point-in-time join
     *
     * @param entityIds     实体ID列表
     * @param timestamps    时间戳列表（与 entityIds 一一对应）
     * @param featureViews  特征视图名称列表
     * @return 与输入顺序对齐的特征列表 List<Map<featureName, value>>，
     *         任一视图缺失时对应位置为 null
     */
    public List<Map<String, Object>> join(
            List<String> entityIds,
            List<String> timestamps,
            List<String> featureViews) {

        if (entityIds.size() != timestamps.size()) {
            throw new BusinessException("实体ID数量与时间戳数量不匹配");
        }

        log.info("开始 Point-in-time join, 实体数: {}, 特征视图数: {}",
                entityIds.size(), featureViews.size());

        // 初始化结果列表
        List<Map<String, Object>> result = new ArrayList<>();
        for (int i = 0; i < entityIds.size(); i++) {
            result.add(new HashMap<>());
        }

        // 记录每个位置是否有效（所有视图都有特征）
        boolean[] valid = new boolean[entityIds.size()];
        Arrays.fill(valid, true);

        // 逐个特征视图处理
        for (String viewName : featureViews) {
            List<Map<String, Object>> viewFeatures = joinSingleView(
                    entityIds, timestamps, viewName);

            for (int i = 0; i < entityIds.size(); i++) {
                if (viewFeatures.get(i) == null) {
                    valid[i] = false;
                } else if (valid[i]) {
                    result.get(i).putAll(viewFeatures.get(i));
                }
            }
        }

        // 无效位置置为 null（后续丢弃）
        int dropped = 0;
        for (int i = 0; i < entityIds.size(); i++) {
            if (!valid[i]) {
                result.set(i, null);
                dropped++;
            }
        }

        log.info("Point-in-time join 完成, 有效记录: {}, 丢弃记录: {}",
                entityIds.size() - dropped, dropped);
        return result;
    }

    /**
     * 单个特征视图的 Point-in-time join
     *
     * 根据每个 entity 的 timestamp 确定对应的分区日期，
     * 从 MinIO 读取该分区的特征文件，按 entity_id 查找特征值。
     * 返回与输入顺序一一对应的特征列表。
     */
    private List<Map<String, Object>> joinSingleView(
            List<String> entityIds,
            List<String> timestamps,
            String viewName) {

        log.debug("处理特征视图: {}", viewName);

        // 获取特征规格列表（仅用于日志和校验，不再填充默认值）
        List<FeatureDefinition.FeatureSpec> featureSpecs = getFeatureSpecs(viewName);
        if (featureSpecs.isEmpty()) {
            log.warn("特征视图 {} 没有配置特征，全部返回 null", viewName);
            List<Map<String, Object>> empty = new ArrayList<>();
            for (int i = 0; i < entityIds.size(); i++) {
                empty.add(null);
            }
            return empty;
        }

        // 按分区日期分组，避免重复读取同一个文件
        Map<String, List<Integer>> partitionToIndices = new HashMap<>();
        for (int i = 0; i < entityIds.size(); i++) {
            String timestamp = timestamps.get(i);
            String partitionDate = parsePartitionDate(timestamp);
            partitionToIndices.computeIfAbsent(partitionDate, k -> new ArrayList<>()).add(i);
        }

        // 初始化结果列表，与输入顺序一一对应
        List<Map<String, Object>> result = new ArrayList<>(Collections.nCopies(entityIds.size(), null));

        // 逐个分区读取特征文件
        for (Map.Entry<String, List<Integer>> entry : partitionToIndices.entrySet()) {
            String partitionDate = entry.getKey();
            List<Integer> indices = entry.getValue();

            // 构建 MinIO 路径
            String path = buildFeaturePath(viewName, partitionDate);
            log.debug("读取特征文件: bucket={}, path={}", FEATURES_BUCKET, path);

            // 读取特征文件
            List<Map<String, Object>> featureRecords;
            try {
                featureRecords = readFeaturesFromMinio(path);
            } catch (Exception e) {
                log.warn("读取特征文件失败: {}/{}, 原因: {}", FEATURES_BUCKET, path, e.getMessage());
                // 文件不存在时，该分区下的所有 entity 标记为缺失
                for (int idx : indices) {
                    result.set(idx, null);
                }
                continue;
            }

            // 建立 entity_id -> 特征记录的索引
            Map<String, Map<String, Object>> entityFeatureMap = new HashMap<>();
            for (Map<String, Object> record : featureRecords) {
                Object eid = record.get("entity_id");
                if (eid != null) {
                    entityFeatureMap.put(eid.toString(), record);
                }
            }

            // 为每个 entity 查找特征
            for (int idx : indices) {
                String entityId = entityIds.get(idx);
                Map<String, Object> record = entityFeatureMap.get(entityId);

                if (record != null) {
                    // 添加特征视图前缀，避免不同视图的特征名冲突
                    Map<String, Object> prefixedFeatures = new HashMap<>();
                    for (Map.Entry<String, Object> field : record.entrySet()) {
                        String key = field.getKey();
                        // 跳过元数据字段
                        if ("entity_id".equals(key) || "computed_at".equals(key)) {
                            continue;
                        }
                        prefixedFeatures.put(viewName + "_" + key, field.getValue());
                    }
                    result.set(idx, prefixedFeatures);
                } else {
                    // entity 不存在于该分区，标记为缺失（后续丢弃）
                    log.debug("实体 {} 在特征视图 {} 的分区 {} 中未找到特征",
                            entityId, viewName, partitionDate);
                    result.set(idx, null);
                }
            }
        }

        return result;
    }

    /**
     * 从 MinIO 读取特征文件
     */
    @SuppressWarnings("unchecked")
    private List<Map<String, Object>> readFeaturesFromMinio(String path) {
        try (InputStream inputStream = minioService.downloadFile(FEATURES_BUCKET, path)) {
            ByteArrayOutputStream buffer = new ByteArrayOutputStream();
            byte[] tmp = new byte[4096];
            int n;
            while ((n = inputStream.read(tmp)) != -1) {
                buffer.write(tmp, 0, n);
            }
            byte[] data = buffer.toByteArray();
            return objectMapper.readValue(data, List.class);
        } catch (Exception e) {
            throw new RuntimeException("从 MinIO 读取特征文件失败: " + path, e);
        }
    }

    /**
     * 构建特征文件路径（与 FeatureComputeService.buildOutputPath 保持一致）
     */
    private String buildFeaturePath(String featureViewName, String partitionDate) {
        return String.format("%s/%s/features_%s.json", featureViewName, partitionDate, partitionDate);
    }

    /**
     * 解析时间戳为分区日期字符串（yyyyMMdd）
     *
     * 支持格式：
     * - 2026-05-01
     * - 2026-05-01T15:26:33
     * - 20260501
     */
    private String parsePartitionDate(String timestamp) {
        if (timestamp == null || timestamp.isEmpty()) {
            return LocalDate.now().format(DATE_FORMATTER);
        }

        // 已经是 yyyyMMdd 格式
        if (timestamp.matches("\\d{8}")) {
            return timestamp;
        }

        try {
            // 尝试解析 ISO 日期或带时间的格式
            if (timestamp.contains("T")) {
                return LocalDate.parse(timestamp.substring(0, 10)).format(DATE_FORMATTER);
            }
            return LocalDate.parse(timestamp).format(DATE_FORMATTER);
        } catch (Exception e) {
            log.warn("无法解析时间戳: {}，使用今天", timestamp);
            return LocalDate.now().format(DATE_FORMATTER);
        }
    }

    /**
     * 获取特征规格列表
     */
    private List<FeatureDefinition.FeatureSpec> getFeatureSpecs(String viewName) {
        List<FeatureDefinition.FeatureSpec> specs = new ArrayList<>();

        try {
            com.mogu.data.feature.entity.FeatureView apiView =
                    featureRegistryService.getFeatureView(viewName);

            if (apiView != null && apiView.getFeatures() != null) {
                for (com.mogu.data.feature.entity.FeatureDefinition.FeatureSpec feature : apiView.getFeatures()) {
                    FeatureDefinition.FeatureSpec spec = new FeatureDefinition.FeatureSpec();
                    spec.setName(feature.getName());
                    spec.setDtype(feature.getDtype());
                    spec.setDescription(feature.getDescription());
                    spec.setTransformExpr(feature.getTransformExpr());
                    specs.add(spec);
                }
            }
        } catch (Exception e) {
            log.error("获取特征规格失败: {}", viewName, e);
        }

        return specs;
    }

}
