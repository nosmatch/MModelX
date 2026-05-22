package com.mogu.data.common.service;

import com.mogu.data.common.entity.DataSource;
import com.mogu.data.common.entity.FeatureView;
import com.mogu.data.common.entity.FeatureViewDataSource;
import com.mogu.data.common.exception.BusinessException;
import com.mogu.data.common.repository.DataSourceRepository;
import com.mogu.data.common.repository.FeatureViewDataSourceRepository;
import com.mogu.data.common.util.EncryptionUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 数据源管理服务
 *
 * 提供数据源的完整生命周期管理：
 * - CRUD 操作
 * - 连接测试
 * - 使用统计
 * - 安全管理（密码加密）
 *
 * @author MModelX Team
 * @since 2026-05-21
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class DataSourceService {

    private final DataSourceRepository dataSourceRepository;
    private final FeatureViewDataSourceRepository featureViewDataSourceRepository;

    /**
     * 创建数据源
     *
     * @param dataSource 数据源对象
     * @return 创建后的数据源ID
     * @throws BusinessException 如果名称已存在或验证失败
     */
    @Transactional
    public Long createDataSource(DataSource dataSource) {
        log.info("Creating datasource: {}", dataSource.getName());

        // 1. 验证名称唯一性
        dataSourceRepository.findByName(dataSource.getName())
            .ifPresent(existing -> {
                throw new BusinessException("数据源名称已存在: " + dataSource.getName());
            });

        // 2. 加密密码（如果提供了密码）
        if (dataSource.getPasswordEncrypted() != null && !dataSource.getPasswordEncrypted().isEmpty()) {
            try {
                String encrypted = EncryptionUtil.encrypt(dataSource.getPasswordEncrypted());
                dataSource.setPasswordEncrypted(encrypted);
                log.debug("Password encrypted for datasource: {}", dataSource.getName());
            } catch (Exception e) {
                throw new BusinessException("密码加密失败: " + e.getMessage());
            }
        }

        // 3. 保存到数据库
        DataSource saved = dataSourceRepository.save(dataSource);

        log.info("Datasource created successfully: {} (ID: {})", saved.getName(), saved.getId());
        return saved.getId();
    }

    /**
     * 更新数据源
     *
     * @param id 数据源ID
     * @param updates 更新的数据
     * @throws BusinessException 如果数据源不存在
     */
    @Transactional
    public void updateDataSource(Long id, DataSource updates) {
        log.info("Updating datasource: {}", id);

        DataSource dataSource = dataSourceRepository.findById(id)
            .orElseThrow(() -> new BusinessException("数据源不存在: " + id));

        // 1. 如果修改了密码，重新加密
        if (updates.getPasswordEncrypted() != null && !updates.getPasswordEncrypted().isEmpty()) {
            try {
                String encrypted = EncryptionUtil.encrypt(updates.getPasswordEncrypted());
                dataSource.setPasswordEncrypted(encrypted);
                log.debug("Password updated and re-encrypted for datasource: {}", dataSource.getName());
            } catch (Exception e) {
                throw new BusinessException("密码加密失败: " + e.getMessage());
            }
        }

        // 2. 更新其他字段
        if (updates.getName() != null) {
            // 检查新名称是否已被其他数据源使用
            dataSourceRepository.findByName(updates.getName())
                .filter(existing -> !existing.getId().equals(id))
                .ifPresent(existing -> {
                    throw new BusinessException("数据源名称已存在: " + updates.getName());
                });
            dataSource.setName(updates.getName());
        }

        if (updates.getType() != null) {
            dataSource.setType(updates.getType());
        }

        if (updates.getDescription() != null) {
            dataSource.setDescription(updates.getDescription());
        }

        if (updates.getHost() != null) {
            dataSource.setHost(updates.getHost());
        }

        if (updates.getPort() != null) {
            dataSource.setPort(updates.getPort());
        }

        if (updates.getDatabaseName() != null) {
            dataSource.setDatabaseName(updates.getDatabaseName());
        }

        if (updates.getUsername() != null) {
            dataSource.setUsername(updates.getUsername());
        }

        if (updates.getProperties() != null) {
            dataSource.setProperties(updates.getProperties());
        }

        if (updates.getStatus() != null) {
            dataSource.setStatus(updates.getStatus());
        }

        // 3. 保存更新
        dataSourceRepository.save(dataSource);

        log.info("Datasource updated successfully: {}", dataSource.getName());
    }

    /**
     * 删除数据源
     *
     * @param id 数据源ID
     * @throws BusinessException 如果数据源不存在或被特征视图使用
     */
    @Transactional
    public void deleteDataSource(Long id) {
        log.info("Deleting datasource: {}", id);

        // 1. 检查是否存在
        DataSource dataSource = dataSourceRepository.findById(id)
            .orElseThrow(() -> new BusinessException("数据源不存在: " + id));

        // 2. 检查是否被特征视图使用
        long usageCount = dataSourceRepository.countUsageByFeatureView(id);
        if (usageCount > 0) {
            // 查询使用该数据源的特征视图列表
            List<FeatureView> usedBy = featureViewDataSourceRepository.findActiveFeatureViewsByDatasourceId(id);
            String viewNames = usedBy.stream()
                .map(FeatureView::getName)
                .reduce((a, b) -> a + ", " + b)
                .orElse("");

            throw new BusinessException(
                String.format("数据源 '%s' 正被 %d 个特征视图使用，无法删除。使用的视图: %s",
                    dataSource.getName(), usageCount, viewNames)
            );
        }

        // 3. 删除数据源（级联删除相关的使用日志）
        dataSourceRepository.deleteById(id);

        log.info("Datasource deleted successfully: {}", dataSource.getName());
    }

    /**
     * 获取数据源详情
     *
     * @param id 数据源ID
     * @return 数据源对象
     * @throws BusinessException 如果数据源不存在
     */
    public DataSource getDataSource(Long id) {
        return dataSourceRepository.findById(id)
            .orElseThrow(() -> new BusinessException("数据源不存在: " + id));
    }

    /**
     * 获取所有非归档的数据源（包括 ACTIVE、DISABLED、ERROR）
     *
     * @return 数据源列表
     */
    public List<DataSource> getAllDataSources() {
        return dataSourceRepository.findAllNonArchivedOrderByCreatedAtDesc();
    }

    /**
     * 根据类型获取数据源
     *
     * @param type 数据源类型
     * @return 该类型的所有数据源
     */
    public List<DataSource> getDataSourcesByType(String type) {
        return dataSourceRepository.findByType(type);
    }

    /**
     * 搜索数据源
     *
     * @param keyword 搜索关键词
     * @return 匹配的数据源列表
     */
    public List<DataSource> searchDataSources(String keyword) {
        return dataSourceRepository.searchActiveDataSources(keyword);
    }

    /**
     * 测试数据源连接
     *
     * @param id 数据源ID
     * @return 测试结果
     * @throws BusinessException 如果数据源不存在
     */
    @Transactional
    public boolean testConnection(Long id) {
        log.info("Testing connection for datasource: {}", id);

        DataSource dataSource = getDataSource(id);

        try {
            String errorMsg = doTestConnection(dataSource);
            boolean result = errorMsg == null;

            // 更新测试结果
            dataSource.setLastTestedAt(LocalDateTime.now());
            dataSource.setLastTestResult(result);
            dataSource.setLastErrorMessage(result ? null : errorMsg);

            if (result) {
                // 如果测试成功，确保状态为 ACTIVE
                if (dataSource.getStatus() != DataSource.DataSourceStatus.ACTIVE) {
                    dataSource.setStatus(DataSource.DataSourceStatus.ACTIVE);
                }
                log.info("Connection test successful for datasource: {}", dataSource.getName());
            } else {
                // 如果测试失败，设置为 ERROR 状态
                dataSource.setStatus(DataSource.DataSourceStatus.ERROR);
                log.warn("Connection test failed for datasource: {} - {}", dataSource.getName(), errorMsg);
            }

            dataSourceRepository.save(dataSource);

            return result;

        } catch (Exception e) {
            log.error("Error testing connection for datasource: {}", dataSource.getName(), e);

            // 更新为错误状态
            dataSource.setLastTestedAt(LocalDateTime.now());
            dataSource.setLastTestResult(false);
            dataSource.setLastErrorMessage(e.getMessage());
            dataSource.setStatus(DataSource.DataSourceStatus.ERROR);
            dataSourceRepository.save(dataSource);

            return false;
        }
    }

    /**
     * 执行实际的连接测试
     * @return null 表示成功，非 null 表示错误信息
     */
    private String doTestConnection(DataSource dataSource) {
        switch (dataSource.getType()) {
            case "postgresql":
            case "mysql":
                return testJdbcConnection(dataSource);
            case "redis":
                return testRedisConnection(dataSource);
            case "api":
                return testHttpConnection(dataSource);
            default:
                // minio / kafka / local_file：仅校验必填字段
                return dataSource.getHost() != null ? null : "缺少 host 配置";
        }
    }

    /**
     * JDBC 连接测试（PostgreSQL / MySQL）
     * @return null 表示成功，非 null 表示错误信息
     */
    private String testJdbcConnection(DataSource dataSource) {
        String driverClass = "postgresql".equals(dataSource.getType())
            ? "org.postgresql.Driver" : "com.mysql.cj.jdbc.Driver";
        String urlPrefix = "postgresql".equals(dataSource.getType())
            ? "jdbc:postgresql" : "jdbc:mysql";

        if (dataSource.getHost() == null) {
            return "缺少 host 配置";
        }
        if (dataSource.getPort() == null) {
            return "缺少 port 配置";
        }

        String url = String.format("%s://%s:%d/%s",
            urlPrefix, dataSource.getHost(), dataSource.getPort(),
            dataSource.getDatabaseName() != null ? dataSource.getDatabaseName() : "");

        String password = null;
        if (dataSource.getPasswordEncrypted() != null) {
            try {
                password = EncryptionUtil.decrypt(dataSource.getPasswordEncrypted());
            } catch (Exception e) {
                log.warn("Password decryption failed for datasource: {}", dataSource.getName());
                return "密码解密失败: " + e.getMessage();
            }
        }

        java.util.Properties props = new java.util.Properties();
        if (dataSource.getUsername() != null) props.setProperty("user", dataSource.getUsername());
        if (password != null) props.setProperty("password", password);
        props.setProperty("connectTimeout", "5");
        props.setProperty("loginTimeout", "5");

        try {
            Class.forName(driverClass);
            try (java.sql.Connection conn = java.sql.DriverManager.getConnection(url, props)) {
                try (java.sql.Statement stmt = conn.createStatement()) {
                    stmt.execute("SELECT 1");
                }
            }
            return null;
        } catch (ClassNotFoundException e) {
            log.warn("JDBC driver not found for {}: {}", dataSource.getName(), driverClass);
            return "缺少 JDBC 驱动: " + driverClass;
        } catch (Exception e) {
            log.warn("JDBC connection test failed for {}: {}", dataSource.getName(), e.getMessage());
            return "连接失败: " + e.getMessage();
        }
    }

    /**
     * Redis 连接测试（使用 Lettuce）
     * @return null 表示成功，非 null 表示错误信息
     */
    private String testRedisConnection(DataSource dataSource) {
        String password = null;
        if (dataSource.getPasswordEncrypted() != null) {
            try {
                password = EncryptionUtil.decrypt(dataSource.getPasswordEncrypted());
            } catch (Exception e) {
                return "密码解密失败: " + e.getMessage();
            }
        }

        if (dataSource.getHost() == null) {
            return "缺少 host 配置";
        }

        int port = dataSource.getPort() != null ? dataSource.getPort() : 6379;
        io.lettuce.core.RedisClient client = null;
        try {
            io.lettuce.core.RedisURI.Builder builder = io.lettuce.core.RedisURI.builder()
                .withHost(dataSource.getHost())
                .withPort(port)
                .withTimeout(java.time.Duration.ofSeconds(5));
            if (password != null && !password.isEmpty()) {
                builder.withPassword(password.toCharArray());
            }
            client = io.lettuce.core.RedisClient.create(builder.build());
            try (io.lettuce.core.api.StatefulRedisConnection<String, String> conn = client.connect()) {
                String pong = conn.sync().ping();
                return "PONG".equalsIgnoreCase(pong) ? null : "Redis PING 响应异常: " + pong;
            }
        } catch (Exception e) {
            log.warn("Redis connection test failed for {}: {}", dataSource.getName(), e.getMessage());
            return "连接失败: " + e.getMessage();
        } finally {
            if (client != null) {
                try { client.shutdown(); } catch (Exception ignored) {}
            }
        }
    }

    /**
     * HTTP API 连接测试
     * @return null 表示成功，非 null 表示错误信息
     */
    private String testHttpConnection(DataSource dataSource) {
        if (dataSource.getHost() == null) {
            return "缺少 URL 配置";
        }
        try {
            java.net.URL url = new java.net.URL(dataSource.getHost());
            java.net.HttpURLConnection conn = (java.net.HttpURLConnection) url.openConnection();
            conn.setConnectTimeout(5000);
            conn.setReadTimeout(5000);
            conn.setRequestMethod("HEAD");
            int code = conn.getResponseCode();
            conn.disconnect();
            if (code >= 500) {
                return "服务器返回错误状态码: " + code;
            }
            return null;
        } catch (Exception e) {
            log.warn("HTTP connection test failed for {}: {}", dataSource.getName(), e.getMessage());
            return "连接失败: " + e.getMessage();
        }
    }

    /**
     * 启用数据源
     *
     * @param id 数据源ID
     */
    @Transactional
    public void enableDataSource(Long id) {
        DataSource dataSource = getDataSource(id);
        dataSource.setStatus(DataSource.DataSourceStatus.ACTIVE);
        dataSourceRepository.save(dataSource);
        log.info("Datasource enabled: {}", dataSource.getName());
    }

    /**
     * 禁用数据源
     *
     * @param id 数据源ID
     */
    @Transactional
    public void disableDataSource(Long id) {
        DataSource dataSource = getDataSource(id);
        dataSource.setStatus(DataSource.DataSourceStatus.DISABLED);
        dataSourceRepository.save(dataSource);
        log.info("Datasource disabled: {}", dataSource.getName());
    }

    /**
     * 获取数据源使用统计
     *
     * @param datasourceId 数据源ID
     * @return 使用该数据源的特征视图数量
     */
    public long getUsageCount(Long datasourceId) {
        return dataSourceRepository.countUsageByFeatureView(datasourceId);
    }

    /**
     * 获取使用该数据源的特征视图列表
     *
     * @param datasourceId 数据源ID
     * @return 特征视图列表
     */
    public List<FeatureView> getUsedByFeatureViews(Long datasourceId) {
        return featureViewDataSourceRepository.findActiveFeatureViewsByDatasourceId(datasourceId);
    }

    /**
     * 解密数据源密码
     *
     * 用于创建实际的数据库连接时使用
     *
     * @param dataSource 数据源对象
     * @return 解密后的密码
     */
    public String getDecryptedPassword(DataSource dataSource) {
        if (dataSource.getPasswordEncrypted() == null) {
            return null;
        }
        try {
            return EncryptionUtil.decrypt(dataSource.getPasswordEncrypted());
        } catch (Exception e) {
            log.error("密码解密失败: {}", dataSource.getName(), e);
            throw new BusinessException("密码解密失败: " + e.getMessage());
        }
    }
}
