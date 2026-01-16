package org.example.meal.model

import jakarta.persistence.*
import org.example.equipment.model.FoodItem

/**
 * 餐食食物关联实体（单向关联）
 */
@Entity
@Table(
    name = "meal_food_items",
    indexes = [
        Index(name = "idx_meal_food_items_meal_day_id", columnList = "meal_day_id"),
        Index(name = "idx_meal_food_items_food_item_id", columnList = "food_item_id"),
        Index(name = "idx_meal_food_items_meal_type", columnList = "meal_type")
    ]
)
data class MealFoodItem(
    @Id
    @Column(length = 64)
    val id: String,

    @Column(name = "meal_day_id", length = 64, nullable = false)
    val mealDayId: String,

    @Column(name = "food_item_id", length = 64, nullable = false)
    val foodItemId: String,

    @Column(name = "meal_type", nullable = false, length = 20)
    val mealType: String, // breakfast, lunch, dinner, snacks, drinks

    @Column(nullable = false)
    var quantity: Int = 1
) {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false
        other as MealFoodItem
        return id == other.id
    }

    override fun hashCode(): Int {
        return id.hashCode()
    }

    override fun toString(): String {
        return "MealFoodItem(id='$id', mealDayId='$mealDayId', foodItemId='$foodItemId')"
    }
}
