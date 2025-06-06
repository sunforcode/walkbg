package org.example.model

import jakarta.persistence.*
import java.time.Instant

/**
 * 水源实体
 */
@Entity
@Table(name = "water_sources")
data class WaterSource(
    @Id
    @Column(length = 64)
    val id: String,
    
    @Column(nullable = false, length = 200)
    val name: String,
    
    @Column(columnDefinition = "TEXT")
    val description: String? = null,
    
    @Column(length = 50)
    val type: String? = null,
    
    @Column(length = 200)
    val location: String? = null,
    
    @Column(name = "distance_from_trail", precision = 8)
    val distanceFromTrail: Double? = null,
    
    @Column(length = 20)
    val quality: String? = null,
    
    val reliability: Int? = null,
    
    @Column(name = "estimated_volume")
    val estimatedVolume: Int? = null,
    
    @Column(name = "needs_treatment", nullable = false)
    val needsTreatment: Boolean = true,
    
    @Column(name = "created_at", nullable = false, updatable = false)
    val createdAt: Instant = Instant.now(),
    
    @Column(name = "updated_at", nullable = false)
    var updatedAt: Instant = Instant.now(),
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "water_plan_id")
    var waterPlan: WaterPlan? = null
) {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false
        
        other as WaterSource
        
        return id == other.id
    }
    
    override fun hashCode(): Int {
        return id.hashCode()
    }
    
    override fun toString(): String {
        return "WaterSource(id='$id', name='$name')"
    }
}