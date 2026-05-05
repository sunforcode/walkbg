package org.example.user.dto

import com.fasterxml.jackson.annotation.JsonProperty
import org.example.user.model.User
import java.time.Instant

/**
 * 用户登录响应DTO
 */
data class UserLoginResponse(
    val id: String,
    val username: String,
    val email: String,
    val nickname: String?,
    val phone: String?,
    @JsonProperty("avatar_url")
    val avatarUrl: String?,
    val token: String,
    @JsonProperty("refresh_token")
    val refreshToken: String,
    @JsonProperty("expires_at")
    val expiresAt: Instant
) {
    companion object {
        fun fromUser(user: User, token: String, refreshToken: String, expiresAt: Instant): UserLoginResponse {
            return UserLoginResponse(
                id = user.id,
                username = user.username,
                email = user.email,
                nickname = user.nickname,
                phone = user.phone,
                avatarUrl = user.avatarUrl,
                token = token,
                refreshToken = refreshToken,
                expiresAt = expiresAt
            )
        }
    }
}
