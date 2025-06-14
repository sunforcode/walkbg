package org.example.route.dto

import com.fasterxml.jackson.annotation.JsonProperty

/**
 * 路线评分DTO
 */
data class RatingDto(
    val overall: Double?,
    val scenery: Double?,
    val difficulty: Double?,
    val experience: Double?,
    val facilities: Double?,
    @JsonProperty("rating_count")
    val ratingCount: Int
)
