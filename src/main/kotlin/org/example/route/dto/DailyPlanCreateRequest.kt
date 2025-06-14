package org.example.route.dto

import com.fasterxml.jackson.annotation.JsonProperty
import jakarta.validation.constraints.*
import java.math.BigDecimal

/**
 * 日程计划创建请求DTO
 */
data class DailyPlanCreateRequest(
    val id: String? = null,

    @field:NotBlank(message = "日程标题不能为空")
    @field:Size(max = 200, message = "日程标题长度不能超过200个字符")
    val title: String,

    @field:Size(max = 2000, message = "日程描述长度不能超过2000个字符")
    val description: String?,

    @field:Min(value = 1, message = "天数必须大于0")
    @field:Max(value = 365, message = "天数不能超过365天")
    val dayNumber: Int,

    @field:DecimalMin(value = "0.0", message = "距离不能为负数")
    @field:DecimalMax(value = "1000.0", message = "单日距离不能超过1000公里")
    val distance: BigDecimal?,

    @field:DecimalMin(value = "0.0", message = "爬升不能为负数")
    @field:DecimalMax(value = "5000.0", message = "单日爬升不能超过5000米")
    @JsonProperty("elevation_gain")
    val elevationGain: BigDecimal?,

    @field:DecimalMin(value = "0.0", message = "下降不能为负数")
    @field:DecimalMax(value = "5000.0", message = "单日下降不能超过5000米")
    @JsonProperty("elevation_loss")
    val elevationLoss: BigDecimal?,

    @field:DecimalMin(value = "0.0", message = "预计时长不能为负数")
    @field:DecimalMax(value = "24.0", message = "单日时长不能超过24小时")
    @JsonProperty("estimated_time")
    val estimatedTime: Double?,

    @field:Min(value = 1, message = "难度等级范围应在1-5之间")
    @field:Max(value = 5, message = "难度等级范围应在1-5之间")
    val difficulty: Int?,

    @field:Size(max = 1000, message = "备注长度不能超过1000个字符")
    val notes: String?,

    @JsonProperty("start_campsite_id")
    @field:Size(max = 64, message = "起始营地ID长度不能超过64个字符")
    val startCampsiteId: String?,

    @JsonProperty("end_campsite_id")
    @field:Size(max = 64, message = "结束营地ID长度不能超过64个字符")
    val endCampsiteId: String?
)
