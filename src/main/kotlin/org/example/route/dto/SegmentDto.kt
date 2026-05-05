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
    @JsonProperty("sequence_number")
    val sequenceNumber: Int,
    @JsonProperty("track_start_index")
    val trackStartIndex: Int?,
    @JsonProperty("track_end_index")
    val trackEndIndex: Int?,
    val color: String?,
    @JsonProperty("start_point")
    val startPoint: WaypointDto?,
    @JsonProperty("end_point")
    val endPoint: WaypointDto?,
    val keypoints: List<WaypointDto> = emptyList()
) {
    companion object {
        /**
         * 从Segment实体创建DTO
         */
        fun fromSegment(segment: org.example.route.model.Segment): SegmentDto {
            return SegmentDto(
                id = segment.id,
                name = segment.name,
                description = segment.description,
                distance = segment.distance,
                elevationGain = segment.elevationGain,
                elevationLoss = segment.elevationLoss,
                estimatedTime = segment.estimatedTime,
                difficulty = segment.difficulty,
                routeType = segment.routeType,
                notes = segment.notes,
                sequenceNumber = segment.sequenceNumber,
                trackStartIndex = segment.trackStartIndex,
                trackEndIndex = segment.trackEndIndex,
                color = segment.color,
                startPoint = null,
                endPoint = null,
                keypoints = emptyList()
            )
        }

        /**
         * 从Segment实体和关联的Waypoint创建DTO
         */
        fun fromSegmentWithWaypoints(
            segment: org.example.route.model.Segment,
            startPoint: WaypointDto? = null,
            endPoint: WaypointDto? = null,
            keypoints: List<WaypointDto> = emptyList()
        ): SegmentDto {
            return SegmentDto(
                id = segment.id,
                name = segment.name,
                description = segment.description,
                distance = segment.distance,
                elevationGain = segment.elevationGain,
                elevationLoss = segment.elevationLoss,
                estimatedTime = segment.estimatedTime,
                difficulty = segment.difficulty,
                routeType = segment.routeType,
                notes = segment.notes,
                sequenceNumber = segment.sequenceNumber,
                trackStartIndex = segment.trackStartIndex,
                trackEndIndex = segment.trackEndIndex,
                color = segment.color,
                startPoint = startPoint,
                endPoint = endPoint,
                keypoints = keypoints
            )
        }
    }
}

