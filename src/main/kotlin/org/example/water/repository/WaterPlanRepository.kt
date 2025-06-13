package org.example.water.repository

import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.query.Param
import org.springframework.stereotype.Repository
import org.example.water.model.WaterPlan

/**
 * 用水计划Repository
 */
@Repository
interface WaterPlanRepository : JpaRepository<WaterPlan, String> {

    /**
     * 根据行程ID查找用水计划
     */
    fun findByTripId(tripId: String): List<WaterPlan>

    /**
     * 根据创建者查找用水计划
     */
    fun findByCreatedBy(createdBy: String): List<WaterPlan>

    /**
     * 根据用水计划名称模糊查询
     */
    fun findByNameContainingIgnoreCase(name: String): List<WaterPlan>

    /**
     * 分页查找指定行程的用水计划
     */
    fun findByTripId(tripId: String, pageable: Pageable): Page<WaterPlan>

    /**
     * 分页查找指定创建者的用水计划
     */
    fun findByCreatedBy(createdBy: String, pageable: Pageable): Page<WaterPlan>

    /**
     * 检查行程是否已有用水计划
     */
    fun existsByTripId(tripId: String): Boolean

    /**
     * 统计指定创建者的用水计划数量
     */
    fun countByCreatedBy(createdBy: String): Long

    /**
     * 统计指定行程的用水计划数量
     */
    fun countByTripId(tripId: String): Long

    /**
     * 查找最新创建的用水计划
     */
    fun findTop10ByOrderByCreatedAtDesc(): List<WaterPlan>

    /**
     * 多条件搜索用水计划
     */
    @Query("""
        SELECT wp FROM WaterPlan wp
        WHERE (:keyword IS NULL OR
               LOWER(wp.name) LIKE LOWER(CONCAT('%', :keyword, '%')) OR
               LOWER(wp.description) LIKE LOWER(CONCAT('%', :keyword, '%')))
        AND (:tripId IS NULL OR wp.tripId = :tripId)
        AND (:createdBy IS NULL OR wp.createdBy = :createdBy)
    """)
    fun searchWaterPlans(
        @Param("keyword") keyword: String?,
        @Param("tripId") tripId: String?,
        @Param("createdBy") createdBy: String?,
        pageable: Pageable
    ): Page<WaterPlan>

    /**
     * 查找指定创建者最近的用水计划
     */
    @Query("""
        SELECT wp FROM WaterPlan wp
        WHERE wp.createdBy = :createdBy
        ORDER BY wp.createdAt DESC
    """)
    fun findRecentWaterPlansByCreator(@Param("createdBy") createdBy: String, pageable: Pageable): Page<WaterPlan>

    /**
     * 统计用水计划的创建者分布
     */
    @Query("""
        SELECT wp.createdBy, COUNT(wp) as count
        FROM WaterPlan wp
        WHERE wp.createdBy IS NOT NULL
        GROUP BY wp.createdBy
        ORDER BY count DESC
    """)
    fun getWaterPlanCountByCreator(): List<Array<Any>>

    /**
     * 查找包含指定关键词的用水计划
     */
    @Query("""
        SELECT wp FROM WaterPlan wp
        WHERE LOWER(wp.name) LIKE LOWER(CONCAT('%', :keyword, '%'))
        OR LOWER(wp.description) LIKE LOWER(CONCAT('%', :keyword, '%'))
    """)
    fun findByKeyword(@Param("keyword") keyword: String): List<WaterPlan>

    /**
     * 获取用水计划统计信息
     */
    @Query("""
        SELECT new map(
            COUNT(wp) as totalPlans,
            COUNT(DISTINCT wp.createdBy) as uniqueCreators,
            COUNT(DISTINCT wp.tripId) as associatedTrips
        )
        FROM WaterPlan wp
    """)
    fun getWaterPlanStatistics(): Map<String, Any>

    /**
     * 查找没有关联行程的用水计划
     */
    fun findByTripIdIsNull(): List<WaterPlan>

    /**
     * 查找有关联行程的用水计划
     */
    fun findByTripIdIsNotNull(): List<WaterPlan>

    /**
     * 根据创建时间范围查找用水计划
     */
    @Query("""
        SELECT wp FROM WaterPlan wp
        WHERE wp.createdAt BETWEEN :startTime AND :endTime
        ORDER BY wp.createdAt DESC
    """)
    fun findByCreatedAtBetween(
        @Param("startTime") startTime: java.time.Instant,
        @Param("endTime") endTime: java.time.Instant
    ): List<WaterPlan>

    /**
     * 统计用水计划每日的水源总数
     */
    @Query("""
        SELECT 0
        FROM WaterPlan wp
        WHERE wp.id = :waterPlanId
    """)
    fun getWaterSourceCount(@Param("waterPlanId") waterPlanId: String): Long?

    /**
     * 统计用水计划的总预计水量
     */
    @Query("""
        SELECT 0
        FROM WaterPlan wp
        WHERE wp.id = :waterPlanId
    """)
    fun getTotalEstimatedVolume(@Param("waterPlanId") waterPlanId: String): Long?

    /**
     * 删除指定行程的所有用水计划
     */
    fun deleteByTripId(tripId: String): Long

    /**
     * 删除指定创建者的所有用水计划
     */
    fun deleteByCreatedBy(createdBy: String): Long
}