package org.example.meal.model

import jakarta.persistence.*

/**
 * 每日餐食表
 */
@Entity
@Table(
    name = "meal_days",
    indexes = [
        Index(name = "idx_meal_days_meal_plan_id", columnList = "meal_plan_id"),
        Index(name = "idx_meal_days_day_number", columnList = "day_number")
    ]
)
data class MealDay(
    @Id
    @Column(length = 64)
    val id: String,

    @Column(name = "meal_plan_id", length = 64, nullable = false)
    val mealPlanId: String,

    @Column(name = "day_number", nullable = false)
    val dayNumber: Int,

    @Column(columnDefinition = "TEXT")
    var notes: String? = null
) {
    /**
     * 注意：不再持有以下关联关系的集合引用
     * - mealItems: 通过 MealItemRepository.findByMealDayId(mealDayId) 查询
     */

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as MealDay

        return id == other.id
    }

    override fun hashCode(): Int {
        return id.hashCode()
    }

    override fun toString(): String {
        return "MealDay(id='$id', dayNumber=$dayNumber)"
    }
}