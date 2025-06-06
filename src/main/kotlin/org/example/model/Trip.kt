package org.example.model

import com.fasterxml.jackson.annotation.JsonIgnore
import jakarta.persistence.*
import java.time.Instant

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
    
    // 关联关系
    @JsonIgnore
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "organizer_id", insertable = false, updatable = false)
    var organizer: User? = null
    
    @JsonIgnore
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "primary_route_id", insertable = false, updatable = false)
    var primaryRoute: Route? = null
    
    @JsonIgnore
    @OneToMany(mappedBy = "trip", cascade = [CascadeType.ALL], orphanRemoval = true, fetch = FetchType.LAZY)
    val tripRoutes: MutableList<TripRouteAssociation> = mutableListOf()
    
    @JsonIgnore
    @OneToMany(mappedBy = "trip", cascade = [CascadeType.ALL], orphanRemoval = true, fetch = FetchType.LAZY)
    val participants: MutableList<TripParticipant> = mutableListOf()
    
    @JsonIgnore
    @OneToMany(mappedBy = "trip", cascade = [CascadeType.ALL], orphanRemoval = true, fetch = FetchType.LAZY)
    val equipmentLists: MutableList<EquipmentList> = mutableListOf()
    
    @JsonIgnore
    @OneToMany(mappedBy = "trip", cascade = [CascadeType.ALL], orphanRemoval = true, fetch = FetchType.LAZY)
    val mealPlans: MutableList<MealPlan> = mutableListOf()
    
    @JsonIgnore
    @OneToMany(mappedBy = "trip", cascade = [CascadeType.ALL], orphanRemoval = true, fetch = FetchType.LAZY)
    val waterPlans: MutableList<WaterPlan> = mutableListOf()
    
    @JsonIgnore
    @OneToMany(mappedBy = "trip", cascade = [CascadeType.ALL], orphanRemoval = true, fetch = FetchType.LAZY)
    val itinerary: MutableList<TripItinerary> = mutableListOf()
    
    @JsonIgnore
    @OneToMany(mappedBy = "trip", cascade = [CascadeType.ALL], orphanRemoval = true, fetch = FetchType.LAZY)
    val images: MutableList<TripImage> = mutableListOf()

    // 业务方法
    fun addRoute(route: Route, isPrimary: Boolean = false) {
        val tripRoute = TripRouteAssociation(tripId = this.id, routeId = route.id, isPrimary = isPrimary)
        tripRoutes.add(tripRoute)
        tripRoute.trip = this
        tripRoute.route = route
        if (isPrimary) {
            primaryRoute = route
            primaryRouteId = route.id
        }
    }
    
    fun addParticipant(user: User, role: Int = 0, status: Int = 0) {
        val participant = TripParticipant(tripId = this.id, userId = user.id, role = role, status = status)
        participants.add(participant)
        participant.trip = this
        participant.user = user
    }
    
    fun addItineraryItem(itineraryItem: TripItinerary) {
        itinerary.add(itineraryItem)
        itineraryItem.trip = this
    }
    
    fun addImage(url: String, isCover: Boolean = false, sequenceNumber: Int = images.size + 1) {
        val image = TripImage(trip = this, imageUrl = url, isCover = isCover, sequenceNumber = sequenceNumber)
        images.add(image)
        if (isCover) {
            coverUrl = url
        }
    }
    
    // 计算属性
    val routeIds: List<String>
        get() = tripRoutes.map { it.routeId }
        
    val imageUrls: List<String>
        get() = images.map { it.imageUrl }
        
    fun hasEquipmentLists(): Boolean = equipmentLists.isNotEmpty()
    
    fun hasMealPlans(): Boolean = mealPlans.isNotEmpty()
    
    fun hasWaterPlans(): Boolean = waterPlans.isNotEmpty()

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