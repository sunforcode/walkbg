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
                    distance = route.mapData?.distance,
                    duration = route.mapData?.duration,
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
                    route.isFavoriteByUser = routeService.isFavorite(id, userId)
                }

                val response = RouteWithDetailsDto(
                    id = route.id,
                    name = route.name,
                    description = route.description,
                    region = route.region,
                    regionId = route.regionId,
                    difficulty = route.difficulty,
                    routeType = route.routeType,
                    // 从关联的 mapData 获取地理信息
                    distance = route.mapData?.distance,
                    duration = route.mapData?.duration,
                    latitude = route.mapData?.latitude,
                    longitude = route.mapData?.longitude,
                    altitude = route.mapData?.altitude,
                    elevationGain = route.mapData?.elevationGain,
                    elevationLoss = route.mapData?.elevationLoss,
                    status = route.status,
                    coverUrl = route.coverUrl,
                    defaultMapId = route.defaultMapId,
                    popularity = route.popularity,
                    usageCount = route.usageCount,
                    isLoop = route.isLoop,
                    imageUrls = route.imageUrls?.split(",") ?: emptyList(),
                    isFavorite = route.isFavorite,
                    createdAt = route.createdAt,
                    updatedAt = route.updatedAt,
                    // 统计数据从 mapData 获取
                    favoriteCount = route.mapData?.favoriteCount ?: 0,
                    completionCount = route.mapData?.completionCount ?: 0,
                    tripCount = route.mapData?.tripCount ?: 0,
                    // 创建者信息
                    creator = route.creator?.let { user ->
                        org.example.user.dto.UserBasicDto(
                            id = user.id,
                            username = user.username,
                            nickname = user.nickname,
                            email = user.email,
                            avatarUrl = user.avatarUrl,
                            createdAt = user.createdAt
                        )
                    },
                    // 关联对象数据
                    tags = route.tags.map { it.tag },
                    campsites = route.campsites.map { campsite ->
                        org.example.route.dto.CampsiteDto(
                            id = campsite.id,
                            name = campsite.name,
                            description = campsite.description,
                            latitude = campsite.latitude,
                            longitude = campsite.longitude,
                            elevation = campsite.elevation,
                            campsiteType = campsite.campsiteType,
                            notes = campsite.notes,
                            createdAt = campsite.createdAt,
                            updatedAt = campsite.updatedAt,
                            verifiedBy = campsite.verifiedBy?.let { user ->
                                org.example.user.dto.UserBasicDto(
                                    id = user.id,
                                    username = user.username,
                                    nickname = user.nickname,
                                    email = user.email,
                                    avatarUrl = user.avatarUrl,
                                    createdAt = user.createdAt
                                )
                            }
                        )
                    },
                    markerPoints = route.markerPoints.map { marker ->
                        org.example.route.dto.MarkerPointDto(
                            id = marker.id,
                            name = marker.name,
                            description = marker.description,
                            markerType = marker.markerType,
                            iconUrl = marker.iconUrl,
                            latitude = marker.latitude,
                            longitude = marker.longitude,
                            createdAt = marker.createdAt,
                            updatedAt = marker.updatedAt,
                            color = marker.color,
                            elevation = marker.elevation
                        )
                    },
                    supplies = route.supplies.map { supply ->
                        org.example.route.dto.SupplyDto(
                            id = supply.id,
                            name = supply.name,
                            description = supply.description,
                            routeId = supply.route?.id,
                            latitude = supply.latitude,
                            longitude = supply.longitude,
                            elevation = supply.elevation,
                            supplyType = supply.supplyType,
                            lastVerified = supply.lastVerified,
                            lastVerifiedAt = supply.lastVerifiedAt,
                            updatedBy = supply.updatedByUser?.let { user ->
                                org.example.user.dto.UserBasicDto(
                                    id = user.id,
                                    username = user.username,
                                    nickname = user.nickname,
                                    email = user.email,
                                    avatarUrl = user.avatarUrl,
                                    createdAt = user.createdAt
                                )
                            },
                            createdAt = supply.createdAt,
                            updatedAt = supply.updatedAt
                        )
                    },
                    waterSources = route.waterSources.map { water ->
                        org.example.water.dto.WaterSourceDto(
                            id = water.id,
                            name = water.name,
                            description = water.description,
                            latitude = water.latitude,
                            longitude = water.longitude,
                            elevation = water.elevation,
                            waterType = water.waterType,
                            waterQuality = water.waterQuality,
                            requiresTreatment = water.requiresTreatment,
                            reliability = water.reliability,
                            notes = water.notes,
                            lastVerified = water.lastVerified,
                            verifiedBy = water.verifiedBy?.let { user ->
                                org.example.user.dto.UserBasicDto(
                                    id = user.id,
                                    username = user.username,
                                    nickname = user.nickname,
                                    email = user.email,
                                    avatarUrl = user.avatarUrl,
                                    createdAt = user.createdAt
                                )
                            },
                            createdAt = water.createdAt,
                            updatedAt = water.updatedAt
                        )
                    },
                    dailyPlans = route.dailyPlans.map { plan ->
                        org.example.route.dto.DailyPlanDto(
                            id = plan.id,
                            dayNumber = plan.dayNumber,
                            title = plan.title,
                            description = plan.description,
                            distance = plan.distance,
                            estimatedTime = plan.estimatedTime,
                            elevationGain = plan.elevationGain,
                            elevationLoss = plan.elevationLoss,
                            maxElevation = plan.maxElevation,
                            minElevation = plan.minElevation,
                            accommodation = plan.accommodation,
                            notes = plan.notes,
                            createdAt = plan.createdAt,
                            updatedAt = plan.updatedAt,
                            segments = plan.segments.map { planSegment ->
                                org.example.route.dto.DailyPlanSegmentDto(
                                    id = planSegment.id,
                                    sequenceNumber = planSegment.sequenceNumber,
                                    segment = planSegment.segment?.let { segment ->
                                        org.example.route.dto.SegmentDto(
                                            id = segment.id,
                                            name = segment.name,
                                            description = segment.description,
                                            distance = segment.distance,
                                            elevationGain = segment.elevationGain,
                                            elevationLoss = segment.elevationLoss,
                                            estimatedTime = segment.estimatedTime,
                                            difficulty = segment.difficulty,
                                            routeType = segment.routeType,
                                            notes = segment.notes,
                                            startPoint = null, // 简化处理
                                            endPoint = null,   // 简化处理
                                            keypoints = emptyList() // 简化处理
                                        )
                                    }
                                )
                            }
                        )
                    },
                    weatherInfo = route.weatherInfo?.let { weather ->
                        org.example.route.dto.RouteWeatherDto(
                            id = weather.id,
                            description = weather.description,
                            precautions = weather.precautions,
                            seasonalWeather = weather.seasonalWeather.map { seasonal ->
                                org.example.route.dto.SeasonalWeatherDto(
                                    id = seasonal.id,
                                    season = seasonal.season,
                                    description = seasonal.description
                                )
                            }
                        )
                    }
                )

                ResponseUtil.success(response)
            }
        } catch (e: Exception) {
            ResponseUtil.error("查询路线详情失败: ${e.message}")
        }
    }
}