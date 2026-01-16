package org.example.guide.dto

import com.fasterxml.jackson.annotation.JsonProperty
import org.example.guide.model.Guide

/**
 * 攻略基础信息响应DTO
 */
data class GuideBasicResponse(
    val id: String,
    val title: String,
    val content: String,
    val author: String, // 作者名称
    @JsonProperty("author_id")
    val authorId: String,
    @JsonProperty("author_avatar_url")
    val authorAvatarUrl: String?, // 作者头像URL
    @JsonProperty("cover_url")
    val coverUrl: String?,
    @JsonProperty("icon_code")
    val iconCode: String, // 封面图标代码
    val tags: List<String>, // 标签列表
    val likes: Int, // 点赞数
    val views: Int, // 浏览数
    @JsonProperty("is_liked")
    val isLiked: Boolean, // 是否已点赞
    @JsonProperty("is_bookmarked")
    val isBookmarked: Boolean, // 是否已收藏
    val difficulty: Int, // 难度等级 1-5
    @JsonProperty("reading_time")
    val readingTime: Int, // 阅读时长（分钟）
    @JsonProperty("comment_count")
    val commentCount: Int, // 评论数
    val location: String, // 地理位置
    @JsonProperty("best_time")
    val bestTime: String?, // 最佳时间建议
    @JsonProperty("actual_cost")
    val actualCost: Double?, // 实际花费
    @JsonProperty("actual_days")
    val actualDays: Int?, // 实际用时天数
    val highlights: List<String>, // 行程亮点
    @JsonProperty("personal_tips")
    val personalTips: List<String>, // 个人建议
    val status: Int,
    @JsonProperty("publish_date")
    val publishDate: Long, // 发布时间戳（秒）
    @JsonProperty("update_date")
    val updateDate: Long // 更新时间戳（秒）
) {
    companion object {
        /**
         * 从Guide实体创建基础响应DTO
         */
        fun fromGuide(guide: Guide, authorName: String = "未知作者"): GuideBasicResponse {
            val tagList = guide.tags?.split(",")?.map { it.trim() }?.filter { it.isNotEmpty() } ?: emptyList()
            return GuideBasicResponse(
                id = guide.id,
                title = guide.title,
                content = guide.content ?: "",
                author = authorName,
                authorId = guide.authorId,
                authorAvatarUrl = null,
                coverUrl = guide.coverUrl,
                iconCode = "hiking", // 默认图标
                tags = tagList,
                likes = guide.likeCount,
                views = guide.viewCount,
                isLiked = false,
                isBookmarked = false,
                difficulty = 3, // 默认中等难度
                readingTime = (guide.content?.length ?: 0) / 100, // 估算阅读时间
                commentCount = 0,
                location = tagList.firstOrNull() ?: "未知地点",
                bestTime = null,
                actualCost = null,
                actualDays = null,
                highlights = emptyList(),
                personalTips = emptyList(),
                status = guide.status,
                publishDate = guide.createdAt.epochSecond,
                updateDate = guide.updatedAt.epochSecond
            )
        }
    }
}
