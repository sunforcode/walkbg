package org.example.service

import org.example.dto.*
import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable

interface EMEquipmentTemplateService {
    
    fun getEquipmentTemplates(type: String?, season: String?, isOfficial: Boolean?, search: String?, pageable: Pageable): Page<EMEquipmentTemplateResponse>
    
    fun getEquipmentTemplateById(templateId: String): EMEquipmentTemplateDetailResponse
    
    fun createEquipmentTemplate(userId: String, userName: String, request: EMCreateEquipmentTemplateRequest): EMEquipmentTemplateDetailResponse
    
    fun createTemplateFromList(userId: String, userName: String, request: EMCreateTemplateFromListRequest): EMEquipmentTemplateDetailResponse
    
    fun getUserTemplates(userId: String, pageable: Pageable): Page<EMEquipmentTemplateResponse>
    
    fun getPopularTemplates(limit: Int): List<EMEquipmentTemplateResponse>
    
    fun getHighestRatedTemplates(limit: Int): List<EMEquipmentTemplateResponse>
    
    fun getOfficialTemplates(limit: Int): List<EMEquipmentTemplateResponse>
    
    fun incrementTemplateUsage(templateId: String)
}