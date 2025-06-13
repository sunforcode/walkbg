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
    @Query("SELECT ufr FROM UserFavoriteRoute ufr JOIN ufr.user u WHERE u.id = :userId")
    fun findByUserId(@Param("userId") userId: String): List<UserFavoriteRoute>

    /**
     * 根据路线ID查找收藏的用户
     */
    @Query("SELECT ufr FROM UserFavoriteRoute ufr JOIN ufr.route r WHERE r.id = :routeId")
    fun findByRouteId(@Param("routeId") routeId: String): List<UserFavoriteRoute>

    /**
     * 检查用户是否收藏了路线
     */
    @Query("SELECT COUNT(ufr) > 0 FROM UserFavoriteRoute ufr JOIN ufr.user u JOIN ufr.route r WHERE u.id = :userId AND r.id = :routeId")
    fun existsByUserIdAndRouteId(@Param("userId") userId: String, @Param("routeId") routeId: String): Boolean

    /**
     * 统计用户收藏的路线数量
     */
    @Query("SELECT COUNT(ufr) FROM UserFavoriteRoute ufr JOIN ufr.user u WHERE u.id = :userId")
    fun countByUserId(@Param("userId") userId: String): Long

    /**
     * 统计路线被收藏的次数
     */
    @Query("SELECT COUNT(ufr) FROM UserFavoriteRoute ufr JOIN ufr.route r WHERE r.id = :routeId")
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
     */
    @Query("""
        SELECT r.id, COUNT(ufr) as favoriteCount
        FROM UserFavoriteRoute ufr
        JOIN ufr.route r
        GROUP BY r.id
        ORDER BY favoriteCount DESC
    """)
    fun findMostFavoritedRoutes(pageable: Pageable): Page<Array<Any>>

    /**
     * 推荐路线（基于用户收藏历史）
     */
    @Query("""
        SELECT DISTINCT r2.id
        FROM UserFavoriteRoute ufr1
        JOIN ufr1.user u1
        JOIN ufr1.route r1
        JOIN UserFavoriteRoute ufr2 ON u1.id != ufr2.user.id
        JOIN ufr2.route r2
        WHERE u1.id = :userId
        AND r1.id IN :favoriteRouteIds
        AND r2.id NOT IN :favoriteRouteIds
        ORDER BY ufr2.createdAt DESC
    """)
    fun findRecommendedRoutes(@Param("userId") userId: String, @Param("favoriteRouteIds") favoriteRouteIds: List<String>): List<String>

    /**
     * 查找相似用户（有相同收藏偏好的用户）
     */
    @Query("""
        SELECT DISTINCT u2.id
        FROM UserFavoriteRoute ufr1
        JOIN ufr1.user u1
        JOIN ufr1.route r1
        JOIN UserFavoriteRoute ufr2 ON r1.id = ufr2.route.id
        JOIN ufr2.user u2
        WHERE u1.id = :userId
        AND u2.id != :userId
        AND r1.id IN :favoriteRouteIds
        GROUP BY u2.id
        HAVING COUNT(u2.id) >= 2
        ORDER BY COUNT(u2.id) DESC
    """)
    fun findSimilarUsers(@Param("userId") userId: String, @Param("favoriteRouteIds") favoriteRouteIds: List<String>): List<String>

    /**
     * 删除用户对路线的收藏
     */
    @Query("DELETE FROM UserFavoriteRoute ufr WHERE ufr.user.id = :userId AND ufr.route.id = :routeId")
    @Modifying
    fun deleteByUserIdAndRouteId(@Param("userId") userId: String, @Param("routeId") routeId: String): Long

    /**
     * 删除用户的所有收藏
     */
    @Query("DELETE FROM UserFavoriteRoute ufr WHERE ufr.user.id = :userId")
    @Modifying
    fun deleteByUserId(@Param("userId") userId: String): Long
}