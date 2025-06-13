package org.example.water.service

import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import org.example.water.model.WaterPlan
import org.example.water.model.WaterDay
import java.time.Instant

/**
 * 用水计划服务接口
 */
interface WaterPlanService {

    // 基础CRUD操作
    fun getAllWaterPlans(pageable: Pageable): Page<WaterPlan>
    fun getWaterPlanById(id: String): WaterPlan?
    fun createWaterPlan(waterPlan: WaterPlan): WaterPlan
    fun updateWaterPlan(id: String, waterPlan: WaterPlan): WaterPlan?
    fun deleteWaterPlan(id: String): Boolean

    // 按行程查询
    fun getWaterPlansByTripId(tripId: String): List<WaterPlan>
    fun getWaterPlansByTripId(tripId: String, pageable: Pageable): Page<WaterPlan>

    // 按创建者查询
    fun getWaterPlansByCreator(createdBy: String): List<WaterPlan>
    fun getWaterPlansByCreator(createdBy: String, pageable: Pageable): Page<WaterPlan>

    // 搜索功能
    fun searchWaterPlans(
        keyword: String?,
        tripId: String?,
        createdBy: String?,
        pageable: Pageable
    ): Page<WaterPlan>

    fun searchByName(name: String): List<WaterPlan>

    // 统计功能
    fun getWaterPlanStatistics(): Map<String, Any>
    fun getWaterPlanCountByCreator(): List<Array<Any>>
    fun countByTripId(tripId: String): Long
    fun countByCreator(createdBy: String): Long

    // 行程关联检查
    fun existsByTripId(tripId: String): Boolean
    fun getWaterPlansWithoutTrip(): List<WaterPlan>
    fun getWaterPlansWithTrip(): List<WaterPlan>

    // 时间范围查询
    fun getWaterPlansByCreatedAtBetween(startTime: Instant, endTime: Instant): List<WaterPlan>
    fun getRecentWaterPlansByCreator(createdBy: String, pageable: Pageable): Page<WaterPlan>
    fun getLatestWaterPlans(): List<WaterPlan>

    // 用水计划详情
    fun getWaterPlanWithDays(id: String): WaterPlan?
    fun getWaterDaysByPlanId(waterPlanId: String): List<WaterDay>

    // 水源统计
    fun getWaterSourceCount(waterPlanId: String): Long?
    fun getTotalEstimatedVolume(waterPlanId: String): Long?

    // 批量操作
    fun deleteByTripId(tripId: String): Long
    fun deleteByCreator(createdBy: String): Long

    // 验证
    fun existsById(id: String): Boolean
    fun validateWaterPlan(waterPlan: WaterPlan): Boolean

    // 关键词搜索
    fun findByKeyword(keyword: String): List<WaterPlan>
}