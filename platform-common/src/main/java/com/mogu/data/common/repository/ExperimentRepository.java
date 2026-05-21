package com.mogu.data.common.repository;

import com.mogu.data.common.entity.Experiment;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

/**
 * 实验Repository
 *
 * @author MModelX Team
 * @since 2026-05-20
 */
@Repository
public interface ExperimentRepository extends JpaRepository<Experiment, Long> {

    /**
     * 根据数据集查找实验
     *
     * @param datasetId 数据集ID
     * @return 实验列表
     */
    List<Experiment> findByDatasetId(Long datasetId);

    /**
     * 根据状态查找实验
     *
     * @param status 实验状态
     * @return 实验列表
     */
    List<Experiment> findByStatus(Experiment.ExperimentStatus status);

    /**
     * 根据实验名称查找实验
     *
     * @param name 实验名称
     * @return 实验列表
     */
    List<Experiment> findByName(String name);

    /**
     * 根据模型类型查找实验
     *
     * @param modelType 模型类型
     * @return 实验列表
     */
    List<Experiment> findByModelType(String modelType);

    /**
     * 查找运行中的实验
     *
     * @return 运行中的实验列表
     */
    @Query("SELECT e FROM Experiment e WHERE e.status = 'RUNNING'")
    List<Experiment> findRunningExperiments();

    /**
     * 根据指标值排序实验
     *
     * @param pageable 分页参数
     * @return 实验列表
     */
    @Query("SELECT e FROM Experiment e WHERE e.status = 'COMPLETED' ORDER BY e.metricValue DESC")
    List<Experiment> findTopCompletedExperiments(org.springframework.data.domain.Pageable pageable);
}
