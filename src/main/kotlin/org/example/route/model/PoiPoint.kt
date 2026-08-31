package org.example.route.model

import jakarta.persistence.*
import java.time.Instant

/**
 * 统一附属信息点实体
 *
 * 将路线上所有类型的附属位置信息（营地、水源、补给、拍照点、垭口、气象点等）
 * 统一存储，通过 category 字段区分类型，通过 card_data JSON 存储各类型的扩展属性。
 *
 * category 枚举值：
 * - water:    水源（泉水/河流/自来水等）
 * - camp:     营地
 * - supply:   补给点（村庄/商店/餐饮）
 * - photo:    拍照/打卡点
 * - pass:     垭口（高程局部最高点）
 * - valley:   河谷（高程局部最低点）
 * - weather:  气象信息点
 * - danger:   危险点
 * - start:    起点
 * - end:      终点
 *
 * source 枚举值：
 * - kml_marker:  KML 文件内置标记点（Phase 1，已实现）
 * - algorithm:   算法检测（垭口/河谷）（Phase 2，TODO）
 * - osm:         OSM Overpass API 查询（Phase 3，TODO）
 * - weather_api: Open-Meteo 气象 API（Phase 4，TODO）
 * - experience:  经验帖子数据提取（Phase 5，TODO）
 *
 * card_data 为 JSON 字符串，walkbg 侧不反序列化，直接透传给 App。
 * 各 category 的 card_data 结构由 Agent 侧 spec 定义。
 */
@Entity
@Table(
    name = "poi_points",
    indexes = [
        Index(name = "idx_poi_points_route_id", columnList = "route_id"),
        Index(name = "idx_poi_points_category", columnList = "category"),
        Index(name = "idx_poi_points_route_category", columnList = "route_id, category")
    ]
)
data class PoiPoint(
    @Id
    @Column(length = 64)
    val id: String,

    @Column(name = "route_id", length = 64, nullable = false)
    val routeId: String,

    @Column(nullable = false, length = 200)
    var name: String,

    @Column(nullable = false)
    val latitude: Double,

    @Column(nullable = false)
    val longitude: Double,

    val elevation: Double? = null,

    /**
     * POI 类型：water | camp | supply | photo | pass | valley | weather | danger | start | end
     */
    @Column(nullable = false, length = 32)
    val category: String,

    /**
     * 细分类型，如 water 的 spring/river/tap，supply 的 shop/restaurant
     */
    @Column(name = "sub_category", length = 64)
    val subCategory: String? = null,

    /**
     * 数据来源：kml_marker | algorithm | osm | weather_api | experience
     */
    @Column(nullable = false, length = 32)
    val source: String,

    @Column(columnDefinition = "TEXT")
    val description: String? = null,

    /**
     * 数据置信度 0.0-1.0，kml_marker 来源默认 1.0
     */
    val confidence: Double? = null,

    /**
     * 各 category 的扩展属性，JSON 字符串格式
     * walkbg 不解析，直接透传给 App
     */
    @Column(name = "card_data", columnDefinition = "TEXT")
    val cardData: String? = null,

    /**
     * 数据状态: draft(分析建议草稿) | confirmed(人工确认/采纳)
     * 分析回调写入的数据为 draft，人工采纳后变为 confirmed
     */
    @Column(length = 20)
    var status: String = "confirmed",

    @Column(name = "created_at", nullable = false, updatable = false)
    val createdAt: Instant = Instant.now()
) {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false
        other as PoiPoint
        return id == other.id
    }

    override fun hashCode(): Int = id.hashCode()

    override fun toString(): String =
        "PoiPoint(id='$id', routeId='$routeId', name='$name', category='$category', source='$source')"
}
