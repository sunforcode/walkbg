package org.example.route.model

import jakarta.persistence.*

/**
 * 路线设施信息（单向关联）
 */
@Entity
@Table(
    name = "route_facilities",
    indexes = [
        Index(name = "idx_route_facilities_route_id", columnList = "route_id", unique = true)
    ]
)
data class RouteFacilities(
    @Id
    @Column(length = 64)
    val id: String,

    @Column(name = "route_id", length = 64, nullable = false, unique = true)
    val routeId: String,

    @Column(columnDefinition = "TEXT")
    val water: String? = null,

    @Column(columnDefinition = "TEXT")
    val food: String? = null,

    @Column(columnDefinition = "TEXT")
    val accommodation: String? = null,

    @Column(columnDefinition = "TEXT")
    val toilets: String? = null,

    @Column(name = "signal_coverage", columnDefinition = "TEXT")
    val signalCoverage: String? = null
) {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false
        other as RouteFacilities
        return id == other.id
    }

    override fun hashCode(): Int {
        return id.hashCode()
    }

    override fun toString(): String {
        return "RouteFacilities(id='$id', routeId='$routeId')"
    }
}
