package org.example.route.dto

import com.fasterxml.jackson.annotation.JsonProperty
import java.math.BigDecimal
import java.time.Instant

/**
 * 路线基础信息响应DTO - 用于列表、搜索等轻量场景
 */
data class RouteBasicResponse(
    val id: String,
    val name: String,
    val description: String?,
    val region: String?,
    val distance: BigDecimal?,
    val duration: Int?,
    val difficulty: Int?,
    @JsonProperty("cover_url")
    val coverUrl: String?,
    val popularity: Int,
    @JsonProperty("created_at")
    val createdAt: Instant,
    @JsonProperty("created_by")
    val createdBy: String?
)