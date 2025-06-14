package org.example.route.dto

import com.fasterxml.jackson.annotation.JsonProperty

/**
 * 路线天气DTO
 */
data class RouteWeatherDto(
    val id: Long,
    val description: String?,
    val precautions: String?,
    @JsonProperty("best_seasons")
    val bestSeasons: List<String> = emptyList(),
    @JsonProperty("seasonal_weather")
    val seasonalWeather: List<SeasonalWeatherDto> = emptyList()
)

/**
 * 季节性天气DTO
 */
data class SeasonalWeatherDto(
    val id: Long,
    val season: String,
    val description: String?
)
