package org.example.route.repository

import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.query.Param
import org.springframework.stereotype.Repository
import org.example.route.model.UserRouteCompletion
import java.time.Instant

@Repository
interface UserRouteCompletionRepository : JpaRepository<UserRouteCompletion, Long> {
    
    /**
     * 检查用户是否完成过路线
     */
    fun existsByUserIdAndRouteId(userId: String, routeId: String): Boolean
    
    /**
     * 根据用户ID和路线ID查找完成记录
     */
    fun findByUserIdAndRouteId(userId: String, routeId: String): UserRouteCompletion?
    
    /**
     * 根据用户ID查找所有完成的路线
     */
    fun findByUserId(userId: String, pageable: Pageable): Page<UserRouteCompletion>
    
    /**
     * 根据用户ID查找所有完成的路线（不分页，按完成时间倒序）
     */
    fun findByUserIdOrderByCompletedAtDesc(userId: String): List<UserRouteCompletion>
    
    /**
     * 根据路线ID查找所有完成过该路线的用户
     */
    fun findByRouteId(routeId: String, pageable: Pageable): Page<UserRouteCompletion>
    
    /**
     * 统计用户完成的路线数量
     */
    fun countByUserId(userId: String): Long
    
    /**
     * 统计路线被完成的次数
     */
    fun countByRouteId(routeId: String): Long
    
    /**
     * 查询用户在某个时间范围内完成的路线
     */
    @Query("SELECT u FROM UserRouteCompletion u WHERE u.userId = :userId AND u.completedAt BETWEEN :startTime AND :endTime ORDER BY u.completedAt DESC")
    fun findByUserIdAndCompletedAtBetween(
        @Param("userId") userId: String,
        @Param("startTime") startTime: Instant,
        @Param("endTime") endTime: Instant
    ): List<UserRouteCompletion>
}
