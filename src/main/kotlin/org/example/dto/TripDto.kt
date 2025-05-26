package org.example.dto

import org.example.model.*
import java.time.Instant

data class TripDto(
    val id: String,
    val name: String,
    val description: String?,
    val startDate: Instant?,
    val endDate: Instant?,
    val status: Int,
    val participantCount: Int,
    val organizerId: String?,
    val budget: Double?,
    val actualCost: Double?,
    val notes: String?,
    val privacySetting: String?,
    val coverUrl: String?,
    val createdAt: Instant,
    val updatedAt: Instant,
    val primaryRouteId: String?,
    val routes: List<TripRouteDto> = emptyList(),
    val participants: List<ParticipantDto> = emptyList(),
    val equipmentList: EquipmentListDto? = null,
    val mealPlan: MealPlanDto? = null,
    val waterPlan: WaterPlanDto? = null,
    val itinerary: List<TripItineraryDto> = emptyList(),
    val images: List<TripImageDto> = emptyList()
)

data class TripRouteDto(
    val routeId: String,
    val routeName: String?,
    val isPrimary: Boolean
)

data class ParticipantDto(
    val id: String,
    val userId: String?,
    val name: String,
    val role: String?,
    val status: String?,
    val contact: String?,
    val notes: String?
)

data class EquipmentListDto(
    val id: String,
    val name: String,
    val description: String?,
    val routeId: String?,
    val routeName: String?,
    val tripDays: Int?,
    val totalWeight: Double?,
    val baseWeight: Double?,
    val consumableWeight: Double?,
    val wornWeight: Double?,
    val creatorId: String?,
    val creatorName: String?,
    val isOfficial: Boolean,
    val createdAt: Instant,
    val updatedAt: Instant,
    val seasons: List<Int> = emptyList(),
    val tags: List<String> = emptyList(),
    val equipments: List<EquipmentItemDto> = emptyList()
)

data class EquipmentItemDto(
    val id: String,
    val name: String,
    val description: String?,
    val weight: Double?,
    val quantity: Int,
    val necessity: Int?,
    val brand: String?,
    val model: String?,
    val price: Double?,
    val notes: String?,
    val category: String?,
    val prepared: Boolean,
    val isOwned: Boolean,
    val createdAt: Instant,
    val updatedAt: Instant
)

data class MealPlanDto(
    val id: String,
    val name: String,
    val description: String?,
    val tripDays: Int?,
    val personCount: Int?,
    val creatorId: String?,
    val creatorName: String?,
    val createdAt: Instant,
    val updatedAt: Instant,
    val tags: List<String> = emptyList(),
    val dayMealPlans: List<DayMealPlanDto> = emptyList()
)

data class DayMealPlanDto(
    val dayNumber: Int,
    val breakfast: List<FoodItemDto> = emptyList(),
    val lunch: List<FoodItemDto> = emptyList(),
    val dinner: List<FoodItemDto> = emptyList(),
    val snacks: List<FoodItemDto> = emptyList(),
    val drinks: List<FoodItemDto> = emptyList()
)

data class FoodItemDto(
    val id: String,
    val name: String,
    val description: String?,
    val weight: Double?,
    val quantity: Int,
    val calories: Double?,
    val protein: Double?,
    val fat: Double?,
    val carbs: Double?,
    val price: Double?,
    val prepared: Boolean,
    val isOwned: Boolean,
    val notes: String?
)

data class WaterPlanDto(
    val id: String,
    val name: String,
    val description: String?,
    val tripDays: Int?,
    val personCount: Int?,
    val creatorId: String?,
    val creatorName: String?,
    val createdAt: Instant,
    val updatedAt: Instant,
    val tags: List<String> = emptyList(),
    val dayWaterPlans: List<DayWaterPlanDto> = emptyList(),
    val waterSources: List<WaterSourceDto> = emptyList()
)

data class DayWaterPlanDto(
    val dayNumber: Int,
    val baseWaterIntake: Int?,
    val activityWaterIntake: Int?,
    val temperature: Double?,
    val activityIntensity: String?,
    val availableSources: List<WaterSourceDto> = emptyList()
)

data class WaterSourceDto(
    val id: String,
    val name: String,
    val description: String?,
    val type: String?,
    val location: String?,
    val distanceFromTrail: Double?,
    val quality: String?,
    val reliability: Int?,
    val estimatedVolume: Int?,
    val needsTreatment: Boolean,
    val createdAt: Instant,
    val updatedAt: Instant
)

data class TripItineraryDto(
    val id: String,
    val dayNumber: Int,
    val title: String,
    val description: String?,
    val distance: Double?,
    val duration: String?,
    val elevationGain: Double?,
    val elevationLoss: Double?,
    val accommodation: String?,
    val startWaypointId: String?,
    val endWaypointId: String?
)

data class TripImageDto(
    val id: Long,
    val imageUrl: String,
    val isCover: Boolean,
    val sequenceNumber: Int
)

// 转换函数
fun Trip.toDto(): TripDto {
    return TripDto(
        id = id,
        name = name,
        description = description,
        startDate = startDate,
        endDate = endDate,
        status = status,
        participantCount = participantCount,
        organizerId = organizerId,
        budget = budget,
        actualCost = actualCost,
        notes = notes,
        privacySetting = privacySetting,
        coverUrl = coverUrl,
        createdAt = createdAt,
        updatedAt = updatedAt,
        primaryRouteId = primaryRoute?.id,
        routes = tripRoutes.map { it.toDto() },
        participants = participants.map { it.toDto() },
        equipmentList = equipmentList?.toDto(),
        mealPlan = mealPlan?.toDto(),
        waterPlan = waterPlan?.toDto(),
        itinerary = itinerary.map { it.toDto() },
        images = images.map { it.toDto() }
    )
}

