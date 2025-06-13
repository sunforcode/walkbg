package org.example.water.service

import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import org.example.water.model.WaterPlan
import org.example.water.repository.WaterPlanRepository
import java.time.Instant

/**
 * 用水计划服务实现类
 */
@Service
@Transactional
class WaterPlanServiceImpl(
    private val waterPlanRepository: WaterPlanRepository
) : WaterPlanService {

    // 基础CRUD操作
    override fun getAllWaterPlans(pageable: Pageable): Page<WaterPlan> {
        return waterPlanRepository.findAll(pageable)
    }

    override fun getWaterPlanById(id: String): WaterPlan? {
        return waterPlanRepository.findById(id).orElse(null)
    }

    override fun createWaterPlan(waterPlan: WaterPlan): WaterPlan {
        return waterPlanRepository.save(waterPlan)
    }

    override fun updateWaterPlan(id: String, waterPlan: WaterPlan): WaterPlan? {
        return if (waterPlanRepository.existsById(id)) {
            val updated = waterPlan.copy(id = id, updatedAt = Instant.now())
            waterPlanRepository.save(updated)
        } else {
            null
        }
    }

    override fun deleteWaterPlan(id: String): Boolean {
        return if (waterPlanRepository.existsById(id)) {
            waterPlanRepository.deleteById(id)
            true
        } else {
            false
        }
    }

    // 按行程查询
    override fun getWaterPlansByTripId(tripId: String): List<WaterPlan> {
        return waterPlanRepository.findByTripId(tripId)
    }

    override fun getWaterPlansByTripId(tripId: String, pageable: Pageable): Page<WaterPlan> {
        return waterPlanRepository.findByTripId(tripId, pageable)
    }

    // 按创建者查询
    override fun getWaterPlansByCreator(createdBy: String): List<WaterPlan> {
        return waterPlanRepository.findByCreatedBy(createdBy)
    }

    override fun getWaterPlansByCreator(createdBy: String, pageable: Pageable): Page<WaterPlan> {
        return waterPlanRepository.findByCreatedBy(createdBy, pageable)
    }

    // 搜索功能
    override fun searchWaterPlans(
        keyword: String?,
        tripId: String?,
        createdBy: String?,
        pageable: Pageable
    ): Page<WaterPlan> {
        return waterPlanRepository.searchWaterPlans(keyword, tripId, createdBy, pageable)
    }

    override fun searchByName(name: String): List<WaterPlan> {
        return waterPlanRepository.findByNameContainingIgnoreCase(name)
    }

    // 统计功能
    override fun getWaterPlanStatistics(): Map<String, Any> {
        return waterPlanRepository.getWaterPlanStatistics()
    }

    override fun getWaterPlanCountByCreator(): List<Array<Any>> {
        return waterPlanRepository.getWaterPlanCountByCreator()
    }

    override fun countByTripId(tripId: String): Long {
        return waterPlanRepository.countByTripId(tripId)
    }

    override fun countByCreator(createdBy: String): Long {
        return waterPlanRepository.countByCreatedBy(createdBy)
    }

    // 行程关联检查
    override fun existsByTripId(tripId: String): Boolean {
        return waterPlanRepository.existsByTripId(tripId)
    }

    override fun getWaterPlansWithoutTrip(): List<WaterPlan> {
        return waterPlanRepository.findByTripIdIsNull()
    }

    override fun getWaterPlansWithTrip(): List<WaterPlan> {
        return waterPlanRepository.findByTripIdIsNotNull()
    }

    // 时间范围查询
    override fun getWaterPlansByCreatedAtBetween(startTime: Instant, endTime: Instant): List<WaterPlan> {
        return waterPlanRepository.findByCreatedAtBetween(startTime, endTime)
    }

    override fun getRecentWaterPlansByCreator(createdBy: String, pageable: Pageable): Page<WaterPlan> {
        return waterPlanRepository.findRecentWaterPlansByCreator(createdBy, pageable)
    }

    override fun getLatestWaterPlans(): List<WaterPlan> {
        return waterPlanRepository.findTop10ByOrderByCreatedAtDesc()
    }

    // 用水计划详情
    override fun getWaterPlanWithDays(id: String): WaterPlan? {
        val waterPlan = getWaterPlanById(id)
      
        return waterPlan
    }

    // 水源统计
    override fun getWaterSourceCount(waterPlanId: String): Long? {
        return waterPlanRepository.getWaterSourceCount(waterPlanId)
    }

    override fun getTotalEstimatedVolume(waterPlanId: String): Long? {
        return waterPlanRepository.getTotalEstimatedVolume(waterPlanId)
    }

    // 批量操作
    override fun deleteByTripId(tripId: String): Long {
        return waterPlanRepository.deleteByTripId(tripId)
    }

    override fun deleteByCreator(createdBy: String): Long {
        return waterPlanRepository.deleteByCreatedBy(createdBy)
    }

    // 验证
    override fun existsById(id: String): Boolean {
        return waterPlanRepository.existsById(id)
    }

    override fun validateWaterPlan(waterPlan: WaterPlan): Boolean {
        return waterPlan.name.isNotBlank() && 
               waterPlan.createdBy?.isNotBlank() == true
    }

    // 关键词搜索
    override fun findByKeyword(keyword: String): List<WaterPlan> {
        return waterPlanRepository.findByKeyword(keyword)
    }
}