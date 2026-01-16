package org.example.equipment.model

import jakarta.persistence.*
import org.example.user.model.User
import org.example.trip.model.Trip
import java.math.BigDecimal
import java.time.Instant

@Entity
@Table(
    name = "equipment_lists",
    indexes = [
        Index(name = "idx_equipment_lists_trip_id", columnList = "trip_id"),
        Index(name = "idx_equipment_lists_creator_id", columnList = "creator_id"),
        Index(name = "idx_equipment_lists_type", columnList = "type")
    ]
)
data class EquipmentList(
    @Id
    @Column(length = 64)
    val id: String,

    @Column(nullable = false, length = 200)
    var name: String,
    
    @Column(nullable = false)
    var type: Int, // 0: 个人装备, 1: 团队装备, 2: 模板装备
    
    @Column(name = "trip_id", length = 64)
    var tripId: String? = null,
    
    @Column(name = "creator_id", length = 64)
    var creatorId: String? = null,
    
    @Column(name = "total_weight", nullable = false, precision = 8, scale = 2)
    var totalWeight: BigDecimal = BigDecimal.ZERO,

    @Column(name = "person_count", nullable = false)
    var personCount: Int = 1,
    
    @Column(nullable = false)
    var status: Int = 0, // 0: 规划中, 1: 准备中, 2: 已完成
    
    @Column(name = "created_at", nullable = false, updatable = false)
    val createdAt: Instant = Instant.now(),
    
    @Column(name = "updated_at", nullable = false)
    var updatedAt: Instant = Instant.now()
) {
    /**
     * 注意：不再持有以下关联关系的集合引用
     * - equipmentListItems: 通过 EquipmentListItemRepository.findByEquipmentListId(equipmentListId) 查询
     * 
     * 优势：
     * 1. 避免 N+1 查询问题
     * 2. 减少内存占用
     * 3. 避免序列化死循环
     * 4. 提高查询灵活性（按需加载）
     */

    /**
     * 领域行为：更新总重量
     */
    fun updateTotalWeight(newWeight: BigDecimal) {
        this.totalWeight = newWeight
        this.updatedAt = Instant.now()
    }

    /**
     * 领域行为：更新状态
     */
    fun updateStatus(newStatus: Int) {
        require(newStatus in 0..2) { "状态值必须在 0-2 之间" }
        this.status = newStatus
        this.updatedAt = Instant.now()
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as EquipmentList

        return id == other.id
    }

    override fun hashCode(): Int {
        return id.hashCode()
    }

    override fun toString(): String {
        return "EquipmentList(id='$id', name='$name')"
    }
}

enum class EquipmentListType {
    SHORT_HIKE, LONG_HIKE, CAMPING, MOUNTAINEERING, TREKKING, CUSTOM
}
enum class EquipmentListStatus {
    PLANNING, PREPARING, READY, IN_USE, COMPLETED, ARCHIVED
}
