package com.mogu.data.feature.repository;

import com.mogu.data.common.entity.Favorite;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/**
 * 特征查询收藏夹 Repository
 *
 * @author MModelX Team
 * @since 2026-05-20
 */
@Repository
public interface FavoriteRepository extends JpaRepository<Favorite, Long> {

    /**
     * 根据创建者查找收藏
     *
     * @param createdBy 创建者
     * @return 收藏列表
     */
    List<Favorite> findByCreatedBy(String createdBy);

    /**
     * 根据创建者和收藏类型查找收藏
     *
     * @param createdBy 创建者
     * @param favoriteType 收藏类型
     * @return 收藏列表
     */
    List<Favorite> findByCreatedByAndFavoriteType(String createdBy, Favorite.FavoriteType favoriteType);

    /**
     * 根据创建者和名称查找收藏
     *
     * @param createdBy 创建者
     * @param name 收藏名称
     * @return 收藏
     */
    Optional<Favorite> findByCreatedByAndName(String createdBy, String name);

    /**
     * 根据创建者查找默认收藏
     *
     * @param createdBy 创建者
     * @return 默认收藏列表
     */
    List<Favorite> findByCreatedByAndIsDefaultTrue(String createdBy);

    /**
     * 根据实体类型查找收藏
     *
     * @param entityType 实体类型
     * @return 收藏列表
     */
    List<Favorite> findByEntityType(String entityType);

    /**
     * 根据特征视图名称查找收藏
     *
     * @param featureViewName 特征视图名称
     * @return 收藏列表
     */
    List<Favorite> findByFeatureViewName(String featureViewName);

    /**
     * 根据创建者查找收藏，按排序序号排序
     *
     * @param createdBy 创建者
     * @return 收藏列表
     */
    @Query("SELECT f FROM Favorite f WHERE f.createdBy = :createdBy ORDER BY f.sortOrder ASC, f.createdAt DESC")
    List<Favorite> findByCreatedByOrderBySortOrder(@Param("createdBy") String createdBy);

    /**
     * 根据创建者和收藏类型查找收藏，按排序序号排序
     *
     * @param createdBy 创建者
     * @param favoriteType 收藏类型
     * @return 收藏列表
     */
    @Query("SELECT f FROM Favorite f WHERE f.createdBy = :createdBy AND f.favoriteType = :favoriteType ORDER BY f.sortOrder ASC, f.createdAt DESC")
    List<Favorite> findByCreatedByAndFavoriteTypeOrderBySortOrder(@Param("createdBy") String createdBy,
                                                                    @Param("favoriteType") Favorite.FavoriteType favoriteType);

    /**
     * 检查收藏名称是否已存在
     *
     * @param createdBy 创建者
     * @param name 收藏名称
     * @return 是否存在
     */
    @Query("SELECT CASE WHEN COUNT(f) > 0 THEN true ELSE false END FROM Favorite f WHERE f.createdBy = :createdBy AND f.name = :name")
    boolean existsByCreatedByAndName(@Param("createdBy") String createdBy, @Param("name") String name);

    /**
     * 统计用户的收藏数量
     *
     * @param createdBy 创建者
     * @return 收藏数量
     */
    @Query("SELECT COUNT(f) FROM Favorite f WHERE f.createdBy = :createdBy")
    long countByCreatedBy(@Param("createdBy") String createdBy);

    /**
     * 统计用户指定类型的收藏数量
     *
     * @param createdBy 创建者
     * @param favoriteType 收藏类型
     * @return 收藏数量
     */
    @Query("SELECT COUNT(f) FROM Favorite f WHERE f.createdBy = :createdBy AND f.favoriteType = :favoriteType")
    long countByCreatedByAndFavoriteType(@Param("createdBy") String createdBy,
                                          @Param("favoriteType") Favorite.FavoriteType favoriteType);

    /**
     * 查找收藏的最大排序序号
     *
     * @param createdBy 创建者
     * @return 最大排序序号
     */
    @Query("SELECT COALESCE(MAX(f.sortOrder), -1) FROM Favorite f WHERE f.createdBy = :createdBy")
    Integer findMaxSortOrderByCreatedBy(@Param("createdBy") String createdBy);
}
