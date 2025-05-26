package org.example.model

import jakarta.persistence.*
import java.time.Instant

@Entity
@Table(name = "em_user_equipment_inventories")
data class EMUserEquipmentInventory(
    @Id
    val id: String,
    
    @Column(nullable = false)
    val userId: String,
    
    @Column(nullable = false)
    var lastUpdatedAt: Instant = Instant.now(),
    
    @OneToMany(mappedBy = "userInventory", cascade = [CascadeType.ALL], orphanRemoval = true)
    val equipmentItems: MutableList<EMEquipmentItem> = mutableListOf()
) {
    // 添加关联实体的辅助方法
    fun addEquipmentItem(item: EMEquipmentItem) {
        equipmentItems.add(item)
        item.userInventory = this
        lastUpdatedAt = Instant.now()
    }
    
    fun removeEquipmentItem(item: EMEquipmentItem) {
        equipmentItems.remove(item)
        lastUpdatedAt = Instant.now()
    }
    
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false
        
        other as EMUserEquipmentInventory
        
        return id == other.id
    }
    
    override fun hashCode(): Int {
        return id.hashCode()
    }
    
    override fun toString(): String {
        return "EMUserEquipmentInventory(id='$id', userId='$userId')"
    }
}

@Entity
@Table(name = "em_equipment_list_seasons")
data class EMEquipmentListSeason(
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    val id: Long? = null,
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "equipment_list_id")
    var equipmentList: EMEquipmentList? = null,
    
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    val season: EMSeasonSuitability
)

@Entity
@Table(name = "em_equipment_list_tags")
data class EMEquipmentListTag(
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    val id: Long? = null,
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "equipment_list_id")
    var equipmentList: EMEquipmentList? = null,
    
    @Column(nullable = false)
    val tag: String
)