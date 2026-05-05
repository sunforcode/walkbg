package org.example.route.service

import org.example.route.model.Route
import org.example.route.dto.RouteCreateRequest

/**
 * 路线领域服务接口
 * 职责：领域业务逻辑、业务规则验证、领域对象操作
 * 不直接操作Repository，专注于纯领域逻辑
 */
interface RouteService {

    // ========== 业务规则验证 ==========
    /**
     * 检查路线访问权限
     * 领域规则：根据路线状态和用户身份判断是否可访问
     */
    fun isRouteAccessible(route: Route, userId: String?): Boolean

    /**
     * 验证路线创建请求
     * 领域规则：验证路线创建的业务规则
     */
    fun validateCompleteRouteCreation(request: RouteCreateRequest)

    // ========== 路线生命周期管理 ==========
    /**
     * 创建路线（包含业务规则验证）
     * 领域逻辑：应用创建路线的业务规则
     */
    fun createRouteWithValidation(route: Route): Route

    /**
     * 发布路线
     * 领域规则：只有规划中的路线才能发布
     */
    fun publishRoute(routeId: String): Boolean

    /**
     * 关闭路线
     * 领域规则：只有已发布的路线才能关闭
     */
    fun closeRoute(routeId: String): Boolean

    // ========== 用户交互业务逻辑 ==========
    /**
     * 检查路线是否被用户收藏
     * 领域逻辑：根据业务规则判断收藏状态
     */
    fun isRouteFavorited(routeId: String, userId: String): Boolean

    /**
     * 收藏路线
     * 领域规则：收藏的业务逻辑和约束
     */
    fun favoriteRoute(routeId: String, userId: String): Boolean

    /**
     * 取消收藏路线
     * 领域规则：取消收藏的业务逻辑
     */
    fun unfavoriteRoute(routeId: String, userId: String): Boolean

    // ========== 访问统计业务逻辑 ==========
    /**
     * 根据业务规则记录路线访问
     * 领域规则：什么情况下需要记录访问，如何更新统计数据
     */
    fun recordRouteVisitIfNeeded(route: Route, userId: String?)

    /**
     * 记录路线完成
     * 领域规则：路线完成的业务逻辑和统计更新
     */
    fun recordRouteCompletion(routeId: String, userId: String): Boolean

    // ========== 数据访问方法（遵循分层架构） ==========
    /**
     * 获取路线（包含访问权限检查）
     * 结合数据访问和业务规则
     */
    fun getRouteWithAccessCheck(routeId: String, userId: String?): Route?



    /**
     * 搜索路线（包含业务规则）
     * 结合数据访问和业务规则
     */
    fun searchRoutes(
        keyword: String? = null,
        regionId: String? = null,
        difficulty: Int? = null,
        routeType: Int? = null,
        minDistance: Double? = null,
        maxDistance: Double? = null,
        userId: String? = null,
        pageable: org.springframework.data.domain.Pageable
    ): org.springframework.data.domain.Page<Route>

    /**
     * 根据ID获取路线
     * 简单的数据访问
     */
    fun getRouteById(routeId: String): Route?

    /**
     * 更新路线
     * 包含业务规则验证
     */
    fun updateRoute(route: Route): Route

    /**
     * 获取热门路线
     * 领域逻辑：按热度排序返回路线列表
     */
    fun getPopularRoutes(limit: Int, pageable: org.springframework.data.domain.Pageable): org.springframework.data.domain.Page<Route>

    /**
     * 获取新晋路线
     * 领域逻辑：按创建时间降序返回路线列表
     */
    fun getNewRoutes(limit: Int, pageable: org.springframework.data.domain.Pageable): org.springframework.data.domain.Page<Route>

    /**
     * 获取季节性路线
     * 领域逻辑：根据季节标签返回路线列表
     */
    fun getSeasonalRoutes(season: String?, limit: Int, pageable: org.springframework.data.domain.Pageable): org.springframework.data.domain.Page<Route>

    /**
     * 获取周末路线
     * 领域逻辑：返回适合周末的短途路线
     */
    fun getWeekendRoutes(limit: Int, pageable: org.springframework.data.domain.Pageable): org.springframework.data.domain.Page<Route>

    /**
     * 按标签搜索路线
     * 领域逻辑：根据标签列表查找路线
     */
    fun searchRoutesByTags(
        tags: List<String>,
        pageable: org.springframework.data.domain.Pageable
    ): org.springframework.data.domain.Page<Route>

    /**
     * 统一搜索路线（支持所有抽象参数）
     * 领域逻辑：结合所有查询条件搜索路线
     */
    fun searchRoutesUnified(
        keyword: String? = null,
        regionId: String? = null,
        difficulty: Int? = null,
        routeType: Int? = null,
        minDistance: Double? = null,
        maxDistance: Double? = null,
        userId: String? = null,
        tags: List<String>? = null,
        sortBy: String? = "popular",
        pageable: org.springframework.data.domain.Pageable
    ): org.springframework.data.domain.Page<Route>
}
