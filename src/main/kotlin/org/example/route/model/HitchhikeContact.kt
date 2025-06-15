package org.example.route.model

import com.fasterxml.jackson.annotation.JsonProperty
import jakarta.persistence.*
import java.time.Instant

/**
 * 搭车联系人实体
 */
@Entity
@Table(name = "hitchhike_contacts")
data class HitchhikeContact(
    @Id
    @Column(length = 64)
    val id: String,

    @Column(nullable = false)
    val name: String,

    @Column(nullable = false)
    val phone: String,

    @Column(columnDefinition = "TEXT")
    val description: String? = null,

    val location: String? = null,

    val price: Double? = null,

    @Column(name = "last_verified", nullable = false)
    val lastVerified: Boolean = false,

    @Column(name = "created_at", nullable = false, updatable = false)
    val createdAt: Instant = Instant.now(),

    @Column(name = "updated_at", nullable = false)
    var updatedAt: Instant = Instant.now(),

    @Column(name = "created_by", length = 64)
    var createdBy: String? = null,

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "route_id")
    var route: Route? = null,

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "created_by", insertable = false, updatable = false)
    var creator: org.example.user.model.User? = null
) {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as HitchhikeContact

        return id == other.id
    }

    override fun hashCode(): Int {
        return id.hashCode()
    }

    override fun toString(): String {
        return "HitchhikeContact(id='$id', name='$name', phone='$phone')"
    }
}
