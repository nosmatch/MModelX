package com.mogu.data.feature.repository;

import com.mogu.data.common.entity.Feature;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

/**
 * Feature Repository
 *
 * 特征定义数据访问层
 *
 * @author MModelX Team
 * @since 2026-05-20
 */
@Repository
public interface FeatureRepository extends JpaRepository<Feature, Long> {

    /**
     * 根据特征视图ID查找特征列表
     *
     * @param featureViewId 特征视图ID
     * @return 特征列表
     */
    List<Feature> findByFeatureView_Id(Long featureViewId);

    /**
     * 根据特征视图ID查找活跃的特征（包含DRAFT和ACTIVE状态）
     *
     * @param viewId 特征视图ID
     * @return 特征列表
     */
    @Query("SELECT f FROM Feature f WHERE f.featureView.id = :viewId AND f.status IN ('ACTIVE', 'DRAFT', 'COMPUTING')")
    List<Feature> findActiveFeaturesByViewId(@Param("viewId") Long viewId);

    /**
     * 根据特征视图名称查找特征列表
     *
     * @param featureViewName 特征视图名称
     * @return 特征列表
     */
    List<Feature> findByFeatureView_Name(String featureViewName);

    /**
     * 根据名称查找特征
     *
     * @param name 特征名称
     * @return 特征列表
     */
    List<Feature> findByName(String name);

    /**
     * 根据数据源类型查找特征
     *
     * @param sourceType 数据源类型
     * @return 特征列表
     */
    List<Feature> findBySourceType(String sourceType);

    /**
     * 根据状态查找特征
     *
     * @param status 特征状态
     * @return 特征列表
     */
    List<Feature> findByStatus(Feature.FeatureStatus status);

    /**
     * 检查特征名称在特征视图中是否存在
     *
     * @param featureViewId 特征视图ID
     * @param name 特征名称
     * @return 是否存在
     */
    @Query("SELECT CASE WHEN COUNT(f) > 0 THEN true ELSE false END FROM Feature f WHERE f.featureView.id = :featureViewId AND f.name = :name")
    boolean existsByFeatureViewIdAndName(@Param("featureViewId") Long featureViewId, @Param("name") String name);
}
