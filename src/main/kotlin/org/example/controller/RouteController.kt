package org.example.controller

import org.example.model.Route
import org.example.service.RouteService
import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.*

/**
 * 路线控制器
 */
@RestController
@RequestMapping("/api/routes")
@CrossOrigin(origins = ["*"])
class RouteController(
    private val routeService: RouteService
) {

    /**
     * 获取所有路线（分页）
     */
    @GetMapping
    fun getAllRoutes(pageable: Pageable): ResponseEntity<Page<Route>> {
        val routes = routeService.getAllRoutes(pageable)
        return ResponseEntity.ok(routes)
    }

    /**
     * 根据ID获取路线
     */
    @GetMapping("/{id}")
    fun getRouteById(@PathVariable id: String): ResponseEntity<Route> {
        val route = routeService.getRouteById(id)
        return if (route != null) {
            ResponseEntity.ok(route)
        } else {
            ResponseEntity.notFound().build()
        }
    }

    /**
     * 创建路线
     */
    @PostMapping
    fun createRoute(@RequestBody route: Route): ResponseEntity<Route> {
        return try {
            val createdRoute = routeService.createRoute(route)
            ResponseEntity.status(HttpStatus.CREATED).body(createdRoute)
        } catch (e: Exception) {
            ResponseEntity.badRequest().build()
        }
    }

    /**
     * 更新路线
     */
    @PutMapping("/{id}")
    fun updateRoute(@PathVariable id: String, @RequestBody route: Route): ResponseEntity<Route> {
        val updatedRoute = routeService.updateRoute(id, route)
        return if (updatedRoute != null) {
            ResponseEntity.ok(updatedRoute)
        } else {
            ResponseEntity.notFound().build()
        }
    }

    /**
     * 删除路线
     */
    @DeleteMapping("/{id}")
    fun deleteRoute(@PathVariable id: String): ResponseEntity<Void> {
        val deleted = routeService.deleteRoute(id)
        return if (deleted) {
            ResponseEntity.noContent().build()
        } else {
            ResponseEntity.notFound().build()
        }
    }

    /**
     * 搜索路线
     */
    @GetMapping("/search")
    fun searchRoutes(
        @RequestParam(required = false) keyword: String?,
        @RequestParam(required = false) difficulty: Int?,
        @RequestParam(required = false) region: String?,
        pageable: Pageable
    ): ResponseEntity<Page<Route>> {
        val routes = routeService.searchRoutes(keyword, region, difficulty, pageable = pageable)
        return ResponseEntity.ok(routes)
    }

    /**
     * 根据难度获取路线
     */
    @GetMapping("/difficulty/{difficulty}")
    fun getRoutesByDifficulty(
        @PathVariable difficulty: Int,
        pageable: Pageable
    ): ResponseEntity<Page<Route>> {
        val routes = routeService.searchByDifficulty(difficulty, pageable)
        return ResponseEntity.ok(routes)
    }

    /**
     * 根据地区获取路线
     */
    @GetMapping("/region/{region}")
    fun getRoutesByRegion(
        @PathVariable region: String,
        pageable: Pageable
    ): ResponseEntity<Page<Route>> {
        val routes = routeService.searchByRegion(region, pageable)
        return ResponseEntity.ok(routes)
    }

    /**
     * 获取热门路线
     */
    @GetMapping("/popular")
    fun getPopularRoutes(): ResponseEntity<List<Route>> {
        val routes = routeService.getPopularRoutes()
        return ResponseEntity.ok(routes)
    }

    /**
     * 获取最受收藏的路线
     */
    @GetMapping("/most-favorited")
    fun getMostFavoritedRoutes(): ResponseEntity<List<Route>> {
        val routes = routeService.getMostFavoritedRoutes()
        return ResponseEntity.ok(routes)
    }

    /**
     * 获取最多完成的路线
     */
    @GetMapping("/most-completed")
    fun getMostCompletedRoutes(): ResponseEntity<List<Route>> {
        val routes = routeService.getMostCompletedRoutes()
        return ResponseEntity.ok(routes)
    }

    /**
     * 增加路线热度
     */
    @PostMapping("/{id}/increment-popularity")
    fun incrementPopularity(@PathVariable id: String): ResponseEntity<Route> {
        return try {
            routeService.incrementPopularity(id)
            val route = routeService.getRouteById(id)
            if (route != null) {
                ResponseEntity.ok(route)
            } else {
                ResponseEntity.notFound().build()
            }
        } catch (e: Exception) {
            ResponseEntity.badRequest().build()
        }
    }

    /**
     * 获取路线统计信息
     */
    @GetMapping("/statistics")
    fun getRouteStatistics(): ResponseEntity<Map<String, Any>> {
        val statistics = routeService.getRouteStatistics()
        return ResponseEntity.ok(statistics)
    }

    /**
     * 根据创建者获取路线
     */
    @GetMapping("/creator/{creatorId}")
    fun getRoutesByCreator(
        @PathVariable creatorId: String,
        pageable: Pageable
    ): ResponseEntity<Page<Route>> {
        val routes = routeService.getRoutesByCreator(creatorId, pageable)
        return ResponseEntity.ok(routes)
    }

    /**
     * 根据标签获取路线
     */
    @GetMapping("/tag/{tag}")
    fun getRoutesByTag(
        @PathVariable tag: String,
        pageable: Pageable
    ): ResponseEntity<Page<Route>> {
        val routes = routeService.getRoutesByTag(tag, pageable)
        return ResponseEntity.ok(routes)
    }

    /**
     * 根据季节获取路线
     */
    @GetMapping("/season/{season}")
    fun getRoutesBySeason(
        @PathVariable season: String,
        pageable: Pageable
    ): ResponseEntity<Page<Route>> {
        val routes = routeService.getRoutesBySeason(season, pageable)
        return ResponseEntity.ok(routes)
    }
}