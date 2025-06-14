package org.example.route.dto

import com.fasterxml.jackson.annotation.JsonProperty
import jakarta.validation.constraints.*

/**
 * 营地创建请求DTO
 */
data class CampsiteCreateRequest(
    val id: String? = null,

    @field:NotBlank(message = "营地名称不能为空")
    @field:Size(max = 200, message = "营地名称长度不能超过200个字符")
    val name: String,

    @field:Size(max = 2000, message = "营地描述长度不能超过2000个字符")
    val description: String?,

    @field:DecimalMin(value = "-90.0", message = "纬度范围应在-90到90之间")
    @field:DecimalMax(value = "90.0", message = "纬度范围应在-90到90之间")
    val latitude: Double?,

    @field:DecimalMin(value = "-180.0", message = "经度范围应在-180到180之间")
    @field:DecimalMax(value = "180.0", message = "经度范围应在-180到180之间")
    val longitude: Double?,

    @field:DecimalMin(value = "-500.0", message = "海拔不能低于-500米")
    @field:DecimalMax(value = "10000.0", message = "海拔不能超过10000米")
    val elevation: Double?,

    @JsonProperty("campsite_type")
    @field:Min(value = 0, message = "营地类型值无效")
    @field:Max(value = 5, message = "营地类型值无效")
    val campsiteType: Int = 0, // 0: 普通营地, 1: 官方营地, 2: 野营地, 3: 避难所, 4: 小屋, 5: 其他

    @field:Size(max = 1000, message = "备注长度不能超过1000个字符")
    val notes: String?,

    @JsonProperty("verified_by")
    @field:Size(max = 64, message = "验证者ID长度不能超过64个字符")
    val verifiedBy: String?
)
