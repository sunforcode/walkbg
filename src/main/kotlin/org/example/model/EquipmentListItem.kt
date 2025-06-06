package org.example.model

import jakarta.persistence.*

/**
 * 装备清单物品关联表
 */
@Entity
@Table(
    name = "equipment_list_items",
    indexes = [
        Index(name = "idx_equipment_list_items_list_id", columnList = "equipment_list_id"),
        Index(name = "idx_equipment_list_items_item_id", columnList = "equipment_item_id")
    ]
)
@IdClass(EquipmentListItemId::class)
data class EquipmentListItem(
    @Id
    @Column(name = "equipment_list_id", length = 64)
    val equipmentListId: String,

    @Id
    @Column(name = "equipment_item_id", length = 64)
    val equipmentItemId: String,

    @Column(nullable = false)
    var quantity: Int = 1,

    @Column(columnDefinition = "TEXT")
    var notes: String? = null,

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "equipment_list_id", insertable = false, updatable = false)
    var equipmentList: EquipmentList? = null,

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "equipment_item_id", insertable = false, updatable = false)
    var equipmentItem: EquipmentItem? = null
) {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as EquipmentListItem

        return equipmentListId == other.equipmentListId && equipmentItemId == other.equipmentItemId
    }

    override fun hashCode(): Int {
        return equipmentListId.hashCode() * 31 + equipmentItemId.hashCode()
    }

    override fun toString(): String {
        return "EquipmentListItem(equipmentListId='$equipmentListId', equipmentItemId='$equipmentItemId')"
    }
}

/**
 * 复合主键类
 */
data class EquipmentListItemId(
    val equipmentListId: String = "",
    val equipmentItemId: String = ""
) : java.io.Serializable {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as EquipmentListItemId

        return equipmentListId == other.equipmentListId && equipmentItemId == other.equipmentItemId
    }

    override fun hashCode(): Int {
        return equipmentListId.hashCode() * 31 + equipmentItemId.hashCode()
    }
}