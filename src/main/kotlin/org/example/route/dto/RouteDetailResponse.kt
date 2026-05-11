package org.example.route.dto

import com.fasterxml.jackson.annotation.JsonProperty
import org.example.user.dto.UserBasicDto

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
    val createdAt: Long,
    @JsonProperty("updated_at")
    val updatedAt: Long,
    @JsonProperty("created_by")
    val createdBy: String?,
    val creator: UserBasicDto?,

    // 关联对象信息
    @JsonProperty("segment_schemes")
    val segmentSchemes: List<SegmentSchemeDto> = emptyList(),
    @JsonProperty("poi_points")
    val poiPoints: List<PoiPointDto> = emptyList(),
    @JsonProperty("daily_plans")
    val dailyPlans: List<DailyPlanDto> = emptyList(),
    val tags: List<String> = emptyList(),
    @JsonProperty("image_urls")
    val imageUrls: List<String> = emptyList(),
    val ratings: RatingDto? = null,
    @JsonProperty("weather_info")
    val weatherInfo: WeatherInfoDto? = null,
    @JsonProperty("hitchhike_contacts")
    val hitchhikeContacts: List<HitchhikeContactDto> = emptyList(),
    @JsonProperty("track_points")
    val trackPoints: List<TrackPointDto> = emptyList()
) {
    companion object {
        /**
         * 从 Route 实体创建详细响应 DTO
         *
         * @deprecated 使用 RouteApplicationService.enrichRouteDetail() 替代，它会从数据库填充所有嵌套集合。
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
                distance = null,
                duration = null,
                elevationGain = null,
                elevationLoss = null,
                difficulty = route.difficulty,
                routeType = route.routeType,
                routeDirection = null,
                coverUrl = route.coverUrl,
                defaultMapId = route.defaultMapId,
                kmlUrl = null,
                gpxUrl = null,
                popularity = route.popularity,
                usageCount = route.usageCount,
                isLoop = route.isLoop,
                isFavorite = isFavorite,
                status = route.status,
                createdAt = route.createdAt.epochSecond,
                updatedAt = route.updatedAt.epochSecond,
                createdBy = route.createdBy,
                creator = null,
                tags = emptyList(),
                segmentSchemes = emptyList(),
                poiPoints = emptyList(),
                dailyPlans = emptyList(),
                ratings = null,
                weatherInfo = null,
                hitchhikeContacts = emptyList(),
                trackPoints = emptyList()
            )
        }
    }
}
