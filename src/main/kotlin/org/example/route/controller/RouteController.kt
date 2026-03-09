package org.example.route.controller

import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.Parameter
import io.swagger.v3.oas.annotations.tags.Tag
import org.example.common.dto.ApiResponse
import org.example.common.exception.BusinessException
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
    private val routeApplicationService: RouteApplicationService,
    private val routeService: org.example.route.service.RouteService,
    private val userRouteFavoriteRepository: org.example.route.repository.UserRouteFavoriteRepository,
    private val userRouteCompletionRepository: org.example.route.repository.UserRouteCompletionRepository,
    private val routeRepository: org.example.route.repository.RouteRepository
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
        val pageable = org.springframework.data.domain.PageRequest.of(page, size)
        val routes = routeApplicationService.searchRoutes(
            keyword, regionId, difficulty, routeType,
            minDistance, maxDistance, userId, pageable
        )
        return ResponseUtil.successPage(routes)
    }

    /**
     * 根据ID查询路线详情
     */
    @GetMapping("/{id}")
    @Operation(summary = "查询路线详情", description = "根据路线ID获取详细信息")
    fun getRouteById(
        @Parameter(description = "路线ID") @PathVariable id: String,
        @Parameter(description = "用户ID") @RequestParam(required = false) userId: String?
    ): ResponseEntity<ApiResponse<RouteDetailResponse>> {
        val route = routeApplicationService.getRouteFullDetails(id, userId)
            ?: throw BusinessException.notFound("路线不存在")
        return ResponseUtil.success(route)
    }

    /**
     * 创建路线
     */
    @PostMapping
    @Operation(summary = "创建路线", description = "创建新的路线")
    fun createRoute(
        @RequestBody @Valid request: org.example.route.dto.RouteCreateRequest
    ): ResponseEntity<ApiResponse<RouteBasicResponse>> {
        val route = routeApplicationService.createCompleteRoute(request)
        return ResponseUtil.created(route, "路线创建成功")
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
        // TODO: 实现更新逻辑
        throw BusinessException.badRequest("更新功能暂未实现")
    }

    /**
     * 删除路线
     */
    @DeleteMapping("/{id}")
    @Operation(summary = "删除路线", description = "删除指定ID的路线")
    fun deleteRoute(
        @Parameter(description = "路线ID") @PathVariable id: String
    ): ResponseEntity<ApiResponse<Nothing>> {
        // TODO: 实现删除逻辑
        throw BusinessException.badRequest("删除功能暂未实现")
    }

    /**
     * 收藏路线
     */
    @PostMapping("/{id}/favorite")
    @Operation(summary = "收藏路线", description = "将指定路线添加到收藏")
    fun favoriteRoute(
        @Parameter(description = "路线ID") @PathVariable id: String,
        @Parameter(description = "用户ID") @RequestParam userId: String
    ): ResponseEntity<ApiResponse<Nothing>> {
        // 验证路线是否存在
        routeRepository.findById(id).orElseThrow {
            BusinessException.notFound("路线不存在")
        }
        
        // 幂等性处理：如果已经收藏，则不重复创建
        if (!userRouteFavoriteRepository.existsByUserIdAndRouteId(userId, id)) {
            val favorite = org.example.route.model.UserRouteFavorite(
                userId = userId,
                routeId = id
            )
            userRouteFavoriteRepository.save(favorite)
        }
        
        return ResponseUtil.success(null, "收藏成功")
    }

    /**
     * 取消收藏路线
     */
    @DeleteMapping("/{id}/favorite")
    @Operation(summary = "取消收藏路线", description = "从收藏中移除指定路线")
    fun unfavoriteRoute(
        @Parameter(description = "路线ID") @PathVariable id: String,
        @Parameter(description = "用户ID") @RequestParam userId: String
    ): ResponseEntity<ApiResponse<Nothing>> {
        // 幂等性处理：如果没有收藏，则不报错
        userRouteFavoriteRepository.deleteByUserIdAndRouteId(userId, id)
        return ResponseUtil.success(null, "取消收藏成功")
    }

    /**
     * 完成路线
     */
    @PostMapping("/{id}/complete")
    @Operation(summary = "完成路线", description = "标记路线为已完成")
    fun completeRoute(
        @Parameter(description = "路线ID") @PathVariable id: String,
        @Parameter(description = "用户ID") @RequestParam userId: String
    ): ResponseEntity<ApiResponse<Nothing>> {
        // 验证路线是否存在
        val route = routeRepository.findById(id).orElseThrow {
            BusinessException.notFound("路线不存在")
        }
        
        // 幂等性处理：每个用户每次完成路线都会需要记录，但usage_count仅在第一次有效完成时增加
        val existingCompletion = userRouteCompletionRepository.findByUserIdAndRouteId(userId, id)
        if (existingCompletion == null) {
            // 第一次完成，需要更新路线的usage_count
            val completion = org.example.route.model.UserRouteCompletion(
                userId = userId,
                routeId = id
            )
            userRouteCompletionRepository.save(completion)
            
            // 更新路线的usage_count
            route.incrementUsageCount()
            routeRepository.save(route)
        }
        
        return ResponseUtil.success(null, "路线完成记录成功")
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
        val pageable = org.springframework.data.domain.PageRequest.of(page, size, org.springframework.data.domain.Sort.by(org.springframework.data.domain.Sort.Direction.DESC, "createdAt"))
        val routes = routeRepository.findByCreatedBy(userId, pageable)
        val result = routes.map { RouteBasicResponse.fromRoute(it) }
        return ResponseUtil.successPage(result)
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
        val pageable = org.springframework.data.domain.PageRequest.of(page, size)
        val favorites = userRouteFavoriteRepository.findByUserId(userId, pageable)
        // 关联路线信息
        val result = favorites.map { favorite ->
            routeRepository.findById(favorite.routeId).map { RouteBasicResponse.fromRoute(it) }.orElse(null)
        }.filterNotNull()
        // 重新币造Page对象
        return ResponseUtil.successPage(
            org.springframework.data.domain.PageImpl(
                result,
                pageable,
                favorites.totalElements
            )
        )
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
        val pageable = org.springframework.data.domain.PageRequest.of(page, size)
        val completions = userRouteCompletionRepository.findByUserId(userId, pageable)
        // 关联路线信息
        val result = completions.map { completion ->
            routeRepository.findById(completion.routeId).map { RouteBasicResponse.fromRoute(it) }.orElse(null)
        }.filterNotNull()
        // 重新造活Page对象
        return ResponseUtil.successPage(
            org.springframework.data.domain.PageImpl(
                result,
                pageable,
                completions.totalElements
            )
        )
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
        // TODO: 实现路线推荐逻辑
        val emptyPage = org.springframework.data.domain.PageImpl<RouteBasicResponse>(
            emptyList(),
            org.springframework.data.domain.PageRequest.of(page, size),
            0
        )
        return ResponseUtil.successPage(emptyPage)
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
        // TODO: 实现附近路线查询逻辑
        val emptyPage = org.springframework.data.domain.PageImpl<RouteBasicResponse>(
            emptyList(),
            org.springframework.data.domain.PageRequest.of(page, size),
            0
        )
        return ResponseUtil.successPage(emptyPage)
    }

    /**
     * 获取热门路线
     */
    @GetMapping("/popular")
    @Operation(summary = "获取热门路线", description = "按热度排序返回热门路线列表")
    fun getPopularRoutes(
        @Parameter(description = "返回数量限制") @RequestParam(defaultValue = "10") limit: Int
    ): ResponseEntity<ApiResponse<Page<RouteBasicResponse>>> {
        val routes = routeApplicationService.getPopularRoutes(limit)
        return ResponseUtil.successPage(routes)
    }
}