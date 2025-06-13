package org.example.water.repository

import org.example.water.model.WaterSourceItem
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.query.Param
import org.springframework.stereotype.Repository

/**
 * 水源表Repository
 */
@Repository
interface WaterSourceItemRepository : JpaRepository<WaterSourceItem, String> {

    /**
     * 根据每日用水ID查找水源
     */
    fun findByWaterDayId(waterDayId: String): List<WaterSourceItem>

    /**
     * 根据水源类型查找水源
     */
    fun findByType(type: Int): List<WaterSourceItem>

    /**
     * 根据每日用水ID和水源类型查找水源
     */
    fun findByWaterDayIdAndType(waterDayId: String, type: Int): List<WaterSourceItem>

    /**
     * 根据水源名称模糊查询
     */
    fun findBySourceNameContainingIgnoreCase(sourceName: String): List<WaterSourceItem>

    /**
     * 统计每日用水的水源数量
     */
    fun countByWaterDayId(waterDayId: String): Long

    /**
     * 统计指定类型的水源数量
     */
    fun countByWaterDayIdAndType(waterDayId: String, type: Int): Long

    /**
     * 删除每日用水的所有水源
     */
    fun deleteByWaterDayId(waterDayId: String): Long

    /**
     * 删除指定类型的所有水源
     */
    fun deleteByWaterDayIdAndType(waterDayId: String, type: Int): Long

    /**
     * 计算每日用水的总预计水量
     */
    @Query("""
        SELECT SUM(wsi.estimatedVolume) FROM WaterSourceItem wsi 
        WHERE wsi.waterDayId = :waterDayId AND wsi.estimatedVolume IS NOT NULL
    """)
    fun getTotalEstimatedVolumeByWaterDayId(@Param("waterDayId") waterDayId: String): Long?

    /**
     * 根据水源类型统计预计水量
     */
    @Query("""
        SELECT wsi.type, COUNT(wsi), SUM(wsi.estimatedVolume)
        FROM WaterSourceItem wsi
        WHERE wsi.waterDayId = :waterDayId AND wsi.estimatedVolume IS NOT NULL
        GROUP BY wsi.type
        ORDER BY wsi.type
    """)
    fun getVolumeStatsByType(@Param("waterDayId") waterDayId: String): List<Array<Any>>

    /**
     * 查找最常用的水源名称
     */
    @Query("""
        SELECT wsi.sourceName, COUNT(wsi) as usageCount
        FROM WaterSourceItem wsi
        GROUP BY wsi.sourceName
        ORDER BY usageCount DESC
    """)
    fun findMostUsedWaterSources(): List<Array<Any>>

    /**
     * 查找包含指定关键词的水源
     */
    @Query("""
        SELECT wsi FROM WaterSourceItem wsi
        WHERE LOWER(wsi.sourceName) LIKE LOWER(CONCAT('%', :keyword, '%'))
        OR LOWER(wsi.notes) LIKE LOWER(CONCAT('%', :keyword, '%'))
    """)
    fun searchWaterSources(@Param("keyword") keyword: String): List<WaterSourceItem>

    /**
     * 统计水源类型分布
     */
    @Query("""
        SELECT wsi.type, COUNT(wsi) as count
        FROM WaterSourceItem wsi
        WHERE wsi.waterDayId = :waterDayId
        GROUP BY wsi.type
        ORDER BY wsi.type
    """)
    fun getWaterSourceTypeDistribution(@Param("waterDayId") waterDayId: String): List<Array<Any>>
}