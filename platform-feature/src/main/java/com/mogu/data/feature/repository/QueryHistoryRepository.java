package com.mogu.data.feature.repository;

import com.mogu.data.common.entity.QueryHistory;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 在线特征查询历史记录 Repository
 *
 * @author MModelX Team
 * @since 2026-05-20
 */
@Repository
public interface QueryHistoryRepository extends JpaRepository<QueryHistory, Long> {

    /**
     * 根据查询者查找查询历史
     *
     * @param queriedBy 查询者
     * @return 查询历史列表
     */
    List<QueryHistory> findByQueriedBy(String queriedBy);

    /**
     * 根据实体类型查找查询历史
     *
     * @param entityType 实体类型
     * @return 查询历史列表
     */
    List<QueryHistory> findByEntityType(String entityType);

    /**
     * 根据状态查找查询历史
     *
     * @param status 查询状态
     * @return 查询历史列表
     */
    List<QueryHistory> findByStatus(QueryHistory.QueryStatus status);

    /**
     * 根据查询名称查找查询历史
     *
     * @param queryName 查询名称
     * @return 查询历史列表
     */
    List<QueryHistory> findByQueryName(String queryName);

    /**
     * 查找最近的N条查询历史
     *
     * @param queriedBy 查询者
     * @param limit 限制数量
     * @return 查询历史列表
     */
    @Query("SELECT q FROM QueryHistory q WHERE q.queriedBy = :queriedBy ORDER BY q.createdAt DESC")
    List<QueryHistory> findRecentQueries(@Param("queriedBy") String queriedBy);

    /**
     * 查找指定时间范围内的查询历史
     *
     * @param startTime 开始时间
     * @param endTime 结束时间
     * @return 查询历史列表
     */
    @Query("SELECT q FROM QueryHistory q WHERE q.createdAt BETWEEN :startTime AND :endTime ORDER BY q.createdAt DESC")
    List<QueryHistory> findByCreatedAtBetween(@Param("startTime") LocalDateTime startTime,
                                               @Param("endTime") LocalDateTime endTime);

    /**
     * 统计查询者的查询次数
     *
     * @param queriedBy 查询者
     * @return 查询次数
     */
    @Query("SELECT COUNT(q) FROM QueryHistory q WHERE q.queriedBy = :queriedBy")
    long countByQueriedBy(@Param("queriedBy") String queriedBy);

    /**
     * 统计成功的查询次数
     *
     * @param queriedBy 查询者
     * @return 成功查询次数
     */
    @Query("SELECT COUNT(q) FROM QueryHistory q WHERE q.queriedBy = :queriedBy AND q.status = 'SUCCESS'")
    long countSuccessfulQueries(@Param("queriedBy") String queriedBy);

    /**
     * 统计失败的查询次数
     *
     * @param queriedBy 查询者
     * @return 失败查询次数
     */
    @Query("SELECT COUNT(q) FROM QueryHistory q WHERE q.queriedBy = :queriedBy AND q.status = 'FAILED'")
    long countFailedQueries(@Param("queriedBy") String queriedBy);

    /**
     * 计算平均查询耗时
     *
     * @param queriedBy 查询者
     * @return 平均耗时（毫秒）
     */
    @Query("SELECT AVG(q.durationMs) FROM QueryHistory q WHERE q.queriedBy = :queriedBy AND q.status = 'SUCCESS'")
    Double averageQueryDuration(@Param("queriedBy") String queriedBy);

    /**
     * 根据实体类型和实体ID查找查询历史
     *
     * @param entityType 实体类型
     * @param entityId 实体ID
     * @return 查询历史列表
     */
    @Query("SELECT q FROM QueryHistory q WHERE q.entityType = :entityType AND q.entityId = :entityId ORDER BY q.createdAt DESC")
    List<QueryHistory> findByEntityTypeAndEntityId(@Param("entityType") String entityType,
                                                     @Param("entityId") String entityId);

    /**
     * 删除指定时间之前的查询历史
     *
     * @param beforeTime 时间阈值
     * @return 删除的记录数
     */
    @Query("DELETE FROM QueryHistory q WHERE q.createdAt < :beforeTime")
    int deleteOldQueries(@Param("beforeTime") LocalDateTime beforeTime);
}
