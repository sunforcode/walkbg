package org.example.equipment.service

import org.example.equipment.repository.EquipmentItemRepository
import org.example.equipment.repository.EquipmentListItemRepository
import org.example.equipment.repository.EquipmentListRepository
import org.example.equipment.repository.EquipmentTemplateRepository
import org.example.equipment.model.*
import org.example.equipment.repository.*
import org.example.user.model.UserEquipmentItem
import org.example.user.repository.UserEquipmentItemRepository
import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.math.BigDecimal
import java.time.Instant
import java.util.*

/**
 * 装备服务实现类
 */
@Service
@Transactional
class EquipmentServiceImpl(
    private val equipmentListRepository: EquipmentListRepository,
    private val equipmentItemRepository: EquipmentItemRepository,
    private val equipmentListItemRepository: EquipmentListItemRepository,
    private val userEquipmentItemRepository: UserEquipmentItemRepository,
    private val equipmentTemplateRepository: EquipmentTemplateRepository
) : EquipmentService {

    override fun getUserEquipmentLists(userId: String, pageable: Pageable): Page<EquipmentList> {
        return equipmentListRepository.findByCreatorId(userId, pageable)
    }

    override fun getEquipmentListById(listId: String): EquipmentList? {
        return equipmentListRepository.findById(listId).orElse(null)
    }

    override fun createEquipmentList(request: Map<String, Any>, userId: String, userName: String): EquipmentList {
        val equipmentList = EquipmentList(
            id = UUID.randomUUID().toString(),
            name = request["name"] as String,
            type = (request["type"] as? String)?.toIntOrNull() ?: 0, // 转换为Int类型
            creatorId = userId,
            personCount = request["personCount"] as? Int ?: 1,
            status = (request["status"] as? String)?.toIntOrNull() ?: 0 // 转换为Int类型
        )
        return equipmentListRepository.save(equipmentList)
    }

    override fun createEquipmentListFromTemplate(request: Map<String, Any>, userId: String, userName: String): EquipmentList {
        val templateId = request["templateId"] as String
        val template = equipmentTemplateRepository.findById(templateId).orElse(null)

        // 将模板的type字段（Int）直接使用，而不是枚举转换
        val typeValue = template?.type ?: 0

        val equipmentList = EquipmentList(
            id = UUID.randomUUID().toString(),
            name = request["name"] as String,
            type = typeValue, // 直接使用Int类型
            creatorId = userId,
            personCount = request["personCount"] as? Int ?: 1,
            status = 0
        )
        return equipmentListRepository.save(equipmentList)
    }

    override fun updateEquipmentList(listId: String, request: Map<String, Any>): EquipmentList? {
        val list = equipmentListRepository.findById(listId).orElse(null) ?: return null
        list.apply {
            name = request["name"] as? String ?: name
            status = (request["status"] as? String)?.toIntOrNull() ?: status // 转换为Int类型
            updatedAt = Instant.now()
        }
        return equipmentListRepository.save(list)
    }

    override fun deleteEquipmentList(listId: String) {
        if (equipmentListRepository.existsById(listId)) {
            equipmentListRepository.deleteById(listId)
        }
    }

    override fun getEquipmentListStatistics(listId: String): Map<String, Any> {
        val list = equipmentListRepository.findById(listId).orElse(null) ?: return emptyMap()
        val itemCount = equipmentListItemRepository.countByEquipmentListId(listId)
        val totalWeight = equipmentListItemRepository.calculateTotalWeight(listId) ?: java.math.BigDecimal.ZERO
        val categoryStats = equipmentListItemRepository.getEquipmentStatsByCategory(listId)

        return mapOf(
            "listId" to listId,
            "listName" to list.name,
            "itemCount" to itemCount,
            "totalWeight" to totalWeight,
            "categoryStats" to categoryStats,
            "createdAt" to list.createdAt,
            "updatedAt" to list.updatedAt
        )
    }

    override fun addEquipmentItemToList(listId: String, request: Map<String, Any>): EquipmentItem {
        // 首先创建或获取装备物品
        val equipmentItem = EquipmentItem(
            id = UUID.randomUUID().toString(),
            name = request["name"] as String,
            category = (request["category"] as? String)?.toIntOrNull() ?: 0,
            weight = (request["weight"] as? Number)?.let { BigDecimal(it.toString()) } ?: BigDecimal.ZERO,
            weightUnit = (request["weightUnit"] as? String)?.toIntOrNull() ?: 0,
            quantity = request["quantity"] as? Int ?: 1,
            createdBy = request["createdBy"] as? String
        )
        val savedItem = equipmentItemRepository.save(equipmentItem)

        // 创建装备清单物品关联
        val listItem = EquipmentListItem(
            equipmentListId = listId,
            equipmentItemId = savedItem.id,
            quantity = request["quantity"] as? Int ?: 1,
            notes = request["notes"] as? String
        )
        equipmentListItemRepository.save(listItem)

        return savedItem
    }

    override fun updateEquipmentItem(listId: String, itemId: String, request: Map<String, Any>): EquipmentItem? {
        val item = equipmentItemRepository.findById(itemId).orElse(null) ?: return null
        item.apply {
            name = request["name"] as? String ?: name
            category = (request["category"] as? String)?.toIntOrNull() ?: category
            weight = (request["weight"] as? Number)?.let { java.math.BigDecimal(it.toString()) } ?: weight
            weightUnit = (request["weightUnit"] as? String)?.toIntOrNull() ?: weightUnit
            quantity = request["quantity"] as? Int ?: quantity
            updatedAt = Instant.now()
        }

        // 同时更新关联表中的信息
        val listItem = equipmentListItemRepository.findById(EquipmentListItemId(listId, itemId)).orElse(null)
        listItem?.let {
            val updatedListItem = it.copy(
                quantity = request["quantity"] as? Int ?: it.quantity,
                notes = request["notes"] as? String ?: it.notes
            )
            equipmentListItemRepository.save(updatedListItem)
        }

        return equipmentItemRepository.save(item)
    }

    override fun deleteEquipmentItem(listId: String, itemId: String) {
        equipmentListItemRepository.deleteByEquipmentListIdAndEquipmentItemId(listId, itemId)
    }

    override fun batchUpdatePreparationStatus(listId: String, request: Map<String, Any>): Map<String, Any> {
        val itemIds = request["itemIds"] as? List<String> ?: emptyList()
        val notes = request["notes"] as? String

        var updatedCount = 0
        itemIds.forEach { itemId ->
            val listItem = equipmentListItemRepository.findById(EquipmentListItemId(listId, itemId)).orElse(null)
            listItem?.let {
                val updated = it.copy(notes = notes ?: it.notes)
                equipmentListItemRepository.save(updated)
                updatedCount++
            }
        }

        return mapOf(
            "updatedCount" to updatedCount,
            "totalRequested" to itemIds.size,
            "notes" to (notes ?: "")
        )
    }

    override fun getEquipmentTemplates(filters: Map<String, Any>, pageable: Pageable): Page<EquipmentTemplate> {
        return equipmentTemplateRepository.findAll(pageable)
    }

    override fun getEquipmentTemplateById(templateId: String): EquipmentTemplate? {
        return equipmentTemplateRepository.findById(templateId).orElse(null)
    }

    override fun createEquipmentTemplate(request: Map<String, Any>, userId: String, userName: String): EquipmentTemplate {
        val typeValue = (request["type"] as? String)?.toIntOrNull() ?: 5 // 默认为CUSTOM (5)

        val template = EquipmentTemplate(
            id = UUID.randomUUID().toString(),
            name = request["name"] as String,
            category = request["category"] as? Int ?: 0, // 默认为住宿装备
            description = request["description"] as? String,
            type = typeValue, // 使用Int类型
            creatorId = userId,
            creatorName = userName,
            isOfficial = request["isOfficial"] as? Boolean ?: false
        )
        return equipmentTemplateRepository.save(template)
    }

    override fun createTemplateFromList(request: Map<String, Any>, userId: String, userName: String): EquipmentTemplate {
        val listId = request["listId"] as String
        val list = equipmentListRepository.findById(listId).orElse(null) ?: throw RuntimeException("Equipment list not found")

        // 直接使用list.type的Int值，不需要枚举转换
        val typeValue = list.type

        val template = EquipmentTemplate(
            id = UUID.randomUUID().toString(),
            name = request["name"] as? String ?: "${list.name} 模板",
            category = request["category"] as? Int ?: 0, // 默认为住宿装备
            description = request["description"] as? String ?: "基于装备清单 ${list.name} 创建的模板",
            type = typeValue, // 直接使用Int类型
            creatorId = userId,
            creatorName = userName,
            isOfficial = false
        )
        return equipmentTemplateRepository.save(template)
    }

    override fun getUserEquipmentInventory(userId: String, filters: Map<String, Any>): Map<String, Any> {
        val userEquipment = userEquipmentItemRepository.findByUserId(userId)
        val stats = userEquipmentItemRepository.getUserEquipmentStats(userId)

        return mapOf(
            "userId" to userId,
            "equipmentItems" to userEquipment,
            "statistics" to stats
        )
    }

    override fun addEquipmentToUserInventory(userId: String, request: Map<String, Any>): EquipmentItem {
        val equipmentItemId = request["equipmentItemId"] as String
        val quantity = request["quantity"] as? Int ?: 1
        val notes = request["notes"] as? String

        // 检查装备物品是否存在
        val equipmentItem = equipmentItemRepository.findById(equipmentItemId).orElseThrow {
            RuntimeException("Equipment item not found")
        }

        // 添加到用户装备库存
        val userEquipment = UserEquipmentItem(
            userId = userId,
            equipmentItemId = equipmentItemId,
            quantity = quantity,
            notes = notes
        )
        userEquipmentItemRepository.save(userEquipment)

        return equipmentItem
    }
}