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
@RequestMapping("/api/v1/routes")
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
    ): ResponseEntity<ApiResponse<org.example.route.dto.RouteDetailResponse?>> {
        return try {
            val route = routeApplicationService.getRouteFullDetails(id, userId)
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
     * 创建路线
     */
    @PostMapping
    @Operation(summary = "创建路线", description = "创建新的路线")
    fun createRoute(
        @RequestBody @Valid request: org.example.route.dto.RouteCreateRequest
    ): ResponseEntity<ApiResponse<RouteBasicResponse>> {
        return try {
            val route = routeApplicationService.createCompleteRoute(request)
            ResponseUtil.success(route, "路线创建成功")
        } catch (e: Exception) {
            ResponseUtil.error("创建路线失败: ${e.message}")
        }
    }

    /**
     * 更新路线
     */
    @PutMapping("/{id}")
    @Operation(summary = "更新路线", description = "更新指定ID的路线信息")
    fun updateRoute(
        @Parameter(description = "路线ID") @PathVariable id: String,
        @RequestBody @Valid request: org.example.route.dto.RouteCreateRequest
    ): ResponseEntity<ApiResponse<RouteBasicResponse>> {
        return try {
            // TODO: 实现更新逻辑
            ResponseUtil.error("更新功能暂未实现")
        } catch (e: Exception) {
            ResponseUtil.error("更新路线失败: ${e.message}")
        }
    }

    /**
     * 删除路线
     */
    @DeleteMapping("/{id}")
    @Operation(summary = "删除路线", description = "删除指定ID的路线")
    fun deleteRoute(
        @Parameter(description = "路线ID") @PathVariable id: String
    ): ResponseEntity<ApiResponse<String>> {
        return try {
            // TODO: 实现删除逻辑
            ResponseUtil.error("删除功能暂未实现")
        } catch (e: Exception) {
            ResponseUtil.error("删除路线失败: ${e.message}")
        }
    }

    /**
     * 收藏路线
     */
    @PostMapping("/{id}/favorite")
    @Operation(summary = "收藏路线", description = "将指定路线添加到收藏")
    fun favoriteRoute(
        @Parameter(description = "路线ID") @PathVariable id: String,
        @Parameter(description = "用户ID") @RequestParam userId: String
    ): ResponseEntity<ApiResponse<String>> {
        return try {
            // TODO: 实现收藏逻辑
            ResponseUtil.success("收藏成功")
        } catch (e: Exception) {
            ResponseUtil.error("收藏路线失败: ${e.message}")
        }
    }

    /**
     * 取消收藏路线
     */
    @DeleteMapping("/{id}/favorite")
    @Operation(summary = "取消收藏路线", description = "从收藏中移除指定路线")
    fun unfavoriteRoute(
        @Parameter(description = "路线ID") @PathVariable id: String,
        @Parameter(description = "用户ID") @RequestParam userId: String
    ): ResponseEntity<ApiResponse<String>> {
        return try {
            // TODO: 实现取消收藏逻辑
            ResponseUtil.success("取消收藏成功")
        } catch (e: Exception) {
            ResponseUtil.error("取消收藏失败: ${e.message}")
        }
    }

    /**
     * 完成路线
     */
    @PostMapping("/{id}/complete")
    @Operation(summary = "完成路线", description = "标记路线为已完成")
    fun completeRoute(
        @Parameter(description = "路线ID") @PathVariable id: String,
        @Parameter(description = "用户ID") @RequestParam userId: String
    ): ResponseEntity<ApiResponse<String>> {
        return try {
            // TODO: 实现完成路线逻辑
            ResponseUtil.success("路线完成记录成功")
        } catch (e: Exception) {
            ResponseUtil.error("记录路线完成失败: ${e.message}")
        }
    }

    /**
     * 查询我创建的路线
     */
    @GetMapping("/my")
    @Operation(summary = "查询我创建的路线", description = "获取当前用户创建的路线列表")
    fun getMyRoutes(
        @Parameter(description = "用户ID") @RequestParam userId: String,
        @Parameter(description = "页码，从0开始") @RequestParam(defaultValue = "0") page: Int,
        @Parameter(description = "每页大小") @RequestParam(defaultValue = "10") size: Int
    ): ResponseEntity<ApiResponse<Page<RouteBasicResponse>>> {
        return try {
            // TODO: 实现查询我创建的路线逻辑
            val emptyPage = org.springframework.data.domain.PageImpl<RouteBasicResponse>(
                emptyList(),
                org.springframework.data.domain.PageRequest.of(page, size),
                0
            )
            ResponseUtil.successPage(emptyPage)
        } catch (e: Exception) {
            ResponseUtil.error("查询我的路线失败: ${e.message}")
        }
    }

    /**
     * 查询我收藏的路线
     */
    @GetMapping("/favorites")
    @Operation(summary = "查询我收藏的路线", description = "获取当前用户收藏的路线列表")
    fun getFavoriteRoutes(
        @Parameter(description = "用户ID") @RequestParam userId: String,
        @Parameter(description = "页码，从0开始") @RequestParam(defaultValue = "0") page: Int,
        @Parameter(description = "每页大小") @RequestParam(defaultValue = "10") size: Int
    ): ResponseEntity<ApiResponse<Page<RouteBasicResponse>>> {
        return try {
            // TODO: 实现查询收藏路线逻辑
            val emptyPage = org.springframework.data.domain.PageImpl<RouteBasicResponse>(
                emptyList(),
                org.springframework.data.domain.PageRequest.of(page, size),
                0
            )
            ResponseUtil.successPage(emptyPage)
        } catch (e: Exception) {
            ResponseUtil.error("查询收藏路线失败: ${e.message}")
        }
    }

    /**
     * 查询我完成的路线
     */
    @GetMapping("/completed")
    @Operation(summary = "查询我完成的路线", description = "获取当前用户完成的路线列表")
    fun getCompletedRoutes(
        @Parameter(description = "用户ID") @RequestParam userId: String,
        @Parameter(description = "页码，从0开始") @RequestParam(defaultValue = "0") page: Int,
        @Parameter(description = "每页大小") @RequestParam(defaultValue = "10") size: Int
    ): ResponseEntity<ApiResponse<Page<RouteBasicResponse>>> {
        return try {
            // TODO: 实现查询完成路线逻辑
            val emptyPage = org.springframework.data.domain.PageImpl<RouteBasicResponse>(
                emptyList(),
                org.springframework.data.domain.PageRequest.of(page, size),
                0
            )
            ResponseUtil.successPage(emptyPage)
        } catch (e: Exception) {
            ResponseUtil.error("查询完成路线失败: ${e.message}")
        }
    }

    /**
     * 获取推荐路线
     */
    @GetMapping("/recommendations")
    @Operation(summary = "获取推荐路线", description = "根据用户偏好推荐路线")
    fun getRecommendedRoutes(
        @Parameter(description = "用户ID") @RequestParam(required = false) userId: String?,
        @Parameter(description = "页码，从0开始") @RequestParam(defaultValue = "0") page: Int,
        @Parameter(description = "每页大小") @RequestParam(defaultValue = "10") size: Int,
        @Parameter(description = "推荐类型") @RequestParam(required = false) type: String?
    ): ResponseEntity<ApiResponse<Page<RouteBasicResponse>>> {
        return try {
            // TODO: 实现路线推荐逻辑
            val emptyPage = org.springframework.data.domain.PageImpl<RouteBasicResponse>(
                emptyList(),
                org.springframework.data.domain.PageRequest.of(page, size),
                0
            )
            ResponseUtil.successPage(emptyPage)
        } catch (e: Exception) {
            ResponseUtil.error("获取推荐路线失败: ${e.message}")
        }
    }

    /**
     * 获取附近的路线
     */
    @GetMapping("/nearby")
    @Operation(summary = "获取附近的路线", description = "根据地理位置获取附近的路线")
    fun getNearbyRoutes(
        @Parameter(description = "纬度") @RequestParam latitude: Double,
        @Parameter(description = "经度") @RequestParam longitude: Double,
        @Parameter(description = "搜索半径（公里）") @RequestParam(defaultValue = "10.0") radius: Double,
        @Parameter(description = "页码，从0开始") @RequestParam(defaultValue = "0") page: Int,
        @Parameter(description = "每页大小") @RequestParam(defaultValue = "10") size: Int
    ): ResponseEntity<ApiResponse<Page<RouteBasicResponse>>> {
        return try {
            // TODO: 实现附近路线查询逻辑
            val emptyPage = org.springframework.data.domain.PageImpl<RouteBasicResponse>(
                emptyList(),
                org.springframework.data.domain.PageRequest.of(page, size),
                0
            )
            ResponseUtil.successPage(emptyPage)
        } catch (e: Exception) {
            ResponseUtil.error("获取附近路线失败: ${e.message}")
        }
    }
}