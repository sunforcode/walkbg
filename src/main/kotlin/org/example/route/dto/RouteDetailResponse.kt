package org.example.route.dto

import com.fasterxml.jackson.annotation.JsonProperty
import java.time.Instant
import org.example.user.dto.UserBasicDto
import org.example.water.dto.WaterSourceDto

/**
 * 路线详细信息响应DTO
 * 包含所有关联对象信息
 */
data class RouteDetailResponse(
    val id: String,
    val name: String,
    val description: String?,
    @JsonProperty("region_id")
    val regionId: String?,
    val region: String?,
    val distance: Double?,
    val duration: Int?,
    @JsonProperty("elevation_gain")
    val elevationGain: Double?,
    @JsonProperty("elevation_loss")
    val elevationLoss: Double?,
    val difficulty: Int?,
    @JsonProperty("route_type")
    val routeType: Int?,
    @JsonProperty("route_direction")
    val routeDirection: Int?,
    @JsonProperty("cover_url")
    val coverUrl: String?,
    @JsonProperty("default_map_id")
    val defaultMapId: String?,
    val popularity: Int,
    @JsonProperty("usage_count")
    val usageCount: Int,
    @JsonProperty("is_loop")
    val isLoop: Boolean,
    @JsonProperty("is_favorite")
    val isFavorite: Boolean,
    val status: Int,
    @JsonProperty("created_at")
    val createdAt: Instant,
    @JsonProperty("updated_at")
    val updatedAt: Instant,
    @JsonProperty("created_by")
    val createdBy: String?,
    val creator: UserBasicDto?,

    // 关联对象信息
    val segments: List<SegmentDto> = emptyList(),
    @JsonProperty("daily_plans")
    val dailyPlans: List<DailyPlanDto> = emptyList(),
    val tags: List<String> = emptyList(),
    @JsonProperty("image_urls")
    val imageUrls: List<String> = emptyList(),
    val ratings: RatingDto? = null,
    @JsonProperty("weather_info")
    val weatherInfo: WeatherInfoDto? = null,
    @JsonProperty("water_sources")
    val waterSources: List<WaterSourceDto> = emptyList(),
    val campsites: List<CampsiteDto> = emptyList(),
    val supplies: List<SupplyDto> = emptyList(),
    @JsonProperty("hitchhike_contacts")
    val hitchhikeContacts: List<HitchhikeContactDto> = emptyList(),
    @JsonProperty("marker_points")
    val markerPoints: List<MarkerPointDto> = emptyList()
) {
    companion object {
        /**
         * 从Route实体创建详细响应DTO
         */
        fun fromRoute(route: org.example.route.model.Route, isFavorited: Boolean = false): RouteDetailResponse {
            return RouteDetailResponse(
                // 基础信息
                id = route.id,
                name = route.name,
                description = route.description,
                region = route.region,
                regionId = route.regionId,
                difficulty = route.difficulty,
                routeType = route.routeType,
                routeDirection = 0, // 默认值，需要根据实际业务调整
                status = route.status,

                // 地理信息（来自mapData）
                distance = route.mapData?.distance?.toDouble(),
                duration = route.mapData?.duration,
                elevationGain = route.mapData?.elevationGain?.toDouble(),
                elevationLoss = route.mapData?.elevationLoss?.toDouble(),

                // 媒体信息
                coverUrl = route.coverUrl,
                defaultMapId = route.defaultMapId,
                imageUrls = route.images.map { it.imageUrl },

                // 统计信息
                popularity = route.popularity,
                usageCount = route.usageCount,
                isLoop = route.isLoop,
                isFavorite = isFavorited,

                // 时间信息
                createdAt = route.createdAt,
                updatedAt = route.updatedAt,
                createdBy = route.createdBy,

                // 创建者信息
                creator = route.creator?.let { user ->
                    UserBasicDto(
                        id = user.id,
                        username = user.username,
                        nickname = user.nickname,
                        email = user.email,
                        avatarUrl = user.avatarUrl,
                        createdAt = user.createdAt
                    )
                },

                // 关联对象信息 - 使用各DTO的工厂方法
                tags = route.tags.map { it.tag },
                segments = route.segments.map { SegmentDto.fromSegment(it) },
                campsites = route.campsites.map { CampsiteDto.fromCampsite(it) },
                supplies = route.supplies.map { SupplyDto.fromSupply(it) },
                waterSources = route.waterSources.map { WaterSourceDto.fromWaterSource(it) },
                markerPoints = route.markerPoints.map { MarkerPointDto.fromMarkerPoint(it) },
                dailyPlans = route.dailyPlans.map { DailyPlanDto.fromDailyPlan(it) },

                // 暂时使用默认值，后续可以根据实际需求调整
                ratings = null,
                weatherInfo = null,
                hitchhikeContacts = route.hitchhikeContacts.map { HitchhikeContactDto.fromHitchhikeContact(it) }
            )
        }
    }
}
