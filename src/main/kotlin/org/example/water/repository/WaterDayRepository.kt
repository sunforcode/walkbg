package org.example.water.repository

import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.query.Param
import org.springframework.stereotype.Repository
import org.example.water.model.WaterDay

/**
 * 每日用水表Repository
 */
@Repository
interface WaterDayRepository : JpaRepository<WaterDay, String> {

    /**
     * 根据用水计划ID查找每日用水
     */
    fun findByWaterPlanId(waterPlanId: String): List<WaterDay>

    /**
     * 根据用水计划ID按天数排序查找每日用水
     */
    fun findByWaterPlanIdOrderByDayNumber(waterPlanId: String): List<WaterDay>

    /**
     * 根据用水计划ID和天数查找每日用水
     */
    fun findByWaterPlanIdAndDayNumber(waterPlanId: String, dayNumber: Int): WaterDay?

    /**
     * 统计用水计划的天数
     */
    fun countByWaterPlanId(waterPlanId: String): Long

    /**
     * 查找指定天数范围内的用水
     */
    fun findByWaterPlanIdAndDayNumberBetween(
        waterPlanId: String, 
        startDay: Int, 
        endDay: Int
    ): List<WaterDay>

    /**
     * 删除用水计划的所有每日用水
     */
    fun deleteByWaterPlanId(waterPlanId: String): Long

    /**
     * 检查用水计划是否有指定天数的用水
     */
    fun existsByWaterPlanIdAndDayNumber(waterPlanId: String, dayNumber: Int): Boolean

    /**
     * 查找用水计划的最大天数
     */
    @Query("""
        SELECT MAX(wd.dayNumber) FROM WaterDay wd WHERE wd.waterPlanId = :waterPlanId
    """)
    fun findMaxDayNumberByWaterPlanId(@Param("waterPlanId") waterPlanId: String): Int?

    /**
     * 查找用水计划的最小天数
     */
    @Query("""
        SELECT MIN(wd.dayNumber) FROM WaterDay wd WHERE wd.waterPlanId = :waterPlanId
    """)
    fun findMinDayNumberByWaterPlanId(@Param("waterPlanId") waterPlanId: String): Int?

    /**
     * 统计用水计划每日的水源数量
     */
    @Query("""
        SELECT wd.dayNumber, COUNT(ws) as sourceCount
        FROM WaterDay wd
        LEFT JOIN WaterSourceItem ws ON ws.waterDayId = wd.id
        WHERE wd.waterPlanId = :waterPlanId
        GROUP BY wd.dayNumber
        ORDER BY wd.dayNumber
    """)
    fun getWaterSourceCountByDay(@Param("waterPlanId") waterPlanId: String): List<Array<Any>>
}