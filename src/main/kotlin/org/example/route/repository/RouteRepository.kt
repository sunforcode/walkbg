package org.example.route.repository

import org.example.route.model.Route
import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.query.Param
import org.springframework.stereotype.Repository
import java.math.BigDecimal

/**
 * 路线仓库
 */
@Repository
interface RouteRepository : JpaRepository<Route, String> {
    
    /**
     * 根据名称查找路线
     */
    fun findByNameContainingIgnoreCase(name: String, pageable: Pageable): Page<Route>
    
    /**
     * 根据区域查找路线
     */
    fun findByRegion(region: String, pageable: Pageable): Page<Route>
    
    /**
     * 根据难度查找路线
     */
    fun findByDifficulty(difficulty: Int, pageable: Pageable): Page<Route>
    
    /**
     * 根据路线类型查找路线
     */
    fun findByRouteType(routeType: Int, pageable: Pageable): Page<Route>

    /**
     * 根据状态查找路线
     */
    fun findByStatus(status: Int, pageable: Pageable): Page<Route>

    /**
     * 根据标签查找路线
     * 注意：由于单向关联，需要通过 RouteTagRepository 查询
     * 此方法暂时返回空结果
     */
    @Query("SELECT r FROM Route r WHERE 1=0")
    fun findByTagsTag(@Param("tag") tag: String, pageable: Pageable): Page<Route>
    
    
    /**
     * 根据距离范围查找路线
     * 注意：距离信息存储在 MapData 中，需要通过 MapDataRepository 查询
     * 此方法暂时返回空结果
     */
    @Query("SELECT r FROM Route r WHERE 1=0")
    fun findByDistanceBetween(@Param("minDistance") minDistance: BigDecimal, @Param("maxDistance") maxDistance: BigDecimal, pageable: Pageable): Page<Route>
    
    /**
     * 查找热门路线
     */
    fun findTop10ByOrderByPopularityDesc(): List<Route>



    /**
     * 根据创建者查找路线
     */
    fun findByCreatedBy(createdBy: String, pageable: Pageable): Page<Route>

    /**
     * 多条件搜索路线
     * 注意：由于单向关联，移除了 tags 的 JOIN 条件
     * 如需按标签搜索，应该通过 RouteTagRepository 实现
     */
    @Query("""
        SELECT DISTINCT r FROM Route r
        WHERE (:keyword IS NULL OR
               LOWER(r.name) LIKE LOWER(CONCAT('%', :keyword, '%')) OR
               LOWER(r.description) LIKE LOWER(CONCAT('%', :keyword, '%')))
        AND (:region IS NULL OR r.region = :region)
        AND (:difficulty IS NULL OR r.difficulty = :difficulty)
        AND (:routeType IS NULL OR r.routeType = :routeType)
        AND (:status IS NULL OR r.status = :status)
    """)
    fun searchRoutes(
        @Param("keyword") keyword: String?,
        @Param("region") region: String?,
        @Param("difficulty") difficulty: Int?,
        @Param("routeType") routeType: Int?,
        @Param("status") status: Int?,
        pageable: Pageable
    ): Page<Route>

    /**
     * 查找用户收藏的路线
     * 注意：由于单向关联，需要通过 UserFavoriteRouteRepository 查询
     * 此方法暂时返回空结果
     */
    @Query("SELECT r FROM Route r WHERE 1=0")
    fun findUserFavoriteRoutes(@Param("userId") userId: String, pageable: Pageable): Page<Route>

    /**
     * 查找用户完成的路线
     * 注意：由于单向关联，需要通过 UserCompletedRouteRepository 查询
     * 此方法暂时返回空结果
     */
    @Query("SELECT r FROM Route r WHERE 1=0")
    fun findUserCompletedRoutes(@Param("userId") userId: String, pageable: Pageable): Page<Route>

    /**
     * 检查用户是否收藏了路线
     * 注意：由于单向关联，需要通过 UserFavoriteRouteRepository 查询
     * 此方法暂时返回 false
     */
    @Query("""
        SELECT COUNT(ufr) > 0 FROM UserFavoriteRoute ufr
        WHERE ufr.userId = :userId AND ufr.routeId = :routeId
    """)
    fun isRouteFavoritedByUser(@Param("userId") userId: String, @Param("routeId") routeId: String): Boolean

    /**
     * 检查用户是否完成了路线
     * 注意：由于单向关联，需要通过 UserCompletedRouteRepository 查询
     * 此方法暂时返回 false
     */
    @Query("""
        SELECT COUNT(ucr) > 0 FROM UserCompletedRoute ucr
        WHERE ucr.userId = :userId AND ucr.routeId = :routeId
    """)
    fun isRouteCompletedByUser(@Param("userId") userId: String, @Param("routeId") routeId: String): Boolean

    /**
     * 统计路线被收藏的次数
     * 注意：由于单向关联，简化实现
     */
    @Query("""
        SELECT COUNT(ufr) FROM UserFavoriteRoute ufr
        WHERE ufr.routeId = :routeId
    """)
    fun countRouteFavorites(@Param("routeId") routeId: String): Long

    /**
     * 统计路线被完成的次数
     * 注意：由于单向关联，简化实现
     */
    @Query("""
        SELECT COUNT(ucr) FROM UserCompletedRoute ucr
        WHERE ucr.routeId = :routeId
    """)
    fun countRouteCompletions(@Param("routeId") routeId: String): Long
}