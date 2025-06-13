package org.example.water.controller

import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.Parameter
import io.swagger.v3.oas.annotations.tags.Tag
import org.example.common.dto.ApiResponse
import org.example.common.dto.BaseQueryRequest
import org.example.common.exception.BusinessException
import org.example.common.util.ResponseUtil
import org.example.water.model.WaterSource
import org.example.water.service.WaterSourceService
import org.springframework.data.domain.Page
import org.springframework.http.ResponseEntity
import org.springframework.validation.annotation.Validated
import org.springframework.web.bind.annotation.*
import jakarta.validation.Valid
import java.math.BigDecimal

@RestController
@RequestMapping("/api/water-sources")
@Tag(name = "水源管理", description = "水源相关的API接口")
@Validated
class WaterSourceController(
    private val waterSourceService: WaterSourceService
) {

    @GetMapping
    @Operation(summary = "获取水源列表", description = "分页获取所有水源")
    fun getAllWaterSources(
        @Valid @ModelAttribute request: BaseQueryRequest
    ): ResponseEntity<ApiResponse<Page<WaterSource>>> {
        val waterSources = waterSourceService.getAllWaterSources(request.toPageable())
        return ResponseUtil.successPage(waterSources, "获取水源列表成功")
    }

    @GetMapping("/{id}")
    @Operation(summary = "根据ID获取水源", description = "根据水源ID获取详细信息")
    fun getWaterSourceById(
        @Parameter(description = "水源ID") @PathVariable id: String
    ): ResponseEntity<ApiResponse<WaterSource>> {
        val waterSource = waterSourceService.getWaterSourceById(id)
        return ResponseUtil.conditional(
            data = waterSource,
            successMessage = "获取水源信息成功",
            notFoundMessage = "水源不存在"
        )
    }

    @PostMapping
    @Operation(summary = "创建新水源", description = "创建一个新的水源记录")
    fun createWaterSource(
        @Valid @RequestBody waterSource: WaterSource
    ): ResponseEntity<ApiResponse<WaterSource>> {
        // 业务验证
        if (waterSource.name.isBlank()) {
            throw BusinessException.badRequest("水源名称不能为空")
        }

        val createdWaterSource = waterSourceService.createWaterSource(waterSource)
        return ResponseUtil.created(createdWaterSource, "创建水源成功")
    }

    @PutMapping("/{id}")
    @Operation(summary = "更新水源信息", description = "根据ID更新水源信息")
    fun updateWaterSource(
        @Parameter(description = "水源ID") @PathVariable id: String,
        @Valid @RequestBody waterSource: WaterSource
    ): ResponseEntity<ApiResponse<WaterSource>> {
        val updatedWaterSource = waterSourceService.updateWaterSource(id, waterSource)
        return ResponseUtil.conditional(
            data = updatedWaterSource,
            successMessage = "更新水源信息成功",
            notFoundMessage = "水源不存在"
        )
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "删除水源", description = "根据ID删除水源")
    fun deleteWaterSource(
        @Parameter(description = "水源ID") @PathVariable id: String
    ): ResponseEntity<ApiResponse<Nothing>> {
        val deleted = waterSourceService.deleteWaterSource(id)
        return ResponseUtil.conditionalOperation(
            success = deleted,
            successMessage = "删除水源成功",
            failMessage = "水源不存在或删除失败"
        )
    }

    @GetMapping("/route/{routeId}")
    @Operation(summary = "获取路线的水源", description = "获取指定路线的所有水源")
    fun getWaterSourcesByRoute(
        @Parameter(description = "路线ID") @PathVariable routeId: String,
        @Valid @ModelAttribute request: BaseQueryRequest
    ): ResponseEntity<ApiResponse<Page<WaterSource>>> {
        val waterSources = waterSourceService.getWaterSourcesByRoute(routeId, request.toPageable())
        return ResponseUtil.successPage(waterSources, "获取路线水源成功")
    }

    @GetMapping("/route/{routeId}/sorted")
    @Operation(summary = "获取路线的水源（按海拔排序）", description = "获取指定路线的所有水源，按海拔升序排列")
    fun getWaterSourcesByRouteSorted(
        @Parameter(description = "路线ID") @PathVariable routeId: String
    ): ResponseEntity<ApiResponse<List<WaterSource>>> {
        val waterSources = waterSourceService.getWaterSourcesByRouteSorted(routeId)
        return ResponseUtil.success(waterSources, "获取路线水源成功")
    }

    @GetMapping("/type/{waterType}")
    @Operation(summary = "根据水源类型获取水源", description = "根据水源类型获取水源列表")
    fun getWaterSourcesByType(
        @Parameter(description = "水源类型") @PathVariable waterType: Int,
        @Valid @ModelAttribute request: BaseQueryRequest
    ): ResponseEntity<ApiResponse<Page<WaterSource>>> {
        val waterSources = waterSourceService.getWaterSourcesByType(waterType, request.toPageable())
        return ResponseUtil.successPage(waterSources, "获取指定类型水源成功")
    }

    @GetMapping("/search")
    @Operation(summary = "复合条件搜索水源", description = "根据多个条件搜索水源")
    fun searchWaterSourcesWithFilters(
        @Parameter(description = "路线ID") @RequestParam(required = false) routeId: String?,
        @Parameter(description = "水源类型") @RequestParam(required = false) waterType: Int?,
        @Parameter(description = "水质等级") @RequestParam(required = false) waterQuality: Int?,
        @Parameter(description = "最低海拔") @RequestParam(required = false) minElevation: BigDecimal?,
        @Parameter(description = "最高海拔") @RequestParam(required = false) maxElevation: BigDecimal?,
        @Parameter(description = "是否需要处理") @RequestParam(required = false) requiresTreatment: Boolean?,
        @Parameter(description = "名称关键词") @RequestParam(required = false) name: String?,
        @Valid @ModelAttribute request: BaseQueryRequest
    ): ResponseEntity<ApiResponse<Page<WaterSource>>> {
        val waterSources = waterSourceService.searchWaterSourcesWithFilters(
            routeId, waterType, waterQuality, minElevation, maxElevation,
            requiresTreatment, name, request.toPageable()
        )
        return ResponseUtil.successPage(waterSources, "搜索水源成功")
    }

    @GetMapping("/route/{routeId}/count")
    @Operation(summary = "统计路线水源数量", description = "统计指定路线的水源数量")
    fun countWaterSourcesByRoute(
        @Parameter(description = "路线ID") @PathVariable routeId: String
    ): ResponseEntity<ApiResponse<Map<String, Long>>> {
        val count = waterSourceService.countWaterSourcesByRoute(routeId)
        return ResponseUtil.success(mapOf("count" to count), "统计水源数量成功")
    }

    @PatchMapping("/{id}/verify")
    @Operation(summary = "验证水源信息", description = "验证水源信息的准确性")
    fun verifyWaterSource(
        @Parameter(description = "水源ID") @PathVariable id: String,
        @Parameter(description = "验证者ID") @RequestParam verifiedBy: String
    ): ResponseEntity<ApiResponse<WaterSource>> {
        if (verifiedBy.isBlank()) {
            throw BusinessException.badRequest("验证者ID不能为空")
        }

        val verifiedWaterSource = waterSourceService.verifyWaterSource(id, verifiedBy)
        return ResponseUtil.conditional(
            data = verifiedWaterSource,
            successMessage = "验证水源信息成功",
            notFoundMessage = "水源不存在"
        )
    }
}