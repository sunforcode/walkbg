package org.example.trip.model


import jakarta.persistence.*
import java.time.Instant
import org.example.route.model.Contact

/**
 * 行程联系人关联表
 */
@Entity
@Table(
    name = "trip_contacts",
    indexes = [
        Index(name = "idx_trip_contacts_trip_id", columnList = "trip_id"),
        Index(name = "idx_trip_contacts_contact_id", columnList = "contact_id"),
        Index(name = "idx_trip_contacts_contact_type", columnList = "contact_type")
    ]
)
data class TripContact(
    @Id
    @Column(length = 64)
    val id: String,

    @Column(name = "trip_id", length = 64, nullable = false)
    val tripId: String,

    @Column(name = "contact_id", length = 64, nullable = false)
    val contactId: String,

    @Column(name = "contact_type", nullable = false)
    var contactType: Int = 0, // 0: 向导, 1: 接送服务, 2: 住宿联系人, 3: 紧急联系人, 4: 其他

    @Column(name = "priority", nullable = false)
    var priority: Int = 0, // 优先级，数字越小优先级越高

    @Column(columnDefinition = "TEXT")
    var notes: String? = null, // 备注信息

    @Column(name = "is_active", nullable = false)
    var isActive: Boolean = true, // 是否激活

    @Column(name = "created_at", nullable = false, updatable = false)
    val createdAt: Instant = Instant.now(),

    @Column(name = "updated_at", nullable = false)
    var updatedAt: Instant = Instant.now(),

    // 关联关系
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "trip_id", insertable = false, updatable = false)
    var trip: Trip? = null,

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "contact_id", insertable = false, updatable = false)
    var contact: Contact? = null
) {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as TripContact

        return id == other.id
    }

    override fun hashCode(): Int {
        return id.hashCode()
    }

    override fun toString(): String {
        return "TripContact(id='$id', tripId='$tripId', contactId='$contactId', contactType=$contactType)"
    }
}