package com.mogu.data.feature.datasource;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.mogu.data.common.logger.Logger;
import lombok.RequiredArgsConstructor;
import org.springframework.http.*;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

import java.time.LocalDate;
import java.util.*;

/**
 * API Data Source Adapter
 *
 * 通过HTTP API获取数据
 *
 * 配置示例:
 * {
 *   "url": "http://api.example.com/users",
 *   "method": "GET",
 *   "headers": {"Authorization": "Bearer token"},
 *   "entityField": "userId",
 *   "requestBody": {...},  // POST/PUT时使用
 *   "queryParams": {"date": "2026-05-20"}  // URL查询参数
 * }
 *
 * @author MModelX Team
 * @since 2026-05-20
 */
@Component
@RequiredArgsConstructor
public class ApiDataSourceAdapter implements DataSourceAdapter {

    private static final Logger log = Logger.getLogger(ApiDataSourceAdapter.class);

    private final RestTemplate restTemplate;
    private final ObjectMapper objectMapper;

    @Override
    public Map<String, List<Map<String, Object>>> readData(String config, LocalDate partitionDate) {
        try {
            Map<String, Object> configMap = objectMapper.readValue(config, Map.class);

            String url = (String) configMap.get("url");
            String method = (String) configMap.getOrDefault("method", "GET");
            String entityField = (String) configMap.get("entityField");

            @SuppressWarnings("unchecked")
            Map<String, String> headersConfig = (Map<String, String>) configMap.getOrDefault("headers", Collections.emptyMap());

            @SuppressWarnings("unchecked")
            Map<String, Object> queryParams = (Map<String, Object>) configMap.getOrDefault("queryParams", Collections.emptyMap());

            @SuppressWarnings("unchecked")
            Map<String, Object> requestBody = (Map<String, Object>) configMap.get("requestBody");

            log.info("Calling API: {} with method: {}", url, method);

            // 构建HTTP请求
            HttpHeaders httpHeaders = new HttpHeaders();
            headersConfig.forEach(httpHeaders::add);

            // 添加默认Content-Type
            if (!httpHeaders.containsKey(HttpHeaders.CONTENT_TYPE)) {
                httpHeaders.setContentType(MediaType.APPLICATION_JSON);
            }

            HttpEntity<?> entity;
            if ("POST".equalsIgnoreCase(method) || "PUT".equalsIgnoreCase(method) || "PATCH".equalsIgnoreCase(method)) {
                entity = new HttpEntity<>(requestBody, httpHeaders);
            } else {
                entity = new HttpEntity<>(httpHeaders);
            }

            // 添加查询参数
            String fullUrl = url;
            if (!queryParams.isEmpty()) {
                fullUrl = buildUrlWithParams(url, queryParams);
            }

            // 发送HTTP请求
            ResponseEntity<Object> response = restTemplate.exchange(
                fullUrl,
                HttpMethod.valueOf(method.toUpperCase()),
                entity,
                Object.class
            );

            // 解析响应数据
            Object responseBody = response.getBody();
            List<Map<String, Object>> responseData = parseResponseData(responseBody);

            log.info("Received {} records from API", responseData.size());

            // 按entityField分组
            Map<String, List<Map<String, Object>>> groupedData = new HashMap<>();
            for (Map<String, Object> record : responseData) {
                Object entityId = record.get(entityField);
                if (entityId != null) {
                    String key = entityId.toString();
                    groupedData.computeIfAbsent(key, k -> new ArrayList<>()).add(record);
                }
            }

            log.info("Grouped into {} entities", groupedData.size());

            return groupedData;

        } catch (Exception e) {
            log.error("Failed to read data from API", e);
            throw new RuntimeException("API data read failed: " + e.getMessage(), e);
        }
    }

    /**
     * 解析响应数据
     * 支持直接返回数组或包装在data字段中
     */
    @SuppressWarnings("unchecked")
    private List<Map<String, Object>> parseResponseData(Object responseBody) {
        if (responseBody == null) {
            return Collections.emptyList();
        }

        // 如果直接是数组
        if (responseBody instanceof List) {
            return (List<Map<String, Object>>) responseBody;
        }

        // 如果是Map，可能包含data、items、results等字段
        if (responseBody instanceof Map) {
            Map<String, Object> responseMap = (Map<String, Object>) responseBody;

            // 尝试常见的字段名
            String[] possibleFields = {"data", "items", "results", "records", "list"};
            for (String field : possibleFields) {
                Object data = responseMap.get(field);
                if (data instanceof List) {
                    return (List<Map<String, Object>>) data;
                }
            }

            log.warn("Response is a Map but no data field found, returning empty list");
            return Collections.emptyList();
        }

        log.warn("Unsupported response type: {}", responseBody.getClass());
        return Collections.emptyList();
    }

    /**
     * 构建带查询参数的URL
     */
    private String buildUrlWithParams(String baseUrl, Map<String, Object> params) {
        if (params.isEmpty()) {
            return baseUrl;
        }

        StringBuilder urlBuilder = new StringBuilder(baseUrl);
        if (!baseUrl.contains("?")) {
            urlBuilder.append("?");
        } else {
            urlBuilder.append("&");
        }

        boolean first = true;
        for (Map.Entry<String, Object> entry : params.entrySet()) {
            if (!first) {
                urlBuilder.append("&");
            }
            urlBuilder.append(entry.getKey())
                      .append("=")
                      .append(entry.getValue() != null ? entry.getValue().toString() : "");
            first = false;
        }

        return urlBuilder.toString();
    }

    @Override
    public boolean testConnection(String config) {
        try {
            Map<String, Object> configMap = objectMapper.readValue(config, Map.class);
            String url = (String) configMap.get("url");
            String method = (String) configMap.getOrDefault("method", "GET");

            // 构建简单的测试请求
            HttpHeaders headers = new HttpHeaders();
            HttpEntity<?> entity = new HttpEntity<>(headers);

            ResponseEntity<String> response = restTemplate.exchange(
                url,
                HttpMethod.valueOf(method.toUpperCase()),
                entity,
                String.class
            );

            boolean success = response.getStatusCode().is2xxSuccessful();
            log.info("API connection test: {}, status: {}", url, response.getStatusCode());

            return success;

        } catch (Exception e) {
            log.error("API connection test failed", e);
            return false;
        }
    }

    @Override
    public String getDescription() {
        return "HTTP API Adapter - supports GET, POST, PUT, PATCH, DELETE";
    }
}
