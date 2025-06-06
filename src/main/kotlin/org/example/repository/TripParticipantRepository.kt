package org.example.repository

import org.example.model.TripParticipant
import org.example.model.TripParticipantId
import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.query.Param
import org.springframework.stereotype.Repository

/**
 * 行程参与者关联表Repository
 */
@Repository
interface TripParticipantRepository : JpaRepository<TripParticipant, TripParticipantId> {

    /**
     * 根据行程ID查找参与者
     */
    fun findByTripId(tripId: String): List<TripParticipant>

    /**
     * 根据用户ID查找参与的行程
     */
    fun findByUserId(userId: String): List<TripParticipant>

    /**
     * 根据行程ID和用户ID查找参与记录
     */
    fun findByTripIdAndUserId(tripId: String, userId: String): TripParticipant?

    /**
     * 统计行程的参与者数量
     */
    fun countByTripId(tripId: String): Long

    /**
     * 统计用户参与的行程数量
     */
    fun countByUserId(userId: String): Long

    /**
     * 根据角色查找参与者
     */
    fun findByTripIdAndRole(tripId: String, role: Int): List<TripParticipant>

    /**
     * 根据状态查找参与者
     */
    fun findByTripIdAndStatus(tripId: String, status: Int): List<TripParticipant>

    /**
     * 查找用户参与的指定状态的行程
     */
    fun findByUserIdAndStatus(userId: String, status: Int): List<TripParticipant>

    /**
     * 检查用户是否参与了行程
     */
    fun existsByTripIdAndUserId(tripId: String, userId: String): Boolean

    /**
     * 查找行程的组织者
     */
    @Query("""
        SELECT tp FROM TripParticipant tp 
        WHERE tp.tripId = :tripId AND tp.role = 1
    """)
    fun findTripOrganizers(@Param("tripId") tripId: String): List<TripParticipant>

    /**
     * 分页查找用户参与的行程
     */
    fun findByUserId(userId: String, pageable: Pageable): Page<TripParticipant>

    /**
     * 分页查找行程的参与者
     */
    fun findByTripId(tripId: String, pageable: Pageable): Page<TripParticipant>

    /**
     * 查找用户最近参与的行程
     */
    @Query("""
        SELECT tp FROM TripParticipant tp 
        WHERE tp.userId = :userId 
        ORDER BY tp.joinedAt DESC
    """)
    fun findRecentUserParticipations(@Param("userId") userId: String, pageable: Pageable): Page<TripParticipant>
}