package org.example.model

import com.fasterxml.jackson.annotation.JsonIgnore
import jakarta.persistence.*
import java.time.Instant

/**
 * 用户收藏路线关联表
 */
@Entity
@Table(
    name = "user_favorite_routes",
    indexes = [
        Index(name = "idx_user_favorite_routes_user_id", columnList = "user_id"),
        Index(name = "idx_user_favorite_routes_route_id", columnList = "route_id"),
        Index(name = "idx_user_favorite_routes_favorited_at", columnList = "favorited_at")
    ]
)
@IdClass(UserFavoriteRouteId::class)
data class UserFavoriteRoute(
    @Id
    @Column(name = "user_id", length = 64)
    val userId: String,

    @Id
    @Column(name = "route_id", length = 64)
    val routeId: String,

    @Column(name = "favorited_at", nullable = false)
    val favoritedAt: Instant = Instant.now(),

    @JsonIgnore
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", insertable = false, updatable = false)
    var user: User? = null,

    @JsonIgnore
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "route_id", insertable = false, updatable = false)
    var route: Route? = null
) {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as UserFavoriteRoute

        return userId == other.userId && routeId == other.routeId
    }

    override fun hashCode(): Int {
        return userId.hashCode() * 31 + routeId.hashCode()
    }

    override fun toString(): String {
        return "UserFavoriteRoute(userId='$userId', routeId='$routeId')"
    }
}

/**
 * 复合主键类
 */
data class UserFavoriteRouteId(
    val userId: String = "",
    val routeId: String = ""
) : java.io.Serializable {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as UserFavoriteRouteId

        return userId == other.userId && routeId == other.routeId
    }

    override fun hashCode(): Int {
        return userId.hashCode() * 31 + routeId.hashCode()
    }
}