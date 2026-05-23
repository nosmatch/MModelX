package com.mogu.data.sample.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.mogu.data.common.exception.BusinessException;
import com.mogu.data.common.logger.Logger;
import com.mogu.data.common.storage.MinioService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.io.ByteArrayInputStream;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;

/**
 * 样本划分服务
 * 将样本划分为训练集、验证集、测试集，并持久化到 MinIO
 *
 * @author MModelX Team
 * @since 2026-05-23
 */
@Service
@RequiredArgsConstructor
public class SampleSplitterService {

    private static final Logger log = Logger.getLogger(SampleSplitterService.class);
    private static final String SAMPLES_BUCKET = "samples";

    private final MinioService minioService;
    private final ObjectMapper objectMapper;

    /**
     * 划分样本并持久化到 MinIO
     *
     * @param sampleData    样本数据
     * @param trainRatio    训练集比例
     * @param valRatio      验证集比例
     * @param testRatio     测试集比例
     * @param splitStrategy 划分策略
     * @param sampleName    样本名称（用于生成路径）
     * @param stratifyColumn 分层列名（分层策略时使用）
     * @return 各集文件路径 Map
     */
    public Map<String, String> splitAndPersist(List<Map<String, Object>> sampleData,
                                                double trainRatio,
                                                double valRatio,
                                                double testRatio,
                                                String splitStrategy,
                                                String sampleName,
                                                String stratifyColumn) {

        log.info("开始划分样本, 总数: {}, 策略: {}", sampleData.size(), splitStrategy);

        // 验证比例
        if (Math.abs(trainRatio + valRatio + testRatio - 1.0) > 0.001) {
            throw new BusinessException("训练集、验证集、测试集比例之和必须为1");
        }

        // 执行划分
        SplitResult splitResult = split(sampleData, trainRatio, valRatio, testRatio,
                splitStrategy, stratifyColumn);

        // 生成路径前缀
        String dateStr = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss"));
        String basePath = String.format("%s/%s", sampleName, dateStr);

        // 持久化到 MinIO
        Map<String, String> paths = new HashMap<>();
        paths.put("train", persistToMinio(splitResult.trainSet, basePath + "/train.json"));
        paths.put("val", persistToMinio(splitResult.valSet, basePath + "/val.json"));
        paths.put("test", persistToMinio(splitResult.testSet, basePath + "/test.json"));
        paths.put("output", basePath);

        log.info("样本划分完成并持久化: {}", paths);
        return paths;
    }

    /**
     * 仅划分样本（不持久化，兼容旧接口）
     */
    public Map<String, String> splitSample(List<Map<String, Object>> sampleData,
                                            double trainRatio,
                                            double valRatio,
                                            double testRatio,
                                            String splitStrategy) {
        return splitAndPersist(sampleData, trainRatio, valRatio, testRatio,
                splitStrategy, "default", null);
    }

    /**
     * 执行划分逻辑
     */
    private SplitResult split(List<Map<String, Object>> sampleData,
                               double trainRatio,
                               double valRatio,
                               double testRatio,
                               String splitStrategy,
                               String stratifyColumn) {

        switch (splitStrategy.toLowerCase()) {
            case "random":
                return randomSplit(sampleData, trainRatio, valRatio, testRatio);
            case "temporal":
                return temporalSplit(sampleData, trainRatio, valRatio, testRatio);
            case "stratified":
                return stratifiedSplit(sampleData, trainRatio, valRatio, testRatio, stratifyColumn);
            default:
                throw new BusinessException("不支持的划分策略: " + splitStrategy);
        }
    }

    /**
     * 随机划分
     */
    private SplitResult randomSplit(List<Map<String, Object>> sampleData,
                                     double trainRatio,
                                     double valRatio,
                                     double testRatio) {
        log.info("执行随机划分");

        List<Map<String, Object>> shuffled = new ArrayList<>(sampleData);
        Collections.shuffle(shuffled);

        return splitByRatio(shuffled, trainRatio, valRatio, testRatio);
    }

