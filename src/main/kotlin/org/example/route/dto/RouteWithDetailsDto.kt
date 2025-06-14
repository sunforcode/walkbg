package org.example.route.dto

import com.fasterxml.jackson.annotation.JsonProperty
import java.math.BigDecimal
import java.time.Instant
import org.example.route.model.Route
import org.example.user.dto.UserBasicDto

/**
 * 路线详细信息DTO - 包含所有关联数据
 */
data class RouteWithDetailsDto(
    // 基础信息
    val id: String,
    val name: String,
    val description: String?,
    val region: String?,
    @JsonProperty("region_id")
    val regionId: String?,
    val difficulty: Int?,
    @JsonProperty("route_type")
    val routeType: Int?,

    // 从关联的 mapData 获取的地理信息
    val distance: BigDecimal?,
    val duration: Int?,
    val latitude: BigDecimal?,
    val longitude: BigDecimal?,
    val altitude: BigDecimal?,
    @JsonProperty("elevation_gain")
    val elevationGain: BigDecimal?,
    @JsonProperty("elevation_loss")
    val elevationLoss: BigDecimal?,
    val status: Int,
    @JsonProperty("cover_url")
    val coverUrl: String?,
    @JsonProperty("default_map_id")
    val defaultMapId: String?,
    val popularity: Int,
    @JsonProperty("usage_count")
    val usageCount: Int,
    @JsonProperty("is_loop")
    val isLoop: Boolean,
    @JsonProperty("image_urls")
    val imageUrls: List<String> = emptyList(),
    @JsonProperty("is_favorite")
    val isFavorite: Boolean,
    @JsonProperty("created_at")
    val createdAt: Instant,
    @JsonProperty("updated_at")
    val updatedAt: Instant,
    
    // 关联数据
    val tags: List<String> = emptyList(),
    // waypoints字段已移除，waypoint数据现在通过segments返回
    val segments: List<SegmentDto> = emptyList(),
    val campsites: List<CampsiteDto> = emptyList(),
    @JsonProperty("marker_points")
    val markerPoints: List<MarkerPointDto> = emptyList(),
    val supplies: List<SupplyDto> = emptyList(),
    @JsonProperty("water_sources")
    val waterSources: List<org.example.water.dto.WaterSourceDto> = emptyList(),
    @JsonProperty("daily_plans")
    val dailyPlans: List<DailyPlanDto> = emptyList(),
    @JsonProperty("weather_info")
    val weatherInfo: RouteWeatherDto? = null,
    @JsonProperty("hitchhike_contacts")
    val hitchhikeContacts: List<ContactDto> = emptyList(),
    
    // 统计数据（从 mapData 获取）
    @JsonProperty("favorite_count")
    val favoriteCount: Long = 0,
    @JsonProperty("completion_count")
    val completionCount: Long = 0,
    @JsonProperty("trip_count")
    val tripCount: Long = 0,
    
    // 创建者信息（从关联的 creator 获取）
    val creator: UserBasicDto? = null,
    
    // 评分信息
    val ratings: RouteRatingDto? = null
)

/**
 * 路线图片DTO
 */
data class RouteImageDto(
    val id: String,
    @JsonProperty("image_url")
    val imageUrl: String,
    @JsonProperty("is_cover")
    val isCover: Boolean,
    @JsonProperty("sequence_number")
    val sequenceNumber: Int
)


/**
 * 路线评分DTO
 */
data class RouteRatingDto(
    val overall: Double?,
    val scenery: Double?,
    val difficulty: Double?,
    val experience: Double?,
    val facilities: Double?,
    @JsonProperty("rating_count")
    val ratingCount: Int
)
