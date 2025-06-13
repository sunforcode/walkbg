package org.example.meal.repository

import org.example.meal.model.MealDay
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.query.Param
import org.springframework.stereotype.Repository

/**
 * 每日餐食表Repository
 */
@Repository
interface MealDayRepository : JpaRepository<MealDay, String> {

    /**
     * 根据餐食计划ID查找每日餐食
     */
    fun findByMealPlanId(mealPlanId: String): List<MealDay>

    /**
     * 根据餐食计划ID按天数排序查找每日餐食
     */
    fun findByMealPlanIdOrderByDayNumber(mealPlanId: String): List<MealDay>

    /**
     * 根据餐食计划ID和天数查找每日餐食
     */
    fun findByMealPlanIdAndDayNumber(mealPlanId: String, dayNumber: Int): MealDay?

    /**
     * 统计餐食计划的天数
     */
    fun countByMealPlanId(mealPlanId: String): Long

    /**
     * 查找指定天数范围内的餐食
     */
    fun findByMealPlanIdAndDayNumberBetween(
        mealPlanId: String, 
        startDay: Int, 
        endDay: Int
    ): List<MealDay>

    /**
     * 删除餐食计划的所有每日餐食
     */
    fun deleteByMealPlanId(mealPlanId: String): Long

    /**
     * 检查餐食计划是否有指定天数的餐食
     */
    fun existsByMealPlanIdAndDayNumber(mealPlanId: String, dayNumber: Int): Boolean

    /**
     * 查找餐食计划的最大天数
     */
    @Query("""
        SELECT MAX(md.dayNumber) FROM MealDay md WHERE md.mealPlanId = :mealPlanId
    """)
    fun findMaxDayNumberByMealPlanId(@Param("mealPlanId") mealPlanId: String): Int?

    /**
     * 查找餐食计划的最小天数
     */
    @Query("""
        SELECT MIN(md.dayNumber) FROM MealDay md WHERE md.mealPlanId = :mealPlanId
    """)
    fun findMinDayNumberByMealPlanId(@Param("mealPlanId") mealPlanId: String): Int?
}