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
    @JsonProperty("kml_url")
    val kmlUrl: String?,
    @JsonProperty("gpx_url")
    val gpxUrl: String?,
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
    val markerPoints: List<MarkerPointDto> = emptyList(),
    @JsonProperty("track_points")
    val trackPoints: List<TrackPointDto> = emptyList()
) {
    companion object {
        /**
         * 从 Route实体创建详细响应 DTO
         * 
         * @deprecated 使用 RouteApplicationService.enrichRouteDetail() 替代，它会从数据库填充所有嵌套集合。
         *            fromRoute() 你仅为了向后兑改而保留，新代码不应使用此方法。
         */
        @Deprecated(
            message = "使用 RouteApplicationService.enrichRouteDetail() 替代",
            replaceWith = ReplaceWith("enrichRouteDetail(route, null)"),
            level = DeprecationLevel.WARNING
        )
        fun fromRoute(route: org.example.route.model.Route, isFavorite: Boolean = false): RouteDetailResponse {
            return RouteDetailResponse(
                id = route.id,
                name = route.name,
                description = route.description,
                regionId = route.regionId,
                region = route.region,
                distance = null,  // 需要通过 Repository 查询 MapData
                duration = null,  // 需要通过 Repository 查询 MapData
                elevationGain = null,  // 需要通过 Repository 查询 MapData
                elevationLoss = null,  // 需要通过 Repository 查询 MapData
                difficulty = route.difficulty,
                routeType = route.routeType,
                routeDirection = null, // 暂时设为null，后续可以根据实际需求调整
                coverUrl = route.coverUrl,
                defaultMapId = route.defaultMapId,
                kmlUrl = null,  // 需要通过 RouteMapDataRepository 查询
                gpxUrl = null,  // 需要通过 RouteMapDataRepository 查询
                popularity = route.popularity,
                usageCount = route.usageCount,
                isLoop = route.isLoop,
                isFavorite = isFavorite,
                status = route.status,
                createdAt = route.createdAt.epochSecond, // 转换为时间戳
                updatedAt = route.updatedAt.epochSecond, // 转换为时间戳
                createdBy = route.createdBy,
                creator = null,  // 需要通过 Repository 查询 User

                // 关联对象映射 - 需要通过 Repository 按需查询
                tags = emptyList(),  // 需要通过 RouteTagRepository 查询
                segments = emptyList(),  // 需要通过 SegmentRepository 查询
                campsites = emptyList(),  // 需要通过 CampsiteRepository 查询
                supplies = emptyList(),  // 需要通过 SupplyRepository 查询
                waterSources = emptyList(),  // 需要通过 WaterSourceRepository 查询
                markerPoints = emptyList(),  // 需要通过 MarkerPointRepository 查询
                dailyPlans = emptyList(),  // 需要通过 DailyPlanRepository 查询

                // 暂时使用默认值，后续可以根据实际需求调整
                ratings = null,
                weatherInfo = null,
                hitchhikeContacts = emptyList(),  // 需要通过 HitchhikeContactRepository 查询
                trackPoints = emptyList()  // 需要通过 WaypointRepository 查询
            )
        }
    }
}
