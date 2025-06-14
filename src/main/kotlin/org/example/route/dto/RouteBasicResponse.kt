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
) {
    companion object {
        /**
         * 从Route实体创建基础响应DTO
         */
        fun fromRoute(route: org.example.route.model.Route): RouteBasicResponse {
            return RouteBasicResponse(
                id = route.id,
                name = route.name,
                description = route.description,
                region = route.region,
                distance = route.mapData?.distance,
                duration = route.mapData?.duration,
                difficulty = route.difficulty,
                coverUrl = route.coverUrl,
                popularity = route.popularity,
                createdAt = route.createdAt,
                createdBy = route.createdBy
            )

        }
    }
}
