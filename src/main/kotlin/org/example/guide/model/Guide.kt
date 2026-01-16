package org.example.guide.model

import com.fasterxml.jackson.annotation.JsonProperty
import jakarta.persistence.*
import java.time.Instant

/**
 * 攻略领域模型
 * 
 * 设计原则：
 * 1. 单向关联：不持有其他实体的集合引用，避免循环依赖
 * 2. 按需查询：需要关联数据时通过 Repository 查询
 * 3. 富领域模型：包含业务行为，而不仅仅是数据容器
 */
@Entity
@Table(
    name = "guides",
    indexes = [
        Index(name = "idx_guides_author_id", columnList = "author_id"),
        Index(name = "idx_guides_status", columnList = "status"),
        Index(name = "idx_guides_created_at", columnList = "created_at"),
        Index(name = "idx_guides_view_count", columnList = "view_count"),
        Index(name = "idx_guides_like_count", columnList = "like_count")
    ]
)
data class Guide(
    @Id
    @Column(length = 64)
    val id: String,

    @Column(nullable = false, length = 200)
    var title: String,

    @Column(columnDefinition = "TEXT")
    var content: String? = null,

    @JsonProperty("author_id")
    @Column(name = "author_id", length = 64, nullable = false)
    var authorId: String,

    @JsonProperty("cover_url")
    @Column(name = "cover_url", length = 500)
    var coverUrl: String? = null,

    @Column(length = 500)
    var tags: String? = null, // 逗号分隔的标签字符串

    @JsonProperty("view_count")
    @Column(name = "view_count", nullable = false)
    var viewCount: Int = 0,

    @JsonProperty("like_count")
    @Column(name = "like_count", nullable = false)
    var likeCount: Int = 0,

    @Column(nullable = false)
    var status: Int = 0, // 0: 草稿, 1: 已发布, 2: 已下线

    @JsonProperty("created_at")
    @Column(name = "created_at", nullable = false, updatable = false)
    val createdAt: Instant = Instant.now(),

    @JsonProperty("updated_at")
    @Column(name = "updated_at", nullable = false)
    var updatedAt: Instant = Instant.now()
) {
    /**
     * 领域行为：增加浏览次数
     */
    fun incrementViewCount() {
        viewCount += 1
        updatedAt = Instant.now()
    }

    /**
     * 领域行为：增加点赞数
     */
    fun incrementLikeCount() {
        likeCount += 1
        updatedAt = Instant.now()
    }

    /**
     * 领域行为：减少点赞数
     */
    fun decrementLikeCount() {
        if (likeCount > 0) {
            likeCount -= 1
            updatedAt = Instant.now()
        }
    }

    /**
     * 领域行为：发布攻略
     */
    fun publish() {
        require(status == 0) { "只有草稿状态的攻略才能发布" }
        status = 1
        updatedAt = Instant.now()
    }

    /**
     * 领域行为：下线攻略
     */
    fun takeOffline() {
        require(status == 1) { "只有已发布的攻略才能下线" }
        status = 2
        updatedAt = Instant.now()
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as Guide

        return id == other.id
    }

    override fun hashCode(): Int {
        return id.hashCode()
    }

    override fun toString(): String {
        return "Guide(id='$id', title='$title', authorId='$authorId')"
    }
}
