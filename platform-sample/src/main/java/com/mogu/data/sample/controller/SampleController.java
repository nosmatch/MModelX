package com.mogu.data.sample.controller;

import com.mogu.data.common.entity.Dataset;
import com.mogu.data.common.entity.DatasetVersion;
import com.mogu.data.common.entity.SampleBuildJob;
import com.mogu.data.common.entity.SampleConfig;
import com.mogu.data.common.exception.BusinessException;
import com.mogu.data.common.repository.DatasetRepository;
import com.mogu.data.common.repository.DatasetVersionRepository;
import com.mogu.data.common.repository.SampleBuildJobRepository;
import com.mogu.data.common.repository.SampleConfigRepository;
import com.mogu.data.common.result.Result;
import com.mogu.data.sample.service.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * 样本工程控制器
 *
 * 提供样本配置管理、样本构建、数据集版本管理等 REST API
 */
@Slf4j
@RestController
@RequestMapping("/api/v1/samples")
@RequiredArgsConstructor
public class SampleController {

    private final SampleBuilderService sampleBuilderService;
    private final SampleSplitterService sampleSplitterService;
    private final DatasetVersioningService datasetVersioningService;

    private final SampleConfigRepository sampleConfigRepository;
    private final SampleBuildJobRepository sampleBuildJobRepository;
    private final DatasetRepository datasetRepository;
    private final DatasetVersionRepository datasetVersionRepository;

    // ==================== 样本配置 CRUD ====================

    /**
     * 列出所有样本配置
     */
    @GetMapping("/configs")
    public Result<List<SampleConfig>> listSampleConfigs() {
        List<SampleConfig> configs = sampleConfigRepository.findAll();
        return Result.success(configs);
    }

    /**
     * 创建样本配置
     */
    @PostMapping("/configs")
    public Result<SampleConfig> createSampleConfig(@RequestBody SampleConfig config) {
        Optional<SampleConfig> existing = sampleConfigRepository.findByName(config.getName());
        if (existing.isPresent()) {
            throw new BusinessException("样本配置名称已存在: " + config.getName());
        }
        SampleConfig saved = sampleConfigRepository.save(config);
        return Result.success(saved);
    }

    /**
     * 更新样本配置
     */
    @PutMapping("/configs/{id}")
    public Result<SampleConfig> updateSampleConfig(@PathVariable Long id, @RequestBody SampleConfig config) {
        SampleConfig existing = sampleConfigRepository.findById(id)
                .orElseThrow(() -> new BusinessException("样本配置不存在: " + id));

        existing.setDescription(config.getDescription());
        existing.setLabelType(config.getLabelType());
        existing.setLabelTable(config.getLabelTable());
        existing.setLabelColumn(config.getLabelColumn());
        existing.setTimeColumn(config.getTimeColumn());
        existing.setStartTime(config.getStartTime());
        existing.setEndTime(config.getEndTime());
        existing.setSplitStrategy(config.getSplitStrategy());
        existing.setTrainRatio(config.getTrainRatio());
        existing.setValRatio(config.getValRatio());
        existing.setTestRatio(config.getTestRatio());
        existing.setStratifyColumn(config.getStratifyColumn());
        existing.setNegativeSamplingEnabled(config.getNegativeSamplingEnabled());
        existing.setNegativeRatio(config.getNegativeRatio());
        existing.setFeatureViews(config.getFeatureViews());
        existing.setStatus(config.getStatus());

        SampleConfig updated = sampleConfigRepository.save(existing);
        return Result.success(updated);
    }

    /**
     * 删除样本配置
     */
    @DeleteMapping("/configs/{id}")
    public Result<Void> deleteSampleConfig(@PathVariable Long id) {
        SampleConfig config = sampleConfigRepository.findById(id)
                .orElseThrow(() -> new BusinessException("样本配置不存在: " + id));
        sampleConfigRepository.delete(config);
        return Result.success();
    }

