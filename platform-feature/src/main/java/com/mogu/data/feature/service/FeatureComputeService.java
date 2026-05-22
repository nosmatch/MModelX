package com.mogu.data.feature.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.mogu.data.common.entity.FeatureView;
import com.mogu.data.common.exception.BusinessException;
import com.mogu.data.common.storage.MinioService;
import com.mogu.data.common.storage.RedisService;
import com.mogu.data.common.logger.Logger;
import com.mogu.data.common.repository.DataSourceRepository;
import com.mogu.data.feature.datasource.DataSourceAdapter;
import com.mogu.data.feature.datasource.DataSourceFactory;
import com.mogu.data.feature.datasource.DataSourceType;
import com.mogu.data.feature.engine.FeatureComputeEngine;
import com.mogu.data.feature.entity.FeatureDefinition;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.concurrent.TimeUnit;

/**
 * Feature Compute Service
 *
 * 特征计算服务，实现特征计算和物化的完整流程
 * - 离线特征计算并存储到MinIO
 * - 在线特征物化到Redis
 *
 * @author MModelX Team
 * @since 2026-05-20
 */
@Service
@RequiredArgsConstructor
public class FeatureComputeService {

    private static final Logger log = Logger.getLogger(FeatureComputeService.class);
    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("yyyyMMdd");
    private static final String FEATURES_BUCKET = "features";

    private final FeatureComputeEngine computeEngine;
    private final DataSourceFactory dataSourceFactory;
    private final MinioService minioService;
    private final RedisService redisService;
    private final FeatureRegistryService featureRegistryService;
    private final DataSourceRepository dataSourceRepository;
    private final ObjectMapper objectMapper;

    /**
     * 计算特征
     *
     * @param definition 特征定义
     * @param inputPath 输入路径（暂时未使用，保留兼容性）
     * @param outputPath 输出路径
     */
    public void computeFeatures(FeatureDefinition definition, String inputPath, String outputPath) {
        String featureViewName = definition.getFeatureView();
        LocalDate partitionDate = LocalDate.now(); // 默认使用今天
        if (outputPath == null || outputPath.isEmpty()) {
            outputPath = buildOutputPath(featureViewName, partitionDate);
        }
        log.info("开始计算特征: {}, 输出路径: {}", featureViewName, outputPath);

        try {
            // 1. 从数据源读取数据
            DataSourceType sourceType = DataSourceType.fromString(definition.getSource().getType());
            DataSourceAdapter adapter = dataSourceFactory.getAdapter(sourceType);

            // 将Map配置转换为JSON字符串，并合并数据源连接信息
            Map<String, Object> sourceConfig = definition.getSource().getConfig();
            if (sourceConfig == null) {
                sourceConfig = new HashMap<>();
            }
            enrichWithDataSourceConnection(sourceConfig, featureViewName);
            String configJson = convertConfigToJson(sourceConfig);

            Map<String, List<Map<String, Object>>> rawData = adapter.readData(configJson, partitionDate);

            log.info("从数据源读取了 {} 个实体的数据", rawData.size());

            // 2. 按实体计算特征
            Map<String, Map<String, Object>> computedFeatures = new HashMap<>();
            for (Map.Entry<String, List<Map<String, Object>>> entry : rawData.entrySet()) {
                String entityId = entry.getKey();
                List<Map<String, Object>> entityData = entry.getValue();

                Map<String, Object> features = computeEngine.compute(
                    entityData,
                    definition.getFeatures()
                );

                // 添加元数据
                features.put("entity_id", entityId);
                features.put("computed_at", System.currentTimeMillis());

                computedFeatures.put(entityId, features);
            }

            log.info("特征计算完成，计算了 {} 个实体的特征", computedFeatures.size());

            // 3. 写入MinIO（当前使用JSON格式）
            writeToMinio(computedFeatures, outputPath);

            log.info("特征已写入MinIO: {}", outputPath);

        } catch (BusinessException e) {
            log.error("特征计算失败，特征视图: " + featureViewName);
            throw e;
        } catch (Exception e) {
            log.error("特征计算失败，特征视图: " + featureViewName);
            String msg = "特征计算失败: " + (e.getMessage() != null ? e.getMessage() : "未知错误");
            throw new BusinessException("500", msg, e);
        }
    }