    /**
     * 时间划分
     */
    private SplitResult temporalSplit(List<Map<String, Object>> sampleData,
                                       double trainRatio,
                                       double valRatio,
                                       double testRatio) {
        log.info("执行时间划分");

        List<Map<String, Object>> sorted = new ArrayList<>(sampleData);
        sorted.sort(Comparator.comparing(this::extractTimestamp));

        return splitByRatio(sorted, trainRatio, valRatio, testRatio);
    }

    /**
     * 分层划分
     */
    private SplitResult stratifiedSplit(List<Map<String, Object>> sampleData,
                                         double trainRatio,
                                         double valRatio,
                                         double testRatio,
                                         String stratifyColumn) {
        log.info("执行分层划分, 分层列: {}", stratifyColumn);

        if (stratifyColumn == null || stratifyColumn.isEmpty()) {
            log.warn("未指定分层列，回退到随机划分");
            return randomSplit(sampleData, trainRatio, valRatio, testRatio);
        }

        // 按分层列分组
        Map<Object, List<Map<String, Object>>> groupedData = new HashMap<>();
        for (Map<String, Object> sample : sampleData) {
            Object key = sample.get(stratifyColumn);
            if (key == null) key = "null";
            groupedData.computeIfAbsent(key, k -> new ArrayList<>()).add(sample);
        }

        List<Map<String, Object>> trainSet = new ArrayList<>();
        List<Map<String, Object>> valSet = new ArrayList<>();
        List<Map<String, Object>> testSet = new ArrayList<>();

        for (List<Map<String, Object>> group : groupedData.values()) {
            SplitResult groupSplit = splitByRatio(new ArrayList<>(group), trainRatio, valRatio, testRatio);
            trainSet.addAll(groupSplit.trainSet);
            valSet.addAll(groupSplit.valSet);
            testSet.addAll(groupSplit.testSet);
        }

        // 打乱各集内部顺序
        Collections.shuffle(trainSet);
        Collections.shuffle(valSet);
        Collections.shuffle(testSet);

        return new SplitResult(trainSet, valSet, testSet);
    }

    /**
     * 按比例切分列表
     */
    private SplitResult splitByRatio(List<Map<String, Object>> data,
                                      double trainRatio,
                                      double valRatio,
                                      double testRatio) {
        int totalSize = data.size();
        int trainSize = (int) (totalSize * trainRatio);
        int valSize = (int) (totalSize * valRatio);

        List<Map<String, Object>> trainSet = data.subList(0, trainSize);
        List<Map<String, Object>> valSet = data.subList(trainSize, trainSize + valSize);
        List<Map<String, Object>> testSet = data.subList(trainSize + valSize, totalSize);

        return new SplitResult(
                new ArrayList<>(trainSet),
                new ArrayList<>(valSet),
                new ArrayList<>(testSet)
        );
    }

    /**
     * 提取时间戳用于排序
     */
    private String extractTimestamp(Map<String, Object> row) {
        Object ts = row.get("timestamp");
        return ts != null ? ts.toString() : "";
    }

    /**
     * 持久化到 MinIO
     */
    private String persistToMinio(List<Map<String, Object>> data, String path) {
        try {
            String json = objectMapper.writeValueAsString(data);
            byte[] bytes = json.getBytes(StandardCharsets.UTF_8);

            try (ByteArrayInputStream inputStream = new ByteArrayInputStream(bytes)) {
                minioService.uploadFile(SAMPLES_BUCKET, path, inputStream, bytes.length, "application/json");
            }

            log.info("样本数据已持久化: {}/{}", SAMPLES_BUCKET, path);
            return SAMPLES_BUCKET + "/" + path;

        } catch (Exception e) {
            log.error("持久化样本数据失败: {}", path, e);
            throw new BusinessException("持久化样本数据失败: " + e.getMessage());
        }
    }

    /**
     * 划分结果内部类
     */
    private static class SplitResult {
        final List<Map<String, Object>> trainSet;
        final List<Map<String, Object>> valSet;
        final List<Map<String, Object>> testSet;

        SplitResult(List<Map<String, Object>> trainSet,
                    List<Map<String, Object>> valSet,
                    List<Map<String, Object>> testSet) {
            this.trainSet = trainSet;
            this.valSet = valSet;
            this.testSet = testSet;
        }
    }
}
