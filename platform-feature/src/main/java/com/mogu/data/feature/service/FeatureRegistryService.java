package com.mogu.data.feature.service;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.mogu.data.common.entity.DataSource;
import com.mogu.data.common.entity.Feature;
import com.mogu.data.common.entity.FeatureView;
import com.mogu.data.common.exception.BusinessException;
import com.mogu.data.common.repository.DataSourceRepository;
import com.mogu.data.feature.entity.FeatureDefinition;
import com.mogu.data.feature.repository.FeatureRepository;
import com.mogu.data.feature.repository.FeatureViewRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * Feature Registry Service
 *
 * 特征注册服务，管理特征视图的注册和更新
 *
 * @author MModelX Team
 * @since 2026-05-20
 */
@Service
@RequiredArgsConstructor
@Slf4j
@Transactional
public class FeatureRegistryService {

    private final FeatureViewRepository featureViewRepository;
    private final FeatureRepository featureRepository;
    private final DataSourceRepository dataSourceRepository;
    private final ObjectMapper objectMapper = new ObjectMapper();

    /**
     * 获取特征视图
     *
     * @param featureViewId 特征视图ID
     * @return 特征视图
     */
    public FeatureView getFeatureView(Long featureViewId) {
        return featureViewRepository.findById(featureViewId)
            .orElseThrow(() -> new BusinessException("特征视图不存在: " + featureViewId));
    }

    /**
     * 获取特征视图（按名称）
     *
     * @param featureViewName 特征视图名称
     * @return 特征视图
     */
    public com.mogu.data.feature.entity.FeatureView getFeatureView(String featureViewName) {
        FeatureView dbFeatureView = featureViewRepository.findActiveByName(featureViewName)
            .orElseThrow(() -> new BusinessException("特征视图不存在或已归档: " + featureViewName));

        // 转换为API使用的FeatureView
        com.mogu.data.feature.entity.FeatureView apiFeatureView =
            new com.mogu.data.feature.entity.FeatureView();

        apiFeatureView.setId(dbFeatureView.getId());
        apiFeatureView.setName(dbFeatureView.getName());
        apiFeatureView.setEntity(dbFeatureView.getEntity());
        apiFeatureView.setTtl(dbFeatureView.getTtl());
        apiFeatureView.setDescription(dbFeatureView.getDescription());
        apiFeatureView.setVersion(Math.toIntExact(dbFeatureView.getId())); // 简化版本号
        apiFeatureView.setStatus(dbFeatureView.getStatus().name());
        apiFeatureView.setCreatedAt(dbFeatureView.getCreatedAt());
        apiFeatureView.setUpdatedAt(dbFeatureView.getUpdatedAt());

        // 填充数据源关联信息
        Long dsId = dbFeatureView.getDatasourceId();
        if (dsId == null && dbFeatureView.getDatasource() != null) {
            dsId = dbFeatureView.getDatasource().getId();
        }
        apiFeatureView.setDatasourceId(dsId);
        if (dsId != null) {
            if (dbFeatureView.getDatasource() != null) {
                apiFeatureView.setDatasourceName(dbFeatureView.getDatasource().getName());
                apiFeatureView.setDatasourceType(dbFeatureView.getDatasource().getType());
            } else {
                dataSourceRepository.findById(dsId)
                    .ifPresent(ds -> {
                        apiFeatureView.setDatasourceName(ds.getName());
                        apiFeatureView.setDatasourceType(ds.getType());
                    });
            }
        }
        if (dbFeatureView.getSourceConfig() != null) {
            apiFeatureView.setSourceConfig(dbFeatureView.getSourceConfig().toString());
        }

        // 加载该视图下的特征列表
        List<Feature> features = featureRepository.findActiveFeaturesByViewId(dbFeatureView.getId());
        apiFeatureView.setFeatures(
            features.stream()
                .map(this::convertToFeatureSpec)
                .collect(java.util.stream.Collectors.toList())
        );

        return apiFeatureView;
    }

    /**
     * 更新特征视图
     *
     * @param featureView 特征视图
     */
    public void updateFeatureView(FeatureView featureView) {
        if (featureView.getId() == null) {
            throw new BusinessException("特征视图ID不能为空");
        }

        if (!featureViewRepository.existsById(featureView.getId())) {
            throw new BusinessException("特征视图不存在: " + featureView.getId());
        }

        featureViewRepository.save(featureView);
    }