    /**
     * 计算特征（使用日期参数）
     *
     * @param definition 特征定义
     * @param partitionDate 分区日期
     */
    public void computeFeatures(FeatureDefinition definition, LocalDate partitionDate) {
        String outputPath = buildOutputPath(definition.getFeatureView(), partitionDate);
        computeFeatures(definition, null, outputPath);
    }

    /**
     * 批量计算特征
     *
     * @param definitions 特征定义列表
     */
    public void batchComputeFeatures(List<FeatureDefinition> definitions) {
        log.info("开始批量计算特征，数量: {}", definitions.size());

        LocalDate today = LocalDate.now();
        int successCount = 0;
        int failCount = 0;

        for (FeatureDefinition definition : definitions) {
            try {
                computeFeatures(definition, today);
                successCount++;

            } catch (Exception e) {
                log.error("计算特征失败: " + definition.getFeatureView() + ", 错误: " + e.getMessage());
                failCount++;
            }
        }

        log.info("批量特征计算完成，成功: {}, 失败: {}", successCount, failCount);
    }

    /**
     * 物化特征到Redis（在线特征）
     *
     * @param featureViewName 特征视图名称
     */
    public void materializeFeatures(String featureViewName) {
        log.info("开始物化特征到Redis: {}", featureViewName);

        try {
            // 1. 获取特征视图定义
            FeatureView featureView = getFeatureViewById(featureViewName);
            log.info("特征视图TTL: {} 天", featureView.getTtl());

            // 2. 从MinIO读取最新的特征文件
            String latestPath = getLatestFeaturePath(featureViewName);
            log.info("从MinIO读取特征文件: {}", latestPath);

            List<Map<String, Object>> features = readFromMinio(latestPath);
            log.info("读取了 {} 条特征记录", features.size());

            // 3. 写入Redis
            int ttl = featureView.getTtl() != null ? featureView.getTtl() : 30;
            String entity = featureView.getEntity();
            String prefix = "feature:" + entity + ":";

            int writeCount = 0;
            for (Map<String, Object> featureRecord : features) {
                Object entityIdObj = featureRecord.get("entity_id");
                if (entityIdObj == null) {
                    continue;
                }

                String entityId = entityIdObj.toString();

                // 写入每个特征字段
                for (Map.Entry<String, Object> entry : featureRecord.entrySet()) {
                    String key = entry.getKey();

                    // 跳过元数据字段
                    if ("entity_id".equals(key) || "computed_at".equals(key)) {
                        continue;
                    }

                    String redisKey = prefix + entityId + ":" + key;
                    redisService.set(redisKey, entry.getValue(), ttl, TimeUnit.DAYS);
                    writeCount++;
                }
            }

            log.info("特征物化完成，写入了 {} 个Redis key", writeCount);

        } catch (BusinessException e) {
            log.error("特征物化失败，特征视图: " + featureViewName);
            throw e;
        } catch (Exception e) {
            log.error("特征物化失败，特征视图: " + featureViewName);
            String msg = "特征物化失败: " + (e.getMessage() != null ? e.getMessage() : "未知错误");
            throw new BusinessException("500", msg, e);
        }
    }

    /**
     * 获取在线特征
     *
     * @param entityType 实体类型
     * @param entityId 实体ID
     * @param featureNames 特征名称列表
     * @return 特征值Map
     */
    public Map<String, Object> getOnlineFeatures(String entityType, String entityId, List<String> featureNames) {
        log.info("获取在线特征: entityType={}, entityId={}, features={}",
                entityType, entityId, featureNames);

        try {
            Map<String, Object> features = new HashMap<>();
            String prefix = "feature:" + entityType + ":" + entityId + ":";

            for (String featureName : featureNames) {
                String key = prefix + featureName;
                Object value = redisService.get(key);

                if (value != null) {
                    features.put(featureName, value);
                } else {
                    log.warn("特征未找到: " + key);
                }
            }

            log.info("获取到 {} 个在线特征", features.size());
            return features;

        } catch (BusinessException e) {
            log.error("获取在线特征失败");
            throw e;
        } catch (Exception e) {
            log.error("获取在线特征失败");
            String msg = "获取在线特征失败: " + (e.getMessage() != null ? e.getMessage() : "未知错误");
            throw new BusinessException("500", msg, e);
        }
    }

