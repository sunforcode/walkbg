package org.example.route.dto

import com.fasterxml.jackson.annotation.JsonProperty
import jakarta.validation.constraints.*

/**
 * 路径点创建请求DTO
 */
data class WaypointCreateRequest(
    @field:Size(max = 64, message = "路点ID长度不能超过64个字符")
    val id: String? = null,

    @field:NotBlank(message = "路点名称不能为空")
    @field:Size(max = 200, message = "路点名称长度不能超过200个字符")
    val name: String,

    @field:Size(max = 1000, message = "路点描述长度不能超过1000个字符")
    val description: String?,

    @field:DecimalMin(value = "-90.0", message = "纬度范围应在-90到90之间")
    @field:DecimalMax(value = "90.0", message = "纬度范围应在-90到90之间")
    val latitude: Double,

    @field:DecimalMin(value = "-180.0", message = "经度范围应在-180到180之间")
    @field:DecimalMax(value = "180.0", message = "经度范围应在-180到180之间")
    val longitude: Double,

    @field:DecimalMin(value = "-500.0", message = "海拔不能低于-500米")
    @field:DecimalMax(value = "10000.0", message = "海拔不能超过10000米")
    val elevation: Double?,

    @field:Size(max = 50, message = "路点类型长度不能超过50个字符")
    val type: String?,

    @JsonProperty("icon_url")
    @field:Size(max = 500, message = "图标URL长度不能超过500个字符")
    @field:Pattern(regexp = "^(https?://.*|)$", message = "图标URL格式不正确")
    val iconUrl: String?,

    @JsonProperty("image_url")
    @field:Size(max = 500, message = "图片URL长度不能超过500个字符")
    @field:Pattern(regexp = "^(https?://.*|)$", message = "图片URL格式不正确")
    val imageUrl: String?,

    @JsonProperty("sequence_number")
    @field:Min(value = 1, message = "序号必须大于0")
    @field:Max(value = 1000, message = "序号不能超过1000")
    val sequenceNumber: Int
)
