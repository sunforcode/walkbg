package org.example.route.model

import com.fasterxml.jackson.annotation.JsonProperty
import jakarta.persistence.*
import java.time.Instant

/**
 * 路线领域模型
 * 
 * 设计原则：
 * 1. 单向关联：不持有其他实体的集合引用，避免循环依赖和 N+1 查询
 * 2. 按需查询：需要关联数据时通过 Repository 查询
 * 3. 富领域模型：包含业务行为，而不仅仅是数据容器
 */
@Entity
@Table(
    name = "routes",
    indexes = [
        Index(name = "idx_routes_created_by", columnList = "created_by"),
        Index(name = "idx_routes_status", columnList = "status"),
        Index(name = "idx_routes_region", columnList = "region"),
        Index(name = "idx_routes_difficulty", columnList = "difficulty"),
        Index(name = "idx_routes_created_at", columnList = "created_at")
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

    @Column(nullable = true)
    var difficulty: Int? = null, // 1-5 难度等级

    @Column(name = "route_type", nullable = true)
    var routeType: Int? = null, // 0: 往返, 1: 环线, 2: 单程, 3: 多日

    @Column(nullable = false)
    var status: Int = 0, // 0: 规划中, 1: 已发布, 2: 已关闭

    @JsonProperty("cover_url")
    @Column(name = "cover_url", length = 500)
    var coverUrl: String? = null,

    @JsonProperty("default_map_id")
    @Column(name = "default_map_id", length = 64)
    var defaultMapId: String? = null,

    @Column(nullable = false)
    var popularity: Int = 0,

    @Column(name = "usage_count", nullable = false)
    var usageCount: Int = 0,

    @Column(name = "is_loop", nullable = false)
    var isLoop: Boolean = false,

    @Column(name = "image_urls", columnDefinition = "TEXT")
    var imageUrls: String? = null, // JSON 字符串存储图片URL数组

    @JsonProperty("created_at")
    @Column(name = "created_at", nullable = false, updatable = false)
    val createdAt: Instant = Instant.now(),

    @JsonProperty("updated_at")
    @Column(name = "updated_at", nullable = false)
    var updatedAt: Instant = Instant.now(),

    @Column(name = "created_by", length = 64, nullable = false)
    var createdBy: String
) {
    /**
     * 注意：不再持有以下关联关系的集合引用
     * - segments: 通过 SegmentRepository.findByRouteId(routeId) 查询
     * - waypoints: 通过 WaypointRepository.findByRouteId(routeId) 查询
     * - tags: 通过 RouteTagRepository.findByRouteId(routeId) 查询
     * - images: 通过 RouteImageRepository.findByRouteId(routeId) 查询
     * - waterSources: 通过 WaterSourceRepository.findByRouteId(routeId) 查询
     * - campsites: 通过 CampsiteRepository.findByRouteId(routeId) 查询
     * - supplies: 通过 SupplyRepository.findByRouteId(routeId) 查询
     * - contacts: 通过 ContactRepository.findByRouteId(routeId) 查询
     * - hitchhikeContacts: 通过 HitchhikeContactRepository.findByRouteId(routeId) 查询
     * - markerPoints: 通过 MarkerPointRepository.findByRouteId(routeId) 查询
     * - dailyPlans: 通过 DailyPlanRepository.findByRouteId(routeId) 查询
     * - tripRouteAssociations: 通过 TripRouteAssociationRepository.findByRouteId(routeId) 查询
     * - userFavoriteRoutes: 通过 UserFavoriteRouteRepository.findByRouteId(routeId) 查询
     * - userCompletedRoutes: 通过 UserCompletedRouteRepository.findByRouteId(routeId) 查询
     * - routeContacts: 通过 RouteContactRepository.findByRouteId(routeId) 查询
     * - rating: 通过 RouteRatingRepository.findByRouteId(routeId) 查询
     * - weatherInfo: 通过 RouteWeatherRepository.findByRouteId(routeId) 查询
     * 
     * 优势：
     * 1. 避免 N+1 查询问题
     * 2. 减少内存占用
     * 3. 避免序列化死循环
     * 4. 提高查询灵活性（按需加载）
     */

    /**
     * 领域行为：增加人气值
     */
    fun incrementPopularity() {
        popularity += 1
        updatedAt = Instant.now()
    }

    /**
     * 领域行为：增加使用次数
     */
    fun incrementUsageCount() {
        usageCount += 1
        updatedAt = Instant.now()
    }

    /**
     * 领域行为：发布路线
     */
    fun publish() {
        require(status == 0) { "只有规划中的路线才能发布" }
        status = 1
        updatedAt = Instant.now()
    }

    /**
     * 领域行为：关闭路线
     */
    fun close() {
        require(status == 1) { "只有已发布的路线才能关闭" }
        status = 2
        updatedAt = Instant.now()
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
        return "Route(id='$id', name='$name', createdBy='$createdBy')"
    }
}
