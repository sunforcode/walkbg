package org.example.model

import jakarta.persistence.*

/**
 * 餐食计划标签实体
 */
@Entity
@Table(name = "meal_plan_tags")
data class MealPlanTag(
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    val id: Long = 0,
    
    @Column(nullable = false, length = 50)
    val tag: String,
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "meal_plan_id", nullable = false)
    var mealPlan: MealPlan? = null
)