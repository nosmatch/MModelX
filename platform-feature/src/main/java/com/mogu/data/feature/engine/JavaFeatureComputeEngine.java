package com.mogu.data.feature.engine;

import com.mogu.data.common.logger.Logger;
import com.mogu.data.feature.entity.FeatureDefinition;
import org.springframework.stereotype.Component;

import java.sql.Timestamp;
import java.time.*;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
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
        return compute(entityData, featureSpecs, LocalDate.now());
    }

    @Override
    public Map<String, Object> compute(
            List<Map<String, Object>> entityData,
            List<FeatureDefinition.FeatureSpec> featureSpecs,
            LocalDate partitionDate) {

        if (entityData == null || entityData.isEmpty()) {
            log.warn("实体数据为空，返回空特征");
            return new HashMap<>();
        }

        Map<String, Object> result = new HashMap<>();

        for (FeatureDefinition.FeatureSpec spec : featureSpecs) {
            try {
                // 按时间窗口过滤数据
                List<Map<String, Object>> filteredData = filterByTimeWindow(
                    entityData, spec.getTimeWindow(), partitionDate
                );

                String expr = spec.getTransformExpr();
                Object value = evaluateExpression(expr, filteredData);
                result.put(spec.getName(), value);

                log.debug("计算特征 {} = {} (窗口: {}, 原始行数: {}, 过滤后: {})",
                    spec.getName(), value, spec.getTimeWindow(),
                    entityData.size(), filteredData.size());

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

    // ==================== 时间窗口过滤 ====================

    /**
     * 按时间窗口过滤数据
     *
     * @param data 原始数据
     * @param timeWindow 时间窗口字符串（如 "7d", "30d", "1h"）
     * @param partitionDate 分区日期（基准日期）
     * @return 过滤后的数据
     */
    private List<Map<String, Object>> filterByTimeWindow(
            List<Map<String, Object>> data, String timeWindow, LocalDate partitionDate) {
        if (timeWindow == null || timeWindow.isEmpty() || data == null || data.isEmpty()) {
            return data;
        }

        Duration windowDuration = parseTimeWindow(timeWindow);
        if (windowDuration == null || windowDuration.isZero() || windowDuration.isNegative()) {
            return data;
        }

        String timestampField = findTimestampField(data);
        if (timestampField == null) {
            log.warn("无法识别时间戳字段，跳过时间窗口过滤: {}", timeWindow);
            return data;
        }

        LocalDateTime cutoff = partitionDate.atStartOfDay().minus(windowDuration);

        List<Map<String, Object>> filtered = data.stream()
            .filter(row -> {
                Object tsValue = row.get(timestampField);
                if (tsValue == null) {
                    return false;
                }
                LocalDateTime rowTime = parseTimestamp(tsValue);
                if (rowTime == null) {
                    return false;
                }
                return !rowTime.isBefore(cutoff);
            })
            .collect(Collectors.toList());

        log.debug("时间窗口过滤: {} -> 原始 {} 行, 过滤后 {} 行", timeWindow, data.size(), filtered.size());
        return filtered;
    }

    /**
     * 解析时间窗口字符串
     *
     * @param timeWindow 如 "7d", "1h", "30d", "90d"
     * @return Duration
     */
    private Duration parseTimeWindow(String timeWindow) {
        if (timeWindow == null || timeWindow.isEmpty()) {
            return null;
        }

        timeWindow = timeWindow.trim().toLowerCase();
        try {
            if (timeWindow.endsWith("d")) {
                int days = Integer.parseInt(timeWindow.substring(0, timeWindow.length() - 1));
                return Duration.ofDays(days);
            } else if (timeWindow.endsWith("h")) {
                int hours = Integer.parseInt(timeWindow.substring(0, timeWindow.length() - 1));
                return Duration.ofHours(hours);
            } else if (timeWindow.endsWith("w")) {
                int weeks = Integer.parseInt(timeWindow.substring(0, timeWindow.length() - 1));
                return Duration.ofDays(weeks * 7L);
            } else if (timeWindow.endsWith("m")) {
                int minutes = Integer.parseInt(timeWindow.substring(0, timeWindow.length() - 1));
                return Duration.ofMinutes(minutes);
            }
        } catch (NumberFormatException e) {
            log.warn("无法解析时间窗口: {}", timeWindow);
        }
        return null;
    }

    /**
     * 识别时间戳字段名
     *
     * @param data 数据行列表
     * @return 时间戳字段名，未找到返回 null
     */
    private String findTimestampField(List<Map<String, Object>> data) {
        if (data == null || data.isEmpty()) {
            return null;
        }

        Map<String, Object> firstRow = data.get(0);
        if (firstRow == null || firstRow.isEmpty()) {
            return null;
        }

        // 常见时间戳字段名，按优先级排序
        String[] candidates = {
            "timestamp", "created_at", "event_time", "order_date",
            "ts", "date", "time", "datetime", "event_date",
            "created_time", "updated_at", "updated_time"
        };

        for (String candidate : candidates) {
            if (firstRow.containsKey(candidate)) {
                return candidate;
            }
        }

        // 如果常见字段名都不匹配，尝试查找包含时间戳值的字段
        for (Map.Entry<String, Object> entry : firstRow.entrySet()) {
            if (entry.getValue() instanceof Timestamp ||
                entry.getValue() instanceof java.sql.Date ||
                entry.getValue() instanceof LocalDateTime ||
                entry.getValue() instanceof LocalDate) {
                return entry.getKey();
            }
        }

        return null;
    }

    /**
     * 解析时间戳值
     *
     * @param value 原始值
     * @return LocalDateTime，解析失败返回 null
     */
    private LocalDateTime parseTimestamp(Object value) {
        if (value == null) {
            return null;
        }

        try {
            if (value instanceof Timestamp) {
                return ((Timestamp) value).toLocalDateTime();
            }
            if (value instanceof java.sql.Date) {
                return ((java.sql.Date) value).toLocalDate().atStartOfDay();
            }
            if (value instanceof LocalDateTime) {
                return (LocalDateTime) value;
            }
            if (value instanceof LocalDate) {
                return ((LocalDate) value).atStartOfDay();
            }
            if (value instanceof Instant) {
                return LocalDateTime.ofInstant((Instant) value, ZoneId.systemDefault());
            }
            if (value instanceof Number) {
                long millis = ((Number) value).longValue();
                // 自动判断是毫秒还是秒
                if (millis > 1_000_000_000_000L) {
                    return LocalDateTime.ofInstant(Instant.ofEpochMilli(millis), ZoneId.systemDefault());
                } else {
                    return LocalDateTime.ofInstant(Instant.ofEpochSecond(millis), ZoneId.systemDefault());
                }
            }
            if (value instanceof String) {
                String str = ((String) value).trim();
                // 尝试多种日期格式
                DateTimeFormatter[] formatters = {
                    DateTimeFormatter.ISO_LOCAL_DATE_TIME,
                    DateTimeFormatter.ISO_DATE_TIME,
                    DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"),
                    DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss.S"),
                    DateTimeFormatter.ofPattern("yyyy-MM-dd"),
                    DateTimeFormatter.ofPattern("yyyy/MM/dd HH:mm:ss"),
                    DateTimeFormatter.ofPattern("yyyy/MM/dd")
                };
                for (DateTimeFormatter formatter : formatters) {
                    try {
                        if (str.contains("T") || str.contains(":")) {
                            return LocalDateTime.parse(str, formatter);
                        } else {
                            return LocalDate.parse(str, formatter).atStartOfDay();
                        }
                    } catch (DateTimeParseException ignored) {
                        // 继续尝试下一个格式
                    }
                }
            }
        } catch (Exception e) {
            log.debug("解析时间戳失败: {} (类型: {})", value, value.getClass().getName());
        }

        return null;
    }
}
