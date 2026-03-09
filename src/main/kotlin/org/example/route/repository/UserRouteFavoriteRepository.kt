package org.example.route.repository

import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.query.Param
import org.springframework.stereotype.Repository
import org.example.route.model.UserRouteFavorite

@Repository
interface UserRouteFavoriteRepository : JpaRepository<UserRouteFavorite, Long> {
    
    /**
     * 检查用户是否收藏了路线
     */
    fun existsByUserIdAndRouteId(userId: String, routeId: String): Boolean
    
    /**
     * 根据用户ID和路线ID查找收藏记录
     */
    fun findByUserIdAndRouteId(userId: String, routeId: String): UserRouteFavorite?
    
    /**
     * 根据用户ID查找所有收藏的路线
     */
    fun findByUserId(userId: String, pageable: Pageable): Page<UserRouteFavorite>
    
    /**
     * 根据用户ID查找所有收藏的路线（不分页）
     */
    fun findByUserId(userId: String): List<UserRouteFavorite>
    
    /**
     * 根据路线ID查找所有收藏该路线的用户
     */
    fun findByRouteId(routeId: String, pageable: Pageable): Page<UserRouteFavorite>
    
    /**
     * 删除用户对路线的收藏
     */
    fun deleteByUserIdAndRouteId(userId: String, routeId: String): Long
    
    /**
     * 统计用户的收藏数量
     */
    fun countByUserId(userId: String): Long
    
    /**
     * 统计路线被收藏的次数
     */
    fun countByRouteId(routeId: String): Long
}
