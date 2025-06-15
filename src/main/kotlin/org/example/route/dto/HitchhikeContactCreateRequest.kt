package org.example.route.dto

import com.fasterxml.jackson.annotation.JsonProperty
import jakarta.validation.constraints.*

/**
 * 搭车联系人创建请求DTO
 */
data class HitchhikeContactCreateRequest(
    val id: String? = null,

    @field:NotBlank(message = "联系人姓名不能为空")
    @field:Size(max = 100, message = "联系人姓名长度不能超过100个字符")
    val name: String,

    @field:NotBlank(message = "联系人电话不能为空")
    @field:Pattern(regexp = "^[0-9+\\-\\s()]{7,20}$", message = "电话号码格式不正确")
    val phone: String,

    @field:Size(max = 500, message = "描述长度不能超过500个字符")
    val description: String?,

    @field:Size(max = 200, message = "位置信息长度不能超过200个字符")
    val location: String?,

    @field:DecimalMin(value = "0.0", message = "价格不能为负数")
    @field:DecimalMax(value = "10000.0", message = "价格不能超过10000元")
    val price: Double?,

    val verified: Boolean? = false
)
