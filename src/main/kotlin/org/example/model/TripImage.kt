package org.example.model

import jakarta.persistence.*

/**
 * 行程图片实体
 */
@Entity
@Table(name = "trip_images")
data class TripImage(
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    val id: Long = 0,
    
    @Column(name = "image_url", nullable = false, length = 500)
    val imageUrl: String,
    
    @Column(name = "is_cover", nullable = false)
    val isCover: Boolean = false,
    
    @Column(name = "sequence_number", nullable = false)
    val sequenceNumber: Int,
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "trip_id", nullable = false)
    var trip: Trip? = null
)