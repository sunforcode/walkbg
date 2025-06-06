package org.example.model

import jakarta.persistence.*

/**
 * 用户装备物品关联表（用户装备库存）
 */
@Entity
@Table(
    name = "user_equipment_items",
    indexes = [
        Index(name = "idx_user_equipment_items_user_id", columnList = "user_id"),
        Index(name = "idx_user_equipment_items_item_id", columnList = "equipment_item_id")
    ]
)
@IdClass(UserEquipmentItemId::class)
data class UserEquipmentItem(
    @Id
    @Column(name = "user_id", length = 64)
    val userId: String,

    @Id
    @Column(name = "equipment_item_id", length = 64)
    val equipmentItemId: String,

    @Column(nullable = false)
    var quantity: Int = 1,

    @Column(columnDefinition = "TEXT")
    var notes: String? = null,

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", insertable = false, updatable = false)
    var user: User? = null,

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "equipment_item_id", insertable = false, updatable = false)
    var equipmentItem: EquipmentItem? = null
) {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as UserEquipmentItem

        return userId == other.userId && equipmentItemId == other.equipmentItemId
    }

    override fun hashCode(): Int {
        return userId.hashCode() * 31 + equipmentItemId.hashCode()
    }

    override fun toString(): String {
        return "UserEquipmentItem(userId='$userId', equipmentItemId='$equipmentItemId')"
    }
}

/**
 * 复合主键类
 */
data class UserEquipmentItemId(
    val userId: String = "",
    val equipmentItemId: String = ""
) : java.io.Serializable {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as UserEquipmentItemId

        return userId == other.userId && equipmentItemId == other.equipmentItemId
    }

    override fun hashCode(): Int {
        return userId.hashCode() * 31 + equipmentItemId.hashCode()
    }
}