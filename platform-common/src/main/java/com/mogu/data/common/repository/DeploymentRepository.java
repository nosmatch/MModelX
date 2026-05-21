package com.mogu.data.common.repository;

import com.mogu.data.common.entity.Deployment;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/**
 * 部署Repository
 *
 * @author MModelX Team
 * @since 2026-05-20
 */
@Repository
public interface DeploymentRepository extends JpaRepository<Deployment, Long> {

    /**
     * 根据模型查找部署
     *
     * @param modelId 模型ID
     * @return 部署列表
     */
    List<Deployment> findByModelId(Long modelId);

    /**
     * 根据环境查找部署
     *
     * @param environment 部署环境
     * @return 部署列表
     */
    List<Deployment> findByEnvironment(Deployment.Environment environment);

    /**
     * 根据环境查找运行中的部署
     *
     * @param environment 部署环境
     * @return 部署列表
     */
    List<Deployment> findByEnvironmentAndStatus(Deployment.Environment environment, Deployment.DeploymentStatus status);

    /**
     * 查找生产环境的部署
     *
     * @return 部署列表
     */
    @Query("SELECT d FROM Deployment d WHERE d.environment = 'PRODUCTION' AND d.status = 'RUNNING'")
    List<Deployment> findProductionDeployments();

    /**
     * 查找某个模型的活跃部署
     *
     * @param modelId 模型ID
     * @return 部署对象
     */
    Optional<Deployment> findByModelIdAndStatus(Long modelId, Deployment.DeploymentStatus status);

    /**
     * 计算某个环境的总流量
     *
     * @param environment 部署环境
     * @return 总流量百分比
     */
    @Query("SELECT SUM(d.trafficPercentage) FROM Deployment d WHERE d.environment = :environment AND d.status = 'RUNNING'")
    Integer sumTrafficByEnvironment(@Param("environment") Deployment.Environment environment);
}
