package com.mogu.data.common.repository;

import com.mogu.data.common.entity.AlgorithmConfig;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/**
 * 算法配置Repository
 *
 * @author MModelX Team
 * @since 2026-05-20
 */
@Repository
public interface AlgorithmConfigRepository extends JpaRepository<AlgorithmConfig, Long> {

    /**
     * 根据算法名称查找配置
     *
     * @param algorithmName 算法名称
     * @return 配置对象
     */
    Optional<AlgorithmConfig> findByAlgorithmName(String algorithmName);

    /**
     * 根据框架类型查找配置
     *
     * @param framework 框架类型
     * @return 配置列表
     */
    List<AlgorithmConfig> findByFramework(String framework);

    /**
     * 查找启用的配置
     *
     * @return 配置列表
     */
    List<AlgorithmConfig> findByEnabledTrue();
}
