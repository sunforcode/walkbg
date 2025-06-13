package org.example.user.service

import org.example.user.model.User
import org.example.user.model.UserFavoriteRoute
import org.example.user.model.UserCompletedRoute
import org.example.user.model.UserEquipmentItem
import org.example.user.model.UserEquipmentItemId
import org.example.user.repository.UserRepository
import org.example.user.repository.UserFavoriteRouteRepository
import org.example.user.repository.UserCompletedRouteRepository
import org.example.user.repository.UserEquipmentItemRepository
import org.example.infrastructure.repository.RouteRepository
import org.example.trip.repository.TripParticipantRepository
import org.example.trip.model.TripParticipant
import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.time.Instant
import java.util.UUID

/**
 * 用户服务实现类
 */
@Service
@Transactional
class UserServiceImpl(
    private val userRepository: UserRepository,
    private val userFavoriteRouteRepository: UserFavoriteRouteRepository,
    private val userCompletedRouteRepository: UserCompletedRouteRepository,
    private val userEquipmentItemRepository: UserEquipmentItemRepository,
    private val tripParticipantRepository: TripParticipantRepository,
    private val routeRepository: RouteRepository
) : UserService {

    // 基础CRUD操作
    override fun getAllUsers(pageable: Pageable): Page<User> {
        return userRepository.findAll(pageable)
    }

    override fun getUserById(id: String): User? {
        return userRepository.findById(id).orElse(null)
    }

    override fun getUserByEmail(email: String): User? {
        return userRepository.findByEmail(email)
    }

    override fun getUserByUsername(username: String): User? {
        return userRepository.findByUsername(username)
    }

    override fun createUser(user: User): User {
        return userRepository.save(user)
    }

    override fun updateUser(id: String, user: User): User? {
        return if (userRepository.existsById(id)) {
            userRepository.save(user.copy(id = id, updatedAt = Instant.now()))
        } else {
            null
        }
    }

    override fun deleteUser(id: String): Boolean {
        return if (userRepository.existsById(id)) {
            userRepository.deleteById(id)
            true
        } else {
            false
        }
    }

    override fun searchUsers(keyword: String?, isActive: Boolean?, pageable: Pageable): Page<User> {
        return userRepository.searchUsers(keyword, isActive, pageable)
    }

    // 用户统计信息
    override fun getUserStatistics(): Map<String, Any> {
        return userRepository.getUserStatistics()
    }

    override fun getUserStats(userId: String): Map<String, Any> {
        val completedRoutes = userRepository.countUserCompletedRoutes(userId)
        val favoriteRoutes = userRepository.countUserFavoriteRoutes(userId)
        val equipmentLists = userRepository.countUserEquipmentLists(userId)
        val tripParticipations = userRepository.countUserTripParticipations(userId)

        return mapOf(
            "completedRoutes" to completedRoutes,
            "favoriteRoutes" to favoriteRoutes,
            "equipmentLists" to equipmentLists,
            "tripParticipations" to tripParticipations
        )
    }

    override fun getMostActiveUsers(pageable: Pageable): Page<User> {
        return userRepository.findTop10MostActiveUsers(pageable)
    }

    // 用户收藏路线管理
    override fun addFavoriteRoute(userId: String, routeId: String): UserFavoriteRoute {
        val favoriteRoute = UserFavoriteRoute(
            id = UUID.randomUUID().toString()
        )
        return userFavoriteRouteRepository.save(favoriteRoute)
    }

    override fun removeFavoriteRoute(userId: String, routeId: String): Boolean {
        return userFavoriteRouteRepository.deleteByUserIdAndRouteId(userId, routeId) > 0
    }

    override fun getUserFavoriteRoutes(userId: String, pageable: Pageable): Page<UserFavoriteRoute> {
        return userFavoriteRouteRepository.findByUserId(userId, pageable)
    }

    override fun isRouteFavorited(userId: String, routeId: String): Boolean {
        return userFavoriteRouteRepository.existsByUserIdAndRouteId(userId, routeId)
    }

    override fun countUserFavoriteRoutes(userId: String): Long {
        return userFavoriteRouteRepository.countByUserId(userId)
    }

    // 用户完成路线管理
    override fun markRouteAsCompleted(userId: String, routeId: String): UserCompletedRoute {
        val completedRoute = UserCompletedRoute(
            userId = userId,
            routeId = routeId,
            completedAt = Instant.now()
        )
        return userCompletedRouteRepository.save(completedRoute)
    }

    override fun getUserCompletedRoutes(userId: String, pageable: Pageable): Page<UserCompletedRoute> {
        return userCompletedRouteRepository.findByUserId(userId, pageable)
    }

    override fun countUserCompletedRoutes(userId: String): Long {
        return userCompletedRouteRepository.countByUserId(userId)
    }

    override fun getUserCompletionStats(userId: String): Map<String, Any> {
        val yearlyStats = userCompletedRouteRepository.getUserYearlyCompletionStats(userId)
        val totalCompleted = userCompletedRouteRepository.countByUserId(userId)
        
        return mapOf(
            "totalCompleted" to totalCompleted,
            "yearlyStats" to yearlyStats
        )
    }

    // 用户装备库存管理
    override fun getUserEquipmentItems(userId: String, pageable: Pageable): Page<UserEquipmentItem> {
        return userEquipmentItemRepository.findByUserId(userId, pageable)
    }

    override fun addEquipmentToUser(userId: String, equipmentItemId: String, quantity: Int, notes: String?): UserEquipmentItem {
        val userEquipment = UserEquipmentItem(
            userId = userId,
            equipmentItemId = equipmentItemId,
            quantity = quantity,
            notes = notes
        )
        return userEquipmentItemRepository.save(userEquipment)
    }

    override fun updateUserEquipment(userId: String, equipmentItemId: String, quantity: Int, notes: String?): UserEquipmentItem? {
        val existing = userEquipmentItemRepository.findById(UserEquipmentItemId(userId, equipmentItemId)).orElse(null)
        return if (existing != null) {
            val updated = existing.copy(quantity = quantity, notes = notes)
            userEquipmentItemRepository.save(updated)
        } else {
            null
        }
    }

    override fun removeEquipmentFromUser(userId: String, equipmentItemId: String): Boolean {
        return userEquipmentItemRepository.deleteByUserIdAndEquipmentItemId(userId, equipmentItemId) > 0
    }

    override fun getUserEquipmentStats(userId: String): Map<String, Any> {
        return userEquipmentItemRepository.getUserEquipmentStats(userId)
    }

    // 用户参与行程管理
    override fun getUserTripParticipations(userId: String, pageable: Pageable): Page<TripParticipant> {
        return tripParticipantRepository.findByUserId(userId, pageable)
    }

    override fun countUserTripParticipations(userId: String): Long {
        return tripParticipantRepository.countByUserId(userId)
    }

    // 用户验证
    override fun existsByEmail(email: String): Boolean {
        return userRepository.existsByEmail(email)
    }

    override fun existsByUsername(username: String): Boolean {
        return userRepository.existsByUsername(username)
    }

    override fun validateUser(username: String, email: String): Boolean {
        return !existsByUsername(username) && !existsByEmail(email)
    }
}