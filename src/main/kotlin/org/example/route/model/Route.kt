package org.example.route.model

import com.fasterxml.jackson.annotation.JsonIgnore
import com.fasterxml.jackson.annotation.JsonProperty
import jakarta.persistence.*
import org.example.user.model.User
import org.example.user.model.UserFavoriteRoute
import org.example.user.model.UserCompletedRoute
import org.example.trip.model.TripRouteAssociation
import org.example.water.model.WaterSource
import java.math.BigDecimal
import java.time.Instant

/**
 * 路线实体
 */
@Entity
@Table(name = "routes")
data class Route(
    @Id
    @Column(length = 64)
    val id: String,

    @Column(nullable = false)
    var name: String,

    @Column(columnDefinition = "TEXT")
    var description: String? = null,

    var region: String? = null,

    @Column(name = "region_id")
    var regionId: String? = null,

    var distance: BigDecimal? = null,
    var duration: Int? = null,
    var elevationGain: BigDecimal? = null,
    var elevationLoss: BigDecimal? = null,
    var difficulty: Int? = null,

    @Column(name = "route_type")
    var routeType: Int? = null, // 0: 往返, 1: 环线, 2: 单程, 3: 多日

    @Column(name = "route_direction")
    var routeDirection: Int? = null, // 0: 顺时针, 1: 逆时针, 2: 双向

    @Column(nullable = false)
    var status: Int = 0, // 0: 规划中, 1: 已发布, 2: 已关闭

    @JsonProperty("cover_url")
    @Column(name = "cover_url", length = 500)
    var coverUrl: String? = null,

    @JsonProperty("default_map_id")
    @Column(name = "default_map_id", length = 64)
    var defaultMapId: String = "",

    @Column(nullable = false)
    var popularity: Int = 0,

    @Column(name = "usage_count", nullable = false)
    var usageCount: Int = 0,

    @Column(name = "is_loop", nullable = false)
    var isLoop: Boolean = false,

    @JsonProperty("created_at")
    @Column(name = "created_at", nullable = false, updatable = false)
    val createdAt: Instant = Instant.now(),

    @JsonProperty("updated_at")
    @Column(name = "updated_at", nullable = false)
    var updatedAt: Instant = Instant.now(),

    @Column(name = "created_by", length = 64)
    var createdBy: String? = null,

    @JsonIgnore
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "created_by", insertable = false, updatable = false)
    var creator: User? = null,

    @JsonIgnore
    @OneToMany(mappedBy = "route", cascade = [CascadeType.ALL], orphanRemoval = true, fetch = FetchType.LAZY)
    val segments: MutableList<Segment> = mutableListOf(),

    @JsonIgnore
    @OneToMany(mappedBy = "route", cascade = [CascadeType.ALL], orphanRemoval = true, fetch = FetchType.LAZY)
    val tags: MutableList<RouteTag> = mutableListOf(),

    @JsonIgnore
    @OneToMany(mappedBy = "route", cascade = [CascadeType.ALL], orphanRemoval = true, fetch = FetchType.LAZY)
    val images: MutableList<RouteImage> = mutableListOf(),

    @JsonIgnore
    @OneToMany(mappedBy = "route", cascade = [CascadeType.ALL], fetch = FetchType.LAZY)
    val waterSources: MutableList<WaterSource> = mutableListOf(),

    @JsonIgnore
    @OneToMany(mappedBy = "route", cascade = [CascadeType.ALL], fetch = FetchType.LAZY)
    val campsites: MutableList<Campsite> = mutableListOf(),

    @JsonIgnore
    @OneToMany(mappedBy = "route", cascade = [CascadeType.ALL], fetch = FetchType.LAZY)
    val supplies: MutableList<Supply> = mutableListOf(),

    @JsonIgnore
    @OneToMany(mappedBy = "route", cascade = [CascadeType.ALL], fetch = FetchType.LAZY)
    val hitchhikeContacts: MutableList<HitchhikeContact> = mutableListOf(),

    @JsonIgnore
    @OneToMany(mappedBy = "route", cascade = [CascadeType.ALL], fetch = FetchType.LAZY)
    val markerPoints: MutableList<MarkerPoint> = mutableListOf(),

    @JsonIgnore
    @OneToMany(mappedBy = "route", cascade = [CascadeType.ALL], fetch = FetchType.LAZY)
    val dailyPlans: MutableList<DailyPlan> = mutableListOf(),

    @JsonIgnore
    @OneToMany(mappedBy = "route", cascade = [CascadeType.ALL], fetch = FetchType.LAZY)
    val tripRouteAssociations: MutableList<TripRouteAssociation> = mutableListOf(),

    @JsonIgnore
    @OneToMany(mappedBy = "route", cascade = [CascadeType.ALL], fetch = FetchType.LAZY)
    val userFavoriteRoutes: MutableList<UserFavoriteRoute> = mutableListOf(),

    @JsonIgnore
    @OneToMany(mappedBy = "route", cascade = [CascadeType.ALL], fetch = FetchType.LAZY)
    val userCompletedRoutes: MutableList<UserCompletedRoute> = mutableListOf(),

    // 评分关联
    @JsonIgnore
    @OneToOne(mappedBy = "route", cascade = [CascadeType.ALL], fetch = FetchType.LAZY)
    var rating: RouteRating? = null,

    // 天气关联
    @JsonIgnore
    @OneToOne(mappedBy = "route", cascade = [CascadeType.ALL], fetch = FetchType.LAZY)
    var weatherInfo: RouteWeather? = null
) {

    /**
     * 计算属性：获取创建者ID
     * 用于向后兼容，从creator关联实体中获取ID
     */
    @get:JsonProperty("created_by")
    val createdById: String?
        get() = creator?.id

    /**
     * 计算属性：是否被当前用户收藏
     * 需要在查询时设置
     */
    @Transient
    var isFavorite: Boolean = false

    fun incrementPopularity() {
        popularity += 1
        updatedAt = Instant.now()
    }

    fun incrementUsageCount() {
        usageCount += 1
        updatedAt = Instant.now()
    }

    fun addSegment(segment: Segment) {
        segments.add(segment)
        segment.route = this
    }

    fun addTag(tag: String) {
        tags.add(RouteTag(
            id = java.util.UUID.randomUUID().toString(),
            tag = tag,
            route = this
        ))
    }

    fun addImage(imageUrl: String, isCover: Boolean = false, sequenceNumber: Int = images.size + 1) {
        images.add(RouteImage(
            id = java.util.UUID.randomUUID().toString(),
            imageUrl = imageUrl,
            isCover = isCover,
            sequenceNumber = sequenceNumber,
            route = this
        ))
    }

    fun addHitchhikeContact(contact: HitchhikeContact) {
        hitchhikeContacts.add(contact)
        contact.route = this
    }

    fun addMarkerPoint(markerPoint: MarkerPoint) {
        markerPoints.add(markerPoint)
        markerPoint.route = this
    }

    fun addDailyPlan(dailyPlan: DailyPlan) {
        dailyPlans.add(dailyPlan)
        dailyPlan.route = this
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
