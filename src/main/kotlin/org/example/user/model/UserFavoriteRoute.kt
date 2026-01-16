package org.example.user.model

import jakarta.persistence.*

import java.time.Instant
import org.example.route.model.Route

/**
 * 用户收藏路线关联表
 */
@Entity
@Table(
    name = "user_favorite_routes",
    uniqueConstraints = [
        UniqueConstraint(columnNames = ["user_id", "route_id"])
    ],
    indexes = [
        Index(name = "idx_user_favorite_routes_user_id", columnList = "user_id"),
        Index(name = "idx_user_favorite_routes_route_id", columnList = "route_id")
    ]
)
data class UserFavoriteRoute(
    @Id
    @Column(length = 64)
    val id: String,

    @Column(name = "user_id", length = 64, nullable = false)
    val userId: String,

    @Column(name = "route_id", length = 64, nullable = false)
    val routeId: String,

    @Column(name = "created_at", nullable = false, updatable = false)
    val createdAt: Instant = Instant.now(),

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", insertable = false, updatable = false, foreignKey = ForeignKey(name = "fk_user_favorite_routes_user_id"))
    var user: User? = null,

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "route_id", insertable = false, updatable = false, foreignKey = ForeignKey(name = "fk_user_favorite_routes_route_id"))
    var route: Route? = null
) {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as UserFavoriteRoute

        return id == other.id
    }

    override fun hashCode(): Int {
        return id.hashCode()
    }

    override fun toString(): String {
        return "UserFavoriteRoute(id='$id', userId='${user?.id}', routeId='${route?.id}')"
    }
}
