package org.example.user.dto

import com.fasterxml.jackson.annotation.JsonProperty
import java.time.Instant

/**
 * Token刷新响应DTO
 */
data class TokenRefreshResponse(
    val token: String,
    @JsonProperty("refresh_token")
    val refreshToken: String,
    @JsonProperty("expires_at")
    val expiresAt: Instant
)
