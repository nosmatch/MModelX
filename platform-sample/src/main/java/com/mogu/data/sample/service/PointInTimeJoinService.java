package com.mogu.data.sample.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.mogu.data.common.entity.DataSource;
import com.mogu.data.common.entity.FeatureView;
import com.mogu.data.common.exception.BusinessException;
import com.mogu.data.common.logger.Logger;
import com.mogu.data.common.repository.DataSourceRepository;
import com.mogu.data.feature.engine.FeatureComputeEngine;
import com.mogu.data.feature.entity.FeatureDefinition;
import com.mogu.data.feature.service.FeatureRegistryService;
import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.DriverManagerDataSource;
import org.springframework.stereotype.Service;

import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.util.*;

/**
 * Point-in-time Join 服务
 *
 * 核心功能：防止数据穿越（Data Leakage）
 * 对于每个 (entity, timestamp) 组合，获取该时间点之前的最新特征值
 *
 * @author MModelX Team
 * @since 2026-05-23
 */
@Service
@RequiredArgsConstructor
public class PointInTimeJoinService {

    private static final Logger log = Logger.getLogger(PointInTimeJoinService.class);

    private final FeatureRegistryService featureRegistryService;
    private final DataSourceRepository dataSourceRepository;
    private final FeatureComputeEngine computeEngine;
    private final ObjectMapper objectMapper;

    /**
     * 执行 Point-in-time join
     *
     * @param entityIds     实体ID列表
     * @param timestamps    时间戳列表（与 entityIds 一一对应）
     * @param featureViews  特征视图名称列表
     * @return 每个 entity 对应的特征快照 Map<entityId, Map<featureName, value>>
     */
    public Map<String, Map<String, Object>> join(
            List<String> entityIds,
            List<String> timestamps,
            List<String> featureViews) {

        if (entityIds.size() != timestamps.size()) {
            throw new BusinessException("实体ID数量与时间戳数量不匹配");
        }

        log.info("开始 Point-in-time join, 实体数: {}, 特征视图数: {}",
                entityIds.size(), featureViews.size());

        Map<String, Map<String, Object>> result = new HashMap<>();

        // 初始化结果Map
        for (String entityId : entityIds) {
            result.put(entityId, new HashMap<>());
        }

        // 逐个特征视图处理
        for (String viewName : featureViews) {
            Map<String, Map<String, Object>> viewFeatures = joinSingleView(
                    entityIds, timestamps, viewName);

            // 合并到总结果
            for (Map.Entry<String, Map<String, Object>> entry : viewFeatures.entrySet()) {
                String entityId = entry.getKey();
                Map<String, Object> features = entry.getValue();
                result.get(entityId).putAll(features);
            }
        }

        log.info("Point-in-time join 完成, 共处理 {} 个实体", result.size());
        return result;
    }

    /**
     * 单个特征视图的 Point-in-time join
     */
    private Map<String, Map<String, Object>> joinSingleView(
            List<String> entityIds,
            List<String> timestamps,
            String viewName) {

        log.debug("处理特征视图: {}", viewName);

        // 1. 获取特征视图定义
        FeatureView view = getFeatureView(viewName);
        if (view == null) {
            throw new BusinessException("特征视图不存在: " + viewName);
        }

        // 2. 获取特征列表
        List<FeatureDefinition.FeatureSpec> featureSpecs = getFeatureSpecs(view);
        if (featureSpecs.isEmpty()) {
            log.warn("特征视图 {} 没有配置特征", viewName);
            return new HashMap<>();
        }

        // 3. 获取数据源配置
        JdbcTemplate jdbcTemplate = resolveJdbcTemplate(view);
        JsonNode sourceConfig = view.getSourceConfig();
        if (sourceConfig == null) {
            throw new BusinessException("特征视图 " + viewName + " 未配置数据源");
        }

        String table = sourceConfig.has("table") ? sourceConfig.get("table").asText() : null;
        String entityColumn = sourceConfig.has("entityColumn") ? sourceConfig.get("entityColumn").asText() : null;
        String dateColumn = sourceConfig.has("dateColumn") ? sourceConfig.get("dateColumn").asText() : null;

        if (table == null || entityColumn == null || dateColumn == null) {
            throw new BusinessException("特征视图 " + viewName + " 数据源配置不完整");
        }

        // 4. 对每个 entity 查询历史数据并计算特征
        Map<String, Map<String, Object>> result = new HashMap<>();

        for (int i = 0; i < entityIds.size(); i++) {
            String entityId = entityIds.get(i);
            String timestamp = timestamps.get(i);

            try {
                // 查询该 entity 在 timestamp 之前的所有数据
                List<Map<String, Object>> entityData = queryEntityHistory(
                        jdbcTemplate, table, entityColumn, dateColumn, entityId, timestamp);

                // 使用特征计算引擎计算特征
                Map<String, Object> features = computeEngine.compute(entityData, featureSpecs);

                // 添加特征视图前缀，避免不同视图的特征名冲突
                Map<String, Object> prefixedFeatures = new HashMap<>();
                for (Map.Entry<String, Object> entry : features.entrySet()) {
                    prefixedFeatures.put(viewName + "_" + entry.getKey(), entry.getValue());
                }

                result.put(entityId, prefixedFeatures);

            } catch (Exception e) {
                log.error("计算实体 {} 的特征失败: {}", entityId, e.getMessage());
                // 使用默认值
                Map<String, Object> defaultFeatures = new HashMap<>();
                for (FeatureDefinition.FeatureSpec spec : featureSpecs) {
                    defaultFeatures.put(viewName + "_" + spec.getName(), getDefaultValue(spec));
                }
                result.put(entityId, defaultFeatures);
            }
        }

        return result;
    }

