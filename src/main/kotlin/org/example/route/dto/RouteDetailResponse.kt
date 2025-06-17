package org.example.route.dto

import com.fasterxml.jackson.annotation.JsonProperty
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
    val createdAt: Long, // 改为时间戳（秒）
    @JsonProperty("updated_at")
    val updatedAt: Long, // 改为时间戳（秒）
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
        fun fromRoute(route: org.example.route.model.Route, isFavorite: Boolean = false): RouteDetailResponse {
            return RouteDetailResponse(
                id = route.id,
                name = route.name,
                description = route.description,
                regionId = route.regionId,
                region = route.region,
                distance = route.mapData?.distance?.toDouble(),
                duration = route.mapData?.duration,
                elevationGain = route.mapData?.elevationGain?.toDouble(),
                elevationLoss = route.mapData?.elevationLoss?.toDouble(),
                difficulty = route.difficulty,
                routeType = route.routeType,
                routeDirection = null, // 暂时设为null，后续可以根据实际需求调整
                coverUrl = route.coverUrl,
                defaultMapId = route.defaultMapId,
                popularity = route.popularity,
                usageCount = route.usageCount,
                isLoop = route.isLoop,
                isFavorite = isFavorite,
                status = route.status,
                createdAt = route.createdAt.epochSecond, // 转换为时间戳
                updatedAt = route.updatedAt.epochSecond, // 转换为时间戳
                createdBy = route.createdBy,
                creator = route.creator?.let { user ->
                    UserBasicDto(
                        id = user.id,
                        username = user.username,
                        nickname = user.nickname,
                        email = user.email,
                        avatarUrl = user.avatarUrl,
                        createdAt = user.createdAt.epochSecond // 转换为时间戳
                    )
                },

                // 关联对象映射
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
