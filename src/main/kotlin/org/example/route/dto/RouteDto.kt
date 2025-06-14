package org.example.route.dto

import com.fasterxml.jackson.annotation.JsonProperty
import java.time.Instant
import org.example.route.model.RouteRating
import org.example.route.model.DailyPlan
import org.example.route.model.Segment
import org.example.route.model.Waypoint
import org.example.user.model.User
import org.example.water.dto.WaterSourceDto

data class RouteDto(
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
    @JsonProperty("create_user")
    val createUser: UserDto?,
    // waypoints字段已移除，waypoint数据现在通过segments返回
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
)

data class UserDto(
    val id: String,
    val username: String,
    val nickname: String?,
    @JsonProperty("avatar_url")
    val avatarUrl: String?
)



data class RatingDto(
    val overall: Double?,
    val scenery: Double?,
    val difficulty: Double?,
    val experience: Double?,
    val facilities: Double?,
    @JsonProperty("rating_count")
    val ratingCount: Int
)

data class WeatherInfoDto(
    val description: String?,
    val precautions: String?,
    @JsonProperty("best_seasons")
    val bestSeasons: List<String> = emptyList(),
    @JsonProperty("seasonal_weather")
    val seasonalWeather: Map<String, String> = emptyMap()
)

data class HitchhikeContactDto(
    val id: String,
    val name: String,
    val phone: String,
    val description: String?,
    val location: String?,
    val price: Double?,
    @JsonProperty("last_verified")
    val lastVerified: Boolean
)
