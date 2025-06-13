
package org.example.user.model

import com.fasterxml.jackson.annotation.JsonIgnore
import jakarta.persistence.*
import java.time.Instant
import org.example.trip.model.Trip
import org.example.trip.model.TripParticipant
import org.example.route.model.Route

/**
 * 用户模型
 */
@Entity
@Table(
    name = "users",
    indexes = [
        Index(name = "idx_users_email", columnList = "email", unique = true),
        Index(name = "idx_users_username", columnList = "username", unique = true)
    ]
)
data class User(
    @Id
    @Column(length = 64)
    val id: String,

    @Column(nullable = false, unique = true, length = 50)
    var username: String = "",

    @Column(nullable = false, length = 50)
    var nickname: String = "",

    @Column(nullable = false, unique = true, length = 100)
    var email: String = "",

    @Column(name = "avatar_url", length = 500)
    var avatarUrl: String? = null,

    @Column(name = "phone", length = 20)
    var phone: String? = null,

    @Column(name = "bio", columnDefinition = "TEXT")
    var bio: String? = null,

    @Column(name = "is_active", nullable = false)
    var isActive: Boolean = true,

    @Column(name = "created_at", nullable = false, updatable = false)
    val createdAt: Instant = Instant.now(),

    @Column(name = "last_login_at")
    var lastLoginAt: Instant? = null,

    @Column(name = "updated_at", nullable = false)
    var updatedAt: Instant = Instant.now()
) {

    // 关联关系
    @JsonIgnore
    @OneToMany(mappedBy = "organizer", cascade = [CascadeType.ALL], fetch = FetchType.LAZY)
    val organizedTrips: MutableList<Trip> = mutableListOf()

    @JsonIgnore
    @OneToMany(mappedBy = "creator", cascade = [CascadeType.ALL], fetch = FetchType.LAZY)
    val createdRoutes: MutableList<Route> = mutableListOf()

    @JsonIgnore
    @OneToMany(mappedBy = "user", cascade = [CascadeType.ALL], fetch = FetchType.LAZY)
    val tripParticipations: MutableList<TripParticipant> = mutableListOf()

    @JsonIgnore
    @OneToMany(mappedBy = "user", cascade = [CascadeType.ALL], fetch = FetchType.LAZY)
    val favoriteRoutes: MutableList<UserFavoriteRoute> = mutableListOf()

    @JsonIgnore
    @OneToMany(mappedBy = "user", cascade = [CascadeType.ALL], fetch = FetchType.LAZY)
    val completedRoutes: MutableList<UserCompletedRoute> = mutableListOf()

    @JsonIgnore
    @OneToMany(mappedBy = "user", cascade = [CascadeType.ALL], fetch = FetchType.LAZY)
    val equipmentItems: MutableList<UserEquipmentItem> = mutableListOf()

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as User

        return id == other.id
    }

    override fun hashCode(): Int {
        return id.hashCode()
    }

    override fun toString(): String {
        return "User(id='$id', username='$username')"
    }
}
