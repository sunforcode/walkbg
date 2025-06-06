package org.example.repository

import org.example.model.EquipmentListItem
import org.example.model.EquipmentListItemId
import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.query.Param
import org.springframework.stereotype.Repository
import java.math.BigDecimal

/**
 * 装备清单物品关联表Repository
 */
@Repository
interface EquipmentListItemRepository : JpaRepository<EquipmentListItem, EquipmentListItemId> {

    /**
     * 根据装备清单ID查找装备项目
     */
    fun findByEquipmentListId(equipmentListId: String): List<EquipmentListItem>

    /**
     * 分页根据装备清单ID查找装备项目
     */
    fun findByEquipmentListId(equipmentListId: String, pageable: Pageable): Page<EquipmentListItem>

    /**
     * 根据装备物品ID查找所在的清单
     */
    fun findByEquipmentItemId(equipmentItemId: String): List<EquipmentListItem>

    /**
     * 分页根据装备物品ID查找所在的清单
     */
    fun findByEquipmentItemId(equipmentItemId: String, pageable: Pageable): Page<EquipmentListItem>

    /**
     * 检查装备是否在清单中
     */
    fun existsByEquipmentListIdAndEquipmentItemId(equipmentListId: String, equipmentItemId: String): Boolean

    /**
     * 统计装备清单中的物品数量
     */
    fun countByEquipmentListId(equipmentListId: String): Long

    /**
     * 统计装备物品被使用的清单数量
     */
    fun countByEquipmentItemId(equipmentItemId: String): Long

    /**
     * 计算装备清单的总重量
     */
    @Query("""
        SELECT SUM(ei.weight * eli.quantity)
        FROM EquipmentListItem eli
        JOIN EquipmentItem ei ON eli.equipmentItemId = ei.id
        WHERE eli.equipmentListId = :equipmentListId
    """)
    fun calculateTotalWeight(@Param("equipmentListId") equipmentListId: String): BigDecimal?

    /**
     * 计算装备清单的总数量
     */
    @Query("""
        SELECT SUM(eli.quantity)
        FROM EquipmentListItem eli
        WHERE eli.equipmentListId = :equipmentListId
    """)
    fun calculateTotalQuantity(@Param("equipmentListId") equipmentListId: String): Int?

    /**
     * 根据装备分类统计清单中的物品
     */
    @Query("""
        SELECT new map(
            ei.category as category,
            COUNT(eli) as itemCount,
            SUM(ei.weight * eli.quantity) as totalWeight
        )
        FROM EquipmentListItem eli
        JOIN EquipmentItem ei ON eli.equipmentItemId = ei.id
        WHERE eli.equipmentListId = :equipmentListId
        GROUP BY ei.category
    """)
    fun getEquipmentStatsByCategory(@Param("equipmentListId") equipmentListId: String): List<Map<String, Any>>

    /**
     * 查找装备清单中指定分类的物品
     */
    @Query("""
        SELECT eli FROM EquipmentListItem eli
        JOIN EquipmentItem ei ON eli.equipmentItemId = ei.id
        WHERE eli.equipmentListId = :equipmentListId AND ei.category = :category
    """)
    fun findByEquipmentListIdAndCategory(
        @Param("equipmentListId") equipmentListId: String,
        @Param("category") category: Int
    ): List<EquipmentListItem>

    /**
     * 查找装备清单中指定重量范围的物品
     */
    @Query("""
        SELECT eli FROM EquipmentListItem eli
        JOIN EquipmentItem ei ON eli.equipmentItemId = ei.id
        WHERE eli.equipmentListId = :equipmentListId
        AND ei.weight BETWEEN :minWeight AND :maxWeight
    """)
    fun findByEquipmentListIdAndWeightBetween(
        @Param("equipmentListId") equipmentListId: String,
        @Param("minWeight") minWeight: BigDecimal,
        @Param("maxWeight") maxWeight: BigDecimal
    ): List<EquipmentListItem>

    /**
     * 查找装备清单中最重的物品
     */
    @Query("""
        SELECT eli FROM EquipmentListItem eli
        JOIN EquipmentItem ei ON eli.equipmentItemId = ei.id
        WHERE eli.equipmentListId = :equipmentListId
        ORDER BY ei.weight DESC
    """)
    fun findHeaviestItems(@Param("equipmentListId") equipmentListId: String, pageable: Pageable): Page<EquipmentListItem>

