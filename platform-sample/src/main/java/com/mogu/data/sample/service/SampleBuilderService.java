package com.mogu.data.sample.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.mogu.data.common.entity.SampleBuildJob;
import com.mogu.data.common.entity.SampleConfig;
import com.mogu.data.common.exception.BusinessException;
import com.mogu.data.common.logger.Logger;
import com.mogu.data.common.repository.SampleBuildJobRepository;
import com.mogu.data.common.repository.SampleConfigRepository;
import com.mogu.data.common.storage.MinioService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;

/**
 * 样本构造服务
 * 实现完整的样本构造流程：特征获取 → Point-in-time Join → 负采样 → 质量校验 → 划分 → 注册版本
 *
 * @author MModelX Team
 * @since 2026-05-23
 */
@Service
@RequiredArgsConstructor
public class SampleBuilderService {

    private static final Logger log = Logger.getLogger(SampleBuilderService.class);
    private static final String SAMPLES_BUCKET = "samples";
    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("yyyyMMdd");

    private final PointInTimeJoinService pointInTimeJoinService;
    private final SampleQualityService sampleQualityService;
    private final SampleSplitterService sampleSplitterService;
    private final DatasetVersioningService datasetVersioningService;
    private final SampleBuildJobRepository jobRepository;
    private final SampleConfigRepository configRepository;
    private final MinioService minioService;
    private final ObjectMapper objectMapper;

    /**
     * 构造训练样本（完整流程）
     *
     * @param config 样本配置
     * @return 构建任务ID
     */
    public Long buildSample(SampleConfig config) {
        log.info("开始构造训练样本: {}", config.getName());

        // 1. 保存或获取样本配置
        SampleConfig savedConfig = saveOrGetConfig(config);

        // 2. 创建构建任务
        SampleBuildJob job = createBuildJob(savedConfig);

        // 3. 异步执行构建（实际项目中应使用 @Async 或消息队列）
        try {
            executeBuild(job, savedConfig);
        } catch (Exception e) {
            log.error("样本构造失败: {}", e.getMessage(), e);
            failJob(job, e.getMessage());
        }

        return job.getId();
    }

    /**
     * 执行构建流程
     */
    private void executeBuild(SampleBuildJob job, SampleConfig config) {
        updateJobProgress(job, 10, "读取标签数据");

        // 1. 读取标签数据（简化实现，实际应从标签表查询）
        List<Map<String, Object>> labelData = readLabelData(config);

        updateJobProgress(job, 25, "执行 Point-in-time Join");

        // 2. Point-in-time Join 获取特征
        List<String> entityIds = extractEntityIds(labelData);
        List<String> timestamps = extractTimestamps(labelData);
        List<String> featureViews = extractFeatureViews(config.getFeatureViews());

        Map<String, Map<String, Object>> features = pointInTimeJoinService.join(
                entityIds, timestamps, featureViews);

        updateJobProgress(job, 50, "合并样本数据");

        // 3. 合并特征和标签
        List<Map<String, Object>> sampleData = mergeFeaturesAndLabels(labelData, features);

        updateJobProgress(job, 60, "负样本采样");

        // 4. 负样本采样（如果启用）
        if (Boolean.TRUE.equals(config.getNegativeSamplingEnabled())) {
            sampleData = performNegativeSampling(sampleData, config);
        }

        updateJobProgress(job, 75, "质量校验");

        // 5. 质量校验
        Map<String, Object> qualityReport = sampleQualityService.validate(
                sampleData, config.getLabelColumn());

        updateJobProgress(job, 85, "样本划分");

        // 6. 样本划分并持久化
        Map<String, String> splitPaths = sampleSplitterService.splitAndPersist(
                sampleData,
                config.getTrainRatio(),
                config.getValRatio(),
                config.getTestRatio(),
                config.getSplitStrategy().name(),
                config.getName(),
                config.getStratifyColumn()
        );

        updateJobProgress(job, 95, "注册数据集版本");

        // 7. 注册数据集版本
        Integer featureCount = (Integer) qualityReport.get("featureCount");
        String datasetVersion = datasetVersioningService.registerFromBuild(
                config.getName(),
                config.getLabelColumn(),
                splitPaths,
                sampleData.size(),
                featureCount,
                qualityReport
        );

        // 8. 完成任务
        completeJob(job, sampleData.size(), splitPaths, qualityReport);

        log.info("样本构造完成: {}, 数据集版本: {}", config.getName(), datasetVersion);
    }