    // ==================== 样本构建 ====================

    /**
     * 构造训练样本
     */
    @PostMapping("/build")
    public Result<Long> buildSample(@RequestBody SampleConfig config) {
        Long jobId = sampleBuilderService.buildSample(config);
        return Result.success(jobId);
    }

    /**
     * 获取构建任务状态
     */
    @GetMapping("/build/{jobId}/status")
    public Result<Map<String, Object>> getBuildJobStatus(@PathVariable Long jobId) {
        SampleBuildJob job = sampleBuildJobRepository.findById(jobId)
                .orElseThrow(() -> new BusinessException("构建任务不存在: " + jobId));

        Map<String, Object> result = new HashMap<>();
        result.put("jobId", job.getId());
        result.put("jobName", job.getJobName());
        result.put("status", job.getStatus());
        result.put("progress", job.getProgress());
        result.put("currentStep", job.getCurrentStep());
        result.put("errorMessage", job.getErrorMessage());
        result.put("startedAt", job.getStartedAt());
        result.put("completedAt", job.getCompletedAt());
        return Result.success(result);
    }

    /**
     * Point-in-time join
     */
    @PostMapping("/join")
    public Result<Map<String, Object>> pointInTimeJoin(@RequestBody Map<String, Object> request) {
        @SuppressWarnings("unchecked")
        List<String> entityIds = (List<String>) request.get("entityIds");
        @SuppressWarnings("unchecked")
        List<String> timestamps = (List<String>) request.get("timestamps");
        @SuppressWarnings("unchecked")
        List<String> featureViews = (List<String>) request.get("featureViews");

        Map<String, Object> result = sampleBuilderService.pointInTimeJoin(entityIds, timestamps, featureViews);
        return Result.success(result);
    }

    /**
     * 负样本采样
     */
    @PostMapping("/sample-negatives")
    public Result<Map<String, Object>> sampleNegatives(@RequestBody Map<String, Object> request) {
        @SuppressWarnings("unchecked")
        List<Map<String, Object>> positiveSamples = (List<Map<String, Object>>) request.get("positiveSamples");
        double negativeRatio = ((Number) request.getOrDefault("negativeRatio", 1.0)).doubleValue();

        Map<String, Object> result = sampleBuilderService.sampleNegatives(positiveSamples, negativeRatio);
        return Result.success(result);
    }

    /**
     * 样本质量校验
     */
    @PostMapping("/validate")
    public Result<Map<String, Object>> validateSampleQuality(@RequestBody Map<String, Object> sampleData) {
        Map<String, Object> result = sampleBuilderService.validateSampleQuality(sampleData);
        return Result.success(result);
    }

    /**
     * 划分样本
     */
    @PostMapping("/split")
    public Result<Map<String, String>> splitSample(@RequestBody Map<String, Object> request) {
        @SuppressWarnings("unchecked")
        List<Map<String, Object>> sampleData = (List<Map<String, Object>>) request.get("sampleData");
        double trainRatio = ((Number) request.getOrDefault("trainRatio", 0.8)).doubleValue();
        double valRatio = ((Number) request.getOrDefault("valRatio", 0.1)).doubleValue();
        double testRatio = ((Number) request.getOrDefault("testRatio", 0.1)).doubleValue();
        String splitStrategy = (String) request.getOrDefault("splitStrategy", "random");

        Map<String, String> paths = sampleSplitterService.splitSample(sampleData, trainRatio, valRatio, testRatio, splitStrategy);
        return Result.success(paths);
    }

    // ==================== 数据集管理 ====================

