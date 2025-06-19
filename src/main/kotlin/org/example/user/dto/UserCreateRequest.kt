package org.example.user.dto

import com.fasterxml.jackson.annotation.JsonProperty
import jakarta.validation.constraints.*

/**
 * 用户创建请求DTO
 */
data class UserCreateRequest(
    @field:NotBlank(message = "用户名不能为空")
    @field:Size(min = 3, max = 50, message = "用户名长度必须在3-50个字符之间")
    @field:Pattern(regexp = "^[a-zA-Z0-9_]+$", message = "用户名只能包含字母、数字和下划线")
    val username: String,

    @field:NotBlank(message = "邮箱不能为空")
    @field:Email(message = "邮箱格式不正确")
    @field:Size(max = 100, message = "邮箱长度不能超过100个字符")
    val email: String,

    @field:Size(max = 50, message = "昵称长度不能超过50个字符")
    val nickname: String? = null,

    @field:Pattern(regexp = "^[1-9]\\d{10}$", message = "手机号格式不正确")
    val phone: String? = null,

    @JsonProperty("avatar_url")
    @field:Size(max = 500, message = "头像URL长度不能超过500个字符")
    @field:Pattern(regexp = "^(https?://.*|)$", message = "头像URL格式不正确")
    val avatarUrl: String? = null
)
