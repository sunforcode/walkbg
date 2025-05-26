package org.example.controller

import org.example.dto.RouteDto
import org.example.dto.toDto
import org.example.model.Route
import org.example.service.RouteService
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.*
import java.time.Instant
import java.util.*

@RestController
@RequestMapping("/api/routes")
class RouteController(private val routeService: RouteService) {

    @GetMapping
    fun getAllRoutes(): ResponseEntity<List<RouteDto>> {
        return ResponseEntity.ok(routeService.getAllRoutes().map { it.toDto() })
    }

    @GetMapping("/{id}")
    fun getRouteById(@PathVariable id: String): ResponseEntity<RouteDto> {
        val route = routeService.getRouteById(id)
        return if (route != null) {
            ResponseEntity.ok(route.toDto())
        } else {
            ResponseEntity.notFound().build()
        }
    }

    @GetMapping("/search/name")
    fun getRoutesByName(@RequestParam name: String): ResponseEntity<List<RouteDto>> {
        return ResponseEntity.ok(routeService.getRoutesByName(name).map { it.toDto() })
    }

    @GetMapping("/search/region")
    fun getRoutesByRegion(@RequestParam region: String): ResponseEntity<List<RouteDto>> {
        return ResponseEntity.ok(routeService.getRoutesByRegion(region).map { it.toDto() })
    }

    @GetMapping("/search/tag")
    fun getRoutesByTag(@RequestParam tag: String): ResponseEntity<List<RouteDto>> {
        return ResponseEntity.ok(routeService.getRoutesByTag(tag).map { it.toDto() })
    }

    @GetMapping("/search/season")
    fun getRoutesBySeason(@RequestParam season: String): ResponseEntity<List<RouteDto>> {
        return ResponseEntity.ok(routeService.getRoutesBySeason(season).map { it.toDto() })
    }

    @GetMapping("/search/difficulty/max")
    fun getRoutesByMaxDifficulty(@RequestParam difficulty: Int): ResponseEntity<List<RouteDto>> {
        return ResponseEntity.ok(routeService.getRoutesByDifficultyLessThanEqual(difficulty).map { it.toDto() })
    }

    @GetMapping("/search/difficulty/min")
    fun getRoutesByMinDifficulty(@RequestParam difficulty: Int): ResponseEntity<List<RouteDto>> {
        return ResponseEntity.ok(routeService.getRoutesByDifficultyGreaterThanEqual(difficulty).map { it.toDto() })
    }

    @GetMapping("/search/distance")
    fun getRoutesByDistanceRange(
        @RequestParam minDistance: Double,
        @RequestParam maxDistance: Double
    ): ResponseEntity<List<RouteDto>> {
        return ResponseEntity.ok(routeService.getRoutesByDistanceRange(minDistance, maxDistance).map { it.toDto() })
    }

    @GetMapping("/popular")
    fun getPopularRoutes(@RequestParam(required = false, defaultValue = "0") minPopularity: Int): ResponseEntity<List<RouteDto>> {
        return ResponseEntity.ok(routeService.getPopularRoutes(minPopularity).map { it.toDto() })
    }

    @GetMapping("/top10")
    fun getTop10PopularRoutes(): ResponseEntity<List<RouteDto>> {
        return ResponseEntity.ok(routeService.getTop10PopularRoutes().map { it.toDto() })
    }

    @PostMapping
    fun createRoute(@RequestBody route: Route): ResponseEntity<RouteDto> {
        return ResponseEntity.status(HttpStatus.CREATED).body(routeService.createRoute(route).toDto())
    }

    @PostMapping("/sample")
    fun createSampleRoute(): ResponseEntity<RouteDto> {
        val route = Route(
            id = UUID.randomUUID().toString(),
            name = "黄山经典徒步路线",
            description = "这条路线带您游览黄山最著名的景点，包括迎客松、光明顶和西海大峡谷。",
            region = "黄山风景区",
            distance = 15.5,
            duration = "8小时",
            difficulty = 2,
            createdAt = Instant.now(),
            updatedAt = Instant.now()
        )

        val savedRoute = routeService.createRoute(route)

        // 添加季节和标签
        savedRoute.addSeason("春季")
        savedRoute.addSeason("秋季")
        savedRoute.addTag("山岳")
        savedRoute.addTag("森林")

        val updatedRoute = routeService.updateRoute(savedRoute.id, savedRoute)

        return ResponseEntity.status(HttpStatus.CREATED).body(updatedRoute?.toDto())
    }

    @PutMapping("/{id}")
    fun updateRoute(@PathVariable id: String, @RequestBody route: Route): ResponseEntity<RouteDto> {
        val updatedRoute = routeService.updateRoute(id, route)
        return if (updatedRoute != null) {
            ResponseEntity.ok(updatedRoute.toDto())
        } else {
            ResponseEntity.notFound().build()
        }
    }

    @DeleteMapping("/{id}")
    fun deleteRoute(@PathVariable id: String): ResponseEntity<Void> {
        val deleted = routeService.deleteRoute(id)
        return if (deleted) {
            ResponseEntity.noContent().build()
        } else {
            ResponseEntity.notFound().build()
        }
    }

    @PostMapping("/{id}/popularity")
    fun incrementRoutePopularity(@PathVariable id: String): ResponseEntity<RouteDto> {
        val route = routeService.incrementRoutePopularity(id)
        return if (route != null) {
            ResponseEntity.ok(route.toDto())
        } else {
            ResponseEntity.notFound().build()
        }
    }
}