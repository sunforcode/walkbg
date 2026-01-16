package org.example.trip.model

import jakarta.persistence.*

/**
 * 行程图片实体（单向关联）
 */
@Entity
@Table(
    name = "trip_images",
    indexes = [
        Index(name = "idx_trip_images_trip_id", columnList = "trip_id"),
        Index(name = "idx_trip_images_is_cover", columnList = "is_cover"),
        Index(name = "idx_trip_images_sequence", columnList = "sequence_number")
    ]
)
data class TripImage(
    @Id
    @Column(length = 64)
    val id: String,

    @Column(name = "trip_id", length = 64, nullable = false)
    val tripId: String,
    
    @Column(name = "image_url", nullable = false, length = 500)
    val imageUrl: String,
    
    @Column(name = "is_cover", nullable = false)
    val isCover: Boolean = false,
    
    @Column(name = "sequence_number", nullable = false)
    val sequenceNumber: Int
) {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false
        other as TripImage
        return id == other.id
    }

    override fun hashCode(): Int {
        return id.hashCode()
    }

    override fun toString(): String {
        return "TripImage(id='$id', tripId='$tripId', imageUrl='$imageUrl')"
    }
}
