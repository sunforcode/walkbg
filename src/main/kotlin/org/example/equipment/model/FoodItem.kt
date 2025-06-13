package org.example.equipment.model

import jakarta.persistence.*
import java.time.Instant

/**
 * 食物物品实体
 */
@Entity
@Table(name = "food_items")
data class FoodItem(
    @Id
    @Column(length = 64)
    val id: String,
    
    @Column(nullable = false, length = 200)
    val name: String,
    
    @Column(columnDefinition = "TEXT")
    val description: String? = null,
    
    @Column(precision = 8)
    val weight: Double? = null,
    
    @Column(nullable = false)
    val quantity: Int = 1,
    
    @Column(precision = 8)
    val calories: Double? = null,
    
    @Column(precision = 8)
    val protein: Double? = null,
    
    @Column(precision = 8)
    val fat: Double? = null,
    
    @Column(precision = 8)
    val carbs: Double? = null,
    
    @Column(precision = 10)
    val price: Double? = null,
    
    @Column(nullable = false)
    val prepared: Boolean = false,
    
    @Column(name = "is_owned", nullable = false)
    val isOwned: Boolean = false,
    
    @Column(columnDefinition = "TEXT")
    val notes: String? = null,
    
    @Column(name = "created_at", nullable = false, updatable = false)
    val createdAt: Instant = Instant.now(),
    
    @Column(name = "updated_at", nullable = false)
    var updatedAt: Instant = Instant.now()
) {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false
        
        other as FoodItem
        
        return id == other.id
    }
    
    override fun hashCode(): Int {
        return id.hashCode()
    }
    
    override fun toString(): String {
        return "FoodItem(id='$id', name='$name')"
    }
}