    /**
     * 查找装备清单中最轻的物品
     */
    @Query("""
        SELECT eli FROM EquipmentListItem eli
        JOIN EquipmentItem ei ON eli.equipmentItemId = ei.id
        WHERE eli.equipmentListId = :equipmentListId
        ORDER BY ei.weight ASC
    """)
    fun findLightestItems(@Param("equipmentListId") equipmentListId: String, pageable: Pageable): Page<EquipmentListItem>

    /**
     * 查找最常用的装备物品
     */
    @Query("""
        SELECT new map(
            eli.equipmentItemId as equipmentItemId,
            COUNT(eli) as usageCount
        )
        FROM EquipmentListItem eli
        GROUP BY eli.equipmentItemId
        ORDER BY COUNT(eli) DESC
    """)
    fun findMostUsedItems(pageable: Pageable): Page<Map<String, Any>>

    /**
     * 获取装备物品的总使用数量
     */
    @Query("""
        SELECT SUM(eli.quantity)
        FROM EquipmentListItem eli
        WHERE eli.equipmentItemId = :equipmentItemId
    """)
    fun getTotalQuantityByItem(@Param("equipmentItemId") equipmentItemId: String): Int?

    /**
     * 获取装备物品的最近使用记录
     */
    @Query("""
        SELECT eli FROM EquipmentListItem eli
        WHERE eli.equipmentItemId = :equipmentItemId
        ORDER BY eli.equipmentList.updatedAt DESC
    """)
    fun getRecentUsageByItem(@Param("equipmentItemId") equipmentItemId: String, pageable: Pageable): Page<EquipmentListItem>

    /**
     * 获取装备物品的最近使用记录（简化版本）
     */
    @Query("""
        SELECT eli FROM EquipmentListItem eli
        WHERE eli.equipmentItemId = :equipmentItemId
        ORDER BY eli.equipmentList.updatedAt DESC
    """)
    fun getRecentUsageByItemList(@Param("equipmentItemId") equipmentItemId: String, pageable: Pageable): List<EquipmentListItem>
    /**
     * 查找未使用的装备物品
     */
    @Query("""
        SELECT ei.id FROM EquipmentItem ei
        WHERE ei.id NOT IN (SELECT DISTINCT eli.equipmentItemId FROM EquipmentListItem eli)
    """)
    fun findUnusedItems(): List<String>
    /**
     * 推荐装备物品（基于清单相似性）
     */
    @Query("""
        SELECT DISTINCT eli2.equipmentItemId
        FROM EquipmentListItem eli1
        JOIN EquipmentListItem eli2 ON eli1.equipmentListId != eli2.equipmentListId
        WHERE eli1.equipmentListId = :equipmentListId
        AND eli2.equipmentItemId NOT IN :currentItems
        AND eli1.equipmentItemId = eli2.equipmentItemId
        GROUP BY eli2.equipmentItemId
        ORDER BY COUNT(eli2.equipmentItemId) DESC
    """)
    fun findRecommendedItems(
        @Param("equipmentListId") equipmentListId: String,
        @Param("currentItems") currentItems: List<String>
    ): List<String>

    /**
     * 查找相似的装备清单
     */
    @Query("""
        SELECT DISTINCT eli2.equipmentListId
        FROM EquipmentListItem eli1
        JOIN EquipmentListItem eli2 ON eli1.equipmentItemId = eli2.equipmentItemId
        WHERE eli1.equipmentListId = :equipmentListId
        AND eli2.equipmentListId != :equipmentListId
        AND eli1.equipmentItemId IN :currentItems
        GROUP BY eli2.equipmentListId
        HAVING COUNT(eli2.equipmentListId) >= 3
        ORDER BY COUNT(eli2.equipmentListId) DESC
    """)
    fun findSimilarLists(
        @Param("equipmentListId") equipmentListId: String,
        @Param("currentItems") currentItems: List<String>
    ): List<String>

    /**
     * 查找互补的装备物品
     */
    @Query("""
        SELECT DISTINCT eli2.equipmentItemId
        FROM EquipmentListItem eli1
        JOIN EquipmentListItem eli2 ON eli1.equipmentListId = eli2.equipmentListId
        WHERE eli1.equipmentItemId IN :currentItems
        AND eli2.equipmentItemId NOT IN :currentItems
        GROUP BY eli2.equipmentItemId
        ORDER BY COUNT(eli2.equipmentItemId) DESC
    """)
    fun findComplementaryItems(@Param("currentItems") currentItems: List<String>): List<String>
    /**
     * 删除装备清单中的指定物品
     */
    fun deleteByEquipmentListIdAndEquipmentItemId(equipmentListId: String, equipmentItemId: String): Long

    /**
     * 批量删除装备清单中的所有物品
     */
    fun deleteByEquipmentListId(equipmentListId: String): Long
}