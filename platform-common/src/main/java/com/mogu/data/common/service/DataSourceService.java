package com.mogu.data.common.service;

import com.fasterxml.jackson.databind.JsonNode;
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
     * 获取所有激活的数据源
     *
     * @return 激活的数据源列表
     */
    public List<DataSource> getActiveDataSources() {
        return dataSourceRepository.findByStatusOrderByCreatedAtDesc(
            DataSource.DataSourceStatus.ACTIVE
        );
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
            // TODO: 调用相应的适配器测试连接
            // 这里需要注入 DataSourceFactory 或各个适配器
            // 暂时实现：简单地返回成功（实际应该调用适配器的 testConnection 方法）

            boolean result = doTestConnection(dataSource);

            // 更新测试结果
            dataSource.setLastTestedAt(LocalDateTime.now());
            dataSource.setLastTestResult(result);
            dataSource.setLastErrorMessage(result ? null : "测试连接功能待实现");

            if (result) {
                // 如果测试成功，确保状态为 ACTIVE
                if (dataSource.getStatus() != DataSource.DataSourceStatus.ACTIVE) {
                    dataSource.setStatus(DataSource.DataSourceStatus.ACTIVE);
                }
                log.info("Connection test successful for datasource: {}", dataSource.getName());
            } else {
                // 如果测试失败，设置为 ERROR 状态
                dataSource.setStatus(DataSource.DataSourceStatus.ERROR);
                log.warn("Connection test failed for datasource: {}", dataSource.getName());
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
     *
     * TODO: 实现真正的连接测试逻辑
     * - 根据数据源类型调用相应的适配器
     * - PostgreSQL: 使用 JdbcTemplate 执行 SELECT 1
     * - Redis: 使用 RedisTemplate 执行 PING
     * - API: 使用 RestTemplate 发送测试请求
     * - Kafka: 创建 Consumer 测试连接
     *
     * @param dataSource 数据源对象
     * @return 是否连接成功
     */
    private boolean doTestConnection(DataSource dataSource) {
        // 暂时实现：简单地检查配置是否完整
        // 实际应该调用 DataSourceAdapter 接口的 testConnection 方法

        switch (dataSource.getType()) {
            case "postgresql":
            case "mysql":
                // 检查必要字段
                return dataSource.getHost() != null
                    && dataSource.getPort() != null
                    && dataSource.getDatabaseName() != null;

            case "redis":
                return dataSource.getHost() != null
                    && dataSource.getPort() != null;

            case "api":
                return dataSource.getHost() != null;

            default:
                return true; // 其他类型暂时返回 true
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
