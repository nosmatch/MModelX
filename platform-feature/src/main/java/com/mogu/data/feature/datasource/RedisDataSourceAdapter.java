package com.mogu.data.feature.datasource;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.mogu.data.common.storage.RedisService;
import com.mogu.data.common.logger.Logger;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.util.*;

/**
 * Redis Data Source Adapter
 *
 * 从Redis读取数据
 *
 * 配置示例:
 * {
 *   "keyPattern": "user:*",           // Redis key模式
 *   "entityField": "userId",          // 实体ID字段名
 *   "dataType": "string",             // 数据类型: string, hash, list
 *   "flatten": true                   // 是否将hash字段展平为独立记录
 * }
 *
 * @author MModelX Team
 * @since 2026-05-20
 */
@Component
@RequiredArgsConstructor
public class RedisDataSourceAdapter implements DataSourceAdapter {

    private static final Logger log = Logger.getLogger(RedisDataSourceAdapter.class);

    private final RedisService redisService;
    private final ObjectMapper objectMapper;

    @Override
    public Map<String, List<Map<String, Object>>> readData(String config, LocalDate partitionDate) {
        try {
            Map<String, Object> configMap = objectMapper.readValue(config, Map.class);

            String keyPattern = (String) configMap.get("keyPattern");
            String entityField = (String) configMap.getOrDefault("entityField", "entityId");
            String dataType = (String) configMap.getOrDefault("dataType", "auto");
            boolean flatten = (Boolean) configMap.getOrDefault("flatten", true);

            log.info("Reading from Redis with pattern: {}, type: {}", keyPattern, dataType);

            // Scan所有匹配的key
            Set<String> keys = redisService.scan(keyPattern);
            log.info("Found {} keys matching pattern: {}", keys.size(), keyPattern);

            // 批量获取数据
            Map<String, List<Map<String, Object>>> groupedData = new HashMap<>();

            if ("hash".equalsIgnoreCase(dataType) || "auto".equalsIgnoreCase(dataType)) {
                // 处理Hash类型数据
                processHashData(keys, entityField, flatten, groupedData);
            }

            if ("string".equalsIgnoreCase(dataType)) {
                // 处理String类型数据
                processStringData(keys, entityField, groupedData);
            }

            if ("list".equalsIgnoreCase(dataType)) {
                // 处理List类型数据
                processListData(keys, entityField, groupedData);
            }

            log.info("Grouped into {} entities", groupedData.size());

            return groupedData;

        } catch (Exception e) {
            log.error("Failed to read data from Redis", e);
            throw new RuntimeException("Redis data read failed: " + e.getMessage(), e);
        }
    }

    /**
     * 处理Hash类型数据
     */
    private void processHashData(Set<String> keys, String entityField,
                                 boolean flatten, Map<String, List<Map<String, Object>>> groupedData) {

        for (String key : keys) {
            try {
                // 提取entity_id（从key中或从hash字段中）
                String entityId = extractEntityId(key, entityField);

                // 获取hash的所有字段
                Map<Object, Object> hashData = redisService.hGetAll(key);

                if (flatten) {
                    // 将每个hash字段作为独立的记录
                    for (Map.Entry<Object, Object> entry : hashData.entrySet()) {
                        Map<String, Object> record = new HashMap<>();
                        record.put("key", key);
                        record.put("field", entry.getKey().toString());
                        record.put("value", entry.getValue());
                        record.put(entityField, entityId);

                        groupedData.computeIfAbsent(entityId, k -> new ArrayList<>()).add(record);
                    }
                } else {
                    // 将整个hash作为一个记录
                    Map<String, Object> record = new HashMap<>();
                    record.put("key", key);
                    // 转换Map<Object, Object>到Map<String, Object>
                    hashData.forEach((k, v) -> record.put(k.toString(), v));
                    record.put(entityField, entityId);

                    groupedData.computeIfAbsent(entityId, k -> new ArrayList<>()).add(record);
                }

            } catch (Exception e) {
                log.warn("Failed to process hash key: {}", key, e);
            }
        }
    }

    /**
     * 处理String类型数据
     */
    private void processStringData(Set<String> keys, String entityField,
                                   Map<String, List<Map<String, Object>>> groupedData) {

        Map<String, Object> keyValueMap = redisService.multiGet(keys);

        for (Map.Entry<String, Object> entry : keyValueMap.entrySet()) {
            try {
                String key = entry.getKey();
                Object value = entry.getValue();

                if (value == null) {
                    continue;
                }

                String entityId = extractEntityId(key, entityField);

                Map<String, Object> record = new HashMap<>();
                record.put("key", key);
                record.put("value", value);
                record.put(entityField, entityId);

                groupedData.computeIfAbsent(entityId, k -> new ArrayList<>()).add(record);

            } catch (Exception e) {
                log.warn("Failed to process string key", e);
            }
        }
    }

    /**
     * 处理List类型数据
     */
    private void processListData(Set<String> keys, String entityField,
                                 Map<String, List<Map<String, Object>>> groupedData) {

        for (String key : keys) {
            try {
                String entityId = extractEntityId(key, entityField);

                // 获取list的所有元素
                List<Object> listData = redisService.lRange(key, 0, -1);

                // 将每个元素作为独立记录
                for (int i = 0; i < listData.size(); i++) {
                    Map<String, Object> record = new HashMap<>();
                    record.put("key", key);
                    record.put("index", i);
                    record.put("value", listData.get(i));
                    record.put(entityField, entityId);

                    groupedData.computeIfAbsent(entityId, k -> new ArrayList<>()).add(record);
                }

            } catch (Exception e) {
                log.warn("Failed to process list key: {}", key, e);
            }
        }
    }

    /**
     * 从key中提取entity_id
     */
    private String extractEntityId(String key, String entityField) {
        // 尝试从key中提取（例如: user:123:info -> 123）
        String[] parts = key.split(":");

        // 查找最像ID的部分（数字或特定模式）
        for (String part : parts) {
            if (part.matches("\\d+") || part.matches("[a-zA-Z0-9_-]{8,}")) {
                return part;
            }
        }

        // 如果找不到，返回整个key（去掉特殊字符）
        return key.replaceAll("[^a-zA-Z0-9_-]", "_");
    }

    @Override
    public boolean testConnection(String config) {
        try {
            Map<String, Object> configMap = objectMapper.readValue(config, Map.class);
            String keyPattern = (String) configMap.get("keyPattern");

            // 测试scan操作
            Set<String> keys = redisService.scan(keyPattern, 10);

            // 测试读写操作
            String testKey = "__feature_adapter_test__";
            redisService.set(testKey, "test", 10, java.util.concurrent.TimeUnit.SECONDS);
            Object value = redisService.get(testKey);
            redisService.delete(testKey);

            boolean success = "test".equals(value);
            log.info("Redis connection test: {}, found {} keys", success, keys.size());

            return success;

        } catch (Exception e) {
            log.error("Redis connection test failed", e);
            return false;
        }
    }

    @Override
    public String getDescription() {
        return "Redis Cache Adapter - supports string, hash, list data types";
    }
}
