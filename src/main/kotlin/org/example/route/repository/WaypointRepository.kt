package org.example.route.repository

import org.example.route.model.Waypoint
import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.query.Param
import org.springframework.stereotype.Repository

@Repository
interface WaypointRepository : JpaRepository<Waypoint, String> {
    
    /**
     * 根据路线 ID 查找所有路径点，按序列号升序排列
     */
    fun findByRouteIdOrderBySequenceNumberAsc(routeId: String): List<Waypoint>
    
    /**
     * 根据路线 ID 查找所有路径点
     */
    fun findByRouteId(routeId: String, pageable: Pageable): Page<Waypoint>
    
    /**
     * 根据类型查找路径点
     */
    fun findByType(type: String): List<Waypoint>
    
    /**
     * 根据路线 ID 和 类型查找路径点
     */
    fun findByRouteIdAndType(routeId: String, type: String): List<Waypoint>
    
    /**
     * 统计指定路线的路径点数量
     */
    fun countByRouteId(routeId: String): Long
    
    /**
     * 根据地理位置范围查找路径点
     */
    @Query("SELECT w FROM Waypoint w WHERE w.latitude BETWEEN :minLat AND :maxLat AND w.longitude BETWEEN :minLon AND :maxLon")
    fun findByLocationRange(
        @Param("minLat") minLatitude: Double,
        @Param("maxLat") maxLatitude: Double,
        @Param("minLon") minLongitude: Double,
        @Param("maxLon") maxLongitude: Double
    ): List<Waypoint>
    
    /**
     * 削除指定路线的所有路径点
     */
    fun deleteByRouteId(routeId: String): Int
}