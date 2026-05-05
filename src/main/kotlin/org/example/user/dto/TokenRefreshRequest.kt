package org.example.user.dto

import com.fasterxml.jackson.annotation.JsonProperty

/**
 * Token刷新请求DTO
 */
data class TokenRefreshRequest(
    @JsonProperty("refresh_token")
    val refreshToken: String? = null
)