    /**
     * 更新特征视图（按名称）
     *
     * @param featureViewName 特征视图名称
     * @param apiFeatureView API特征视图
     */
    public void updateFeatureView(String featureViewName, com.mogu.data.feature.entity.FeatureView apiFeatureView) {
        FeatureView dbFeatureView = featureViewRepository.findActiveByName(featureViewName)
            .orElseThrow(() -> new BusinessException("特征视图不存在: " + featureViewName));

        // 更新字段
        if (apiFeatureView.getEntity() != null) {
            dbFeatureView.setEntity(apiFeatureView.getEntity());
        }
        if (apiFeatureView.getTtl() != null) {
            dbFeatureView.setTtl(apiFeatureView.getTtl());
        }
        if (apiFeatureView.getDescription() != null) {
            dbFeatureView.setDescription(apiFeatureView.getDescription());
        }

        // 更新数据源关联
        if (apiFeatureView.getDatasourceId() != null) {
            DataSource dataSource = dataSourceRepository.findById(apiFeatureView.getDatasourceId())
                .orElseThrow(() -> new BusinessException("数据源不存在: " + apiFeatureView.getDatasourceId()));
            dbFeatureView.setDatasourceId(apiFeatureView.getDatasourceId());
            dbFeatureView.setDatasource(dataSource);
        }

        // 更新数据源使用配置
        if (apiFeatureView.getSourceConfig() != null) {
            try {
                dbFeatureView.setSourceConfig(objectMapper.readTree(apiFeatureView.getSourceConfig()));
            } catch (Exception e) {
                throw new BusinessException("数据源配置JSON格式错误: " + e.getMessage());
            }
        }

        featureViewRepository.save(dbFeatureView);
    }

    /**
     * 注册特征视图
     *
     * @param apiFeatureView API特征视图
     * @return 特征视图ID
     */
    public Long registerFeatureView(com.mogu.data.feature.entity.FeatureView apiFeatureView) {
        // 验证名称唯一性
        if (featureViewRepository.existsByName(apiFeatureView.getName())) {
            throw new BusinessException("特征视图名称已存在: " + apiFeatureView.getName());
        }

        // 创建数据库实体
        FeatureView dbFeatureView = new FeatureView();
        dbFeatureView.setName(apiFeatureView.getName());
        dbFeatureView.setEntity(apiFeatureView.getEntity());
        dbFeatureView.setTtl(apiFeatureView.getTtl());
        dbFeatureView.setDescription(apiFeatureView.getDescription());
        dbFeatureView.setStatus(FeatureView.FeatureViewStatus.DRAFT);

        // 设置数据源关联
        if (apiFeatureView.getDatasourceId() != null) {
            DataSource dataSource = dataSourceRepository.findById(apiFeatureView.getDatasourceId())
                .orElseThrow(() -> new BusinessException("数据源不存在: " + apiFeatureView.getDatasourceId()));
            dbFeatureView.setDatasourceId(apiFeatureView.getDatasourceId());
            dbFeatureView.setDatasource(dataSource);
        }

        // 设置数据源使用配置
        if (apiFeatureView.getSourceConfig() != null && !apiFeatureView.getSourceConfig().isEmpty()) {
            try {
                dbFeatureView.setSourceConfig(objectMapper.readTree(apiFeatureView.getSourceConfig()));
            } catch (Exception e) {
                throw new BusinessException("数据源配置JSON格式错误: " + e.getMessage());
            }
        }

        FeatureView saved = featureViewRepository.save(dbFeatureView);
        return saved.getId();
    }

    /**
     * 列出所有特征视图
     *
     * @return 特征视图列表
     */
    @Transactional(readOnly = true)
    public List<com.mogu.data.feature.entity.FeatureView> listFeatureViews() {
        List<FeatureView> dbFeatureViews = featureViewRepository.findActiveFeatureViews();
        log.info("FeatureView query returned {} records", dbFeatureViews.size());

        return dbFeatureViews.stream()
            .map(this::convertToApiFeatureView)
            .collect(java.util.stream.Collectors.toList());
    }

    /**
     * 删除特征视图
     *
     * @param featureViewName 特征视图名称
     */
    public void deleteFeatureView(String featureViewName) {
        FeatureView featureView = featureViewRepository.findByName(featureViewName)
            .orElseThrow(() -> new BusinessException("特征视图不存在: " + featureViewName));

        // 软删除：设置为归档状态
        featureView.setStatus(FeatureView.FeatureViewStatus.ARCHIVED);
        featureViewRepository.save(featureView);
    }

