package org.example.route.dto

import com.fasterxml.jackson.annotation.JsonProperty

/**
 * 天气信息DTO
 */
data class WeatherInfoDto(
    val description: String?,
    val precautions: String?,
    @JsonProperty("best_seasons")
    val bestSeasons: List<String> = emptyList(),
    @JsonProperty("seasonal_weather")
    val seasonalWeather: Map<String, String> = emptyMap()
)
