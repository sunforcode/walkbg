package org.example.route.repository

import org.example.route.model.RouteRating
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.query.Param
import org.springframework.stereotype.Repository

@Repository
interface RouteRatingRepository : JpaRepository<RouteRating, Long> {
    
    /**
     * 根据路线ID查找评分
     */
    fun findByRouteId(routeId: String): RouteRating?
    
    /**
     * 删除路线的评分
     */
    fun deleteByRouteId(routeId: String): Int
    
    /**
     * 根据总体评分范围查找评分
     */
    @Query("SELECT rr FROM RouteRating rr WHERE rr.overall BETWEEN :minRating AND :maxRating")
    fun findByOverallRatingRange(
        @Param("minRating") minRating: Double,
        @Param("maxRating") maxRating: Double
    ): List<RouteRating>
    
    /**
     * 获取平均评分
     */
    @Query("SELECT AVG(rr.overall) FROM RouteRating rr WHERE rr.overall IS NOT NULL")
    fun getAverageOverallRating(): Double?
}