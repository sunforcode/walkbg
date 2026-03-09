package org.example.route.model

import jakarta.persistence.*
import java.math.BigDecimal
import java.time.Instant

/**
 * 路线地图数据实体 - 存储路线的地理信息和统计数据
 */
@Entity
@Table(name = "route_map_data")
data class RouteMapData(
    @Id
    @Column(length = 64)
    val id: String,

    // 地理信息
    val distance: BigDecimal? = null,
    val duration: Int? = null,
    val latitude: BigDecimal? = null,
    val longitude: BigDecimal? = null,
    val altitude: BigDecimal? = null,

    @Column(name = "elevation_gain")
    val elevationGain: BigDecimal? = null,

    @Column(name = "elevation_loss")
    val elevationLoss: BigDecimal? = null,

    // 文件数据
    @Column(name = "kml_url", length = 500)
    val kmlUrl: String? = null,

    @Column(name = "gpx_url", length = 500)
    val gpxUrl: String? = null,

    // 统计信息
    @Column(name = "favorite_count", nullable = false)
    var favoriteCount: Long = 0,

    @Column(name = "completion_count", nullable = false)
    var completionCount: Long = 0,

    @Column(name = "trip_count", nullable = false)
    var tripCount: Long = 0,

    @Column(name = "created_at", nullable = false, updatable = false)
    val createdAt: Instant = Instant.now(),

    @Column(name = "updated_at", nullable = false)
    var updatedAt: Instant = Instant.now()
) {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as RouteMapData

        return id == other.id
    }

    override fun hashCode(): Int {
        return id.hashCode()
    }

    override fun toString(): String {
        return "RouteMapData(id='$id', distance=$distance, duration=$duration, kmlUrl=$kmlUrl, gpxUrl=$gpxUrl)"
    }
}
