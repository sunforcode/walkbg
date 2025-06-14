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
) {
    companion object {
        /**
         * 从DailyPlan实体创建DTO
         */
        fun fromDailyPlan(plan: org.example.route.model.DailyPlan): DailyPlanDto {
            return DailyPlanDto(
                id = plan.id,
                dayNumber = plan.dayNumber,
                title = plan.title,
                description = plan.description,
                distance = plan.distance,
                estimatedTime = plan.estimatedTime,
                elevationGain = plan.elevationGain?.toInt(),
                elevationLoss = plan.elevationLoss,
                maxElevation = plan.maxElevation,
                minElevation = plan.minElevation,
                accommodation = plan.accommodation,
                notes = plan.notes,
                createdAt = plan.createdAt,
                updatedAt = plan.updatedAt,
                segments = emptyList() // 简化处理，避免循环引用
            )
        }
    }
}

/**
 * 每日计划路段DTO
 */
data class DailyPlanSegmentDto(
    val id: String,
    @JsonProperty("sequence_number")
    val sequenceNumber: Int,
    val segment: SegmentDto?
)
