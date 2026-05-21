package com.mogu.data.feature.datasource;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.mogu.data.common.logger.Logger;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.io.BufferedReader;
import java.io.File;
import java.io.InputStreamReader;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.time.LocalDate;
import java.util.*;

/**
 * Local File Data Source Adapter
 *
 * 从本地文件系统读取数据 (CSV, JSON, Excel)
 *
 * 配置示例:
 * {
 *   "path": "/data/orders/{date}.csv",
 *   "format": "csv",
 *   "entityColumn": "user_id"
 * }
 *
 * @author MModelX Team
 * @since 2026-05-20
 */
@Component
@RequiredArgsConstructor
public class LocalFileDataSourceAdapter implements DataSourceAdapter {

    private static final Logger log = Logger.getLogger(LocalFileDataSourceAdapter.class);

    private final ObjectMapper objectMapper;

    @Override
    public Map<String, List<Map<String, Object>>> readData(String config, LocalDate partitionDate) {
        try {
            Map<String, Object> configMap = objectMapper.readValue(config, Map.class);

            String pathTemplate = (String) configMap.get("path");
            String format = (String) configMap.getOrDefault("format", "csv");
            String entityColumn = (String) configMap.get("entityColumn");

            // 替换日期占位符
            String path = pathTemplate.replace("{date}", partitionDate.toString());

            log.info("Reading from local file: {}", path);

            File file = new File(path);
            if (!file.exists()) {
                log.warn("File not found: {}", path);
                return new HashMap<>();
            }

            // 根据格式读取文件
            List<Map<String, Object>> rows = readFile(file, format);

            // 按entity_id分组
            Map<String, List<Map<String, Object>>> groupedData = new HashMap<>();
            for (Map<String, Object> row : rows) {
                Object entityId = row.get(entityColumn);
                if (entityId != null) {
                    String key = entityId.toString();
                    groupedData.computeIfAbsent(key, k -> new ArrayList<>()).add(row);
                }
            }

            log.info("Read {} rows for {} entities from local file",
                     rows.size(), groupedData.size());

            return groupedData;

        } catch (Exception e) {
            log.error("Failed to read data from local file: {}", e.getMessage(), e);
            throw new RuntimeException("Local file data read failed", e);
        }
    }

    /**
     * 读取文件
     */
    private List<Map<String, Object>> readFile(File file, String format) {
        switch (format.toLowerCase()) {
            case "json":
                return readJsonFile(file);

            case "csv":
                return readCsvFile(file);

            case "excel":
                return readExcelFile(file);

            default:
                throw new UnsupportedOperationException("Unsupported file format: " + format);
        }
    }

    /**
     * 读取JSON文件
     */
    @SuppressWarnings("unchecked")
    private List<Map<String, Object>> readJsonFile(File file) {
        try {
            byte[] data = Files.readAllBytes(file.toPath());
            return objectMapper.readValue(data, List.class);
        } catch (Exception e) {
            log.error("Failed to read JSON file: {}", file.getPath(), e.getMessage(), e);
            throw new RuntimeException("JSON file parsing failed", e);
        }
    }

    /**
     * 读取CSV文件
     */
    private List<Map<String, Object>> readCsvFile(File file) {
        List<Map<String, Object>> rows = new ArrayList<>();
        try (BufferedReader reader = new BufferedReader(new java.io.FileReader(file))) {

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
            log.error("Failed to read CSV file: {}", file.getPath(), e.getMessage(), e);
            throw new RuntimeException("CSV file parsing failed", e);
        }
        return rows;
    }

    /**
     * 读取Excel文件
     * TODO: 实现Excel解析
     */
    private List<Map<String, Object>> readExcelFile(File file) {
        log.warn("Excel file parsing not implemented: {}", file.getPath());
        // TODO: 集成Apache POI库
        return new ArrayList<>();
    }

    @Override
    public boolean testConnection(String config) {
        try {
            Map<String, Object> configMap = objectMapper.readValue(config, Map.class);
            String pathTemplate = (String) configMap.get("path");
            String path = pathTemplate.replace("{date}", LocalDate.now().toString());

            File file = new File(path);
            return file.exists();

        } catch (Exception e) {
            log.error("Local file connection test failed", e);
            return false;
        }
    }

    @Override
    public String getDescription() {
        return "Local File System Adapter";
    }
}
