package com.mogu.data.feature.repository;

import com.mogu.data.common.entity.FeatureView;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/**
 * Feature View Repository
 *
 * 特征视图数据访问层
 *
 * @author MModelX Team
 * @since 2026-05-20
 */
@Repository
public interface FeatureViewRepository extends JpaRepository<FeatureView, Long> {

    /**
     * 根据名称查找特征视图
     *
     * @param name 特征视图名称
     * @return 特征视图
     */
    Optional<FeatureView> findByName(String name);

    /**
     * 查找所有活跃状态的特征视图
     *
     * @return 活跃的特征视图列表
     */
    @Query("SELECT f FROM FeatureView f WHERE f.status = 'ACTIVE'")
    List<FeatureView> findActiveFeatureViews();

    /**
     * 根据名称查找活跃的特征视图
     *
     * @param name 特征视图名称
     * @return 活跃的特征视图
     */
    @Query("SELECT f FROM FeatureView f WHERE f.name = :name AND f.status != 'ARCHIVED'")
    Optional<FeatureView> findActiveByName(@Param("name") String name);

    /**
     * 根据创建者查找特征视图
     *
     * @param username 创建者用户名
     * @return 特征视图列表
     */
    List<FeatureView> findByCreatedBy_Username(String username);

    /**
     * 根据实体类型查找特征视图
     *
     * @param entity 实体类型
     * @return 特征视图列表
     */
    List<FeatureView> findByEntity(String entity);

    /**
     * 检查特征视图名称是否存在
     *
     * @param name 特征视图名称
     * @return 是否存在
     */
    boolean existsByName(String name);
}