    /**
     * 根据名称获取数据库FeatureView
     */
    private FeatureView getFeatureById(String featureViewName) {
        try {
            List<com.mogu.data.feature.entity.FeatureView> apiViews = featureRegistryService.listFeatureViews();
            for (com.mogu.data.feature.entity.FeatureView apiView : apiViews) {
                if (apiView.getName().equals(featureViewName)) {
                    return createDbFeatureView(apiView);
                }
            }
            throw new BusinessException("特征视图不存在: " + featureViewName);
        } catch (Exception e) {
            log.error("获取FeatureView失败: " + featureViewName);
            throw new BusinessException("特征视图不存在: " + featureViewName);
        }
    }

    /**
     * 根据名称获取数据库FeatureView（内部使用）
     */
    private FeatureView getFeatureViewById(String featureViewName) {
        // 调用getFeatureView(Long)方法
        // 先通过API层获取FeatureView，然后转换
        List<com.mogu.data.feature.entity.FeatureView> apiViews = featureRegistryService.listFeatureViews();

        for (com.mogu.data.feature.entity.FeatureView apiView : apiViews) {
            if (apiView.getName().equals(featureViewName)) {
                // 创建数据库FeatureView实体
                return createDbFeatureView(apiView);
            }
        }

        throw new BusinessException("特征视图不存在: " + featureViewName);
    }

    /**
     * 将API FeatureView转换为数据库FeatureView
     */
    private FeatureView createDbFeatureView(com.mogu.data.feature.entity.FeatureView apiView) {
        FeatureView dbView = new FeatureView();
        dbView.setId(apiView.getId());
        dbView.setName(apiView.getName());
        dbView.setEntity(apiView.getEntity());
        dbView.setTtl(apiView.getTtl());
        dbView.setDescription(apiView.getDescription());

        // 设置状态
        if (apiView.getStatus() != null) {
            dbView.setStatus(FeatureView.FeatureViewStatus.valueOf(apiView.getStatus()));
        }

        return dbView;
    }

    /**
     * 写入数据到MinIO
     *
     * @param features 特征数据
     * @param path 对象路径
     */
    private void writeToMinio(Map<String, Map<String, Object>> features, String path) {
        try {
            // 转换为JSON格式（临时方案，后续使用Parquet）
            byte[] jsonData = convertToJson(features);

            try (ByteArrayInputStream inputStream = new ByteArrayInputStream(jsonData)) {
                minioService.uploadFile(FEATURES_BUCKET, path, inputStream, jsonData.length, "application/json");
            }

        } catch (Exception e) {
            log.error("写入MinIO失败: " + e.getMessage());
            throw new RuntimeException("写入MinIO失败", e);
        }
    }

    /**
     * 从MinIO读取数据
     *
     * @param path 对象路径
     * @return 特征数据列表
     */
    private List<Map<String, Object>> readFromMinio(String path) {
        InputStream inputStream = null;
        try {
            inputStream = minioService.downloadFile(FEATURES_BUCKET, path);
            byte[] data = new byte[inputStream.available()];
            inputStream.read(data);

            return parseJson(data);

        } catch (Exception e) {
            log.error("从MinIO读取失败: " + e.getMessage());
            throw new RuntimeException("从MinIO读取失败", e);
        } finally {
            if (inputStream != null) {
                try {
                    inputStream.close();
                } catch (Exception e) {
                    // ignore
                }
            }
        }
    }

