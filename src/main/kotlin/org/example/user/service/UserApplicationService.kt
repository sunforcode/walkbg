package org.example.user.service

import org.example.common.exception.BusinessException
import org.example.user.dto.UserBasicResponse
import org.example.user.dto.UserCreateRequest
import org.example.user.model.User
import org.example.common.util.IdGenerator
import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.time.Instant

/**
 * 用户应用服务
 * 职责：业务用例编排、跨领域协调、DTO转换
 * 严格遵循分层架构，仅依赖DomainService层，不直接操作Repository
 */
@Service
class UserApplicationService(
    private val userService: UserService
) {

    /**
     * 业务用例：分页搜索用户
     * 通过领域服务进行搜索，遵循分层架构
     */
    @Transactional(readOnly = true)
    fun searchUsers(
        keyword: String? = null,
        status: Int? = null,
        pageable: Pageable
    ): Page<UserBasicResponse> {
        // 1. 通过领域服务进行搜索
        val users = userService.searchUsers(keyword, status, pageable)

        // 2. DTO转换（应用层职责）
        return users.map { UserBasicResponse.fromUser(it) }
    }

    /**
     * 业务用例：根据ID获取用户
     * 通过领域服务协调业务逻辑和数据访问
     */
    @Transactional(readOnly = true)
    fun getUserById(userId: String): UserBasicResponse? {
        // 1. 通过领域服务获取用户
        val user = userService.getUserById(userId) ?: return null

        // 2. DTO转换（应用层职责）
        return UserBasicResponse.fromUser(user)
    }

    /**
     * 业务用例：创建用户
     * 通过领域服务协调业务逻辑和数据访问
     */
    @Transactional
    fun createUser(request: UserCreateRequest): UserBasicResponse {
        // 1. 构建用户实体
        val user = User(
            id = IdGenerator.generateIdWithPrefix("user"),
            username = request.username,
            nickname = request.nickname ?: request.username,
            email = request.email,
            avatarUrl = request.avatarUrl,
            phone = request.phone,
            createdAt = Instant.now(),
            updatedAt = Instant.now()
        )

        // 2. 业务规则验证和创建（通过领域服务）
        val savedUser = userService.createUserWithValidation(user)

        // 3. DTO转换（应用层职责）
        return UserBasicResponse.fromUser(savedUser)
    }

    /**
     * 业务用例：根据用户名获取用户
     * 内部使用，支持其他业务用例
     */
    @Transactional(readOnly = true)
    fun getUserByUsername(username: String): UserBasicResponse? {
        val user = userService.getUserByUsername(username) ?: return null
        return UserBasicResponse.fromUser(user)
    }

    /**
     * 业务用例：根据邮箱获取用户
     * 内部使用，支持其他业务用例
     */
    @Transactional(readOnly = true)
    fun getUserByEmail(email: String): UserBasicResponse? {
        val user = userService.getUserByEmail(email) ?: return null
        return UserBasicResponse.fromUser(user)
    }

    /**
     * 业务用例：更新用户信息
     * 通过领域服务协调业务逻辑和数据访问
     */
    @Transactional
    fun updateUser(userId: String, request: UserCreateRequest): UserBasicResponse {
        // 1. 构建更新的用户实体
        val user = User(
            id = userId,
            username = request.username,
            nickname = request.nickname ?: request.username,
            email = request.email,
            avatarUrl = request.avatarUrl,
            phone = request.phone,
            createdAt = Instant.now(), // 这个值会被忽略
            updatedAt = Instant.now()
        )

        // 2. 业务规则验证和更新（通过领域服务）
        val updatedUser = userService.updateUser(userId, user)

        // 3. DTO转换（应用层职责）
        return UserBasicResponse.fromUser(updatedUser)
    }

    /**
     * 业务用例：获取用户统计信息
     * 按需查询和组装统计数据（单向关联的优势）
     */
    @Transactional(readOnly = true)
    fun getUserStats(userId: String): Map<String, Any> {
        // 1. 验证用户存在
        val user = userService.getUserById(userId)
            ?: throw BusinessException.notFound("用户不存在")

        // 2. 按需查询统计数据（通过领域服务，遵循分层原则）
        val routeCount = userService.countUserCreatedRoutes(userId)
        val tripCount = userService.countUserTripParticipations(userId)
        val favoriteCount = userService.countUserFavoriteRoutes(userId)
        val completedCount = userService.countUserCompletedRoutes(userId)

        // 3. 组装返回数据
        return mapOf(
            "userId" to user.id,
            "username" to user.username,
            "routeCount" to routeCount,
            "tripCount" to tripCount,
            "favoriteCount" to favoriteCount,
            "completedCount" to completedCount
        )
    }
}
