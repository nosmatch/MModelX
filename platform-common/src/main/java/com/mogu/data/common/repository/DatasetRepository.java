package com.mogu.data.common.repository;

import com.mogu.data.common.entity.Dataset;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/**
 * 数据集Repository
 *
 * @author MModelX Team
 * @since 2026-05-20
 */
@Repository
public interface DatasetRepository extends JpaRepository<Dataset, Long> {

    /**
     * 根据名称查找数据集
     *
     * @param name 数据集名称
     * @return 数据集列表
     */
    List<Dataset> findByName(String name);

    /**
     * 根据名称和版本查找数据集
     *
     * @param name 数据集名称
     * @param version 版本号
     * @return 数据集对象
     */
    Optional<Dataset> findByNameAndVersion(String name, String version);

    /**
     * 根据状态查找数据集
     *
     * @param status 数据集状态
     * @return 数据集列表
     */
    List<Dataset> findByStatus(Dataset.DatasetStatus status);

    /**
     * 查找最新的数据集版本
     *
     * @param name 数据集名称
     * @return 数据集对象
     */
    @Query("SELECT d FROM Dataset d WHERE d.name = :name ORDER BY d.createdAt DESC")
    List<Dataset> findLatestByName(@Param("name") String name);
}
