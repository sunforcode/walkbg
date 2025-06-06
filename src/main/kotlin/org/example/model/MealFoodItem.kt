package org.example.model

import jakarta.persistence.*

/**
 * 餐食食物关联实体
 */
@Entity
@Table(name = "meal_food_items")
data class MealFoodItem(
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    val id: Long = 0,
    
    @Column(name = "meal_type", nullable = false, length = 20)
    val mealType: String, // breakfast, lunch, dinner, snacks, drinks
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "meal_day_id", nullable = false)
    var mealDay: MealDay? = null,
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "food_item_id", nullable = false)
    var foodItem: FoodItem? = null
)