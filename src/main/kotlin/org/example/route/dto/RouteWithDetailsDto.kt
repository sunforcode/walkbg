package org.example.route.dto

import com.fasterxml.jackson.annotation.JsonProperty
import java.math.BigDecimal
import java.time.Instant
import org.example.route.model.Route

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
    val distance: BigDecimal?,
    val duration: Int?,
    val latitude: BigDecimal?,
    val longitude: BigDecimal?,
    val altitude: BigDecimal?,
    @JsonProperty("elevation_gain")
    val elevationGain: BigDecimal?,
    @JsonProperty("elevation_loss")
    val elevationLoss: BigDecimal?,
    val difficulty: Int?,
    @JsonProperty("route_type")
    val routeType: Int?,
    @JsonProperty("route_direction")
    val routeDirection: Int?,
    val status: Int,
    @JsonProperty("cover_url")
    val coverUrl: String?,
    @JsonProperty("map_data_id")
    val mapDataId: String?,
    @JsonProperty("default_map_id")
    val defaultMapId: String?,
    @JsonProperty("created_by")
    val createdBy: String?,
    val popularity: Int,
    @JsonProperty("created_at")
    val createdAt: Instant,
    @JsonProperty("updated_at")
    val updatedAt: Instant,
    
    // 关联数据
    val tags: List<String> = emptyList(),
    val seasons: List<String> = emptyList(),
    val images: List<RouteImageDto> = emptyList(),
    // waypoints字段已移除，waypoint数据现在通过segments返回
    val segments: List<SegmentDto> = emptyList(),
    
    // 统计数据
    @JsonProperty("favorite_count")
    val favoriteCount: Long = 0,
    @JsonProperty("completion_count")
    val completionCount: Long = 0,
    @JsonProperty("trip_count")
    val tripCount: Long = 0,
    
    // 创建者信息
    @JsonProperty("creator_name")
    val creatorName: String? = null,
    @JsonProperty("creator_avatar_url")
    val creatorAvatarUrl: String? = null,
    
    // 评分信息
    val rating: RouteRatingDto? = null
)

/**
 * 路线图片DTO
 */
data class RouteImageDto(
    val id: Long,
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
