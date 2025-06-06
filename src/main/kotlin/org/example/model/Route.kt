package org.example.model

import com.fasterxml.jackson.annotation.JsonIgnore
import jakarta.persistence.*
import java.math.BigDecimal
import java.time.Instant
// 移除错误的Region导入
// import org.hibernate.cache.spi.Region

/**
 * 路线模型
 */
@Entity
@Table(
    name = "routes",
    indexes = [
        Index(name = "idx_routes_region_id", columnList = "region_id"),
        Index(name = "idx_routes_difficulty", columnList = "difficulty"),
        Index(name = "idx_routes_popularity", columnList = "popularity DESC"),
        Index(name = "idx_routes_created_by", columnList = "created_by"),
        Index(name = "idx_routes_status", columnList = "status")
    ]
)
data class Route(
    @Id
    @Column(length = 64)
    val id: String,

    @Column(nullable = false, length = 200)
    var name: String,
    
    @Column(columnDefinition = "TEXT")
    var description: String? = null,
    
    @Column(length = 100)
    var region: String? = null,

    @Column(name = "region_id", length = 64)
    var regionId: String? = null,
    
    @Column(precision = 8, scale = 2)
    var distance: BigDecimal? = null,
    
    @Column
    var duration: Int? = null, // 预计用时（小时）
    
    @Column(precision = 10, scale = 6)
    var latitude: BigDecimal? = null,

    @Column(precision = 10, scale = 6)
    var longitude: BigDecimal? = null,

    @Column(precision = 8, scale = 2)
    var altitude: BigDecimal? = null,

    @Column(name = "elevation_gain", precision = 8, scale = 2)
    var elevationGain: BigDecimal? = null,
    
    @Column(name = "elevation_loss", precision = 8, scale = 2)
    var elevationLoss: BigDecimal? = null,
    
    @Column
    var difficulty: Int? = null, // 0: 简单, 1: 中等, 2: 困难, 3: 极难
    
    @Column(name = "route_type")
    var routeType: Int? = null, // 0: 往返, 1: 环线, 2: 单程, 3: 多日
    
    @Column(name = "route_direction")
    var routeDirection: Int? = null, // 0: 顺时针, 1: 逆时针, 2: 双向
    
    @Column(nullable = false)
    var status: Int = 0, // 0: 规划中, 1: 已发布, 2: 已关闭
    
    @Column(name = "cover_url", length = 500)
    var coverUrl: String? = null,
    
    @Column(name = "map_data_id", length = 64)
    var mapDataId: String? = null,
    
    @Column(name = "default_map_id", length = 64)
    var defaultMapId: String = "",

    @Column(name = "created_by", length = 64)
    var createdBy: String? = null,
    
    @Column(nullable = false)
    var popularity: Int = 0,

    @Column(name = "created_at", nullable = false, updatable = false)
    val createdAt: Instant = Instant.now(),

    @Column(name = "updated_at", nullable = false)
    var updatedAt: Instant = Instant.now(),
    
    // 关联关系
    // TODO: 创建Region模型后取消注释
    // @ManyToOne(fetch = FetchType.LAZY)
    // @JoinColumn(name = "region_id", insertable = false, updatable = false)
    // var regionEntity: Region? = null,

    @JsonIgnore
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "created_by", insertable = false, updatable = false)
    var creator: User? = null,
    
    @JsonIgnore
    @OneToMany(mappedBy = "route", cascade = [CascadeType.ALL], orphanRemoval = true, fetch = FetchType.LAZY)
    val waypoints: MutableList<Waypoint> = mutableListOf(),
    
    @JsonIgnore
    @OneToMany(mappedBy = "route", cascade = [CascadeType.ALL], orphanRemoval = true, fetch = FetchType.LAZY)
    val segments: MutableList<Segment> = mutableListOf(),
    
    @JsonIgnore
    @OneToMany(mappedBy = "route", cascade = [CascadeType.ALL], orphanRemoval = true, fetch = FetchType.LAZY)
    val tags: MutableList<RouteTag> = mutableListOf(),
    
    @JsonIgnore
    @OneToMany(mappedBy = "route", cascade = [CascadeType.ALL], orphanRemoval = true, fetch = FetchType.LAZY)
    val seasons: MutableList<RouteSeason> = mutableListOf(),
    
    @JsonIgnore
    @OneToMany(mappedBy = "route", cascade = [CascadeType.ALL], orphanRemoval = true, fetch = FetchType.LAZY)
    val images: MutableList<RouteImage> = mutableListOf(),
    
    @JsonIgnore
    @OneToMany(mappedBy = "route", cascade = [CascadeType.ALL], fetch = FetchType.LAZY)
    val tripRouteAssociations: MutableList<TripRouteAssociation> = mutableListOf(),

    @JsonIgnore
    @OneToMany(mappedBy = "route", cascade = [CascadeType.ALL], fetch = FetchType.LAZY)
    val userFavoriteRoutes: MutableList<UserFavoriteRoute> = mutableListOf(),

    @JsonIgnore
    @OneToMany(mappedBy = "route", cascade = [CascadeType.ALL], fetch = FetchType.LAZY)
    val userCompletedRoutes: MutableList<UserCompletedRoute> = mutableListOf()
) {

    fun incrementPopularity() {
        popularity += 1
        updatedAt = Instant.now()
    }

    fun addWaypoint(waypoint: Waypoint) {
        waypoints.add(waypoint)
        waypoint.route = this
    }
    
    fun addSegment(segment: Segment) {
        segments.add(segment)
        segment.route = this
    }
    
    fun addTag(tag: String) {
        tags.add(RouteTag(route = this, tag = tag))
    }
    
    fun addSeason(season: String) {
        seasons.add(RouteSeason(route = this, season = season))
    }

    
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as Route

        return id == other.id
    }

    override fun hashCode(): Int {
        return id.hashCode()
    }

    override fun toString(): String {
        return "Route(id='$id', name='$name')"
    }
}