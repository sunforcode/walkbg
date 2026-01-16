package org.example.meal.model

import jakarta.persistence.*
import org.example.user.model.User
import org.example.trip.model.Trip
import java.time.Instant

/**
 * 餐食计划实体
 */
@Entity
@Table(
    name = "meal_plans",
    indexes = [
        Index(name = "idx_meal_plans_trip_id", columnList = "trip_id"),
        Index(name = "idx_meal_plans_created_by", columnList = "created_by")
    ]
)
data class MealPlan(
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
    var updatedAt: Instant = Instant.now()
) {
    /**
     * 注意：不再持有以下关联关系的集合引用
     * - mealDays: 通过 MealDayRepository.findByMealPlanId(mealPlanId) 查询
     * - tags: 通过 MealPlanTagRepository.findByMealPlanId(mealPlanId) 查询
     */

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false
        
        other as MealPlan
        
        return id == other.id
    }
    
    override fun hashCode(): Int {
        return id.hashCode()
    }
    
    override fun toString(): String {
        return "MealPlan(id='$id', name='$name')"
    }
}