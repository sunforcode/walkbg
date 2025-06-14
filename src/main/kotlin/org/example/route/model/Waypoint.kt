package org.example.route.model

import jakarta.persistence.*
import java.time.Instant

/**
 * 路径点实体
 */
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

    @Column(name = "icon_url")
    val iconUrl: String? = null,

    @Column(name = "image_url")
    val imageUrl: String? = null,

    @Column(name = "sequence_number")
    val sequenceNumber: Int,

    @Column(name = "created_at", nullable = false, updatable = false)
    val createdAt: Instant = Instant.now(),

    @Column(name = "updated_at", nullable = false)
    var updatedAt: Instant = Instant.now(),

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "route_id")
    var route: Route? = null
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
