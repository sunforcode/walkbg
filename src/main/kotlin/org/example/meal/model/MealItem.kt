package org.example.meal.model

import jakarta.persistence.*

/**
 * 餐食项目表
 */
@Entity
@Table(
    name = "meal_items",
    indexes = [
        Index(name = "idx_meal_items_meal_day_id", columnList = "meal_day_id"),
        Index(name = "idx_meal_items_meal_type", columnList = "meal_type")
    ]
)
data class MealItem(
    @Id
    @Column(length = 64)
    val id: String,

    @Column(name = "meal_day_id", length = 64, nullable = false)
    val mealDayId: String,

    @Column(name = "meal_type", nullable = false)
    val mealType: Int, // 0: 早餐, 1: 午餐, 2: 晚餐, 3: 加餐

    @Column(name = "food_name", length = 100, nullable = false)
    var foodName: String,

    @Column(nullable = false)
    var quantity: Int = 1,

    @Column(columnDefinition = "TEXT")
    var notes: String? = null
) {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as MealItem

        return id == other.id
    }

    override fun hashCode(): Int {
        return id.hashCode()
    }

    override fun toString(): String {
        return "MealItem(id='$id', foodName='$foodName')"
    }
}