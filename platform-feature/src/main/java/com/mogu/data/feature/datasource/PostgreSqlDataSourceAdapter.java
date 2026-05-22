package com.mogu.data.feature.datasource;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.mogu.data.common.logger.Logger;
import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.DriverManagerDataSource;
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

            // 校验必要字段
            if (table == null || table.isEmpty()) {
                throw new IllegalArgumentException("数据源配置缺少 'table' 字段，请在特征视图中配置数据表");
            }
            if (entityColumn == null || entityColumn.isEmpty()) {
                throw new IllegalArgumentException("数据源配置缺少 'entityColumn' 字段，请在特征视图中配置实体字段");
            }
            if (dateColumn == null || dateColumn.isEmpty()) {
                throw new IllegalArgumentException("数据源配置缺少 'dateColumn' 字段，请在特征视图中配置日期字段");
            }

            // 构建SQL查询
            String sql = buildSelectQuery(table, columns, dateColumn, partitionDate);

            log.info("Executing SQL: {}", sql);
            log.debug("Reading from table: {} for date: {}", table, partitionDate);

            // 根据配置解析目标 JdbcTemplate
            JdbcTemplate targetTemplate = resolveJdbcTemplate(configMap);

            // 执行查询
            List<Map<String, Object>> rows = targetTemplate.queryForList(sql);

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

    /**
     * 根据配置解析目标 JdbcTemplate。
     * 如果配置中包含 jdbcUrl、username、password，则动态创建连接；否则回退到默认 JdbcTemplate。
     */
    private JdbcTemplate resolveJdbcTemplate(Map<String, Object> configMap) {
        String jdbcUrl = (String) configMap.get("jdbcUrl");
        String username = (String) configMap.get("username");
        String password = (String) configMap.get("password");

        if (jdbcUrl != null && !jdbcUrl.isEmpty() && username != null) {
            DriverManagerDataSource ds = new DriverManagerDataSource();
            ds.setUrl(jdbcUrl);
            ds.setUsername(username);
            ds.setPassword(password);

            // 根据 URL 前缀自动判断驱动类名
            if (jdbcUrl.startsWith("jdbc:mysql:")) {
                ds.setDriverClassName("com.mysql.cj.jdbc.Driver");
                log.info("Using dynamic MySQL connection: {}", jdbcUrl);
            } else if (jdbcUrl.startsWith("jdbc:postgresql:")) {
                ds.setDriverClassName("org.postgresql.Driver");
                log.info("Using dynamic PostgreSQL connection: {}", jdbcUrl);
            } else {
                log.warn("Unknown JDBC URL prefix, attempting with default driver: {}", jdbcUrl);
            }

            return new JdbcTemplate(ds);
        }

        log.debug("Using default JdbcTemplate (no dynamic connection config found)");
        return jdbcTemplate;
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
