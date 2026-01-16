package org.example.route.model

import jakarta.persistence.*
import java.time.Instant

/**
 * 路线联系人关联表
 */
@Entity
@Table(
    name = "route_contacts",
    indexes = [
        Index(name = "idx_route_contacts_route_id", columnList = "route_id"),
        Index(name = "idx_route_contacts_contact_id", columnList = "contact_id"),
        Index(name = "idx_route_contacts_contact_type", columnList = "contact_type")
    ]
)
data class RouteContact(
    @Id
    @Column(length = 64)
    val id: String,

    @Column(name = "route_id", length = 64, nullable = false)
    val routeId: String,

    @Column(name = "contact_id", length = 64, nullable = false)
    val contactId: String,

    @Column(name = "contact_type", nullable = false)
    var contactType: Int = 0, // 0: 向导, 1: 接送服务, 2: 住宿联系人, 3: 紧急联系人, 4: 其他

    @Column(name = "priority", nullable = false)
    var priority: Int = 0, // 优先级，数字越小优先级越高

    @Column(columnDefinition = "TEXT")
    var notes: String? = null, // 备注信息

    @Column(name = "created_at", nullable = false, updatable = false)
    val createdAt: Instant = Instant.now(),

    @Column(name = "updated_at", nullable = false)
    var updatedAt: Instant = Instant.now()
) {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as RouteContact

        return id == other.id
    }

    override fun hashCode(): Int {
        return id.hashCode()
    }

    override fun toString(): String {
        return "RouteContact(id='$id', routeId='$routeId', contactId='$contactId', contactType=$contactType)"
    }
}