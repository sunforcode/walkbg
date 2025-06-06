package org.example.service

import org.example.model.Route
import org.example.model.UserFavoriteRoute
import org.example.model.UserCompletedRoute
import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import java.math.BigDecimal

/**
 * 路线服务接口
 */
interface RouteService {
    
    // 基础CRUD操作
    fun getAllRoutes(pageable: Pageable): Page<Route>
    fun getRouteById(id: String): Route?
    fun createRoute(route: Route): Route
    fun updateRoute(id: String, route: Route): Route?
    fun deleteRoute(id: String): Boolean
    
    // 搜索功能
    fun searchRoutes(
        keyword: String? = null,
        region: String? = null,
        difficulty: Int? = null, // 改为Int类型
        routeType: Int? = null,
        status: Int? = null,
        tag: String? = null,
        season: String? = null,
        minDistance: BigDecimal? = null,
        maxDistance: BigDecimal? = null,
        pageable: Pageable
    ): Page<Route>
    
    fun searchByName(name: String, pageable: Pageable): Page<Route>
    fun searchByRegion(region: String, pageable: Pageable): Page<Route>
    fun searchByDifficulty(difficulty: Int, pageable: Pageable): Page<Route> // 改为Int类型
    fun searchByRouteType(routeType: Int, pageable: Pageable): Page<Route>
    fun searchByStatus(status: Int, pageable: Pageable): Page<Route>
    fun searchByDistanceRange(minDistance: BigDecimal, maxDistance: BigDecimal, pageable: Pageable): Page<Route>

    // 热门和推荐
    fun getPopularRoutes(): List<Route>
    fun incrementPopularity(id: String)

    // 用户收藏路线功能
    fun addToFavorites(userId: String, routeId: String): UserFavoriteRoute
    fun removeFromFavorites(userId: String, routeId: String): Boolean
    fun getUserFavoriteRoutes(userId: String, pageable: Pageable): Page<Route>
    fun isRouteFavorited(userId: String, routeId: String): Boolean
    fun getRouteFavoriteCount(routeId: String): Long
    fun getMostFavoritedRoutes(): List<Route>

    // 用户完成路线功能
    fun markRouteAsCompleted(userId: String, routeId: String): UserCompletedRoute
    fun getUserCompletedRoutes(userId: String, pageable: Pageable): Page<Route>
    fun isRouteCompleted(userId: String, routeId: String): Boolean
    fun getRouteCompletionCount(routeId: String): Long
    fun getMostCompletedRoutes(): List<Route>
    fun getUserCompletionStats(userId: String): Map<String, Any>

    // 地理位置相关
    fun getRoutesByLocation(
        minLatitude: BigDecimal, maxLatitude: BigDecimal,
        minLongitude: BigDecimal, maxLongitude: BigDecimal,
        pageable: Pageable
    ): Page<Route>

    // 创建者相关
    fun getRoutesByCreator(createdBy: String, pageable: Pageable): Page<Route>

    // 统计功能
    fun getRouteStatistics(): Map<String, Any>
    fun getRoutesByTag(tag: String, pageable: Pageable): Page<Route>
    fun getRoutesBySeason(season: String, pageable: Pageable): Page<Route>

    // 验证
    fun existsById(id: String): Boolean
    fun validateRoute(route: Route): Boolean
}