package org.example.route.controller

import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.Parameter
import io.swagger.v3.oas.annotations.tags.Tag
import org.example.common.dto.ApiResponse
import org.example.common.util.ResponseUtil
import org.example.route.service.RouteApplicationService
import org.example.route.dto.RouteDetailResponse
import org.example.route.dto.RouteBasicResponse
import org.springframework.data.domain.Page
import org.springframework.http.ResponseEntity
import org.springframework.validation.annotation.Validated
import org.springframework.web.bind.annotation.*
import jakarta.validation.Valid
import jakarta.validation.constraints.NotBlank

/**
 * 路线控制器
 * 只处理HTTP请求响应，业务逻辑委托给ApplicationService
 */
@RestController
@RequestMapping("/api/routes")
@Tag(name = "路线管理", description = "路线相关的API接口")
@Validated
class RouteController(
    private val routeApplicationService: RouteApplicationService
) {

    /**
     * 分页查询路线列表
     */
    @GetMapping
    @Operation(summary = "分页查询路线列表", description = "获取路线列表，支持分页")
    fun getRoutes(
        @Parameter(description = "页码，从0开始") @RequestParam(defaultValue = "0") page: Int,
        @Parameter(description = "每页大小") @RequestParam(defaultValue = "10") size: Int,
        @Parameter(description = "关键词搜索") @RequestParam(required = false) keyword: String?,
        @Parameter(description = "区域ID") @RequestParam(required = false) regionId: String?,
        @Parameter(description = "难度等级") @RequestParam(required = false) difficulty: Int?,
        @Parameter(description = "路线类型") @RequestParam(required = false) routeType: Int?,
        @Parameter(description = "最小距离") @RequestParam(required = false) minDistance: Double?,
        @Parameter(description = "最大距离") @RequestParam(required = false) maxDistance: Double?,
        @Parameter(description = "用户ID") @RequestParam(required = false) userId: String?
    ): ResponseEntity<ApiResponse<Page<RouteBasicResponse>>> {
        return try {
            val pageable = org.springframework.data.domain.PageRequest.of(page, size)
            val routes = routeApplicationService.searchRoutes(
                keyword, regionId, difficulty, routeType,
                minDistance, maxDistance, userId, pageable
            )

            ResponseUtil.successPage(routes)
        } catch (e: Exception) {
            ResponseUtil.error("查询路线列表失败: ${e.message}")
        }
    }

    /**
     * 根据ID查询路线详情
     */
    @GetMapping("/{id}")
    @Operation(summary = "查询路线详情", description = "根据路线ID获取详细信息")
    fun getRouteById(
        @Parameter(description = "路线ID") @PathVariable id: String,
        @Parameter(description = "用户ID") @RequestParam(required = false) userId: String?
    ): ResponseEntity<ApiResponse<RouteBasicResponse?>> {
        return try {
            val route = routeApplicationService.getRouteDetails(id, userId)
            if (route == null) {
                ResponseUtil.error("路线不存在")
            } else {
                ResponseUtil.success(route)
            }
        } catch (e: Exception) {
            ResponseUtil.error("查询路线详情失败: ${e.message}")
        }
    }



    /**
     * 创建完整路线
     */
    @PostMapping
    @Operation(summary = "创建完整路线", description = "创建包含路段、路点、标签等关联对象的完整路线")
    fun createRoute(
        @RequestBody @Valid request: org.example.route.dto.RouteCreateRequest
    ): ResponseEntity<ApiResponse<RouteBasicResponse>> {
        return try {
            val route = routeApplicationService.createCompleteRoute(request)
            ResponseUtil.success(route)
        } catch (e: Exception) {
            ResponseUtil.error("创建完整路线失败: ${e.message}")
        }
    }
}