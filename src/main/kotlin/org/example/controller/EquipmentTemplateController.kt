package org.example.controller

import org.example.dto.*
import org.example.service.EMEquipmentTemplateService
import org.springframework.data.domain.PageRequest
import org.springframework.data.domain.Sort
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.*

@RestController
@RequestMapping("/api/equipment-templates")
class EMEquipmentTemplateController(private val equipmentTemplateService: EMEquipmentTemplateService) {
    
    @GetMapping
    fun getEquipmentTemplates(
        @RequestParam(required = false) type: String?,
        @RequestParam(required = false) season: String?,
        @RequestParam(required = false) isOfficial: Boolean?,
        @RequestParam(required = false) search: String?,
        @RequestParam(defaultValue = "1") page: Int,
        @RequestParam(defaultValue = "20") pageSize: Int,
        @RequestParam(defaultValue = "usageCount") sortBy: String,
        @RequestParam(defaultValue = "desc") sortOrder: String
    ): ResponseEntity<ApiResponse<Map<String, Any>>> {
        val direction = if (sortOrder.equals("asc", ignoreCase = true)) Sort.Direction.ASC else Sort.Direction.DESC
        val pageable = PageRequest.of(page - 1, pageSize, Sort.by(direction, sortBy))
        
        val templatesPage = equipmentTemplateService.getEquipmentTemplates(type, season, isOfficial, search, pageable)
        
        val response = mapOf(
            "total" to templatesPage.totalElements,
            "page" to page,
            "pageSize" to pageSize,
            "templates" to templatesPage.content
        )
        
        return ResponseEntity.ok(ApiResponse(200, "success", response))
    }
    
    @GetMapping("/{templateId}")
    fun getEquipmentTemplateById(@PathVariable templateId: String): ResponseEntity<ApiResponse<EMEquipmentTemplateDetailResponse>> {
        val template = equipmentTemplateService.getEquipmentTemplateById(templateId)
        return ResponseEntity.ok(ApiResponse(200, "success", template))
    }
    
    @PostMapping
    fun createEquipmentTemplate(
        @RequestParam userId: String,
        @RequestParam userName: String,
        @RequestBody request: EMCreateEquipmentTemplateRequest
    ): ResponseEntity<ApiResponse<EMEquipmentTemplateDetailResponse>> {
        val createdTemplate = equipmentTemplateService.createEquipmentTemplate(userId, userName, request)
        return ResponseEntity.status(HttpStatus.CREATED)
            .body(ApiResponse(200, "装备模板创建成功", createdTemplate))
    }
    
    @PostMapping("/from-list")
    fun createTemplateFromList(
        @RequestParam userId: String,
        @RequestParam userName: String,
        @RequestBody request: EMCreateTemplateFromListRequest
    ): ResponseEntity<ApiResponse<EMEquipmentTemplateDetailResponse>> {
        val createdTemplate = equipmentTemplateService.createTemplateFromList(userId, userName, request)
        return ResponseEntity.status(HttpStatus.CREATED)
            .body(ApiResponse(200, "装备模板创建成功", createdTemplate))
    }
    
    @GetMapping("/user/{userId}")
    fun getUserTemplates(
        @PathVariable userId: String,
        @RequestParam(defaultValue = "1") page: Int,
        @RequestParam(defaultValue = "20") pageSize: Int
    ): ResponseEntity<ApiResponse<Map<String, Any>>> {
        val pageable = PageRequest.of(page - 1, pageSize)
        val templatesPage = equipmentTemplateService.getUserTemplates(userId, pageable)
        
        val response = mapOf(
            "total" to templatesPage.totalElements,
            "page" to page,
            "pageSize" to pageSize,
            "templates" to templatesPage.content
        )
        
        return ResponseEntity.ok(ApiResponse(200, "success", response))
    }
    
    @GetMapping("/popular")
    fun getPopularTemplates(@RequestParam(defaultValue = "10") limit: Int): ResponseEntity<ApiResponse<List<EMEquipmentTemplateResponse>>> {
        val templates = equipmentTemplateService.getPopularTemplates(limit)
        return ResponseEntity.ok(ApiResponse(200, "success", templates))
    }
    
    @GetMapping("/highest-rated")
    fun getHighestRatedTemplates(@RequestParam(defaultValue = "10") limit: Int): ResponseEntity<ApiResponse<List<EMEquipmentTemplateResponse>>> {
        val templates = equipmentTemplateService.getHighestRatedTemplates(limit)
        return ResponseEntity.ok(ApiResponse(200, "success", templates))
    }
    
    @GetMapping("/official")
    fun getOfficialTemplates(@RequestParam(defaultValue = "10") limit: Int): ResponseEntity<ApiResponse<List<EMEquipmentTemplateResponse>>> {
        val templates = equipmentTemplateService.getOfficialTemplates(limit)
        return ResponseEntity.ok(ApiResponse(200, "success", templates))
    }
    
    @PostMapping("/{templateId}/increment-usage")
    fun incrementTemplateUsage(@PathVariable templateId: String): ResponseEntity<ApiResponse<Nothing>> {
        equipmentTemplateService.incrementTemplateUsage(templateId)
        return ResponseEntity.ok(ApiResponse(200, "模板使用次数已更新", null))
    }
}