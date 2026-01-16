package org.example.route.model

import jakarta.persistence.*
import java.time.Instant

/**
 * 路径点实体（单向关联）
 */
@Entity
@Table(
    name = "waypoints",
    indexes = [
        Index(name = "idx_waypoints_route_id", columnList = "route_id"),
        Index(name = "idx_waypoints_sequence", columnList = "sequence_number")
    ]
)
data class Waypoint(
    @Id
    @Column(length = 64)
    val id: String,

    @Column(name = "route_id", length = 64, nullable = false)
    val routeId: String,

    @Column(nullable = false, length = 200)
    val name: String,

    @Column(columnDefinition = "TEXT")
    val description: String? = null,

    @Column(nullable = false)
    val latitude: Double,

    @Column(nullable = false)
    val longitude: Double,

    val elevation: Double? = null,

    @Column(length = 50)
    val type: String? = null,

    @Column(name = "icon_url", length = 500)
    val iconUrl: String? = null,

    @Column(name = "image_url", length = 500)
    val imageUrl: String? = null,

    @Column(name = "sequence_number", nullable = false)
    val sequenceNumber: Int,

    @Column(name = "created_at", nullable = false, updatable = false)
    val createdAt: Instant = Instant.now(),

    @Column(name = "updated_at", nullable = false)
    var updatedAt: Instant = Instant.now()
) {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as Waypoint

        return id == other.id
    }

    override fun hashCode(): Int {
        return id.hashCode()
    }

    override fun toString(): String {
        return "Waypoint(id='$id', name='$name')"
    }
}
