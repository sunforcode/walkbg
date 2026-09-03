package org.example.route.model

import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.Id
import jakarta.persistence.Table
import java.math.BigDecimal
import java.time.Instant

@Entity
@Table(name = "route_versions")
data class RouteVersion(
    @Id
    @Column(length = 64)
    val id: String,

    @Column(name = "route_id", nullable = false, length = 64)
    val routeId: String,

    @Column(name = "version_label", length = 200)
    val versionLabel: String? = null,

    @Column(name = "route_type", length = 32)
    val routeType: String?,

    @Column(length = 200)
    val name: String? = null,

    @Column(length = 100)
    val region: String? = null,

    @Column(name = "start_name", length = 200)
    val startName: String? = null,

    @Column(name = "end_name", length = 200)
    val endName: String? = null,

    @Column(name = "estimated_duration_seconds")
    val estimatedDurationSeconds: Long? = null,

    @Column(length = 100)
    val difficulty: String? = null,

    @Column(length = 100)
    val direction: String? = null,

    @Column(name = "distance_meters", precision = 14, scale = 3)
    val distanceMeters: BigDecimal? = null,

    @Column(name = "ascent_meters", precision = 14, scale = 3)
    val ascentMeters: BigDecimal? = null,

    @Column(name = "descent_meters", precision = 14, scale = 3)
    val descentMeters: BigDecimal? = null,

    @Column(name = "max_elevation_meters", precision = 14, scale = 3)
    val maxElevationMeters: BigDecimal? = null,

    @Column(name = "suggested_days")
    val suggestedDays: Int? = null,

    @Column(name = "tags_json", columnDefinition = "LONGTEXT")
    val tagsJson: String? = null,

    @Column(columnDefinition = "TEXT")
    val introduction: String? = null,

    @Column(name = "professional_analysis_json", columnDefinition = "LONGTEXT")
    val professionalAnalysisJson: String? = null,

    @Column(name = "reference_days_json", columnDefinition = "LONGTEXT")
    val referenceDaysJson: String? = null,

    @Column(name = "seasonal_weather_json", columnDefinition = "LONGTEXT")
    val seasonalWeatherJson: String? = null,

    @Column(name = "seasonal_equipment_recommendations_json", columnDefinition = "LONGTEXT")
    val seasonalEquipmentRecommendationsJson: String? = null,

    @Column(name = "main_track_availability", nullable = false, length = 32)
    val mainTrackAvailability: String,

    @Column(name = "main_track_reference_system", length = 64)
    val mainTrackReferenceSystem: String? = null,

    @Column(name = "main_track_json", columnDefinition = "LONGTEXT")
    val mainTrackJson: String? = null,

    @Column(name = "created_at", nullable = false, updatable = false)
    val createdAt: Instant = Instant.now()
)
