package org.example.model

import jakarta.persistence.*
import java.time.Instant

@Entity
@Table(name = "trip_routes")
data class TripRoute(
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    val id: Long = 0,
    
    val isPrimary: Boolean = false,
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "trip_id")
    var trip: Trip? = null,
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "route_id")
    var route: Route? = null
)

@Entity
@Table(name = "participants")
data class Participant(
    @Id
    val id: String,
    
    val userId: String? = null,
    
    @Column(nullable = false)
    val name: String,
    
    val role: String? = null,
    
    val status: String? = null,
    
    val contact: String? = null,
    
    @Column(columnDefinition = "TEXT")
    val notes: String? = null,
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "trip_id")
    var trip: Trip? = null
)

@Entity
@Table(name = "equipment_lists")
data class EquipmentList(
    @Id
    val id: String,
    
    @Column(nullable = false)
    val name: String,
    
    @Column(columnDefinition = "TEXT")
    val description: String? = null,
    
    val routeId: String? = null,
    
    val routeName: String? = null,
    
    val tripDays: Int? = null,
    
    val totalWeight: Double? = null,
    
    val baseWeight: Double? = null,
    
    val consumableWeight: Double? = null,
    
    val wornWeight: Double? = null,
    
    val creatorId: String? = null,
    
    val creatorName: String? = null,
    
    val isOfficial: Boolean = false,
    
    @Column(nullable = false)
    val createdAt: Instant = Instant.now(),
    
    @Column(nullable = false)
    val updatedAt: Instant = Instant.now(),
    
    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "trip_id")
    var trip: Trip? = null,
    
    @OneToMany(mappedBy = "equipmentList", cascade = [CascadeType.ALL], orphanRemoval = true)
    val seasons: MutableList<EquipmentListSeason> = mutableListOf(),
    
    @OneToMany(mappedBy = "equipmentList", cascade = [CascadeType.ALL], orphanRemoval = true)
    val tags: MutableList<EquipmentListTag> = mutableListOf(),
    
    @OneToMany(mappedBy = "equipmentList", cascade = [CascadeType.ALL], orphanRemoval = true)
    val equipments: MutableList<EquipmentItem> = mutableListOf()
) {
    fun addSeason(season: Int) {
        seasons.add(EquipmentListSeason(equipmentList = this, season = season))
    }
    
    fun addTag(tag: String) {
        tags.add(EquipmentListTag(equipmentList = this, tag = tag))
    }
    
    fun addEquipment(equipment: EquipmentItem) {
        equipments.add(equipment)
        equipment.equipmentList = this
    }
}

@Entity
@Table(name = "equipment_list_seasons")
data class EquipmentListSeason(
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    val id: Long = 0,
    
    val season: Int,
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "equipment_list_id")
    var equipmentList: EquipmentList? = null
)

@Entity
@Table(name = "equipment_list_tags")
data class EquipmentListTag(
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    val id: Long = 0,
    
    @Column(nullable = false)
    val tag: String,
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "equipment_list_id")
    var equipmentList: EquipmentList? = null
)

@Entity
@Table(name = "equipment_items")
data class EquipmentItem(
    @Id
    val id: String,
    
    @Column(nullable = false)
    val name: String,
    
    @Column(columnDefinition = "TEXT")
    val description: String? = null,
    
    val weight: Double? = null,
    
    val quantity: Int = 1,
    
    val necessity: Int? = null,
    
    val brand: String? = null,
    
    val model: String? = null,
    
    val price: Double? = null,
    
    @Column(columnDefinition = "TEXT")
    val notes: String? = null,
    
    val category: String? = null,
    
    val prepared: Boolean = false,
    
    val isOwned: Boolean = false,
    
    @Column(nullable = false)
    val createdAt: Instant = Instant.now(),
    
    @Column(nullable = false)
    val updatedAt: Instant = Instant.now(),
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "equipment_list_id")
    var equipmentList: EquipmentList? = null
)