    /**
     * 转换为JSON字节数组
     */
    private byte[] convertToJson(Map<String, Map<String, Object>> features) {
        try {
            List<Map<String, Object>> featureList = new ArrayList<>(features.values());
            return objectMapper.writeValueAsBytes(featureList);
        } catch (Exception e) {
            log.error("JSON转换失败: " + e.getMessage());
            throw new RuntimeException("JSON转换失败", e);
        }
    }

    /**
     * 解析JSON数据
     */
    @SuppressWarnings("unchecked")
    private List<Map<String, Object>> parseJson(byte[] data) {
        try {
            return objectMapper.readValue(data, List.class);
        } catch (Exception e) {
            log.error("JSON解析失败: " + e.getMessage());
            throw new RuntimeException("JSON解析失败", e);
        }
    }

    /**
     * 构建输出路径
     *
     * @param featureViewName 特征视图名称
     * @param partitionDate 分区日期
     * @return 对象路径
     */
    private String buildOutputPath(String featureViewName, LocalDate partitionDate) {
        String dateStr = partitionDate.format(DATE_FORMATTER);
        return String.format("%s/%s/features_%s.json", featureViewName, dateStr, dateStr);
    }

    /**
     * 将Map配置转换为JSON字符串
     *
     * @param configMap 配置Map
     * @return JSON字符串
     */
    @SuppressWarnings("unchecked")
    private String convertConfigToJson(Map<String, Object> configMap) {
        try {
            return objectMapper.writeValueAsString(configMap);
        } catch (Exception e) {
            log.error("配置转换失败");
            throw new BusinessException("500", "配置转换失败", e);
        }
    }

    /**
     * 从特征视图关联的数据源中获取连接信息，合并到sourceConfig中
     */
    private void enrichWithDataSourceConnection(Map<String, Object> sourceConfig, String featureViewName) {
        try {
            com.mogu.data.feature.entity.FeatureView apiView = featureRegistryService.getFeatureView(featureViewName);
            Long dsId = apiView.getDatasourceId();
            if (dsId != null) {
                com.mogu.data.common.entity.DataSource ds = dataSourceRepository.findById(dsId).orElse(null);
                if (ds != null) {
                    String jdbcUrl = buildJdbcUrl(ds);
                    String password = decryptPassword(ds.getPasswordEncrypted());
                    sourceConfig.put("jdbcUrl", jdbcUrl);
                    sourceConfig.put("username", ds.getUsername());
                    sourceConfig.put("password", password);
                    sourceConfig.put("dbType", ds.getType());
                    log.debug("Enriched source config with data source connection: {} ({})", ds.getName(), jdbcUrl);
                } else {
                    log.warn("Data source not found for id: {}", dsId);
                }
            } else {
                log.warn("Feature view '{}' has no datasourceId, using default connection", featureViewName);
            }
        } catch (Exception e) {
            log.warn("Failed to enrich source config with data source connection: {}", e.getMessage());
        }
    }

