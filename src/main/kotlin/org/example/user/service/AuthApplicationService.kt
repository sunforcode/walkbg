package org.example.user.service

import org.example.common.exception.BusinessException
import org.example.common.util.IdGenerator
import org.example.security.JwtTokenUtil
import org.example.security.PasswordEncoderService
import org.example.user.dto.*
import org.example.user.model.User
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.time.Instant

/**
 * 认证应用服务
 * 职责：业务用例编排、认证相关业务逻辑、DTO转换
 * 严格遵循分层架构，协调领域服务和安全组件
 */
@Service
class AuthApplicationService(
    private val userService: UserService,
    private val passwordEncoderService: PasswordEncoderService,
    private val jwtTokenUtil: JwtTokenUtil
) {

    /**
     * 业务用例：用户登录
     */
    @Transactional
    fun login(request: UserLoginRequest): UserLoginResponse {
        val user = userService.getUserByUsername(request.username)
            ?: throw BusinessException.unauthorized("用户名或密码错误")

        if (!passwordEncoderService.matches(request.password, user.password)) {
            throw BusinessException.unauthorized("用户名或密码错误")
        }

        val token = jwtTokenUtil.generateToken(user.id, user.username)
        val refreshToken = jwtTokenUtil.generateRefreshToken(user.id, user.username)
        val expiresAt = jwtTokenUtil.getExpirationDateFromToken(token)?.toInstant() ?: Instant.now()

        // 持久化 lastLoginAt（使用领域服务，同时触发缓存失效）
        userService.updateLastLoginAt(user.id)

        return UserLoginResponse.fromUser(
            user = user.copy(lastLoginAt = Instant.now()),
            token = token,
            refreshToken = refreshToken,
            expiresAt = expiresAt
        )
    }

    /**
     * 业务用例：用户注册
     * 注册成功后自动登录，返回包含Token的响应
     */
    @Transactional
    fun register(request: UserRegisterRequest): UserLoginResponse {
        if (!userService.isUsernameAvailable(request.username)) {
            throw BusinessException.conflict("用户名已存在")
        }

        if (!userService.isEmailAvailable(request.email)) {
            throw BusinessException.conflict("邮箱已被注册")
        }

        val user = User(
            id = IdGenerator.generateIdWithPrefix("user"),
            username = request.username,
            nickname = request.nickname ?: request.username,
            email = request.email,
            password = passwordEncoderService.encode(request.password),
            avatarUrl = request.avatarUrl,
            phone = request.phone,
            createdAt = Instant.now(),
            updatedAt = Instant.now()
        )

        val savedUser = userService.createUserWithValidation(user)

        // 注册成功后自动生成Token（自动登录）
        val token = jwtTokenUtil.generateToken(savedUser.id, savedUser.username)
        val refreshToken = jwtTokenUtil.generateRefreshToken(savedUser.id, savedUser.username)
        val expiresAt = jwtTokenUtil.getExpirationDateFromToken(token)?.toInstant() ?: Instant.now()

        return UserLoginResponse.fromUser(
            user = savedUser,
            token = token,
            refreshToken = refreshToken,
            expiresAt = expiresAt
        )
    }

    /**
     * 业务用例：刷新 Token
     */
    fun refreshToken(request: TokenRefreshRequest): TokenRefreshResponse {
        val refreshToken = request.refreshToken

        if (refreshToken.isNullOrBlank()) {
            throw BusinessException.badRequest("刷新Token不能为空")
        }
        if (jwtTokenUtil.getTokenTypeFromToken(refreshToken) != "refresh") {
            throw BusinessException.unauthorized("无效的刷新Token")
        }

        val username = jwtTokenUtil.getUsernameFromToken(refreshToken)
            ?: throw BusinessException.unauthorized("无效的刷新Token")

        val userId = jwtTokenUtil.getUserIdFromToken(refreshToken)
            ?: throw BusinessException.unauthorized("无法从Token中获取用户信息")

        if (!jwtTokenUtil.validateToken(refreshToken, username)) {
            throw BusinessException.unauthorized("刷新Token已过期，请重新登录")
        }

        val newToken = jwtTokenUtil.generateToken(userId, username)
        val newRefreshToken = jwtTokenUtil.generateRefreshToken(userId, username)
        val expiresAt = jwtTokenUtil.getExpirationDateFromToken(newToken)?.toInstant() ?: Instant.now()

        return TokenRefreshResponse(
            token = newToken,
            refreshToken = newRefreshToken,
            expiresAt = expiresAt
        )
    }

    /**
     * 业务用例：检查用户名是否可用
     */
    @Transactional(readOnly = true)
    fun checkUsernameAvailability(username: String): Boolean {
        return userService.isUsernameAvailable(username)
    }

    /**
     * 业务用例：检查邮箱是否可用
     */
    @Transactional(readOnly = true)
    fun checkEmailAvailability(email: String): Boolean {
        return userService.isEmailAvailable(email)
    }
}
