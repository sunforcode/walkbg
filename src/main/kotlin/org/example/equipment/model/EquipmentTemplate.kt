package org.example.equipment.model

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
    var updatedAt: Instant = Instant.now()
) {
    /**
     * 注意：不再持有以下关联关系的集合引用
     * - equipmentItems: 通过 TemplateEquipmentItemRepository.findByTemplateId(templateId) 查询
     * - seasons: 通过 TemplateSeasonSuitabilityRepository.findByTemplateId(templateId) 查询
     * - tags: 通过 TemplateTagRepository.findByTemplateId(templateId) 查询
     */

    /**
     * 领域行为：更新使用次数
     */
    fun incrementUsageCount() {
        this.usageCount++
        this.updatedAt = Instant.now()
    }

    /**
     * 领域行为：更新评分
     */
    fun updateRating(newRating: Double) {
        require(newRating in 0.0..5.0) { "评分必须在 0-5 之间" }
        this.rating = newRating
        this.updatedAt = Instant.now()
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