@Entity
@Table(name = "meal_plans")
data class MealPlan(
    @Id
    val id: String,
    
    @Column(nullable = false)
    val name: String,
    
    @Column(columnDefinition = "TEXT")
    val description: String? = null,
    
    val tripDays: Int? = null,
    
    val personCount: Int? = null,
    
    val creatorId: String? = null,
    
    val creatorName: String? = null,
    
    @Column(nullable = false)
    val createdAt: Instant = Instant.now(),
    
    @Column(nullable = false)
    val updatedAt: Instant = Instant.now(),
    
    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "trip_id")
    var trip: Trip? = null,
    
    @OneToMany(mappedBy = "mealPlan", cascade = [CascadeType.ALL], orphanRemoval = true)
    val tags: MutableList<MealPlanTag> = mutableListOf(),
    
    @OneToMany(mappedBy = "mealPlan", cascade = [CascadeType.ALL], orphanRemoval = true)
    val dayMealPlans: MutableList<DayMealPlan> = mutableListOf()
) {
    fun addTag(tag: String) {
        tags.add(MealPlanTag(mealPlan = this, tag = tag))
    }
    
    fun addDayMealPlan(dayMealPlan: DayMealPlan) {
        dayMealPlans.add(dayMealPlan)
        dayMealPlan.mealPlan = this
    }
}

@Entity
@Table(name = "meal_plan_tags")
data class MealPlanTag(
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    val id: Long = 0,
    
    @Column(nullable = false)
    val tag: String,
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "meal_plan_id")
    var mealPlan: MealPlan? = null
)

@Entity
@Table(name = "day_meal_plans")
data class DayMealPlan(
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    val id: Long = 0,
    
    val dayNumber: Int,
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "meal_plan_id")
    var mealPlan: MealPlan? = null,
    
    @OneToMany(mappedBy = "dayMealPlan", cascade = [CascadeType.ALL], orphanRemoval = true)
    val mealFoodItems: MutableList<MealFoodItem> = mutableListOf()
) {
    fun addFoodItem(foodItem: FoodItem, mealType: String) {
        mealFoodItems.add(MealFoodItem(dayMealPlan = this, foodItem = foodItem, mealType = mealType))
    }
}

@Entity
@Table(name = "food_items")
data class FoodItem(
    @Id
    val id: String,
    
    @Column(nullable = false)
    val name: String,
    
    @Column(columnDefinition = "TEXT")
    val description: String? = null,
    
    val weight: Double? = null,
    
    val quantity: Int = 1,
    
    val calories: Double? = null,
    
    val protein: Double? = null,
    
    val fat: Double? = null,
    
    val carbs: Double? = null,
    
    val price: Double? = null,
    
    val prepared: Boolean = false,
    
    val isOwned: Boolean = false,
    
    @Column(columnDefinition = "TEXT")
    val notes: String? = null
)

@Entity
@Table(name = "meal_food_items")
data class MealFoodItem(
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    val id: Long = 0,
    
    @Column(nullable = false)
    val mealType: String, // breakfast, lunch, dinner, snacks, drinks
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "day_meal_plan_id")
    var dayMealPlan: DayMealPlan? = null,
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "food_item_id")
    var foodItem: FoodItem? = null
)

