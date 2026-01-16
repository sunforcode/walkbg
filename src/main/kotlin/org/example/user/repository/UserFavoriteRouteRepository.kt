package org.example.user.repository

import org.example.user.model.UserFavoriteRoute
import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Modifying
import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.query.Param
import org.springframework.stereotype.Repository
import java.time.Instant

/**
 * 用户收藏路线关联表Repository
 */
@Repository
interface UserFavoriteRouteRepository : JpaRepository<UserFavoriteRoute, String> {

    /**
     * 根据用户ID查找收藏的路线
     */
    fun findByUserId(@Param("userId") userId: String): List<UserFavoriteRoute>

    /**
     * 根据路线ID查找收藏的用户
     */
    fun findByRouteId(@Param("routeId") routeId: String): List<UserFavoriteRoute>

    /**
     * 检查用户是否收藏了路线
     */
    fun existsByUserIdAndRouteId(@Param("userId") userId: String, @Param("routeId") routeId: String): Boolean

    /**
     * 统计用户收藏的路线数量
     */
    fun countByUserId(@Param("userId") userId: String): Long

    /**
     * 统计路线被收藏的次数
     */
    fun countByRouteId(@Param("routeId") routeId: String): Long

    // TODO: 以下方法需要重新实现，暂时注释掉以便应用启动
    /*
    fun findByUserId(userId: String, pageable: Pageable): Page<UserFavoriteRoute>
    fun findByRouteId(routeId: String, pageable: Pageable): Page<UserFavoriteRoute>
    fun findByUserIdOrderByCreatedAtDesc(userId: String): List<UserFavoriteRoute>
    fun findByUserIdOrderByCreatedAtDesc(userId: String, pageable: Pageable): Page<UserFavoriteRoute>
    fun findTop5ByUserIdOrderByCreatedAtDesc(userId: String): List<UserFavoriteRoute>
    fun findTop10ByUserIdOrderByCreatedAtDesc(userId: String): List<UserFavoriteRoute>
    fun findFirstByUserIdOrderByCreatedAtAsc(userId: String): UserFavoriteRoute?
    fun findByUserIdAndRouteId(userId: String, routeId: String): UserFavoriteRoute?
    fun findByUserIdAndCreatedAtBetween(userId: String, startDate: Instant, endDate: Instant): List<UserFavoriteRoute>
    fun findByUserIdAndCreatedAtBetween(userId: String, startDate: Instant, endDate: Instant, pageable: Pageable): Page<UserFavoriteRoute>
    */

    /**
     * 查找指定时间段内的收藏记录
     */
    fun findByCreatedAtBetween(startTime: Instant, endTime: Instant): List<UserFavoriteRoute>

    /**
     * 查找最受欢迎的路线（按收藏数排序）
     * 注意：由于单向关联，简化实现
     */
    @Query("""
        SELECT ufr.routeId, COUNT(ufr) as favoriteCount
        FROM UserFavoriteRoute ufr
        GROUP BY ufr.routeId
        ORDER BY favoriteCount DESC
    """)
    fun findMostFavoritedRoutes(pageable: Pageable): Page<Array<Any>>

    /**
     * 推荐路线（基于用户收藏历史）
     * 注意：由于单向关联，简化实现
     */
    @Query("""
        SELECT DISTINCT ufr2.routeId
        FROM UserFavoriteRoute ufr1
        JOIN UserFavoriteRoute ufr2 ON ufr1.routeId = ufr2.routeId
        WHERE ufr1.userId = :userId
        AND ufr2.routeId NOT IN :favoriteRouteIds
        ORDER BY ufr2.createdAt DESC
    """)
    fun findRecommendedRoutes(@Param("userId") userId: String, @Param("favoriteRouteIds") favoriteRouteIds: List<String>): List<String>

    /**
     * 查找相似用户（有相同收藏偏好的用户）
     * 注意：由于单向关联，简化实现
     */
    @Query("""
        SELECT DISTINCT ufr2.userId
        FROM UserFavoriteRoute ufr1
        JOIN UserFavoriteRoute ufr2 ON ufr1.routeId = ufr2.routeId
        WHERE ufr1.userId = :userId
        AND ufr2.userId != :userId
        AND ufr1.routeId IN :favoriteRouteIds
        GROUP BY ufr2.userId
        HAVING COUNT(ufr2.userId) >= 2
        ORDER BY COUNT(ufr2.userId) DESC
    """)
    fun findSimilarUsers(@Param("userId") userId: String, @Param("favoriteRouteIds") favoriteRouteIds: List<String>): List<String>

    /**
     * 删除用户对路线的收藏
     */
    fun deleteByUserIdAndRouteId(@Param("userId") userId: String, @Param("routeId") routeId: String): Long

    /**
     * 删除用户的所有收藏
     */
    fun deleteByUserId(@Param("userId") userId: String): Long
}