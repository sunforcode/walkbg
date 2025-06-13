package org.example.route.repository

import org.example.route.model.RouteImage
import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.query.Param
import org.springframework.stereotype.Repository

@Repository
interface RouteImageRepository : JpaRepository<RouteImage, Long> {
    
    /**
     * 根据路线ID查找图片
     */
    fun findByRouteIdOrderBySequenceNumber(routeId: String): List<RouteImage>
    
    /**
     * 根据路线ID查找图片（分页）
     */
    fun findByRouteId(routeId: String, pageable: Pageable): Page<RouteImage>
    
    /**
     * 查找封面图片
     */
    fun findByRouteIdAndIsCover(routeId: String, isCover: Boolean): List<RouteImage>
    
    /**
     * 统计路线的图片数量
     */
    fun countByRouteId(routeId: String): Long
    
    /**
     * 删除路线的所有图片
     */
    fun deleteByRouteId(routeId: String): Int
    
    /**
     * 查找路线的封面图片
     */
    @Query("SELECT ri FROM RouteImage ri WHERE ri.route.id = :routeId AND ri.isCover = true")
    fun findCoverImageByRouteId(@Param("routeId") routeId: String): RouteImage?
}