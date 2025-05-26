package org.example.dto

import org.example.model.*
import java.time.Instant

data class RouteDto(
    val id: String,
    val name: String,
    val description: String?,
    val regionId: String?,
    val region: String?,
    val distance: Double?,
    val duration: String?,
    val elevationGain: Double?,
    val elevationLoss: Double?,
    val difficulty: Int?,
    val routeType: Int?,
    val routeDirection: Int?,
    val coverUrl: String?,
    val mapDataId: String?,
    val createdBy: String?,
    val popularity: Int,
    val status: String?,
    val createdAt: Instant,
    val updatedAt: Instant,
    val waypoints: List<WaypointDto> = emptyList(),
    val segments: List<SegmentDto> = emptyList(),
    val dailyPlans: List<DailyPlanDto> = emptyList(),
    val seasons: List<String> = emptyList(),
    val tags: List<String> = emptyList(),
    val rating: RouteRatingDto? = null,
    val images: List<RouteImageDto> = emptyList(),
    val facilities: RouteFacilitiesDto? = null,
    val weatherInfo: RouteWeatherDto? = null,
    val safetyInfo: SafetyInfoDto? = null
)

data class WaypointDto(
    val id: String,
    val name: String,
    val description: String?,
    val latitude: Double,
    val longitude: Double,
    val elevation: Double?,
    val type: String?,
    val iconUrl: String?,
    val imageUrl: String?,
    val sequenceNumber: Int
)

data class SegmentDto(
    val id: String,
    val distance: Double?,
    val elevationGain: Double?,
    val elevationLoss: Double?,
    val estimatedTime: Double?,
    val difficulty: Int?,
    val terrain: String?,
    val surfaceType: String?,
    val trafficLevel: Int?,
    val startPointId: String?,
    val endPointId: String?,
    val hazards: List<String> = emptyList()
)

data class DailyPlanDto(
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
    val endWaypointId: String?,
    val segmentIds: List<String> = emptyList()
)

data class RouteRatingDto(
    val overall: Double?,
    val scenery: Double?,
    val difficulty: Double?,
    val experience: Double?,
    val facilities: Double?,
    val ratingCount: Int
)

data class RouteImageDto(
    val id: Long,
    val imageUrl: String,
    val isCover: Boolean,
    val sequenceNumber: Int
)

data class RouteFacilitiesDto(
    val water: String?,
    val food: String?,
    val accommodation: String?,
    val toilets: String?,
    val signalCoverage: String?
)

data class RouteWeatherDto(
    val description: String?,
    val precautions: String?,
    val seasonalWeather: Map<String, String> = emptyMap()
)

data class SafetyInfoDto(
    val emergencyContacts: List<EmergencyContactDto> = emptyList(),
    val riskAreas: List<RiskAreaDto> = emptyList()
)

data class EmergencyContactDto(
    val name: String,
    val phone: String,
    val description: String?
)

data class RiskAreaDto(
    val id: String,
    val name: String,
    val description: String?,
    val level: Int?,
    val boundaries: List<BoundaryPointDto> = emptyList()
)

data class BoundaryPointDto(
    val latitude: Double,
    val longitude: Double,
    val altitude: Double?,
    val sequenceNumber: Int
)

// 转换函数
fun Route.toDto(): RouteDto {
    return RouteDto(
        id = id,
        name = name,
        description = description,
        regionId = regionId,
        region = region,
        distance = distance,
        duration = duration,
        elevationGain = elevationGain,
        elevationLoss = elevationLoss,
        difficulty = difficulty,
        routeType = routeType,
        routeDirection = routeDirection,
        coverUrl = coverUrl,
        mapDataId = mapDataId,
        createdBy = createdBy,
        popularity = popularity,
        status = status,
        createdAt = createdAt,
        updatedAt = updatedAt,
        waypoints = waypoints.map { it.toDto() },
        segments = segments.map { it.toDto() },
        dailyPlans = dailyPlans.map { it.toDto() },
        seasons = seasons.map { it.season },
        tags = tags.map { it.tag },
        rating = rating?.toDto(),
        images = images.map { it.toDto() },
        facilities = facilities?.toDto(),
        weatherInfo = weatherInfo?.toDto(),
        safetyInfo = safetyInfo?.toDto()
    )
}

fun Waypoint.toDto(): WaypointDto {
    return WaypointDto(
        id = id,
        name = name,
        description = description,
        latitude = latitude,
        longitude = longitude,
        elevation = elevation,
        type = type,
        iconUrl = iconUrl,
        imageUrl = imageUrl,
        sequenceNumber = sequenceNumber
    )
}

fun Segment.toDto(): SegmentDto {
    return SegmentDto(
        id = id,
        distance = distance,
        elevationGain = elevationGain,
        elevationLoss = elevationLoss,
        estimatedTime = estimatedTime,
        difficulty = difficulty,
        terrain = terrain,
        surfaceType = surfaceType,
        trafficLevel = trafficLevel,
        startPointId = startPoint?.id,
        endPointId = endPoint?.id,
        hazards = hazards.map { it.hazard }
    )
}

fun DailyPlan.toDto(): DailyPlanDto {
    return DailyPlanDto(
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
        endWaypointId = endWaypoint?.id,
        segmentIds = segments.map { it.segment?.id ?: "" }.filter { it.isNotEmpty() }
    )
}

fun RouteRating.toDto(): RouteRatingDto {
    return RouteRatingDto(
        overall = overall,
        scenery = scenery,
        difficulty = difficulty,
        experience = experience,
        facilities = facilities,
        ratingCount = ratingCount
    )
}

fun RouteImage.toDto(): RouteImageDto {
    return RouteImageDto(
        id = id,
        imageUrl = imageUrl,
        isCover = isCover,
        sequenceNumber = sequenceNumber
    )
}

fun RouteFacilities.toDto(): RouteFacilitiesDto {
    return RouteFacilitiesDto(
        water = water,
        food = food,
        accommodation = accommodation,
        toilets = toilets,
        signalCoverage = signalCoverage
    )
}

fun RouteWeather.toDto(): RouteWeatherDto {
    return RouteWeatherDto(
        description = description,
        precautions = precautions,
        seasonalWeather = seasonalWeather.associate { it.season to (it.description ?: "") }
    )
}

fun SafetyInfo.toDto(): SafetyInfoDto {
    return SafetyInfoDto(
        emergencyContacts = emergencyContacts.map { it.toDto() },
        riskAreas = riskAreas.map { it.toDto() }
    )
}

fun EmergencyContact.toDto(): EmergencyContactDto {
    return EmergencyContactDto(
        name = name,
        phone = phone,
        description = description
    )
}

fun RiskArea.toDto(): RiskAreaDto {
    return RiskAreaDto(
        id = id,
        name = name,
        description = description,
        level = level,
        boundaries = boundaries.map { it.toDto() }
    )
}

fun RiskAreaBoundary.toDto(): BoundaryPointDto {
    return BoundaryPointDto(
        latitude = latitude,
        longitude = longitude,
        altitude = altitude,
        sequenceNumber = sequenceNumber
    )
}