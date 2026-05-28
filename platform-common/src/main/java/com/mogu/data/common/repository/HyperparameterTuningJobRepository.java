package com.mogu.data.common.repository;

import com.mogu.data.common.entity.HyperparameterTuningJob;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface HyperparameterTuningJobRepository extends JpaRepository<HyperparameterTuningJob, Long> {

    List<HyperparameterTuningJob> findByStatusInOrderByCreatedAtDesc(List<HyperparameterTuningJob.JobStatus> statuses);

    List<HyperparameterTuningJob> findAllByOrderByCreatedAtDesc();
}
