package org.example.trip.controller

import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.Parameter
import io.swagger.v3.oas.annotations.tags.Tag
import org.example.common.dto.ApiResponse
import org.example.common.exception.BusinessException
import org.example.common.util.ResponseUtil
import org.example.common.util.IdGenerator
import org.example.trip.service.TripService
import org.example.trip.model.Trip
import org.example.trip.dto.TripBasicResponse
import org.example.trip.dto.TripCreateRequest
import org.example.trip.dto.TripUpdateRequest
import org.example.equipment.dto.EquipmentListResponse
import org.example.equipment.repository.EquipmentListRepository
import org.example.equipment.service.EquipmentListItemService
import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import org.springframework.http.ResponseEntity
import org.springframework.validation.annotation.Validated
import org.springframework.web.bind.annotation.*
import jakarta.validation.Valid
import java.time.Instant

/**
 * 旧版行程控制器。
 *
 * 稳定的 `/api/v1/trips` 命名空间由个人行程合同使用；旧版接口保留在显式
 * legacy 路径，避免两套不兼容模型注册相同的 Spring MVC 映射。
 */
@RestController
@RequestMapping("/api/v1/legacy/trips")
@Tag(name = "旧版行程管理", description = "旧版行程相关的API接口")
@Validated
class TripController(
    private val tripService: TripService,
    private val equipmentListRepository: EquipmentListRepository,
    private val equipmentListItemService: EquipmentListItemService
) {

    /**
     * 解析行程关联的装备清单ID。
     *
     * 一个行程可能关联多个装备清单（如多名参与者各自的个人清单），
     * 此处按 `created_at` 降序取最新创建的一个作为摘要字段；
     * 需要完整列表的调用方应使用 `GET /trips/{id}/equipment-lists`。
     */
    private fun resolveEquipmentListId(tripId: String): String? {
        return equipmentListRepository.findByTripId(tripId)
            .maxByOrNull { it.createdAt }
            ?.id
    }

    /**
     * 获取所有行程（分页）
     * 支持统一查询参数：keyword, status, organizerId, participantId, scope, sort
     */
    @GetMapping
    @Operation(summary = "分页查询行程列表", description = "获取行程列表，支持分页和统一查询参数")
    fun getAllTrips(
        @Parameter(description = "关键词搜索") @RequestParam(required = false) keyword: String?,
        @Parameter(description = "状态：0-规划中，1-进行中，2-已完成，3-已取消") @RequestParam(required = false) status: Int?,
        @Parameter(description = "组织者ID") @RequestParam(required = false) organizerId: String?,
        @Parameter(description = "参与者ID") @RequestParam(required = false) participantId: String?,
        pageable: Pageable
    ): ResponseEntity<ApiResponse<Page<TripBasicResponse>>> {
        val trips = when {
            keyword != null || status != null || organizerId != null ->
                tripService.searchTrips(keyword, status, organizerId, pageable)
            participantId != null ->
                tripService.getTripsByParticipant(participantId, pageable)
            else ->
                tripService.getAllTrips(pageable)
        }
        // 批量取回关联路线，避免逐条查询造成的 N+1
        val routeIdsByTrip = tripService.getRouteIdsByTripIds(trips.content.map { it.id })
        val response = trips.map { TripBasicResponse.fromTrip(it, routeIds = routeIdsByTrip[it.id]) }
        return ResponseUtil.successPage(response)
    }

    /**
     * 根据ID获取行程
     */
    @GetMapping("/{id}")
    @Operation(summary = "查询行程详情", description = "根据行程ID获取详细信息")
    fun getTripById(
        @Parameter(description = "行程ID") @PathVariable id: String
    ): ResponseEntity<ApiResponse<TripBasicResponse>> {
        val trip = tripService.getTripById(id)
            ?: throw BusinessException.notFound("行程不存在")
        return ResponseUtil.success(
            TripBasicResponse.fromTrip(trip, resolveEquipmentListId(id), tripService.getRouteIds(id))
        )
    }

    /**
     * 创建行程
     */
    @PostMapping
    @Operation(summary = "创建行程", description = "创建新的行程")
    fun createTrip(
        @Valid @RequestBody request: TripCreateRequest
    ): ResponseEntity<ApiResponse<TripBasicResponse>> {
        // 归一化并校验路线集合：至少一条路线，且主路线必为集合成员
        val resolvedRoutes = request.resolveRoutes()

        val trip = Trip(
            id = IdGenerator.generateId(),
            name = request.name,
            description = request.description,
            startDate = request.startDate?.let { Instant.ofEpochSecond(it) },
            endDate = request.endDate?.let { Instant.ofEpochSecond(it) },
            organizerId = request.organizerId ?: "",
            primaryRouteId = resolvedRoutes.primaryRouteId,
            budget = request.budget,
            notes = request.notes,
            privacySetting = request.privacySetting,
            coverUrl = request.coverUrl
        )
        val createdTrip = tripService.createTrip(
            trip,
            resolvedRoutes.routeIds,
            resolvedRoutes.primaryRouteId
        )
        return ResponseUtil.created(
            TripBasicResponse.fromTrip(
                createdTrip,
                resolveEquipmentListId(createdTrip.id),
                tripService.getRouteIds(createdTrip.id)
            ),
            "创建成功"
        )
    }

    /**
     * 更新行程
     */
    @PutMapping("/{id}")
    @Operation(summary = "更新行程", description = "更新指定ID的行程信息")
    fun updateTrip(
        @Parameter(description = "行程ID") @PathVariable id: String,
        @Valid @RequestBody request: TripUpdateRequest
    ): ResponseEntity<ApiResponse<TripBasicResponse>> {
        val existing = tripService.getTripById(id)
            ?: throw BusinessException.notFound("行程不存在")

        // 只更新请求中提供的字段
        request.name?.let { existing.name = it }
        request.description?.let { existing.description = it }
        request.startDate?.let { existing.startDate = Instant.ofEpochSecond(it) }
        request.endDate?.let { existing.endDate = Instant.ofEpochSecond(it) }
        request.primaryRouteId?.let { existing.primaryRouteId = it }
        request.budget?.let { existing.budget = it }
        request.actualCost?.let { existing.actualCost = it }
        request.notes?.let { existing.notes = it }
        request.privacySetting?.let { existing.privacySetting = it }
        request.coverUrl?.let { existing.coverUrl = it }
        existing.updatedAt = Instant.now()

        // 主路线发生变更时需同步关联表，否则权威关联数据会与 Trip 行产生分歧
        val updatedTrip = tripService.updateTrip(id, existing, request.primaryRouteId)
            ?: throw BusinessException.notFound("行程不存在")
        return ResponseUtil.success(
            TripBasicResponse.fromTrip(updatedTrip, resolveEquipmentListId(id), tripService.getRouteIds(id)),
            "更新成功"
        )
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
     * 更新行程状态
     */
    @PatchMapping("/{id}/status")
    @Operation(summary = "更新行程状态", description = "更新指定行程的状态")
    fun updateTripStatus(
        @Parameter(description = "行程ID") @PathVariable id: String,
        @Parameter(description = "状态：0-规划中，1-进行中，2-已完成，3-已取消") @RequestParam status: Int
    ): ResponseEntity<ApiResponse<TripBasicResponse>> {
        val trip = tripService.updateTripStatus(id, status)
            ?: throw BusinessException.notFound("行程不存在")
        return ResponseUtil.success(
            TripBasicResponse.fromTrip(trip, resolveEquipmentListId(id), tripService.getRouteIds(id)),
            "状态更新成功"
        )
    }

    /**
     * 获取行程关联的所有装备清单（分页）
     */
    @GetMapping("/{id}/equipment-lists")
    @Operation(summary = "查询行程关联的装备清单", description = "获取指定行程关联的所有装备清单列表，支持分页")
    fun getTripEquipmentLists(
        @Parameter(description = "行程ID") @PathVariable id: String,
        pageable: Pageable
    ): ResponseEntity<ApiResponse<Page<EquipmentListResponse>>> {
        tripService.getTripById(id)
            ?: throw BusinessException.notFound("行程不存在")
        val lists = equipmentListRepository.findByTripId(id, pageable)
        val response = lists.map { list ->
            EquipmentListResponse.fromEntity(list, equipmentListItemService.countListItems(list.id))
        }
        return ResponseUtil.successPage(response)
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
}
