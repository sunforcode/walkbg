package org.example.equipment.controller

import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.Parameter
import io.swagger.v3.oas.annotations.tags.Tag
import org.example.common.dto.ApiResponse
import org.example.common.exception.BusinessException
import org.example.common.util.ResponseUtil
import org.example.equipment.dto.EquipmentTemplateResponse
import org.example.equipment.service.EquipmentService
import org.example.equipment.service.EquipmentItemService
import org.example.equipment.service.EquipmentListItemService
import org.springframework.data.domain.Page
import org.springframework.data.domain.PageRequest
import org.springframework.http.ResponseEntity
import org.springframework.validation.annotation.Validated
import org.springframework.web.bind.annotation.*
import jakarta.validation.Valid

@RestController
@RequestMapping("/api/v1/equipment-templates")
@Tag(name = "装备模板管理", description = "装备模板相关的API接口")
@Validated
class EquipmentTemplateController(
    private val equipmentService: EquipmentService,
    private val equipmentListItemService: EquipmentListItemService
) {

    @GetMapping("")
    @Operation(summary = "分页查询装备模板列表", description = "获取装备模板列表，支持分页和筛选")
    fun getAllTemplates(
        @Parameter(description = "页码") @RequestParam(defaultValue = "0") page: Int,
        @Parameter(description = "每页数量") @RequestParam(defaultValue = "10") size: Int,
        @Parameter(description = "关键词搜索") @RequestParam(required = false) keyword: String?,
        @Parameter(description = "分类筛选") @RequestParam(required = false) category: Int?,
        @Parameter(description = "类型筛选") @RequestParam(required = false) type: Int?,
        @Parameter(description = "是否官方模板") @RequestParam(required = false) isOfficial: Boolean?
    ): ResponseEntity<ApiResponse<Page<EquipmentTemplateResponse>>> {
        val pageable = PageRequest.of(page, size)
        val filters = mutableMapOf<String, Any>()
        keyword?.let { filters["keyword"] = it }
        category?.let { filters["category"] = it }
        type?.let { filters["type"] = it }
        isOfficial?.let { filters["isOfficial"] = it }
        
        val templates = equipmentService.getEquipmentTemplates(filters, pageable)
        val responses = templates.map { EquipmentTemplateResponse.fromEntity(it) }
        
        return ResponseUtil.successPage(responses)
    }

    @GetMapping("/{id}")
    @Operation(summary = "获取装备模板详情", description = "根据模板ID获取详细信息")
    fun getTemplateById(
        @Parameter(description = "模板ID") @PathVariable id: String
    ): ResponseEntity<ApiResponse<EquipmentTemplateResponse>> {
        val template = equipmentService.getEquipmentTemplateById(id)
            ?: throw BusinessException.notFound("装备模板不存在")
        
        val response = EquipmentTemplateResponse.fromEntity(template)
        return ResponseUtil.success(response)
    }

    @PostMapping("")
    @Operation(summary = "创建装备模板", description = "创建新的装备模板")
    fun createTemplate(
        @Valid @RequestBody request: Map<String, Any>
    ): ResponseEntity<ApiResponse<EquipmentTemplateResponse>> {
        val template = equipmentService.createEquipmentTemplate(request, "admin", "管理员")
        val response = EquipmentTemplateResponse.fromEntity(template)
        return ResponseUtil.created(response, "装备模板创建成功")
    }

    @PostMapping("/from-list")
    @Operation(summary = "从装备清单创建模板", description = "基于现有装备清单创建新模板")
    fun createTemplateFromList(
        @Valid @RequestBody request: Map<String, Any>
    ): ResponseEntity<ApiResponse<EquipmentTemplateResponse>> {
        val listId = request["listId"] as? String
            ?: throw BusinessException.badRequest("清单ID不能为空")
        
        val template = equipmentService.createTemplateFromList(request, "admin", "管理员")
        val response = EquipmentTemplateResponse.fromEntity(template)
        return ResponseUtil.created(response, "模板创建成功")
    }

    @GetMapping("/official")
    @Operation(summary = "获取官方模板", description = "获取官方推荐的装备模板")
    fun getOfficialTemplates(
        @Parameter(description = "页码") @RequestParam(defaultValue = "0") page: Int,
        @Parameter(description = "每页数量") @RequestParam(defaultValue = "10") size: Int
    ): ResponseEntity<ApiResponse<Page<EquipmentTemplateResponse>>> {
        val pageable = PageRequest.of(page, size)
        val filters = mapOf("isOfficial" to true)
        val templates = equipmentService.getEquipmentTemplates(filters, pageable)
        val responses = templates.map { EquipmentTemplateResponse.fromEntity(it) }
        
        return ResponseUtil.successPage(responses)
    }

    @GetMapping("/popular")
    @Operation(summary = "获取热门模板", description = "获取使用次数最多的装备模板")
    fun getPopularTemplates(): ResponseEntity<ApiResponse<List<Map<String, Any>>>> {
        val popularStats = equipmentListItemService.getMostUsedItems(PageRequest.of(0, 10)).content
        
        val response: List<Map<String, Any>> = popularStats.map { stat ->
            mapOf(
                "equipmentItemId" to (stat["equipmentItemId"] ?: ""),
                "usageCount" to (stat["usageCount"] ?: 0)
            )
        }
        
        return ResponseUtil.success(response)
    }

    @GetMapping("/recommended")
    @Operation(summary = "获取推荐模板", description = "获取推荐的装备模板")
    fun getRecommendedTemplates(
        @Parameter(description = "清单ID（用于基于相似性推荐）") @RequestParam(required = false) listId: String?
    ): ResponseEntity<ApiResponse<List<String>>> {
        val recommended = if (listId != null) {
            equipmentListItemService.getRecommendedItems(listId)
        } else {
            emptyList<String>()
        }
        
        return ResponseUtil.success(recommended)
    }
}
