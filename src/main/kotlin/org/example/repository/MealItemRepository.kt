package org.example.repository

import org.example.model.MealItem
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.query.Param
import org.springframework.stereotype.Repository

/**
 * 餐食项目表Repository
 */
@Repository
interface MealItemRepository : JpaRepository<MealItem, String> {

    /**
     * 根据每日餐食ID查找餐食项目
     */
    fun findByMealDayId(mealDayId: String): List<MealItem>

    /**
     * 根据餐别查找餐食项目
     */
    fun findByMealType(mealType: Int): List<MealItem>

    /**
     * 根据每日餐食ID和餐别查找餐食项目
     */
    fun findByMealDayIdAndMealType(mealDayId: String, mealType: Int): List<MealItem>

    /**
     * 根据食物名称模糊查询
     */
    fun findByFoodNameContainingIgnoreCase(foodName: String): List<MealItem>

    /**
     * 统计每日餐食的项目数量
     */
    fun countByMealDayId(mealDayId: String): Long

    /**
     * 统计指定餐别的项目数量
     */
    fun countByMealDayIdAndMealType(mealDayId: String, mealType: Int): Long

    /**
     * 删除每日餐食的所有项目
     */
    fun deleteByMealDayId(mealDayId: String): Long

    /**
     * 删除指定餐别的所有项目
     */
    fun deleteByMealDayIdAndMealType(mealDayId: String, mealType: Int): Long

    /**
     * 查找最受欢迎的食物（按使用频率排序）
     */
    @Query("""
        SELECT mi.foodName, COUNT(mi) as usageCount
        FROM MealItem mi
        GROUP BY mi.foodName
        ORDER BY usageCount DESC
    """)
    fun findMostPopularFoods(): List<Array<Any>>

    /**
     * 统计餐别分布
     */
    @Query("""
        SELECT mi.mealType, COUNT(mi) as count
        FROM MealItem mi
        WHERE mi.mealDayId = :mealDayId
        GROUP BY mi.mealType
        ORDER BY mi.mealType
    """)
    fun getMealTypeDistribution(@Param("mealDayId") mealDayId: String): List<Array<Any>>

    /**
     * 查找包含指定食物的餐食项目
     */
    @Query("""
        SELECT mi FROM MealItem mi
        WHERE LOWER(mi.foodName) LIKE LOWER(CONCAT('%', :keyword, '%'))
        OR LOWER(mi.notes) LIKE LOWER(CONCAT('%', :keyword, '%'))
    """)
    fun searchMealItems(@Param("keyword") keyword: String): List<MealItem>

    /**
     * 统计总的餐食项目数量
     */
    @Query("""
        SELECT SUM(mi.quantity) FROM MealItem mi WHERE mi.mealDayId = :mealDayId
    """)
    fun getTotalQuantityByMealDayId(@Param("mealDayId") mealDayId: String): Long?
}