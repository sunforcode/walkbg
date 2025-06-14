package org.example.route.dto

import com.fasterxml.jackson.annotation.JsonProperty
import java.time.Instant

/**
 * 标记点DTO
 */
data class MarkerPointDto(
    val id: String,
    val name: String?,
    val description: String?,
    @JsonProperty("marker_type")
    val markerType: Int,
    @JsonProperty("icon_url")
    val iconUrl: String?,
    val latitude: Double?,
    val longitude: Double?,
    val elevation: Double?,
    val color: String?,
    @JsonProperty("created_at")
    val createdAt: Instant,
    @JsonProperty("updated_at")
    val updatedAt: Instant
) {
    companion object {
        /**
         * 从MarkerPoint实体创建DTO
         */
        fun fromMarkerPoint(marker: org.example.route.model.MarkerPoint): MarkerPointDto {
            return MarkerPointDto(
                id = marker.id,
                name = marker.name,
                description = marker.description,
                markerType = marker.markerType,
                iconUrl = marker.iconUrl,
                latitude = marker.latitude,
                longitude = marker.longitude,
                elevation = marker.elevation,
                color = marker.color,
                createdAt = marker.createdAt,
                updatedAt = marker.updatedAt
            )
        }
    }
}
