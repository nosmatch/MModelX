package com.mogu.data.feature.entity;

import java.util.List;
import java.util.Map;

/**
 * 特征定义实体
 * 对应YAML配置文件中的特征定义
 */
public class FeatureDefinition {

    /**
     * 特征视图名称
     */
    private String featureView;

    /**
     * 实体类型（user_id, item_id等）
     */
    private String entity;

    /**
     * 特征TTL（天数）
     */
    private Integer ttl;

    /**
     * 特征列表
     */
    private List<FeatureSpec> features;

    /**
     * 数据源配置
     */
    private SourceConfig source;

    // Getters and Setters
    public String getFeatureView() {
        return featureView;
    }

    public void setFeatureView(String featureView) {
        this.featureView = featureView;
    }

    public String getEntity() {
        return entity;
    }

    public void setEntity(String entity) {
        this.entity = entity;
    }

    public Integer getTtl() {
        return ttl;
    }

    public void setTtl(Integer ttl) {
        this.ttl = ttl;
    }

    public List<FeatureSpec> getFeatures() {
        return features;
    }

    public void setFeatures(List<FeatureSpec> features) {
        this.features = features;
    }

    public SourceConfig getSource() {
        return source;
    }

    public void setSource(SourceConfig source) {
        this.source = source;
    }

    /**
     * 特征规格
     */
    public static class FeatureSpec {
        private String name;
        private String dtype;
        private String description;
        private String transformExpr;
        private String timeWindow;
        private Map<String, Object> defaultValue;

        // Getters and Setters
        public String getName() {
            return name;
        }

        public void setName(String name) {
            this.name = name;
        }

        public String getDtype() {
            return dtype;
        }

        public void setDtype(String dtype) {
            this.dtype = dtype;
        }

        public String getDescription() {
            return description;
        }

        public void setDescription(String description) {
            this.description = description;
        }

        public String getTransformExpr() {
            return transformExpr;
        }

        public void setTransformExpr(String transformExpr) {
            this.transformExpr = transformExpr;
        }

        public String getTimeWindow() {
            return timeWindow;
        }

        public void setTimeWindow(String timeWindow) {
            this.timeWindow = timeWindow;
        }

        public Map<String, Object> getDefaultValue() {
            return defaultValue;
        }

        public void setDefaultValue(Map<String, Object> defaultValue) {
            this.defaultValue = defaultValue;
        }
    }

    /**
     * 数据源配置
     */
    public static class SourceConfig {
        private String type;
        private String path;
        private String format;
        private Map<String, Object> config;

        // Getters and Setters
        public String getType() {
            return type;
        }

        public void setType(String type) {
            this.type = type;
        }

        public String getPath() {
            return path;
        }

        public void setPath(String path) {
            this.path = path;
        }

        public String getFormat() {
            return format;
        }

        public void setFormat(String format) {
            this.format = format;
        }

        public Map<String, Object> getConfig() {
            return config;
        }

        public void setConfig(Map<String, Object> config) {
            this.config = config;
        }
    }
}