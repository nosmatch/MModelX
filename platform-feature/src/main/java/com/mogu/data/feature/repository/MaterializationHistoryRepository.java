package com.mogu.data.feature.repository;

import com.mogu.data.common.entity.MaterializationHistory;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 特征物化历史记录 Repository
 *
 * @author MModelX Team
 * @since 2026-05-20
 */
@Repository
public interface MaterializationHistoryRepository extends JpaRepository<MaterializationHistory, Long> {

    /**
     * 根据特征视图名称查找物化历史
     *
     * @param featureViewName 特征视图名称
     * @return 物化历史列表
     */
    List<MaterializationHistory> findByFeatureViewName(String featureViewName);

    /**
     * 根据特征视图名称查找最近的N条物化历史
     *
     * @param featureViewName 特征视图名称
     * @param limit 限制数量
     * @return 物化历史列表
     */
    @Query("SELECT m FROM MaterializationHistory m WHERE m.featureViewName = :featureViewName ORDER BY m.startedAt DESC")
    List<MaterializationHistory> findRecentByFeatureViewName(@Param("featureViewName") String featureViewName);

    /**
     * 根据状态查找物化历史
     *
     * @param status 物化状态
     * @return 物化历史列表
     */
    List<MaterializationHistory> findByStatus(MaterializationHistory.MaterializationStatus status);

    /**
     * 根据操作者查找物化历史
     *
     * @param operator 操作者
     * @return 物化历史列表
     */
    List<MaterializationHistory> findByOperator(String operator);

    /**
     * 查找指定时间范围内的物化历史
     *
     * @param startTime 开始时间
     * @param endTime 结束时间
     * @return 物化历史列表
     */
    @Query("SELECT m FROM MaterializationHistory m WHERE m.startedAt BETWEEN :startTime AND :endTime ORDER BY m.startedAt DESC")
    List<MaterializationHistory> findByStartedAtBetween(@Param("startTime") LocalDateTime startTime,
                                                         @Param("endTime") LocalDateTime endTime);

    /**
     * 查找正在运行的物化任务
     *
     * @return 物化历史列表
     */
    @Query("SELECT m FROM MaterializationHistory m WHERE m.status = 'RUNNING' ORDER BY m.startedAt DESC")
    List<MaterializationHistory> findRunningTasks();

    /**
     * 统计特征视图的物化次数
     *
     * @param featureViewName 特征视图名称
     * @return 物化次数
     */
    @Query("SELECT COUNT(m) FROM MaterializationHistory m WHERE m.featureViewName = :featureViewName AND m.status = 'SUCCESS'")
    long countSuccessfulMaterializations(@Param("featureViewName") String featureViewName);

    /**
     * 查找最近一次成功的物化记录
     * 使用Spring Data JPA的First关键字
     */
    MaterializationHistory findFirstByFeatureViewNameAndStatusOrderByCompletedAtDesc(
        @Param("featureViewName") String featureViewName,
        MaterializationHistory.MaterializationStatus status
    );
}
