package org.example.model

import jakarta.persistence.*
import java.time.Instant

@Entity
@Table(name = "equipment_templates")
data class EquipmentTemplate(
    @Id
    @Column(length = 64)
    val id: String,
    
    @Column(nullable = false)
    var name: String,
    
    @Column(nullable = false)
    var category: Int, // 0: 住宿装备, 1: 饮食装备, 2: 保暖装备, 等等

    @Column(columnDefinition = "TEXT")
    var description: String? = null,
    
    @Column(nullable = false)
    var type: Int, // 改为Int类型，避免枚举依赖问题

    @Column(nullable = false)
    var isOfficial: Boolean = false,
    
    var creatorId: String? = null,
    
    var creatorName: String? = null,

    @Column(nullable = false)
    var usageCount: Int = 0,

    @Column(nullable = false)
    var rating: Double = 0.0,
    
    @Column(name = "created_at", nullable = false, updatable = false)
    val createdAt: Instant = Instant.now(),
    
    @Column(name = "updated_at", nullable = false)
    var updatedAt: Instant = Instant.now(),
    
    @OneToMany(mappedBy = "template", cascade = [CascadeType.ALL], orphanRemoval = true)
    val equipmentItems: MutableList<TemplateEquipmentItem> = mutableListOf(),
    
    @OneToMany(mappedBy = "template", cascade = [CascadeType.ALL], orphanRemoval = true)
    val seasons: MutableList<TemplateSeasonSuitability> = mutableListOf(),
    
    @OneToMany(mappedBy = "template", cascade = [CascadeType.ALL], orphanRemoval = true)
    val tags: MutableList<TemplateTag> = mutableListOf()
) {

    // 添加关联实体的辅助方法
    fun addEquipmentItem(item: TemplateEquipmentItem) {
        equipmentItems.add(item)
        item.template = this
    }

    fun addSeason(season: Int) { // 0: 春季, 1: 夏季, 2: 秋季, 3: 冬季
        seasons.add(TemplateSeasonSuitability(template = this, season = season))
    }
    
    fun addTag(tag: String) {
        tags.add(TemplateTag(template = this, tag = tag))
    }
    
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false
        
        other as EquipmentTemplate
        
        return id == other.id
    }
    
    override fun hashCode(): Int {
        return id.hashCode()
    }
    
    override fun toString(): String {
        return "EquipmentTemplate(id='$id', name='$name')"
    }
}

@Entity
@Table(name = "template_equipment_items")
data class TemplateEquipmentItem(
    @Id
    val id: String,

    @Column(nullable = false)
    var name: String,
    
    @Column(nullable = false)
    var category: Int, // 0: 住宿装备, 1: 饮食装备, 2: 保暖装备, 等等
    
    @Column(columnDefinition = "TEXT")
    var description: String? = null,
    
    @Column(nullable = false)
    var weight: Double = 0.0,
    
    @Column(nullable = false)
    var weightUnit: Int = 0, // 0: 克, 1: 千克, 2: 磅
    
    @Column(nullable = false)
    var quantity: Int = 1,
    
    @Column(nullable = false)
    var necessity: Int = 1, // 0: 必需, 1: 推荐, 2: 可选
    
    var brand: String? = null,
    
    var model: String? = null,
    
    var isShared: Boolean = false,
    
    var sharedPersonCount: Int? = null,
    
    var imageUrl: String? = null,
    
    @Column(columnDefinition = "TEXT")
    var notes: String? = null,
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "template_id")
    var template: EquipmentTemplate? = null
) {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false
        
        other as TemplateEquipmentItem
        
        return id == other.id
    }
    
    override fun hashCode(): Int {
        return id.hashCode()
    }
}

@Entity
@Table(name = "template_seasons")
data class TemplateSeasonSuitability(
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    val id: Long? = null,
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "template_id")
    var template: EquipmentTemplate? = null,
    
    @Column(nullable = false)
    val season: Int // 0: 春季, 1: 夏季, 2: 秋季, 3: 冬季
)

@Entity
@Table(name = "template_tags")
data class TemplateTag(
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    val id: Long? = null,
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "template_id")
    var template: EquipmentTemplate? = null,
    
    @Column(nullable = false)
    val tag: String
)