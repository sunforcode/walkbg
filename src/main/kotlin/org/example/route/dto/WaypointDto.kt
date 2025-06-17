package org.example.route.dto

import com.fasterxml.jackson.annotation.JsonProperty
import org.example.route.model.Waypoint
import java.time.Instant

/**
 * 路径点DTO
 */
data class WaypointDto(
    val id: String,
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
    val sequenceNumber: Int,
    @JsonProperty("created_at")
    val createdAt: Instant,
    @JsonProperty("updated_at")
    val updatedAt: Instant
) {
    companion object {
        /**
         * 从Waypoint实体创建DTO
         */
        fun fromWaypoint(waypoint: Waypoint): WaypointDto {
            return WaypointDto(
                id = waypoint.id,
                name = waypoint.name,
                description = waypoint.description,
                latitude = waypoint.latitude,
                longitude = waypoint.longitude,
                elevation = waypoint.elevation,
                type = waypoint.type,
                iconUrl = waypoint.iconUrl,
                imageUrl = waypoint.imageUrl,
                sequenceNumber = waypoint.sequenceNumber,
                createdAt = waypoint.createdAt,
                updatedAt = waypoint.updatedAt
            )
        }
    }
}
