package com.mogu.data.sample.entity;

import lombok.Data;

import java.util.List;
import java.util.Map;

/**
 * 样本构造配置
 */
@Data
public class SampleConfig {

    /**
     * 样本名称
     */
    private String sampleName;

    /**
     * 特征视图列表
     */
    private List<String> featureViews;

    /**
     * 标签配置
     */
    private LabelConfig labelConfig;

    /**
     * 时间配置
     */
    private TimeConfig timeConfig;

    /**
     * 采样配置
     */
    private SampleSplitConfig splitConfig;

    /**
     * 标签配置
     */
    @Data
    public static class LabelConfig {
        private String labelTable;
        private String labelColumn;
        private String labelType;
    }

    /**
     * 时间配置
     */
    @Data
    public static class TimeConfig {
        private String timestampColumn;
        private String startTime;
        private String endTime;
    }

    /**
     * 采样配置
     */
    @Data
    public static class SampleSplitConfig {
        private double trainRatio = 0.8;
        private double valRatio = 0.1;
        private double testRatio = 0.1;
        private String strategy = "random"; // random, temporal, stratified
        private String stratifyColumn;
    }
}