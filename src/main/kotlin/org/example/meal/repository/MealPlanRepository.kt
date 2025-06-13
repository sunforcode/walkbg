package org.example.meal.repository

import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.query.Param
import org.springframework.stereotype.Repository
import org.example.meal.model.MealPlan

/**
 * 餐食计划Repository
 */
@Repository
interface MealPlanRepository : JpaRepository<MealPlan, String> {

    /**
     * 根据行程ID查找餐食计划
     */
    fun findByTripId(tripId: String): List<MealPlan>

    /**
     * 根据创建者查找餐食计划
     */
    fun findByCreatedBy(createdBy: String): List<MealPlan>

    /**
     * 根据餐食计划名称模糊查询
     */
    fun findByNameContainingIgnoreCase(name: String): List<MealPlan>

    /**
     * 分页查找指定行程的餐食计划
     */
    fun findByTripId(tripId: String, pageable: Pageable): Page<MealPlan>

    /**
     * 分页查找指定创建者的餐食计划
     */
    fun findByCreatedBy(createdBy: String, pageable: Pageable): Page<MealPlan>

    /**
     * 检查行程是否已有餐食计划
     */
    fun existsByTripId(tripId: String): Boolean

    /**
     * 统计指定创建者的餐食计划数量
     */
    fun countByCreatedBy(createdBy: String): Long

    /**
     * 统计指定行程的餐食计划数量
     */
    fun countByTripId(tripId: String): Long

    /**
     * 查找最新创建的餐食计划
     */
    fun findTop10ByOrderByCreatedAtDesc(): List<MealPlan>

    /**
     * 多条件搜索餐食计划
     */
    @Query("""
        SELECT mp FROM MealPlan mp
        WHERE (:keyword IS NULL OR
               LOWER(mp.name) LIKE LOWER(CONCAT('%', :keyword, '%')) OR
               LOWER(mp.description) LIKE LOWER(CONCAT('%', :keyword, '%')))
        AND (:tripId IS NULL OR mp.tripId = :tripId)
        AND (:createdBy IS NULL OR mp.createdBy = :createdBy)
    """)
    fun searchMealPlans(
        @Param("keyword") keyword: String?,
        @Param("tripId") tripId: String?,
        @Param("createdBy") createdBy: String?,
        pageable: Pageable
    ): Page<MealPlan>

    /**
     * 查找指定创建者最近的餐食计划
     */
    @Query("""
        SELECT mp FROM MealPlan mp
        WHERE mp.createdBy = :createdBy
        ORDER BY mp.createdAt DESC
    """)
    fun findRecentMealPlansByCreator(@Param("createdBy") createdBy: String, pageable: Pageable): Page<MealPlan>

    /**
     * 统计餐食计划的创建者分布
     */
    @Query("""
        SELECT mp.createdBy, COUNT(mp) as count
        FROM MealPlan mp
        WHERE mp.createdBy IS NOT NULL
        GROUP BY mp.createdBy
        ORDER BY count DESC
    """)
    fun getMealPlanCountByCreator(): List<Array<Any>>

    /**
     * 查找包含指定关键词的餐食计划
     */
    @Query("""
        SELECT mp FROM MealPlan mp
        WHERE LOWER(mp.name) LIKE LOWER(CONCAT('%', :keyword, '%'))
        OR LOWER(mp.description) LIKE LOWER(CONCAT('%', :keyword, '%'))
    """)
    fun findByKeyword(@Param("keyword") keyword: String): List<MealPlan>

    /**
     * 获取餐食计划统计信息
     */
    @Query("""
        SELECT new map(
            COUNT(mp) as totalPlans,
            COUNT(DISTINCT mp.createdBy) as uniqueCreators,
            COUNT(DISTINCT mp.tripId) as associatedTrips
        )
        FROM MealPlan mp
    """)
    fun getMealPlanStatistics(): Map<String, Any>

    /**
     * 查找没有关联行程的餐食计划
     */
    fun findByTripIdIsNull(): List<MealPlan>

    /**
     * 查找有关联行程的餐食计划
     */
    fun findByTripIdIsNotNull(): List<MealPlan>

    /**
     * 根据创建时间范围查找餐食计划
     */
    @Query("""
        SELECT mp FROM MealPlan mp
        WHERE mp.createdAt BETWEEN :startTime AND :endTime
        ORDER BY mp.createdAt DESC
    """)
    fun findByCreatedAtBetween(
        @Param("startTime") startTime: java.time.Instant,
        @Param("endTime") endTime: java.time.Instant
    ): List<MealPlan>

    /**
     * 删除指定行程的所有餐食计划
     */
    fun deleteByTripId(tripId: String): Long

    /**
     * 删除指定创建者的所有餐食计划
     */
    fun deleteByCreatedBy(createdBy: String): Long
}