    /**
     * 注册特征定义
     *
     * @param featureDefinition 特征定义
     * @return 注册的特征定义
     */
    @Transactional
    public FeatureDefinition registerFeatureDefinition(FeatureDefinition featureDefinition) {
        // 验证特征视图是否存在
        String featureViewName = featureDefinition.getFeatureView();
        FeatureView featureView = featureViewRepository.findActiveByName(featureViewName)
            .orElseThrow(() -> new BusinessException("特征视图不存在: " + featureViewName));

        // 验证特征列表不为空
        if (featureDefinition.getFeatures() == null || featureDefinition.getFeatures().isEmpty()) {
            throw new BusinessException("特征列表不能为空");
        }

        // 准备数据源配置信息
        String sourceType = featureDefinition.getSource() != null ?
            featureDefinition.getSource().getType() : "unknown";
        String sourcePath = featureDefinition.getSource() != null ?
            featureDefinition.getSource().getPath() : null;

        // 遍历特征规格列表，创建Feature实体
        for (FeatureDefinition.FeatureSpec featureSpec : featureDefinition.getFeatures()) {
            // 检查特征名称是否已存在
            if (featureRepository.existsByFeatureViewIdAndName(featureView.getId(), featureSpec.getName())) {
                throw new BusinessException("特征名称已存在于该特征视图中: " + featureSpec.getName());
            }

            // 创建Feature实体
            Feature feature = new Feature();
            feature.setFeatureView(featureView);
            feature.setName(featureSpec.getName());
            feature.setDtype(featureSpec.getDtype() != null ? featureSpec.getDtype() : "FLOAT64");
            feature.setDescription(featureSpec.getDescription());
            feature.setSourceType(sourceType);
            feature.setSourcePath(sourcePath);
            feature.setStatus(Feature.FeatureStatus.DRAFT);

            // 构建配置JSON（包含transformExpr和defaultValue）
            com.fasterxml.jackson.databind.node.ObjectNode configNode = objectMapper.createObjectNode();
            if (featureSpec.getTransformExpr() != null) {
                configNode.put("transformExpr", featureSpec.getTransformExpr());
            }
            if (featureSpec.getDefaultValue() != null) {
                configNode.set("defaultValue", objectMapper.valueToTree(featureSpec.getDefaultValue()));
            }
            feature.setConfig(configNode);

            // 保存到数据库
            featureRepository.save(feature);
        }

        return featureDefinition;
    }

/**
     * 转换数据库实体为API实体
     *
     * @param dbFeatureView 数据库特征视图
     * @return API特征视图
     */
    private com.mogu.data.feature.entity.FeatureView convertToApiFeatureView(FeatureView dbFeatureView) {
        com.mogu.data.feature.entity.FeatureView apiFeatureView =
            new com.mogu.data.feature.entity.FeatureView();

        apiFeatureView.setId(dbFeatureView.getId());
        apiFeatureView.setName(dbFeatureView.getName());
        apiFeatureView.setEntity(dbFeatureView.getEntity());
        apiFeatureView.setTtl(dbFeatureView.getTtl());
        apiFeatureView.setDescription(dbFeatureView.getDescription());
        apiFeatureView.setVersion(Math.toIntExact(dbFeatureView.getId()));
        apiFeatureView.setStatus(dbFeatureView.getStatus().name());
        apiFeatureView.setCreatedAt(dbFeatureView.getCreatedAt());
        apiFeatureView.setUpdatedAt(dbFeatureView.getUpdatedAt());

        // 填充数据源关联信息（优先使用直接映射的列值）
        Long dsId = dbFeatureView.getDatasourceId();
        if (dsId == null && dbFeatureView.getDatasource() != null) {
            dsId = dbFeatureView.getDatasource().getId();
        }
        apiFeatureView.setDatasourceId(dsId);
        if (dsId != null) {
            if (dbFeatureView.getDatasource() != null) {
                apiFeatureView.setDatasourceName(dbFeatureView.getDatasource().getName());
                apiFeatureView.setDatasourceType(dbFeatureView.getDatasource().getType());
            } else {
                dataSourceRepository.findById(dsId)
                    .ifPresent(ds -> {
                        apiFeatureView.setDatasourceName(ds.getName());
                        apiFeatureView.setDatasourceType(ds.getType());
                    });
            }
        }
        if (dbFeatureView.getSourceConfig() != null) {
            apiFeatureView.setSourceConfig(dbFeatureView.getSourceConfig().toString());
        }

        return apiFeatureView;
    }

    /**
     * 获取特征视图的特征定义列表
     *
     * @param featureViewName 特征视图名称
     * @return 特征定义列表
     */
    public List<com.mogu.data.feature.entity.FeatureDefinition.FeatureSpec> getFeatureDefinitions(String featureViewName) {
        // 获取特征视图
        FeatureView featureView = featureViewRepository.findActiveByName(featureViewName)
            .orElseThrow(() -> new BusinessException("特征视图不存在: " + featureViewName));

        // 获取该视图的所有特征
        List<Feature> features = featureRepository.findActiveFeaturesByViewId(featureView.getId());

        // 转换为FeatureSpec列表
        return features.stream()
            .map(this::convertToFeatureSpec)
            .collect(java.util.stream.Collectors.toList());
    }