    /**
     * 根据数据源信息构建 JDBC URL
     */
    private String buildJdbcUrl(com.mogu.data.common.entity.DataSource ds) {
        String type = ds.getType();
        String host = ds.getHost();
        Integer port = ds.getPort();
        String database = ds.getDatabaseName();

        if ("mysql".equalsIgnoreCase(type)) {
            return String.format("jdbc:mysql://%s:%d/%s?useSSL=false&serverTimezone=Asia/Shanghai",
                host, port != null ? port : 3306, database);
        } else if ("postgresql".equalsIgnoreCase(type)) {
            return String.format("jdbc:postgresql://%s:%d/%s",
                host, port != null ? port : 5432, database);
        }

        // 其他类型，尝试使用 host 作为完整 URL
        if (host != null && host.startsWith("jdbc:")) {
            return host;
        }

        log.warn("Unable to build JDBC URL for type: {}, host: {}", type, host);
        return null;
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
            log.warn("Failed to decrypt password: {}", e.getMessage());
            return null;
        }
    }

    /**
     * 获取最新的特征文件路径
     *
     * @param featureViewName 特征视图名称
     * @return 对象路径
     */
    private String getLatestFeaturePath(String featureViewName) {
        try {
            // 1. 列出MinIO中该特征视图的所有文件
            List<String> files = minioService.listObjects(FEATURES_BUCKET);

            if (files == null || files.isEmpty()) {
                // 如果没有文件，fallback到今天的日期
                String today = LocalDate.now().format(DATE_FORMATTER);
                return String.format("%s/%s/features_%s.json", featureViewName, today, today);
            }

            // 2. 过滤出该特征视图的文件并按日期排序，取最新的
            // 文件格式: featureViewName/YYYYMMDD/features_YYYYMMDD.json
            String latestFile = files.stream()
                .filter(f -> f.startsWith(featureViewName + "/"))
                .filter(f -> f.contains("features_"))
                .max((f1, f2) -> {
                    // 提取日期部分进行比较
                    String date1 = extractDateFromPath(f1);
                    String date2 = extractDateFromPath(f2);
                    return date1.compareTo(date2);
                })
                .orElse(null);

            if (latestFile != null) {
                log.info("最新的特征文件: {}", latestFile);
                return latestFile;
            } else {
                // 如果没有找到，fallback到今天的日期
                String today = LocalDate.now().format(DATE_FORMATTER);
                return String.format("%s/%s/features_%s.json", featureViewName, today, today);
            }

        } catch (Exception e) {
            log.warn("列出MinIO文件失败，使用今天的日期: {}", e.getMessage());
            String today = LocalDate.now().format(DATE_FORMATTER);
            return String.format("%s/%s/features_%s.json", featureViewName, today, today);
        }
    }

    /**
     * 从文件路径中提取日期
     */
    private String extractDateFromPath(String path) {
        // 路径格式: featureViewName/YYYYMMDD/features_YYYYMMDD.json
        String[] parts = path.split("/");
        for (String part : parts) {
            if (part.matches("\\d{8}")) {
                return part;
            }
        }
        return "00000000"; // 如果找不到日期，返回最小值
    }

    /**
     * 检查特征是否已物化
     *
     * @param featureViewName 特征视图名称
     * @return 是否已物化
     */
    public boolean isFeatureMaterialized(String featureViewName) {
        try {
            String latestPath = getLatestFeaturePath(featureViewName);
            // 尝试下载文件来判断是否存在
            InputStream testStream = minioService.downloadFile(FEATURES_BUCKET, latestPath);
            if (testStream != null) {
                testStream.close();
                return true;
            }
            return false;
        } catch (Exception e) {
            log.error("检查特征物化状态失败: " + e.getMessage());
            return false;
        }
    }

    /**
     * 获取特征物化统计信息
     *
     * @param featureViewName 特征视图名称
     * @return 统计信息Map
     */
    public Map<String, Object> getMaterializationStats(String featureViewName) {
        Map<String, Object> stats = new HashMap<>();

        try {
            // 检查MinIO中的文件
            List<String> files = minioService.listObjects(FEATURES_BUCKET);
            long fileCount = files.stream()
                .filter(f -> f.startsWith(featureViewName + "/"))
                .count();

            stats.put("minioFileCount", fileCount);

            // 检查Redis中的key（简化版本：只检查一个示例key是否存在）
            String entity = getEntityFromFeatureView(featureViewName);
            String testKey = "feature:" + entity + ":test";
            boolean redisAccessible = redisService.exists(testKey);
            stats.put("redisAccessible", redisAccessible);
            stats.put("redisKeyCount", "N/A"); // 需要scan命令支持

            stats.put("status", "success");

        } catch (Exception e) {
            log.error("获取物化统计信息失败: " + e.getMessage());
            stats.put("status", "failed");
            stats.put("error", e.getMessage());
        }

        return stats;
    }

    /**
     * 从特征视图名称获取实体类型
     */
    private String getEntityFromFeatureView(String featureViewName) {
        FeatureView featureView = getFeatureViewById(featureViewName);
        return featureView.getEntity();
    }
}
