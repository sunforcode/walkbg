package org.example.controller

import org.example.dto.*
import org.example.service.EMEquipmentListService
import org.springframework.data.domain.PageRequest
import org.springframework.data.domain.Sort
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.*

@RestController
@RequestMapping("/api/equipment-lists")
class EMEquipmentListController(private val equipmentListService: EMEquipmentListService) {
    
    @GetMapping
    fun getUserEquipmentLists(
        @RequestParam userId: String,
        @RequestParam(required = false) status: String?,
        @RequestParam(required = false) type: String?,
        @RequestParam(required = false) season: String?,
        @RequestParam(required = false) search: String?,
        @RequestParam(defaultValue = "1") page: Int,
        @RequestParam(defaultValue = "20") pageSize: Int,
        @RequestParam(defaultValue = "createdAt") sortBy: String,
        @RequestParam(defaultValue = "desc") sortOrder: String
    ): ResponseEntity<ApiResponse<Map<String, Any>>> {
        val direction = if (sortOrder.equals("asc", ignoreCase = true)) Sort.Direction.ASC else Sort.Direction.DESC
        val pageable = PageRequest.of(page - 1, pageSize, Sort.by(direction, sortBy))
        
        val listsPage = equipmentListService.getUserEquipmentLists(userId, status, type, season, search, pageable)
        
        val response = mapOf(
            "total" to listsPage.totalElements,
            "page" to page,
            "pageSize" to pageSize,
            "lists" to listsPage.content
        )
        
        return ResponseEntity.ok(ApiResponse(200, "success", response))
    }
    
    @GetMapping("/{listId}")
    fun getEquipmentListById(@PathVariable listId: String): ResponseEntity<ApiResponse<EMEquipmentListDetailResponse>> {
        val list = equipmentListService.getEquipmentListById(listId)
        return ResponseEntity.ok(ApiResponse(200, "success", list))
    }
    
    @PostMapping
    fun createEquipmentList(
        @RequestParam userId: String,
        @RequestParam userName: String,
        @RequestBody request: EMCreateEquipmentListRequest
    ): ResponseEntity<ApiResponse<EMEquipmentListDetailResponse>> {
        val createdList = equipmentListService.createEquipmentList(userId, userName, request)
        return ResponseEntity.status(HttpStatus.CREATED)
            .body(ApiResponse(200, "装备清单创建成功", createdList))
    }
    
    @PostMapping("/from-template")
    fun createEquipmentListFromTemplate(
        @RequestParam userId: String,
        @RequestParam userName: String,
        @RequestBody request: EMCreateFromTemplateRequest
    ): ResponseEntity<ApiResponse<EMEquipmentListDetailResponse>> {
        val createdList = equipmentListService.createEquipmentListFromTemplate(userId, userName, request)
        return ResponseEntity.status(HttpStatus.CREATED)
            .body(ApiResponse(200, "装备清单创建成功", createdList))
    }
    
    @PutMapping("/{listId}")
    fun updateEquipmentList(
        @PathVariable listId: String,
        @RequestBody request: EMUpdateEquipmentListRequest
    ): ResponseEntity<ApiResponse<EMEquipmentListDetailResponse>> {
        val updatedList = equipmentListService.updateEquipmentList(listId, request)
        return ResponseEntity.ok(ApiResponse(200, "装备清单更新成功", updatedList))
    }
    
    @DeleteMapping("/{listId}")
    fun deleteEquipmentList(@PathVariable listId: String): ResponseEntity<ApiResponse<Nothing>> {
        equipmentListService.deleteEquipmentList(listId)
        return ResponseEntity.ok(ApiResponse(200, "装备清单删除成功", null))
    }
    
    @GetMapping("/{listId}/stats")
    fun getEquipmentListStats(@PathVariable listId: String): ResponseEntity<ApiResponse<EMEquipmentListStatsResponse>> {
        val stats = equipmentListService.getEquipmentListStats(listId)
        return ResponseEntity.ok(ApiResponse(200, "success", stats))
    }
    
    @PostMapping("/{listId}/items")
    fun addEquipmentItem(
        @PathVariable listId: String,
        @RequestBody request: EMCreateEquipmentItemRequest
    ): ResponseEntity<ApiResponse<EMEquipmentItemResponse>> {
        val item = equipmentListService.addEquipmentItem(listId, request)
        return ResponseEntity.status(HttpStatus.CREATED)
            .body(ApiResponse(200, "装备项目添加成功", item))
    }
    
    @PutMapping("/{listId}/items/{itemId}")
    fun updateEquipmentItem(
        @PathVariable listId: String,
        @PathVariable itemId: String,
        @RequestBody request: EMUpdateEquipmentItemRequest
    ): ResponseEntity<ApiResponse<EMEquipmentItemResponse>> {
        val item = equipmentListService.updateEquipmentItem(listId, itemId, request)
        return ResponseEntity.ok(ApiResponse(200, "装备项目更新成功", item))
    }
    
    @DeleteMapping("/{listId}/items/{itemId}")
    fun deleteEquipmentItem(
        @PathVariable listId: String,
        @PathVariable itemId: String
    ): ResponseEntity<ApiResponse<Nothing>> {
        equipmentListService.deleteEquipmentItem(listId, itemId)
        return ResponseEntity.ok(ApiResponse(200, "装备项目删除成功", null))
    }
    
    @PutMapping("/{listId}/items/preparation")
    fun updatePreparationStatus(
        @PathVariable listId: String,
        @RequestBody request: EMUpdatePreparationStatusRequest
    ): ResponseEntity<ApiResponse<Map<String, Any>>> {
        val result = equipmentListService.updatePreparationStatus(listId, request)
        return ResponseEntity.ok(ApiResponse(200, "装备准备状态更新成功", result))
    }
    
    @GetMapping("/by-route/{routeId}")
    fun getEquipmentListsByRouteId(@PathVariable routeId: String): ResponseEntity<ApiResponse<List<EMEquipmentListResponse>>> {
        val lists = equipmentListService.getEquipmentListsByRouteId(routeId)
        return ResponseEntity.ok(ApiResponse(200, "success", lists))
    }
    
    @GetMapping("/by-trip/{tripId}")
    fun getEquipmentListsByTripId(@PathVariable tripId: String): ResponseEntity<ApiResponse<List<EMEquipmentListResponse>>> {
        val lists = equipmentListService.getEquipmentListsByTripId(tripId)
        return ResponseEntity.ok(ApiResponse(200, "success", lists))
    }
    
    @GetMapping("/recent")
    fun getRecentEquipmentLists(
        @RequestParam userId: String,
        @RequestParam(defaultValue = "5") limit: Int
    ): ResponseEntity<ApiResponse<List<EMEquipmentListResponse>>> {
        val lists = equipmentListService.getRecentEquipmentLists(userId, limit)
        return ResponseEntity.ok(ApiResponse(200, "success", lists))
    }
}