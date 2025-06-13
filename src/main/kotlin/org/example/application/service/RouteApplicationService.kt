package org.example.application.service

import org.example.route.service.RouteService
import org.example.route.dto.RouteDto

import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

/**
 * 路线应用服务
 */
@Service
class RouteApplicationService(
    private val routeService: RouteService
) {
    /**
     * 根据ID查询路线
     */
    @Transactional(readOnly = true)
    fun findById(id: String, userId: String? = null): RouteDto? {
        val route = routeService.findById(id) ?: return null

        // 如果提供了用户ID，检查是否收藏
        if (userId != null) {
            route.isFavorite = routeService.isFavorite(id, userId)
        }

        // TODO: 实现Route到RouteDto的转换
        return null
    }

    /**
     * 分页查询路线
     */
    @Transactional(readOnly = true)
    fun findAll(pageable: Pageable, userId: String? = null): Page<RouteDto> {
        val routes = routeService.findAll(pageable)

        // 如果提供了用户ID，检查每条路线是否被收藏
        if (userId != null) {
            routes.forEach { route ->
                route.isFavorite = routeService.isFavorite(route.id, userId)
            }
        }

        // TODO: 实现Route到RouteDto的转换
        return routes.map {
            // 临时返回空的RouteDto
            RouteDto(
                id = it.id,
                name = it.name,
                description = it.description,
                regionId = null,
                region = null,                distance = it.distance?.toDouble(),
                duration = it.duration,
                elevationGain = it.elevationGain?.toDouble(),
                elevationLoss = it.elevationLoss?.toDouble(),
                difficulty = it.difficulty,
                routeType = it.routeType,
                routeDirection = it.routeDirection,
                coverUrl = it.coverUrl,
                defaultMapId = it.defaultMapId,
                popularity = it.popularity,
                usageCount = it.usageCount,
                isLoop = it.isLoop,
                isFavorite = it.isFavorite ?: false,
                status = it.status,
                createdAt = it.createdAt,
                updatedAt = it.updatedAt,
                createdBy = it.createdBy,
                createUser = null
            )
        }
    }

    /**
     * 根据条件分页查询路线
     */
    @Transactional(readOnly = true)
    fun findByCondition(
        keyword: String? = null,
        regionId: String? = null,
        difficulty: Int? = null,
        routeType: Int? = null,
        minDistance: Double? = null,
        maxDistance: Double? = null,
        tags: List<String>? = null,
        userId: String? = null,
        pageable: Pageable
    ): Page<RouteDto> {
        val routes = routeService.findByCondition(
            keyword, regionId, difficulty, routeType,
            minDistance, maxDistance, tags, userId, pageable
        )

        // 如果提供了用户ID，检查每条路线是否被收藏
        if (userId != null) {
            routes.forEach { route ->
                route.isFavorite = routeService.isFavorite(route.id, userId)
            }
        }

        // TODO: 实现Route到RouteDto的转换
        return routes.map {
            // 临时返回空的RouteDto
            RouteDto(
                id = it.id,
                name = it.name,
                description = it.description,
                regionId = null,
                region = null,
                distance = it.distance?.toDouble(),
                duration = it.duration,
                elevationGain = it.elevationGain?.toDouble(),
                elevationLoss = it.elevationLoss?.toDouble(),
                difficulty = it.difficulty,
                routeType = it.routeType,
                routeDirection = it.routeDirection,
                coverUrl = it.coverUrl,
                defaultMapId = it.defaultMapId,
                popularity = it.popularity,
                usageCount = it.usageCount,
                isLoop = it.isLoop,
                isFavorite = it.isFavorite ?: false,
                status = it.status,
                createdAt = it.createdAt,
                updatedAt = it.updatedAt,
                createdBy = it.createdBy,
                createUser = null
            )
        }
    }

    /**
     * 收藏路线
     */
    @Transactional
    fun favorite(routeId: String, userId: String): Boolean {
        return routeService.favorite(routeId, userId)
    }

    /**
     * 取消收藏路线
     */
    @Transactional
    fun unfavorite(routeId: String, userId: String): Boolean {
        return routeService.unfavorite(routeId, userId)
    }

    /**
     * 增加路线热度
     */
    @Transactional
    fun incrementPopularity(id: String): Boolean {
        return routeService.incrementPopularity(id)
    }

    /**
     * 增加路线使用次数
     */
    @Transactional
    fun incrementUsageCount(id: String): Boolean {
        return routeService.incrementUsageCount(id)
    }
}
