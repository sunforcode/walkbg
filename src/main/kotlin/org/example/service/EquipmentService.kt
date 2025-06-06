package org.example.service

import org.example.model.*
import org.example.model.EquipmentTemplate
import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable

/**
 * 装备服务接口
 */
interface EquipmentService {
    
    // 装备清单相关
    fun getUserEquipmentLists(userId: String, pageable: Pageable): Page<EquipmentList>
    fun getEquipmentListById(listId: String): EquipmentList?
    fun createEquipmentList(request: Map<String, Any>, userId: String, userName: String): EquipmentList
    fun createEquipmentListFromTemplate(request: Map<String, Any>, userId: String, userName: String): EquipmentList
    fun updateEquipmentList(listId: String, request: Map<String, Any>): EquipmentList?
    fun deleteEquipmentList(listId: String)
    fun getEquipmentListStatistics(listId: String): Map<String, Any>
    
    // 装备项目相关
    fun addEquipmentItemToList(listId: String, request: Map<String, Any>): EquipmentItem
    fun updateEquipmentItem(listId: String, itemId: String, request: Map<String, Any>): EquipmentItem?
    fun deleteEquipmentItem(listId: String, itemId: String)
    fun batchUpdatePreparationStatus(listId: String, request: Map<String, Any>): Map<String, Any>
    
    // 装备模板相关
    fun getEquipmentTemplates(filters: Map<String, Any>, pageable: Pageable): Page<EquipmentTemplate>
    fun getEquipmentTemplateById(templateId: String): EquipmentTemplate?
    fun createEquipmentTemplate(request: Map<String, Any>, userId: String, userName: String): EquipmentTemplate
    fun createTemplateFromList(request: Map<String, Any>, userId: String, userName: String): EquipmentTemplate
    
    // 用户装备库相关
    fun getUserEquipmentInventory(userId: String, filters: Map<String, Any>): Map<String, Any>
    fun addEquipmentToUserInventory(userId: String, request: Map<String, Any>): EquipmentItem
}