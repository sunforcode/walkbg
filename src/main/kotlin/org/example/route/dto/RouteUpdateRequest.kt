package org.example.route.dto

import com.fasterxml.jackson.annotation.JsonProperty
import jakarta.validation.constraints.DecimalMax
import jakarta.validation.constraints.DecimalMin
import jakarta.validation.constraints.Max
import jakarta.validation.constraints.Min
import jakarta.validation.constraints.Pattern
import jakarta.validation.constraints.Size
import java.math.BigDecimal

/**
 * 路线基本信息更新请求DTO（管理端）
 *
 * 全部字段可选：仅更新请求中出现的字段（null 表示不修改）。
 * 注意：不含 status 字段，路线状态只能通过 POST /{id}/status 流转，
 * 防止"编辑"与"状态流转"两个入口竞争。
 */
data class RouteUpdateRequest(
    @field:Size(max = 200, message = "路线名称长度不能超过200个字符")
    val name: String? = null,

    @field:Size(max = 2000, message = "路线描述长度不能超过2000个字符")
    val description: String? = null,

    @field:Size(max = 100, message = "区域名称长度不能超过100个字符")
    val region: String? = null,

    @JsonProperty("region_id")
    @field:Size(max = 64, message = "区域ID长度不能超过64个字符")
    val regionId: String? = null,

    @field:Min(value = 1, message = "难度等级范围应在1-5之间")
    @field:Max(value = 5, message = "难度等级范围应在1-5之间")
    val difficulty: Int? = null,

    @JsonProperty("route_type")
    @field:Min(value = 0, message = "路线类型范围应在0-3之间")
    @field:Max(value = 3, message = "路线类型范围应在0-3之间")
    val routeType: Int? = null,

    @JsonProperty("is_loop")
    val isLoop: Boolean? = null,

    @JsonProperty("cover_url")
    @field:Size(max = 500, message = "封面图片URL长度不能超过500个字符")
    @field:Pattern(regexp = "^(https?://.*|)$", message = "封面图片URL格式不正确")
    val coverUrl: String? = null,

    val distance: BigDecimal? = null,

    @JsonProperty("elevation_gain")
    @field:DecimalMin(value = "0.0", message = "爬升不能为负数")
    @field:DecimalMax(value = "10000.0", message = "爬升不能超过10000米")
    val elevationGain: BigDecimal? = null,

    @JsonProperty("elevation_loss")
    @field:DecimalMin(value = "0.0", message = "下降不能为负数")
    @field:DecimalMax(value = "10000.0", message = "下降不能超过10000米")
    val elevationLoss: BigDecimal? = null
)

/**
 * 路线状态流转请求DTO（管理端）
 *
 * 合法迁移：
 * - 0 规划中 → 1 已发布（publish，含发布前检查）
 * - 1 已发布 → 0 规划中（unpublish）
 * - 1 已发布 → 2 已关闭（close）
 * - 2 已关闭 → 1 已发布（reopen + publish）
 * - 2 已关闭 → 0 规划中（reopen）
 * - 3 分析中 → 不可手动变更，由分析回调自动流转
 */
data class RouteStatusUpdateRequest(
    @JsonProperty("target_status")
    @field:Min(value = 0, message = "目标状态值范围应在0-2之间")
    @field:Max(value = 2, message = "目标状态值范围应在0-2之间")
    val targetStatus: Int,

    /** 流转原因，仅作运营记录 */
    val reason: String? = null
)
