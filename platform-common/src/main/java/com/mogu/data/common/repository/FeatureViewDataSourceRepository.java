package com.mogu.data.common.repository;

import com.mogu.data.common.entity.FeatureView;
import com.mogu.data.common.entity.FeatureViewDataSource;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/**
 * 特征视图与数据源关联 Repository
 *
 * 提供特征视图和数据源关联关系的数据库访问操作
 *
 * @author MModelX Team
 * @since 2026-05-21
 */
@Repository
public interface FeatureViewDataSourceRepository extends JpaRepository<FeatureViewDataSource, Long> {

    /**
     * 查找特征视图的所有数据源关联
     *
     * @param featureViewId 特征视图ID
     * @return 关联列表
     */
    List<FeatureViewDataSource> findByFeatureViewId(Long featureViewId);

    /**
     * 查找特征视图的主数据源
     *
     * @param featureViewId 特征视图ID
     * @return 主数据源关联对象
     */
    Optional<FeatureViewDataSource> findByFeatureViewIdAndIsPrimaryTrue(Long featureViewId);

    /**
     * 查找使用指定数据源的所有特征视图关联
     *
     * @param datasourceId 数据源ID
     * @return 关联列表
     */
    @Query("SELECT fvd FROM FeatureViewDataSource fvd WHERE fvd.datasource.id = :datasourceId " +
           "ORDER BY fvd.isPrimary DESC")
    List<FeatureViewDataSource> findByDatasourceId(@Param("datasourceId") Long datasourceId);

    /**
     * 查找使用指定数据源的活跃特征视图
     *
     * @param datasourceId 数据源ID
     * @return 特征视图列表
     */
    @Query("SELECT DISTINCT fv FROM FeatureView fv " +
           "INNER JOIN FeatureViewDataSource fvd ON fv.id = fvd.featureView.id " +
           "WHERE fvd.datasource.id = :datasourceId " +
           "AND fv.status IN ('ACTIVE', 'DRAFT') " +
           "ORDER BY fv.name")
    List<FeatureView> findActiveFeatureViewsByDatasourceId(@Param("datasourceId") Long datasourceId);

    /**
     * 检查特征视图是否使用了指定数据源
     *
     * @param featureViewId 特征视图ID
     * @param datasourceId 数据源ID
     * @return 是否存在关联
     */
    @Query("SELECT COUNT(fvd) > 0 FROM FeatureViewDataSource fvd " +
           "WHERE fvd.featureView.id = :featureViewId " +
           "AND fvd.datasource.id = :datasourceId")
    boolean existsByFeatureViewIdAndDatasourceId(@Param("featureViewId") Long featureViewId,
                                                   @Param("datasourceId") Long datasourceId);

    /**
     * 删除特征视图的所有数据源关联
     *
     * @param featureViewId 特征视图ID
     */
    void deleteByFeatureViewId(Long featureViewId);

    /**
     * 统计数据源被多少个特征视图使用
     *
     * @param datasourceId 数据源ID
     * @return 使用数量
     */
    @Query("SELECT COUNT(fvd) FROM FeatureViewDataSource fvd WHERE fvd.datasource.id = :datasourceId")
    long countByDatasourceId(@Param("datasourceId") Long datasourceId);

    /**
     * 查找所有使用指定数据源的特征视图ID列表
     *
     * @param datasourceId 数据源ID
     * @return 特征视图ID列表
     */
    @Query("SELECT fvd.featureView.id FROM FeatureViewDataSource fvd " +
           "WHERE fvd.datasource.id = :datasourceId")
    List<Long> findFeatureViewIdsByDatasourceId(@Param("datasourceId") Long datasourceId);
}