@Entity
@Table(name = "water_plans")
data class WaterPlan(
    @Id
    val id: String,
    
    @Column(nullable = false)
    val name: String,
    
    @Column(columnDefinition = "TEXT")
    val description: String? = null,
    
    val tripDays: Int? = null,
    
    val personCount: Int? = null,
    
    val creatorId: String? = null,
    
    val creatorName: String? = null,
    
    @Column(nullable = false)
    val createdAt: Instant = Instant.now(),
    
    @Column(nullable = false)
    val updatedAt: Instant = Instant.now(),
    
    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "trip_id")
    var trip: Trip? = null,
    
    @OneToMany(mappedBy = "waterPlan", cascade = [CascadeType.ALL], orphanRemoval = true)
    val tags: MutableList<WaterPlanTag> = mutableListOf(),
    
    @OneToMany(mappedBy = "waterPlan", cascade = [CascadeType.ALL], orphanRemoval = true)
    val dayWaterPlans: MutableList<DayWaterPlan> = mutableListOf(),
    
    @OneToMany(mappedBy = "waterPlan", cascade = [CascadeType.ALL], orphanRemoval = true)
    val waterSources: MutableList<WaterSource> = mutableListOf()
) {
    fun addTag(tag: String) {
        tags.add(WaterPlanTag(waterPlan = this, tag = tag))
    }
    
    fun addDayWaterPlan(dayWaterPlan: DayWaterPlan) {
        dayWaterPlans.add(dayWaterPlan)
        dayWaterPlan.waterPlan = this
    }
    
    fun addWaterSource(waterSource: WaterSource) {
        waterSources.add(waterSource)
        waterSource.waterPlan = this
    }
}

@Entity
@Table(name = "water_plan_tags")
data class WaterPlanTag(
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    val id: Long = 0,
    
    @Column(nullable = false)
    val tag: String,
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "water_plan_id")
    var waterPlan: WaterPlan? = null
)

@Entity
@Table(name = "day_water_plans")
data class DayWaterPlan(
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    val id: Long = 0,
    
    val dayNumber: Int,
    
    val baseWaterIntake: Int? = null,
    
    val activityWaterIntake: Int? = null,
    
    val temperature: Double? = null,
    
    val activityIntensity: String? = null,
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "water_plan_id")
    var waterPlan: WaterPlan? = null,
    
    @OneToMany(mappedBy = "dayWaterPlan", cascade = [CascadeType.ALL], orphanRemoval = true)
    val availableSources: MutableList<DayWaterPlanSource> = mutableListOf()
) {
    fun addWaterSource(waterSource: WaterSource) {
        availableSources.add(DayWaterPlanSource(dayWaterPlan = this, waterSource = waterSource))
    }
}

@Entity
@Table(name = "water_sources")
data class WaterSource(
    @Id
    val id: String,
    
    @Column(nullable = false)
    val name: String,
    
    @Column(columnDefinition = "TEXT")
    val description: String? = null,
    
    val type: String? = null,
    
    val location: String? = null,
    
    val distanceFromTrail: Double? = null,
    
    val quality: String? = null,
    
    val reliability: Int? = null,
    
    val estimatedVolume: Int? = null,
    
    val needsTreatment: Boolean = true,
    
    @Column(nullable = false)
    val createdAt: Instant = Instant.now(),
    
    @Column(nullable = false)
    val updatedAt: Instant = Instant.now(),
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "water_plan_id")
    var waterPlan: WaterPlan? = null
)

@Entity
@Table(name = "day_water_plan_sources")
data class DayWaterPlanSource(
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    val id: Long = 0,
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "day_water_plan_id")
    var dayWaterPlan: DayWaterPlan? = null,
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "water_source_id")
    var waterSource: WaterSource? = null
)

@Entity
@Table(name = "trip_images")
data class TripImage(
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    val id: Long = 0,
    
    @Column(nullable = false)
    val imageUrl: String,
    
    val isCover: Boolean = false,
    
    val sequenceNumber: Int,
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "trip_id")
    var trip: Trip? = null
)

@Entity
@Table(name = "trip_itinerary")
data class TripItinerary(
    @Id
    val id: String,
    
    val dayNumber: Int,
    
    @Column(nullable = false)
    val title: String,
    
    @Column(columnDefinition = "TEXT")
    val description: String? = null,
    
    val distance: Double? = null,
    
    val duration: String? = null,
    
    val elevationGain: Double? = null,
    
    val elevationLoss: Double? = null,
    
    val accommodation: String? = null,
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "trip_id")
    var trip: Trip? = null,
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "start_waypoint_id")
    var startWaypoint: Waypoint? = null,
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "end_waypoint_id")
    var endWaypoint: Waypoint? = null
)