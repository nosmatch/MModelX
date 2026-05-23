package com.mogu.data.common.repository;

import com.mogu.data.common.entity.SampleBuildJob;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/**
 * 样本构建任务Repository
 *
 * @author MModelX Team
 * @since 2026-05-23
 */
@Repository
public interface SampleBuildJobRepository extends JpaRepository<SampleBuildJob, Long> {

    /**
     * 根据配置ID查找任务
     */
    List<SampleBuildJob> findBySampleConfigIdOrderByCreatedAtDesc(Long sampleConfigId);

    /**
     * 根据状态查找任务
     */
    List<SampleBuildJob> findByStatus(SampleBuildJob.JobStatus status);

    /**
     * 查找最新的任务
     */
    Optional<SampleBuildJob> findTopBySampleConfigIdOrderByCreatedAtDesc(Long sampleConfigId);

    /**
     * 查找正在运行的任务
     */
    @Query("SELECT j FROM SampleBuildJob j WHERE j.status = 'RUNNING'")
    List<SampleBuildJob> findRunningJobs();

    /**
     * 统计某个配置的历史构建次数
     */
    long countBySampleConfigId(Long sampleConfigId);
}
