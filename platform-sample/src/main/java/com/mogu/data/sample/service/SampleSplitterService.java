package com.mogu.data.sample.service;

import com.mogu.data.common.exception.BusinessException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 样本划分服务
 * 将样本划分为训练集、验证集、测试集
 */
@Slf4j
@Service
public class SampleSplitterService {

    /**
     * 划分样本
     * @param sampleData 样本数据
     * @param trainRatio 训练集比例
     * @param valRatio 验证集比例
     * @param testRatio 测试集比例
     * @param splitStrategy 划分策略 (random, temporal, stratified)
     * @return 划分后的样本路径
     */
    public Map<String, String> splitSample(List<Map<String, Object>> sampleData,
                                           double trainRatio,
                                           double valRatio,
                                           double testRatio,
                                           String splitStrategy) {
        log.info("开始划分样本, 总数: {}, 训练集:{}, 验证集:{}, 测试集:{}, 策略:{}",
                sampleData.size(), trainRatio, valRatio, testRatio, splitStrategy);

        try {
            // 验证比例
            if (Math.abs(trainRatio + valRatio + testRatio - 1.0) > 0.001) {
                throw new BusinessException("训练集、验证集、测试集比例之和必须为1");
            }

            Map<String, String> splitPaths = new HashMap<>();

            switch (splitStrategy) {
                case "random":
                    splitPaths = randomSplit(sampleData, trainRatio, valRatio, testRatio);
                    break;
                case "temporal":
                    splitPaths = temporalSplit(sampleData, trainRatio, valRatio, testRatio);
                    break;
                case "stratified":
                    splitPaths = stratifiedSplit(sampleData, trainRatio, valRatio, testRatio);
                    break;
                default:
                    throw new BusinessException("不支持的划分策略: " + splitStrategy);
            }

            log.info("样本划分完成: {}", splitPaths);
            return splitPaths;

        } catch (Exception e) {
            log.error("样本划分失败: {}", e.getMessage(), e);
            throw new BusinessException("样本划分失败: " + e.getMessage());
        }
    }

    /**
     * 随机划分
     */
    private Map<String, String> randomSplit(List<Map<String, Object>> sampleData,
                                           double trainRatio,
                                           double valRatio,
                                           double testRatio) {
        log.info("执行随机划分");

        // 打乱数据
        java.util.Collections.shuffle(sampleData);

        int totalSize = sampleData.size();
        int trainSize = (int) (totalSize * trainRatio);
        int valSize = (int) (totalSize * valRatio);

        List<Map<String, Object>> trainSet = sampleData.subList(0, trainSize);
        List<Map<String, Object>> valSet = sampleData.subList(trainSize, trainSize + valSize);
        List<Map<String, Object>> testSet = sampleData.subList(trainSize + valSize, totalSize);

        Map<String, String> paths = new HashMap<>();
        paths.put("train", "samples/train.parquet");
        paths.put("val", "samples/val.parquet");
        paths.put("test", "samples/test.parquet");

        log.info("随机划分完成 - 训练集:{}, 验证集:{}, 测试集:{}",
                trainSet.size(), valSet.size(), testSet.size());

        return paths;
    }

    /**
     * 时间划分
     */
    private Map<String, String> temporalSplit(List<Map<String, Object>> sampleData,
                                              double trainRatio,
                                              double valRatio,
                                              double testRatio) {
        log.info("执行时间划分");

        // 按时间排序
        sampleData.sort((a, b) -> {
            String timeA = (String) a.get("timestamp");
            String timeB = (String) b.get("timestamp");
            return timeA.compareTo(timeB);
        });

        int totalSize = sampleData.size();
        int trainSize = (int) (totalSize * trainRatio);
        int valSize = (int) (totalSize * valRatio);

        List<Map<String, Object>> trainSet = sampleData.subList(0, trainSize);
        List<Map<String, Object>> valSet = sampleData.subList(trainSize, trainSize + valSize);
        List<Map<String, Object>> testSet = sampleData.subList(trainSize + valSize, totalSize);

        Map<String, String> paths = new HashMap<>();
        paths.put("train", "samples/train_temporal.parquet");
        paths.put("val", "samples/val_temporal.parquet");
        paths.put("test", "samples/test_temporal.parquet");

        log.info("时间划分完成 - 训练集:{}, 验证集:{}, 测试集:{}",
                trainSet.size(), valSet.size(), testSet.size());

        return paths;
    }

    /**
     * 分层划分
     */
    private Map<String, String> stratifiedSplit(List<Map<String, Object>> sampleData,
                                                double trainRatio,
                                                double valRatio,
                                                double testRatio) {
        log.info("执行分层划分");

        // 按标签分组，然后对每组进行随机划分
        Map<Object, List<Map<String, Object>>> groupedData = new HashMap<>();
        for (Map<String, Object> sample : sampleData) {
            Object label = sample.get("label");
            groupedData.computeIfAbsent(label, k -> new java.util.ArrayList<>()).add(sample);
        }

        // 对每组进行划分
        List<Map<String, Object>> trainSet = new java.util.ArrayList<>();
        List<Map<String, Object>> valSet = new java.util.ArrayList<>();
        List<Map<String, Object>> testSet = new java.util.ArrayList<>();

        for (List<Map<String, Object>> group : groupedData.values()) {
            java.util.Collections.shuffle(group);

            int totalSize = group.size();
            int trainSize = (int) (totalSize * trainRatio);
            int valSize = (int) (totalSize * valRatio);

            trainSet.addAll(group.subList(0, trainSize));
            valSet.addAll(group.subList(trainSize, trainSize + valSize));
            testSet.addAll(group.subList(trainSize + valSize, totalSize));
        }

        Map<String, String> paths = new HashMap<>();
        paths.put("train", "samples/train_stratified.parquet");
        paths.put("val", "samples/val_stratified.parquet");
        paths.put("test", "samples/test_stratified.parquet");

        log.info("分层划分完成 - 训练集:{}, 验证集:{}, 测试集:{}",
                trainSet.size(), valSet.size(), testSet.size());

        return paths;
    }
}