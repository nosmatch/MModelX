package com.mogu.data.sample.service;

import com.mogu.data.common.logger.Logger;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.*;

/**
 * 样本质量校验服务
 *
 * 校验项：
 * - 行数校验
 * - 特征完整性（空值率）
 * - 标签分布校验
 * - 特征相关性分析
 * - 重复样本检测
 *
 * @author MModelX Team
 * @since 2026-05-23
 */
@Service
@RequiredArgsConstructor
public class SampleQualityService {

    private static final Logger log = Logger.getLogger(SampleQualityService.class);

    /**
     * 执行样本质量校验
     *
     * @param sampleData 样本数据（List of Map）
     * @param labelColumn 标签列名
     * @return 质量报告
     */
    public Map<String, Object> validate(List<Map<String, Object>> sampleData, String labelColumn) {
        log.info("开始样本质量校验, 样本数: {}", sampleData.size());

        if (sampleData == null || sampleData.isEmpty()) {
            Map<String, Object> emptyReport = new HashMap<>();
            emptyReport.put("isValid", false);
            emptyReport.put("overallScore", 0);
            emptyReport.put("error", "样本数据为空");
            return emptyReport;
        }

        Map<String, Object> report = new HashMap<>();

        // 1. 基本统计
        report.put("totalSamples", sampleData.size());
        report.put("featureCount", countFeatures(sampleData));

        // 2. 标签分布
        Map<String, Object> labelDistribution = analyzeLabelDistribution(sampleData, labelColumn);
        report.put("labelDistribution", labelDistribution);

        // 3. 空值率
        Map<String, Object> nullRateReport = analyzeNullRate(sampleData);
        report.put("nullRate", nullRateReport);

        // 4. 重复样本
        Map<String, Object> duplicateReport = analyzeDuplicates(sampleData);
        report.put("duplicates", duplicateReport);

        // 5. 特征统计
        Map<String, Object> featureStats = analyzeFeatureStats(sampleData, labelColumn);
        report.put("featureStats", featureStats);

        // 6. 计算综合评分
        int overallScore = calculateOverallScore(nullRateReport, duplicateReport, labelDistribution);
        report.put("overallScore", overallScore);
        report.put("isValid", overallScore >= 60);

        log.info("样本质量校验完成, 综合评分: {}", overallScore);
        return report;
    }

    /**
     * 统计特征数量（排除标签列和元数据列）
     */
    private int countFeatures(List<Map<String, Object>> sampleData) {
        if (sampleData.isEmpty()) return 0;
        Set<String> keys = new HashSet<>(sampleData.get(0).keySet());
        keys.remove("entity_id");
        keys.remove("timestamp");
        keys.remove("label");
        return keys.size();
    }

    /**
     * 分析标签分布
     */
    private Map<String, Object> analyzeLabelDistribution(List<Map<String, Object>> sampleData, String labelColumn) {
        Map<String, Object> result = new HashMap<>();

        Map<Object, Long> distribution = new HashMap<>();
        long nullCount = 0;

        for (Map<String, Object> row : sampleData) {
            Object label = row.get(labelColumn);
            if (label == null) {
                nullCount++;
            } else {
                distribution.merge(label, 1L, Long::sum);
            }
        }

        result.put("distribution", distribution);
        result.put("nullCount", nullCount);
        result.put("nullRate", (double) nullCount / sampleData.size());

        // 计算正负样本比例（二分类场景）
        if (distribution.size() == 2) {
            List<Long> counts = new ArrayList<>(distribution.values());
            long positive = counts.get(0);
            long negative = counts.get(1);
            double ratio = negative > 0 ? (double) positive / negative : 0;
            result.put("positiveNegativeRatio", String.format("%.2f:1", ratio));
            result.put("isBalanced", ratio >= 0.3 && ratio <= 3.0);
        }

        return result;
    }

