package org.example.equipment.model

import jakarta.persistence.*
import org.example.user.model.User
import org.example.user.model.UserEquipmentItem
import java.math.BigDecimal
import java.time.Instant

@Entity
@Table(
    name = "equipment_items",
    indexes = [
        Index(name = "idx_equipment_items_category", columnList = "category"),
        Index(name = "idx_equipment_items_created_by", columnList = "created_by")
    ]
)
data class EquipmentItem(
    @Id
    @Column(length = 64)
    val id: String,

    @Column(nullable = false, length = 200)
    var name: String,

    @Column(nullable = false)
    var category: Int, // 0: 住宿装备, 1: 饮食装备, 2: 保暖装备, 3: 背包装备, 4: 导航装备, 5: 照明装备, 6: 急救装备, 7: 工具装备, 8: 电子装备, 9: 个人护理, 10: 其他装备

    @Column(nullable = false, precision = 8, scale = 2)
    var weight: BigDecimal = BigDecimal.ZERO,

    @Column(name = "weight_unit", nullable = false)
    var weightUnit: Int = 0, // 0: 克, 1: 千克, 2: 磅, 3: 盎司

    @Column(nullable = false)
    var quantity: Int = 1,

    @Column(name = "created_by", length = 64)
    var createdBy: String? = null,

    @Column(name = "created_at", nullable = false, updatable = false)
    val createdAt: Instant = Instant.now(),

    @Column(name = "updated_at", nullable = false)
    var updatedAt: Instant = Instant.now()
) {
    /**
     * 注意：不再持有以下关联关系的集合引用
     * - equipmentListItems: 通过 EquipmentListItemRepository.findByEquipmentItemId(equipmentItemId) 查询
     * - userEquipmentItems: 通过 UserEquipmentItemRepository.findByEquipmentItemId(equipmentItemId) 查询
     */

    /**
     * 领域行为：更新重量
     */
    fun updateWeight(newWeight: BigDecimal, unit: Int) {
        require(unit in 0..3) { "重量单位必须在 0-3 之间" }
        this.weight = newWeight
        this.weightUnit = unit
        this.updatedAt = Instant.now()
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false
        
        other as EquipmentItem
        
        return id == other.id
    }
    
    override fun hashCode(): Int {
        return id.hashCode()
    }
    
    override fun toString(): String {
        return "EquipmentItem(id='$id', name='$name')"
    }
}
