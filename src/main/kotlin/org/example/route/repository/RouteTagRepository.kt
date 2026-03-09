package org.example.route.repository

import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.stereotype.Repository
import org.example.route.model.RouteTag

@Repository
interface RouteTagRepository : JpaRepository<RouteTag, String> {
    
    /**
     * 根据路线ID查找所有标签
     */
    fun findByRouteId(routeId: String): List<RouteTag>
    
    /**
     * 统计路线的标签数量
     */
    fun countByRouteId(routeId: String): Long
    
    /**
     * 根据标签值查找
     */
    fun findByTag(tag: String): List<RouteTag>
    
    /**
     * 删除路线的所有标签
     */
    fun deleteByRouteId(routeId: String): Long
}
