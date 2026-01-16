package org.example.guide.dto

import com.fasterxml.jackson.annotation.JsonProperty
import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.Size

/**
 * 创建攻略请求DTO
 */
data class GuideCreateRequest(
    @field:NotBlank(message = "标题不能为空")
    @field:Size(max = 200, message = "标题长度不能超过200")
    val title: String,

    val content: String?,

    @JsonProperty("author_id")
    @field:NotBlank(message = "作者ID不能为空")
    val authorId: String,

    @JsonProperty("cover_url")
    val coverUrl: String?,

    val tags: List<String>?, // 标签列表

    val status: Int = 0 // 默认为草稿状态
)
