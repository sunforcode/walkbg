package org.example.trip.dto

import com.fasterxml.jackson.annotation.JsonProperty
import jakarta.validation.constraints.NotBlank
import java.math.BigDecimal

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

    @JsonProperty("primary_route_id")
    val primaryRouteId: String? = null,

    val budget: BigDecimal? = null,

    val notes: String? = null,

    @JsonProperty("privacy_setting")
    val privacySetting: Int = 0, // 0: 公开, 1: 仅好友, 2: 私有

    @JsonProperty("cover_url")
    val coverUrl: String? = null
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
