package org.example.equipment.service

import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import org.example.equipment.model.EquipmentListItemId
import org.example.equipment.model.EquipmentListItem
import org.example.equipment.repository.EquipmentListItemRepository
import java.math.BigDecimal

/**
 * 装备清单物品服务实现类
 */
@Service
@Transactional
class EquipmentListItemServiceImpl(
    private val equipmentListItemRepository: EquipmentListItemRepository
) : EquipmentListItemService {

    // 基础操作
    override fun addItemToList(equipmentListId: String, equipmentItemId: String, quantity: Int, notes: String?): EquipmentListItem {
        val listItem = EquipmentListItem(
            equipmentListId = equipmentListId,
            equipmentItemId = equipmentItemId,
            quantity = quantity,
            notes = notes
        )
        return equipmentListItemRepository.save(listItem)
    }

    override fun removeItemFromList(equipmentListId: String, equipmentItemId: String): Boolean {
        return equipmentListItemRepository.deleteByEquipmentListIdAndEquipmentItemId(equipmentListId, equipmentItemId) > 0
    }

    override fun updateListItem(equipmentListId: String, equipmentItemId: String, quantity: Int?, notes: String?): EquipmentListItem? {
        val listItem = equipmentListItemRepository.findById(EquipmentListItemId(equipmentListId, equipmentItemId)).orElse(null)
        return listItem;
    }

    override fun getListItem(equipmentListId: String, equipmentItemId: String): EquipmentListItem? {
        return equipmentListItemRepository.findById(EquipmentListItemId(equipmentListId, equipmentItemId)).orElse(null)
    }

    // 查询操作
    override fun getListItems(equipmentListId: String): List<EquipmentListItem> {
        return equipmentListItemRepository.findByEquipmentListId(equipmentListId)
    }

    override fun getListItems(equipmentListId: String, pageable: Pageable): Page<EquipmentListItem> {
        return equipmentListItemRepository.findByEquipmentListId(equipmentListId, pageable)
    }

    override fun getItemLists(equipmentItemId: String): List<EquipmentListItem> {
        return equipmentListItemRepository.findByEquipmentItemId(equipmentItemId)
    }

    override fun getItemLists(equipmentItemId: String, pageable: Pageable): Page<EquipmentListItem> {
        return equipmentListItemRepository.findByEquipmentItemId(equipmentItemId, pageable)
    }

    // 检查操作
    override fun existsInList(equipmentListId: String, equipmentItemId: String): Boolean {
        return equipmentListItemRepository.existsByEquipmentListIdAndEquipmentItemId(equipmentListId, equipmentItemId)
    }

    override fun isItemInList(equipmentListId: String, equipmentItemId: String): Boolean {
        return equipmentListItemRepository.existsByEquipmentListIdAndEquipmentItemId(equipmentListId, equipmentItemId)
    }

    // 统计操作
    override fun countListItems(equipmentListId: String): Long {
        return equipmentListItemRepository.countByEquipmentListId(equipmentListId)
    }

    override fun countItemUsage(equipmentItemId: String): Long {
        return equipmentListItemRepository.countByEquipmentItemId(equipmentItemId)
    }

    override fun calculateTotalWeight(equipmentListId: String): BigDecimal {
        return equipmentListItemRepository.calculateTotalWeight(equipmentListId) ?: BigDecimal.ZERO
    }

    override fun calculateTotalQuantity(equipmentListId: String): Int {
        return equipmentListItemRepository.calculateTotalQuantity(equipmentListId) ?: 0
    }

    override fun getListStatistics(equipmentListId: String): Map<String, Any> {
        val itemCount = countListItems(equipmentListId)
        val totalWeight = calculateTotalWeight(equipmentListId)
        val totalQuantity = calculateTotalQuantity(equipmentListId)
        val categoryStats = getEquipmentStatsByCategory(equipmentListId)
        val avgWeight = if (itemCount > 0) totalWeight.divide(BigDecimal(itemCount), 2, BigDecimal.ROUND_HALF_UP) else BigDecimal.ZERO
        val heaviestItem = getHeaviestItems(equipmentListId, 1).firstOrNull()
        val lightestItem = getLightestItems(equipmentListId, 1).firstOrNull()
        
        return mapOf(
            "itemCount" to itemCount,
            "totalWeight" to totalWeight,
            "totalQuantity" to totalQuantity,
            "averageWeight" to avgWeight,
            "categoryStats" to categoryStats,
            "heaviestItem" to (heaviestItem?.toString() ?: "无"),
            "lightestItem" to (lightestItem?.toString() ?: "无")
        )
    }

    // 重量相关
    override fun getItemsByWeightRange(equipmentListId: String, minWeight: BigDecimal, maxWeight: BigDecimal): List<EquipmentListItem> {
        return equipmentListItemRepository.findByEquipmentListIdAndWeightBetween(equipmentListId, minWeight, maxWeight)
    }

    override fun getHeaviestItems(equipmentListId: String, limit: Int): List<EquipmentListItem> {
        return equipmentListItemRepository.findHeaviestItems(equipmentListId, Pageable.ofSize(limit)).content
    }

    override fun getLightestItems(equipmentListId: String, limit: Int): List<EquipmentListItem> {
        return equipmentListItemRepository.findLightestItems(equipmentListId, Pageable.ofSize(limit)).content
    }

    // 分类统计
    override fun getEquipmentStatsByCategory(equipmentListId: String): List<Map<String, Any>> {
        return equipmentListItemRepository.getEquipmentStatsByCategory(equipmentListId)
    }

    override fun getItemsByCategory(equipmentListId: String, category: Int): List<EquipmentListItem> {
        return equipmentListItemRepository.findByEquipmentListIdAndCategory(equipmentListId, category)
    }

    override fun getCategoryWeightDistribution(equipmentListId: String): Map<String, BigDecimal> {
        val categoryStats = getEquipmentStatsByCategory(equipmentListId)
        return categoryStats.associate { stat ->
            val categoryName = when (stat["category"] as Int) {
                0 -> "住宿装备"
                1 -> "饮食装备"
                2 -> "保暖装备"
                3 -> "背包装备"
                4 -> "导航装备"
                5 -> "照明装备"
                6 -> "急救装备"
                7 -> "工具装备"
                8 -> "电子装备"
                9 -> "个人护理"
                else -> "其他装备"
            }
            categoryName to (stat["totalWeight"] as BigDecimal)
        }
    }

    // 批量操作
    override fun addMultipleItems(equipmentListId: String, items: List<Map<String, Any>>): List<EquipmentListItem> {
        val listItems = items.map { item ->
            EquipmentListItem(
                equipmentListId = equipmentListId,
                equipmentItemId = item["equipmentItemId"] as String,
                quantity = item["quantity"] as? Int ?: 1,
                notes = item["notes"] as? String
            )
        }
        return equipmentListItemRepository.saveAll(listItems)
    }

    override fun removeMultipleItems(equipmentListId: String, equipmentItemIds: List<String>): Long {
        var removedCount = 0L
        equipmentItemIds.forEach { equipmentItemId ->
            if (removeItemFromList(equipmentListId, equipmentItemId)) {
                removedCount++
            }
        }
        return removedCount
    }

    override fun updateMultipleItems(equipmentListId: String, updates: List<Map<String, Any>>): List<EquipmentListItem> {
        val updatedItems = mutableListOf<EquipmentListItem>()
        updates.forEach { update ->
            val equipmentItemId = update["equipmentItemId"] as String
            val quantity = update["quantity"] as? Int
            val notes = update["notes"] as? String
            
            updateListItem(equipmentListId, equipmentItemId, quantity, notes)?.let {
                updatedItems.add(it)
            }
        }
        return updatedItems
    }

    override fun copyItemsToList(sourceListId: String, targetListId: String): List<EquipmentListItem> {
        val sourceItems = getListItems(sourceListId)
        val targetItems = sourceItems.map { sourceItem ->
            EquipmentListItem(
                equipmentListId = targetListId,
                equipmentItemId = sourceItem.equipmentItemId,
                quantity = sourceItem.quantity,
                notes = sourceItem.notes
            )
        }
        return equipmentListItemRepository.saveAll(targetItems)
    }

    // 使用频率分析
    override fun getMostUsedItems(): List<Map<String, Any>> {
        return equipmentListItemRepository.findMostUsedItems(Pageable.ofSize(20)).content
    }

    override fun getMostUsedItems(pageable: Pageable): Page<Map<String, Any>> {
        return equipmentListItemRepository.findMostUsedItems(pageable)
    }

    override fun getItemUsageStats(equipmentItemId: String): Map<String, Any> {
        val usageCount = countItemUsage(equipmentItemId)
        val totalQuantity = equipmentListItemRepository.getTotalQuantityByItem(equipmentItemId) ?: 0
        val avgQuantity = if (usageCount > 0) totalQuantity.toDouble() / usageCount else 0.0
        val recentUsage = equipmentListItemRepository.getRecentUsageByItemList(equipmentItemId, Pageable.ofSize(5)).take(5)
        
        return mapOf(
            "equipmentItemId" to equipmentItemId,
            "usageCount" to usageCount,
            "totalQuantity" to totalQuantity,
            "averageQuantity" to avgQuantity,
            "recentUsage" to recentUsage,
            "popularityRank" to getItemPopularityRank(equipmentItemId)
        )
    }

    override fun getUnusedItems(): List<String> {
        return equipmentListItemRepository.findUnusedItems()
    }

    // 推荐功能
    override fun getRecommendedItems(equipmentListId: String): List<String> {
        val currentItems = getListItems(equipmentListId).map { it.equipmentItemId }
        return equipmentListItemRepository.findRecommendedItems(equipmentListId, currentItems)
    }

    override fun getSimilarLists(equipmentListId: String): List<String> {
        val currentItems = getListItems(equipmentListId).map { it.equipmentItemId }
        return equipmentListItemRepository.findSimilarLists(equipmentListId, currentItems)
    }

    override fun getComplementaryItems(equipmentListId: String): List<String> {
        val currentItems = getListItems(equipmentListId).map { it.equipmentItemId }
        return equipmentListItemRepository.findComplementaryItems(currentItems)
    }

    // 验证
    override fun validateListItem(equipmentListId: String, equipmentItemId: String, quantity: Int): Boolean {
        return equipmentListId.isNotBlank() && 
               equipmentItemId.isNotBlank() && 
               quantity > 0 && 
               !existsInList(equipmentListId, equipmentItemId)
    }

    override fun validateListCapacity(equipmentListId: String): Boolean {
        val itemCount = countListItems(equipmentListId)
        val maxCapacity = 200 // 假设最大容量为200个物品
        return itemCount < maxCapacity
    }

    // 私有辅助方法
    private fun getItemPopularityRank(equipmentItemId: String): Int {
        val mostUsedItems = getMostUsedItems()
        return mostUsedItems.indexOfFirst { 
            it["equipmentItemId"] == equipmentItemId 
        } + 1
    }
}