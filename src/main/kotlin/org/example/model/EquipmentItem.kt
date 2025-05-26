package org.example.model

import jakarta.persistence.*
import java.time.Instant

@Entity
@Table(name = "em_equipment_items")
data class EMEquipmentItem(
    @Id
    val id: String,
    
    @Column(nullable = false)
    var name: String,
    
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    var category: EMEquipmentCategory,
    
    @Column(columnDefinition = "TEXT")
    var description: String? = null,
    
    @Column(nullable = false)
    var weight: Double = 0.0,
    
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    var weightUnit: EMWeightUnit = EMWeightUnit.GRAM,
    
    @Column(nullable = false)
    var quantity: Int = 1,
    
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    var necessity: EMEquipmentNecessity = EMEquipmentNecessity.RECOMMENDED,
    
    @Column(nullable = false)
    var prepared: Boolean = false,
    
    @Column(nullable = false)
    var isOwned: Boolean = false,
    
    @Column(nullable = false)
    var isShared: Boolean = false,
    
    @Column(nullable = false)
    var isWorn: Boolean = false,
    
    var sharedPersonCount: Int? = null,
    
    var brand: String? = null,
    
    var model: String? = null,
    
    var price: Double? = null,
    
    var purchaseLink: String? = null,
    
    var purchaseDate: Instant? = null,
    
    @Column(nullable = false)
    var usageCount: Int = 0,
    
    @Enumerated(EnumType.STRING)
    var condition: EMEquipmentCondition? = null,
    
    var imageUrl: String? = null,
    
    @Column(columnDefinition = "TEXT")
    var notes: String? = null,
    
    @Column(nullable = false)
    val createdAt: Instant = Instant.now(),
    
    @Column(nullable = false)
    var updatedAt: Instant = Instant.now(),
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "equipment_list_id")
    var equipmentList: EMEquipmentList? = null,
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_inventory_id")
    var userInventory: EMUserEquipmentInventory? = null,
    
    @ManyToMany
    @JoinTable(
        name = "em_equipment_item_alternatives",
        joinColumns = [JoinColumn(name = "equipment_item_id")],
        inverseJoinColumns = [JoinColumn(name = "alternative_item_id")]
    )
    val alternativeItems: MutableSet<EMEquipmentItem> = mutableSetOf()
) {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false
        
        other as EMEquipmentItem
        
        return id == other.id
    }
    
    override fun hashCode(): Int {
        return id.hashCode()
    }
    
    override fun toString(): String {
        return "EMEquipmentItem(id='$id', name='$name')"
    }
}

// 枚举类型定义
enum class EMEquipmentCategory {
    SHELTER,      // 住宿装备（帐篷、睡袋、睡垫等）
    FOOD,         // 饮食装备（炉具、餐具、水壶等）
    CLOTHING,     // 保暖装备（衣物、手套、帽子等）
    BACKPACK,     // 背包装备（背包、防雨罩等）
    NAVIGATION,   // 导航装备（地图、指南针、GPS等）
    LIGHTING,     // 照明装备（头灯、手电筒等）
    FIRST_AID,    // 急救装备（急救包、药品等）
    TOOLS,        // 工具装备（刀具、绳索、修理工具等）
    ELECTRONICS,  // 电子装备（手机、相机、充电宝等）
    PERSONAL_CARE, // 个人护理（洗漱用品、防晒用品等）
    OTHER         // 其他装备
}

enum class EMEquipmentNecessity {
    ESSENTIAL,   // 必需
    RECOMMENDED, // 推荐
    OPTIONAL     // 可选
}

enum class EMEquipmentCondition {
    NEW,      // 全新
    GOOD,     // 良好
    FAIR,     // 一般
    POOR,     // 较差
    DAMAGED   // 损坏
}

enum class EMWeightUnit {
    GRAM,     // 克
    KILOGRAM, // 千克
    POUND,    // 磅
    OUNCE     // 盎司
}