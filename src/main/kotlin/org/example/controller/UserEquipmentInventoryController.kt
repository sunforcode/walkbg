package org.example.controller

import org.example.dto.*
import org.example.service.EMUserEquipmentInventoryService
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.*

@RestController
@RequestMapping("/api/user-equipment-inventory")
class EMUserEquipmentInventoryController(private val userEquipmentInventoryService: EMUserEquipmentInventoryService) {
    
    @GetMapping
    fun getUserEquipmentInventory(
        @RequestParam userId: String,
        @RequestParam(required = false) category: String?,
        @RequestParam(required = false) condition: String?,
        @RequestParam(required = false) search: String?
    ): ResponseEntity<ApiResponse<EMUserEquipmentInventoryResponse>> {
        val inventory = userEquipmentInventoryService.getUserEquipmentInventory(userId, category, condition, search)
        return ResponseEntity.ok(ApiResponse(200, "success", inventory))
    }
    
    @PostMapping("/items")
    fun addEquipmentToInventory(
        @RequestParam userId: String,
        @RequestBody request: EMAddEquipmentToInventoryRequest
    ): ResponseEntity<ApiResponse<EMUserEquipmentItemResponse>> {
        val item = userEquipmentInventoryService.addEquipmentToInventory(userId, request)
        return ResponseEntity.status(HttpStatus.CREATED)
            .body(ApiResponse(200, "装备添加成功", item))
    }
    
    @PutMapping("/items/{itemId}")
    fun updateEquipmentInInventory(
        @RequestParam userId: String,
        @PathVariable itemId: String,
        @RequestBody request: EMAddEquipmentToInventoryRequest
    ): ResponseEntity<ApiResponse<EMUserEquipmentItemResponse>> {
        val item = userEquipmentInventoryService.updateEquipmentInInventory(userId, itemId, request)
        return ResponseEntity.ok(ApiResponse(200, "装备更新成功", item))
    }
    
    @DeleteMapping("/items/{itemId}")
    fun deleteEquipmentFromInventory(
        @RequestParam userId: String,
        @PathVariable itemId: String
    ): ResponseEntity<ApiResponse<Nothing>> {
        userEquipmentInventoryService.deleteEquipmentFromInventory(userId, itemId)
        return ResponseEntity.ok(ApiResponse(200, "装备删除成功", null))
    }
    
    @GetMapping("/items/{itemId}")
    fun getEquipmentItemById(@PathVariable itemId: String): ResponseEntity<ApiResponse<EMUserEquipmentItemResponse>> {
        val item = userEquipmentInventoryService.getEquipmentItemById(itemId)
        return ResponseEntity.ok(ApiResponse(200, "success", item))
    }
    
    @PostMapping("/items/{itemId}/increment-usage")
    fun incrementEquipmentUsage(@PathVariable itemId: String): ResponseEntity<ApiResponse<Nothing>> {
        userEquipmentInventoryService.incrementEquipmentUsage(itemId)
        return ResponseEntity.ok(ApiResponse(200, "装备使用次数已更新", null))
    }
}