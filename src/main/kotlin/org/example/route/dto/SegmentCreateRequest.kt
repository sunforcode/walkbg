package org.example.route.dto

import com.fasterxml.jackson.annotation.JsonProperty
import jakarta.validation.constraints.*

/**
 * 路段创建请求DTO
 */
data class SegmentCreateRequest( 
    @field:Size(max = 64, message = "路段ID长度不能超过64个字符")
    val id: String? = null,

    @field:DecimalMin(value = "0.0", message = "距离不能为负数")
    @field:DecimalMax(value = "1000.0", message = "单段距离不能超过1000公里")
    val distance: Double?,

    @JsonProperty("elevation_gain")
    @field:DecimalMin(value = "0.0", message = "爬升不能为负数")
    @field:DecimalMax(value = "5000.0", message = "单段爬升不能超过5000米")
    val elevationGain: Double?,

    @JsonProperty("elevation_loss")
    @field:DecimalMin(value = "0.0", message = "下降不能为负数")
    @field:DecimalMax(value = "5000.0", message = "单段下降不能超过5000米")
    val elevationLoss: Double?,

    @JsonProperty("estimated_time")
    @field:DecimalMin(value = "0.0", message = "预计时间不能为负数")
    @field:DecimalMax(value = "720.0", message = "单段预计时间不能超过720分钟")
    val estimatedTime: Double?,

    @field:Min(value = 1, message = "难度等级范围应在1-5之间")
    @field:Max(value = 5, message = "难度等级范围应在1-5之间")
    val difficulty: Int?,

    @field:Size(max = 50, message = "地形类型长度不能超过50个字符")
    val terrain: String?,

    @JsonProperty("surface_type")
    @field:Size(max = 50, message = "路面类型长度不能超过50个字符")
    val surfaceType: String?,

    @JsonProperty("traffic_level")
    @field:Min(value = 0, message = "交通等级范围应在0-5之间")
    @field:Max(value = 5, message = "交通等级范围应在0-5之间")
    val trafficLevel: Int?
)
