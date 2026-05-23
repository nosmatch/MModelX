package com.mogu.data.common.repository;

import com.mogu.data.common.entity.SampleConfig;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/**
 * 样本配置Repository
 *
 * @author MModelX Team
 * @since 2026-05-23
 */
@Repository
public interface SampleConfigRepository extends JpaRepository<SampleConfig, Long> {

    /**
     * 根据名称查找样本配置
     */
    Optional<SampleConfig> findByName(String name);

    /**
     * 根据状态查找
     */
    List<SampleConfig> findByStatus(SampleConfig.ConfigStatus status);

    /**
     * 搜索名称包含关键字的配置
     */
    List<SampleConfig> findByNameContainingIgnoreCase(String keyword);

    /**
     * 列出所有活跃的配置
     */
    @Query("SELECT sc FROM SampleConfig sc WHERE sc.status = 'ACTIVE' ORDER BY sc.updatedAt DESC")
    List<SampleConfig> findAllActive();
}
