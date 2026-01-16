package org.example.route.model

import com.fasterxml.jackson.annotation.JsonProperty
import jakarta.persistence.*
import java.time.Instant

/**
 * 标记点类型枚举
 */
enum class MarkerPointType(val value: Int) {
    SCENIC(0),      // 景点
    VIEWPOINT(1),   // 观景点
    DANGER(2),      // 危险区域
    REST(3),        // 休息区
    WATER(4),       // 水源
    FOOD(5),        // 食物
    SHELTER(6),     // 避难所
    OTHER(7);       // 其他

    companion object {
        fun fromValue(value: Int): MarkerPointType {
            return values().find { it.value == value } ?: OTHER
        }
    }
}

/**
 * 标记点实体（单向关联）
 */
@Entity
@Table(
    name = "marker_points",
    indexes = [
        Index(name = "idx_marker_points_route_id", columnList = "route_id"),
        Index(name = "idx_marker_points_type", columnList = "marker_type")
    ]
)
data class MarkerPoint(
    @Id
    @Column(length = 64)
    val id: String,

    @Column(name = "route_id", length = 64, nullable = false)
    val routeId: String,

    @Column(length = 200)
    val name: String? = null,

    @Column(columnDefinition = "TEXT")
    val description: String? = null,

    @Column(name = "marker_type", nullable = false)
    val markerType: Int = 0,

    @Column(name = "icon_url", length = 500)
    val iconUrl: String? = null,

    val latitude: Double? = null,

    val longitude: Double? = null,

    val elevation: Double? = null,

    @Column(length = 7)
    val color: String? = null, // 颜色值，如 #228B22

    @Column(name = "created_at", nullable = false, updatable = false)
    val createdAt: Instant = Instant.now(),

    @Column(name = "updated_at", nullable = false)
    var updatedAt: Instant = Instant.now()
) {
    /**
     * 获取标记点类型枚举
     */
    @Transient
    fun getMarkerTypeEnum(): MarkerPointType {
        return MarkerPointType.fromValue(markerType)
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as MarkerPoint

        return id == other.id
    }

    override fun hashCode(): Int {
        return id.hashCode()
    }

    override fun toString(): String {
        return "MarkerPoint(id='$id', name='$name', markerType=${getMarkerTypeEnum()})"
    }
}
