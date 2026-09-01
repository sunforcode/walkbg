package org.example.route.controller

import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.Parameter
import io.swagger.v3.oas.annotations.tags.Tag
import org.example.common.dto.ApiResponse
import org.example.common.util.ResponseUtil
import org.example.route.dto.RouteBasicResponse
import org.example.route.service.RouteApplicationService
import org.springframework.data.domain.Page
import org.springframework.data.domain.PageRequest
import org.springframework.data.domain.Sort
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController

/**
 * 管理端路线接口
 *
 * 与公共接口 GET /api/v1/routes 的区别：返回全部状态（含规划中/分析中/已关闭），
 * 支持按状态筛选。公共接口只返回已发布路线，保证 C 端可见性边界。
 * 路线的更新/状态流转/删除仍在 RouteController。
 */
@RestController
@RequestMapping("/api/v1/admin/routes")
@Tag(name = "路线管理（管理端）", description = "后台专用：全状态路线查询")
class AdminRouteController(
    private val routeApplicationService: RouteApplicationService
) {

    @GetMapping
    @Operation(summary = "管理端路线列表", description = "返回全部状态的路线，支持关键词/难度/状态筛选")
    fun getAdminRoutes(
        @Parameter(description = "页码，从0开始") @RequestParam(defaultValue = "0") page: Int,
        @Parameter(description = "每页大小") @RequestParam(defaultValue = "10") size: Int,
        @Parameter(description = "关键词搜索") @RequestParam(required = false) keyword: String?,
        @Parameter(description = "难度等级 1-5") @RequestParam(required = false) difficulty: Int?,
        @Parameter(description = "状态筛选：0规划中 1已发布 2已关闭 3分析中，不传返回全部") @RequestParam(required = false) status: Int?
    ): ResponseEntity<ApiResponse<Page<RouteBasicResponse>>> {
        val pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "createdAt"))
        val routes = routeApplicationService.searchRoutesForAdmin(
            keyword = keyword,
            difficulty = difficulty,
            status = status,
            pageable = pageable
        )
        return ResponseUtil.successPage(routes)
    }
}
