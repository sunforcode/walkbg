package org.example.model

import jakarta.persistence.*
import java.time.Instant

@Entity
@Table(name = "routes")
data class Route(
    @Id
    val id: String,
    
    @Column(nullable = false)
    val name: String,
    
    @Column(columnDefinition = "TEXT")
    val description: String? = null,
    
    val regionId: String? = null,
    
    val region: String? = null,
    
    val distance: Double? = null,
    
    val duration: String? = null,
    
    val elevationGain: Double? = null,
    
    val elevationLoss: Double? = null,
    
    val difficulty: Int? = null,
    
    val routeType: Int? = null,
    
    val routeDirection: Int? = null,
    
    val coverUrl: String? = null,
    
    val mapDataId: String? = null,
    
    val createdBy: String? = null,
    
    val popularity: Int = 0,
    
    val status: String? = null,
    
    @Column(nullable = false)
    val createdAt: Instant = Instant.now(),
    
    @Column(nullable = false)
    val updatedAt: Instant = Instant.now(),
    
    @OneToMany(mappedBy = "route", cascade = [CascadeType.ALL], orphanRemoval = true)
    val waypoints: MutableList<Waypoint> = mutableListOf(),
    
    @OneToMany(mappedBy = "route", cascade = [CascadeType.ALL], orphanRemoval = true)
    val segments: MutableList<Segment> = mutableListOf(),
    
    @OneToMany(mappedBy = "route", cascade = [CascadeType.ALL], orphanRemoval = true)
    val dailyPlans: MutableList<DailyPlan> = mutableListOf(),
    
    @OneToMany(mappedBy = "route", cascade = [CascadeType.ALL], orphanRemoval = true)
    val seasons: MutableList<RouteSeason> = mutableListOf(),
    
    @OneToMany(mappedBy = "route", cascade = [CascadeType.ALL], orphanRemoval = true)
    val tags: MutableList<RouteTag> = mutableListOf(),
    
    @OneToOne(mappedBy = "route", cascade = [CascadeType.ALL], orphanRemoval = true)
    var rating: RouteRating? = null,
    
    @OneToMany(mappedBy = "route", cascade = [CascadeType.ALL], orphanRemoval = true)
    val images: MutableList<RouteImage> = mutableListOf(),
    
    @OneToOne(mappedBy = "route", cascade = [CascadeType.ALL], orphanRemoval = true)
    var facilities: RouteFacilities? = null,
    
    @OneToOne(mappedBy = "route", cascade = [CascadeType.ALL], orphanRemoval = true)
    var weatherInfo: RouteWeather? = null,
    
    @OneToOne(mappedBy = "route", cascade = [CascadeType.ALL], orphanRemoval = true)
    var safetyInfo: SafetyInfo? = null
) {
    // 添加关联实体的辅助方法
    fun addWaypoint(waypoint: Waypoint) {
        waypoints.add(waypoint)
        waypoint.route = this
    }
    
    fun addSegment(segment: Segment) {
        segments.add(segment)
        segment.route = this
    }
    
    fun addDailyPlan(dailyPlan: DailyPlan) {
        dailyPlans.add(dailyPlan)
        dailyPlan.route = this
    }
    
    fun addSeason(season: String) {
        seasons.add(RouteSeason(route = this, season = season))
    }
    
    fun addTag(tag: String) {
        tags.add(RouteTag(route = this, tag = tag))
    }
    
    fun addImage(url: String, isCover: Boolean = false, sequenceNumber: Int = images.size + 1) {
        images.add(RouteImage(route = this, imageUrl = url, isCover = isCover, sequenceNumber = sequenceNumber))
    }
    
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false
        
        other as Route
        
        return id == other.id
    }
    
    override fun hashCode(): Int {
        return id.hashCode()
    }
    
    override fun toString(): String {
        return "Route(id='$id', name='$name')"
    }
}