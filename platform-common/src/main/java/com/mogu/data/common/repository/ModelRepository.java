package com.mogu.data.common.repository;

import com.mogu.data.common.entity.Model;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/**
 * 模型Repository
 *
 * @author MModelX Team
 * @since 2026-05-20
 */
@Repository
public interface ModelRepository extends JpaRepository<Model, Long> {

    /**
     * 根据名称查找模型
     *
     * @param name 模型名称
     * @return 模型列表
     */
    List<Model> findByName(String name);

    /**
     * 根据名称和版本查找模型
     *
     * @param name 模型名称
     * @param version 版本号
     * @return 模型对象
     */
    Optional<Model> findByNameAndVersion(String name, String version);

    /**
     * 根据实验查找模型
     *
     * @param experimentId 实验ID
     * @return 模型列表
     */
    List<Model> findByExperimentId(Long experimentId);

    /**
     * 根据框架类型查找模型
     *
     * @param framework 框架类型
     * @return 模型列表
     */
    List<Model> findByFramework(String framework);

    /**
     * 查找最新的模型版本
     *
     * @param name 模型名称
     * @return 模型对象
     */
    @Query("SELECT m FROM Model m WHERE m.name = :name ORDER BY m.registeredAt DESC")
    List<Model> findLatestByName(@Param("name") String name);

    /**
     * 查找已注册的模型
     *
     * @return 模型列表
     */
    @Query("SELECT m FROM Model m WHERE m.experiment.status = 'COMPLETED' ORDER BY m.registeredAt DESC")
    List<Model> findRegisteredModels();
}
