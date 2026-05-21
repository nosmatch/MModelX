package com.mogu.data.feature.datasource;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.mogu.data.common.storage.MinioService;
import com.mogu.data.common.logger.Logger;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.time.LocalDate;
import java.util.*;

/**
 * MinIO Data Source Adapter
 *
 * 从MinIO读取Parquet/CSV/JSON文件
 *
 * 配置示例:
 * {
 *   "bucket": "raw-data",
 *   "path": "orders/{date}/",
 *   "format": "parquet",
 *   "entityColumn": "user_id"
 * }
 *
 * @author MModelX Team
 * @since 2026-05-20
 */
@Component
@RequiredArgsConstructor
public class MinioDataSourceAdapter implements DataSourceAdapter {

    private static final Logger log = Logger.getLogger(MinioDataSourceAdapter.class);

    private final MinioService minioService;
    private final ObjectMapper objectMapper;

    @Override
    public Map<String, List<Map<String, Object>>> readData(String config, LocalDate partitionDate) {
        try {
            Map<String, Object> configMap = objectMapper.readValue(config, Map.class);

            String bucket = (String) configMap.get("bucket");
            String pathTemplate = (String) configMap.get("path");
            String format = (String) configMap.getOrDefault("format", "parquet");
            String entityColumn = (String) configMap.get("entityColumn");

            // 替换日期占位符
            String path = pathTemplate.replace("{date}", partitionDate.toString());
            String objectKey = path.startsWith("/") ? path.substring(1) : path;

            log.info("Reading from MinIO: {}/{}", bucket, objectKey);

            // 从MinIO读取文件
            byte[] data;
            try (java.io.InputStream inputStream = minioService.downloadFile(bucket, objectKey)) {
                data = new byte[inputStream.available()];
                inputStream.read(data);
            }

            // 解析数据
            List<Map<String, Object>> rows = parseData(data, format);

            // 按entity_id分组
            Map<String, List<Map<String, Object>>> groupedData = new HashMap<>();
            for (Map<String, Object> row : rows) {
                Object entityId = row.get(entityColumn);
                if (entityId != null) {
                    String key = entityId.toString();
                    groupedData.computeIfAbsent(key, k -> new ArrayList<>()).add(row);
                }
            }

            log.info("Read {} rows for {} entities from MinIO",
                     rows.size(), groupedData.size());

            return groupedData;

        } catch (Exception e) {
            log.error("Failed to read data from MinIO", e);
            throw new RuntimeException("MinIO data read failed", e);
        }
    }

    /**
     * 解析数据
     */
    private List<Map<String, Object>> parseData(byte[] data, String format) {
        switch (format.toLowerCase()) {
            case "json":
                return parseJson(data);

            case "csv":
                return parseCsv(data);

            case "parquet":
                return parseParquet(data);

            default:
                throw new UnsupportedOperationException("Unsupported format: " + format);
        }
    }

    /**
     * 解析JSON格式
     */
    @SuppressWarnings("unchecked")
    private List<Map<String, Object>> parseJson(byte[] data) {
        try {
            return objectMapper.readValue(data, List.class);
        } catch (Exception e) {
            log.error("Failed to parse JSON", e);
            throw new RuntimeException("JSON parsing failed", e);
        }
    }

    /**
     * 解析CSV格式
     */
    private List<Map<String, Object>> parseCsv(byte[] data) {
        List<Map<String, Object>> rows = new ArrayList<>();
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(
                new java.io.ByteArrayInputStream(data)))) {

            String headerLine = reader.readLine();
            if (headerLine == null) {
                return rows;
            }

            String[] headers = headerLine.split(",");
            String line;
            while ((line = reader.readLine()) != null) {
                String[] values = line.split(",");
                Map<String, Object> row = new HashMap<>();
                for (int i = 0; i < headers.length && i < values.length; i++) {
                    row.put(headers[i].trim(), values[i].trim());
                }
                rows.add(row);
            }

        } catch (Exception e) {
            log.error("Failed to parse CSV", e);
            throw new RuntimeException("CSV parsing failed", e);
        }
        return rows;
    }

    /**
     * 解析Parquet格式
     * TODO: 实现真正的Parquet解析
     */
    private List<Map<String, Object>> parseParquet(byte[] data) {
        // 简化版本: 暂时先用JSON解析
        // 后续需要集成Apache Parquet库
        log.warn("Parquet parsing not fully implemented, using JSON as fallback");
        return parseJson(data);
    }

    @Override
    public boolean testConnection(String config) {
        try {
            Map<String, Object> configMap = objectMapper.readValue(config, Map.class);
            String bucket = (String) configMap.get("bucket");
            return minioService.bucketExists(bucket);
        } catch (Exception e) {
            log.error("MinIO connection test failed", e);
            return false;
        }
    }

    @Override
    public String getDescription() {
        return "MinIO Object Storage Adapter";
    }
}
