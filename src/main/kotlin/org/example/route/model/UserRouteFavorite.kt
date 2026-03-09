package org.example.route.model

import jakarta.persistence.*
import java.time.Instant

/**
 * 用户路线收藏关系实体
 * 记录用户收藏的路线
 */
@Entity
@Table(
    name = "user_route_favorites",
    uniqueConstraints = [
        UniqueConstraint(columnNames = ["user_id", "route_id"], name = "uk_user_route_favorite")
    ],
    indexes = [
        Index(name = "idx_user_route_favorites_user_id", columnList = "user_id"),
        Index(name = "idx_user_route_favorites_route_id", columnList = "route_id"),
        Index(name = "idx_user_route_favorites_created_at", columnList = "created_at")
    ]
)
data class UserRouteFavorite(
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    val id: Long = 0,

    @Column(name = "user_id", length = 64, nullable = false)
    val userId: String,

    @Column(name = "route_id", length = 64, nullable = false)
    val routeId: String,

    @Column(name = "created_at", nullable = false, updatable = false)
    val createdAt: Instant = Instant.now()
) {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as UserRouteFavorite

        return id == other.id
    }

    override fun hashCode(): Int {
        return id.hashCode()
    }

    override fun toString(): String {
        return "UserRouteFavorite(id=$id, userId='$userId', routeId='$routeId')"
    }
}
