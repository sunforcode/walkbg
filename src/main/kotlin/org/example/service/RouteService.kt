package org.example.service

import org.example.model.*
import org.example.repository.RouteRepository
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.time.Instant

@Service
class RouteService(private val routeRepository: RouteRepository) {

    fun getAllRoutes(): List<Route> = routeRepository.findAll()
    
    fun getRouteById(id: String): Route? = routeRepository.findById(id).orElse(null)
    
    fun getRoutesByName(name: String): List<Route> = routeRepository.findByName(name)
    
    fun getRoutesByRegion(region: String): List<Route> = routeRepository.findByRegion(region)
    
    fun getRoutesByTag(tag: String): List<Route> = routeRepository.findByTag(tag)
    
    fun getRoutesBySeason(season: String): List<Route> = routeRepository.findBySeason(season)
    
    fun getRoutesByDifficultyLessThanEqual(difficulty: Int): List<Route> = 
        routeRepository.findByDifficultyLessThanEqual(difficulty)
    
    fun getRoutesByDifficultyGreaterThanEqual(difficulty: Int): List<Route> = 
        routeRepository.findByDifficultyGreaterThanEqual(difficulty)
    
    fun getRoutesByDistanceRange(minDistance: Double, maxDistance: Double): List<Route> = 
        routeRepository.findByDistanceBetween(minDistance, maxDistance)
    
    fun getPopularRoutes(minPopularity: Int = 0): List<Route> = 
        routeRepository.findByPopularityGreaterThanOrderByPopularityDesc(minPopularity)
    
    fun getTop10PopularRoutes(): List<Route> = routeRepository.findTop10ByOrderByPopularityDesc()
    
    @Transactional
    fun createRoute(route: Route): Route = routeRepository.save(route)
    
    @Transactional
    fun updateRoute(id: String, route: Route): Route? {
        return if (routeRepository.existsById(id)) {
            val updatedRoute = route.copy(
                id = id,
                updatedAt = Instant.now()
            )
            routeRepository.save(updatedRoute)
        } else {
            null
        }
    }
    
    @Transactional
    fun deleteRoute(id: String): Boolean {
        return if (routeRepository.existsById(id)) {
            routeRepository.deleteById(id)
            true
        } else {
            false
        }
    }
    
    @Transactional
    fun addWaypointToRoute(routeId: String, waypoint: Waypoint): Route? {
        val route = getRouteById(routeId) ?: return null
        route.addWaypoint(waypoint)
        return routeRepository.save(route)
    }
    
    @Transactional
    fun addSegmentToRoute(routeId: String, segment: Segment): Route? {
        val route = getRouteById(routeId) ?: return null
        route.addSegment(segment)
        return routeRepository.save(route)
    }
    
    @Transactional
    fun addDailyPlanToRoute(routeId: String, dailyPlan: DailyPlan): Route? {
        val route = getRouteById(routeId) ?: return null
        route.addDailyPlan(dailyPlan)
        return routeRepository.save(route)
    }
    
    @Transactional
    fun addTagToRoute(routeId: String, tag: String): Route? {
        val route = getRouteById(routeId) ?: return null
        route.addTag(tag)
        return routeRepository.save(route)
    }
    
    @Transactional
    fun addSeasonToRoute(routeId: String, season: String): Route? {
        val route = getRouteById(routeId) ?: return null
        route.addSeason(season)
        return routeRepository.save(route)
    }
    
    @Transactional
    fun addImageToRoute(routeId: String, imageUrl: String, isCover: Boolean = false): Route? {
        val route = getRouteById(routeId) ?: return null
        route.addImage(imageUrl, isCover)
        return routeRepository.save(route)
    }
    
    @Transactional
    fun incrementRoutePopularity(routeId: String): Route? {
        val route = getRouteById(routeId) ?: return null
        val updatedRoute = route.copy(popularity = route.popularity + 1)
        return routeRepository.save(updatedRoute)
    }
}