package com.mogu.data.common.repository;

import com.mogu.data.common.entity.K8sNamespace;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/**
 * K8s Namespace Repository
 *
 * @author MModelX Team
 * @since 2026-05-24
 */
@Repository
public interface K8sNamespaceRepository extends JpaRepository<K8sNamespace, Long> {

    /**
     * 根据名称查找
     */
    Optional<K8sNamespace> findByName(String name);

    /**
     * 根据业务线查找
     */
    List<K8sNamespace> findByBusinessLine(String businessLine);

    /**
     * 查找所有活跃的 namespace
     */
    List<K8sNamespace> findByStatus(K8sNamespace.Status status);

    /**
     * 根据名称和状态查找
     */
    Optional<K8sNamespace> findByNameAndStatus(String name, K8sNamespace.Status status);
}
