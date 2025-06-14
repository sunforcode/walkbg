package org.example.route.dto

import com.fasterxml.jackson.annotation.JsonProperty
import org.example.route.model.Segment
import org.example.route.dto.WaypointDto

/**
 * 路段DTO
 */
data class SegmentDto(
    val id: String,
    val name: String,
    val description: String?,
    val distance: Double?,
    @JsonProperty("elevation_gain")
    val elevationGain: Double?,
    @JsonProperty("elevation_loss")
    val elevationLoss: Double?,
    @JsonProperty("estimated_time")
    val estimatedTime: Double?,
    val difficulty: Int?,
    @JsonProperty("route_type")
    val routeType: Int?,
    val notes: String?,
    @JsonProperty("start_point")
    val startPoint: WaypointDto?,
    @JsonProperty("end_point")
    val endPoint: WaypointDto?,
    val keypoints: List<WaypointDto> = emptyList()
)


