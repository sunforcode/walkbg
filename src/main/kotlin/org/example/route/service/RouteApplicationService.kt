package org.example.route.service

import org.example.route.dto.RouteDetailResponse
import org.example.route.dto.RouteBasicResponse
import org.example.route.model.Route
import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.time.Instant
import java.util.*

/**
 * 路线应用服务
 * 协调多个领域服务，处理完整的业务用例
 */
@Service
class RouteApplicationService(
    private val routeService: RouteService
) {

    /**
     * 读取路线详情
     * 简单的读取操作，包含基本的业务逻辑
     */
    @Transactional(readOnly = true)
    fun getRouteDetails(routeId: String, userId: String? = null): RouteBasicResponse? {
        // 1. 获取路线详情
        val route = routeService.getRouteWithDetails(routeId) ?: return null

        // 2. 检查用户收藏状态（如果提供了用户ID）
        val isFavorited = userId?.let {
            routeService.isRouteFavorited(routeId, it)
        } ?: false

        // 3. 记录访问
        routeService.recordRouteVisit(routeId)

        // 4. 转换为DTO
        return RouteBasicResponse.fromRoute(route)
    }

    /**
     * 分页查询路线列表
     * 简单的搜索功能
     */
    @Transactional(readOnly = true)
    fun searchRoutes(
        keyword: String? = null,
        regionId: String? = null,
        difficulty: Int? = null,
        routeType: Int? = null,
        minDistance: Double? = null,
        maxDistance: Double? = null,
        userId: String? = null,
        pageable: Pageable
    ): Page<RouteBasicResponse> {
        // 1. 搜索路线
        val routes = routeService.searchRoutes(
            keyword, regionId, difficulty, routeType,
            minDistance, maxDistance, null, userId, pageable
        )

        // 2. 转换为DTO
        return routes.map { RouteBasicResponse.fromRoute(it) }
    }

    /**
     * 获取路线完整详情
     * 包含所有关联对象信息
     */
    @Transactional(readOnly = true)
    fun getRouteWithFullDetails(routeId: String, userId: String? = null): RouteDetailResponse? {
        // 1. 获取路线详情（包含关联对象）
        val route = routeService.getRouteWithDetails(routeId) ?: return null

        // 2. 检查用户收藏状态
        val isFavorited = userId?.let {
            routeService.isRouteFavorited(routeId, it)
        } ?: false

        // 3. 记录访问
        routeService.recordRouteVisit(routeId)

        // 4. 转换为详细DTO
        return RouteDetailResponse.fromRoute(route, isFavorited)
    }

    /**
     * 写入路线
     * 简单的创建功能
     */
    @Transactional
    fun createRoute(
        name: String,
        description: String? = null,
        region: String? = null,
        difficulty: Int? = null,
        routeType: Int? = null,
        creatorId: String
    ): RouteBasicResponse {
        // 1. 创建路线实体
        val route = Route(
            id = UUID.randomUUID().toString(),
            name = name,
            description = description,
            region = region,
            difficulty = difficulty,
            routeType = routeType,
            createdBy = creatorId,
            createdAt = Instant.now(),
            updatedAt = Instant.now()
        )

        // 2. 保存路线
        val savedRoute = routeService.createRoute(route)

        // 3. 转换为响应DTO
        return RouteBasicResponse.fromRoute(savedRoute)
    }
}
