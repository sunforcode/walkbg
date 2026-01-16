package org.example.user.model

import com.fasterxml.jackson.annotation.JsonIgnore
import jakarta.persistence.*
import java.time.Instant
import org.example.route.model.Route

/**
 * 用户完成路线关联表
 */
@Entity
@Table(
    name = "user_completed_routes",
    indexes = [
        Index(name = "idx_user_completed_routes_user_id", columnList = "user_id"),
        Index(name = "idx_user_completed_routes_route_id", columnList = "route_id"),
        Index(name = "idx_user_completed_routes_completed_at", columnList = "completed_at")
    ]
)
@IdClass(UserCompletedRouteId::class)
data class UserCompletedRoute(
    @Id
    @Column(name = "user_id", length = 64, nullable = false)
    val userId: String,

    @Id
    @Column(name = "route_id", length = 64, nullable = false)
    val routeId: String,

    @Column(name = "completed_at", nullable = false)
    val completedAt: Instant = Instant.now(),

    @JsonIgnore
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", insertable = false, updatable = false, foreignKey = ForeignKey(name = "fk_user_completed_routes_user_id"))
    var user: User? = null,

    @JsonIgnore
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "route_id", insertable = false, updatable = false, foreignKey = ForeignKey(name = "fk_user_completed_routes_route_id"))
    var route: Route? = null
) {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as UserCompletedRoute

        return userId == other.userId && routeId == other.routeId
    }

    override fun hashCode(): Int {
        return userId.hashCode() * 31 + routeId.hashCode()
    }

    override fun toString(): String {
        return "UserCompletedRoute(userId='$userId', routeId='$routeId')"
    }
}

/**
 * 复合主键类
 */
data class UserCompletedRouteId(
    val userId: String = "",
    val routeId: String = ""
) : java.io.Serializable {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as UserCompletedRouteId

        return userId == other.userId && routeId == other.routeId
    }

    override fun hashCode(): Int {
        return userId.hashCode() * 31 + routeId.hashCode()
    }
}