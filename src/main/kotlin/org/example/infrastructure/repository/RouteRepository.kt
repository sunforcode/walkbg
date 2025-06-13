package org.example.infrastructure.repository

import org.example.route.model.Route
import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.JpaSpecificationExecutor
import org.springframework.data.jpa.repository.Modifying
import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.query.Param
import org.springframework.stereotype.Repository

/**
 * 路线仓储接口
 */
@Repository
interface RouteRepository : JpaRepository<Route, String>, JpaSpecificationExecutor<Route> {
    /**
     * 根据关键字查询路线
     */
    @Query("SELECT r FROM Route r WHERE r.name LIKE %:keyword% OR r.description LIKE %:keyword%")
    fun findByKeyword(@Param("keyword") keyword: String, pageable: Pageable): Page<Route>

    /**
     * 根据地区ID查询路线
     */
    fun findByRegionId(regionId: String, pageable: Pageable): Page<Route>

    /**
     * 根据难度查询路线
     */
    fun findByDifficulty(difficulty: Int, pageable: Pageable): Page<Route>

    /**
     * 根据路线类型查询路线
     */
    fun findByRouteType(routeType: Int, pageable: Pageable): Page<Route>

    /**
     * 根据距离范围查询路线
     */
    @Query("SELECT r FROM Route r WHERE r.distance >= :minDistance AND r.distance <= :maxDistance")
    fun findByDistanceBetween(
        @Param("minDistance") minDistance: Double,
        @Param("maxDistance") maxDistance: Double,
        pageable: Pageable
    ): Page<Route>

    /**
     * 根据标签查询路线
     */
    @Query("SELECT r FROM Route r JOIN r.tags t WHERE t.tag IN :tags GROUP BY r HAVING COUNT(DISTINCT t.tag) = :tagCount")
    fun findByTags(
        @Param("tags") tags: List<String>,
        @Param("tagCount") tagCount: Long,
        pageable: Pageable
    ): Page<Route>

    /**
     * 根据用户ID查询收藏的路线
     */
    @Query("SELECT r FROM Route r JOIN r.userFavoriteRoutes f WHERE f.user.id = :userId")
    fun findFavoritesByUserId(@Param("userId") userId: String, pageable: Pageable): Page<Route>

    /**
     * 增加路线热度
     */
    @Modifying
    @Query("UPDATE Route r SET r.popularity = r.popularity + 1, r.updatedAt = CURRENT_TIMESTAMP WHERE r.id = :id")
    fun incrementPopularity(@Param("id") id: String): Int

    /**
     * 增加路线使用次数
     */
    @Modifying
    @Query("UPDATE Route r SET r.usageCount = r.usageCount + 1, r.updatedAt = CURRENT_TIMESTAMP WHERE r.id = :id")
    fun incrementUsageCount(@Param("id") id: String): Int
}
