package com.mogu.data.common.repository;

import com.mogu.data.common.entity.HyperparameterTrial;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface HyperparameterTrialRepository extends JpaRepository<HyperparameterTrial, Long> {

    List<HyperparameterTrial> findByTuningJobIdOrderByTrialIndexAsc(Long tuningJobId);
}
