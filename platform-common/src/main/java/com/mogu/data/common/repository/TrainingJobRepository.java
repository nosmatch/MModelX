package com.mogu.data.common.repository;

import com.mogu.data.common.entity.TrainingJob;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

/**
 * 训练任务Repository
 *
 * @author MModelX Team
 * @since 2026-05-24
 */
@Repository
public interface TrainingJobRepository extends JpaRepository<TrainingJob, Long> {

    /**
     * 根据实验名称查找训练任务
     *
     * @param experimentName 实验名称
     * @return 任务列表
     */
    List<TrainingJob> findByExperimentNameOrderByCreatedAtDesc(String experimentName);

    /**
     * 根据状态查找训练任务
     *
     * @param status 任务状态
     * @return 任务列表
     */
    List<TrainingJob> findByStatusOrderByCreatedAtDesc(TrainingJob.JobStatus status);

    /**
     * 查找运行中的训练任务
     *
     * @return 任务列表
     */
    List<TrainingJob> findByStatusInOrderByCreatedAtDesc(List<TrainingJob.JobStatus> statuses);
}
