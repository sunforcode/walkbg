package org.example.service.impl

import org.example.model.Route
import org.example.model.UserFavoriteRoute
import org.example.model.UserCompletedRoute
import org.example.repository.RouteRepository
import org.example.repository.UserFavoriteRouteRepository
import org.example.repository.UserCompletedRouteRepository
import org.example.service.RouteService
import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.math.BigDecimal
import java.time.Instant

/**
 * 路线服务实现类
 */
@Service
@Transactional
class RouteServiceImpl(
    private val routeRepository: RouteRepository,
    private val userFavoriteRouteRepository: UserFavoriteRouteRepository,
    private val userCompletedRouteRepository: UserCompletedRouteRepository
) : RouteService {

    // 基础CRUD操作
    override fun getAllRoutes(pageable: Pageable): Page<Route> {
        return routeRepository.findAll(pageable)
    }

    override fun getRouteById(id: String): Route? {
        return routeRepository.findById(id).orElse(null)
    }

    override fun createRoute(route: Route): Route {
        return routeRepository.save(route)
    }

    override fun updateRoute(id: String, route: Route): Route? {
        return routeRepository.findById(id).map { existingRoute ->
            existingRoute.apply {
                name = route.name
                description = route.description
                region = route.region
                regionId = route.regionId
                distance = route.distance
                duration = route.duration
                difficulty = route.difficulty
                routeType = route.routeType
                status = route.status
                elevationGain = route.elevationGain
                elevationLoss = route.elevationLoss
                latitude = route.latitude
                longitude = route.longitude
                altitude = route.altitude
                updatedAt = Instant.now()
            }
            routeRepository.save(existingRoute)
        }.orElse(null)
    }

    override fun deleteRoute(id: String): Boolean {
        return if (routeRepository.existsById(id)) {
            routeRepository.deleteById(id)
            true
        } else {
            false
        }
    }

    // 搜索功能
    override fun searchRoutes(
        keyword: String?,
        region: String?,
        difficulty: Int?,
        routeType: Int?,
        status: Int?,
        tag: String?,
        season: String?,
        minDistance: BigDecimal?,
        maxDistance: BigDecimal?,
        pageable: Pageable
    ): Page<Route> {
        return routeRepository.searchRoutes(keyword, region, difficulty, routeType, status, tag, season, pageable)
    }

    override fun searchByName(name: String, pageable: Pageable): Page<Route> {
        return routeRepository.findByNameContainingIgnoreCase(name, pageable)
    }

    override fun searchByRegion(region: String, pageable: Pageable): Page<Route> {
        return routeRepository.findByRegion(region, pageable)
    }

    override fun searchByDifficulty(difficulty: Int, pageable: Pageable): Page<Route> {
        return routeRepository.findByDifficulty(difficulty, pageable)
    }

    override fun searchByRouteType(routeType: Int, pageable: Pageable): Page<Route> {
        return routeRepository.findByRouteType(routeType, pageable)
    }

    override fun searchByStatus(status: Int, pageable: Pageable): Page<Route> {
        return routeRepository.findByStatus(status, pageable)
    }

    override fun searchByDistanceRange(minDistance: BigDecimal, maxDistance: BigDecimal, pageable: Pageable): Page<Route> {
        return routeRepository.findByDistanceBetween(minDistance, maxDistance, pageable)
    }

    // 热门和推荐
    override fun getPopularRoutes(): List<Route> {
        return routeRepository.findTop10ByOrderByPopularityDesc()
    }

    override fun incrementPopularity(id: String) {
        routeRepository.findById(id).ifPresent { route ->
            route.incrementPopularity()
            routeRepository.save(route)
        }
    }

    // 用户收藏路线功能
    override fun addToFavorites(userId: String, routeId: String): UserFavoriteRoute {
        val favoriteRoute = UserFavoriteRoute(
            userId = userId,
            routeId = routeId,
            favoritedAt = Instant.now()
        )
        return userFavoriteRouteRepository.save(favoriteRoute)
    }

    override fun removeFromFavorites(userId: String, routeId: String): Boolean {
        return userFavoriteRouteRepository.deleteByUserIdAndRouteId(userId, routeId) > 0
    }

    override fun getUserFavoriteRoutes(userId: String, pageable: Pageable): Page<Route> {
        return routeRepository.findUserFavoriteRoutes(userId, pageable)
    }

    override fun isRouteFavorited(userId: String, routeId: String): Boolean {
        return routeRepository.isRouteFavoritedByUser(userId, routeId)
    }

    override fun getRouteFavoriteCount(routeId: String): Long {
        return routeRepository.countRouteFavorites(routeId)
    }

    override fun getMostFavoritedRoutes(): List<Route> {
        val favoriteStats = userFavoriteRouteRepository.findMostFavoritedRoutes(Pageable.ofSize(10))
        return favoriteStats.content.mapNotNull { stat ->
            val routeId = stat[0] as String
            routeRepository.findById(routeId).orElse(null)
        }
    }

    // 用户完成路线功能
    override fun markRouteAsCompleted(userId: String, routeId: String): UserCompletedRoute {
        val completedRoute = UserCompletedRoute(
            userId = userId,
            routeId = routeId,
            completedAt = Instant.now()
        )
        return userCompletedRouteRepository.save(completedRoute)
    }

    override fun getUserCompletedRoutes(userId: String, pageable: Pageable): Page<Route> {
        return routeRepository.findUserCompletedRoutes(userId, pageable)
    }

    override fun isRouteCompleted(userId: String, routeId: String): Boolean {
        return routeRepository.isRouteCompletedByUser(userId, routeId)
    }

    override fun getRouteCompletionCount(routeId: String): Long {
        return routeRepository.countRouteCompletions(routeId)
    }

    override fun getMostCompletedRoutes(): List<Route> {
        val completionStats = userCompletedRouteRepository.findMostCompletedRoutes(Pageable.ofSize(10))
        return completionStats.content.mapNotNull { stat ->
            val routeId = stat[0] as String
            routeRepository.findById(routeId).orElse(null)
        }
    }

    override fun getUserCompletionStats(userId: String): Map<String, Any> {
        val yearlyStats = userCompletedRouteRepository.getUserYearlyCompletionStats(userId)
        val totalCompleted = userCompletedRouteRepository.countByUserId(userId)

        return mapOf(
            "totalCompleted" to totalCompleted,
            "yearlyStats" to yearlyStats
        )
    }

    // 地理位置相关
    override fun getRoutesByLocation(
        minLatitude: BigDecimal, maxLatitude: BigDecimal,
        minLongitude: BigDecimal, maxLongitude: BigDecimal,
        pageable: Pageable
    ): Page<Route> {
        return routeRepository.findByLatitudeBetweenAndLongitudeBetween(
            minLatitude, maxLatitude, minLongitude, maxLongitude, pageable
        )
    }

    // 创建者相关
    override fun getRoutesByCreator(createdBy: String, pageable: Pageable): Page<Route> {
        return routeRepository.findByCreatedBy(createdBy, pageable)
    }

    // 统计功能
    override fun getRouteStatistics(): Map<String, Any> {
        val totalRoutes = routeRepository.count()
        val popularRoutes = routeRepository.findTop10ByOrderByPopularityDesc()
        val avgPopularity = popularRoutes.map { it.popularity }.average()

        return mapOf(
            "totalRoutes" to totalRoutes,
            "avgPopularity" to avgPopularity,
            "mostPopularRoute" to (popularRoutes.firstOrNull()?.name ?: "无")
        )
    }

    override fun getRoutesByTag(tag: String, pageable: Pageable): Page<Route> {
        return routeRepository.findByTagsTag(tag, pageable)
    }

    override fun getRoutesBySeason(season: String, pageable: Pageable): Page<Route> {
        return routeRepository.findBySeasonsSeason(season, pageable)
    }

    // 验证
    override fun existsById(id: String): Boolean {
        return routeRepository.existsById(id)
    }

    override fun validateRoute(route: Route): Boolean {
        return route.name.isNotBlank() &&
               route.createdBy?.isNotBlank() == true &&
               route.difficulty != null &&
               route.difficulty in 0..3
    }
}