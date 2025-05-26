package org.example.model

import jakarta.persistence.*
import java.time.Instant

@Entity
@Table(name = "em_equipment_lists")
data class EMEquipmentList(
    @Id
    val id: String,
    
    @Column(nullable = false)
    var name: String,
    
    @Column(columnDefinition = "TEXT")
    var description: String? = null,
    
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    var type: EMEquipmentListType,
    
    var routeId: String? = null,
    
    var routeName: String? = null,
    
    var tripId: String? = null,
    
    var tripDays: Int = 1,
    
    var personCount: Int = 1,
    
    @Column(nullable = false)
    var totalWeight: Double = 0.0,
    
    var baseWeight: Double = 0.0,
    
    var consumableWeight: Double = 0.0,
    
    var wornWeight: Double = 0.0,
    
    var creatorId: String? = null,
    
    var creatorName: String? = null,
    
    var isOfficial: Boolean = false,
    
    var isTemplate: Boolean = false,
    
    var templateId: String? = null,
    
    @Enumerated(EnumType.STRING)
    var status: EMEquipmentListStatus = EMEquipmentListStatus.PLANNING,
    
    var lastUsedAt: Instant? = null,
    
    @Column(nullable = false)
    val createdAt: Instant = Instant.now(),
    
    @Column(nullable = false)
    var updatedAt: Instant = Instant.now(),
    
    @OneToMany(mappedBy = "equipmentList", cascade = [CascadeType.ALL], orphanRemoval = true)
    val equipmentItems: MutableList<EMEquipmentItem> = mutableListOf(),
    
    @OneToMany(mappedBy = "equipmentList", cascade = [CascadeType.ALL], orphanRemoval = true)
    val seasons: MutableList<EMEquipmentListSeason> = mutableListOf(),
    
    @OneToMany(mappedBy = "equipmentList", cascade = [CascadeType.ALL], orphanRemoval = true)
    val tags: MutableList<EMEquipmentListTag> = mutableListOf()
) {
    // 添加关联实体的辅助方法
    fun addEquipmentItem(item: EMEquipmentItem) {
        equipmentItems.add(item)
        item.equipmentList = this
        recalculateWeights()
    }
    
    fun removeEquipmentItem(item: EMEquipmentItem) {
        equipmentItems.remove(item)
        recalculateWeights()
    }
    
    fun addSeason(season: EMSeasonSuitability) {
        seasons.add(EMEquipmentListSeason(equipmentList = this, season = season))
    }
    
    fun addTag(tag: String) {
        tags.add(EMEquipmentListTag(equipmentList = this, tag = tag))
    }
    
    private fun recalculateWeights() {
        var base = 0.0
        var consumable = 0.0
        var worn = 0.0
        
        for (item in equipmentItems) {
            val itemWeight = item.weight * item.quantity
            when {
                item.category == EMEquipmentCategory.FOOD -> consumable += itemWeight
                item.isWorn -> worn += itemWeight
                else -> base += itemWeight
            }
        }
        
        baseWeight = base
        consumableWeight = consumable
        wornWeight = worn
        totalWeight = base + consumable + worn
        updatedAt = Instant.now()
    }
    
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false
        
        other as EMEquipmentList
        
        return id == other.id
    }
    
    override fun hashCode(): Int {
        return id.hashCode()
    }
    
    override fun toString(): String {
        return "EMEquipmentList(id='$id', name='$name')"
    }
}

// 枚举类型定义
enum class EMEquipmentListType {
    SHORT_HIKE,    // 短途徒步（1-3天）
    LONG_HIKE,     // 长途徒步（4天以上）
    CAMPING,       // 露营
    MOUNTAINEERING, // 登山
    TREKKING,      // 穿越
    CUSTOM         // 自定义
}

enum class EMEquipmentListStatus {
    PLANNING,   // 规划中
    PREPARING,  // 准备中
    READY,      // 已完成准备
    IN_USE,     // 使用中
    COMPLETED,  // 已完成
    ARCHIVED    // 已归档
}

enum class EMSeasonSuitability {
    SPRING,     // 春季
    SUMMER,     // 夏季
    AUTUMN,     // 秋季
    WINTER,     // 冬季
    ALL_SEASONS // 四季
}