    /**
     * Point-in-time join（独立调用入口）
     */
    public Map<String, Object> pointInTimeJoin(List<String> entityIds,
                                                List<String> timestamps,
                                                List<String> featureViews) {
        Map<String, Map<String, Object>> result = pointInTimeJoinService.join(
                entityIds, timestamps, featureViews);

        Map<String, Object> response = new HashMap<>();
        response.put("entityIds", entityIds);
        response.put("timestamps", timestamps);
        response.put("featureViews", featureViews);
        response.put("features", result);
        response.put("sampleCount", result.size());

        return response;
    }

    /**
     * 负样本采样（独立调用入口）
     */
    public Map<String, Object> sampleNegatives(List<Map<String, Object>> positiveSamples,
                                                double negativeRatio) {
        log.info("负样本采样, 正样本数量: {}, 负样本比例: {}",
                positiveSamples.size(), negativeRatio);

        int negativeCount = (int) (positiveSamples.size() * negativeRatio);

        // 简化实现：复制正样本并修改标签为负样本
        // 实际项目中应从负样本池中随机采样
        List<Map<String, Object>> negativeSamples = new ArrayList<>();
        for (int i = 0; i < negativeCount; i++) {
            Map<String, Object> negative = new HashMap<>(positiveSamples.get(i % positiveSamples.size()));
            negative.put("label", 0);
            negativeSamples.add(negative);
        }

        List<Map<String, Object>> combined = new ArrayList<>();
        combined.addAll(positiveSamples);
        combined.addAll(negativeSamples);

        // 随机打乱
        Collections.shuffle(combined);

        Map<String, Object> result = new HashMap<>();
        result.put("positiveCount", positiveSamples.size());
        result.put("negativeCount", negativeCount);
        result.put("totalSamples", combined.size());
        result.put("samples", combined);

        return result;
    }

    /**
     * 样本质量校验（独立调用入口）
     */
    public Map<String, Object> validateSampleQuality(Map<String, Object> sampleData) {
        @SuppressWarnings("unchecked")
        List<Map<String, Object>> data = (List<Map<String, Object>>) sampleData.get("samples");
        String labelColumn = (String) sampleData.getOrDefault("labelColumn", "label");

        if (data == null || data.isEmpty()) {
            Map<String, Object> empty = new HashMap<>();
            empty.put("isValid", false);
            empty.put("error", "样本数据为空");
            return empty;
        }

        return sampleQualityService.validate(data, labelColumn);
    }

    // ========== 私有辅助方法 ==========

    private SampleConfig saveOrGetConfig(SampleConfig config) {
        if (config.getId() != null) {
            return configRepository.findById(config.getId())
                    .orElseThrow(() -> new BusinessException("样本配置不存在: " + config.getId()));
        }

        Optional<SampleConfig> existing = configRepository.findByName(config.getName());
        if (existing.isPresent()) {
            return existing.get();
        }

        return configRepository.save(config);
    }

