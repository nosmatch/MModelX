package com.mogu.data.feature.engine;

import com.mogu.data.common.logger.Logger;
import com.mogu.data.feature.entity.FeatureDefinition;
import org.springframework.stereotype.Component;

import java.util.*;
import java.util.stream.Collectors;

/**
 * Java Feature Compute Engine
 *
 * 基于Java 8 Stream API的特征计算引擎实现
 * 支持常见的聚合、数学变换和时间窗口操作
 *
 * @author MModelX Team
 * @since 2026-05-20
 */
@Component
public class JavaFeatureComputeEngine implements FeatureComputeEngine {

    private static final Logger log = Logger.getLogger(JavaFeatureComputeEngine.class);

    @Override
    public Map<String, Object> compute(
            List<Map<String, Object>> entityData,
            List<FeatureDefinition.FeatureSpec> featureSpecs) {

        if (entityData == null || entityData.isEmpty()) {
            log.warn("实体数据为空，返回空特征");
            return new HashMap<>();
        }

        Map<String, Object> result = new HashMap<>();

        for (FeatureDefinition.FeatureSpec spec : featureSpecs) {
            try {
                String expr = spec.getTransformExpr();
                Object value = evaluateExpression(expr, entityData);
                result.put(spec.getName(), value);

                log.debug("计算特征 {} = {}", spec.getName(), value);

            } catch (Exception e) {
                log.error("计算特征失败: {} - {}", spec.getName(), e.getMessage(), e);

                // 使用默认值
                Object defaultValue = getDefaultValue(spec);
                result.put(spec.getName(), defaultValue);
            }
        }

        return result;
    }

    @Override
    public boolean isExpressionSupported(String expression) {
        if (expression == null || expression.isEmpty()) {
            return false;
        }

        String[] parts = expression.split("\\(");
        if (parts.length == 0) {
            return false;
        }

        String funcName = parts[0].toLowerCase();

        return funcName.matches("sum|avg|count|max|min|log|sqrt|abs|power|last|first");
    }

    /**
     * 计算表达式的值
     *
     * @param expr 表达式 (如 "sum(order_amount)", "avg(click_count)")
     * @param data 实体数据
     * @return 计算结果
     */
    private Object evaluateExpression(String expr, List<Map<String, Object>> data) {
        if (expr == null || expr.isEmpty()) {
            throw new IllegalArgumentException("表达式不能为空");
        }

        // 解析表达式: "sum(order_amount)" -> ["sum", "order_amount)"]
        String[] parts = expr.split("\\(");
        if (parts.length < 2) {
            throw new IllegalArgumentException("无效的表达式格式: " + expr);
        }

        String funcName = parts[0].toLowerCase().trim();
        String columnName = parts[1].replaceAll("\\)", "").trim();

        switch (funcName) {
            case "sum":
                return computeSum(data, columnName);

            case "avg":
                return computeAvg(data, columnName);

            case "count":
                return computeCount(data, columnName);

            case "max":
                return computeMax(data, columnName);

            case "min":
                return computeMin(data, columnName);

            case "log":
                return computeLog(data, columnName);

            case "sqrt":
                return computeSqrt(data, columnName);

            case "abs":
                return computeAbs(data, columnName);

            case "power":
                return computePower(data, columnName);

            case "last":
                return computeLast(data, columnName);

            case "first":
                return computeFirst(data, columnName);

            default:
                throw new UnsupportedOperationException("不支持的表达式: " + funcName);
        }
    }

    /**
     * 计算总和
     */
    private double computeSum(List<Map<String, Object>> data, String column) {
        return data.stream()
            .filter(row -> row.get(column) != null)
            .mapToDouble(row -> ((Number) row.get(column)).doubleValue())
            .sum();
    }

    /**
     * 计算平均值
     */
    private double computeAvg(List<Map<String, Object>> data, String column) {
        return data.stream()
            .filter(row -> row.get(column) != null)
            .mapToDouble(row -> ((Number) row.get(column)).doubleValue())
            .average()
            .orElse(0.0);
    }

    /**
     * 计算数量
     */
    private long computeCount(List<Map<String, Object>> data, String column) {
        if (column == null || column.isEmpty()) {
            return data.size();
        }

        return data.stream()
            .filter(row -> row.get(column) != null)
            .count();
    }

    /**
     * 计算最大值
     */
    private double computeMax(List<Map<String, Object>> data, String column) {
        return data.stream()
            .filter(row -> row.get(column) != null)
            .mapToDouble(row -> ((Number) row.get(column)).doubleValue())
            .max()
            .orElse(0.0);
    }

    /**
     * 计算最小值
     */
    private double computeMin(List<Map<String, Object>> data, String column) {
        return data.stream()
            .filter(row -> row.get(column) != null)
            .mapToDouble(row -> ((Number) row.get(column)).doubleValue())
            .min()
            .orElse(0.0);
    }

    /**
     * 计算对数 (log1p: log(x+1) to avoid log(0))
     */
    private double computeLog(List<Map<String, Object>> data, String column) {
        Map<String, Object> lastRow = data.get(data.size() - 1);
        Object value = lastRow.get(column);

        if (value == null) {
            return 0.0;
        }

        double num = ((Number) value).doubleValue();
        return Math.log1p(Math.max(num, 0)); // log(1+x) to avoid log(0)
    }

    /**
     * 计算平方根
     */
    private double computeSqrt(List<Map<String, Object>> data, String column) {
        Map<String, Object> lastRow = data.get(data.size() - 1);
        Object value = lastRow.get(column);

        if (value == null) {
            return 0.0;
        }

        double num = ((Number) value).doubleValue();
        return Math.sqrt(Math.max(num, 0));
    }

    /**
     * 计算绝对值
     */
    private double computeAbs(List<Map<String, Object>> data, String column) {
        Map<String, Object> lastRow = data.get(data.size() - 1);
        Object value = lastRow.get(column);

        if (value == null) {
            return 0.0;
        }

        double num = ((Number) value).doubleValue();
        return Math.abs(num);
    }

    /**
     * 计算幂次方 (默认平方)
     */
    private double computePower(List<Map<String, Object>> data, String column) {
        Map<String, Object> lastRow = data.get(data.size() - 1);
        Object value = lastRow.get(column);

        if (value == null) {
            return 0.0;
        }

        double num = ((Number) value).doubleValue();
        return Math.pow(num, 2); // 默认平方
    }

    /**
     * 获取最后一个值
     */
    private Object computeLast(List<Map<String, Object>> data, String column) {
        if (data.isEmpty()) {
            return null;
        }

        Map<String, Object> lastRow = data.get(data.size() - 1);
        return lastRow.get(column);
    }

    /**
     * 获取第一个值
     */
    private Object computeFirst(List<Map<String, Object>> data, String column) {
        if (data.isEmpty()) {
            return null;
        }

        Map<String, Object> firstRow = data.get(0);
        return firstRow.get(column);
    }

    /**
     * 获取默认值
     */
    private Object getDefaultValue(FeatureDefinition.FeatureSpec spec) {
        if (spec.getDefaultValue() != null && !spec.getDefaultValue().isEmpty()) {
            return spec.getDefaultValue().get("value");
        }

        // 根据数据类型返回默认值
        String dtype = spec.getDtype();
        if (dtype == null) {
            return null;
        }

        switch (dtype.toUpperCase()) {
            case "INT64":
            case "FLOAT64":
                return 0.0;
            case "STRING":
                return "";
            case "BOOLEAN":
                return false;
            default:
                return null;
        }
    }
}
