package org.example.trip.repository

import org.example.trip.model.Trip
import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.query.Param
import org.springframework.stereotype.Repository
import java.time.Instant

/**
 * 行程数据访问层
 */
@Repository
interface TripRepository : JpaRepository<Trip, String> {
    
    /**
     * 根据名称查找行程
     */
    fun findByNameContainingIgnoreCase(name: String, pageable: Pageable): Page<Trip>
    
    /**
     * 根据组织者ID查找行程
     */
    fun findByOrganizerId(organizerId: String, pageable: Pageable): Page<Trip>
    
    /**
     * 根据状态查找行程
     */
    fun findByStatus(status: Int, pageable: Pageable): Page<Trip>
    
    /**
     * 根据隐私设置查找行程
     */
    fun findByPrivacySetting(privacySetting: Int, pageable: Pageable): Page<Trip>
    
    /**
     * 查找指定日期之后开始的行程
     */
    fun findByStartDateAfter(date: Instant, pageable: Pageable): Page<Trip>
    
    /**
     * 查找指定日期之前开始的行程
     */
    fun findByStartDateBefore(date: Instant, pageable: Pageable): Page<Trip>
    
    /**
     * 查找指定日期范围内的行程
     */
    fun findByStartDateBetween(startDate: Instant, endDate: Instant, pageable: Pageable): Page<Trip>

    /**
     * 根据参与者用户ID查找行程
     */
    @Query("""
        SELECT t FROM Trip t
        JOIN TripParticipant tp ON tp.tripId = t.id
        WHERE tp.userId = :userId
    """)
    fun findByParticipantUserId(@Param("userId") userId: String, pageable: Pageable): Page<Trip>
    
    /**
     * 根据路线ID查找行程
     */
    @Query("""
        SELECT t FROM Trip t
        JOIN TripRouteAssociation tra ON tra.tripId = t.id
        WHERE tra.routeId = :routeId
    """)
    fun findByRouteId(@Param("routeId") routeId: String, pageable: Pageable): Page<Trip>
    
    /**
     * 查找最近创建的行程
     */
    fun findTop10ByOrderByCreatedAtDesc(): List<Trip>

    /**
     * 查找即将开始的行程
     */
    @Query("""
        SELECT t FROM Trip t
        WHERE t.startDate > :now
        AND t.status = :status
        ORDER BY t.startDate ASC
    """)
    fun findUpcomingTrips(
        @Param("now") now: Instant,
        @Param("status") status: Int,
        pageable: Pageable
    ): Page<Trip>

    /**
     * 多条件搜索行程
     */
    @Query("""
        SELECT DISTINCT t FROM Trip t
        LEFT JOIN TripParticipant tp ON tp.tripId = t.id
        LEFT JOIN TripRouteAssociation tra ON tra.tripId = t.id
        WHERE (:organizerId IS NULL OR t.organizerId = :organizerId)
        AND (:status IS NULL OR t.status = :status)
        AND (:startDateFrom IS NULL OR t.startDate >= :startDateFrom)
        AND (:startDateTo IS NULL OR t.startDate <= :startDateTo)
        AND (:participantUserId IS NULL OR tp.userId = :participantUserId)
        AND (:routeId IS NULL OR tra.routeId = :routeId)
        AND (:privacySetting IS NULL OR t.privacySetting = :privacySetting)
        AND (:keyword IS NULL OR
             LOWER(t.name) LIKE LOWER(CONCAT('%', :keyword, '%')) OR
             LOWER(t.description) LIKE LOWER(CONCAT('%', :keyword, '%')))
    """)
    fun searchTrips(
        @Param("organizerId") organizerId: String?,
        @Param("status") status: Int?,
        @Param("startDateFrom") startDateFrom: Instant?,
        @Param("startDateTo") startDateTo: Instant?,
        @Param("participantUserId") participantUserId: String?,
        @Param("routeId") routeId: String?,
        @Param("privacySetting") privacySetting: Int?,
        @Param("keyword") keyword: String?,
        pageable: Pageable
    ): Page<Trip>

    /**
     * 获取行程统计信息
     */
    @Query("""
        SELECT new map(
            COUNT(t) as totalTrips,
            COUNT(CASE WHEN t.status = 0 THEN 1 END) as planningTrips,
            COUNT(CASE WHEN t.status = 1 THEN 1 END) as inProgressTrips,
            COUNT(CASE WHEN t.status = 2 THEN 1 END) as completedTrips,
            AVG(CAST((SELECT COUNT(tp) FROM TripParticipant tp WHERE tp.tripId = t.id) AS double)) as avgParticipants
        )
        FROM Trip t
    """)
    fun getTripStatistics(): Map<String, Any>

    /**
     * 查找用户参与的所有行程（包括组织的和参与的）
     */
    @Query("""
        SELECT DISTINCT t FROM Trip t
        LEFT JOIN TripParticipant tp ON tp.tripId = t.id
        WHERE t.organizerId = :userId OR tp.userId = :userId
    """)
    fun findAllUserTrips(@Param("userId") userId: String, pageable: Pageable): Page<Trip>

    /**
     * 查找热门行程（按参与者数量排序）
     */
    @Query("""
        SELECT t, COUNT(tp) as participantCount
        FROM Trip t
        LEFT JOIN TripParticipant tp ON tp.tripId = t.id
        GROUP BY t.id
        ORDER BY participantCount DESC
    """)
    fun findTop10PopularTrips(pageable: Pageable): Page<Trip>

    /**
     * 查找指定组织者的活跃行程
     */
    @Query("""
        SELECT t FROM Trip t
        WHERE t.organizerId = :organizerId
        AND t.status IN (0, 1)
        ORDER BY t.startDate ASC
    """)
    fun findActiveTripsForOrganizer(@Param("organizerId") organizerId: String): List<Trip>

    /**
     * 统计行程的参与者数量
     */
    @Query("""
        SELECT COUNT(tp) FROM TripParticipant tp WHERE tp.tripId = :tripId
    """)
    fun countTripParticipants(@Param("tripId") tripId: String): Long
}