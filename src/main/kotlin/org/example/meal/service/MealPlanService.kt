package org.example.meal.service

import org.example.meal.model.MealDay
import org.example.meal.model.MealPlan
import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import java.time.Instant

/**
 * 餐食计划服务接口
 */
interface MealPlanService {

    // 基础CRUD操作
    fun getAllMealPlans(pageable: Pageable): Page<MealPlan>
    fun getMealPlanById(id: String): MealPlan?
    fun createMealPlan(mealPlan: MealPlan): MealPlan
    fun updateMealPlan(id: String, mealPlan: MealPlan): MealPlan?
    fun deleteMealPlan(id: String): Boolean

    // 按行程查询
    fun getMealPlansByTripId(tripId: String): List<MealPlan>
    fun getMealPlansByTripId(tripId: String, pageable: Pageable): Page<MealPlan>

    // 按创建者查询
    fun getMealPlansByCreator(createdBy: String): List<MealPlan>
    fun getMealPlansByCreator(createdBy: String, pageable: Pageable): Page<MealPlan>

    // 搜索功能
    fun searchMealPlans(
        keyword: String?,
        tripId: String?,
        createdBy: String?,
        pageable: Pageable
    ): Page<MealPlan>

    fun searchByName(name: String): List<MealPlan>

    // 统计功能
    fun getMealPlanStatistics(): Map<String, Any>
    fun getMealPlanCountByCreator(): List<Array<Any>>
    fun countByTripId(tripId: String): Long
    fun countByCreator(createdBy: String): Long

    // 行程关联检查
    fun existsByTripId(tripId: String): Boolean
    fun getMealPlansWithoutTrip(): List<MealPlan>
    fun getMealPlansWithTrip(): List<MealPlan>

    // 时间范围查询
    fun getMealPlansByCreatedAtBetween(startTime: Instant, endTime: Instant): List<MealPlan>
    fun getRecentMealPlansByCreator(createdBy: String, pageable: Pageable): Page<MealPlan>
    fun getLatestMealPlans(): List<MealPlan>

    // 餐食计划详情
    fun getMealPlanWithDays(id: String): MealPlan?
    fun getMealDaysByPlanId(mealPlanId: String): List<MealDay>

    // 批量操作
    fun deleteByTripId(tripId: String): Long
    fun deleteByCreator(createdBy: String): Long

    // 验证
    fun existsById(id: String): Boolean
    fun validateMealPlan(mealPlan: MealPlan): Boolean

    // 关键词搜索
    fun findByKeyword(keyword: String): List<MealPlan>
}