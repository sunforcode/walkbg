package org.example.equipment.service

import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import org.example.equipment.model.EquipmentItem
import java.math.BigDecimal

/**
 * 装备物品服务接口
 */
interface EquipmentItemService {

    // 基础CRUD操作
    fun getAllEquipmentItems(pageable: Pageable): Page<EquipmentItem>
    fun getEquipmentItemById(id: String): EquipmentItem?
    fun createEquipmentItem(equipmentItem: EquipmentItem): EquipmentItem
    fun updateEquipmentItem(id: String, equipmentItem: EquipmentItem): EquipmentItem?
    fun deleteEquipmentItem(id: String): Boolean

    // 分类查询
    fun getEquipmentItemsByCategory(category: Int, pageable: Pageable): Page<EquipmentItem>
    fun getEquipmentItemsByCreator(createdBy: String, pageable: Pageable): Page<EquipmentItem>

    // 搜索功能
    fun searchEquipmentItems(
        keyword: String?,
        category: Int?,
        createdBy: String?,
        minWeight: BigDecimal?,
        maxWeight: BigDecimal?,
        weightUnit: Int?,
        pageable: Pageable
    ): Page<EquipmentItem>

    fun searchByName(name: String): List<EquipmentItem>

    // 重量相关查询
    fun getEquipmentItemsByWeightRange(minWeight: BigDecimal, maxWeight: BigDecimal): List<EquipmentItem>
    fun getEquipmentItemsByWeightUnit(weightUnit: Int): List<EquipmentItem>
    fun getLightestEquipmentItems(): List<EquipmentItem>
    fun getHeaviestEquipmentItems(): List<EquipmentItem>

    // 统计功能
    fun getEquipmentCountByCategory(): List<Array<Any>>
    fun getWeightStatistics(category: Int?): Map<String, Any>
    fun countByCreator(createdBy: String): Long
    fun countByCategory(category: Int): Long

    // 推荐功能
    fun findSimilarWeightItems(
        targetWeight: BigDecimal,
        tolerance: BigDecimal,
        excludeId: String,
        pageable: Pageable
    ): Page<EquipmentItem>

    // 分类和重量组合查询
    fun findByCategoryAndWeightRange(
        category: Int,
        minWeight: BigDecimal,
        maxWeight: BigDecimal
    ): List<EquipmentItem>

    // 最新装备
    fun getLatestEquipmentItems(): List<EquipmentItem>

    // 验证
    fun existsById(id: String): Boolean
    fun validateEquipmentItem(equipmentItem: EquipmentItem): Boolean
}