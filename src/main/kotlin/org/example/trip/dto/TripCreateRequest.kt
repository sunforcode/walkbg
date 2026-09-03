package org.example.trip.dto

import com.fasterxml.jackson.annotation.JsonProperty
import jakarta.validation.constraints.NotBlank
import java.math.BigDecimal
import org.example.common.exception.BusinessException

/**
 * 行程创建请求DTO
 */
data class TripCreateRequest(
    @field:NotBlank(message = "行程名称不能为空")
    val name: String,

    val description: String? = null,

    @JsonProperty("start_date")
    val startDate: Long? = null, // 时间戳（秒）

    @JsonProperty("end_date")
    val endDate: Long? = null, // 时间戳（秒）

    @JsonProperty("organizer_id")
    val organizerId: String? = null,

    /**
     * 行程包含的路线集合，是行程所含路线的权威来源。
     *
     * 与 [primaryRouteId] 的关系见 [resolveRoutes]。
     */
    @JsonProperty("route_ids")
    val routeIds: List<String>? = null,

    /**
     * 主路线标识，仅用于标记 [routeIds] 中的哪一条为主路线。
     */
    @JsonProperty("primary_route_id")
    val primaryRouteId: String? = null,

    val budget: BigDecimal? = null,

    val notes: String? = null,

    @JsonProperty("privacy_setting")
    val privacySetting: Int = 0, // 0: 公开, 1: 仅好友, 2: 私有

    @JsonProperty("cover_url")
    val coverUrl: String? = null
) {
    /**
     * 归一化并校验路线集合与主路线的关系，返回可直接落库的结果。
     *
     * 规则：
     * 1. `route_ids` 缺省时，回退为 `primary_route_id` 的单元素集合（兼容仅发送主路线的旧调用方）。
     * 2. 归一化后集合为空则拒绝——系统当前不提供向已存在行程追加路线的能力，
     *    空行程将无法被补救。
     * 3. `primary_route_id` 非空时必须是集合成员，否则会产生「主路线不在路线列表中」的不可解释状态。
     * 4. 多路线且主路线缺省时自动取首个元素，保证关联记录的 `isPrimary` 标记总有确定归属。
     *
     * @throws BusinessException 校验失败时抛出，携带 `VALIDATION_ERROR` code 与出错字段
     */
    fun resolveRoutes(): ResolvedRoutes {
        // 去重但保持客户端给定顺序，首个元素在主路线缺省时将成为主路线
        val normalized = (routeIds ?: listOfNotNull(primaryRouteId))
            .filter { it.isNotBlank() }
            .distinct()

        if (normalized.isEmpty()) {
            throw BusinessException.validation("创建行程至少需要包含一条路线", "route_ids")
        }

        if (primaryRouteId != null && primaryRouteId !in normalized) {
            throw BusinessException.validation(
                "主路线必须是行程路线之一",
                "primary_route_id"
            )
        }

        return ResolvedRoutes(
            routeIds = normalized,
            primaryRouteId = primaryRouteId ?: normalized.first()
        )
    }
}

/**
 * 归一化后的行程路线信息。
 *
 * [routeIds] 非空，且 [primaryRouteId] 必为其成员。
 */
data class ResolvedRoutes(
    val routeIds: List<String>,
    val primaryRouteId: String
)

/**
 * 行程更新请求DTO
 */
data class TripUpdateRequest(
    val name: String? = null,

    val description: String? = null,

    @JsonProperty("start_date")
    val startDate: Long? = null,

    @JsonProperty("end_date")
    val endDate: Long? = null,

    @JsonProperty("primary_route_id")
    val primaryRouteId: String? = null,

    val budget: BigDecimal? = null,

    @JsonProperty("actual_cost")
    val actualCost: BigDecimal? = null,

    val notes: String? = null,

    @JsonProperty("privacy_setting")
    val privacySetting: Int? = null,

    @JsonProperty("cover_url")
    val coverUrl: String? = null
)