    /**
     * 查询实体在指定时间戳之前的历史数据
     */
    private List<Map<String, Object>> queryEntityHistory(
            JdbcTemplate jdbcTemplate,
            String table,
            String entityColumn,
            String dateColumn,
            String entityId,
            String timestamp) {

        String sql = String.format(
                "SELECT * FROM %s WHERE %s = ? AND %s <= ? ORDER BY %s DESC",
                table, entityColumn, dateColumn, dateColumn
        );

        // 兼容 ISO-8601 格式 (2026-05-23T15:26:33) 和 SQL 格式 (2026-05-23 15:26:33)
        Timestamp ts;
        try {
            ts = Timestamp.valueOf(timestamp);
        } catch (IllegalArgumentException e) {
            ts = Timestamp.valueOf(LocalDateTime.parse(timestamp));
        }

        return jdbcTemplate.queryForList(sql, entityId, ts);
    }

    /**
     * 获取特征视图
     */
    private FeatureView getFeatureView(String viewName) {
        try {
            com.mogu.data.feature.entity.FeatureView apiView =
                    featureRegistryService.getFeatureView(viewName);
            if (apiView == null) return null;

            // 转换为 common entity
            FeatureView view = new FeatureView();
            view.setId(apiView.getId());
            view.setName(apiView.getName());
            view.setEntity(apiView.getEntity());
            if (apiView.getSourceConfig() != null) {
                view.setSourceConfig(objectMapper.readTree(apiView.getSourceConfig()));
            }
            view.setDatasourceId(apiView.getDatasourceId());
            return view;
        } catch (Exception e) {
            log.error("获取特征视图失败: {}", viewName, e);
            return null;
        }
    }

    /**
     * 获取特征规格列表
     */
    private List<FeatureDefinition.FeatureSpec> getFeatureSpecs(FeatureView view) {
        List<FeatureDefinition.FeatureSpec> specs = new ArrayList<>();

        try {
            // 从 featureRegistryService 获取完整特征列表
            com.mogu.data.feature.entity.FeatureView apiView =
                    featureRegistryService.getFeatureView(view.getName());

            if (apiView != null && apiView.getFeatures() != null) {
                for (com.mogu.data.feature.entity.FeatureDefinition.FeatureSpec feature : apiView.getFeatures()) {
                    FeatureDefinition.FeatureSpec spec = new FeatureDefinition.FeatureSpec();
                    spec.setName(feature.getName());
                    spec.setDtype(feature.getDtype());
                    spec.setDescription(feature.getDescription());
                    spec.setTransformExpr(feature.getTransformExpr());
                    specs.add(spec);
                }
            }
        } catch (Exception e) {
            log.error("获取特征规格失败: {}", view.getName(), e);
        }

        return specs;
    }

    /**
     * 解析 JdbcTemplate
     */
    private JdbcTemplate resolveJdbcTemplate(FeatureView view) {
        Long dsId = view.getDatasourceId();
        if (dsId != null) {
            Optional<DataSource> dsOpt = dataSourceRepository.findById(dsId);
            if (dsOpt.isPresent()) {
                DataSource ds = dsOpt.get();
                return buildJdbcTemplate(ds);
            }
        }

        // 回退到默认 JdbcTemplate（从 Spring 上下文获取）
        // 注意：这里需要注入默认的 JdbcTemplate，但当前没有
        // 暂时抛出异常，要求必须配置数据源
        throw new BusinessException("特征视图 " + view.getName() + " 未配置有效数据源");
    }

    /**
     * 根据数据源配置构建 JdbcTemplate
     */
    private JdbcTemplate buildJdbcTemplate(DataSource ds) {
        DriverManagerDataSource dataSource = new DriverManagerDataSource();
        dataSource.setUrl(buildJdbcUrl(ds));
        dataSource.setUsername(ds.getUsername());
        dataSource.setPassword(decryptPassword(ds.getPasswordEncrypted()));

        if ("mysql".equalsIgnoreCase(ds.getType())) {
            dataSource.setDriverClassName("com.mysql.cj.jdbc.Driver");
        } else {
            dataSource.setDriverClassName("org.postgresql.Driver");
        }

        return new JdbcTemplate(dataSource);
    }

    /**
     * 构建 JDBC URL
     */
    private String buildJdbcUrl(DataSource ds) {
        String type = ds.getType();
        String host = ds.getHost();
        Integer port = ds.getPort();
        String database = ds.getDatabaseName();

        if ("mysql".equalsIgnoreCase(type)) {
            return String.format("jdbc:mysql://%s:%d/%s?useSSL=false&serverTimezone=Asia/Shanghai",
                    host, port != null ? port : 3306, database);
        } else {
            return String.format("jdbc:postgresql://%s:%d/%s",
                    host, port != null ? port : 5432, database);
        }
    }

    /**
     * 解密密码
     */
    private String decryptPassword(String encryptedPassword) {
        if (encryptedPassword == null || encryptedPassword.isEmpty()) {
            return null;
        }
        try {
            return com.mogu.data.common.util.EncryptionUtil.decrypt(encryptedPassword);
        } catch (Exception e) {
            log.warn("解密密码失败: {}", e.getMessage());
            return null;
        }
    }

    /**
     * 获取默认值
     */
    private Object getDefaultValue(FeatureDefinition.FeatureSpec spec) {
        String dtype = spec.getDtype();
        if (dtype == null) return null;

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
