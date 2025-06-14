package org.example.route.dto

import com.fasterxml.jackson.annotation.JsonProperty
import jakarta.validation.constraints.*

/**
 * 标记点创建请求DTO
 */
data class MarkerPointCreateRequest(
    val id: String? = null,

    @field:NotBlank(message = "标记点名称不能为空")
    @field:Size(max = 200, message = "标记点名称长度不能超过200个字符")
    val name: String,

    @field:Size(max = 2000, message = "标记点描述长度不能超过2000个字符")
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

    @JsonProperty("marker_type")
    @field:Min(value = 0, message = "标记点类型值无效")
    @field:Max(value = 10, message = "标记点类型值无效")
    val markerType: Int = 0, // 0: 普通标记, 1: 危险点, 2: 景点, 3: 休息点, 4: 分岔路口, 5: 其他

    @JsonProperty("icon_url")
    @field:Size(max = 500, message = "图标URL长度不能超过500个字符")
    val iconUrl: String?,

    @JsonProperty("image_url")
    @field:Size(max = 500, message = "图片URL长度不能超过500个字符")
    val imageUrl: String?,

    @field:Min(value = 1, message = "序号必须大于0")
    @field:Max(value = 9999, message = "序号不能超过9999")
    val sequenceNumber: Int = 1
)
