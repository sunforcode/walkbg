package org.example.trip.model

import com.fasterxml.jackson.annotation.JsonIgnore
import jakarta.persistence.*
import java.time.Instant
import org.example.user.model.User

/**
 * 行程参与者关联表
 */
@Entity
@Table(
    name = "trip_participants",
    indexes = [
        Index(name = "idx_trip_participants_trip_id", columnList = "trip_id"),
        Index(name = "idx_trip_participants_user_id", columnList = "user_id")
    ]
)
@IdClass(TripParticipantId::class)
data class TripParticipant(
    @Id
    @Column(name = "trip_id", length = 64)
    val tripId: String,

    @Id
    @Column(name = "user_id", length = 64)
    val userId: String,

    @Column(nullable = false)
    var role: Int = 0, // 0: 普通成员, 1: 组织者, 2: 副组织者

    @Column(nullable = false)
    var status: Int = 0, // 0: 待确认, 1: 已确认, 2: 已拒绝, 3: 已退出

    @Column(name = "joined_at", nullable = false)
    val joinedAt: Instant = Instant.now(),

    @JsonIgnore
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "trip_id", insertable = false, updatable = false)
    var trip: Trip? = null,

    @JsonIgnore
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", insertable = false, updatable = false)
    var user: User? = null
) {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as TripParticipant

        return tripId == other.tripId && userId == other.userId
    }

    override fun hashCode(): Int {
        return tripId.hashCode() * 31 + userId.hashCode()
    }

    override fun toString(): String {
        return "TripParticipant(tripId='$tripId', userId='$userId')"
    }
}

/**
 * 复合主键类
 */
data class TripParticipantId(
    val tripId: String = "",
    val userId: String = ""
) : java.io.Serializable {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as TripParticipantId

        return tripId == other.tripId && userId == other.userId
    }

    override fun hashCode(): Int {
        return tripId.hashCode() * 31 + userId.hashCode()
    }
}