package org.example.route.repository

import org.example.route.model.RouteMapData
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.stereotype.Repository

@Repository
interface RouteMapDataRepository : JpaRepository<RouteMapData, String> {
    
    /**
     * 根据路线ID查找地图数据
     */
    fun findByIdStartingWith(routeIdPrefix: String): List<RouteMapData>
}
