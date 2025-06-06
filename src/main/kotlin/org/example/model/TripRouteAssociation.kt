package org.example.model

import jakarta.persistence.*

/**
 * 行程路线关联表
 */
@Entity
@Table(
    name = "trip_routes",
    indexes = [
        Index(name = "idx_trip_routes_trip_id", columnList = "trip_id"),
        Index(name = "idx_trip_routes_route_id", columnList = "route_id")
    ]
)
@IdClass(TripRouteId::class)
data class TripRouteAssociation(
    @Id
    @Column(name = "trip_id", length = 64)
    val tripId: String,

    @Id
    @Column(name = "route_id", length = 64)
    val routeId: String,

    @Column(name = "is_primary", nullable = false)
    var isPrimary: Boolean = false,

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "trip_id", insertable = false, updatable = false)
    var trip: Trip? = null,

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "route_id", insertable = false, updatable = false)
    var route: Route? = null
) {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as TripRouteAssociation

        return tripId == other.tripId && routeId == other.routeId
    }

    override fun hashCode(): Int {
        return tripId.hashCode() * 31 + routeId.hashCode()
    }

    override fun toString(): String {
        return "TripRouteAssociation(tripId='$tripId', routeId='$routeId')"
    }
}

/**
 * 复合主键类
 */
data class TripRouteId(
    val tripId: String = "",
    val routeId: String = ""
) : java.io.Serializable {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as TripRouteId

        return tripId == other.tripId && routeId == other.routeId
    }

    override fun hashCode(): Int {
        return tripId.hashCode() * 31 + routeId.hashCode()
    }
}