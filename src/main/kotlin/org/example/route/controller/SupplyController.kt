package org.example.route.controller

import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.Parameter
import io.swagger.v3.oas.annotations.tags.Tag
import org.example.common.dto.ApiResponse
import org.example.common.dto.BaseQueryRequest
import org.example.common.exception.BusinessException
import org.example.common.util.ResponseUtil
import org.example.meal.service.SupplyService
import org.example.route.model.Supply
import org.springframework.data.domain.Page
import org.springframework.http.ResponseEntity
import org.springframework.validation.annotation.Validated
import org.springframework.web.bind.annotation.*
import jakarta.validation.Valid
import java.math.BigDecimal

@RestController
@RequestMapping("/api/supplies")
@Tag(name = "补给管理", description = "补给点相关的API接口")
@Validated
class SupplyController(
    private val supplyService: SupplyService
) {

    @GetMapping
    @Operation(summary = "获取补给点列表", description = "分页获取所有补给点")
    fun getAllSupplies(
        @Valid @ModelAttribute request: BaseQueryRequest
    ): ResponseEntity<ApiResponse<Page<Supply>>> {
        val supplies = supplyService.getAllSupplies(request.toPageable())
        return ResponseUtil.successPage(supplies, "获取补给点列表成功")
    }
    @GetMapping("/{id}")
    @Operation(summary = "根据ID获取补给点", description = "根据补给点ID获取详细信息")
    fun getSupplyById(
        @Parameter(description = "补给点ID") @PathVariable id: String
    ): ResponseEntity<ApiResponse<Supply>> {
        val supply = supplyService.getSupplyById(id)
        return ResponseUtil.conditional(
            data = supply,
            successMessage = "获取补给点信息成功",
            notFoundMessage = "补给点不存在"
        )
    }

    @PostMapping
    @Operation(summary = "创建新补给点", description = "创建一个新的补给点记录")
    fun createSupply(
        @Valid @RequestBody supply: Supply
    ): ResponseEntity<ApiResponse<Supply>> {
        // 业务验证
        if (supply.name.isBlank()) {
            throw BusinessException.badRequest("补给点名称不能为空")
        }

        val createdSupply = supplyService.createSupply(supply)
        return ResponseUtil.created(createdSupply, "创建补给点成功")
    }

    @PutMapping("/{id}")
    @Operation(summary = "更新补给点信息", description = "根据ID更新补给点信息")
    fun updateSupply(
        @Parameter(description = "补给点ID") @PathVariable id: String,
        @Valid @RequestBody supply: Supply
    ): ResponseEntity<ApiResponse<Supply>> {
        val updatedSupply = supplyService.updateSupply(id, supply)
        return ResponseUtil.conditional(
            data = updatedSupply,
            successMessage = "更新补给点信息成功",
            notFoundMessage = "补给点不存在"
        )
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "删除补给点", description = "根据ID删除补给点")
    fun deleteSupply(
        @Parameter(description = "补给点ID") @PathVariable id: String
    ): ResponseEntity<ApiResponse<Nothing>> {
        val deleted = supplyService.deleteSupply(id)
        return ResponseUtil.conditionalOperation(
            success = deleted,
            successMessage = "删除补给点成功",
            failMessage = "补给点不存在或删除失败"
        )
    }

    @GetMapping("/route/{routeId}")
    @Operation(summary = "获取路线的补给点", description = "获取指定路线的所有补给点")
    fun getSuppliesByRoute(
        @Parameter(description = "路线ID") @PathVariable routeId: String,
        @Valid @ModelAttribute request: BaseQueryRequest
    ): ResponseEntity<ApiResponse<Page<Supply>>> {
        val supplies = supplyService.getSuppliesByRoute(routeId, request.toPageable())
        return ResponseUtil.successPage(supplies, "获取路线补给点成功")
    }

    @GetMapping("/route/{routeId}/sorted")
    @Operation(summary = "获取路线的补给点（按海拔排序）", description = "获取指定路线的所有补给点，按海拔升序排列")
    fun getSuppliesByRouteSorted(
        @Parameter(description = "路线ID") @PathVariable routeId: String
    ): ResponseEntity<ApiResponse<List<Supply>>> {
        val supplies = supplyService.getSuppliesByRouteSorted(routeId)
        return ResponseUtil.success(supplies, "获取路线补给点成功")
    }

    @GetMapping("/search")
    @Operation(summary = "复合条件搜索补给点", description = "根据多个条件搜索补给点")
    fun searchSuppliesWithFilters(
        @Parameter(description = "路线ID") @RequestParam(required = false) routeId: String?,
        @Parameter(description = "补给类型") @RequestParam(required = false) supplyType: Int?,
        @Parameter(description = "最低海拔") @RequestParam(required = false) minElevation: BigDecimal?,
        @Parameter(description = "最高海拔") @RequestParam(required = false) maxElevation: BigDecimal?,
        @Parameter(description = "名称关键词") @RequestParam(required = false) name: String?,
        @Valid @ModelAttribute request: BaseQueryRequest
    ): ResponseEntity<ApiResponse<Page<Supply>>> {
        val supplies = supplyService.searchSuppliesWithFilters(
            routeId, supplyType, null, minElevation, maxElevation, name, request.toPageable()
        )
        return ResponseUtil.successPage(supplies, "搜索补给点成功")
    }

    @GetMapping("/route/{routeId}/count")
    @Operation(summary = "统计路线补给点数量", description = "统计指定路线的补给点数量")
    fun countSuppliesByRoute(
        @Parameter(description = "路线ID") @PathVariable routeId: String
    ): ResponseEntity<ApiResponse<Map<String, Long>>> {
        val count = supplyService.countSuppliesByRoute(routeId)
        return ResponseUtil.success(mapOf("count" to count), "统计补给点数量成功")
    }

    @PatchMapping("/{id}/verify")
    @Operation(summary = "更新补给点验证信息", description = "更新补给点的验证信息")
    fun updateSupplyVerification(
        @Parameter(description = "补给点ID") @PathVariable id: String,
        @Parameter(description = "验证者用户ID") @RequestParam verifiedBy: String
    ): ResponseEntity<ApiResponse<Supply>> {
        if (verifiedBy.isBlank()) {
            throw BusinessException.badRequest("验证者用户ID不能为空")
        }

        val verifiedSupply = supplyService.updateSupplyVerification(id, verifiedBy)
        return ResponseUtil.conditional(
            data = verifiedSupply,
            successMessage = "更新补给点验证信息成功",
            notFoundMessage = "补给点不存在"
        )
    }
}