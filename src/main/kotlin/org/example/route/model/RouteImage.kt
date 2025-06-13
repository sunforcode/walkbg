package org.example.route.model

import jakarta.persistence.*

/**
 * 路线图片实体
 */
@Entity
@Table(
    name = "route_images",
    indexes = [
        Index(name = "idx_route_images_route_id", columnList = "route_id"),
        Index(name = "idx_route_images_is_cover", columnList = "is_cover")
    ]
)
data class RouteImage(
    @Id
    @Column(length = 64)
    val id: String,

    @Column(name = "image_url", nullable = false, length = 500)
    val imageUrl: String,

    @Column(name = "is_cover", nullable = false)
    val isCover: Boolean = false,

    @Column(name = "sequence_number", nullable = false)
    val sequenceNumber: Int,

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "route_id")
    var route: Route? = null
) {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as RouteImage

        return id == other.id
    }

    override fun hashCode(): Int {
        return id.hashCode()
    }

    override fun toString(): String {
        return "RouteImage(id='$id', imageUrl='$imageUrl', isCover=$isCover)"
    }
}
