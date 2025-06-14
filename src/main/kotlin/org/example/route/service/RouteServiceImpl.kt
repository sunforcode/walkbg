package org.example.route.service

import org.example.user.repository.UserRepository
import org.example.user.repository.UserFavoriteRouteRepository
import org.example.user.model.UserFavoriteRoute
import org.example.route.model.Route
import org.example.route.model.Waypoint
import org.example.route.repository.RouteRepository
import org.example.route.repository.WaypointRepository

import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.time.Instant
import java.util.*

/**
 * 路线领域服务实现
 * 专注于路线领域的核心业务逻辑
 */
@Service
class RouteServiceImpl(
    private val routeRepository: RouteRepository,
    private val userRepository: UserRepository,
    private val userFavoriteRouteRepository: UserFavoriteRouteRepository,
    private val waypointRepository: WaypointRepository
) : RouteService {

    // ========== 基础查询操作 ==========
    /**
     * 根据ID查询路线（包含所有关联数据）
     */
    override fun getRouteWithDetails(id: String): Route? {
        return routeRepository.findById(id).orElse(null)
    }

    /**
     * 根据条件分页查询路线
     */
    override fun searchRoutes(
        keyword: String?,
        regionId: String?,
        difficulty: Int?,
        routeType: Int?,
        minDistance: Double?,
        maxDistance: Double?,
        tags: List<String>?,
        userId: String?,
        pageable: Pageable
    ): Page<Route> {
        // 如果是查询用户收藏的路线
        if (userId != null) {
            return routeRepository.findUserFavoriteRoutes(userId, pageable)
        }

        // 处理标签查询 - 只取第一个标签
        val tag = tags?.firstOrNull()

        // 使用repository的searchRoutes方法进行多条件查询
        return routeRepository.searchRoutes(
            keyword = keyword,
            region = regionId,
            difficulty = difficulty,
            routeType = routeType,
            status = null,
            tag = tag,
            pageable = pageable
        )
    }

    // ========== 路线生命周期管理 ==========
    /**
     * 创建路线
     * 包含业务规则验证
     */
    @Transactional
    override fun createRoute(route: Route): Route {
        // TODO: 添加业务规则验证
        // 例如：验证路线名称唯一性、验证必填字段等
        return routeRepository.save(route)
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

        // TODO: 添加业务规则验证
        route.updatedAt = Instant.now()
        return routeRepository.save(route)
    }

    /**
     * 发布路线
     * 改变路线状态为已发布
     */
    @Transactional
    override fun publishRoute(routeId: String): Boolean {
        val route = routeRepository.findById(routeId).orElse(null) ?: return false

        // 业务规则：只有规划中的路线才能发布
        if (route.status != 0) {
            throw IllegalStateException("只有规划中的路线才能发布")
        }

        route.status = 1 // 已发布
        route.updatedAt = Instant.now()
        routeRepository.save(route)
        return true
    }

    /**
     * 关闭路线
     * 改变路线状态为已关闭
     */
    @Transactional
    override fun closeRoute(routeId: String): Boolean {
        val route = routeRepository.findById(routeId).orElse(null) ?: return false

        // 业务规则：只有已发布的路线才能关闭
        if (route.status != 1) {
            throw IllegalStateException("只有已发布的路线才能关闭")
        }

        route.status = 2 // 已关闭
        route.updatedAt = Instant.now()
        routeRepository.save(route)
        return true
    }

    /**
     * 删除路线
     */
    @Transactional
    override fun deleteRoute(id: String): Boolean {
        if (!routeRepository.existsById(id)) {
            return false
        }

        // TODO: 添加删除前的业务规则检查
        // 例如：检查是否有关联的行程等
        routeRepository.deleteById(id)
        return true
    }

    // ========== 用户交互功能 ==========
    /**
     * 收藏路线
     */
    @Transactional
    override fun favoriteRoute(routeId: String, userId: String): Boolean {
        // 检查路线和用户是否存在
        val route = routeRepository.findById(routeId).orElse(null) ?: return false
        val user = userRepository.findById(userId).orElse(null) ?: return false

        // 检查是否已经收藏
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
     */
    @Transactional
    override fun unfavoriteRoute(routeId: String, userId: String): Boolean {
        val result = userFavoriteRouteRepository.deleteByUserIdAndRouteId(userId, routeId)
        return result > 0
    }

    /**
     * 检查路线是否被用户收藏
     */
    override fun isRouteFavorited(routeId: String, userId: String): Boolean {
        return routeRepository.isRouteFavoritedByUser(userId, routeId)
    }

    // ========== 统计和计数功能 ==========
    /**
     * 记录路线访问
     * 增加热度和使用次数
     */
    @Transactional
    override fun recordRouteVisit(routeId: String): Boolean {
        val route = routeRepository.findById(routeId).orElse(null) ?: return false
        route.incrementPopularity()
        route.incrementUsageCount()
        routeRepository.save(route)
        return true
    }

    /**
     * 记录路线完成
     * 用户完成某条路线时调用
     */
    @Transactional
    override fun recordRouteCompletion(routeId: String, userId: String): Boolean {
        // TODO: 实现路线完成记录逻辑
        // 1. 检查用户和路线是否存在
        // 2. 创建完成记录
        // 3. 更新统计数据
        return true
    }

    /**
     * 创建路点
     */
    @Transactional
    override fun createWaypoint(waypoint: Waypoint): Waypoint {
        return waypointRepository.save(waypoint)
    }
}
