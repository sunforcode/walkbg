package org.example.route.model

import jakarta.persistence.*

/**
 * 路线天气实体
 */
@Entity
@Table(name = "route_weather")
data class RouteWeather(
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    val id: Long = 0,
    
    @Column(columnDefinition = "TEXT")
    val description: String? = null,
    
    @Column(columnDefinition = "TEXT")
    val precautions: String? = null,
    
    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "route_id")
    var route: Route? = null,
    
    @OneToMany(mappedBy = "routeWeather", cascade = [CascadeType.ALL], orphanRemoval = true)
    val seasonalWeather: MutableList<SeasonalWeather> = mutableListOf()
) {
    fun addSeasonalWeather(season: String, description: String) {
        seasonalWeather.add(SeasonalWeather(routeWeather = this, season = season, description = description))
    }
}

/**
 * 季节性天气实体
 */
@Entity
@Table(name = "seasonal_weather")
data class SeasonalWeather(
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    val id: Long = 0,
    
    @Column(nullable = false)
    val season: String,
    
    @Column(columnDefinition = "TEXT")
    val description: String? = null,
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "route_weather_id")
    var routeWeather: RouteWeather? = null
)