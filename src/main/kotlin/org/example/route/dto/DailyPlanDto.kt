package org.example.route.dto

import com.fasterxml.jackson.annotation.JsonProperty
import java.time.Instant

/**
 * 每日计划DTO
 */
data class DailyPlanDto(
    val id: String,
    @JsonProperty("day_number")
    val dayNumber: Int,
    val title: String,
    val description: String?,
    val distance: Double?,
    @JsonProperty("estimated_time")
    val estimatedTime: Double?,
    @JsonProperty("elevation_gain")
    val elevationGain: Int?,
    @JsonProperty("elevation_loss")
    val elevationLoss: Double?,
    @JsonProperty("max_elevation")
    val maxElevation: Double?,
    @JsonProperty("min_elevation")
    val minElevation: Double?,
    val accommodation: String?,
    val notes: String?,
    @JsonProperty("created_at")
    val createdAt: Instant,
    @JsonProperty("updated_at")
    val updatedAt: Instant,
    val segments: List<DailyPlanSegmentDto> = emptyList()
)

/**
 * 每日计划路段DTO
 */
data class DailyPlanSegmentDto(
    val id: String,
    @JsonProperty("sequence_number")
    val sequenceNumber: Int,
    val segment: SegmentDto?
)
