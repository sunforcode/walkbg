package org.example.model

import jakarta.persistence.*
import java.time.Instant
@Entity
@Table(name = "waypoints")
data class Waypoint(
    @Id
    @Column(length = 64)
    val id: String,
    @Column(nullable = false)
    val name: String,
    @Column(columnDefinition = "TEXT")
    val description: String? = null,
    val latitude: Double,
    val longitude: Double,
    val elevation: Double? = null,
    val type: String? = null,
    val iconUrl: String? = null,
    val imageUrl: String? = null,
    val sequenceNumber: Int,

    @Column(name = "created_at", nullable = false, updatable = false)
    val createdAt: Instant = Instant.now(),

    @Column(name = "updated_at", nullable = false)
    var updatedAt: Instant = Instant.now(),

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "route_id")
    var route: Route? = null
) {
    override fun toString(): String {
        return "Waypoint(id='$id', name='$name')"
    }
}
@Entity
@Table(name = "segments")
data class Segment(
    @Id
    @Column(length = 64)
    val id: String,

    val distance: Double? = null,
    val elevationGain: Double? = null,
    val elevationLoss: Double? = null,
    val estimatedTime: Double? = null,
    val difficulty: Int? = null,
    val terrain: String? = null,
    val surfaceType: String? = null,
    val trafficLevel: Int? = null,

    @Column(name = "created_at", nullable = false, updatable = false)
    val createdAt: Instant = Instant.now(),

    @Column(name = "updated_at", nullable = false)
    var updatedAt: Instant = Instant.now(),
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "route_id")
    var route: Route? = null,
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "start_point")
    var startPoint: Waypoint? = null,
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "end_point")
    var endPoint: Waypoint? = null,
    @OneToMany(mappedBy = "segment", cascade = [CascadeType.ALL], orphanRemoval = true)
    val pathPoints: MutableList<PathPoint> = mutableListOf(),
    @OneToMany(mappedBy = "segment", cascade = [CascadeType.ALL], orphanRemoval = true)
    val hazards: MutableList<SegmentHazard> = mutableListOf(),
    @OneToMany(mappedBy = "segment", cascade = [CascadeType.ALL], orphanRemoval = true)
    val closures: MutableList<SegmentClosure> = mutableListOf()
) {
    fun addPathPoint(pathPoint: PathPoint) {
        pathPoints.add(pathPoint)
        pathPoint.segment = this
    }
    fun addHazard(hazard: String) {
        hazards.add(SegmentHazard(segment = this, hazard = hazard))
    }
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false
        other as Segment
        return id == other.id
    }
    override fun hashCode(): Int {
        return id.hashCode()
    }
    override fun toString(): String {
        return "Segment(id='$id')"
    }
}

@Entity
@Table(name = "path_points")
data class PathPoint(
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    val id: Long = 0,
    val latitude: Double,
    val longitude: Double,
    val elevation: Double? = null,
    val timestamp: Instant? = null,
    val distanceFromStart: Double? = null,
    val pointType: String? = null,
    val name: String? = null,
    @Column(columnDefinition = "TEXT")
    val description: String? = null,
    val type: String? = null,
    val sequenceNumber: Int,
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "segment_id")
    var segment: Segment? = null
)

@Entity
@Table(name = "segment_hazards")
data class SegmentHazard(
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    val id: Long = 0,
    @Column(nullable = false)
    val hazard: String,
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "segment_id")
    var segment: Segment? = null
)

@Entity
@Table(name = "segment_closures")
data class SegmentClosure(
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    val id: Long = 0,
    val startDate: Instant? = null,
    val endDate: Instant? = null,
    @Column(columnDefinition = "TEXT")
    val reason: String? = null,
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "segment_id")
    var segment: Segment? = null
)

@Entity
@Table(name = "daily_plans")
data class DailyPlan(
    @Id
    @Column(length = 64)
    val id: String,

    val dayNumber: Int,
    val title: String,
    @Column(columnDefinition = "TEXT")
    val description: String? = null,
    val distance: Double? = null,
    val duration: String? = null,
    val elevationGain: Double? = null,
    val elevationLoss: Double? = null,
    val accommodation: String? = null,

    @Column(name = "created_at", nullable = false, updatable = false)
    val createdAt: Instant = Instant.now(),

    @Column(name = "updated_at", nullable = false)
    var updatedAt: Instant = Instant.now(),

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "route_id")
    var route: Route? = null,
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "start_waypoint_id")
    var startWaypoint: Waypoint? = null,
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "end_waypoint_id")
    var endWaypoint: Waypoint? = null,
    @OneToMany(mappedBy = "dailyPlan", cascade = [CascadeType.ALL], orphanRemoval = true)
    val segments: MutableList<DailyPlanSegment> = mutableListOf()
) {
    fun addSegment(segment: Segment, sequenceNumber: Int = this.segments.size + 1) {
        segments.add(DailyPlanSegment(dailyPlan = this, segment = segment, sequenceNumber = sequenceNumber))
    }
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false
        other as DailyPlan
        return id == other.id
    }
    override fun hashCode(): Int {
        return id.hashCode()
    }
    override fun toString(): String {
        return "DailyPlan(id='$id', title='$title')"
    }
}

@Entity
@Table(name = "daily_plan_segments")
data class DailyPlanSegment(
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    val id: Long = 0,
    val sequenceNumber: Int,
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "daily_plan_id")
    var dailyPlan: DailyPlan? = null,
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "segment_id")
    var segment: Segment? = null
)

@Entity
@Table(name = "route_seasons")
data class RouteSeason(
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    val id: Long = 0,
    @Column(nullable = false)
    val season: String,
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "route_id")
    var route: Route? = null
)

