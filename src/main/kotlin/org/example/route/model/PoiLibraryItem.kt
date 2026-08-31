package org.example.route.model

import jakarta.persistence.*
import java.time.Instant

/**
 * 全局 POI 库实体
 *
 * 由 AI 筛选 + 人工确认后沉淀的跨路线共享 POI 资产。
 * 分析新路线时，会用路线 POI 与库内条目做"名称 + 距离"匹配，命中直接复用（自动置为 confirmed）。
 *
 * status 取值：
 * - active:  库内有效条目
 */
@Entity
@Table(
    name = "poi_library",
    indexes = [
        Index(name = "idx_poi_library_name", columnList = "name"),
        Index(name = "idx_poi_library_status", columnList = "status")
    ]
)
data class PoiLibraryItem(
    @Id
    @Column(length = 64)
    val id: String,

    @Column(nullable = false, length = 200)
    var name: String,

    @Column(nullable = false)
    var latitude: Double,

    @Column(nullable = false)
    var longitude: Double,

    var elevation: Double? = null,

    /**
     * POI 类型：water | camp | supply | photo | pass | valley | weather | danger | start | end
     */
    @Column(nullable = false, length = 32)
    var category: String,

    /** 地区 ID（来自来源路线，匹配时限定同地区） */
    @Column(name = "region_id", length = 64)
    var regionId: String? = null,

    /** 地区名称（来自来源路线） */
    @Column(name = "region_name", length = 100)
    var regionName: String? = null,

    @Column(name = "sub_category", length = 64)
    var subCategory: String? = null,

    @Column(columnDefinition = "TEXT")
    var description: String? = null,

    /**
     * AI 筛选给出的判断理由（人工确认入库时随条目保存）
     */
    @Column(name = "ai_reason", columnDefinition = "TEXT")
    var aiReason: String? = null,

    /**
     * 来源路线 ID（首次入库时的路线）
     */
    @Column(name = "source_route_id", length = 64)
    var sourceRouteId: String? = null,

    @Column(nullable = false, length = 20)
    var status: String = "active",

    @Column(name = "created_at", nullable = false, updatable = false)
    val createdAt: Instant = Instant.now(),

    @Column(name = "updated_at", nullable = false)
    var updatedAt: Instant = Instant.now()
) {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false
        other as PoiLibraryItem
        return id == other.id
    }

    override fun hashCode(): Int = id.hashCode()

    override fun toString(): String =
        "PoiLibraryItem(id='$id', name='$name', category='$category')"
}
