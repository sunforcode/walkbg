package org.example.route.dto

import com.fasterxml.jackson.annotation.JsonProperty
import java.math.BigDecimal

/**
 * 路线创建请求DTO
 */
data class RouteCreateRequest(
    val id: String? = null,
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
    val status: Int = 1,
    @JsonProperty("cover_url")
    val coverUrl: String?,
    @JsonProperty("map_data_id")
    val mapDataId: String?,
    @JsonProperty("default_map_id")
    val defaultMapId: String = "",
    @JsonProperty("created_by")
    val createdBy: String,
    
    // 关联数据
    val tags: List<String> = emptyList(),
    val seasons: List<String> = emptyList(),
    val waypoints: List<WaypointCreateRequest> = emptyList(),
    val segments: List<SegmentCreateRequest> = emptyList(),
    val images: List<RouteImageCreateRequest> = emptyList()
)

/**
 * 路径点创建请求DTO
 */
data class WaypointCreateRequest(
    val id: String? = null,
    val name: String,
    val description: String?,
    val latitude: Double,
    val longitude: Double,
    val elevation: Double?,
    val type: String?,
    @JsonProperty("icon_url")
    val iconUrl: String?,
    @JsonProperty("image_url")
    val imageUrl: String?,
    @JsonProperty("sequence_number")
    val sequenceNumber: Int
)

/**
 * 路段创建请求DTO
 */
data class SegmentCreateRequest(
    val id: String? = null,
    val distance: Double?,
    @JsonProperty("elevation_gain")
    val elevationGain: Double?,
    @JsonProperty("elevation_loss")
    val elevationLoss: Double?,
    @JsonProperty("estimated_time")
    val estimatedTime: Double?,
    val difficulty: Int?,
    val terrain: String?,
    @JsonProperty("surface_type")
    val surfaceType: String?,
    @JsonProperty("traffic_level")
    val trafficLevel: Int?
)
/**
 * 路线图片创建请求DTO
 */
data class RouteImageCreateRequest(
    val imageUrl: String,
    val isCover: Boolean = false,
    val sequenceNumber: Int
)
