package org.example.route.dto

import com.fasterxml.jackson.annotation.JsonProperty
import java.math.BigDecimal

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
    @JsonProperty("route_type")
    val routeType: Int?,
    @JsonProperty("is_loop")
    val isLoop: Boolean,
    @JsonProperty("cover_url")
    val coverUrl: String?,
    val popularity: Int,
    @JsonProperty("usage_count")
    val usageCount: Int,
    @JsonProperty("elevation_gain")
    val elevationGain: BigDecimal?,
    @JsonProperty("elevation_loss")
    val elevationLoss: BigDecimal?,
    val tags: List<String> = emptyList(),
    val ratings: RatingDto? = null,
    @JsonProperty("created_at")
    val createdAt: Long,
    @JsonProperty("created_by")
    val createdBy: String?
) {
    companion object {
        /**
         * 从Route实体创建基础响应DTO（不含 MapData/tags/ratings，仅用于已废弃路径）
         *
         * @deprecated 使用 RouteApplicationService.enrichRouteBasic() 替代，它会从数据库补全关联数据。
         */
        @Deprecated(
            message = "使用 RouteApplicationService.enrichRouteBasic() 替代",
            level = DeprecationLevel.WARNING
        )
        fun fromRoute(route: org.example.route.model.Route): RouteBasicResponse {
            return RouteBasicResponse(
                id = route.id,
                name = route.name,
                description = route.description,
                region = route.region,
                distance = null,
                duration = null,
                difficulty = route.difficulty,
                routeType = route.routeType,
                isLoop = route.isLoop,
                coverUrl = route.coverUrl,
                popularity = route.popularity,
                usageCount = route.usageCount,
                elevationGain = null,
                elevationLoss = null,
                tags = emptyList(),
                ratings = null,
                createdAt = route.createdAt.epochSecond,
                createdBy = route.createdBy
            )
        }
    }
}
