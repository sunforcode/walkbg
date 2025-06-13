package org.example.equipment.controller

import org.example.meal.model.MealPlan
import org.example.meal.service.MealPlanService
import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.*

/**
 * 餐食计划控制器
 */
@RestController
@RequestMapping("/api/meal-plans")
@CrossOrigin(origins = ["*"])
class MealPlanController(
    private val mealPlanService: MealPlanService
) {

    /**
     * 获取所有餐食计划（分页）
     */
    @GetMapping
    fun getAllMealPlans(pageable: Pageable): ResponseEntity<Page<MealPlan>> {
        val mealPlans = mealPlanService.getAllMealPlans(pageable)
        return ResponseEntity.ok(mealPlans)
    }

    /**
     * 根据ID获取餐食计划
     */
    @GetMapping("/{id}")
    fun getMealPlanById(@PathVariable id: String): ResponseEntity<MealPlan> {
        val mealPlan = mealPlanService.getMealPlanById(id)
        return if (mealPlan != null) {
            ResponseEntity.ok(mealPlan)
        } else {
            ResponseEntity.notFound().build()
        }
    }

    /**
     * 创建餐食计划
     */
    @PostMapping
    fun createMealPlan(@RequestBody mealPlan: MealPlan): ResponseEntity<MealPlan> {
        return try {
            val createdMealPlan = mealPlanService.createMealPlan(mealPlan)
            ResponseEntity.status(HttpStatus.CREATED).body(createdMealPlan)
        } catch (e: Exception) {
            ResponseEntity.badRequest().build()
        }
    }

    /**
     * 更新餐食计划
     */
    @PutMapping("/{id}")
    fun updateMealPlan(
        @PathVariable id: String,
        @RequestBody mealPlan: MealPlan
    ): ResponseEntity<MealPlan> {
        val updatedMealPlan = mealPlanService.updateMealPlan(id, mealPlan)
        return if (updatedMealPlan != null) {
            ResponseEntity.ok(updatedMealPlan)
        } else {
            ResponseEntity.notFound().build()
        }
    }

    /**
     * 删除餐食计划
     */
    @DeleteMapping("/{id}")
    fun deleteMealPlan(@PathVariable id: String): ResponseEntity<Void> {
        val deleted = mealPlanService.deleteMealPlan(id)
        return if (deleted) {
            ResponseEntity.noContent().build()
        } else {
            ResponseEntity.notFound().build()
        }
    }

    /**
     * 根据行程ID获取餐食计划
     */
    @GetMapping("/trip/{tripId}")
    fun getMealPlansByTrip(
        @PathVariable tripId: String,
        pageable: Pageable
    ): ResponseEntity<Page<MealPlan>> {
        val mealPlans = mealPlanService.getMealPlansByTripId(tripId, pageable)
        return ResponseEntity.ok(mealPlans)
    }

    /**
     * 根据创建者获取餐食计划
     */
    @GetMapping("/creator/{creatorId}")
    fun getMealPlansByCreator(
        @PathVariable creatorId: String,
        pageable: Pageable
    ): ResponseEntity<Page<MealPlan>> {
        val mealPlans = mealPlanService.getMealPlansByCreator(creatorId, pageable)
        return ResponseEntity.ok(mealPlans)
    }

    /**
     * 搜索餐食计划
     */
    @GetMapping("/search")
    fun searchMealPlans(
        @RequestParam(required = false) keyword: String?,
        @RequestParam(required = false) tripId: String?,
        @RequestParam(required = false) creatorId: String?,
        pageable: Pageable
    ): ResponseEntity<Page<MealPlan>> {
        val mealPlans = mealPlanService.searchMealPlans(keyword, tripId, creatorId, pageable)
        return ResponseEntity.ok(mealPlans)
    }

    /**
     * 获取餐食计划统计信息
     */
    @GetMapping("/statistics")
    fun getMealPlanStatistics(): ResponseEntity<Map<String, Any>> {
        val statistics = mealPlanService.getMealPlanStatistics()
        return ResponseEntity.ok(statistics)
    }

    /**
     * 获取最新餐食计划
     */
    @GetMapping("/latest")
    fun getLatestMealPlans(): ResponseEntity<List<MealPlan>> {
        val mealPlans = mealPlanService.getLatestMealPlans()
        return ResponseEntity.ok(mealPlans)
    }

    /**
     * 根据名称搜索餐食计划
     */
    @GetMapping("/search-by-name")
    fun searchByName(@RequestParam name: String): ResponseEntity<List<MealPlan>> {
        val mealPlans = mealPlanService.searchByName(name)
        return ResponseEntity.ok(mealPlans)
    }

    /**
     * 获取餐食计划详情（包含餐食天数）
     */
    @GetMapping("/{id}/with-days")
    fun getMealPlanWithDays(@PathVariable id: String): ResponseEntity<MealPlan> {
        val mealPlan = mealPlanService.getMealPlanWithDays(id)
        return if (mealPlan != null) {
            ResponseEntity.ok(mealPlan)
        } else {
            ResponseEntity.notFound().build()
        }
    }
}