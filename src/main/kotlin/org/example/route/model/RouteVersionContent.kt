package org.example.route.model

import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.Id
import jakarta.persistence.Table
import java.math.BigDecimal

@Entity
@Table(name = "route_version_images")
data class RouteVersionImage(
    @Id
    @Column(length = 64)
    val id: String,

    @Column(name = "route_version_id", nullable = false, length = 64)
    val routeVersionId: String,

    @Column(name = "media_reference", nullable = false, length = 500)
    val mediaReference: String,

    @Column(nullable = false, length = 32)
    val role: String,

    @Column(name = "display_order", nullable = false)
    val displayOrder: Int,

    @Column(length = 500)
    val caption: String? = null
)

@Entity
@Table(name = "route_version_segments")
data class RouteVersionSegment(
    @Id
    @Column(length = 64)
    val id: String,

    @Column(name = "route_version_id", nullable = false, length = 64)
    val routeVersionId: String,

    @Column(name = "segment_order", nullable = false)
    val segmentOrder: Int,

    @Column(nullable = false, length = 200)
    val name: String,

    @Column(name = "start_name", length = 200)
    val startName: String? = null,

    @Column(name = "end_name", length = 200)
    val endName: String? = null,

    @Column(name = "distance_meters", precision = 14, scale = 3)
    val distanceMeters: BigDecimal? = null,

    @Column(name = "estimated_duration_seconds")
    val estimatedDurationSeconds: Long? = null,

    @Column(name = "ascent_meters", precision = 14, scale = 3)
    val ascentMeters: BigDecimal? = null,

    @Column(name = "descent_meters", precision = 14, scale = 3)
    val descentMeters: BigDecimal? = null,

    @Column(length = 100)
    val difficulty: String? = null,

    @Column(name = "terrain_or_road_type", length = 100)
    val terrainOrRoadType: String? = null,

    @Column(columnDefinition = "TEXT")
    val description: String? = null,

    @Column(columnDefinition = "TEXT")
    val notes: String? = null,

    @Column(name = "main_track_range_json", columnDefinition = "TEXT")
    val mainTrackRangeJson: String? = null
)

@Entity
@Table(name = "route_version_points")
data class RouteVersionPoint(
    @Id
    @Column(length = 64)
    val id: String,

    @Column(name = "route_version_id", nullable = false, length = 64)
    val routeVersionId: String,

    @Column(name = "point_kind", nullable = false, length = 32)
    val pointKind: String,

    @Column(name = "display_order", nullable = false)
    val displayOrder: Int,

    @Column(nullable = false, length = 200)
    val name: String,

    @Column(length = 100)
    val category: String? = null,

    @Column(name = "sub_category", length = 100)
    val subCategory: String? = null,

    @Column(columnDefinition = "TEXT")
    val description: String? = null,

    @Column(nullable = false)
    val latitude: Double,

    @Column(nullable = false)
    val longitude: Double,

    val elevation: Double? = null,

    @Column(name = "reference_system", nullable = false, length = 64)
    val referenceSystem: String
)
