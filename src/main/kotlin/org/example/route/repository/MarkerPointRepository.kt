package org.example.route.repository

import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.stereotype.Repository
import org.example.route.model.MarkerPoint

@Repository
interface MarkerPointRepository : JpaRepository<MarkerPoint, String> {
    
    /**
     * 根据路线ID查找所有标记点
     */
    fun findByRouteId(routeId: String): List<MarkerPoint>
    
    /**
     * 统计路线的标记点数量
     */
    fun countByRouteId(routeId: String): Long
    
    /**
     * 根据标记点类型查找
     */
    fun findByMarkerType(markerType: Int): List<MarkerPoint>
    
    /**
     * 删除路线的所有标记点
     */
    fun deleteByRouteId(routeId: String): Long
}
