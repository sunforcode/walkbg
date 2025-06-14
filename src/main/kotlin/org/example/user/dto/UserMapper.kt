package org.example.user.dto

import org.example.user.model.User
import org.example.user.model.UserFavoriteRoute
import org.example.user.model.UserCompletedRoute
import org.example.route.model.Route
import java.time.Instant
import java.util.UUID

/**
 * User实体和DTO之间的映射工具类
 */
object UserMapper {

    /**
     * 将User实体转换为UserResponseDTO
     */
    fun toUserResponseDTO(user: User): UserResponseDTO {
        return UserResponseDTO(
            id = user.id,
            username = user.username,
            nickname = user.nickname,
            email = user.email,
            avatarUrl = user.avatarUrl,
            phone = user.phone,
            bio = user.bio,
            createdAt = user.createdAt,
            lastLoginAt = user.lastLoginAt
        )
    }

    /**
     * 将User实体列表转换为UserResponseDTO列表
     */
    fun toUserResponseDTOList(users: List<User>): List<UserResponseDTO> {
        return users.map { toUserResponseDTO(it) }
    }

    /**
     * 将CreateUserRequestDTO转换为User实体
     */
    fun toUser(createRequest: CreateUserRequestDTO): User {
        return User(
            id = UUID.randomUUID().toString(),
            username = createRequest.username,
            nickname = createRequest.nickname,
            email = createRequest.email,
            avatarUrl = createRequest.avatarUrl,
            phone = createRequest.phone,
            bio = createRequest.bio,
            createdAt = Instant.now(),
            updatedAt = Instant.now()
        )
    }

    /**
     * 使用UpdateUserRequestDTO更新User实体
     */
    fun updateUser(user: User, updateRequest: UpdateUserRequestDTO): User {
        return user.copy(
            nickname = updateRequest.nickname ?: user.nickname,
            avatarUrl = updateRequest.avatarUrl ?: user.avatarUrl,
            phone = updateRequest.phone ?: user.phone,
            bio = updateRequest.bio ?: user.bio,
            updatedAt = Instant.now()
        )
    }

    /**
     * 将UserFavoriteRoute实体转换为UserFavoriteRouteDTO
     */
    fun toUserFavoriteRouteDTO(userFavoriteRoute: UserFavoriteRoute, route: Route): UserFavoriteRouteDTO {
        return UserFavoriteRouteDTO(
            routeId = route.id,
            routeName = route.name,
            routeDescription = route.description,
            favoritedAt = userFavoriteRoute.createdAt
        )
    }

    /**
     * 将UserCompletedRoute实体转换为UserCompletedRouteDTO
     */
    fun toUserCompletedRouteDTO(userCompletedRoute: UserCompletedRoute, route: Route): UserCompletedRouteDTO {
        return UserCompletedRouteDTO(
            routeId = route.id,
            routeName = route.name,
            routeDescription = route.description,
            completedAt = userCompletedRoute.completedAt
        )
    }

    /**
     * 将统计数据转换为UserStatsDTO
     */
    fun toUserStatsDTO(stats: Map<String, Any>): UserStatsDTO {
        return UserStatsDTO(
            completedRoutes = (stats["completedRoutes"] as? Number)?.toLong() ?: 0L,
            favoriteRoutes = (stats["favoriteRoutes"] as? Number)?.toLong() ?: 0L,
            equipmentLists = (stats["equipmentLists"] as? Number)?.toLong() ?: 0L,
            tripParticipations = (stats["tripParticipations"] as? Number)?.toLong() ?: 0L
        )
    }
}
