package org.example.trip.controller

import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.Parameter
import io.swagger.v3.oas.annotations.tags.Tag
import org.example.common.dto.ApiResponse
import org.example.common.exception.BusinessException
import org.example.common.util.ResponseUtil
import org.example.trip.service.TripService
import org.example.trip.model.Trip
import org.example.trip.dto.TripBasicResponse
import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import org.springframework.http.ResponseEntity
import org.springframework.validation.annotation.Validated
import org.springframework.web.bind.annotation.*
import jakarta.validation.Valid

/**
 * 行程控制器
 */
@RestController
@RequestMapping("/api/v1/trips")
@Tag(name = "行程管理", description = "行程相关的API接口")
@Validated
class TripController(
    private val tripService: TripService
) {

    /**
     * 获取所有行程（分页）
     */
    @GetMapping
    @Operation(summary = "分页查询行程列表", description = "获取行程列表，支持分页")
    fun getAllTrips(
        pageable: Pageable
    ): ResponseEntity<ApiResponse<Page<Trip>>> {
        val trips = tripService.getAllTrips(pageable)
        return ResponseUtil.successPage(trips)
    }

    /**
     * 根据ID获取行程
     */
    @GetMapping("/{id}")
    @Operation(summary = "查询行程详情", description = "根据行程ID获取详细信息")
    fun getTripById(
        @Parameter(description = "行程ID") @PathVariable id: String
    ): ResponseEntity<ApiResponse<Trip>> {
        val trip = tripService.getTripById(id)
            ?: throw BusinessException.notFound("行程不存在")
        return ResponseUtil.success(trip)
    }

    /**
     * 创建行程
     */
    @PostMapping
    @Operation(summary = "创建行程", description = "创建新的行程")
    fun createTrip(
        @Valid @RequestBody trip: Trip
    ): ResponseEntity<ApiResponse<Trip>> {
        val createdTrip = tripService.createTrip(trip)
        return ResponseUtil.created(createdTrip, "创建成功")
    }

    /**
     * 更新行程
     */
    @PutMapping("/{id}")
    @Operation(summary = "更新行程", description = "更新指定ID的行程信息")
    fun updateTrip(
        @Parameter(description = "行程ID") @PathVariable id: String,
        @Valid @RequestBody trip: Trip
    ): ResponseEntity<ApiResponse<Trip>> {
        val updatedTrip = tripService.updateTrip(id, trip)
            ?: throw BusinessException.notFound("行程不存在")
        return ResponseUtil.success(updatedTrip, "更新成功")
    }

    /**
     * 删除行程
     */
    @DeleteMapping("/{id}")
    @Operation(summary = "删除行程", description = "删除指定ID的行程")
    fun deleteTrip(
        @Parameter(description = "行程ID") @PathVariable id: String
    ): ResponseEntity<ApiResponse<Nothing>> {
        val deleted = tripService.deleteTrip(id)
        if (!deleted) {
            throw BusinessException.notFound("行程不存在")
        }
        return ResponseUtil.noContent("删除成功")
    }

    /**
     * 搜索行程
     */
    @GetMapping("/search")
    @Operation(summary = "搜索行程", description = "根据关键词、状态、组织者等条件搜索行程")
    fun searchTrips(
        @Parameter(description = "关键词") @RequestParam(required = false) keyword: String?,
        @Parameter(description = "状态") @RequestParam(required = false) status: Int?,
        @Parameter(description = "组织者ID") @RequestParam(required = false) organizerId: String?,
        pageable: Pageable
    ): ResponseEntity<ApiResponse<Page<Trip>>> {
        val trips = tripService.searchTrips(keyword, status, organizerId, pageable)
        return ResponseUtil.successPage(trips)
    }

    /**
     * 获取用户的行程
     */
    @GetMapping("/user/{userId}")
    @Operation(summary = "获取用户的行程", description = "获取指定用户创建的所有行程")
    fun getUserTrips(
        @Parameter(description = "用户ID") @PathVariable userId: String,
        pageable: Pageable
    ): ResponseEntity<ApiResponse<Page<Trip>>> {
        val trips = tripService.getUserTrips(userId, pageable)
        return ResponseUtil.successPage(trips)
    }

    /**
     * 获取即将开始的行程
     */
    @GetMapping("/upcoming")
    @Operation(summary = "获取即将开始的行程", description = "获取即将开始的行程列表")
    fun getUpcomingTrips(
        pageable: Pageable
    ): ResponseEntity<ApiResponse<Page<Trip>>> {
        val trips = tripService.getUpcomingTrips(pageable)
        return ResponseUtil.successPage(trips)
    }

    /**
     * 获取热门行程
     */
    @GetMapping("/popular")
    @Operation(summary = "获取热门行程", description = "获取热门行程列表")
    fun getPopularTrips(): ResponseEntity<ApiResponse<List<Trip>>> {
        val trips = tripService.getPopularTrips()
        return ResponseUtil.success(trips)
    }

    /**
     * 获取最近创建的行程
     */
    @GetMapping("/recent")
    @Operation(summary = "获取最近创建的行程", description = "获取最近创建的行程列表")
    fun getRecentTrips(): ResponseEntity<ApiResponse<List<Trip>>> {
        val trips = tripService.getRecentTrips()
        return ResponseUtil.success(trips)
    }

    /**
     * 获取正在进行的行程
     */
    @GetMapping("/ongoing")
    @Operation(summary = "获取正在进行的行程", description = "获取正在进行中的行程列表")
    fun getOngoingTrips(
        pageable: Pageable
    ): ResponseEntity<ApiResponse<Page<Trip>>> {
        val trips = tripService.getOngoingTrips(pageable)
        return ResponseUtil.successPage(trips)
    }

    /**
     * 获取已完成的行程
     */
    @GetMapping("/completed")
    @Operation(summary = "获取已完成的行程", description = "获取已完成的行程列表")
    fun getCompletedTrips(
        pageable: Pageable
    ): ResponseEntity<ApiResponse<Page<Trip>>> {
        val trips = tripService.getCompletedTrips(pageable)
        return ResponseUtil.successPage(trips)
    }

    /**
     * 获取行程统计信息
     */
    @GetMapping("/statistics")
    @Operation(summary = "获取行程统计信息", description = "获取行程的统计数据")
    fun getTripStatistics(): ResponseEntity<ApiResponse<Map<String, Any>>> {
        val statistics = tripService.getTripStatistics()
        return ResponseUtil.success(statistics)
    }

    /**
     * 获取用户参与的行程
     */
    @GetMapping("/participant/{userId}")
    @Operation(summary = "获取用户参与的行程", description = "获取指定用户参与的所有行程")
    fun getTripsByParticipant(
        @Parameter(description = "用户ID") @PathVariable userId: String,
        pageable: Pageable
    ): ResponseEntity<ApiResponse<Page<Trip>>> {
        val trips = tripService.getTripsByParticipant(userId, pageable)
        return ResponseUtil.successPage(trips)
    }

    /**
     * 更新行程状态
     */
    @PatchMapping("/{id}/status")
    @Operation(summary = "更新行程状态", description = "更新指定行程的状态")
    fun updateTripStatus(
        @Parameter(description = "行程ID") @PathVariable id: String,
        @Parameter(description = "状态：0-规划中，1-进行中，2-已完成，3-已取消") @RequestParam status: Int
    ): ResponseEntity<ApiResponse<Trip>> {
        val trip = tripService.updateTripStatus(id, status)
            ?: throw BusinessException.notFound("行程不存在")
        return ResponseUtil.success(trip, "状态更新成功")
    }

    /**
     * 根据组织者获取行程
     */
    @GetMapping("/organizer/{organizerId}")
    @Operation(summary = "根据组织者获取行程", description = "获取指定组织者的所有行程")
    fun getTripsByOrganizer(
        @Parameter(description = "组织者ID") @PathVariable organizerId: String,
        pageable: Pageable
    ): ResponseEntity<ApiResponse<Page<Trip>>> {
        val trips = tripService.getTripsByOrganizer(organizerId, pageable)
        return ResponseUtil.successPage(trips)
    }

    /**
     * 根据状态获取行程
     */
    @GetMapping("/status/{status}")
    @Operation(summary = "根据状态获取行程", description = "获取指定状态的所有行程")
    fun getTripsByStatus(
        @Parameter(description = "状态：0-规划中，1-进行中，2-已完成，3-已取消") @PathVariable status: Int,
        pageable: Pageable
    ): ResponseEntity<ApiResponse<Page<Trip>>> {
        val trips = tripService.getTripsByStatus(status, pageable)
        return ResponseUtil.successPage(trips)
    }

    /**
     * 获取计划中的行程
     */
    @GetMapping("/planned")
    @Operation(summary = "获取计划中的行程", description = "获取所有状态为规划中（0）的行程列表")
    fun getPlannedTrips(
        pageable: Pageable
    ): ResponseEntity<ApiResponse<Page<TripBasicResponse>>> {
        val trips = tripService.getPlannedTrips(pageable)
        val response = trips.map { TripBasicResponse.fromTrip(it) }
        return ResponseUtil.successPage(response)
    }
}