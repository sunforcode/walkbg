package org.example.route.model

import com.fasterxml.jackson.annotation.JsonProperty
import jakarta.persistence.*
import java.time.Instant

/**
 * 搭车联系人实体（单向关联）
 */
@Entity
@Table(
    name = "hitchhike_contacts",
    indexes = [
        Index(name = "idx_hitchhike_contacts_route_id", columnList = "route_id"),
        Index(name = "idx_hitchhike_contacts_location", columnList = "location"),
        Index(name = "idx_hitchhike_contacts_created_by", columnList = "created_by")
    ]
)
data class HitchhikeContact(
    @Id
    @Column(length = 64)
    val id: String,

    @Column(name = "route_id", length = 64, nullable = false)
    val routeId: String,

    @Column(nullable = false, length = 100)
    val name: String,

    @Column(nullable = false, length = 20)
    val phone: String,

    @Column(columnDefinition = "TEXT")
    val description: String? = null,

    @Column(length = 200)
    val location: String? = null,

    val price: Double? = null,

    @Column(name = "last_verified", nullable = false)
    val lastVerified: Boolean = false,

    @Column(name = "created_by", length = 64)
    var createdBy: String? = null,

    @Column(name = "created_at", nullable = false, updatable = false)
    val createdAt: Instant = Instant.now(),

    @Column(name = "updated_at", nullable = false)
    var updatedAt: Instant = Instant.now()
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
