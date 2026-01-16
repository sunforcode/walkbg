package org.example.equipment.controller

import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.Parameter
import io.swagger.v3.oas.annotations.tags.Tag
import org.example.common.dto.ApiResponse
import org.example.common.exception.BusinessException
import org.example.common.util.ResponseUtil
import org.example.equipment.model.EquipmentItem
import org.example.equipment.service.EquipmentItemService
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
@RequestMapping("/api/equipment")
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
    ): ResponseEntity<ApiResponse<Page<EquipmentItem>>> {
        val items = equipmentItemService.getAllEquipmentItems(pageable)
        return ResponseUtil.successPage(items)
    }

    /**
     * 根据ID获取装备物品
     */
    @GetMapping("/items/{id}")
    @Operation(summary = "查询装备详情", description = "根据装备ID获取详细信息")
    fun getEquipmentItemById(
        @Parameter(description = "装备ID") @PathVariable id: String
    ): ResponseEntity<ApiResponse<EquipmentItem>> {
        val item = equipmentItemService.getEquipmentItemById(id)
            ?: throw BusinessException.notFound("装备不存在")
        return ResponseUtil.success(item)
    }

    /**
     * 创建装备物品
     */
    @PostMapping("/items")
    @Operation(summary = "创建装备", description = "创建新的装备物品")
    fun createEquipmentItem(
        @Valid @RequestBody item: EquipmentItem
    ): ResponseEntity<ApiResponse<EquipmentItem>> {
        val createdItem = equipmentItemService.createEquipmentItem(item)
        return ResponseUtil.created(createdItem, "创建成功")
    }

    /**
     * 更新装备物品
     */
    @PutMapping("/items/{id}")
    @Operation(summary = "更新装备", description = "更新指定ID的装备信息")
    fun updateEquipmentItem(
        @Parameter(description = "装备ID") @PathVariable id: String,
        @Valid @RequestBody item: EquipmentItem
    ): ResponseEntity<ApiResponse<EquipmentItem>> {
        val updatedItem = equipmentItemService.updateEquipmentItem(id, item)
            ?: throw BusinessException.notFound("装备不存在")
        return ResponseUtil.success(updatedItem, "更新成功")
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
    ): ResponseEntity<ApiResponse<Page<EquipmentItem>>> {
        val items = equipmentItemService.getEquipmentItemsByCategory(category, pageable)
        return ResponseUtil.successPage(items)
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
    ): ResponseEntity<ApiResponse<Page<EquipmentItem>>> {
        val items = equipmentItemService.searchEquipmentItems(
            keyword, category, createdBy, minWeight, maxWeight, weightUnit, pageable
        )
        return ResponseUtil.successPage(items)
    }

    /**
     * 根据创建者获取装备物品
     */
    @GetMapping("/items/creator/{creatorId}")
    @Operation(summary = "获取创建者的装备", description = "获取指定创建者的所有装备")
    fun getEquipmentItemsByCreator(
        @Parameter(description = "创建者ID") @PathVariable creatorId: String,
        pageable: Pageable
    ): ResponseEntity<ApiResponse<Page<EquipmentItem>>> {
        val items = equipmentItemService.getEquipmentItemsByCreator(creatorId, pageable)
        return ResponseUtil.successPage(items)
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

    /**
     * 获取重量统计
     */
    @GetMapping("/weight-stats")
    @Operation(summary = "获取重量统计", description = "获取装备重量的统计信息")
    fun getWeightStatistics(
        @Parameter(description = "分类") @RequestParam(required = false) category: Int?
    ): ResponseEntity<ApiResponse<Map<String, Any>>> {
        val stats = equipmentItemService.getWeightStatistics(category)
        return ResponseUtil.success(stats)
    }

    /**
     * 获取最新装备
     */
    @GetMapping("/latest")
    @Operation(summary = "获取最新装备", description = "获取最新添加的装备列表")
    fun getLatestEquipment(): ResponseEntity<ApiResponse<List<EquipmentItem>>> {
        val items = equipmentItemService.getLatestEquipmentItems()
        return ResponseUtil.success(items)
    }

    /**
     * 获取最轻的装备
     */
    @GetMapping("/lightest")
    @Operation(summary = "获取最轻装备", description = "获取重量最轻的装备列表")
    fun getLightestEquipment(): ResponseEntity<ApiResponse<List<EquipmentItem>>> {
        val items = equipmentItemService.getLightestEquipmentItems()
        return ResponseUtil.success(items)
    }

    /**
     * 获取最重的装备
     */
    @GetMapping("/heaviest")
    @Operation(summary = "获取最重装备", description = "获取重量最重的装备列表")
    fun getHeaviestEquipment(): ResponseEntity<ApiResponse<List<EquipmentItem>>> {
        val items = equipmentItemService.getHeaviestEquipmentItems()
        return ResponseUtil.success(items)
    }

    /**
     * 根据重量范围查找装备
     */
    @GetMapping("/weight-range")
    @Operation(summary = "按重量范围查找", description = "查找指定重量范围内的装备")
    fun getEquipmentByWeightRange(
        @Parameter(description = "最小重量") @RequestParam minWeight: BigDecimal,
        @Parameter(description = "最大重量") @RequestParam maxWeight: BigDecimal
    ): ResponseEntity<ApiResponse<List<EquipmentItem>>> {
        val items = equipmentItemService.getEquipmentItemsByWeightRange(minWeight, maxWeight)
        return ResponseUtil.success(items)
    }

    /**
     * 查找相似重量的装备
     */
    @GetMapping("/similar-weight")
    @Operation(summary = "查找相似重量装备", description = "查找重量相似的装备")
    fun findSimilarWeightItems(
        @Parameter(description = "目标重量") @RequestParam targetWeight: BigDecimal,
        @Parameter(description = "容差范围") @RequestParam(defaultValue = "0.5") tolerance: BigDecimal,
        @Parameter(description = "排除的装备ID") @RequestParam excludeId: String,
        pageable: Pageable
    ): ResponseEntity<ApiResponse<Page<EquipmentItem>>> {
        val items = equipmentItemService.findSimilarWeightItems(targetWeight, tolerance, excludeId, pageable)
        return ResponseUtil.successPage(items)
    }

    /**
     * 按名称搜索装备
     */
    @GetMapping("/search-by-name")
    @Operation(summary = "按名称搜索", description = "根据名称关键词搜索装备")
    fun searchByName(
        @Parameter(description = "装备名称") @RequestParam name: String
    ): ResponseEntity<ApiResponse<List<EquipmentItem>>> {
        val items = equipmentItemService.searchByName(name)
        return ResponseUtil.success(items)
    }
}