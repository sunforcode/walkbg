package org.example.user.dto

import com.fasterxml.jackson.annotation.JsonProperty
import org.example.user.model.User

/**
 * 用户基础响应DTO
 */
data class UserBasicResponse(
    val id: String,
    val username: String,
    val email: String,
    val nickname: String?,
    val phone: String?,
    @JsonProperty("avatar_url")
    val avatarUrl: String?,
    val bio: String?,
    val status: Int,
    @JsonProperty("last_login_at")
    val lastLoginAt: Long?,
    @JsonProperty("created_at")
    val createdAt: Long // 时间戳（秒）
) {
    companion object {
        /**
         * 从User实体创建DTO
         */
        fun fromUser(user: User): UserBasicResponse {
            return UserBasicResponse(
                id = user.id,
                username = user.username,
                email = user.email,
                nickname = user.nickname,
                phone = user.phone,
                avatarUrl = user.avatarUrl,
                bio = user.bio,
                status = user.status,
                lastLoginAt = user.lastLoginAt?.epochSecond,
                createdAt = user.createdAt.epochSecond
            )
        }
    }
}
