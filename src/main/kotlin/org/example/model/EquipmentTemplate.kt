package org.example.model

import jakarta.persistence.*
import java.time.Instant

@Entity
@Table(name = "em_equipment_templates")
data class EMEquipmentTemplate(
    @Id
    val id: String,
    
    @Column(nullable = false)
    var name: String,
    
    @Column(columnDefinition = "TEXT")
    var description: String? = null,
    
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    var type: EMEquipmentListType,
    
    @Column(nullable = false)
    var isOfficial: Boolean = false,
    
    var creatorId: String? = null,
    
    var creatorName: String? = null,
    
    @Column(nullable = false)
    var usageCount: Int = 0,
    
    @Column(nullable = false)
    var rating: Double = 0.0,
    
    @Column(nullable = false)
    val createdAt: Instant = Instant.now(),
    
    @Column(nullable = false)
    var updatedAt: Instant = Instant.now(),
    
    @OneToMany(mappedBy = "template", cascade = [CascadeType.ALL], orphanRemoval = true)
    val equipmentItems: MutableList<EMTemplateEquipmentItem> = mutableListOf(),
    
    @OneToMany(mappedBy = "template", cascade = [CascadeType.ALL], orphanRemoval = true)
    val seasons: MutableList<EMTemplateSeasonSuitability> = mutableListOf(),
    
    @OneToMany(mappedBy = "template", cascade = [CascadeType.ALL], orphanRemoval = true)
    val tags: MutableList<EMTemplateTag> = mutableListOf()
) {
    // 添加关联实体的辅助方法
    fun addEquipmentItem(item: EMTemplateEquipmentItem) {
        equipmentItems.add(item)
        item.template = this
    }
    
    fun addSeason(season: EMSeasonSuitability) {
        seasons.add(EMTemplateSeasonSuitability(template = this, season = season))
    }
    
    fun addTag(tag: String) {
        tags.add(EMTemplateTag(template = this, tag = tag))
    }
    
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false
        
        other as EMEquipmentTemplate
        
        return id == other.id
    }
    
    override fun hashCode(): Int {
        return id.hashCode()
    }
    
    override fun toString(): String {
        return "EMEquipmentTemplate(id='$id', name='$name')"
    }
}

@Entity
@Table(name = "em_template_equipment_items")
data class EMTemplateEquipmentItem(
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
    
    var brand: String? = null,
    
    var model: String? = null,
    
    var isShared: Boolean = false,
    
    var sharedPersonCount: Int? = null,
    
    var imageUrl: String? = null,
    
    @Column(columnDefinition = "TEXT")
    var notes: String? = null,
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "template_id")
    var template: EMEquipmentTemplate? = null
) {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false
        
        other as EMTemplateEquipmentItem
        
        return id == other.id
    }
    
    override fun hashCode(): Int {
        return id.hashCode()
    }
}

@Entity
@Table(name = "em_template_seasons")
data class EMTemplateSeasonSuitability(
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    val id: Long? = null,
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "template_id")
    var template: EMEquipmentTemplate? = null,
    
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    val season: EMSeasonSuitability
)

@Entity
@Table(name = "em_template_tags")
data class EMTemplateTag(
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    val id: Long? = null,
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "template_id")
    var template: EMEquipmentTemplate? = null,
    
    @Column(nullable = false)
    val tag: String
)