package org.example.model

import jakarta.persistence.*
import java.time.Instant

/**
 * 用水计划实体
 */
@Entity
@Table(
    name = "water_plans",
    indexes = [
        Index(name = "idx_water_plans_trip_id", columnList = "trip_id"),
        Index(name = "idx_water_plans_created_by", columnList = "created_by")
    ]
)
data class WaterPlan(
    @Id
    @Column(length = 64)
    val id: String,
    
    @Column(name = "trip_id", length = 64)
    val tripId: String? = null,

    @Column(nullable = false, length = 200)
    val name: String,
    
    @Column(columnDefinition = "TEXT")
    val description: String? = null,
    
    @Column(name = "created_by", length = 64)
    val createdBy: String? = null,
    
    @Column(name = "created_at", nullable = false, updatable = false)
    val createdAt: Instant = Instant.now(),
    
    @Column(name = "updated_at", nullable = false)
    var updatedAt: Instant = Instant.now(),
    
    // 关联关系
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "trip_id", insertable = false, updatable = false)
    var trip: Trip? = null,
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "created_by", insertable = false, updatable = false)
    var creator: User? = null,
    
    @OneToMany(mappedBy = "waterPlan", cascade = [CascadeType.ALL], orphanRemoval = true, fetch = FetchType.LAZY)
    val waterDays: MutableList<WaterDay> = mutableListOf(),

    @OneToMany(mappedBy = "waterPlan", cascade = [CascadeType.ALL], orphanRemoval = true, fetch = FetchType.LAZY)
    val tags: MutableList<WaterPlanTag> = mutableListOf()
) {
    fun addWaterDay(waterDay: WaterDay) {
        waterDays.add(waterDay)
        waterDay.waterPlan = this
        updatedAt = Instant.now()
    }
    
    fun addTag(tag: String) {
        tags.add(WaterPlanTag(tag = tag, waterPlan = this))
        updatedAt = Instant.now()
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false
        
        other as WaterPlan
        
        return id == other.id
    }
    
    override fun hashCode(): Int {
        return id.hashCode()
    }
    
    override fun toString(): String {
        return "WaterPlan(id='$id', name='$name')"
    }
}