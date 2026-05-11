package org.example.route.model

import jakarta.persistence.*
import java.time.Instant

/**
 * 分段方案实体
 *
 * 每条路线可以有多套分段方案，每套方案代表一种维度的轨迹划分方式。
 * App 端通过切换方案类型来改变轨迹染色模式。
 *
 * scheme_type 枚举值：
 * - slope:     按坡度（爬升/下降/平路），默认方案，真实算法实现
 * - day:       按天（时间间隔 >6h 为新天），有时间戳时真实实现，无则 segments 为空
 * - terrain:   按地形（垭口/山脊/河谷/平台），TODO: 待 find_peaks 算法实现
 * - road_type: 按路况（小径/机耕路/公路），TODO: 待 OSM highway 数据接入
 */
@Entity
@Table(
    name = "segment_schemes",
    indexes = [
        Index(name = "idx_segment_schemes_route_id", columnList = "route_id"),
        Index(name = "idx_segment_schemes_type", columnList = "scheme_type")
    ]
)
data class SegmentScheme(
    @Id
    @Column(length = 64)
    val id: String,

    @Column(name = "route_id", length = 64, nullable = false)
    val routeId: String,

    /**
     * 方案类型：slope | day | terrain | road_type
     */
    @Column(name = "scheme_type", length = 32, nullable = false)
    val schemeType: String,

    /**
     * 展示用标签（中文），如"按坡度"、"按天"
     */
    @Column(length = 64, nullable = false)
    val label: String,

    /**
     * 是否为默认方案（App 未指定时展示此方案）
     * 每条路线有且仅有一个 is_default=true 的方案，通常为 slope 方案
     */
    @Column(name = "is_default", nullable = false)
    val isDefault: Boolean = false,

    @Column(name = "created_at", nullable = false, updatable = false)
    val createdAt: Instant = Instant.now()
) {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false
        other as SegmentScheme
        return id == other.id
    }

    override fun hashCode(): Int = id.hashCode()

    override fun toString(): String =
        "SegmentScheme(id='$id', routeId='$routeId', schemeType='$schemeType', isDefault=$isDefault)"
}
