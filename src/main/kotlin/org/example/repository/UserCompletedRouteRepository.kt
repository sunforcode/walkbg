package org.example.repository

import org.example.model.UserCompletedRoute
import org.example.model.UserCompletedRouteId
import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.query.Param
import org.springframework.stereotype.Repository
import java.time.Instant

/**
 * 用户完成路线关联表Repository
 */
@Repository
interface UserCompletedRouteRepository : JpaRepository<UserCompletedRoute, UserCompletedRouteId> {

    /**
     * 根据用户ID查找完成的路线
     */
    fun findByUserId(userId: String): List<UserCompletedRoute>

    /**
     * 根据路线ID查找完成的用户
     */
    fun findByRouteId(routeId: String): List<UserCompletedRoute>

    /**
     * 检查用户是否完成了路线
     */
    fun existsByUserIdAndRouteId(userId: String, routeId: String): Boolean

    /**
     * 统计用户完成的路线数量
     */
    fun countByUserId(userId: String): Long

    /**
     * 统计路线被完成的次数
     */
    fun countByRouteId(routeId: String): Long

    /**
     * 分页查找用户完成的路线
     */
    fun findByUserId(userId: String, pageable: Pageable): Page<UserCompletedRoute>

    /**
     * 分页查找路线的完成用户
     */
    fun findByRouteId(routeId: String, pageable: Pageable): Page<UserCompletedRoute>

    /**
     * 按完成时间排序查找用户完成的路线
     */
    fun findByUserIdOrderByCompletedAtDesc(userId: String): List<UserCompletedRoute>

    /**
     * 分页按完成时间排序查找用户完成的路线
     */
    fun findByUserIdOrderByCompletedAtDesc(userId: String, pageable: Pageable): Page<UserCompletedRoute>

    /**
     * 查找用户和路线的完成记录
     */
    fun findByUserIdAndRouteId(userId: String, routeId: String): UserCompletedRoute?

    /**
     * 查找用户最近完成的路线（前5个）
     */
    fun findTop5ByUserIdOrderByCompletedAtDesc(userId: String): List<UserCompletedRoute>

    /**
     * 查找用户最近完成的路线（前10个）
     */
    fun findTop10ByUserIdOrderByCompletedAtDesc(userId: String): List<UserCompletedRoute>

    /**
     * 查找用户第一个完成记录
     */
    fun findFirstByUserIdOrderByCompletedAtAsc(userId: String): UserCompletedRoute?

    /**
     * 查找指定时间段内用户的完成记录
     */
    fun findByUserIdAndCompletedAtBetween(userId: String, startDate: Instant, endDate: Instant): List<UserCompletedRoute>

    /**
     * 分页查找指定时间段内用户的完成记录
     */
    fun findByUserIdAndCompletedAtBetween(userId: String, startDate: Instant, endDate: Instant, pageable: Pageable): Page<UserCompletedRoute>

    /**
     * 查找指定时间段内的完成记录
     */
    fun findByCompletedAtBetween(startTime: Instant, endTime: Instant): List<UserCompletedRoute>

    /**
     * 查找最热门的路线（按完成数排序）
     */
    @Query("""
        SELECT ucr.routeId, COUNT(ucr) as completionCount
        FROM UserCompletedRoute ucr
        GROUP BY ucr.routeId
        ORDER BY completionCount DESC
    """)
    fun findMostCompletedRoutes(pageable: Pageable): Page<Array<Any>>

    /**
     * 查找用户在指定年份完成的路线
     */
    @Query("""
        SELECT ucr FROM UserCompletedRoute ucr 
        WHERE ucr.userId = :userId 
        AND YEAR(ucr.completedAt) = :year
        ORDER BY ucr.completedAt DESC
    """)
    fun findUserCompletionsByYear(@Param("userId") userId: String, @Param("year") year: Int): List<UserCompletedRoute>

