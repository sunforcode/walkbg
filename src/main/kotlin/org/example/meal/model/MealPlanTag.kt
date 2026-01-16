package org.example.meal.model

import jakarta.persistence.*

/**
 * 餐食计划标签实体（单向关联）
 */
@Entity
@Table(
    name = "meal_plan_tags",
    indexes = [
        Index(name = "idx_meal_plan_tags_plan_id", columnList = "meal_plan_id"),
        Index(name = "idx_meal_plan_tags_tag", columnList = "tag")
    ]
)
data class MealPlanTag(
    @Id
    @Column(length = 64)
    val id: String,

    @Column(name = "meal_plan_id", length = 64, nullable = false)
    val mealPlanId: String,
    
    @Column(nullable = false, length = 50)
    val tag: String
) {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false
        other as MealPlanTag
        return id == other.id
    }

    override fun hashCode(): Int {
        return id.hashCode()
    }

    override fun toString(): String {
        return "MealPlanTag(id='$id', mealPlanId='$mealPlanId', tag='$tag')"
    }
}
