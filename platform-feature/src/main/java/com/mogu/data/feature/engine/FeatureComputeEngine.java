package com.mogu.data.feature.engine;

import com.mogu.data.feature.entity.FeatureDefinition;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;

/**
 * Feature Compute Engine Interface
 *
 * 特征计算引擎接口
 *
 * @author MModelX Team
 * @since 2026-05-20
 */
public interface FeatureComputeEngine {

    /**
     * 计算单个实体的特征
     *
     * @param entityData 实体历史数据列表
     * @param featureSpecs 特征规格列表
     * @return 特征名 -> 特征值
     */
    Map<String, Object> compute(
        List<Map<String, Object>> entityData,
        List<FeatureDefinition.FeatureSpec> featureSpecs
    );

    /**
     * 计算单个实体的特征（支持时间窗口）
     *
     * @param entityData 实体历史数据列表
     * @param featureSpecs 特征规格列表
     * @param partitionDate 分区日期（时间窗口计算的基准日期）
     * @return 特征名 -> 特征值
     */
    default Map<String, Object> compute(
        List<Map<String, Object>> entityData,
        List<FeatureDefinition.FeatureSpec> featureSpecs,
        LocalDate partitionDate
    ) {
        return compute(entityData, featureSpecs);
    }

    /**
     * 支持的变换表达式类型
     */
    enum TransformType {
        SUM, AVG, COUNT, MAX, MIN,           // 聚合操作
        LOG, SQRT, ABS, POWER,               // 数学变换
        LAST, FIRST,                         // 时间窗口
        WHERE, FILTER                        // 条件过滤
    }

    /**
     * 验证表达式是否支持
     *
     * @param expression 表达式
     * @return 是否支持
     */
    boolean isExpressionSupported(String expression);
}
