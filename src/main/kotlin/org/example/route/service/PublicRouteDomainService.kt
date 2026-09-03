package org.example.route.service

import com.fasterxml.jackson.core.type.TypeReference
import com.fasterxml.jackson.databind.ObjectMapper
import org.example.common.contract.ApiContractException
import org.example.route.dto.PublicRouteBrowseSummary
import org.example.route.dto.PublicRouteCampsite
import org.example.route.dto.PublicRouteCommunicationAndSafety
import org.example.route.dto.PublicRouteDetail
import org.example.route.dto.PublicRouteMainTrackPathPosition
import org.example.route.dto.PublicRouteMainTrackRange
import org.example.route.dto.PublicRouteOvernightPlace
import org.example.route.dto.PublicRouteProfessionalAnalysis
import org.example.route.dto.PublicRouteReferenceDay
import org.example.route.dto.PublicRouteSeasonalEquipmentRecommendation
import org.example.route.dto.PublicRouteSeasonalWeather
import org.example.route.dto.PublicRouteGeoPosition
import org.example.route.dto.PublicRouteImage
import org.example.route.dto.PublicRouteMainTrack
import org.example.route.dto.PublicRouteNamedPoint
import org.example.route.dto.PublicRoutePlace
import org.example.route.dto.PublicRouteSafetyNotice
import org.example.route.dto.PublicRouteSearchSummary
import org.example.route.dto.PublicRouteSegment
import org.example.route.dto.PublicRouteSummary
import org.example.route.dto.PublicRouteSupplyPoint
import org.example.route.dto.PublicRouteVersionDetail
import org.example.route.dto.PublicRouteWaterSource
import org.example.route.dto.RouteGenerationEligibility
import org.example.route.dto.RouteMeters
import org.example.route.dto.RouteSeconds
import org.example.route.model.PublicRouteCollectionEntry
import org.example.route.model.RouteVersion
import org.example.route.model.RouteVersionImage
import org.example.route.model.RouteVersionPoint
import org.example.route.model.RouteVersionSegment
import org.example.route.repository.PublicRouteCollectionRepository
import org.example.route.repository.RouteCurrentPublicVersionRepository
import org.example.route.repository.RouteVersionImageRepository
import org.example.route.repository.RouteVersionPointRepository
import org.example.route.repository.RouteVersionRepository
import org.example.route.repository.RouteVersionSegmentRepository
import org.springframework.http.HttpStatus
import org.springframework.stereotype.Service

@Service
class RouteVersionSummaryPlaceResolver {
    fun resolve(version: RouteVersion, points: List<RouteVersionPoint>): RouteVersionSummaryPlaces =
        RouteVersionSummaryPlaces(
            start = place(version.startName, points, "start"),
            end = place(version.endName, points, "end")
        )

    private fun place(
        storedName: String?,
        points: List<RouteVersionPoint>,
        pointKind: String
    ): PublicRoutePlace? {
        val name = storedName?.takeIf { it.isNotBlank() } ?: return null
        val matchingPoints = points.filter { it.pointKind == pointKind && it.name.trim() == name }
        val position = matchingPoints.singleOrNull()?.toSummaryPosition()
        return PublicRoutePlace(name, position)
    }

    private fun RouteVersionPoint.toSummaryPosition(): PublicRouteGeoPosition? =
        if (latitude.isFinite() && longitude.isFinite() && latitude in -90.0..90.0 && longitude in -180.0..180.0 && referenceSystem.isNotBlank()) {
            PublicRouteGeoPosition(latitude, longitude, referenceSystem)
        } else {
            null
        }
}

data class RouteVersionSummaryPlaces(
    val start: PublicRoutePlace?,
    val end: PublicRoutePlace?
)

