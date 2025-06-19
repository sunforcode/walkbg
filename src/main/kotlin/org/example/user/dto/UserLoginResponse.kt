package org.example.user.dto

import org.example.user.model.User
import java.time.Instant

/**
 * 用户登录响应DTO
 */
data class UserLoginResponse(
    val id: String,
    val username: String,
    val email: String,
    val token: String,
    val expiresAt: Instant
) {
    companion object {
        fun fromUser(user: User, token: String, expiresAt: Instant): UserLoginResponse {
            return UserLoginResponse(
                id = user.id,
                username = user.username,
                email = user.email,
                token = token,
                expiresAt = expiresAt
            )
        }
    }
}