    /**
     * 分析空值率
     */
    private Map<String, Object> analyzeNullRate(List<Map<String, Object>> sampleData) {
        Map<String, Object> result = new HashMap<>();
        Map<String, Double> columnNullRates = new HashMap<>();

        if (sampleData.isEmpty()) {
            result.put("overallNullRate", 0.0);
            result.put("columnNullRates", columnNullRates);
            result.put("highNullRateColumns", new ArrayList<String>());
            return result;
        }

        // 获取所有列
        Set<String> columns = sampleData.get(0).keySet();

        long totalCells = (long) sampleData.size() * columns.size();
        long totalNulls = 0;

        for (String column : columns) {
            long nullCount = 0;
            for (Map<String, Object> row : sampleData) {
                if (row.get(column) == null) {
                    nullCount++;
                }
            }
            double nullRate = (double) nullCount / sampleData.size();
            columnNullRates.put(column, nullRate);
            totalNulls += nullCount;
        }

        double overallNullRate = (double) totalNulls / totalCells;

        // 找出空值率高的列（> 50%）
        List<String> highNullRateColumns = new ArrayList<>();
        for (Map.Entry<String, Double> entry : columnNullRates.entrySet()) {
            if (entry.getValue() > 0.5) {
                highNullRateColumns.add(entry.getKey());
            }
        }

        result.put("overallNullRate", overallNullRate);
        result.put("columnNullRates", columnNullRates);
        result.put("highNullRateColumns", highNullRateColumns);

        return result;
    }

    /**
     * 分析重复样本
     */
    private Map<String, Object> analyzeDuplicates(List<Map<String, Object>> sampleData) {
        Map<String, Object> result = new HashMap<>();

        Set<String> seen = new HashSet<>();
        int duplicateCount = 0;

        for (Map<String, Object> row : sampleData) {
            // 使用 entity_id + timestamp 作为唯一标识
            String key = row.get("entity_id") + "_" + row.get("timestamp");
            if (seen.contains(key)) {
                duplicateCount++;
            } else {
                seen.add(key);
            }
        }

        double duplicateRate = (double) duplicateCount / sampleData.size();

        result.put("duplicateCount", duplicateCount);
        result.put("duplicateRate", duplicateRate);
        result.put("uniqueCount", seen.size());

        return result;
    }

    /**
     * 分析特征统计信息
     */
    private Map<String, Object> analyzeFeatureStats(List<Map<String, Object>> sampleData, String labelColumn) {
        Map<String, Object> result = new HashMap<>();
        Map<String, Map<String, Object>> columnStats = new HashMap<>();

        if (sampleData.isEmpty()) {
            result.put("columnStats", columnStats);
            return result;
        }

        Set<String> columns = new HashSet<>(sampleData.get(0).keySet());
        columns.remove("entity_id");
        columns.remove("timestamp");
        columns.remove(labelColumn);

        for (String column : columns) {
            Map<String, Object> stats = new HashMap<>();

            List<Double> numericValues = new ArrayList<>();
            Set<Object> uniqueValues = new HashSet<>();

            for (Map<String, Object> row : sampleData) {
                Object value = row.get(column);
                if (value != null) {
                    uniqueValues.add(value);
                    if (value instanceof Number) {
                        numericValues.add(((Number) value).doubleValue());
                    }
                }
            }

            stats.put("uniqueCount", uniqueValues.size());
            stats.put("uniqueRate", (double) uniqueValues.size() / sampleData.size());

            if (!numericValues.isEmpty()) {
                stats.put("min", Collections.min(numericValues));
                stats.put("max", Collections.max(numericValues));
                stats.put("mean", numericValues.stream().mapToDouble(Double::doubleValue).average().orElse(0));
                stats.put("std", calculateStd(numericValues));
            }

            columnStats.put(column, stats);
        }

        result.put("columnStats", columnStats);
        return result;
    }

    /**
     * 计算标准差
     */
    private double calculateStd(List<Double> values) {
        if (values.size() < 2) return 0;

        double mean = values.stream().mapToDouble(Double::doubleValue).average().orElse(0);
        double variance = values.stream()
                .mapToDouble(v -> Math.pow(v - mean, 2))
                .average()
                .orElse(0);

        return Math.sqrt(variance);
    }

    /**
     * 计算综合评分（0-100）
     */
    private int calculateOverallScore(Map<String, Object> nullRateReport,
                                       Map<String, Object> duplicateReport,
                                       Map<String, Object> labelDistribution) {
        double score = 100;

        // 空值率扣分
        double overallNullRate = (Double) nullRateReport.get("overallNullRate");
        score -= overallNullRate * 50;

        // 重复率扣分
        double duplicateRate = (Double) duplicateReport.get("duplicateRate");
        score -= duplicateRate * 30;

        // 标签空值扣分
        double labelNullRate = (Double) labelDistribution.get("nullRate");
        score -= labelNullRate * 20;

        return Math.max(0, Math.min(100, (int) score));
    }
}