@Service
class PublicRouteDomainService(
    private val collectionRepository: PublicRouteCollectionRepository,
    private val currentVersionRepository: RouteCurrentPublicVersionRepository,
    private val versionRepository: RouteVersionRepository,
    private val imageRepository: RouteVersionImageRepository,
    private val segmentRepository: RouteVersionSegmentRepository,
    private val pointRepository: RouteVersionPointRepository,
    private val objectMapper: ObjectMapper,
    private val summaryPlaceResolver: RouteVersionSummaryPlaceResolver
) {
    companion object {
        private val MAIN_TRACK_AVAILABILITIES = setOf(
            "missing",
            "processing",
            "pending_review",
            "valid",
            "invalidated"
        )
    }

    fun orderedVersions(featuredOnly: Boolean): List<RouteVersion> {
        val entries = if (featuredOnly) {
            collectionRepository.findAllByFeaturedOrderIsNotNullOrderByFeaturedOrderAsc()
        } else {
            collectionRepository.findAllByOrderByAllRouteOrderAsc()
        }
        return entries.map(::resolveCurrentVersion)
    }

    fun findPublicVersion(routeId: String): RouteVersion {
        val membership = collectionRepository.findById(routeId).orElseThrow(::routeNotFound)
        return resolveCurrentVersion(membership)
    }

    fun browseSummary(version: RouteVersion): PublicRouteBrowseSummary {
        val cover = imageRepository.findByRouteVersionIdOrderByDisplayOrderAsc(version.id)
            .firstOrNull { it.role == "cover" }
            ?.mediaReference
        return PublicRouteBrowseSummary(
            routeId = version.routeId,
            currentVersionId = version.id,
            cover = cover.nonBlankOrNull(),
            name = version.name.nonBlankOrNull(),
            region = version.region.nonBlankOrNull(),
            difficulty = version.difficulty.nonBlankOrNull(),
            distance = version.distanceMeters?.nonNegativeFinite()?.let(::RouteMeters),
            ascent = version.ascentMeters?.nonNegativeFinite()?.let(::RouteMeters),
            estimatedDuration = version.estimatedDurationSeconds?.takeIf { it >= 0 }?.toDouble()?.let(::RouteSeconds)
        )
    }

    fun searchSummary(version: RouteVersion): PublicRouteSearchSummary {
        val browse = browseSummary(version)
        return PublicRouteSearchSummary(
            routeId = browse.routeId,
            currentVersionId = browse.currentVersionId,
            cover = browse.cover,
            name = browse.name,
            region = browse.region,
            difficulty = browse.difficulty,
            distance = browse.distance,
            estimatedDuration = browse.estimatedDuration
        )
    }

    fun detail(version: RouteVersion): PublicRouteDetail {
        if (version.mainTrackAvailability !in MAIN_TRACK_AVAILABILITIES) throw readFailure()
        val points = pointRepository.findByRouteVersionIdOrderByDisplayOrderAsc(version.id)
        val mainTrack = parseValidMainTrack(version)
        val summaryPlaces = summaryPlaceResolver.resolve(version, points)
        val summary = PublicRouteSummary(
            routeType = version.routeType ?: throw readFailure(),
            name = version.name.nonBlankOrNull(),
            region = version.region.nonBlankOrNull(),
            start = summaryPlaces.start,
            end = summaryPlaces.end,
            estimatedDuration = version.estimatedDurationSeconds?.takeIf { it >= 0 }?.toDouble()?.let(::RouteSeconds),
            difficulty = version.difficulty.nonBlankOrNull(),
            direction = version.direction.nonBlankOrNull(),
            distance = version.distanceMeters?.nonNegativeFinite()?.let(::RouteMeters),
            ascent = version.ascentMeters?.nonNegativeFinite()?.let(::RouteMeters),
            descent = version.descentMeters?.nonNegativeFinite()?.let(::RouteMeters),
            maxElevation = version.maxElevationMeters?.toDouble()?.finite()?.let(::RouteMeters),
            suggestedDays = version.suggestedDays?.takeIf { it > 0 },
            tags = parseOptionalNonEmptyList(version.tagsJson),
            introduction = version.introduction.nonBlankOrNull()
        )
        val images = imageRepository.findByRouteVersionIdOrderByDisplayOrderAsc(version.id)
            .mapNotNull { it.toDto() }
            .takeIf { it.isNotEmpty() }
        val segments = segmentRepository.findByRouteVersionIdOrderBySegmentOrderAsc(version.id)
            .mapNotNull { it.toDto(mainTrack) }
            .takeIf { it.isNotEmpty() }

        return PublicRouteDetail(
            routeId = version.routeId,
            currentVersion = PublicRouteVersionDetail(
                versionId = version.id,
                versionLabel = version.versionLabel.nonBlankOrNull(),
                summary = summary,
                mainTrackAvailability = version.mainTrackAvailability,
                mainTrack = mainTrack,
                generationEligibility = eligibility(summary, version.mainTrackAvailability, mainTrack),
                professionalAnalysis = parseProfessionalAnalysis(version.professionalAnalysisJson),
                referenceDays = if (version.routeType == "multi_day") parseReferenceDays(version.referenceDaysJson) else null,
                segments = segments,
                keyPoints = points.filter { it.pointKind == "key" }.mapNotNull { it.toNamedPoint() }.takeIf { it.isNotEmpty() },
                interestPoints = points.filter { it.pointKind == "interest" }.mapNotNull { it.toNamedPoint() }.takeIf { it.isNotEmpty() },
                campsites = points.filter { it.pointKind == "campsite" }.mapNotNull { it.toCampsite() }.takeIf { it.isNotEmpty() },
                overnightPlaces = points.filter { it.pointKind == "overnight_place" }
                    .mapNotNull { it.toOvernightPlace() }.takeIf { it.isNotEmpty() },
                waterSources = points.filter { it.pointKind == "water_source" }.mapNotNull { it.toWaterSource() }.takeIf { it.isNotEmpty() },
                supplyPoints = points.filter { it.pointKind == "supply_point" }.mapNotNull { it.toSupplyPoint() }.takeIf { it.isNotEmpty() },
                communicationAndSafety = points.filter { it.pointKind == "safety_notice" }
                    .mapNotNull { it.toSafetyNotice() }
                    .takeIf { it.isNotEmpty() }
                    ?.let { PublicRouteCommunicationAndSafety(notices = it) },
                seasonalWeather = parseSeasonalWeather(version.seasonalWeatherJson),
                seasonalEquipmentRecommendations = parseSeasonalEquipmentRecommendations(
                    version.seasonalEquipmentRecommendationsJson
                ),
                images = images
            )
        )
    }

    private fun resolveCurrentVersion(entry: PublicRouteCollectionEntry): RouteVersion {
        val current = currentVersionRepository.findById(entry.routeId).orElseThrow(::readFailure)
        val version = versionRepository.findById(current.routeVersionId).orElseThrow(::readFailure)
        if (version.routeId != entry.routeId) throw readFailure()
        return version
    }

    private fun parseValidMainTrack(version: RouteVersion): PublicRouteMainTrack? {
        if (version.mainTrackAvailability != "valid") return null
        val referenceSystem = version.mainTrackReferenceSystem.nonBlankOrNull() ?: throw readFailure()
        val json = version.mainTrackJson.nonBlankOrNull() ?: throw readFailure()
        val raw = try {
            objectMapper.readValue(json, object : TypeReference<List<List<Double?>>>() {})
        } catch (_: Exception) {
            throw readFailure()
        }
        val path = raw.map { item ->
            if (item.size < 2) throw readFailure()
            val latitude = item[0]
            val longitude = item[1]
            if (latitude == null || longitude == null || !latitude.isFinite() || !longitude.isFinite() || latitude !in -90.0..90.0 || longitude !in -180.0..180.0) {
                throw readFailure()
            }
            PublicRouteGeoPosition(latitude, longitude, referenceSystem)
        }
        if (path.isEmpty()) throw readFailure()
        return PublicRouteMainTrack(identity = "${version.id}:main-track", path = path)
    }

    private fun eligibility(
        summary: PublicRouteSummary,
        availability: String,
        mainTrack: PublicRouteMainTrack?
    ): RouteGenerationEligibility {
        val reasons = buildList {
            if (summary.name == null) add("name")
            if (summary.region == null) add("region")
            if (summary.estimatedDuration == null) add("estimatedDuration")
            if (summary.start == null) add("start")
            if (summary.end == null) add("end")
            if (availability != "valid" || mainTrack == null) add("validMainTrack")
        }
        return if (reasons.isEmpty()) {
            RouteGenerationEligibility(eligible = true)
        } else {
            RouteGenerationEligibility(eligible = false, missingReasons = reasons)
        }
    }

    private fun RouteVersionPoint.toGeoPosition(): PublicRouteGeoPosition? =
        if (latitude.isFinite() && longitude.isFinite() && latitude in -90.0..90.0 && longitude in -180.0..180.0 && referenceSystem.isNotBlank()) {
            PublicRouteGeoPosition(latitude, longitude, referenceSystem)
        } else {
            null
        }

    private fun RouteVersionPoint.toPlace(): PublicRoutePlace? {
        val validName = name.nonBlankOrNull() ?: return null
        return PublicRoutePlace(validName, toGeoPosition())
    }

    private fun RouteVersionPoint.toNamedPoint(): PublicRouteNamedPoint? {
        val validName = name.nonBlankOrNull() ?: return null
        val position = toGeoPosition() ?: return null
        return PublicRouteNamedPoint(
            identity = id,
            name = validName,
            positions = listOf(position),
            category = category.nonBlankOrNull(),
            description = description.nonBlankOrNull(),
            elevation = elevation?.finite()?.let(::RouteMeters)
        )
    }

    private fun RouteVersionPoint.toCampsite(): PublicRouteCampsite? {
        val validName = name.nonBlankOrNull() ?: return null
        val position = toGeoPosition() ?: return null
        return PublicRouteCampsite(
            identity = id,
            name = validName,
            positions = listOf(position),
            elevation = elevation?.finite()?.let(::RouteMeters),
            details = description.nonBlankOrNull()
        )
    }

    private fun RouteVersionPoint.toOvernightPlace(): PublicRouteOvernightPlace? {
        val validName = name.nonBlankOrNull() ?: return null
        val position = toGeoPosition() ?: return null
        return PublicRouteOvernightPlace(
            identity = id,
            name = validName,
            positions = listOf(position),
            elevation = elevation?.finite()?.let(::RouteMeters),
            type = subCategory.nonBlankOrNull(),
            details = description.nonBlankOrNull()
        )
    }

    private fun RouteVersionPoint.toWaterSource(): PublicRouteWaterSource? {
        val validName = name.nonBlankOrNull() ?: return null
        val position = toGeoPosition() ?: return null
        return PublicRouteWaterSource(
            identity = id,
            name = validName,
            positions = listOf(position),
            elevation = elevation?.finite()?.let(::RouteMeters),
            sourceType = subCategory.nonBlankOrNull(),
            description = description.nonBlankOrNull()
        )
    }

    private fun RouteVersionPoint.toSupplyPoint(): PublicRouteSupplyPoint? {
        val validName = name.nonBlankOrNull() ?: return null
        val position = toGeoPosition() ?: return null
        return PublicRouteSupplyPoint(
            identity = id,
            name = validName,
            positions = listOf(position),
            supplyType = subCategory.nonBlankOrNull(),
            description = description.nonBlankOrNull()
        )
    }

    private fun RouteVersionPoint.toSafetyNotice(): PublicRouteSafetyNotice? {
        val validTitle = name.nonBlankOrNull() ?: return null
        val validDescription = description.nonBlankOrNull() ?: return null
        val kind = category.nonBlankOrNull() ?: return null
        return PublicRouteSafetyNotice(
            identity = id,
            kind = kind,
            title = validTitle,
            description = validDescription,
            positions = toGeoPosition()?.let(::listOf)
        )
    }

    private fun RouteVersionImage.toDto(): PublicRouteImage? {
        val reference = mediaReference.nonBlankOrNull() ?: return null
        if (role != "cover" && role != "environment") return null
        return PublicRouteImage(
            identity = id,
            mediaReference = reference,
            role = role,
            caption = caption.nonBlankOrNull()
        )
    }

    private fun RouteVersionSegment.toDto(mainTrack: PublicRouteMainTrack?): PublicRouteSegment? {
        val validName = name.nonBlankOrNull() ?: return null
        if (segmentOrder < 1) return null
        return PublicRouteSegment(
            identity = id,
            order = segmentOrder,
            name = validName,
            start = startName.nonBlankOrNull()?.let(::PublicRoutePlace),
            end = endName.nonBlankOrNull()?.let(::PublicRoutePlace),
            distance = distanceMeters?.nonNegativeFinite()?.let(::RouteMeters),
            estimatedDuration = estimatedDurationSeconds?.takeIf { it >= 0 }?.toDouble()?.let(::RouteSeconds),
            ascent = ascentMeters?.nonNegativeFinite()?.let(::RouteMeters),
            descent = descentMeters?.nonNegativeFinite()?.let(::RouteMeters),
            difficulty = difficulty.nonBlankOrNull(),
            terrainOrRoadType = terrainOrRoadType.nonBlankOrNull(),
            description = description.nonBlankOrNull(),
            notes = notes.nonBlankOrNull(),
            mainTrackRange = parseMainTrackRange(mainTrackRangeJson, mainTrack)
        )
    }

    private fun parseProfessionalAnalysis(json: String?): PublicRouteProfessionalAnalysis? =
        parseOptional<PublicRouteProfessionalAnalysis>(json)?.takeIf {
            listOf(
                it.fitnessRequirement,
                it.experienceRequirement,
                it.mainTerrain,
                it.technicalDifficulty,
                it.highAltitudeImpact,
                it.exposedSections,
                it.dangerAreas,
                it.signalCoverage,
                it.evacuationDifficulty
            ).any { value -> value != null }
        }

    private fun parseReferenceDays(json: String?): List<PublicRouteReferenceDay>? {
        val days = parseOptionalNonEmptyList<PublicRouteReferenceDay>(json) ?: return null
        if (days.map { it.dayNumber } != (1..days.size).toList() || days.any { it.identity.isBlank() }) {
            throw readFailure()
        }
        return days
    }

    private fun parseSeasonalWeather(json: String?): PublicRouteSeasonalWeather? =
        parseOptional<PublicRouteSeasonalWeather>(json)?.takeIf {
            !it.bestSeasons.isNullOrEmpty() || !it.conditions.isNullOrEmpty()
        }

    private fun parseSeasonalEquipmentRecommendations(
        json: String?
    ): List<PublicRouteSeasonalEquipmentRecommendation>? {
        val recommendations = parseOptionalNonEmptyList<PublicRouteSeasonalEquipmentRecommendation>(json) ?: return null
        if (recommendations.any {
                it.identity.isBlank() || it.seasonOrCondition.isBlank() || it.name.isBlank() ||
                    it.level !in setOf("required", "recommended")
            }
        ) {
            throw readFailure()
        }
        return recommendations
    }

    private fun parseMainTrackRange(json: String?, mainTrack: PublicRouteMainTrack?): PublicRouteMainTrackRange? {
        val range = parseOptional<PublicRouteMainTrackRange>(json) ?: return null
        val pathSize = mainTrack?.path?.size ?: throw readFailure()
        validatePathPosition(range.startPathPosition, pathSize)
        validatePathPosition(range.endPathPosition, pathSize)
        if (comparePathPosition(range.startPathPosition, range.endPathPosition) >= 0) throw readFailure()
        return range
    }

    private fun validatePathPosition(position: PublicRouteMainTrackPathPosition, pathSize: Int) {
        val isFinalPosition = position.precedingPositionIndex == pathSize - 1
        if (position.precedingPositionIndex !in 0 until pathSize ||
            position.progressToNextPosition?.let { !it.isFinite() || it < 0.0 || it >= 1.0 } == true ||
            (!isFinalPosition && position.progressToNextPosition == null) ||
            (isFinalPosition && position.progressToNextPosition != null)
        ) {
            throw readFailure()
        }
    }

    private fun comparePathPosition(
        first: PublicRouteMainTrackPathPosition,
        second: PublicRouteMainTrackPathPosition
    ): Int {
        val indexComparison = first.precedingPositionIndex.compareTo(second.precedingPositionIndex)
        if (indexComparison != 0) return indexComparison
        return (first.progressToNextPosition ?: 0.0).compareTo(second.progressToNextPosition ?: 0.0)
    }

    private inline fun <reified T> parseOptional(json: String?): T? {
        val content = json.nonBlankOrNull() ?: return null
        return try {
            objectMapper.readValue(content, object : TypeReference<T>() {})
        } catch (_: Exception) {
            throw readFailure()
        }
    }

    private inline fun <reified T> parseOptionalNonEmptyList(json: String?): List<T>? =
        parseOptional<List<T>>(json)?.takeIf { it.isNotEmpty() }

    private fun java.math.BigDecimal.nonNegativeFinite(): Double? = toDouble().takeIf { it.isFinite() && it >= 0 }
    private fun Double.finite(): Double? = takeIf(Double::isFinite)
    private fun String?.nonBlankOrNull(): String? = this?.takeIf { it.isNotBlank() }

    private fun routeNotFound() = ApiContractException(HttpStatus.NOT_FOUND, "route_not_found", "路线不存在")
    private fun readFailure() = ApiContractException.serviceUnavailable("public_route_read_failed", "公共路线资料暂时无法读取")
}
