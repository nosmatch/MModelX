package com.mogu.data.feature.datasource;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.mogu.data.common.logger.Logger;
import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.util.*;

/**
 * PostgreSQL Data Source Adapter
 *
 * 从PostgreSQL数据库表读取数据
 *
 * 配置示例:
 * {
 *   "table": "orders",
 *   "entityColumn": "user_id",
 *   "dateColumn": "order_date",
 *   "columns": ["user_id", "order_amount", "order_date"]
 * }
 *
 * @author MModelX Team
 * @since 2026-05-20
 */
@Component
@RequiredArgsConstructor
public class PostgreSqlDataSourceAdapter implements DataSourceAdapter {

    private static final Logger log = Logger.getLogger(PostgreSqlDataSourceAdapter.class);

    private final JdbcTemplate jdbcTemplate;
    private final ObjectMapper objectMapper;

    @Override
    public Map<String, List<Map<String, Object>>> readData(String config, LocalDate partitionDate) {
        try {
            Map<String, Object> configMap = objectMapper.readValue(config, Map.class);

            String table = (String) configMap.get("table");
            String entityColumn = (String) configMap.get("entityColumn");
            String dateColumn = (String) configMap.get("dateColumn");
            @SuppressWarnings("unchecked")
            List<String> columns = (List<String>) configMap.getOrDefault("columns", Collections.singletonList("*"));

            // 构建SQL查询
            String sql = buildSelectQuery(table, columns, dateColumn, partitionDate);

            log.info("Executing SQL: {}", sql);
            log.debug("Reading from table: {} for date: {}", table, partitionDate);

            // 执行查询
            List<Map<String, Object>> rows = jdbcTemplate.queryForList(sql);

            // 按entity_id分组
            Map<String, List<Map<String, Object>>> groupedData = new HashMap<>();
            for (Map<String, Object> row : rows) {
                Object entityId = row.get(entityColumn);
                if (entityId != null) {
                    String key = entityId.toString();
                    groupedData.computeIfAbsent(key, k -> new ArrayList<>()).add(row);
                }
            }

            log.info("Read {} rows for {} entities from table {}",
                     rows.size(), groupedData.size(), table);

            return groupedData;

        } catch (Exception e) {
            log.error("Failed to read data from PostgreSQL", e);
            throw new RuntimeException("PostgreSQL data read failed", e);
        }
    }

    /**
     * 构建SELECT查询
     */
    private String buildSelectQuery(String table, List<String> columns,
                                     String dateColumn, LocalDate partitionDate) {
        String columnList = String.join(", ", columns);
        return String.format(
            "SELECT %s FROM %s WHERE DATE(%s) = '%s' ORDER BY %s",
            columnList, table, dateColumn, partitionDate, dateColumn
        );
    }

    @Override
    public boolean testConnection(String config) {
        try {
            jdbcTemplate.queryForObject("SELECT 1", Integer.class);
            return true;
        } catch (Exception e) {
            log.error("PostgreSQL connection test failed", e);
            return false;
        }
    }

    @Override
    public String getDescription() {
        return "PostgreSQL Database Adapter";
    }
}
