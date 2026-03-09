package org.example.route.model

import jakarta.persistence.*
import java.time.Instant

/**
 * 用户路线完成记录实体
 * 记录用户完成的路线及其时间戳
 */
@Entity
@Table(
    name = "user_route_completions",
    indexes = [
        Index(name = "idx_user_route_completions_user_id", columnList = "user_id"),
        Index(name = "idx_user_route_completions_route_id", columnList = "route_id"),
        Index(name = "idx_user_route_completions_completed_at", columnList = "completed_at")
    ]
)
data class UserRouteCompletion(
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    val id: Long = 0,

    @Column(name = "user_id", length = 64, nullable = false)
    val userId: String,

    @Column(name = "route_id", length = 64, nullable = false)
    val routeId: String,

    @Column(name = "completed_at", nullable = false)
    val completedAt: Instant = Instant.now(),

    @Column(name = "duration_minutes", nullable = true)
    val durationMinutes: Int? = null,

    @Column(columnDefinition = "TEXT", nullable = true)
    val notes: String? = null,

    @Column(name = "created_at", nullable = false, updatable = false)
    val createdAt: Instant = Instant.now(),

    @Column(name = "updated_at", nullable = false)
    var updatedAt: Instant = Instant.now()
) {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as UserRouteCompletion

        return id == other.id
    }

    override fun hashCode(): Int {
        return id.hashCode()
    }

    override fun toString(): String {
        return "UserRouteCompletion(id=$id, userId='$userId', routeId='$routeId', completedAt=$completedAt)"
    }
}
