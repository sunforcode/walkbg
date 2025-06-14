package org.example.user.dto

import jakarta.validation.constraints.Email
import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.Pattern
import jakarta.validation.constraints.Size
import java.time.Instant

/**
 * 用户响应DTO
 */
data class UserResponseDTO(
    val id: String,
    val username: String,
    val nickname: String,
    val email: String,
    val avatarUrl: String? = null,
    val phone: String? = null,
    val bio: String? = null,
    val createdAt: Instant,
    val lastLoginAt: Instant? = null
)

/**
 * 创建用户请求DTO
 */
data class CreateUserRequestDTO(
    @field:NotBlank(message = "用户名不能为空")
    @field:Size(min = 3, max = 50, message = "用户名长度必须在3-50个字符之间")
    @field:Pattern(regexp = "^[a-zA-Z0-9_]+$", message = "用户名只能包含字母、数字和下划线")
    val username: String,

    @field:NotBlank(message = "昵称不能为空")
    @field:Size(min = 1, max = 50, message = "昵称长度必须在1-50个字符之间")
    val nickname: String,

    @field:NotBlank(message = "邮箱不能为空")
    @field:Email(message = "邮箱格式不正确")
    @field:Size(max = 100, message = "邮箱长度不能超过100个字符")
    val email: String,

    @field:Size(max = 500, message = "头像URL长度不能超过500个字符")
    val avatarUrl: String? = null,

    @field:Pattern(regexp = "^[+]?[\\d\\s-()]+$|^$", message = "手机号格式不正确")
    @field:Size(max = 20, message = "手机号长度不能超过20个字符")
    val phone: String? = null,

    @field:Size(max = 1000, message = "个人简介长度不能超过1000个字符")
    val bio: String? = null
)

/**
 * 更新用户请求DTO
 */
data class UpdateUserRequestDTO(
    @field:Size(min = 1, max = 50, message = "昵称长度必须在1-50个字符之间")
    val nickname: String? = null,

    @field:Size(max = 500, message = "头像URL长度不能超过500个字符")
    val avatarUrl: String? = null,

    @field:Pattern(regexp = "^[+]?[\\d\\s-()]+$|^$", message = "手机号格式不正确")
    @field:Size(max = 20, message = "手机号长度不能超过20个字符")
    val phone: String? = null,

    @field:Size(max = 1000, message = "个人简介长度不能超过1000个字符")
    val bio: String? = null,
)

/**
 * 用户统计信息DTO
 */
data class UserStatsDTO(
    val completedRoutes: Long,
    val favoriteRoutes: Long,
    val equipmentLists: Long,
    val tripParticipations: Long
)

/**
 * 用户收藏路线DTO
 */
data class UserFavoriteRouteDTO(
    val routeId: String,
    val routeName: String,
    val routeDescription: String? = null,
    val favoritedAt: Instant
)

/**
 * 用户完成路线DTO
 */
data class UserCompletedRouteDTO(
    val routeId: String,
    val routeName: String,
    val routeDescription: String? = null,
    val completedAt: Instant
)
