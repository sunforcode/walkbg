package org.example.service

import org.example.model.EquipmentListItem
import org.example.model.EquipmentListItemId
import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import java.math.BigDecimal

/**
 * 装备清单物品服务接口
 */
interface EquipmentListItemService {

    // 基础操作
    fun addItemToList(equipmentListId: String, equipmentItemId: String, quantity: Int, notes: String?): EquipmentListItem
    fun removeItemFromList(equipmentListId: String, equipmentItemId: String): Boolean
    fun updateListItem(equipmentListId: String, equipmentItemId: String, quantity: Int?, notes: String?): EquipmentListItem?
    fun getListItem(equipmentListId: String, equipmentItemId: String): EquipmentListItem?

    // 查询操作
    fun getListItems(equipmentListId: String): List<EquipmentListItem>
    fun getListItems(equipmentListId: String, pageable: Pageable): Page<EquipmentListItem>
    fun getItemLists(equipmentItemId: String): List<EquipmentListItem>
    fun getItemLists(equipmentItemId: String, pageable: Pageable): Page<EquipmentListItem>

    // 检查操作
    fun existsInList(equipmentListId: String, equipmentItemId: String): Boolean
    fun isItemInList(equipmentListId: String, equipmentItemId: String): Boolean

    // 统计操作
    fun countListItems(equipmentListId: String): Long
    fun countItemUsage(equipmentItemId: String): Long
    fun calculateTotalWeight(equipmentListId: String): BigDecimal
    fun calculateTotalQuantity(equipmentListId: String): Int
    fun getListStatistics(equipmentListId: String): Map<String, Any>

    // 重量相关
    fun getItemsByWeightRange(equipmentListId: String, minWeight: BigDecimal, maxWeight: BigDecimal): List<EquipmentListItem>
    fun getHeaviestItems(equipmentListId: String, limit: Int): List<EquipmentListItem>
    fun getLightestItems(equipmentListId: String, limit: Int): List<EquipmentListItem>

    // 分类统计
    fun getEquipmentStatsByCategory(equipmentListId: String): List<Map<String, Any>>
    fun getItemsByCategory(equipmentListId: String, category: Int): List<EquipmentListItem>
    fun getCategoryWeightDistribution(equipmentListId: String): Map<String, BigDecimal>

    // 批量操作
    fun addMultipleItems(equipmentListId: String, items: List<Map<String, Any>>): List<EquipmentListItem>
    fun removeMultipleItems(equipmentListId: String, equipmentItemIds: List<String>): Long
    fun updateMultipleItems(equipmentListId: String, updates: List<Map<String, Any>>): List<EquipmentListItem>
    fun copyItemsToList(sourceListId: String, targetListId: String): List<EquipmentListItem>

    // 使用频率分析
    fun getMostUsedItems(): List<Map<String, Any>>
    fun getMostUsedItems(pageable: Pageable): Page<Map<String, Any>>
    fun getItemUsageStats(equipmentItemId: String): Map<String, Any>
    fun getUnusedItems(): List<String>

    // 推荐功能
    fun getRecommendedItems(equipmentListId: String): List<String>
    fun getSimilarLists(equipmentListId: String): List<String>
    fun getComplementaryItems(equipmentListId: String): List<String>

    // 验证
    fun validateListItem(equipmentListId: String, equipmentItemId: String, quantity: Int): Boolean
    fun validateListCapacity(equipmentListId: String): Boolean
}