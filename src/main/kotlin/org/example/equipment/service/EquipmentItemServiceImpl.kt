package org.example.equipment.service

import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import org.example.equipment.repository.EquipmentItemRepository
import org.example.equipment.model.EquipmentItem
import java.math.BigDecimal
import java.time.Instant

/**
 * 装备物品服务实现类
 */
@Service
@Transactional
class EquipmentItemServiceImpl(
    private val equipmentItemRepository: EquipmentItemRepository
) : EquipmentItemService {

    // 基础CRUD操作
    override fun getAllEquipmentItems(pageable: Pageable): Page<EquipmentItem> {
        return equipmentItemRepository.findAll(pageable)
    }

    override fun getEquipmentItemById(id: String): EquipmentItem? {
        return equipmentItemRepository.findById(id).orElse(null)
    }

    override fun createEquipmentItem(equipmentItem: EquipmentItem): EquipmentItem {
        return equipmentItemRepository.save(equipmentItem)
    }

    override fun updateEquipmentItem(id: String, equipmentItem: EquipmentItem): EquipmentItem? {
        return if (equipmentItemRepository.existsById(id)) {
            val updated = equipmentItem.copy(id = id, updatedAt = Instant.now())
            equipmentItemRepository.save(updated)
        } else {
            null
        }
    }

    override fun deleteEquipmentItem(id: String): Boolean {
        return if (equipmentItemRepository.existsById(id)) {
            equipmentItemRepository.deleteById(id)
            true
        } else {
            false
        }
    }

    // 分类查询
    override fun getEquipmentItemsByCategory(category: Int, pageable: Pageable): Page<EquipmentItem> {
        return equipmentItemRepository.findByCategory(category, pageable)
    }

    override fun getEquipmentItemsByCreator(createdBy: String, pageable: Pageable): Page<EquipmentItem> {
        return equipmentItemRepository.findByCreatedBy(createdBy, pageable)
    }

    // 搜索功能
    override fun searchEquipmentItems(
        keyword: String?,
        category: Int?,
        createdBy: String?,
        minWeight: BigDecimal?,
        maxWeight: BigDecimal?,
        weightUnit: Int?,
        pageable: Pageable
    ): Page<EquipmentItem> {
        return equipmentItemRepository.searchEquipmentItems(
            keyword, category, createdBy, minWeight, maxWeight, weightUnit, pageable
        )
    }

    override fun searchByName(name: String): List<EquipmentItem> {
        return equipmentItemRepository.findByNameContainingIgnoreCase(name)
    }

    // 重量相关查询
    override fun getEquipmentItemsByWeightRange(minWeight: BigDecimal, maxWeight: BigDecimal): List<EquipmentItem> {
        return equipmentItemRepository.findByWeightBetween(minWeight, maxWeight)
    }

    override fun getEquipmentItemsByWeightUnit(weightUnit: Int): List<EquipmentItem> {
        return equipmentItemRepository.findByWeightUnit(weightUnit)
    }

    override fun getLightestEquipmentItems(): List<EquipmentItem> {
        return equipmentItemRepository.findTop10ByOrderByWeightAsc()
    }

    override fun getHeaviestEquipmentItems(): List<EquipmentItem> {
        return equipmentItemRepository.findTop10ByOrderByWeightDesc()
    }

    // 统计功能
    override fun getEquipmentCountByCategory(): List<Array<Any>> {
        return equipmentItemRepository.getEquipmentCountByCategory()
    }

    override fun getWeightStatistics(category: Int?): Map<String, Any> {
        return equipmentItemRepository.getWeightStatistics(category)
    }

    override fun countByCreator(createdBy: String): Long {
        return equipmentItemRepository.countByCreatedBy(createdBy)
    }

    override fun countByCategory(category: Int): Long {
        return equipmentItemRepository.countByCategory(category)
    }

    // 推荐功能
    override fun findSimilarWeightItems(
        targetWeight: BigDecimal,
        tolerance: BigDecimal,
        excludeId: String,
        pageable: Pageable
    ): Page<EquipmentItem> {
        return equipmentItemRepository.findSimilarWeightItems(targetWeight, tolerance, excludeId, pageable)
    }

    // 分类和重量组合查询
    override fun findByCategoryAndWeightRange(
        category: Int,
        minWeight: BigDecimal,
        maxWeight: BigDecimal
    ): List<EquipmentItem> {
        return equipmentItemRepository.findByCategoryAndWeightRange(category, minWeight, maxWeight)
    }

    // 最新装备
    override fun getLatestEquipmentItems(): List<EquipmentItem> {
        return equipmentItemRepository.findTop10ByOrderByCreatedAtDesc()
    }

    // 验证
    override fun existsById(id: String): Boolean {
        return equipmentItemRepository.existsById(id)
    }

    override fun validateEquipmentItem(equipmentItem: EquipmentItem): Boolean {
        return equipmentItem.name.isNotBlank() && 
               equipmentItem.weight > BigDecimal.ZERO &&
               equipmentItem.quantity > 0
    }
}