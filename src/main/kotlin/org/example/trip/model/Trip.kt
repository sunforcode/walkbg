package org.example.trip.model


import com.fasterxml.jackson.annotation.JsonIgnore
import jakarta.persistence.*
import java.time.Instant
import org.example.user.model.User
import org.example.water.model.WaterPlan
import org.example.equipment.model.EquipmentList
import org.example.route.model.Route
import org.example.meal.model.MealPlan

/**
 * 行程模型
 */
@Entity
@Table(
    name = "trips",
    indexes = [
        Index(name = "idx_trips_organizer_id", columnList = "organizer_id"),
        Index(name = "idx_trips_status", columnList = "status"),
        Index(name = "idx_trips_start_date", columnList = "start_date"),
        Index(name = "idx_trips_end_date", columnList = "end_date"),
        Index(name = "idx_trips_date_range", columnList = "start_date, end_date"),
        Index(name = "idx_trips_privacy", columnList = "privacy_setting")
    ]
)
data class Trip(
    @Id
    @Column(length = 64)
    val id: String,

    @Column(nullable = false, length = 200)
    var name: String = "",
    
    @Column(columnDefinition = "TEXT")
    var description: String? = null,
    
    @Column(name = "start_date")
    var startDate: java.time.Instant? = null,
    
    @Column(name = "end_date")
    var endDate: java.time.Instant? = null,
    
    @Column(nullable = false)
    var status: Int = 0, // 0: 规划中, 1: 进行中, 2: 已完成, 3: 已取消
    @Column(name = "organizer_id", nullable = false, length = 64)
    var organizerId: String = "",
    
    @Column(name = "primary_route_id", length = 64)
    var primaryRouteId: String? = null,
    
    @Column(precision = 10, scale = 2)
    var budget: java.math.BigDecimal? = null,
    
    @Column(name = "actual_cost", precision = 10, scale = 2)
    var actualCost: java.math.BigDecimal? = null,

    @Column(columnDefinition = "TEXT")
    var notes: String? = null,
    
    @Column(name = "privacy_setting", nullable = false)
    var privacySetting: Int = 0, // 0: 公开, 1: 仅好友, 2: 私有
    @Column(name = "cover_url", length = 500)
    var coverUrl: String? = null,

    @Column(name = "created_at", nullable = false, updatable = false)
    val createdAt: Instant = Instant.now(),

    @Column(name = "updated_at", nullable = false)
    var updatedAt: Instant = Instant.now()
) {
    /**
     * 注意：不再持有以下关联关系的集合引用
     * - tripRoutes: 通过 TripRouteAssociationRepository.findByTripId(tripId) 查询
     * - participants: 通过 TripParticipantRepository.findByTripId(tripId) 查询
     * - equipmentLists: 通过 EquipmentListRepository.findByTripId(tripId) 查询
     * - mealPlans: 通过 MealPlanRepository.findByTripId(tripId) 查询
     * - waterPlans: 通过 WaterPlanRepository.findByTripId(tripId) 查询
     * - itinerary: 通过 TripItineraryRepository.findByTripId(tripId) 查询
     * - images: 通过 TripImageRepository.findByTripId(tripId) 查询
     * 
     * 优势：
     * 1. 避免 N+1 查询问题
     * 2. 减少内存占用
     * 3. 避免序列化死循环
     * 4. 提高查询灵活性（按需加载）
     */

    /**
     * 领域行为：更新行程状态
     */
    fun updateStatus(newStatus: Int) {
        require(newStatus in 0..3) { "状态值必须在 0-3 之间" }
        this.status = newStatus
        this.updatedAt = Instant.now()
    }

    /**
     * 领域行为：设置主路线
     */
    fun setPrimaryRoute(routeId: String) {
        this.primaryRouteId = routeId
        this.updatedAt = Instant.now()
    }

    /**
     * 领域行为：更新预算
     */
    fun updateBudget(newBudget: java.math.BigDecimal) {
        this.budget = newBudget
        this.updatedAt = Instant.now()
    }

    /**
     * 领域行为：记录实际花费
     */
    fun recordActualCost(cost: java.math.BigDecimal) {
        this.actualCost = cost
        this.updatedAt = Instant.now()
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as Trip

        return id == other.id
    }

    override fun hashCode(): Int {
        return id.hashCode()
    }

    override fun toString(): String {
        return "Trip(id='$id', name='$name')"
    }
}