    /**
     * 查找用户在指定月份完成的路线
     */
    @Query("""
        SELECT ucr FROM UserCompletedRoute ucr 
        WHERE ucr.userId = :userId 
        AND YEAR(ucr.completedAt) = :year 
        AND MONTH(ucr.completedAt) = :month
        ORDER BY ucr.completedAt DESC
    """)
    fun findUserCompletionsByMonth(
        @Param("userId") userId: String, 
        @Param("year") year: Int, 
        @Param("month") month: Int
    ): List<UserCompletedRoute>

    /**
     * 统计用户每年完成的路线数量
     */
    @Query("""
        SELECT YEAR(ucr.completedAt) as year, COUNT(ucr) as count
        FROM UserCompletedRoute ucr 
        WHERE ucr.userId = :userId
        GROUP BY YEAR(ucr.completedAt)
        ORDER BY year DESC
    """)
    fun getUserYearlyCompletionStats(@Param("userId") userId: String): List<Array<Any>>

    /**
     * 统计用户每月完成的路线数量
     */
    @Query("""
        SELECT MONTH(ucr.completedAt) as month, COUNT(ucr) as count
        FROM UserCompletedRoute ucr
        WHERE ucr.userId = :userId
        AND YEAR(ucr.completedAt) = :year
        GROUP BY MONTH(ucr.completedAt)
        ORDER BY month
    """)
    fun getUserMonthlyCompletionStats(@Param("userId") userId: String, @Param("year") year: Int): List<Array<Any>>

    /**
     * 获取完成路线排行榜
     */
    @Query("""
        SELECT ucr.userId, COUNT(ucr) as completionCount
        FROM UserCompletedRoute ucr
        GROUP BY ucr.userId
        ORDER BY completionCount DESC
    """)
    fun getTopCompletionUsers(): List<Array<Any>>

    /**
     * 分页获取完成路线排行榜
     */
    @Query("""
        SELECT ucr.userId, COUNT(ucr) as completionCount
        FROM UserCompletedRoute ucr
        GROUP BY ucr.userId
        ORDER BY completionCount DESC
    """)
    fun getTopCompletionUsers(pageable: Pageable): Page<Array<Any>>

    /**
     * 获取用户完成路线排名
     */
    @Query(value = """
        SELECT COUNT(DISTINCT user_stats.user_id) + 1
        FROM (
            SELECT user_id, COUNT(*) as completion_count
            FROM user_completed_routes
            GROUP BY user_id
        ) user_stats
        WHERE user_stats.completion_count > (
            SELECT COUNT(*)
            FROM user_completed_routes
            WHERE user_id = :userId
        )
    """, nativeQuery = true)
    fun getUserCompletionRank(@Param("userId") userId: String): Int?

    /**
     * 推荐路线（基于用户完成历史）
     */
    @Query("""
        SELECT DISTINCT ucr2.routeId
        FROM UserCompletedRoute ucr1
        JOIN UserCompletedRoute ucr2 ON ucr1.userId != ucr2.userId
        WHERE ucr1.userId = :userId
        AND ucr2.routeId NOT IN :completedRouteIds
        ORDER BY ucr2.completedAt DESC
    """)
    fun findRecommendedRoutes(@Param("userId") userId: String, @Param("completedRouteIds") completedRouteIds: List<String>): List<String>

    /**
     * 查找相似用户（有相同完成偏好的用户）
     */
    @Query("""
        SELECT DISTINCT ucr2.userId
        FROM UserCompletedRoute ucr1
        JOIN UserCompletedRoute ucr2 ON ucr1.routeId = ucr2.routeId
        WHERE ucr1.userId = :userId
        AND ucr2.userId != :userId
        AND ucr1.routeId IN :completedRouteIds
        GROUP BY ucr2.userId
        HAVING COUNT(ucr2.userId) >= 2
        ORDER BY COUNT(ucr2.userId) DESC
    """)
    fun findSimilarUsers(@Param("userId") userId: String, @Param("completedRouteIds") completedRouteIds: List<String>): List<String>

    /**
     * 删除用户对路线的完成记录
     */
    fun deleteByUserIdAndRouteId(userId: String, routeId: String): Long

    /**
     * 删除用户的所有完成记录
     */
    fun deleteByUserId(userId: String): Long
}