package org.example.repository

import org.example.model.Route
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Query
import org.springframework.stereotype.Repository

@Repository
interface RouteRepository : JpaRepository<Route, String> {
    
    fun findByName(name: String): List<Route>
    
    fun findByRegion(region: String): List<Route>
    
    fun findByCreatedBy(createdBy: String): List<Route>
    
    @Query("SELECT r FROM Route r JOIN r.tags t WHERE t.tag = :tag")
    fun findByTag(tag: String): List<Route>
    
    @Query("SELECT r FROM Route r JOIN r.seasons s WHERE s.season = :season")
    fun findBySeason(season: String): List<Route>
    
    fun findByDifficultyLessThanEqual(difficulty: Int): List<Route>
    
    fun findByDifficultyGreaterThanEqual(difficulty: Int): List<Route>
    
    fun findByDistanceBetween(minDistance: Double, maxDistance: Double): List<Route>
    
    fun findByPopularityGreaterThanOrderByPopularityDesc(minPopularity: Int): List<Route>
    
    fun findTop10ByOrderByPopularityDesc(): List<Route>
}