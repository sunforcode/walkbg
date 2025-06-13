package org.example.route.controller

import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.Parameter
import io.swagger.v3.oas.annotations.tags.Tag
import org.example.common.dto.ApiResponse
import org.example.common.dto.BaseQueryRequest
import org.example.common.exception.BusinessException
import org.example.common.util.ResponseUtil
import org.example.route.service.RouteService
import org.example.route.dto.RouteWithDetailsDto

import org.example.route.dto.RouteBasicResponse
import org.example.route.dto.RouteCreateRequest
import org.springframework.data.domain.Page
import org.springframework.http.ResponseEntity
import org.springframework.validation.annotation.Validated
import org.springframework.web.bind.annotation.*
import jakarta.validation.Valid

/**
 * 路线控制器
 */
@RestController
@RequestMapping("/api/routes")
@Tag(name = "路线管理", description = "路线相关的API接口")
@Validated
class RouteController(
    private val routeService: RouteService
) {

    /**
     * 分页查询路线列表
     */
    @GetMapping
    @Operation(summary = "分页查询路线列表", description = "获取路线列表，支持分页")
    fun getRoutes(
        @Parameter(description = "页码，从0开始") @RequestParam(defaultValue = "0") page: Int,
        @Parameter(description = "每页大小") @RequestParam(defaultValue = "10") size: Int,
        @Parameter(description = "关键词搜索") @RequestParam(required = false) keyword: String?,
        @Parameter(description = "区域ID") @RequestParam(required = false) regionId: String?,
        @Parameter(description = "难度等级") @RequestParam(required = false) difficulty: Int?,
        @Parameter(description = "路线类型") @RequestParam(required = false) routeType: Int?,
        @Parameter(description = "最小距离") @RequestParam(required = false) minDistance: Double?,
        @Parameter(description = "最大距离") @RequestParam(required = false) maxDistance: Double?,
        @Parameter(description = "用户ID") @RequestParam(required = false) userId: String?
    ): ResponseEntity<ApiResponse<Page<RouteBasicResponse>>> {
        return try {
            val pageable = org.springframework.data.domain.PageRequest.of(page, size)
            val routes = routeService.findByCondition(
                keyword, regionId, difficulty, routeType,
                minDistance, maxDistance, null, userId, pageable
            )

            val response = routes.map { route ->
                RouteBasicResponse(
                    id = route.id,
                    name = route.name,
                    description = route.description,
                    region = route.region,
                    distance = route.distance,
                    duration = route.duration,
                    difficulty = route.difficulty,
                    coverUrl = route.coverUrl,
                    popularity = route.popularity,
                    createdAt = route.createdAt,
                    createdBy = route.createdBy
                )
            }

            ResponseUtil.success(response)
        } catch (e: Exception) {
            ResponseUtil.error("查询路线列表失败: ${e.message}")
        }
    }

    /**
     * 根据ID查询路线详情
     */
    @GetMapping("/{id}")
    @Operation(summary = "查询路线详情", description = "根据路线ID获取详细信息")
    fun getRouteById(
        @Parameter(description = "路线ID") @PathVariable id: String,
        @Parameter(description = "用户ID") @RequestParam(required = false) userId: String?
    ): ResponseEntity<ApiResponse<RouteWithDetailsDto?>> {
        return try {
            val route = routeService.findById(id)
            if (route == null) {
                ResponseUtil.error("路线不存在")
            } else {
                // 如果提供了用户ID，检查是否收藏
                if (userId != null) {
                    route.isFavorite = routeService.isFavorite(id, userId)
                }

                val response = RouteWithDetailsDto(
                    id = route.id,
                    name = route.name,
                    description = route.description,
                    region = route.region,
                    regionId = route.regionId,
                    distance = route.distance,
                    duration = route.duration,
                    latitude = null,
                    longitude = null,
                    altitude = null,
                    elevationGain = route.elevationGain,
                    elevationLoss = route.elevationLoss,
                    difficulty = route.difficulty,
                    routeType = route.routeType,
                    routeDirection = route.routeDirection,
                    status = route.status,
                    coverUrl = route.coverUrl,
                    mapDataId = null,
                    defaultMapId = route.defaultMapId,
                    createdBy = route.createdBy,
                    popularity = route.popularity,
                    createdAt = route.createdAt,
                    updatedAt = route.updatedAt
                )

                ResponseUtil.success(response)
            }
        } catch (e: Exception) {
            ResponseUtil.error("查询路线详情失败: ${e.message}")
        }
    }
}