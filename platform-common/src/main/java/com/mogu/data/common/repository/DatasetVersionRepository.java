package com.mogu.data.common.repository;

import com.mogu.data.common.entity.DatasetVersion;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/**
 * 数据集版本Repository
 *
 * @author MModelX Team
 * @since 2026-05-23
 */
@Repository
public interface DatasetVersionRepository extends JpaRepository<DatasetVersion, Long> {

    /**
     * 根据数据集ID和版本号查找
     */
    Optional<DatasetVersion> findByDatasetIdAndVersion(Long datasetId, String version);

    /**
     * 根据数据集ID查找所有版本
     */
    List<DatasetVersion> findByDatasetIdOrderByCreatedAtDesc(Long datasetId);

    /**
     * 根据状态查找
     */
    List<DatasetVersion> findByStatus(DatasetVersion.VersionStatus status);

    /**
     * 查找最新版本
     */
    Optional<DatasetVersion> findTopByDatasetIdOrderByCreatedAtDesc(Long datasetId);

    /**
     * 根据版本标签查找
     */
    Optional<DatasetVersion> findByDatasetIdAndVersionTag(Long datasetId, String versionTag);

    /**
     * 根据实验ID查找关联的数据集版本
     */
    List<DatasetVersion> findByExperimentId(String experimentId);

    /**
     * 统计某个数据集的总版本数
     */
    long countByDatasetId(Long datasetId);
}
