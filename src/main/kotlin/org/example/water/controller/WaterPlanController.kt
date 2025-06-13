package org.example.water.controller

import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.*
import org.example.water.service.WaterPlanService
import org.example.water.model.WaterPlan

/**
 * 用水计划控制器
 */
@RestController
@RequestMapping("/api/water-plans")
@CrossOrigin(origins = ["*"])
class WaterPlanController(
    private val waterPlanService: WaterPlanService
) {

    /**
     * 获取所有用水计划（分页）
     */
    @GetMapping
    fun getAllWaterPlans(pageable: Pageable): ResponseEntity<Page<WaterPlan>> {
        val waterPlans = waterPlanService.getAllWaterPlans(pageable)
        return ResponseEntity.ok(waterPlans)
    }

    /**
     * 根据ID获取用水计划
     */
    @GetMapping("/{id}")
    fun getWaterPlanById(@PathVariable id: String): ResponseEntity<WaterPlan> {
        val waterPlan = waterPlanService.getWaterPlanById(id)
        return if (waterPlan != null) {
            ResponseEntity.ok(waterPlan)
        } else {
            ResponseEntity.notFound().build()
        }
    }

    /**
     * 创建用水计划
     */
    @PostMapping
    fun createWaterPlan(@RequestBody waterPlan: WaterPlan): ResponseEntity<WaterPlan> {
        return try {
            val createdWaterPlan = waterPlanService.createWaterPlan(waterPlan)
            ResponseEntity.status(HttpStatus.CREATED).body(createdWaterPlan)
        } catch (e: Exception) {
            ResponseEntity.badRequest().build()
        }
    }

    /**
     * 更新用水计划
     */
    @PutMapping("/{id}")
    fun updateWaterPlan(
        @PathVariable id: String,
        @RequestBody waterPlan: WaterPlan
    ): ResponseEntity<WaterPlan> {
        val updatedWaterPlan = waterPlanService.updateWaterPlan(id, waterPlan)
        return if (updatedWaterPlan != null) {
            ResponseEntity.ok(updatedWaterPlan)
        } else {
            ResponseEntity.notFound().build()
        }
    }

    /**
     * 删除用水计划
     */
    @DeleteMapping("/{id}")
    fun deleteWaterPlan(@PathVariable id: String): ResponseEntity<Void> {
        val deleted = waterPlanService.deleteWaterPlan(id)
        return if (deleted) {
            ResponseEntity.noContent().build()
        } else {
            ResponseEntity.notFound().build()
        }
    }

    /**
     * 根据行程ID获取用水计划
     */
    @GetMapping("/trip/{tripId}")
    fun getWaterPlansByTrip(
        @PathVariable tripId: String,
        pageable: Pageable
    ): ResponseEntity<Page<WaterPlan>> {
        val waterPlans = waterPlanService.getWaterPlansByTripId(tripId, pageable)
        return ResponseEntity.ok(waterPlans)
    }

    /**
     * 根据创建者获取用水计划
     */
    @GetMapping("/creator/{creatorId}")
    fun getWaterPlansByCreator(
        @PathVariable creatorId: String,
        pageable: Pageable
    ): ResponseEntity<Page<WaterPlan>> {
        val waterPlans = waterPlanService.getWaterPlansByCreator(creatorId, pageable)
        return ResponseEntity.ok(waterPlans)
    }

    /**
     * 搜索用水计划
     */
    @GetMapping("/search")
    fun searchWaterPlans(
        @RequestParam(required = false) keyword: String?,
        @RequestParam(required = false) tripId: String?,
        @RequestParam(required = false) creatorId: String?,
        pageable: Pageable
    ): ResponseEntity<Page<WaterPlan>> {
        val waterPlans = waterPlanService.searchWaterPlans(keyword, tripId, creatorId, pageable)
        return ResponseEntity.ok(waterPlans)
    }

    /**
     * 获取用水计划统计信息
     */
    @GetMapping("/statistics")
    fun getWaterPlanStatistics(): ResponseEntity<Map<String, Any>> {
        val statistics = waterPlanService.getWaterPlanStatistics()
        return ResponseEntity.ok(statistics)
    }

    /**
     * 获取最新用水计划
     */
    @GetMapping("/latest")
    fun getLatestWaterPlans(): ResponseEntity<List<WaterPlan>> {
        val waterPlans = waterPlanService.getLatestWaterPlans()
        return ResponseEntity.ok(waterPlans)
    }

    /**
     * 根据名称搜索用水计划
     */
    @GetMapping("/search-by-name")
    fun searchByName(@RequestParam name: String): ResponseEntity<List<WaterPlan>> {
        val waterPlans = waterPlanService.searchByName(name)
        return ResponseEntity.ok(waterPlans)
    }

    /**
     * 获取用水计划详情（包含用水天数）
     */
    @GetMapping("/{id}/with-days")
    fun getWaterPlanWithDays(@PathVariable id: String): ResponseEntity<WaterPlan> {
        val waterPlan = waterPlanService.getWaterPlanWithDays(id)
        return if (waterPlan != null) {
            ResponseEntity.ok(waterPlan)
        } else {
            ResponseEntity.notFound().build()
        }
    }

    /**
     * 获取用水计划的水源统计
     */
    @GetMapping("/{id}/water-source-stats")
    fun getWaterSourceStats(@PathVariable id: String): ResponseEntity<Map<String, Any?>> {
        val sourceCount = waterPlanService.getWaterSourceCount(id)
        val totalVolume = waterPlanService.getTotalEstimatedVolume(id)
        val stats = mapOf(
            "sourceCount" to sourceCount,
            "totalEstimatedVolume" to totalVolume
        )
        return ResponseEntity.ok(stats)
    }
}