package com.mogu.data.common.repository;

import com.mogu.data.common.entity.DataSource;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/**
 * 数据源 Repository
 *
 * 提供数据源的数据库访问操作
 *
 * @author MModelX Team
 * @since 2026-05-21
 */
@Repository
public interface DataSourceRepository extends JpaRepository<DataSource, Long> {

    /**
     * 根据名称查找数据源
     *
     * @param name 数据源名称
     * @return 数据源对象
     */
    Optional<DataSource> findByName(String name);

    /**
     * 根据类型查找数据源
     *
     * @param type 数据源类型（postgresql, mysql, redis, kafka, minio, api, local_file）
     * @return 该类型的所有数据源
     */
    List<DataSource> findByType(String type);

    /**
     * 根据状态查找数据源
     *
     * @param status 数据源状态（ACTIVE, DISABLED, ERROR）
     * @return 该状态的所有数据源
     */
    List<DataSource> findByStatus(DataSource.DataSourceStatus status);

    /**
     * 查找所有激活状态的数据源，按创建时间倒序
     *
     * @return 激活的数据源列表
     */
    List<DataSource> findByStatusOrderByCreatedAtDesc(DataSource.DataSourceStatus status);

    /**
     * 查找所有非归档状态的数据源，按创建时间倒序
     *
     * @return 非归档的数据源列表（ACTIVE, DISABLED, ERROR）
     */
    @Query("SELECT ds FROM DataSource ds WHERE ds.status <> 'ARCHIVED' ORDER BY ds.createdAt DESC")
    List<DataSource> findAllNonArchivedOrderByCreatedAtDesc();

    /**
     * 根据类型和状态查找数据源
     *
     * @param type 数据源类型
     * @param status 数据源状态
     * @return 符合条件的数据源列表
     */
    @Query("SELECT ds FROM DataSource ds WHERE ds.type = :type AND ds.status = :status ORDER BY ds.createdAt DESC")
    List<DataSource> findByTypeAndStatus(@Param("type") String type,
                                         @Param("status") DataSource.DataSourceStatus status);

    /**
     * 搜索数据源（名称或描述包含关键词）
     *
     * @param keyword 搜索关键词
     * @return 匹配的数据源列表
     */
    @Query("SELECT ds FROM DataSource ds WHERE ds.status = 'ACTIVE' AND " +
           "(LOWER(ds.name) LIKE LOWER(CONCAT('%', :keyword, '%')) OR " +
           "LOWER(ds.description) LIKE LOWER(CONCAT('%', :keyword, '%'))) " +
           "ORDER BY ds.createdAt DESC")
    List<DataSource> searchActiveDataSources(@Param("keyword") String keyword);

    /**
     * 检查数据源是否被特征视图使用
     *
     * @param datasourceId 数据源ID
     * @return 使用该数据源的特征视图数量
     */
    @Query("SELECT COUNT(fvd) FROM FeatureViewDataSource fvd WHERE fvd.datasource.id = :datasourceId")
    long countUsageByFeatureView(@Param("datasourceId") Long datasourceId);

    /**
     * 查找使用指定数据源的所有特征视图关联
     *
     * @param datasourceId 数据源ID
     * @return 关联列表
     */
    @Query("SELECT fvd FROM FeatureViewDataSource fvd WHERE fvd.datasource.id = :datasourceId")
    List<com.mogu.data.common.entity.FeatureViewDataSource> findByDatasourceId(@Param("datasourceId") Long datasourceId);
}
