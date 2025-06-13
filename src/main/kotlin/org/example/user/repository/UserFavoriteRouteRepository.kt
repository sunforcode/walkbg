package org.example.user.repository

import org.example.user.model.UserFavoriteRoute
import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import org.springframework.data.jpa.repository.JpaRepository
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
    fun findByUserId(userId: String): List<UserFavoriteRoute>

    /**
     * 根据路线ID查找收藏的用户
     */
    fun findByRouteId(routeId: String): List<UserFavoriteRoute>

    /**
     * 检查用户是否收藏了路线
     */
    fun existsByUserIdAndRouteId(userId: String, routeId: String): Boolean

    /**
     * 统计用户收藏的路线数量
     */
    fun countByUserId(userId: String): Long

    /**
     * 统计路线被收藏的次数
     */
    fun countByRouteId(routeId: String): Long

    /**
     * 分页查找用户收藏的路线
     */
    fun findByUserId(userId: String, pageable: Pageable): Page<UserFavoriteRoute>

    /**
     * 分页查找路线的收藏用户
     */
    fun findByRouteId(routeId: String, pageable: Pageable): Page<UserFavoriteRoute>

    /**
     * 按收藏时间排序查找用户收藏的路线
     */
    fun findByUserIdOrderByFavoritedAtDesc(userId: String): List<UserFavoriteRoute>

    /**
     * 分页按收藏时间排序查找用户收藏的路线
     */
    fun findByUserIdOrderByFavoritedAtDesc(userId: String, pageable: Pageable): Page<UserFavoriteRoute>

    /**
     * 查找用户最近收藏的路线（前5个）
     */
    fun findTop5ByUserIdOrderByFavoritedAtDesc(userId: String): List<UserFavoriteRoute>

    /**
     * 查找用户最近收藏的路线（前10个）
     */
    fun findTop10ByUserIdOrderByFavoritedAtDesc(userId: String): List<UserFavoriteRoute>

    /**
     * 查找用户第一个收藏记录
     */
    fun findFirstByUserIdOrderByFavoritedAtAsc(userId: String): UserFavoriteRoute?

    /**
     * 查找用户和路线的收藏记录
     */
    fun findByUserIdAndRouteId(userId: String, routeId: String): UserFavoriteRoute?

    /**
     * 查找指定时间段内用户的收藏记录
     */
    fun findByUserIdAndFavoritedAtBetween(userId: String, startDate: Instant, endDate: Instant): List<UserFavoriteRoute>

    /**
     * 分页查找指定时间段内用户的收藏记录
     */
    fun findByUserIdAndFavoritedAtBetween(userId: String, startDate: Instant, endDate: Instant, pageable: Pageable): Page<UserFavoriteRoute>

    /**
     * 查找指定时间段内的收藏记录
     */
    fun findByFavoritedAtBetween(startTime: Instant, endTime: Instant): List<UserFavoriteRoute>

    /**
     * 查找最受欢迎的路线（按收藏数排序）
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
     */
    @Query("""
        SELECT DISTINCT ufr2.routeId
        FROM UserFavoriteRoute ufr1
        JOIN UserFavoriteRoute ufr2 ON ufr1.userId != ufr2.userId
        WHERE ufr1.userId = :userId
        AND ufr2.routeId IN :favoriteRouteIds
        AND ufr2.routeId NOT IN :favoriteRouteIds
        ORDER BY ufr2.favoritedAt DESC
    """)
    fun findRecommendedRoutes(@Param("userId") userId: String, @Param("favoriteRouteIds") favoriteRouteIds: List<String>): List<String>

    /**
     * 查找相似用户（有相同收藏偏好的用户）
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
    fun deleteByUserIdAndRouteId(userId: String, routeId: String): Long

    /**
     * 删除用户的所有收藏
     */
    fun deleteByUserId(userId: String): Long
}