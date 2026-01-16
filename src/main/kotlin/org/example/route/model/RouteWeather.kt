package org.example.route.model

import jakarta.persistence.*

/**
 * 路线天气实体（单向关联）
 */
@Entity
@Table(
    name = "route_weather",
    indexes = [
        Index(name = "idx_route_weather_route_id", columnList = "route_id", unique = true)
    ]
)
data class RouteWeather(
    @Id
    @Column(length = 64)
    val id: String,

    @Column(name = "route_id", length = 64, nullable = false, unique = true)
    val routeId: String,
    
    @Column(columnDefinition = "TEXT")
    val description: String? = null,
    
    @Column(columnDefinition = "TEXT")
    val precautions: String? = null,
    
    @Column(name = "best_seasons", columnDefinition = "TEXT")
    val bestSeasons: String? = null // JSON 字符串存储最佳季节数组
) {
    /**
     * 注意：不再持有 seasonalWeather 集合
     * 通过 SeasonalWeatherRepository.findByRouteWeatherId(routeWeatherId) 查询
     */
    
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false
        other as RouteWeather
        return id == other.id
    }

    override fun hashCode(): Int {
        return id.hashCode()
    }

    override fun toString(): String {
        return "RouteWeather(id='$id', routeId='$routeId')"
    }
}

/**
 * 季节性天气实体（单向关联）
 */
@Entity
@Table(
    name = "seasonal_weather",
    indexes = [
        Index(name = "idx_seasonal_weather_route_weather_id", columnList = "route_weather_id"),
        Index(name = "idx_seasonal_weather_season", columnList = "season")
    ]
)
data class SeasonalWeather(
    @Id
    @Column(length = 64)
    val id: String,

    @Column(name = "route_weather_id", length = 64, nullable = false)
    val routeWeatherId: String,
    
    @Column(nullable = false, length = 50)
    val season: String,
    
    @Column(columnDefinition = "TEXT")
    val description: String? = null
) {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false
        other as SeasonalWeather
        return id == other.id
    }

    override fun hashCode(): Int {
        return id.hashCode()
    }

    override fun toString(): String {
        return "SeasonalWeather(id='$id', season='$season')"
    }
}
