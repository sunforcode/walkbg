package org.example.route.repository

import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.stereotype.Repository
import org.example.route.model.DailyPlan

@Repository
interface DailyPlanRepository : JpaRepository<DailyPlan, String> {
    
    /**
     * 根据路线ID查找所有日程计划
     */
    fun findByRouteId(routeId: String): List<DailyPlan>
    
    /**
     * 统计路线的日程计划数量
     */
    fun countByRouteId(routeId: String): Long
    
    /**
     * 删除路线的所有日程计划
     */
    fun deleteByRouteId(routeId: String): Long
}
