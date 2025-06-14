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
     */
    @Query("SELECT r FROM Route r JOIN r.tags t WHERE t.tag = :tag")
    fun findByTagsTag(@Param("tag") tag: String, pageable: Pageable): Page<Route>
    
    
    /**
     * 根据距离范围查找路线
     * 暂时返回所有路线，后续需要实现正确的距离查询逻辑
     */
    @Query("SELECT r FROM Route r")
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
     */
    @Query("""
        SELECT DISTINCT r FROM Route r
        LEFT JOIN r.tags t
        WHERE (:keyword IS NULL OR
               LOWER(r.name) LIKE LOWER(CONCAT('%', :keyword, '%')) OR
               LOWER(r.description) LIKE LOWER(CONCAT('%', :keyword, '%')))
        AND (:region IS NULL OR r.region = :region)
        AND (:difficulty IS NULL OR r.difficulty = :difficulty)
        AND (:routeType IS NULL OR r.routeType = :routeType)
        AND (:status IS NULL OR r.status = :status)
        AND (:tag IS NULL OR t.tag = :tag)
    """)
    fun searchRoutes(
        @Param("keyword") keyword: String?,
        @Param("region") region: String?,
        @Param("difficulty") difficulty: Int?,
        @Param("routeType") routeType: Int?,
        @Param("status") status: Int?,
        @Param("tag") tag: String?,
        pageable: Pageable
    ): Page<Route>

    /**
     * 查找用户收藏的路线
     */
    @Query("""
        SELECT r FROM Route r
        JOIN UserFavoriteRoute ufr ON ufr.route = r
        JOIN User u ON ufr.user = u
        WHERE u.id = :userId
        ORDER BY ufr.createdAt DESC
    """)
    fun findUserFavoriteRoutes(@Param("userId") userId: String, pageable: Pageable): Page<Route>

    /**
     * 查找用户完成的路线
     */
    @Query("""
        SELECT r FROM Route r
        JOIN UserCompletedRoute ucr ON ucr.route = r
        JOIN User u ON ucr.user = u
        WHERE u.id = :userId
        ORDER BY ucr.completedAt DESC
    """)
    fun findUserCompletedRoutes(@Param("userId") userId: String, pageable: Pageable): Page<Route>

    /**
     * 检查用户是否收藏了路线
     */
    @Query("""
        SELECT COUNT(ufr) > 0 FROM UserFavoriteRoute ufr
        JOIN User u ON ufr.user = u
        JOIN Route r ON ufr.route = r
        WHERE u.id = :userId AND r.id = :routeId
    """)
    fun isRouteFavoritedByUser(@Param("userId") userId: String, @Param("routeId") routeId: String): Boolean

    /**
     * 检查用户是否完成了路线
     */
    @Query("""
        SELECT COUNT(ucr) > 0 FROM UserCompletedRoute ucr
        JOIN User u ON ucr.user = u
        JOIN Route r ON ucr.route = r
        WHERE u.id = :userId AND r.id = :routeId
    """)
    fun isRouteCompletedByUser(@Param("userId") userId: String, @Param("routeId") routeId: String): Boolean

    /**
     * 统计路线被收藏的次数
     */
    @Query("""
        SELECT COUNT(ufr) FROM UserFavoriteRoute ufr
        JOIN Route r ON ufr.route = r
        WHERE r.id = :routeId
    """)
    fun countRouteFavorites(@Param("routeId") routeId: String): Long

    /**
     * 统计路线被完成的次数
     */
    @Query("""
        SELECT COUNT(ucr) FROM UserCompletedRoute ucr
        JOIN Route r ON ucr.route = r
        WHERE r.id = :routeId
    """)
    fun countRouteCompletions(@Param("routeId") routeId: String): Long
}