package org.example.user.dto
import com.fasterxml.jackson.annotation.JsonProperty

/**
 * 用户基础信息DTO
 * 用于在其他DTO中嵌入用户信息
 */
data class UserBasicDto(
    val id: String,
    val username: String,
    val nickname: String,
    val email: String,
    @JsonProperty("avatar_url")
    val avatarUrl: String?,
    @JsonProperty("created_at")
    val createdAt: Long // 时间戳（秒）
)
