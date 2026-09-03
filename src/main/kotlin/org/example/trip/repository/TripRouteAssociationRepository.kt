package org.example.trip.repository

import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.query.Param
import org.springframework.stereotype.Repository
import org.example.trip.model.TripRouteAssociation
import org.example.trip.model.TripRouteId

/**
 * 行程路线关联表Repository
 */
@Repository
interface TripRouteAssociationRepository : JpaRepository<TripRouteAssociation, TripRouteId> {

    /**
     * 根据行程ID查找关联的路线
     */
    fun findByTripId(tripId: String): List<TripRouteAssociation>

    /**
     * 分页根据行程ID查找关联的路线
     */
    fun findByTripId(tripId: String, pageable: Pageable): Page<TripRouteAssociation>

    /**
     * 批量根据行程ID集合查找关联的路线
     *
     * 用于列表场景一次性取回全部关联，避免逐个行程查询造成的 N+1。
     */
    fun findByTripIdIn(tripIds: List<String>): List<TripRouteAssociation>

    /**
     * 根据路线ID查找关联的行程
     */
    fun findByRouteId(routeId: String): List<TripRouteAssociation>

    /**
     * 分页根据路线ID查找关联的行程
     */
    fun findByRouteId(routeId: String, pageable: Pageable): Page<TripRouteAssociation>

    /**
     * 根据行程ID和路线ID查找关联记录
     */
    fun findByTripIdAndRouteId(tripId: String, routeId: String): TripRouteAssociation?

    /**
     * 检查行程是否关联了路线
     */
    fun existsByTripIdAndRouteId(tripId: String, routeId: String): Boolean

    /**
     * 统计行程关联的路线数量
     */
    fun countByTripId(tripId: String): Long

    /**
     * 统计路线被关联的行程数量
     */
    fun countByRouteId(routeId: String): Long

    /**
     * 查找行程的主路线
     */
    fun findByTripIdAndIsPrimaryTrue(tripId: String): TripRouteAssociation?

    /**
     * 检查行程是否有主路线
     */
    fun existsByTripIdAndIsPrimaryTrue(tripId: String): Boolean

    /**
     * 统计路线作为主路线的使用次数
     */
    fun countByRouteIdAndIsPrimaryTrue(routeId: String): Long

    /**
     * 查找行程的非主路线
     */
    fun findByTripIdAndIsPrimaryFalse(tripId: String): List<TripRouteAssociation>

    /**
     * 查找作为主路线的所有关联记录
     */
    fun findByIsPrimaryTrue(): List<TripRouteAssociation>

    /**
     * 删除行程和路线的关联
     */
    fun deleteByTripIdAndRouteId(tripId: String, routeId: String): Long

    /**
     * 删除行程的所有路线关联
     */
    fun deleteByTripId(tripId: String): Long

    /**
     * 删除路线的所有行程关联
     */
    fun deleteByRouteId(routeId: String): Long

    /**
     * 推荐路线（基于相似行程）
     */
    @Query("""
        SELECT DISTINCT tra2.routeId
        FROM TripRouteAssociation tra1
        JOIN TripRouteAssociation tra2 ON tra1.tripId != tra2.tripId
        WHERE tra1.tripId = :tripId
        AND tra2.routeId NOT IN :currentRoutes
        AND tra1.routeId = tra2.routeId
        GROUP BY tra2.routeId
        ORDER BY COUNT(tra2.routeId) DESC
    """)
    fun findRecommendedRoutes(
        @Param("tripId") tripId: String,
        @Param("currentRoutes") currentRoutes: List<String>
    ): List<String>

    /**
     * 查找相似的行程（基于路线相似性）
     */
    @Query("""
        SELECT DISTINCT tra2.tripId
        FROM TripRouteAssociation tra1
        JOIN TripRouteAssociation tra2 ON tra1.routeId = tra2.routeId
        WHERE tra1.tripId = :tripId
        AND tra2.tripId != :tripId
        AND tra1.routeId IN :currentRoutes
        GROUP BY tra2.tripId
        HAVING COUNT(tra2.tripId) >= 2
        ORDER BY COUNT(tra2.tripId) DESC
    """)
    fun findSimilarTrips(
        @Param("tripId") tripId: String,
        @Param("currentRoutes") currentRoutes: List<String>
    ): List<String>

    /**
     * 查找最受欢迎的路线（按关联行程数排序）
     */
    @Query("""
        SELECT tra.routeId, COUNT(tra) as tripCount
        FROM TripRouteAssociation tra
        GROUP BY tra.routeId
        ORDER BY tripCount DESC
    """)
    fun findMostPopularRoutes(): List<Array<Any>>

    /**
     * 查找最活跃的行程（按关联路线数排序）
     */
    @Query("""
        SELECT tra.tripId, COUNT(tra) as routeCount
        FROM TripRouteAssociation tra
        GROUP BY tra.tripId
        ORDER BY routeCount DESC
    """)
    fun findMostActiveTrips(): List<Array<Any>>

    /**
     * 统计主路线和非主路线的数量
     */
    @Query("""
        SELECT tra.isPrimary, COUNT(tra) as count
        FROM TripRouteAssociation tra
        WHERE tra.tripId = :tripId
        GROUP BY tra.isPrimary
    """)
    fun getRouteTypeStats(@Param("tripId") tripId: String): List<Array<Any>>
}