    /**
     * 删除特征定义
     *
     * @param featureViewName 特征视图名称
     * @param featureName 特征名称
     */
    @Transactional
    public void deleteFeatureDefinition(String featureViewName, String featureName) {
        // 获取特征视图
        FeatureView featureView = featureViewRepository.findActiveByName(featureViewName)
            .orElseThrow(() -> new BusinessException("特征视图不存在: " + featureViewName));

        // 查找特征
        List<Feature> features = featureRepository.findActiveFeaturesByViewId(featureView.getId());
        Feature featureToDelete = features.stream()
            .filter(f -> f.getName().equals(featureName))
            .findFirst()
            .orElseThrow(() -> new BusinessException("特征不存在: " + featureName));

        // 软删除：设置为失败状态
        featureToDelete.setStatus(Feature.FeatureStatus.FAILED);
        featureRepository.save(featureToDelete);
    }

    /**
     * 获取特征统计信息
     *
     * @param featureViewName 特征视图名称
     * @return 统计信息
     */
    public java.util.Map<String, Object> getFeatureStatistics(String featureViewName) {
        // 获取特征视图
        FeatureView featureView = featureViewRepository.findActiveByName(featureViewName)
            .orElseThrow(() -> new BusinessException("特征视图不存在: " + featureViewName));

        // 获取特征列表
        List<Feature> features = featureRepository.findActiveFeaturesByViewId(featureView.getId());

        java.util.Map<String, Object> stats = new java.util.HashMap<>();
        stats.put("featureViewName", featureViewName);
        stats.put("totalFeatures", features.size());
        stats.put("entityType", featureView.getEntity());
        stats.put("ttl", featureView.getTtl());

        // 按数据类型统计
        java.util.Map<String, Long> dtypeCount = features.stream()
            .collect(java.util.stream.Collectors.groupingBy(
                Feature::getDtype,
                java.util.stream.Collectors.counting()
            ));
        stats.put("dtypeDistribution", dtypeCount);

        // 按状态统计
        java.util.Map<String, Long> statusCount = features.stream()
            .collect(java.util.stream.Collectors.groupingBy(
                f -> f.getStatus().name(),
                java.util.stream.Collectors.counting()
            ));
        stats.put("statusDistribution", statusCount);

        return stats;
    }

    /**
     * 将Feature实体转换为FeatureSpec
     */
    private com.mogu.data.feature.entity.FeatureDefinition.FeatureSpec convertToFeatureSpec(Feature feature) {
        com.mogu.data.feature.entity.FeatureDefinition.FeatureSpec spec =
            new com.mogu.data.feature.entity.FeatureDefinition.FeatureSpec();

        spec.setName(feature.getName());
        spec.setDtype(feature.getDtype());
        spec.setDescription(feature.getDescription());

        // 从config中解析transformExpr和defaultValue
        if (feature.getConfig() != null) {
            try {
                com.fasterxml.jackson.databind.JsonNode config = feature.getConfig();
                if (config.has("transformExpr")) {
                    spec.setTransformExpr(config.get("transformExpr").asText());
                }
                if (config.has("defaultValue")) {
                    spec.setDefaultValue(objectMapper.readValue(config.get("defaultValue").toString(),
                        new TypeReference<Map<String, Object>>() {}));
                }
            } catch (Exception e) {
                // 忽略解析错误
            }
        }

        return spec;
    }

    /**
     * 更新特征视图的数据源配置
     *
     * @param featureViewName 特征视图名称
     * @param dataSourceType 数据源类型
     * @param dataSourceConfig 数据源配置（JSON字符串）
     */
    @Transactional
    public void updateDataSourceConfig(String featureViewName, String dataSourceType, String dataSourceConfig) {
        FeatureView featureView = featureViewRepository.findActiveByName(featureViewName)
            .orElseThrow(() -> new BusinessException("特征视图不存在: " + featureViewName));

        // 更新数据源配置（需要FeatureView实体有相应字段）
        // 注意：当前FeatureView实体可能没有这些字段，需要根据实际情况调整
        // featureView.setDataSourceType(dataSourceType);
        // featureView.setDataSourceConfig(dataSourceConfig);

        featureViewRepository.save(featureView);
    }

    /**
     * 搜索特征视图
     *
     * @param keyword 搜索关键词
     * @return 匹配的特征视图列表
     */
    public List<com.mogu.data.feature.entity.FeatureView> searchFeatureViews(String keyword) {
        List<FeatureView> dbFeatureViews = featureViewRepository.findActiveFeatureViews();

        return dbFeatureViews.stream()
            .filter(fv -> fv.getName().contains(keyword) ||
                        (fv.getDescription() != null && fv.getDescription().contains(keyword)))
            .map(this::convertToApiFeatureView)
            .collect(java.util.stream.Collectors.toList());
    }
}
