package org.example.trip.model

import jakarta.persistence.*
import java.time.Instant
import org.example.route.model.Waypoint

/**
 * 行程安排实体（单向关联）
 */
@Entity
@Table(
    name = "trip_itinerary",
    indexes = [
        Index(name = "idx_trip_itinerary_trip_id", columnList = "trip_id"),
        Index(name = "idx_trip_itinerary_day_number", columnList = "day_number")
    ]
)
data class TripItinerary(
    @Id
    @Column(length = 64)
    val id: String,

    @Column(name = "trip_id", length = 64, nullable = false)
    val tripId: String,
    
    @Column(name = "day_number", nullable = false)
    val dayNumber: Int,
    
    @Column(nullable = false, length = 200)
    val title: String,
    
    @Column(columnDefinition = "TEXT")
    val description: String? = null,
    
    @Column(precision = 8)
    val distance: Double? = null,
    
    @Column(length = 50)
    val duration: String? = null,
    
    @Column(name = "elevation_gain", precision = 8)
    val elevationGain: Double? = null,
    
    @Column(name = "elevation_loss", precision = 8)
    val elevationLoss: Double? = null,
    
    @Column(length = 200)
    val accommodation: String? = null,

    @Column(name = "start_waypoint_id", length = 64)
    val startWaypointId: String? = null,

    @Column(name = "end_waypoint_id", length = 64)
    val endWaypointId: String? = null,
    
    @Column(name = "created_at", nullable = false, updatable = false)
    val createdAt: Instant = Instant.now(),
    
    @Column(name = "updated_at", nullable = false)
    var updatedAt: Instant = Instant.now()
) {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false
        
        other as TripItinerary
        
        return id == other.id
    }
    
    override fun hashCode(): Int {
        return id.hashCode()
    }
    
    override fun toString(): String {
        return "TripItinerary(id='$id', title='$title')"
    }
}