package org.example.user.repository

import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.query.Param
import org.springframework.stereotype.Repository
import org.example.user.model.UserEquipmentItem
import org.example.user.model.UserEquipmentItemId
import java.math.BigDecimal

/**
 * 用户装备物品关联表Repository（用户装备库存）
 */
@Repository
interface UserEquipmentItemRepository : JpaRepository<UserEquipmentItem, UserEquipmentItemId> {

    /**
     * 根据用户ID查找装备库存
     */
    fun findByUserId(userId: String): List<UserEquipmentItem>

    /**
     * 根据装备物品ID查找拥有的用户
     */
    fun findByEquipmentItemId(equipmentItemId: String): List<UserEquipmentItem>

    /**
     * 检查用户是否拥有指定装备
     */
    fun existsByUserIdAndEquipmentItemId(userId: String, equipmentItemId: String): Boolean

    /**
     * 统计用户拥有的装备数量
     */
    fun countByUserId(userId: String): Long

    /**
     * 统计装备物品的拥有用户数量
     */
    fun countByEquipmentItemId(equipmentItemId: String): Long

    /**
     * 分页查找用户的装备库存
     */
    fun findByUserId(userId: String, pageable: Pageable): Page<UserEquipmentItem>

    /**
     * 根据装备分类查找用户装备
     */
    @Query("""
        SELECT uei FROM UserEquipmentItem uei
        JOIN EquipmentItem ei ON uei.equipmentItemId = ei.id
        WHERE uei.userId = :userId AND ei.category = :category
    """)
    fun findByUserIdAndCategory(
        @Param("userId") userId: String,
        @Param("category") category: Int
    ): List<UserEquipmentItem>

    /**
     * 计算用户装备的总重量
     */
    @Query("""
        SELECT SUM(ei.weight * uei.quantity)
        FROM UserEquipmentItem uei
        JOIN EquipmentItem ei ON uei.equipmentItemId = ei.id
        WHERE uei.userId = :userId
    """)
    fun calculateUserTotalEquipmentWeight(@Param("userId") userId: String): BigDecimal?

    /**
     * 根据装备分类统计用户装备
     */
    @Query("""
        SELECT ei.category, COUNT(uei), SUM(ei.weight * uei.quantity)
        FROM UserEquipmentItem uei
        JOIN EquipmentItem ei ON uei.equipmentItemId = ei.id
        WHERE uei.userId = :userId
        GROUP BY ei.category
    """)
    fun getUserEquipmentStatsByCategory(@Param("userId") userId: String): List<Array<Any>>

    /**
     * 查找用户装备库存统计信息
     */
    @Query("""
        SELECT new map(
            COUNT(uei) as totalItems,
            SUM(uei.quantity) as totalQuantity,
            SUM(ei.weight * uei.quantity) as totalWeight,
            COUNT(DISTINCT ei.category) as categoryCount
        )
        FROM UserEquipmentItem uei
        JOIN EquipmentItem ei ON uei.equipmentItemId = ei.id
        WHERE uei.userId = :userId
    """)
    fun getUserEquipmentStats(@Param("userId") userId: String): Map<String, Any>

    /**
     * 查找最受欢迎的装备（按拥有用户数排序）
     */
    @Query("""
        SELECT uei.equipmentItemId, COUNT(uei) as ownerCount
        FROM UserEquipmentItem uei
        GROUP BY uei.equipmentItemId
        ORDER BY ownerCount DESC
    """)
    fun findMostPopularEquipment(): List<Array<Any>>

    /**
     * 删除用户的指定装备
     */
    fun deleteByUserIdAndEquipmentItemId(userId: String, equipmentItemId: String): Long

    /**
     * 批量删除用户的所有装备
     */
    fun deleteByUserId(userId: String): Long

    /**
     * 分页根据装备物品ID查找拥有的用户
     */
    fun findByEquipmentItemId(equipmentItemId: String, pageable: Pageable): Page<UserEquipmentItem>

    /**
     * 获取装备物品的总使用数量
     */
    @Query("""
        SELECT SUM(uei.quantity)
        FROM UserEquipmentItem uei
        WHERE uei.equipmentItemId = :equipmentItemId
    """)
    fun getTotalQuantityByEquipmentItem(@Param("equipmentItemId") equipmentItemId: String): Int?

    /**
     * 获取用户装备分类统计信息
     */
    @Query("""
        SELECT new map(
            ei.category as category,
            COUNT(uei) as itemCount,
            SUM(uei.quantity) as totalQuantity,
            SUM(ei.weight * uei.quantity) as totalWeight
        )
        FROM UserEquipmentItem uei
        JOIN EquipmentItem ei ON uei.equipmentItemId = ei.id
        WHERE uei.userId = :userId
        GROUP BY ei.category
    """)
    fun getUserEquipmentCategoryStats(@Param("userId") userId: String): List<Map<String, Any>>

    /**
     * 推荐装备物品（基于相似用户）
     */
    @Query("""
        SELECT DISTINCT uei2.equipmentItemId
        FROM UserEquipmentItem uei1
        JOIN UserEquipmentItem uei2 ON uei1.userId != uei2.userId
        WHERE uei1.userId = :userId
        AND uei2.equipmentItemId NOT IN :currentEquipment
        AND uei1.equipmentItemId = uei2.equipmentItemId
        GROUP BY uei2.equipmentItemId
        ORDER BY COUNT(uei2.equipmentItemId) DESC
    """)
    fun findRecommendedEquipment(
        @Param("userId") userId: String,
        @Param("currentEquipment") currentEquipment: List<String>
    ): List<String>

    /**
     * 查找相似的用户（基于装备相似性）
     */
    @Query("""
        SELECT DISTINCT uei2.userId
        FROM UserEquipmentItem uei1
        JOIN UserEquipmentItem uei2 ON uei1.equipmentItemId = uei2.equipmentItemId
        WHERE uei1.userId = :userId
        AND uei2.userId != :userId
        AND uei1.equipmentItemId IN :currentEquipment
        GROUP BY uei2.userId
        HAVING COUNT(uei2.userId) >= 3
        ORDER BY COUNT(uei2.userId) DESC
    """)
    fun findSimilarUsers(
        @Param("userId") userId: String,
        @Param("currentEquipment") currentEquipment: List<String>
    ): List<String>
}