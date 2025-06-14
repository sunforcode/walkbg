package org.example.route.dto

import jakarta.validation.constraints.*

/**
 * 路线图片创建请求DTO
 */
data class RouteImageCreateRequest(
    @field:NotBlank(message = "图片URL不能为空")
    @field:Size(max = 500, message = "图片URL长度不能超过500个字符")
    @field:Pattern(regexp = "^https?://.*", message = "图片URL格式不正确")
    val imageUrl: String,

    val isCover: Boolean = false,

    @field:Min(value = 1, message = "序号必须大于0")
    @field:Max(value = 100, message = "序号不能超过100")
    val sequenceNumber: Int
)
