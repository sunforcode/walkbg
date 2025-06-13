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
    
    // TODO: Waypoint 实体中没有 routeId 属性，这些方法暂时注释掉
    // fun findByRouteIdOrderBySequenceNumber(routeId: String): List<Waypoint>
    // fun findByRouteId(routeId: String, pageable: Pageable): Page<Waypoint>
    
    /**
     * 根据类型查找路径点
     */
    fun findByType(type: String): List<Waypoint>
    
    // TODO: Waypoint 实体中没有 routeId 属性，这些方法暂时注释掉
    // fun findByRouteIdAndType(routeId: String, type: String): List<Waypoint>
    // fun countByRouteId(routeId: String): Long
    
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
    
    // TODO: Waypoint 实体中没有 routeId 属性，这个方法暂时注释掉
    // fun deleteByRouteId(routeId: String): Int
}