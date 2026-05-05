package org.example.equipment.controller

import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.Parameter
import io.swagger.v3.oas.annotations.tags.Tag
import org.example.common.dto.ApiResponse
import org.example.common.exception.BusinessException
import org.example.common.util.ResponseUtil
import org.example.equipment.service.EquipmentService
import org.example.user.model.UserEquipmentItem
import org.example.user.repository.UserEquipmentItemRepository
import org.springframework.data.domain.Page
import org.springframework.data.domain.PageRequest
import org.springframework.http.ResponseEntity
import org.springframework.validation.annotation.Validated
import org.springframework.web.bind.annotation.*
import jakarta.validation.Valid
import java.math.BigDecimal

@RestController
@RequestMapping("/api/v1/user-equipment")
@Tag(name = "用户装备库管理", description = "用户装备库相关的API接口")
@Validated
class UserEquipmentInventoryController(
    private val equipmentService: EquipmentService,
    private val userEquipmentItemRepository: UserEquipmentItemRepository
) {

    @GetMapping("/{userId}")
    @Operation(summary = "获取用户装备库", description = "获取指定用户的装备库信息")
    fun getUserInventory(
        @Parameter(description = "用户ID") @PathVariable userId: String,
        @Parameter(description = "页码") @RequestParam(defaultValue = "0") page: Int,
        @Parameter(description = "每页数量") @RequestParam(defaultValue = "20") size: Int,
        @Parameter(description = "分类筛选") @RequestParam(required = false) category: Int?
    ): ResponseEntity<ApiResponse<Map<String, Any>>> {
        val pageable = PageRequest.of(page, size)
        val filters = mutableMapOf<String, Any>()
        category?.let { filters["category"] = it }
        
        val inventory = equipmentService.getUserEquipmentInventory(userId, filters)
        return ResponseUtil.success(inventory)
    }

    @GetMapping("/{userId}/items")
    @Operation(summary = "获取用户装备列表", description = "获取用户装备库中的装备列表")
    fun getUserEquipmentItems(
        @Parameter(description = "用户ID") @PathVariable userId: String,
        @Parameter(description = "页码") @RequestParam(defaultValue = "0") page: Int,
        @Parameter(description = "每页数量") @RequestParam(defaultValue = "20") size: Int,
        @Parameter(description = "分类筛选") @RequestParam(required = false) category: Int?
    ): ResponseEntity<ApiResponse<Page<Map<String, Any>>>> {
        val pageable = PageRequest.of(page, size)
        
        val items = if (category != null) {
            userEquipmentItemRepository.findByUserIdAndCategory(userId, category)
        } else {
            userEquipmentItemRepository.findByUserId(userId, pageable).content
        }
        
        val response: List<Map<String, Any>> = items.map { item ->
            mapOf(
                "userId" to item.userId,
                "equipmentItemId" to item.equipmentItemId,
                "quantity" to item.quantity,
                "notes" to (item.notes ?: "")
            )
        }
        
        return ResponseUtil.success(
            org.springframework.data.domain.PageImpl(
                response,
                pageable,
                userEquipmentItemRepository.countByUserId(userId)
            )
        )
    }

    @PostMapping("/{userId}/items")
    @Operation(summary = "添加装备到用户库", description = "向用户装备库中添加装备")
    fun addEquipmentToInventory(
        @Parameter(description = "用户ID") @PathVariable userId: String,
        @Valid @RequestBody request: Map<String, Any>
    ): ResponseEntity<ApiResponse<Map<String, Any>>> {
        val equipmentItemId = request["equipmentItemId"] as? String
            ?: throw BusinessException.badRequest("装备ID不能为空")
        
        val existing = userEquipmentItemRepository.existsByUserIdAndEquipmentItemId(userId, equipmentItemId)
        if (existing) {
            throw BusinessException.badRequest("该装备已在用户库中")
        }
        
        val quantity = request["quantity"] as? Int ?: 1
        val notes = request["notes"] as? String
        
        val userEquipment = UserEquipmentItem(
            userId = userId,
            equipmentItemId = equipmentItemId,
            quantity = quantity,
            notes = notes
        )
        
        val saved = userEquipmentItemRepository.save(userEquipment)
        
        val response: Map<String, Any> = mapOf(
            "userId" to saved.userId,
            "equipmentItemId" to saved.equipmentItemId,
            "quantity" to saved.quantity,
            "notes" to (saved.notes ?: "")
        )
        
        return ResponseUtil.created(response, "装备添加成功")
    }

    @PutMapping("/{userId}/items/{equipmentItemId}")
    @Operation(summary = "更新用户装备库中的装备", description = "更新用户装备库中的装备信息")
    fun updateUserEquipment(
        @Parameter(description = "用户ID") @PathVariable userId: String,
        @Parameter(description = "装备ID") @PathVariable equipmentItemId: String,
        @RequestBody request: Map<String, Any>
    ): ResponseEntity<ApiResponse<Map<String, Any>>> {
        val id = org.example.user.model.UserEquipmentItemId(userId, equipmentItemId)
        val item = userEquipmentItemRepository.findById(id).orElse(null)
            ?: throw BusinessException.notFound("装备不存在")
        
        val quantity = request["quantity"] as? Int ?: item.quantity
        val notes = request["notes"] as? String ?: item.notes
        
        val updated = item.copy(
            quantity = quantity,
            notes = notes
        )
        
        val saved = userEquipmentItemRepository.save(updated)
        
        val response: Map<String, Any> = mapOf(
            "userId" to saved.userId,
            "equipmentItemId" to saved.equipmentItemId,
            "quantity" to saved.quantity,
            "notes" to (saved.notes ?: "")
        )
        
        return ResponseUtil.success(response, "更新成功")
    }

    @DeleteMapping("/{userId}/items/{equipmentItemId}")
    @Operation(summary = "从用户库中移除装备", description = "从用户装备库中移除装备")
    fun removeEquipmentFromInventory(
        @Parameter(description = "用户ID") @PathVariable userId: String,
        @Parameter(description = "装备ID") @PathVariable equipmentItemId: String
    ): ResponseEntity<ApiResponse<Nothing>> {
        val deleted = userEquipmentItemRepository.deleteByUserIdAndEquipmentItemId(userId, equipmentItemId)
        if (deleted == 0L) {
            throw BusinessException.notFound("装备不存在")
        }
        return ResponseUtil.noContent("移除成功")
    }

    @GetMapping("/{userId}/statistics")
    @Operation(summary = "获取用户装备库统计", description = "获取用户装备库的统计信息")
    fun getUserInventoryStats(
        @Parameter(description = "用户ID") @PathVariable userId: String
    ): ResponseEntity<ApiResponse<Map<String, Any>>> {
        val stats = userEquipmentItemRepository.getUserEquipmentStats(userId)
        val categoryStats = userEquipmentItemRepository.getUserEquipmentCategoryStats(userId)
        
        val response = mutableMapOf<String, Any>()
        response.putAll(stats)
        response["categoryStats"] = categoryStats
        
        return ResponseUtil.success(response)
    }

    @GetMapping("/{userId}/category-stats")
    @Operation(summary = "获取用户装备分类统计", description = "获取用户装备库的分类统计信息")
    fun getUserCategoryStats(
        @Parameter(description = "用户ID") @PathVariable userId: String
    ): ResponseEntity<ApiResponse<List<Map<String, Any>>>> {
        val stats = userEquipmentItemRepository.getUserEquipmentCategoryStats(userId)
        return ResponseUtil.success(stats)
    }

    @GetMapping("/popular")
    @Operation(summary = "获取热门装备", description = "获取拥有用户最多的热门装备")
    fun getPopularEquipment(): ResponseEntity<ApiResponse<List<Map<String, Any>>>> {
        val popular = userEquipmentItemRepository.findMostPopularEquipment()
        
        val response = popular.map { item ->
            mapOf(
                "equipmentItemId" to item[0],
                "ownerCount" to item[1]
            )
        }
        
        return ResponseUtil.success(response)
    }

    @GetMapping("/{userId}/recommended")
    @Operation(summary = "获取推荐装备", description = "基于相似用户获取推荐装备")
    fun getRecommendedEquipment(
        @Parameter(description = "用户ID") @PathVariable userId: String
    ): ResponseEntity<ApiResponse<List<String>>> {
        val currentEquipment = userEquipmentItemRepository.findByUserId(userId).map { it.equipmentItemId }
        val recommended = userEquipmentItemRepository.findRecommendedEquipment(userId, currentEquipment)
        return ResponseUtil.success(recommended)
    }
}
