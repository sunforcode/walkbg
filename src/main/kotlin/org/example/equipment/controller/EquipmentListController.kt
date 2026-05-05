package org.example.equipment.controller

import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.Parameter
import io.swagger.v3.oas.annotations.tags.Tag
import org.example.common.dto.ApiResponse
import org.example.common.exception.BusinessException
import org.example.common.util.ResponseUtil
import org.example.equipment.dto.*
import org.example.equipment.model.EquipmentList
import org.example.equipment.model.EquipmentListItem
import org.example.equipment.service.EquipmentListItemService
import org.example.equipment.service.EquipmentService
import org.springframework.data.domain.Page
import org.springframework.data.domain.PageRequest
import org.springframework.data.domain.Pageable
import org.springframework.http.ResponseEntity
import org.springframework.validation.annotation.Validated
import org.springframework.web.bind.annotation.*
import jakarta.validation.Valid
import java.math.BigDecimal
import java.time.Instant
import java.util.*

@RestController
@RequestMapping("/api/v1/equipment-lists")
@Tag(name = "装备清单管理", description = "装备清单相关的API接口")
@Validated
class EquipmentListController(
    private val equipmentService: EquipmentService,
    private val equipmentListItemService: EquipmentListItemService
) {

    @GetMapping("")
    @Operation(summary = "分页查询装备清单列表", description = "获取装备清单列表，支持分页")
    fun getAllEquipmentLists(
        @Parameter(description = "页码") @RequestParam(defaultValue = "0") page: Int,
        @Parameter(description = "每页数量") @RequestParam(defaultValue = "10") size: Int,
        @Parameter(description = "类型筛选") @RequestParam(required = false) type: Int?,
        @Parameter(description = "状态筛选") @RequestParam(required = false) status: Int?,
        @Parameter(description = "创建者ID") @RequestParam(required = false) creatorId: String?
    ): ResponseEntity<ApiResponse<Page<EquipmentListResponse>>> {
        val pageable = PageRequest.of(page, size)
        val lists = when {
            creatorId != null && type != null -> equipmentService.getUserEquipmentLists(creatorId, pageable)
            else -> equipmentService.getUserEquipmentLists("admin", pageable)
        }
        
        val responses = lists.map { list ->
            val itemCount = equipmentListItemService.countListItems(list.id)
            EquipmentListResponse.fromEntity(list, itemCount)
        }
        
        return ResponseUtil.successPage(responses)
    }

    @GetMapping("/{id}")
    @Operation(summary = "获取装备清单详情", description = "根据清单ID获取详细信息")
    fun getEquipmentListById(
        @Parameter(description = "清单ID") @PathVariable id: String
    ): ResponseEntity<ApiResponse<EquipmentListResponse>> {
        val list = equipmentService.getEquipmentListById(id)
            ?: throw BusinessException.notFound("装备清单不存在")
        
        val itemCount = equipmentListItemService.countListItems(id)
        val response = EquipmentListResponse.fromEntity(list, itemCount)
        
        return ResponseUtil.success(response)
    }

    @PostMapping("")
    @Operation(summary = "创建装备清单", description = "创建新的装备清单")
    fun createEquipmentList(
        @Valid @RequestBody request: EquipmentListCreateRequest
    ): ResponseEntity<ApiResponse<EquipmentListResponse>> {
        val requestMap = mapOf(
            "name" to request.name,
            "type" to request.type,
            "personCount" to request.personCount,
            "description" to (request.description ?: "")
        )
        
        val list = if (request.templateId != null) {
            val templateMap = mapOf(
                "templateId" to request.templateId,
                "name" to request.name,
                "personCount" to request.personCount
            )
            equipmentService.createEquipmentListFromTemplate(templateMap, "admin", "管理员")
        } else {
            equipmentService.createEquipmentList(requestMap, "admin", "管理员")
        }
        
        val response = EquipmentListResponse.fromEntity(list, 0)
        return ResponseUtil.created(response, "装备清单创建成功")
    }

    @PutMapping("/{id}")
    @Operation(summary = "更新装备清单", description = "更新指定ID的装备清单信息")
    fun updateEquipmentList(
        @Parameter(description = "清单ID") @PathVariable id: String,
        @RequestBody request: Map<String, Any>
    ): ResponseEntity<ApiResponse<EquipmentListResponse>> {
        val updatedList = equipmentService.updateEquipmentList(id, request)
            ?: throw BusinessException.notFound("装备清单不存在")
        
        val itemCount = equipmentListItemService.countListItems(id)
        val response = EquipmentListResponse.fromEntity(updatedList, itemCount)
        
        return ResponseUtil.success(response, "更新成功")
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "删除装备清单", description = "删除指定ID的装备清单")
    fun deleteEquipmentList(
        @Parameter(description = "清单ID") @PathVariable id: String
    ): ResponseEntity<ApiResponse<Nothing>> {
        equipmentService.deleteEquipmentList(id)
        return ResponseUtil.noContent("删除成功")
    }

    @GetMapping("/{id}/statistics")
    @Operation(summary = "获取装备清单统计", description = "获取装备清单的统计信息")
    fun getEquipmentListStatistics(
        @Parameter(description = "清单ID") @PathVariable id: String
    ): ResponseEntity<ApiResponse<Map<String, Any>>> {
        val stats = equipmentService.getEquipmentListStatistics(id)
        return ResponseUtil.success(stats)
    }

    @GetMapping("/{id}/items")
    @Operation(summary = "获取清单中的装备项目", description = "获取装备清单中的所有装备项目")
    fun getEquipmentListItems(
        @Parameter(description = "清单ID") @PathVariable id: String,
        @Parameter(description = "页码") @RequestParam(defaultValue = "0") page: Int,
        @Parameter(description = "每页数量") @RequestParam(defaultValue = "20") size: Int
    ): ResponseEntity<ApiResponse<List<Map<String, Any>>>> {
        val pageable = PageRequest.of(page, size)
        val items = equipmentListItemService.getListItems(id, pageable)
        
        val itemResponses: List<Map<String, Any>> = items.content.map { listItem ->
            mapOf(
                "equipmentListId" to listItem.equipmentListId,
                "equipmentItemId" to listItem.equipmentItemId,
                "quantity" to listItem.quantity,
                "notes" to (listItem.notes ?: "")
            )
        }
        
        return ResponseUtil.success(itemResponses)
    }

    @PostMapping("/{id}/items")
    @Operation(summary = "添加装备到清单", description = "向装备清单中添加装备项目")
    fun addEquipmentItemToList(
        @Parameter(description = "清单ID") @PathVariable id: String,
        @RequestBody request: Map<String, Any>
    ): ResponseEntity<ApiResponse<Map<String, Any>>> {
        val equipmentItemId = request["equipmentItemId"] as? String
            ?: throw BusinessException.badRequest("装备ID不能为空")
        val quantity = request["quantity"] as? Int ?: 1
        val notes = request["notes"] as? String
        
        val listItem = equipmentListItemService.addItemToList(id, equipmentItemId, quantity, notes)
        
        val response: Map<String, Any> = mapOf(
            "equipmentListId" to listItem.equipmentListId,
            "equipmentItemId" to listItem.equipmentItemId,
            "quantity" to listItem.quantity,
            "notes" to (listItem.notes ?: "")
        )
        
        return ResponseUtil.created(response, "装备添加成功")
    }

    @PutMapping("/{id}/items/{itemId}")
    @Operation(summary = "更新清单中的装备", description = "更新装备清单中的装备项目")
    fun updateEquipmentItemInList(
        @Parameter(description = "清单ID") @PathVariable id: String,
        @Parameter(description = "装备项目ID") @PathVariable itemId: String,
        @RequestBody request: Map<String, Any>
    ): ResponseEntity<ApiResponse<Map<String, Any>>> {
        val quantity = request["quantity"] as? Int
        val notes = request["notes"] as? String
        
        val updated = equipmentListItemService.updateListItem(id, itemId, quantity, notes)
            ?: throw BusinessException.notFound("装备项目不存在")
        
        val response: Map<String, Any> = mapOf(
            "equipmentListId" to updated.equipmentListId,
            "equipmentItemId" to updated.equipmentItemId,
            "quantity" to updated.quantity,
            "notes" to (updated.notes ?: "")
        )
        
        return ResponseUtil.success(response, "更新成功")
    }

    @DeleteMapping("/{id}/items/{itemId}")
    @Operation(summary = "从清单中移除装备", description = "从装备清单中移除装备项目")
    fun removeEquipmentItemFromList(
        @Parameter(description = "清单ID") @PathVariable id: String,
        @Parameter(description = "装备项目ID") @PathVariable itemId: String
    ): ResponseEntity<ApiResponse<Nothing>> {
        val deleted = equipmentListItemService.removeItemFromList(id, itemId)
        if (!deleted) {
            throw BusinessException.notFound("装备项目不存在")
        }
        return ResponseUtil.noContent("移除成功")
    }

    @GetMapping("/{id}/weight-stats")
    @Operation(summary = "获取清单重量统计", description = "获取装备清单的重量统计信息")
    fun getListWeightStats(
        @Parameter(description = "清单ID") @PathVariable id: String
    ): ResponseEntity<ApiResponse<Map<String, Any>>> {
        val totalWeight = equipmentListItemService.calculateTotalWeight(id)
        val totalQuantity = equipmentListItemService.calculateTotalQuantity(id)
        val categoryStats = equipmentListItemService.getEquipmentStatsByCategory(id)
        val categoryWeightDistribution = equipmentListItemService.getCategoryWeightDistribution(id)
        
        val stats = mapOf(
            "listId" to id,
            "totalWeight" to totalWeight,
            "totalQuantity" to totalQuantity,
            "categoryStats" to categoryStats,
            "categoryWeightDistribution" to categoryWeightDistribution
        )
        
        return ResponseUtil.success(stats)
    }

    @PatchMapping("/{id}/status")
    @Operation(summary = "更新清单状态", description = "更新装备清单的状态")
    fun updateListStatus(
        @Parameter(description = "清单ID") @PathVariable id: String,
        @RequestBody request: Map<String, Int>
    ): ResponseEntity<ApiResponse<EquipmentListResponse>> {
        val status = request["status"] ?: throw BusinessException.badRequest("状态不能为空")
        val updateMap = mapOf("status" to status.toString())
        
        val updatedList = equipmentService.updateEquipmentList(id, updateMap)
            ?: throw BusinessException.notFound("装备清单不存在")
        
        val itemCount = equipmentListItemService.countListItems(id)
        val response = EquipmentListResponse.fromEntity(updatedList, itemCount)
        
        return ResponseUtil.success(response, "状态更新成功")
    }
}
