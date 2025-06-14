package org.example.route.repository

import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.query.Param
import org.springframework.stereotype.Repository
import org.example.route.model.Segment

@Repository
interface SegmentRepository : JpaRepository<Segment, String> {

    /**
     * 根据路线ID查找路段
     */
    @Query("SELECT s FROM Segment s JOIN s.route r WHERE r.id = :routeId")
    fun findByRouteId(@Param("routeId") routeId: String): List<Segment>

    /**
     * 根据路线ID查找路段（分页）
     */
    @Query("SELECT s FROM Segment s JOIN s.route r WHERE r.id = :routeId")
    fun findByRouteId(@Param("routeId") routeId: String, pageable: Pageable): Page<Segment>

    /**
     * 根据难度查找路段
     */
    fun findByDifficulty(difficulty: Int): List<Segment>



    /**
     * 统计路线的路段数量
     */
    @Query("SELECT COUNT(s) FROM Segment s JOIN s.route r WHERE r.id = :routeId")
    fun countByRouteId(@Param("routeId") routeId: String): Long

    /**
     * 根据距离范围查找路段
     */
    @Query("SELECT s FROM Segment s WHERE s.distance BETWEEN :minDistance AND :maxDistance")
    fun findByDistanceRange(
        @Param("minDistance") minDistance: Double,
        @Param("maxDistance") maxDistance: Double
    ): List<Segment>

    /**
     * 根据海拔增益范围查找路段
     */
    @Query("SELECT s FROM Segment s WHERE s.elevationGain BETWEEN :minGain AND :maxGain")
    fun findByElevationGainRange(
        @Param("minGain") minGain: Double,
        @Param("maxGain") maxGain: Double
    ): List<Segment>

    /**
     * 删除路线的所有路段
     */
    @Query("DELETE FROM Segment s WHERE s.route.id = :routeId")
    fun deleteByRouteId(@Param("routeId") routeId: String): Int

    /**
     * 统计路线的总距离
     */
    @Query("SELECT COALESCE(SUM(s.distance), 0) FROM Segment s WHERE s.route.id = :routeId")
    fun sumDistanceByRouteId(@Param("routeId") routeId: String): Double

    /**
     * 统计路线的总海拔增益
     */
    @Query("SELECT COALESCE(SUM(s.elevationGain), 0) FROM Segment s WHERE s.route.id = :routeId")
    fun sumElevationGainByRouteId(@Param("routeId") routeId: String): Double

    /**
     * 统计路线的总海拔损失
     */
    @Query("SELECT COALESCE(SUM(s.elevationLoss), 0) FROM Segment s WHERE s.route.id = :routeId")
    fun sumElevationLossByRouteId(@Param("routeId") routeId: String): Double
}
