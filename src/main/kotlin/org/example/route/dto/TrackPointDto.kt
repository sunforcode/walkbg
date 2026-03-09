package org.example.route.dto

import com.fasterxml.jackson.annotation.JsonProperty
import org.example.route.model.Waypoint

/**
 * 轨迹点DTO - 用于表示路线上的轨迹点坐标信息
 */
data class TrackPointDto(
    val id: String,
    val name: String?,
    val latitude: Double,
    val longitude: Double,
    val elevation: Double?,
    @JsonProperty("sequence_number")
    val sequenceNumber: Int,
    val type: String? = null,
    @JsonProperty("icon_url")
    val iconUrl: String? = null,
    @JsonProperty("image_url")
    val imageUrl: String? = null,
    val description: String? = null
) {
    companion object {
        /**
         * 从Waypoint实体创建TrackPointDto
         */
        fun fromWaypoint(waypoint: Waypoint): TrackPointDto {
            return TrackPointDto(
                id = waypoint.id,
                name = waypoint.name,
                latitude = waypoint.latitude,
                longitude = waypoint.longitude,
                elevation = waypoint.elevation,
                sequenceNumber = waypoint.sequenceNumber,
                type = waypoint.type,
                iconUrl = waypoint.iconUrl,
                imageUrl = waypoint.imageUrl,
                description = waypoint.description
            )
        }
    }
}
