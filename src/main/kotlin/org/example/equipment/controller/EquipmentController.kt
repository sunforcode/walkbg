package org.example.equipment.controller

import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.Parameter
import io.swagger.v3.oas.annotations.tags.Tag
import org.example.common.dto.ApiResponse
import org.example.common.exception.BusinessException
import org.example.common.util.ResponseUtil
import org.example.equipment.dto.EquipmentCreateRequest
import org.example.equipment.dto.EquipmentItemResponse
import org.example.equipment.model.EquipmentItem
import org.example.equipment.service.EquipmentItemService
import org.example.common.util.IdGenerator
import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import org.springframework.http.ResponseEntity
import org.springframework.validation.annotation.Validated
import org.springframework.web.bind.annotation.*
import jakarta.validation.Valid
import java.math.BigDecimal

/**
 * 装备控制器
 */
@RestController
@RequestMapping("/api/v1/equipment")
@Tag(name = "装备管理", description = "装备相关的API接口")
@Validated
class EquipmentController(
    private val equipmentItemService: EquipmentItemService
) {

    /**
     * 获取所有装备物品（分页）
     */
    @GetMapping("/items")
    @Operation(summary = "分页查询装备列表", description = "获取装备物品列表，支持分页")
    fun getAllEquipmentItems(
        pageable: Pageable
    ): ResponseEntity<ApiResponse<Page<EquipmentItemResponse>>> {
        val items = equipmentItemService.getAllEquipmentItems(pageable)
        return ResponseUtil.successPage(items.map { EquipmentItemResponse.fromEntity(it) })
    }

    /**
     * 根据ID获取装备物品
     */
    @GetMapping("/items/{id}")
    @Operation(summary = "查询装备详情", description = "根据装备ID获取详细信息")
    fun getEquipmentItemById(
        @Parameter(description = "装备ID") @PathVariable id: String
    ): ResponseEntity<ApiResponse<EquipmentItemResponse>> {
        val item = equipmentItemService.getEquipmentItemById(id)
            ?: throw BusinessException.notFound("装备不存在")
        return ResponseUtil.success(EquipmentItemResponse.fromEntity(item))
    }

    /**
     * 创建装备物品
     */
    @PostMapping("/items")
    @Operation(summary = "创建装备", description = "创建新的装备物品")
    fun createEquipmentItem(
        @Valid @RequestBody request: EquipmentCreateRequest
    ): ResponseEntity<ApiResponse<EquipmentItemResponse>> {
        val item = EquipmentItem(
            id = IdGenerator.generateId(),
            name = request.name,
            category = request.category,
            weight = request.weight,
            weightUnit = request.weightUnit,
            quantity = request.quantity
        )
        val createdItem = equipmentItemService.createEquipmentItem(item)
        return ResponseUtil.created(EquipmentItemResponse.fromEntity(createdItem), "创建成功")
    }

    /**
     * 更新装备物品
     */
    @PutMapping("/items/{id}")
    @Operation(summary = "更新装备", description = "更新指定ID的装备信息")
    fun updateEquipmentItem(
        @Parameter(description = "装备ID") @PathVariable id: String,
        @Valid @RequestBody request: EquipmentCreateRequest
    ): ResponseEntity<ApiResponse<EquipmentItemResponse>> {
        val item = EquipmentItem(
            id = id,
            name = request.name,
            category = request.category,
            weight = request.weight,
            weightUnit = request.weightUnit,
            quantity = request.quantity
        )
        val updatedItem = equipmentItemService.updateEquipmentItem(id, item)
            ?: throw BusinessException.notFound("装备不存在")
        return ResponseUtil.success(EquipmentItemResponse.fromEntity(updatedItem), "更新成功")
    }

    /**
     * 删除装备物品
     */
    @DeleteMapping("/items/{id}")
    @Operation(summary = "删除装备", description = "删除指定ID的装备")
    fun deleteEquipmentItem(
        @Parameter(description = "装备ID") @PathVariable id: String
    ): ResponseEntity<ApiResponse<Nothing>> {
        val deleted = equipmentItemService.deleteEquipmentItem(id)
        if (!deleted) {
            throw BusinessException.notFound("装备不存在")
        }
        return ResponseUtil.noContent("删除成功")
    }

    /**
     * 根据分类获取装备物品
     */
    @GetMapping("/items/category/{category}")
    @Operation(summary = "根据分类获取装备", description = "获取指定分类的装备列表")
    fun getEquipmentItemsByCategory(
        @Parameter(description = "装备分类") @PathVariable category: Int,
        pageable: Pageable
    ): ResponseEntity<ApiResponse<Page<EquipmentItemResponse>>> {
        val items = equipmentItemService.getEquipmentItemsByCategory(category, pageable)
        return ResponseUtil.successPage(items.map { EquipmentItemResponse.fromEntity(it) })
    }

    /**
     * 搜索装备物品
     */
    @GetMapping("/items/search")
    @Operation(summary = "搜索装备", description = "根据多个条件搜索装备")
    fun searchEquipmentItems(
        @Parameter(description = "关键词") @RequestParam(required = false) keyword: String?,
        @Parameter(description = "分类") @RequestParam(required = false) category: Int?,
        @Parameter(description = "创建者ID") @RequestParam(required = false) createdBy: String?,
        @Parameter(description = "最小重量") @RequestParam(required = false) minWeight: BigDecimal?,
        @Parameter(description = "最大重量") @RequestParam(required = false) maxWeight: BigDecimal?,
        @Parameter(description = "重量单位") @RequestParam(required = false) weightUnit: Int?,
        pageable: Pageable
    ): ResponseEntity<ApiResponse<Page<EquipmentItemResponse>>> {
        val items = equipmentItemService.searchEquipmentItems(
            keyword, category, createdBy, minWeight, maxWeight, weightUnit, pageable
        )
        return ResponseUtil.successPage(items.map { EquipmentItemResponse.fromEntity(it) })
    }

    /**
     * 获取装备分类统计
     */
    @GetMapping("/category-stats")
    @Operation(summary = "获取分类统计", description = "获取装备分类的统计信息")
    fun getCategoryStatistics(): ResponseEntity<ApiResponse<List<Array<Any>>>> {
        val stats = equipmentItemService.getEquipmentCountByCategory()
        return ResponseUtil.success(stats)
    }
}