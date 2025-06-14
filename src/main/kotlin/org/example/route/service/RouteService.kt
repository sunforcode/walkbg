package org.example.route.service

import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import org.example.route.model.Route

/**
 * 路线领域服务接口
 * 专注于路线领域的核心业务逻辑
 */
interface RouteService {

    // ========== 基础查询操作 ==========
    /**
     * 根据ID查询路线（包含所有关联数据）
     */
    fun getRouteWithDetails(id: String): Route?

    /**
     * 根据条件分页查询路线
     */
    fun searchRoutes(
        keyword: String? = null,
        regionId: String? = null,
        difficulty: Int? = null,
        routeType: Int? = null,
        minDistance: Double? = null,
        maxDistance: Double? = null,
        tags: List<String>? = null,
        userId: String? = null,
        pageable: Pageable
    ): Page<Route>

    // ========== 路线生命周期管理 ==========
    /**
     * 创建路线
     * 包含业务规则验证
     */
    fun createRoute(route: Route): Route

    /**
     * 更新路线
     * 包含业务规则验证
     */
    fun updateRoute(route: Route): Route

    /**
     * 发布路线
     * 改变路线状态为已发布
     */
    fun publishRoute(routeId: String): Boolean

    /**
     * 关闭路线
     * 改变路线状态为已关闭
     */
    fun closeRoute(routeId: String): Boolean

    /**
     * 删除路线
     */
    fun deleteRoute(id: String): Boolean

    // ========== 用户交互功能 ==========
    /**
     * 收藏路线
     */
    fun favoriteRoute(routeId: String, userId: String): Boolean

    /**
     * 取消收藏路线
     */
    fun unfavoriteRoute(routeId: String, userId: String): Boolean

    /**
     * 检查路线是否被用户收藏
     */
    fun isRouteFavorited(routeId: String, userId: String): Boolean

    // ========== 统计和计数功能 ==========
    /**
     * 记录路线访问
     * 增加热度和使用次数
     */
    fun recordRouteVisit(routeId: String): Boolean

    /**
     * 记录路线完成
     * 用户完成某条路线时调用
     */
    fun recordRouteCompletion(routeId: String, userId: String): Boolean

    // ========== 路点管理 ==========
    /**
     * 创建路点
     */
    fun createWaypoint(waypoint: org.example.route.model.Waypoint): org.example.route.model.Waypoint
}
