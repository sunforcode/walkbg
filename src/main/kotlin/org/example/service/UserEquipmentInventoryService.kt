package org.example.service

import org.example.dto.EMAddEquipmentToInventoryRequest
import org.example.dto.EMUserEquipmentItemResponse
import org.example.dto.EMUserEquipmentInventoryResponse
import org.example.model.EMEquipmentCategory

interface EMUserEquipmentInventoryService {
    
    fun getUserEquipmentInventory(userId: String, category: String?, condition: String?, search: String?): EMUserEquipmentInventoryResponse
    
    fun addEquipmentToInventory(userId: String, request: EMAddEquipmentToInventoryRequest): EMUserEquipmentItemResponse
    
    fun updateEquipmentInInventory(userId: String, itemId: String, request: EMAddEquipmentToInventoryRequest): EMUserEquipmentItemResponse
    
    fun deleteEquipmentFromInventory(userId: String, itemId: String)
    
    fun getEquipmentItemById(itemId: String): EMUserEquipmentItemResponse
    
    fun incrementEquipmentUsage(itemId: String)
    
    fun getEquipmentCountByCategory(userId: String, category: EMEquipmentCategory): Long
}