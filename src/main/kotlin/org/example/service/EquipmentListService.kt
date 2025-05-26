package org.example.service

import org.example.dto.*
import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable

interface EMEquipmentListService {
    
    fun getUserEquipmentLists(userId: String, status: String?, type: String?, season: String?, search: String?, pageable: Pageable): Page<EMEquipmentListResponse>
    
    fun getEquipmentListById(listId: String): EMEquipmentListDetailResponse
    
    fun createEquipmentList(userId: String, userName: String, request: EMCreateEquipmentListRequest): EMEquipmentListDetailResponse
    
    fun createEquipmentListFromTemplate(userId: String, userName: String, request: EMCreateFromTemplateRequest): EMEquipmentListDetailResponse
    
    fun updateEquipmentList(listId: String, request: EMUpdateEquipmentListRequest): EMEquipmentListDetailResponse
    
    fun deleteEquipmentList(listId: String)
    
    fun getEquipmentListStats(listId: String): EMEquipmentListStatsResponse
    
    fun addEquipmentItem(listId: String, request: EMCreateEquipmentItemRequest): EMEquipmentItemResponse
    
    fun updateEquipmentItem(listId: String, itemId: String, request: EMUpdateEquipmentItemRequest): EMEquipmentItemResponse
    
    fun deleteEquipmentItem(listId: String, itemId: String)
    
    fun updatePreparationStatus(listId: String, request: EMUpdatePreparationStatusRequest): Map<String, Any>
    
    fun getEquipmentListsByRouteId(routeId: String): List<EMEquipmentListResponse>
    
    fun getEquipmentListsByTripId(tripId: String): List<EMEquipmentListResponse>
    
    fun getRecentEquipmentLists(userId: String, limit: Int): List<EMEquipmentListResponse>
}