    private SampleBuildJob createBuildJob(SampleConfig config) {
        SampleBuildJob job = new SampleBuildJob();
        job.setJobName(config.getName() + "_" + LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMddHHmmss")));
        job.setSampleConfigId(config.getId());
        job.setStatus(SampleBuildJob.JobStatus.RUNNING);
        job.setProgress(0);
        job.setTotalSteps(6);
        job.setStartedAt(LocalDateTime.now());

        return jobRepository.save(job);
    }

    private void updateJobProgress(SampleBuildJob job, int progress, String step) {
        job.setProgress(progress);
        job.setCurrentStep(step);
        jobRepository.save(job);
        log.info("样本构建进度: {}% - {}", progress, step);
    }

    private void completeJob(SampleBuildJob job, int sampleCount,
                              Map<String, String> splitPaths,
                              Map<String, Object> qualityReport) {
        job.setProgress(100);
        job.setStatus(SampleBuildJob.JobStatus.SUCCESS);
        job.setCompletedAt(LocalDateTime.now());
        job.setEntityCount((long) sampleCount);
        job.setOutputPath(splitPaths.get("output"));
        job.setTrainPath(splitPaths.get("train"));
        job.setValPath(splitPaths.get("val"));
        job.setTestPath(splitPaths.get("test"));

        try {
            job.setQualityReport(objectMapper.valueToTree(qualityReport));
        } catch (Exception e) {
            log.warn("序列化质量报告失败: {}", e.getMessage());
        }

        jobRepository.save(job);
    }

    private void failJob(SampleBuildJob job, String errorMessage) {
        job.setStatus(SampleBuildJob.JobStatus.FAILED);
        job.setErrorMessage(errorMessage);
        job.setCompletedAt(LocalDateTime.now());
        jobRepository.save(job);
    }

    private List<Map<String, Object>> readLabelData(SampleConfig config) {
        String labelTable = config.getLabelTable();
        String labelColumn = config.getLabelColumn();

        // 防御：如果标签配置为空，使用默认值
        if (labelColumn == null || labelColumn.trim().isEmpty()) {
            labelColumn = "label";
        }
        if (labelTable == null || labelTable.trim().isEmpty()) {
            labelTable = "user_labels";
        }

        log.info("读取标签数据: table={}, column={}", labelTable, labelColumn);

        List<Map<String, Object>> mockData = new ArrayList<>();
        for (int i = 0; i < 100; i++) {
            Map<String, Object> row = new HashMap<>();
            row.put("entity_id", "user_" + String.format("%04d", i + 1));
            row.put("timestamp", LocalDateTime.now().minusDays(i % 30).toString());
            row.put(labelColumn, i % 2);
            mockData.add(row);
        }

        return mockData;
    }

    private List<String> extractEntityIds(List<Map<String, Object>> labelData) {
        List<String> ids = new ArrayList<>();
        for (Map<String, Object> row : labelData) {
            ids.add((String) row.get("entity_id"));
        }
        return ids;
    }

    private List<String> extractTimestamps(List<Map<String, Object>> labelData) {
        List<String> timestamps = new ArrayList<>();
        for (Map<String, Object> row : labelData) {
            timestamps.add((String) row.get("timestamp"));
        }
        return timestamps;
    }

    @SuppressWarnings("unchecked")
    private List<String> extractFeatureViews(JsonNode featureViewsNode) {
        List<String> views = new ArrayList<>();
        if (featureViewsNode != null && featureViewsNode.isArray()) {
            for (JsonNode node : featureViewsNode) {
                views.add(node.asText());
            }
        }
        return views;
    }

    private List<Map<String, Object>> mergeFeaturesAndLabels(
            List<Map<String, Object>> labelData,
            Map<String, Map<String, Object>> features) {

        List<Map<String, Object>> merged = new ArrayList<>();

        for (Map<String, Object> labelRow : labelData) {
            String entityId = (String) labelRow.get("entity_id");
            Map<String, Object> row = new HashMap<>(labelRow);

            Map<String, Object> entityFeatures = features.get(entityId);
            if (entityFeatures != null) {
                row.putAll(entityFeatures);
            }

            merged.add(row);
        }

        return merged;
    }

    private List<Map<String, Object>> performNegativeSampling(
            List<Map<String, Object>> sampleData, SampleConfig config) {
        // 分离正负样本
        List<Map<String, Object>> positiveSamples = new ArrayList<>();
        for (Map<String, Object> row : sampleData) {
            Object label = row.get(config.getLabelColumn());
            if (label != null && (label.equals(1) || label.equals("1") || label.equals(true))) {
                positiveSamples.add(row);
            }
        }

        if (positiveSamples.isEmpty()) {
            return sampleData;
        }

        Map<String, Object> result = sampleNegatives(positiveSamples, config.getNegativeRatio());
        @SuppressWarnings("unchecked")
        List<Map<String, Object>> combined = (List<Map<String, Object>>) result.get("samples");
        return combined != null ? combined : sampleData;
    }
}
