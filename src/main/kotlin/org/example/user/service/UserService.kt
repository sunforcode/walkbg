package org.example.user.service

import org.example.user.model.User
import org.example.user.model.UserFavoriteRoute
import org.example.user.model.UserCompletedRoute
import org.example.user.model.UserEquipmentItem
import org.example.trip.model.TripParticipant
import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable

/**
 * 用户服务接口
 */
interface UserService {

    // 基础CRUD操作
    fun getAllUsers(pageable: Pageable): Page<User>
    fun getUserById(id: String): User?
    fun getUserByEmail(email: String): User?
    fun getUserByUsername(username: String): User?
    fun createUser(user: User): User
    fun updateUser(id: String, user: User): User?
    fun deleteUser(id: String): Boolean
    fun searchUsers(keyword: String?, isActive: Boolean?, pageable: Pageable): Page<User>
    
    // 用户统计信息
    fun getUserStatistics(): Map<String, Any>
    fun getUserStats(userId: String): Map<String, Any>
    fun getMostActiveUsers(pageable: Pageable): Page<User>
    
    // 用户收藏路线管理
    fun addFavoriteRoute(userId: String, routeId: String): UserFavoriteRoute
    fun removeFavoriteRoute(userId: String, routeId: String): Boolean
    fun getUserFavoriteRoutes(userId: String, pageable: Pageable): Page<UserFavoriteRoute>
    fun isRouteFavorited(userId: String, routeId: String): Boolean
    fun countUserFavoriteRoutes(userId: String): Long
    
    // 用户完成路线管理
    fun markRouteAsCompleted(userId: String, routeId: String): UserCompletedRoute
    fun getUserCompletedRoutes(userId: String, pageable: Pageable): Page<UserCompletedRoute>
    fun countUserCompletedRoutes(userId: String): Long
    fun getUserCompletionStats(userId: String): Map<String, Any>

    // 用户装备库存管理
    fun getUserEquipmentItems(userId: String, pageable: Pageable): Page<UserEquipmentItem>
    fun addEquipmentToUser(userId: String, equipmentItemId: String, quantity: Int, notes: String?): UserEquipmentItem
    fun updateUserEquipment(userId: String, equipmentItemId: String, quantity: Int, notes: String?): UserEquipmentItem?
    fun removeEquipmentFromUser(userId: String, equipmentItemId: String): Boolean
    fun getUserEquipmentStats(userId: String): Map<String, Any>
    
    // 用户参与行程管理
    fun getUserTripParticipations(userId: String, pageable: Pageable): Page<TripParticipant>
    fun countUserTripParticipations(userId: String): Long
    
    // 用户验证
    fun existsByEmail(email: String): Boolean
    fun existsByUsername(username: String): Boolean
    fun validateUser(username: String, email: String): Boolean
}