@Entity
@Table(name = "route_tags")
data class RouteTag(
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    val id: Long = 0,
    @Column(nullable = false)
    val tag: String,
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "route_id")
    var route: Route? = null
)

@Entity
@Table(name = "route_ratings")
data class RouteRating(
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    val id: Long = 0,
    val overall: Double? = null,
    val scenery: Double? = null,
    val difficulty: Double? = null,
    val experience: Double? = null,
    val facilities: Double? = null,
    val ratingCount: Int = 0,
    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "route_id")
    var route: Route? = null
)

@Entity
@Table(name = "route_images")
data class RouteImage(
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    val id: Long = 0,
    @Column(nullable = false)
    val imageUrl: String,
    val isCover: Boolean = false,
    val sequenceNumber: Int,
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "route_id")
    var route: Route? = null
)

@Entity
@Table(name = "route_facilities")
data class RouteFacilities(
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    val id: Long = 0,
    @Column(columnDefinition = "TEXT")
    val water: String? = null,
    @Column(columnDefinition = "TEXT")
    val food: String? = null,
    @Column(columnDefinition = "TEXT")
    val accommodation: String? = null,
    @Column(columnDefinition = "TEXT")
    val toilets: String? = null,
    @Column(columnDefinition = "TEXT")
    val signalCoverage: String? = null,
    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "route_id")
    var route: Route? = null
)

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

@Entity
@Table(name = "safety_info")
data class SafetyInfo(
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    val id: Long = 0,
    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "route_id")
    var route: Route? = null,
    @OneToMany(mappedBy = "safetyInfo", cascade = [CascadeType.ALL], orphanRemoval = true)
    val emergencyContacts: MutableList<EmergencyContact> = mutableListOf(),
    @OneToMany(mappedBy = "safetyInfo", cascade = [CascadeType.ALL], orphanRemoval = true)
    val riskAreas: MutableList<RiskArea> = mutableListOf(),
    @OneToMany(mappedBy = "safetyInfo", cascade = [CascadeType.ALL], orphanRemoval = true)
    val signalAreas: MutableList<SignalArea> = mutableListOf()
) {
    fun addEmergencyContact(name: String, phone: String, description: String? = null) {
        emergencyContacts.add(EmergencyContact(safetyInfo = this, name = name, phone = phone, description = description))
    }
    fun addRiskArea(riskArea: RiskArea) {
        riskAreas.add(riskArea)
        riskArea.safetyInfo = this
    }
    fun addSignalArea(signalArea: SignalArea) {
        signalAreas.add(signalArea)
        signalArea.safetyInfo = this
    }
}

@Entity
@Table(name = "emergency_contacts")
data class EmergencyContact(
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    val id: Long = 0,
    @Column(nullable = false)
    val name: String,
    @Column(nullable = false)
    val phone: String,
    @Column(columnDefinition = "TEXT")
    val description: String? = null,
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "safety_info_id")
    var safetyInfo: SafetyInfo? = null
)

@Entity
@Table(name = "risk_areas")
data class RiskArea(
    @Id
    @Column(length = 64)
    val id: String,

    @Column(nullable = false)
    val name: String,
    @Column(columnDefinition = "TEXT")
    val description: String? = null,
    val level: Int? = null,

    @Column(name = "created_at", nullable = false, updatable = false)
    val createdAt: Instant = Instant.now(),

    @Column(name = "updated_at", nullable = false)
    var updatedAt: Instant = Instant.now(),

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "safety_info_id")
    var safetyInfo: SafetyInfo? = null,
    @OneToMany(mappedBy = "riskArea", cascade = [CascadeType.ALL], orphanRemoval = true)
    val boundaries: MutableList<RiskAreaBoundary> = mutableListOf()
) {
    fun addBoundaryPoint(latitude: Double, longitude: Double, altitude: Double? = null, sequenceNumber: Int = boundaries.size + 1) {
        boundaries.add(RiskAreaBoundary(
            riskArea = this,
            latitude = latitude,
            longitude = longitude,
            altitude = altitude,
            sequenceNumber = sequenceNumber
        ))
    }
}

@Entity
@Table(name = "risk_area_boundaries")
data class RiskAreaBoundary(
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    val id: Long = 0,
    val latitude: Double,
    val longitude: Double,
    val altitude: Double? = null,
    val sequenceNumber: Int,
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "risk_area_id")
    var riskArea: RiskArea? = null
)

@Entity
@Table(name = "signal_areas")
data class SignalArea(
    @Id
    @Column(length = 64)
    val id: String,

    @Column(nullable = false)
    val name: String,
    @Column(columnDefinition = "TEXT")
    val description: String? = null,
    val level: Int? = null,

    @Column(name = "created_at", nullable = false, updatable = false)
    val createdAt: Instant = Instant.now(),

    @Column(name = "updated_at", nullable = false)
    var updatedAt: Instant = Instant.now(),

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "safety_info_id")
    var safetyInfo: SafetyInfo? = null,
    @OneToMany(mappedBy = "signalArea", cascade = [CascadeType.ALL], orphanRemoval = true)
    val boundaries: MutableList<SignalAreaBoundary> = mutableListOf()
) {
    fun addBoundaryPoint(latitude: Double, longitude: Double, altitude: Double? = null, sequenceNumber: Int = boundaries.size + 1) {
        boundaries.add(SignalAreaBoundary(
            signalArea = this,
            latitude = latitude,
            longitude = longitude,
            altitude = altitude,
            sequenceNumber = sequenceNumber
        ))
    }
}

@Entity
@Table(name = "signal_area_boundaries")
data class SignalAreaBoundary(
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    val id: Long = 0,

    val latitude: Double,

    val longitude: Double,

    val altitude: Double? = null,

    val sequenceNumber: Int,

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "signal_area_id")
    var signalArea: SignalArea? = null
)
