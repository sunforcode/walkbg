package org.example.route.service

import org.example.user.repository.UserRepository
import org.example.user.repository.UserFavoriteRouteRepository
import org.example.user.model.UserFavoriteRoute
import org.example.route.model.Route
import org.example.route.repository.RouteRepository
import org.example.route.dto.RouteCreateRequest
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.time.Instant
import java.util.*

/**
 * 路线领域服务实现
 * 职责：实现领域业务逻辑和业务规则验证
 * 专注于纯领域逻辑，不处理简单的数据访问
 */
@Service
class RouteServiceImpl(
    private val routeRepository: RouteRepository,
    private val userRepository: UserRepository,
    private val userFavoriteRouteRepository: UserFavoriteRouteRepository
) : RouteService {

    // ========== 业务规则验证 ==========
    /**
     * 检查路线访问权限
     * 领域规则：根据路线状态和用户身份判断是否可访问
     */
    override fun isRouteAccessible(route: Route, userId: String?): Boolean {
        return when (route.status) {
            0 -> route.createdBy == userId  // 规划中的路线只有创建者能访问
            1 -> true  // 已发布的路线所有人都能访问
            2 -> false // 已关闭的路线不能访问
            else -> false
        }
    }

    /**
     * 验证路线创建请求
     * 领域规则：验证路线创建的业务规则
     */
    override fun validateCompleteRouteCreation(request: RouteCreateRequest) {
        // 业务规则验证
        if (request.name.isBlank()) {
            throw IllegalArgumentException("路线名称不能为空")
        }

        if (request.name.length > 100) {
            throw IllegalArgumentException("路线名称不能超过100个字符")
        }

        // 验证难度等级
        if (request.difficulty != null && (request.difficulty < 1 || request.difficulty > 5)) {
            throw IllegalArgumentException("难度等级必须在1-5之间")
        }

        // 验证路线类型
        if (request.routeType != null && (request.routeType < 0 || request.routeType > 3)) {
            throw IllegalArgumentException("路线类型必须在0-3之间")
        }

        // 验证路段数据
        if (request.segments.isNotEmpty() && request.waypoints.size < 2) {
            throw IllegalArgumentException("有路段的路线至少需要2个路点")
        }
    }

    // ========== 路线生命周期管理 ==========
    /**
     * 创建路线（包含业务规则验证）
     * 领域逻辑：应用创建路线的业务规则
     */
    @Transactional
    override fun createRouteWithValidation(route: Route): Route {
        // 领域业务规则验证
        validateRouteForCreation(route)

        // 应用领域规则：新建路线的默认状态
        route.status = 0  // 规划中
        route.popularity = 0
        route.usageCount = 0
        route.updatedAt = Instant.now()

        return routeRepository.save(route)
    }

    /**
     * 发布路线
     * 领域规则：只有规划中的路线才能发布
     */
    @Transactional
    override fun publishRoute(routeId: String): Boolean {
        val route = routeRepository.findById(routeId).orElse(null) ?: return false

        // 领域业务规则：只有规划中的路线才能发布
        if (route.status != 0) {
            throw IllegalStateException("只有规划中的路线才能发布")
        }

        // 领域规则：发布前的验证
        validateRouteForPublish(route)

        // 应用领域逻辑：更新状态
        route.status = 1 // 已发布
        route.updatedAt = Instant.now()
        routeRepository.save(route)
        return true
    }

    /**
     * 关闭路线
     * 领域规则：只有已发布的路线才能关闭
     */
    @Transactional
    override fun closeRoute(routeId: String): Boolean {
        val route = routeRepository.findById(routeId).orElse(null) ?: return false

        // 领域业务规则：只有已发布的路线才能关闭
        if (route.status != 1) {
            throw IllegalStateException("只有已发布的路线才能关闭")
        }

        // 应用领域逻辑：更新状态
        route.status = 2 // 已关闭
        route.updatedAt = Instant.now()
        routeRepository.save(route)
        return true
    }

    /**
     * 领域业务规则：验证路线创建
     */
    private fun validateRouteForCreation(route: Route) {
        if (route.name.isBlank()) {
            throw IllegalArgumentException("路线名称不能为空")
        }

        if (route.name.length > 100) {
            throw IllegalArgumentException("路线名称不能超过100个字符")
        }

        // 检查名称唯一性（在同一区域内）
        if (route.region != null) {
            val existingRoutes = routeRepository.findByNameContainingIgnoreCase(
                route.name,
                org.springframework.data.domain.PageRequest.of(0, 1)
            )
            if (existingRoutes.content.any { it.region == route.region && it.id != route.id }) {
                throw IllegalArgumentException("该区域已存在同名路线")
            }
        }
    }

    /**
     * 领域业务规则：验证路线发布
     */
    private fun validateRouteForPublish(route: Route) {
        if (route.name.isBlank()) {
            throw IllegalStateException("路线名称不能为空，无法发布")
        }

        if (route.description.isNullOrBlank()) {
            throw IllegalStateException("路线描述不能为空，无法发布")
        }

        // 可以添加更多发布前的验证规则
        // 例如：必须有路点、必须有描述等
    }

    // ========== 用户交互业务逻辑 ==========
    /**
     * 检查路线是否被用户收藏
     * 领域逻辑：根据业务规则判断收藏状态
     */
    override fun isRouteFavorited(routeId: String, userId: String): Boolean {
        return routeRepository.isRouteFavoritedByUser(userId, routeId)
    }

    /**
     * 收藏路线
     * 领域规则：收藏的业务逻辑和约束
     */
    @Transactional
    override fun favoriteRoute(routeId: String, userId: String): Boolean {
        // 领域规则验证
        val route = routeRepository.findById(routeId).orElse(null) ?: return false
        val user = userRepository.findById(userId).orElse(null) ?: return false

        // 领域规则：不能收藏自己创建的路线
        if (route.createdBy == userId) {
            throw IllegalArgumentException("不能收藏自己创建的路线")
        }

        // 领域规则：只能收藏已发布的路线
        if (route.status != 1) {
            throw IllegalArgumentException("只能收藏已发布的路线")
        }

        // 检查是否已经收藏（幂等性）
        if (userFavoriteRouteRepository.existsByUserIdAndRouteId(userId, routeId)) {
            return true
        }

        // 创建收藏关系
        val favorite = UserFavoriteRoute(
            id = UUID.randomUUID().toString(),
            user = user,
            route = route
        )

        userFavoriteRouteRepository.save(favorite)
        return true
    }

    /**
     * 取消收藏路线
     * 领域规则：取消收藏的业务逻辑
     */
    @Transactional
    override fun unfavoriteRoute(routeId: String, userId: String): Boolean {
        val result = userFavoriteRouteRepository.deleteByUserIdAndRouteId(userId, routeId)
        return result > 0
    }

    // ========== 访问统计业务逻辑 ==========
    /**
     * 根据业务规则记录路线访问
     * 领域规则：什么情况下需要记录访问，如何更新统计数据
     */
    @Transactional
    override fun recordRouteVisitIfNeeded(route: Route, userId: String?) {
        // 领域规则：创建者访问自己的路线不计入统计
        if (shouldRecordVisit(route, userId)) {
            route.incrementPopularity()
            route.incrementUsageCount()
            routeRepository.save(route)
        }
    }

    /**
     * 记录路线完成
     * 领域规则：路线完成的业务逻辑和统计更新
     */
    @Transactional
    override fun recordRouteCompletion(routeId: String, userId: String): Boolean {
        val route = routeRepository.findById(routeId).orElse(null) ?: return false
        val user = userRepository.findById(userId).orElse(null) ?: return false

        // 领域规则：只有已发布的路线才能标记为完成
        if (route.status != 1) {
            throw IllegalArgumentException("只有已发布的路线才能标记为完成")
        }

        // 领域规则：不能完成自己创建的路线
        if (route.createdBy == userId) {
            throw IllegalArgumentException("不能完成自己创建的路线")
        }

        // TODO: 实现完成记录的具体逻辑
        // 1. 创建完成记录
        // 2. 更新路线统计
        // 3. 可能的奖励逻辑等

        return true
    }

    /**
     * 领域规则：判断是否应该记录访问
     */
    private fun shouldRecordVisit(route: Route, userId: String?): Boolean {
        // 创建者访问自己的路线不计入统计
        return userId != route.createdBy
    }

    // ========== 数据访问方法（遵循分层架构） ==========
    /**
     * 获取路线（包含访问权限检查）
     * 结合数据访问和业务规则
     */
    override fun getRouteWithAccessCheck(routeId: String, userId: String?): Route? {
        val route = routeRepository.findById(routeId).orElse(null) ?: return null

        // 应用业务规则检查
        return if (isRouteAccessible(route, userId)) {
            route
        } else {
            null
        }
    }

    /**
     * 获取路线完整详情（包含访问权限检查）
     * 结合数据访问和业务规则
     */
    override fun getRouteWithFullDetailsAndAccessCheck(routeId: String, userId: String?): Route? {
        val route = routeRepository.findById(routeId).orElse(null) ?: return null

        // 应用业务规则检查
        return if (isRouteAccessible(route, userId)) {
            route
        } else {
            null
        }
    }

    /**
     * 搜索路线（包含业务规则）
     * 结合数据访问和业务规则
     */
    override fun searchRoutes(
        keyword: String?,
        regionId: String?,
        difficulty: Int?,
        routeType: Int?,
        minDistance: Double?,
        maxDistance: Double?,
        userId: String?,
        pageable: org.springframework.data.domain.Pageable
    ): org.springframework.data.domain.Page<Route> {
        // 如果是查询用户收藏的路线
        if (userId != null) {
            return routeRepository.findUserFavoriteRoutes(userId, pageable)
        }

        // 多条件搜索，应用业务规则：只查询已发布的路线
        return routeRepository.searchRoutes(
            keyword = keyword,
            region = regionId,
            difficulty = difficulty,
            routeType = routeType,
            status = 1, // 只查询已发布的路线（业务规则）
            tag = null,
            pageable = pageable
        )
    }

    /**
     * 根据ID获取路线
     * 简单的数据访问
     */
    override fun getRouteById(routeId: String): Route? {
        return routeRepository.findById(routeId).orElse(null)
    }

    /**
     * 更新路线
     * 包含业务规则验证
     */
    @Transactional
    override fun updateRoute(route: Route): Route {
        // 确保路线存在
        if (!routeRepository.existsById(route.id)) {
            throw IllegalArgumentException("路线不存在: ${route.id}")
        }

        // 应用业务规则
        route.updatedAt = Instant.now()
        return routeRepository.save(route)
    }
}
