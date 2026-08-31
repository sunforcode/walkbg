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
    @JsonProperty("scheme_type")
    val schemeType: String? = null,
    /**
     * 数据状态: draft(分析建议草稿) | confirmed(人工确认)
     */
    val status: String = "confirmed",
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
                schemeType = segment.schemeType,
                status = segment.status,
                startPoint = null,
                endPoint = null,
                keypoints = emptyList()
            )
        }

        /**
         * 从 Segment 实体和关联的 Waypoint 创建 DTO
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
                schemeType = segment.schemeType,
                status = segment.status,
                startPoint = startPoint,
                endPoint = endPoint,
                keypoints = keypoints
            )
        }
    }
}

/**
 * 分段方案 DTO（对外 API 返回给 App）
 */
data class SegmentSchemeDto(
    val id: String,
    @JsonProperty("route_id")
    val routeId: String,
    @JsonProperty("scheme_type")
    val schemeType: String,
    val label: String,
    @JsonProperty("is_default")
    val isDefault: Boolean,
    val segments: List<SegmentDto> = emptyList()
) {
    companion object {
        fun fromScheme(
            scheme: org.example.route.model.SegmentScheme,
            segments: List<SegmentDto> = emptyList()
        ): SegmentSchemeDto {
            return SegmentSchemeDto(
                id = scheme.id,
                routeId = scheme.routeId,
                schemeType = scheme.schemeType,
                label = scheme.label,
                isDefault = scheme.isDefault,
                segments = segments
            )
        }
    }
}

