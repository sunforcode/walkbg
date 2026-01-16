package org.example.route.model

import jakarta.persistence.*

/**
 * 路线评分实体（单向关联）
 */
@Entity
@Table(
    name = "route_ratings",
    indexes = [
        Index(name = "idx_route_ratings_route_id", columnList = "route_id", unique = true),
        Index(name = "idx_route_ratings_overall", columnList = "overall")
    ]
)
data class RouteRating(
    @Id
    @Column(length = 64)
    val id: String,

    @Column(name = "route_id", length = 64, nullable = false, unique = true)
    val routeId: String,
    
    val overall: Double? = null,
    val scenery: Double? = null,
    val difficulty: Double? = null,
    val experience: Double? = null,
    val facilities: Double? = null,

    @Column(name = "rating_count", nullable = false)
    val ratingCount: Int = 0
) {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false
        other as RouteRating
        return id == other.id
    }

    override fun hashCode(): Int {
        return id.hashCode()
    }

    override fun toString(): String {
        return "RouteRating(id='$id', routeId='$routeId', overall=$overall, ratingCount=$ratingCount)"
    }
}
