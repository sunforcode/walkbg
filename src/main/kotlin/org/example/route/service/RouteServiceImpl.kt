package org.example.route.service

import org.example.user.repository.UserRepository
import org.example.user.repository.UserFavoriteRouteRepository
import org.example.user.model.UserFavoriteRoute
import org.example.route.model.Route
import org.example.route.repository.RouteRepository

import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.time.Instant
import java.util.*

/**
 * 路线领域服务实现
 */
@Service
class RouteServiceImpl(
    private val routeRepository: RouteRepository,
    private val userRepository: UserRepository,
    private val userFavoriteRouteRepository: UserFavoriteRouteRepository
) : RouteService {
    /**
     * 根据ID查询路线
     */
    override fun findById(id: String): Route? {
        return routeRepository.findById(id).orElse(null)
    }

    /**
     * 分页查询路线
     */
    override fun findAll(pageable: Pageable): Page<Route> {
        return routeRepository.findAll(pageable)
    }

    /**
     * 根据条件分页查询路线
     */
    override fun findByCondition(
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
            status = null, // RouteService接口中没有status参数，传null
            tag = tag,
            pageable = pageable
        )
    }

    /**
     * 创建路线
     */
    @Transactional
    override fun create(route: Route): Route {
        return routeRepository.save(route)
    }

    /**
     * 更新路线
     */
    @Transactional
    override fun update(route: Route): Route {
        // 确保路线存在
        if (!routeRepository.existsById(route.id)) {
            throw IllegalArgumentException("路线不存在: ${route.id}")
        }


        return routeRepository.save(route)
    }

    /**
     * 删除路线
     */
    @Transactional
    override fun delete(id: String): Boolean {
        if (!routeRepository.existsById(id)) {
            return false
        }

        routeRepository.deleteById(id)
        return true
    }

    /**
     * 增加路线热度
     */
    @Transactional
    override fun incrementPopularity(id: String): Boolean {
        val route = routeRepository.findById(id).orElse(null) ?: return false
        route.incrementPopularity()
        routeRepository.save(route)
        return true
    }

    /**
     * 增加路线使用次数
     */
    @Transactional
    override fun incrementUsageCount(id: String): Boolean {
        val route = routeRepository.findById(id).orElse(null) ?: return false
        route.incrementUsageCount()
        routeRepository.save(route)
        return true
    }

    /**
     * 收藏路线
     */
    @Transactional
    override fun favorite(routeId: String, userId: String): Boolean {
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
    override fun unfavorite(routeId: String, userId: String): Boolean {
        val result = userFavoriteRouteRepository.deleteByUserIdAndRouteId(userId, routeId)
        return result > 0
    }

    /**
     * 检查路线是否被收藏
     */
    override fun isFavorite(routeId: String, userId: String): Boolean {
        return routeRepository.isRouteFavoritedByUser(userId, routeId)
    }
}
