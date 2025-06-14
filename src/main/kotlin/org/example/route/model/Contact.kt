package org.example.route.model

import jakarta.persistence.*
import org.example.user.model.User
import java.math.BigDecimal
import java.time.Instant

/**
 * 联系人实体
 */
@Entity
@Table(
    name = "contacts",
    indexes = [
        Index(name = "idx_contacts_location", columnList = "location"),
        Index(name = "idx_contacts_is_verified", columnList = "is_verified"),
        Index(name = "idx_contacts_price", columnList = "price"),
        Index(name = "idx_contacts_created_by", columnList = "created_by"),
        Index(name = "idx_contacts_verified_by", columnList = "verified_by"),
        Index(name = "idx_contacts_route_id", columnList = "route_id")
    ]
)
data class Contact(
    @Id
    @Column(length = 64)
    val id: String,

    @Column(nullable = false, length = 100)
    var name: String,

    @Column(nullable = false, length = 20)
    var phone: String,

    @Column(columnDefinition = "TEXT")
    var description: String? = null,

    @Column(length = 200)
    var location: String? = null,

    @Column(precision = 10, scale = 2)
    var price: BigDecimal? = null,

    @Column(name = "is_verified", nullable = false)
    var isVerified: Boolean = false,

    @Column(name = "created_by", length = 64)
    var createdBy: String? = null,

    @Column(name = "verified_by", length = 64)
    var verifiedBy: String? = null,

    @Column(name = "updated_by", length = 64)
    var updatedBy: String? = null,

    @Column(name = "created_at", nullable = false, updatable = false)
    val createdAt: Instant = Instant.now(),

    @Column(name = "updated_at", nullable = false)
    var updatedAt: Instant = Instant.now(),

    // 关联关系
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "route_id")
    var route: Route? = null,

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "created_by", insertable = false, updatable = false)
    var creator: User? = null,

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "verified_by", insertable = false, updatable = false)
    var verifier: User? = null,

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "updated_by", insertable = false, updatable = false)
    var updater: User? = null
) {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as Contact

        return id == other.id
    }

    override fun hashCode(): Int {
        return id.hashCode()
    }

    override fun toString(): String {
        return "Contact(id='$id', name='$name', phone='$phone', location='$location')"
    }
}