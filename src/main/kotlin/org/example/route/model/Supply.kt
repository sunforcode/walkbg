package org.example.route.model

import jakarta.persistence.*
import org.example.user.model.User
import java.math.BigDecimal
import java.time.Instant

/**
 * 补给实体
 */
@Entity
@Table(
    name = "supplies",
    indexes = [
        Index(name = "idx_supplies_route_id", columnList = "route_id"),
        Index(name = "idx_supplies_supply_type", columnList = "supply_type"),
        Index(name = "idx_supplies_elevation", columnList = "elevation"),
        Index(name = "idx_supplies_last_verified", columnList = "last_verified")
    ]
)
data class Supply(
    @Id
    @Column(length = 64)
    val id: String,

    @Column(nullable = false, length = 200)
    var name: String,

    @Column(columnDefinition = "TEXT")
    var description: String? = null,

    @Column(precision = 10, scale = 6)
    var latitude: BigDecimal? = null,

    @Column(precision = 10, scale = 6)
    var longitude: BigDecimal? = null,

    @Column(precision = 8, scale = 2)
    var elevation: BigDecimal? = null,

    @Column(name = "supply_type")
    var supplyType: Int? = null, // 0: 商店, 1: 餐厅, 2: 自动售货机, 3: 紧急补给点, 4: 其他

    @Column(name = "last_verified", length = 64)
    var lastVerified: String? = null, // 最后验证者的用户ID

    @Column(name = "last_verified_at")
    var lastVerifiedAt: Instant? = null, // 最后验证时间

    @Column(name = "updated_by", length = 64)
    var updatedBy: String? = null,

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "updated_by", insertable = false, updatable = false)
    var updatedByUser: User? = null,

    @Column(name = "created_at", nullable = false, updatable = false)
    val createdAt: Instant = Instant.now(),

    @Column(name = "updated_at", nullable = false)
    var updatedAt: Instant = Instant.now(),

    // 关联关系
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "route_id")
    var route: Route? = null
) {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as Supply

        return id == other.id
    }

    override fun hashCode(): Int {
        return id.hashCode()
    }

    override fun toString(): String {
        return "Supply(id='$id', name='$name', supplyType='$supplyType', elevation=$elevation)"
    }
}