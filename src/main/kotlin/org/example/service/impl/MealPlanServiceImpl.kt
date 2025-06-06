package org.example.service.impl

import org.example.model.MealPlan
import org.example.model.MealDay
import org.example.repository.MealPlanRepository
import org.example.repository.MealDayRepository
import org.example.service.MealPlanService
import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.time.Instant

/**
 * 餐食计划服务实现类
 */
@Service
@Transactional
class MealPlanServiceImpl(
    private val mealPlanRepository: MealPlanRepository,
    private val mealDayRepository: MealDayRepository
) : MealPlanService {

    // 基础CRUD操作
    override fun getAllMealPlans(pageable: Pageable): Page<MealPlan> {
        return mealPlanRepository.findAll(pageable)
    }

    override fun getMealPlanById(id: String): MealPlan? {
        return mealPlanRepository.findById(id).orElse(null)
    }

    override fun createMealPlan(mealPlan: MealPlan): MealPlan {
        return mealPlanRepository.save(mealPlan)
    }

    override fun updateMealPlan(id: String, mealPlan: MealPlan): MealPlan? {
        return if (mealPlanRepository.existsById(id)) {
            val updated = mealPlan.copy(id = id, updatedAt = Instant.now())
            mealPlanRepository.save(updated)
        } else {
            null
        }
    }

    override fun deleteMealPlan(id: String): Boolean {
        return if (mealPlanRepository.existsById(id)) {
            mealPlanRepository.deleteById(id)
            true
        } else {
            false
        }
    }

    // 按行程查询
    override fun getMealPlansByTripId(tripId: String): List<MealPlan> {
        return mealPlanRepository.findByTripId(tripId)
    }

    override fun getMealPlansByTripId(tripId: String, pageable: Pageable): Page<MealPlan> {
        return mealPlanRepository.findByTripId(tripId, pageable)
    }

    // 按创建者查询
    override fun getMealPlansByCreator(createdBy: String): List<MealPlan> {
        return mealPlanRepository.findByCreatedBy(createdBy)
    }

    override fun getMealPlansByCreator(createdBy: String, pageable: Pageable): Page<MealPlan> {
        return mealPlanRepository.findByCreatedBy(createdBy, pageable)
    }

    // 搜索功能
    override fun searchMealPlans(
        keyword: String?,
        tripId: String?,
        createdBy: String?,
        pageable: Pageable
    ): Page<MealPlan> {
        return mealPlanRepository.searchMealPlans(keyword, tripId, createdBy, pageable)
    }

    override fun searchByName(name: String): List<MealPlan> {
        return mealPlanRepository.findByNameContainingIgnoreCase(name)
    }

    // 统计功能
    override fun getMealPlanStatistics(): Map<String, Any> {
        return mealPlanRepository.getMealPlanStatistics()
    }

    override fun getMealPlanCountByCreator(): List<Array<Any>> {
        return mealPlanRepository.getMealPlanCountByCreator()
    }

    override fun countByTripId(tripId: String): Long {
        return mealPlanRepository.countByTripId(tripId)
    }

    override fun countByCreator(createdBy: String): Long {
        return mealPlanRepository.countByCreatedBy(createdBy)
    }

    // 行程关联检查
    override fun existsByTripId(tripId: String): Boolean {
        return mealPlanRepository.existsByTripId(tripId)
    }

    override fun getMealPlansWithoutTrip(): List<MealPlan> {
        return mealPlanRepository.findByTripIdIsNull()
    }

    override fun getMealPlansWithTrip(): List<MealPlan> {
        return mealPlanRepository.findByTripIdIsNotNull()
    }

    // 时间范围查询
    override fun getMealPlansByCreatedAtBetween(startTime: Instant, endTime: Instant): List<MealPlan> {
        return mealPlanRepository.findByCreatedAtBetween(startTime, endTime)
    }

    override fun getRecentMealPlansByCreator(createdBy: String, pageable: Pageable): Page<MealPlan> {
        return mealPlanRepository.findRecentMealPlansByCreator(createdBy, pageable)
    }

    override fun getLatestMealPlans(): List<MealPlan> {
        return mealPlanRepository.findTop10ByOrderByCreatedAtDesc()
    }

    // 餐食计划详情
    override fun getMealPlanWithDays(id: String): MealPlan? {
        val mealPlan = getMealPlanById(id)
        if (mealPlan != null) {
            // 预加载餐食天数数据
            mealPlan.mealDays.size // 触发懒加载
        }
        return mealPlan
    }

    override fun getMealDaysByPlanId(mealPlanId: String): List<MealDay> {
        return mealDayRepository.findByMealPlanIdOrderByDayNumber(mealPlanId)
    }

    // 批量操作
    override fun deleteByTripId(tripId: String): Long {
        return mealPlanRepository.deleteByTripId(tripId)
    }

    override fun deleteByCreator(createdBy: String): Long {
        return mealPlanRepository.deleteByCreatedBy(createdBy)
    }

    // 验证
    override fun existsById(id: String): Boolean {
        return mealPlanRepository.existsById(id)
    }

    override fun validateMealPlan(mealPlan: MealPlan): Boolean {
        return mealPlan.name.isNotBlank() && 
               mealPlan.createdBy?.isNotBlank() == true
    }

    // 关键词搜索
    override fun findByKeyword(keyword: String): List<MealPlan> {
        return mealPlanRepository.findByKeyword(keyword)
    }
}