fun TripRoute.toDto(): TripRouteDto {
    return TripRouteDto(
        routeId = route?.id ?: "",
        routeName = route?.name,
        isPrimary = isPrimary
    )
}

fun Participant.toDto(): ParticipantDto {
    return ParticipantDto(
        id = id,
        userId = userId,
        name = name,
        role = role,
        status = status,
        contact = contact,
        notes = notes
    )
}

fun EquipmentList.toDto(): EquipmentListDto {
    return EquipmentListDto(
        id = id,
        name = name,
        description = description,
        routeId = routeId,
        routeName = routeName,
        tripDays = tripDays,
        totalWeight = totalWeight,
        baseWeight = baseWeight,
        consumableWeight = consumableWeight,
        wornWeight = wornWeight,
        creatorId = creatorId,
        creatorName = creatorName,
        isOfficial = isOfficial,
        createdAt = createdAt,
        updatedAt = updatedAt,
        seasons = seasons.map { it.season },
        tags = tags.map { it.tag },
        equipments = equipments.map { it.toDto() }
    )
}

fun EquipmentItem.toDto(): EquipmentItemDto {
    return EquipmentItemDto(
        id = id,
        name = name,
        description = description,
        weight = weight,
        quantity = quantity,
        necessity = necessity,
        brand = brand,
        model = model,
        price = price,
        notes = notes,
        category = category,
        prepared = prepared,
        isOwned = isOwned,
        createdAt = createdAt,
        updatedAt = updatedAt
    )
}

fun MealPlan.toDto(): MealPlanDto {
    return MealPlanDto(
        id = id,
        name = name,
        description = description,
        tripDays = tripDays,
        personCount = personCount,
        creatorId = creatorId,
        creatorName = creatorName,
        createdAt = createdAt,
        updatedAt = updatedAt,
        tags = tags.map { it.tag },
        dayMealPlans = dayMealPlans.map { it.toDto() }
    )
}

fun DayMealPlan.toDto(): DayMealPlanDto {
    val mealsByType = mealFoodItems.groupBy { it.mealType }
    
    return DayMealPlanDto(
        dayNumber = dayNumber,
        breakfast = mealsByType["breakfast"]?.map { it.foodItem?.toDto() }?.filterNotNull() ?: emptyList(),
        lunch = mealsByType["lunch"]?.map { it.foodItem?.toDto() }?.filterNotNull() ?: emptyList(),
        dinner = mealsByType["dinner"]?.map { it.foodItem?.toDto() }?.filterNotNull() ?: emptyList(),
        snacks = mealsByType["snacks"]?.map { it.foodItem?.toDto() }?.filterNotNull() ?: emptyList(),
        drinks = mealsByType["drinks"]?.map { it.foodItem?.toDto() }?.filterNotNull() ?: emptyList()
    )
}

fun FoodItem.toDto(): FoodItemDto {
    return FoodItemDto(
        id = id,
        name = name,
        description = description,
        weight = weight,
        quantity = quantity,
        calories = calories,
        protein = protein,
        fat = fat,
        carbs = carbs,
        price = price,
        prepared = prepared,
        isOwned = isOwned,
        notes = notes
    )
}

fun WaterPlan.toDto(): WaterPlanDto {
    return WaterPlanDto(
        id = id,
        name = name,
        description = description,
        tripDays = tripDays,
        personCount = personCount,
        creatorId = creatorId,
        creatorName = creatorName,
        createdAt = createdAt,
        updatedAt = updatedAt,
        tags = tags.map { it.tag },
        dayWaterPlans = dayWaterPlans.map { it.toDto() },
        waterSources = waterSources.map { it.toDto() }
    )
}

fun DayWaterPlan.toDto(): DayWaterPlanDto {
    return DayWaterPlanDto(
        dayNumber = dayNumber,
        baseWaterIntake = baseWaterIntake,
        activityWaterIntake = activityWaterIntake,
        temperature = temperature,
        activityIntensity = activityIntensity,
        availableSources = availableSources.mapNotNull { it.waterSource?.toDto() }
    )
}

fun WaterSource.toDto(): WaterSourceDto {
    return WaterSourceDto(
        id = id,
        name = name,
        description = description,
        type = type,
        location = location,
        distanceFromTrail = distanceFromTrail,
        quality = quality,
        reliability = reliability,
        estimatedVolume = estimatedVolume,
        needsTreatment = needsTreatment,
        createdAt = createdAt,
        updatedAt = updatedAt
    )
}

fun TripItinerary.toDto(): TripItineraryDto {
    return TripItineraryDto(
        id = id,
        dayNumber = dayNumber,
        title = title,
        description = description,
        distance = distance,
        duration = duration,
        elevationGain = elevationGain,
        elevationLoss = elevationLoss,
        accommodation = accommodation,
        startWaypointId = startWaypoint?.id,
        endWaypointId = endWaypoint?.id
    )
}

fun TripImage.toDto(): TripImageDto {
    return TripImageDto(
        id = id,
        imageUrl = imageUrl,
        isCover = isCover,
        sequenceNumber = sequenceNumber
    )
}