    /**
     * 列出所有数据集（聚合版本信息）
     */
    @GetMapping("/datasets")
    public Result<List<Map<String, Object>>> listDatasets() {
        List<Dataset> datasets = datasetRepository.findAll();
        List<Map<String, Object>> result = new ArrayList<>();

        for (Dataset dataset : datasets) {
            Map<String, Object> item = new HashMap<>();
            item.put("id", dataset.getId());
            item.put("name", dataset.getName());
            item.put("description", dataset.getDescription());
            item.put("sampleCount", dataset.getSampleCount());
            item.put("featureCount", dataset.getFeatureCount() != null ? dataset.getFeatureCount() : 0);

            // 查询版本列表
            List<DatasetVersion> versions = datasetVersionRepository
                    .findByDatasetIdOrderByCreatedAtDesc(dataset.getId());
            item.put("versionCount", versions.size());

            List<Map<String, Object>> versionList = new ArrayList<>();
            for (DatasetVersion v : versions) {
                Map<String, Object> vMap = new HashMap<>();
                vMap.put("version", v.getVersion());
                vMap.put("sampleCount", v.getRowCount());
                vMap.put("featureCount", v.getFeatureCount() != null ? v.getFeatureCount() : 0);
                vMap.put("createdAt", v.getCreatedAt());

                // 划分信息
                Map<String, Object> splitInfo = new HashMap<>();
                if (v.getTrainPath() != null) splitInfo.put("train", v.getTrainPath());
                if (v.getValPath() != null) splitInfo.put("val", v.getValPath());
                if (v.getTestPath() != null) splitInfo.put("test", v.getTestPath());
                vMap.put("splitInfo", splitInfo);

                versionList.add(vMap);
            }
            item.put("versions", versionList);

            // 从关联的 SampleConfig 推断 labelType
            Optional<SampleConfig> configOpt = sampleConfigRepository.findByName(dataset.getName());
            item.put("labelType", configOpt.map(c -> c.getLabelType().name()).orElse("BINARY"));

            result.add(item);
        }

        return Result.success(result);
    }

    /**
     * 注册数据集版本
     */
    @PostMapping("/datasets")
    public Result<String> registerDataset(@RequestBody Dataset dataset) {
        String version = datasetVersioningService.registerDataset(dataset);
        return Result.success(version);
    }

    /**
     * 获取数据集
     */
    @GetMapping("/datasets/{name}/{version}")
    public Result<Dataset> getDataset(@PathVariable String name, @PathVariable String version) {
        Dataset dataset = datasetVersioningService.getDataset(name, version);
        return Result.success(dataset);
    }

    /**
     * 列出数据集的所有版本
     */
    @GetMapping("/datasets/{name}/versions")
    public Result<List<String>> listDatasetVersions(@PathVariable String name) {
        List<String> versions = datasetVersioningService.listDatasetVersions(name);
        return Result.success(versions);
    }

    /**
     * 删除数据集版本
     */
    @DeleteMapping("/datasets/{name}/{version}")
    public Result<Void> deleteDataset(@PathVariable String name, @PathVariable String version) {
        datasetVersioningService.deleteDataset(name, version);
        return Result.success();
    }

    /**
     * 比较数据集版本
     */
    @GetMapping("/datasets/{name}/compare")
    public Result<Map<String, Object>> compareDatasets(@PathVariable String name,
                                                        @RequestParam String version1,
                                                        @RequestParam String version2) {
        Map<String, Object> comparison = datasetVersioningService.compareDatasets(name, version1, version2);
        return Result.success(comparison);
    }

    /**
     * 获取最新版本
     */
    @GetMapping("/datasets/{name}/latest")
    public Result<String> getLatestVersion(@PathVariable String name) {
        String version = datasetVersioningService.getLatestVersion(name);
        return Result.success(version);
    }

    /**
     * 预览数据集样本数据
     */
    @GetMapping("/datasets/{name}/{version}/preview")
    public Result<Map<String, Object>> previewDataset(
            @PathVariable String name,
            @PathVariable String version,
            @RequestParam(defaultValue = "train") String split,
            @RequestParam(defaultValue = "50") int limit) {
        Map<String, Object> preview = datasetVersioningService.previewDataset(name, version, split, limit);
        return Result.success(preview);
    }
}
