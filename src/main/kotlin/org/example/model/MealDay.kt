package org.example.model

import jakarta.persistence.*
import org.example.model.MealItem

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
    var notes: String? = null,

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "meal_plan_id", insertable = false, updatable = false)
    var mealPlan: MealPlan? = null,

    @OneToMany(mappedBy = "mealDay", cascade = [CascadeType.ALL], orphanRemoval = true, fetch = FetchType.LAZY)
    val mealItems: MutableList<MealItem> = mutableListOf()
) {
    fun addMealItem(mealItem: MealItem) {
        mealItems.add(mealItem)
        mealItem.mealDay = this
    }

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