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
    val terrain: Int?, // 0: 石阶路, 1: 山路, 2: 岩石路, 3: 雪地, 4: 其他
    @JsonProperty("surface_type")
    val surfaceType: String?,
    val notes: String?,
    @JsonProperty("start_point")
    val startPoint: WaypointDto?,
    @JsonProperty("end_point")
    val endPoint: WaypointDto?,
    val keypoints: List<WaypointDto> = emptyList()
)


