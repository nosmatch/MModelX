package com.mogu.data.sample.controller;

import com.mogu.data.common.result.Result;
import com.mogu.data.sample.entity.Dataset;
import com.mogu.data.sample.entity.SampleConfig;
import com.mogu.data.sample.service.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

/**
 * 样本工程控制器
 */
@Slf4j
@RestController
@RequestMapping("/api/samples")
@RequiredArgsConstructor
public class SampleController {

    private final SampleBuilderService sampleBuilderService;
    private final SampleSplitterService sampleSplitterService;
    private final DatasetVersioningService datasetVersioningService;

    /**
     * 构造训练样本
     */
    @PostMapping("/build")
    public Result<String> buildSample(@RequestBody SampleConfig config) {
        String path = sampleBuilderService.buildSample(config);
        return Result.success(path);
    }

    /**
     * Point-in-time join
     */
    @PostMapping("/join")
    public Result<Map<String, Object>> pointInTimeJoin(@RequestBody Map<String, Object> request) {
        List<String> entityIds = (List<String>) request.get("entityIds");
        List<String> timestamps = (List<String>) request.get("timestamps");
        List<String> featureViews = (List<String>) request.get("featureViews");

        Map<String, Object> result = sampleBuilderService.pointInTimeJoin(entityIds, timestamps, featureViews);
        return Result.success(result);
    }

    /**
     * 负样本采样
     */
    @PostMapping("/sample-negatives")
    public Result<Map<String, Object>> sampleNegatives(@RequestBody Map<String, Object> request) {
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
        List<Map<String, Object>> sampleData = (List<Map<String, Object>>) request.get("sampleData");
        double trainRatio = ((Number) request.getOrDefault("trainRatio", 0.8)).doubleValue();
        double valRatio = ((Number) request.getOrDefault("valRatio", 0.1)).doubleValue();
        double testRatio = ((Number) request.getOrDefault("testRatio", 0.1)).doubleValue();
        String splitStrategy = (String) request.getOrDefault("splitStrategy", "random");

        Map<String, String> paths = sampleSplitterService.splitSample(sampleData, trainRatio, valRatio, testRatio, splitStrategy);
        return Result.success(paths);
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
}