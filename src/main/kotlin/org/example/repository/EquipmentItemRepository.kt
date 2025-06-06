package org.example.repository

import org.example.model.EquipmentItem
import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.query.Param
import org.springframework.stereotype.Repository
import java.math.BigDecimal

/**
 * 装备物品Repository
 */
@Repository
interface EquipmentItemRepository : JpaRepository<EquipmentItem, String> {

    /**
     * 根据装备分类查找装备
     */
    fun findByCategory(category: Int): List<EquipmentItem>

    /**
     * 根据创建者查找装备
     */
    fun findByCreatedBy(createdBy: String): List<EquipmentItem>

    /**
     * 根据装备名称模糊查询
     */
    fun findByNameContainingIgnoreCase(name: String): List<EquipmentItem>

    /**
     * 根据重量范围查找装备
     */
    fun findByWeightBetween(minWeight: BigDecimal, maxWeight: BigDecimal): List<EquipmentItem>

    /**
     * 根据重量单位查找装备
     */
    fun findByWeightUnit(weightUnit: Int): List<EquipmentItem>

    /**
     * 分页查找指定分类的装备
     */
    fun findByCategory(category: Int, pageable: Pageable): Page<EquipmentItem>

    /**
     * 分页查找指定创建者的装备
     */
    fun findByCreatedBy(createdBy: String, pageable: Pageable): Page<EquipmentItem>

    /**
     * 多条件搜索装备
     */
    @Query("""
        SELECT ei FROM EquipmentItem ei
        WHERE (:keyword IS NULL OR
               LOWER(ei.name) LIKE LOWER(CONCAT('%', :keyword, '%')))
        AND (:category IS NULL OR ei.category = :category)
        AND (:createdBy IS NULL OR ei.createdBy = :createdBy)
        AND (:minWeight IS NULL OR ei.weight >= :minWeight)
        AND (:maxWeight IS NULL OR ei.weight <= :maxWeight)
        AND (:weightUnit IS NULL OR ei.weightUnit = :weightUnit)
    """)
    fun searchEquipmentItems(
        @Param("keyword") keyword: String?,
        @Param("category") category: Int?,
        @Param("createdBy") createdBy: String?,
        @Param("minWeight") minWeight: BigDecimal?,
        @Param("maxWeight") maxWeight: BigDecimal?,
        @Param("weightUnit") weightUnit: Int?,
        pageable: Pageable
    ): Page<EquipmentItem>

    /**
     * 统计各分类的装备数量
     */
    @Query("""
        SELECT ei.category, COUNT(ei) as count
        FROM EquipmentItem ei
        GROUP BY ei.category
        ORDER BY count DESC
    """)
    fun getEquipmentCountByCategory(): List<Array<Any>>

    /**
     * 统计指定创建者的装备数量
     */
    fun countByCreatedBy(createdBy: String): Long

    /**
     * 统计指定分类的装备数量
     */
    fun countByCategory(category: Int): Long

    /**
     * 查找最轻的装备
     */
    fun findTop10ByOrderByWeightAsc(): List<EquipmentItem>

    /**
     * 查找最重的装备
     */
    fun findTop10ByOrderByWeightDesc(): List<EquipmentItem>

    /**
     * 查找最新创建的装备
     */
    fun findTop10ByOrderByCreatedAtDesc(): List<EquipmentItem>

    /**
     * 根据分类和重量范围查找装备
     */
    @Query("""
        SELECT ei FROM EquipmentItem ei
        WHERE ei.category = :category
        AND ei.weight BETWEEN :minWeight AND :maxWeight
        ORDER BY ei.weight ASC
    """)
    fun findByCategoryAndWeightRange(
        @Param("category") category: Int,
        @Param("minWeight") minWeight: BigDecimal,
        @Param("maxWeight") maxWeight: BigDecimal
    ): List<EquipmentItem>

    /**
     * 获取装备重量统计信息
     */
    @Query("""
        SELECT new map(
            COUNT(ei) as totalItems,
            AVG(ei.weight) as avgWeight,
            MIN(ei.weight) as minWeight,
            MAX(ei.weight) as maxWeight,
            SUM(ei.weight) as totalWeight
        )
        FROM EquipmentItem ei
        WHERE (:category IS NULL OR ei.category = :category)
    """)
    fun getWeightStatistics(@Param("category") category: Int?): Map<String, Any>

    /**
     * 查找相似重量的装备
     */
    @Query("""
        SELECT ei FROM EquipmentItem ei
        WHERE ei.weight BETWEEN :targetWeight - :tolerance AND :targetWeight + :tolerance
        AND ei.id != :excludeId
        ORDER BY ABS(ei.weight - :targetWeight) ASC
    """)
    fun findSimilarWeightItems(
        @Param("targetWeight") targetWeight: BigDecimal,
        @Param("tolerance") tolerance: BigDecimal,
        @Param("excludeId") excludeId: String,
        pageable: Pageable
    ): Page<EquipmentItem>
}