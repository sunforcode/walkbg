package org.example.route.controller

import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.Parameter
import io.swagger.v3.oas.annotations.tags.Tag
import org.example.common.dto.ApiResponse
import org.example.common.exception.BusinessException
import org.example.common.util.ResponseUtil
import org.example.route.model.Campsite
import org.example.route.service.CampsiteService
import org.springframework.data.domain.Page
import org.springframework.data.domain.PageRequest
import org.springframework.data.domain.Pageable
import org.springframework.data.domain.Sort
import org.springframework.http.ResponseEntity
import org.springframework.validation.annotation.Validated
import org.springframework.web.bind.annotation.*
import jakarta.validation.Valid
import java.math.BigDecimal

@RestController
@RequestMapping("/api/campsites")
@Tag(name = "营地管理", description = "营地相关的API接口")
@Validated
class CampsiteController(
    private val campsiteService: CampsiteService
) {

    @GetMapping
    @Operation(summary = "获取营地列表", description = "分页获取所有营地")
    fun getAllCampsites(
        @Parameter(description = "页码，从0开始") @RequestParam(defaultValue = "0") page: Int,
        @Parameter(description = "每页大小") @RequestParam(defaultValue = "10") size: Int,
        @Parameter(description = "排序字段") @RequestParam(defaultValue = "createdAt") sortBy: String,
        @Parameter(description = "排序方向") @RequestParam(defaultValue = "desc") sortDir: String
    ): ResponseEntity<ApiResponse<Page<Campsite>>> {
        val sort = if (sortDir.lowercase() == "desc") {
            Sort.by(sortBy).descending()
        } else {
            Sort.by(sortBy).ascending()
        }
        val pageable: Pageable = PageRequest.of(page, size, sort)
        val campsites = campsiteService.getAllCampsites(pageable)
        return ResponseUtil.successPage(campsites)
    }

    @GetMapping("/{id}")
    @Operation(summary = "根据ID获取营地", description = "根据营地ID获取详细信息")
    fun getCampsiteById(
        @Parameter(description = "营地ID") @PathVariable id: String
    ): ResponseEntity<ApiResponse<Campsite>> {
        val campsite = campsiteService.getCampsiteById(id)
            ?: throw BusinessException.notFound("营地不存在")
        return ResponseUtil.success(campsite)
    }

    @PostMapping
    @Operation(summary = "创建新营地", description = "创建一个新的营地记录")
    fun createCampsite(
        @Valid @RequestBody campsite: Campsite
    ): ResponseEntity<ApiResponse<Campsite>> {
        val createdCampsite = campsiteService.createCampsite(campsite)
        return ResponseUtil.created(createdCampsite, "创建成功")
    }

    @PutMapping("/{id}")
    @Operation(summary = "更新营地信息", description = "根据ID更新营地信息")
    fun updateCampsite(
        @Parameter(description = "营地ID") @PathVariable id: String,
        @Valid @RequestBody campsite: Campsite
    ): ResponseEntity<ApiResponse<Campsite>> {
        val updatedCampsite = campsiteService.updateCampsite(id, campsite)
            ?: throw BusinessException.notFound("营地不存在")
        return ResponseUtil.success(updatedCampsite, "更新成功")
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "删除营地", description = "根据ID删除营地（软删除）")
    fun deleteCampsite(
        @Parameter(description = "营地ID") @PathVariable id: String
    ): ResponseEntity<ApiResponse<Nothing>> {
        val deleted = campsiteService.deleteCampsite(id)
        if (!deleted) {
            throw BusinessException.notFound("营地不存在")
        }
        return ResponseUtil.noContent("删除成功")
    }

    @GetMapping("/route/{routeId}")
    @Operation(summary = "获取路线的营地", description = "获取指定路线的所有营地")
    fun getCampsitesByRoute(
        @Parameter(description = "路线ID") @PathVariable routeId: String,
        @Parameter(description = "页码，从0开始") @RequestParam(defaultValue = "0") page: Int,
        @Parameter(description = "每页大小") @RequestParam(defaultValue = "10") size: Int
    ): ResponseEntity<ApiResponse<Page<Campsite>>> {
        val pageable: Pageable = PageRequest.of(page, size)
        val campsites = campsiteService.getCampsitesByRoute(routeId, pageable)
        return ResponseUtil.successPage(campsites)
    }

    @GetMapping("/route/{routeId}/sorted")
    @Operation(summary = "获取路线的营地（按海拔排序）", description = "获取指定路线的所有营地，按海拔升序排列")
    fun getCampsitesByRouteSorted(
        @Parameter(description = "路线ID") @PathVariable routeId: String
    ): ResponseEntity<ApiResponse<List<Campsite>>> {
        val campsites = campsiteService.getCampsitesByRouteSorted(routeId)
        return ResponseUtil.success(campsites)
    }

    @GetMapping("/type/{campsiteType}")
    @Operation(summary = "根据类型获取营地", description = "根据营地类型获取营地列表")
    fun getCampsitesByType(
        @Parameter(description = "营地类型：0-指定营地，1-野营点，2-避难所，3-山屋") @PathVariable campsiteType: Int,
        @Parameter(description = "页码，从0开始") @RequestParam(defaultValue = "0") page: Int,
        @Parameter(description = "每页大小") @RequestParam(defaultValue = "10") size: Int
    ): ResponseEntity<ApiResponse<Page<Campsite>>> {
        val pageable: Pageable = PageRequest.of(page, size)
        val campsites = campsiteService.getCampsitesByType(campsiteType, pageable)
        return ResponseUtil.successPage(campsites)
    }

    @GetMapping("/route/{routeId}/type/{campsiteType}")
    @Operation(summary = "获取路线的特定类型营地", description = "获取指定路线的特定类型营地")
    fun getCampsitesByRouteAndType(
        @Parameter(description = "路线ID") @PathVariable routeId: String,
        @Parameter(description = "营地类型") @PathVariable campsiteType: Int,
        @Parameter(description = "页码，从0开始") @RequestParam(defaultValue = "0") page: Int,
        @Parameter(description = "每页大小") @RequestParam(defaultValue = "10") size: Int
    ): ResponseEntity<ApiResponse<Page<Campsite>>> {
        val pageable: Pageable = PageRequest.of(page, size)
        val campsites = campsiteService.getCampsitesByRouteAndType(routeId, campsiteType, pageable)
        return ResponseUtil.successPage(campsites)
    }

    @GetMapping("/search/name")
    @Operation(summary = "根据名称搜索营地", description = "根据名称关键词搜索营地")
    fun searchCampsitesByName(
        @Parameter(description = "名称关键词") @RequestParam name: String,
        @Parameter(description = "页码，从0开始") @RequestParam(defaultValue = "0") page: Int,
        @Parameter(description = "每页大小") @RequestParam(defaultValue = "10") size: Int
    ): ResponseEntity<ApiResponse<Page<Campsite>>> {
        val pageable: Pageable = PageRequest.of(page, size)
        val campsites = campsiteService.searchCampsitesByName(name, pageable)
        return ResponseUtil.successPage(campsites)
    }

    @GetMapping("/search")
    @Operation(summary = "复合条件搜索营地", description = "根据多个条件搜索营地")
    fun searchCampsitesWithFilters(
        @Parameter(description = "路线ID") @RequestParam(required = false) routeId: String?,
        @Parameter(description = "营地类型") @RequestParam(required = false) campsiteType: Int?,
        @Parameter(description = "最低海拔") @RequestParam(required = false) minElevation: BigDecimal?,
        @Parameter(description = "最高海拔") @RequestParam(required = false) maxElevation: BigDecimal?,
        @Parameter(description = "名称关键词") @RequestParam(required = false) name: String?,
        @Parameter(description = "页码，从0开始") @RequestParam(defaultValue = "0") page: Int,
        @Parameter(description = "每页大小") @RequestParam(defaultValue = "10") size: Int
    ): ResponseEntity<ApiResponse<Page<Campsite>>> {
        val pageable: Pageable = PageRequest.of(page, size)
        val campsites = campsiteService.searchCampsitesWithFilters(routeId, campsiteType, minElevation, maxElevation, name, pageable)
        return ResponseUtil.successPage(campsites)
    }

    @GetMapping("/route/{routeId}/count")
    @Operation(summary = "统计路线营地数量", description = "统计指定路线的营地数量")
    fun countCampsitesByRoute(
        @Parameter(description = "路线ID") @PathVariable routeId: String
    ): ResponseEntity<ApiResponse<Map<String, Long>>> {
        val count = campsiteService.countCampsitesByRoute(routeId)
        return ResponseUtil.success(mapOf("count" to count))
    }

    @PatchMapping("/{id}/verify")
    @Operation(summary = "验证营地信息", description = "验证营地信息的准确性")
    fun verifyCampsite(
        @Parameter(description = "营地ID") @PathVariable id: String,
        @Parameter(description = "验证者ID") @RequestParam verifiedBy: String
    ): ResponseEntity<ApiResponse<Campsite>> {
        val verifiedCampsite = campsiteService.verifyCampsite(id, verifiedBy)
            ?: throw BusinessException.notFound("营地不存在")
        return ResponseUtil.success(verifiedCampsite, "验证成功")
    }
}