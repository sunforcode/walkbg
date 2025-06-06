package org.example.controller

import org.example.model.EquipmentItem
import org.example.service.EquipmentItemService
import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.*
import java.math.BigDecimal

/**
 * 装备控制器
 */
@RestController
@RequestMapping("/api/equipment")
@CrossOrigin(origins = ["*"])
class EquipmentController(
    private val equipmentItemService: EquipmentItemService
) {

    /**
     * 获取所有装备物品（分页）
     */
    @GetMapping("/items")
    fun getAllEquipmentItems(pageable: Pageable): ResponseEntity<Page<EquipmentItem>> {
        val items = equipmentItemService.getAllEquipmentItems(pageable)
        return ResponseEntity.ok(items)
    }

    /**
     * 根据ID获取装备物品
     */
    @GetMapping("/items/{id}")
    fun getEquipmentItemById(@PathVariable id: String): ResponseEntity<EquipmentItem> {
        val item = equipmentItemService.getEquipmentItemById(id)
        return if (item != null) {
            ResponseEntity.ok(item)
        } else {
            ResponseEntity.notFound().build()
        }
    }

    /**
     * 创建装备物品
     */
    @PostMapping("/items")
    fun createEquipmentItem(@RequestBody item: EquipmentItem): ResponseEntity<EquipmentItem> {
        return try {
            val createdItem = equipmentItemService.createEquipmentItem(item)
            ResponseEntity.status(HttpStatus.CREATED).body(createdItem)
        } catch (e: Exception) {
            ResponseEntity.badRequest().build()
        }
    }

    /**
     * 更新装备物品
     */
    @PutMapping("/items/{id}")
    fun updateEquipmentItem(
        @PathVariable id: String,
        @RequestBody item: EquipmentItem
    ): ResponseEntity<EquipmentItem> {
        val updatedItem = equipmentItemService.updateEquipmentItem(id, item)
        return if (updatedItem != null) {
            ResponseEntity.ok(updatedItem)
        } else {
            ResponseEntity.notFound().build()
        }
    }

    /**
     * 删除装备物品
     */
    @DeleteMapping("/items/{id}")
    fun deleteEquipmentItem(@PathVariable id: String): ResponseEntity<Void> {
        val deleted = equipmentItemService.deleteEquipmentItem(id)
        return if (deleted) {
            ResponseEntity.noContent().build()
        } else {
            ResponseEntity.notFound().build()
        }
    }

    /**
     * 根据分类获取装备物品
     */
    @GetMapping("/items/category/{category}")
    fun getEquipmentItemsByCategory(
        @PathVariable category: Int,
        pageable: Pageable
    ): ResponseEntity<Page<EquipmentItem>> {
        val items = equipmentItemService.getEquipmentItemsByCategory(category, pageable)
        return ResponseEntity.ok(items)
    }

    /**
     * 搜索装备物品
     */
    @GetMapping("/items/search")
    fun searchEquipmentItems(
        @RequestParam(required = false) keyword: String?,
        @RequestParam(required = false) category: Int?,
        @RequestParam(required = false) createdBy: String?,
        @RequestParam(required = false) minWeight: BigDecimal?,
        @RequestParam(required = false) maxWeight: BigDecimal?,
        @RequestParam(required = false) weightUnit: Int?,
        pageable: Pageable
    ): ResponseEntity<Page<EquipmentItem>> {
        val items = equipmentItemService.searchEquipmentItems(
            keyword, category, createdBy, minWeight, maxWeight, weightUnit, pageable
        )
        return ResponseEntity.ok(items)
    }

    /**
     * 根据创建者获取装备物品
     */
    @GetMapping("/items/creator/{creatorId}")
    fun getEquipmentItemsByCreator(
        @PathVariable creatorId: String,
        pageable: Pageable
    ): ResponseEntity<Page<EquipmentItem>> {
        val items = equipmentItemService.getEquipmentItemsByCreator(creatorId, pageable)
        return ResponseEntity.ok(items)
    }

    /**
     * 获取装备分类统计
     */
    @GetMapping("/category-stats")
    fun getCategoryStatistics(): ResponseEntity<List<Array<Any>>> {
        val stats = equipmentItemService.getEquipmentCountByCategory()
        return ResponseEntity.ok(stats)
    }

    /**
     * 获取重量统计
     */
    @GetMapping("/weight-stats")
    fun getWeightStatistics(
        @RequestParam(required = false) category: Int?
    ): ResponseEntity<Map<String, Any>> {
        val stats = equipmentItemService.getWeightStatistics(category)
        return ResponseEntity.ok(stats)
    }

    /**
     * 获取最新装备
     */
    @GetMapping("/latest")
    fun getLatestEquipment(): ResponseEntity<List<EquipmentItem>> {
        val items = equipmentItemService.getLatestEquipmentItems()
        return ResponseEntity.ok(items)
    }

    /**
     * 获取最轻的装备
     */
    @GetMapping("/lightest")
    fun getLightestEquipment(): ResponseEntity<List<EquipmentItem>> {
        val items = equipmentItemService.getLightestEquipmentItems()
        return ResponseEntity.ok(items)
    }

    /**
     * 获取最重的装备
     */
    @GetMapping("/heaviest")
    fun getHeaviestEquipment(): ResponseEntity<List<EquipmentItem>> {
        val items = equipmentItemService.getHeaviestEquipmentItems()
        return ResponseEntity.ok(items)
    }

    /**
     * 根据重量范围查找装备
     */
    @GetMapping("/weight-range")
    fun getEquipmentByWeightRange(
        @RequestParam minWeight: BigDecimal,
        @RequestParam maxWeight: BigDecimal
    ): ResponseEntity<List<EquipmentItem>> {
        val items = equipmentItemService.getEquipmentItemsByWeightRange(minWeight, maxWeight)
        return ResponseEntity.ok(items)
    }

    /**
     * 查找相似重量的装备
     */
    @GetMapping("/similar-weight")
    fun findSimilarWeightItems(
        @RequestParam targetWeight: BigDecimal,
        @RequestParam(defaultValue = "0.5") tolerance: BigDecimal,
        @RequestParam excludeId: String,
        pageable: Pageable
    ): ResponseEntity<Page<EquipmentItem>> {
        val items = equipmentItemService.findSimilarWeightItems(targetWeight, tolerance, excludeId, pageable)
        return ResponseEntity.ok(items)
    }

    /**
     * 按名称搜索装备
     */
    @GetMapping("/search-by-name")
    fun searchByName(@RequestParam name: String): ResponseEntity<List<EquipmentItem>> {
        val items = equipmentItemService.searchByName(name)
        return ResponseEntity.ok(items)
    }
}