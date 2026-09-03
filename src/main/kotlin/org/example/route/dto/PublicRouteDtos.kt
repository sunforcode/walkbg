package org.example.route.dto

import com.fasterxml.jackson.annotation.JsonInclude
data class RouteMeters(val meters: Double)

data class RouteSeconds(val seconds: Double)

data class PublicRouteDiscoveryResult(val items: List<PublicRouteBrowseSummary>)

data class PublicRouteCollectionResult(val items: List<PublicRouteBrowseSummary>)

data class PublicRouteSearchResult(
    val state: String,
    val items: List<PublicRouteSearchSummary>
)

@JsonInclude(JsonInclude.Include.NON_NULL)
data class PublicRouteBrowseSummary(
    val routeId: String,
    val currentVersionId: String,
    val cover: String? = null,
    val name: String? = null,
    val region: String? = null,
    val difficulty: String? = null,
    val distance: RouteMeters? = null,
    val ascent: RouteMeters? = null,
    val estimatedDuration: RouteSeconds? = null
)

@JsonInclude(JsonInclude.Include.NON_NULL)
data class PublicRouteSearchSummary(
    val routeId: String,
    val currentVersionId: String,
    val cover: String? = null,
    val name: String? = null,
    val region: String? = null,
    val difficulty: String? = null,
    val distance: RouteMeters? = null,
    val estimatedDuration: RouteSeconds? = null
)

data class PublicRouteDetail(
    val routeId: String,
    val currentVersion: PublicRouteVersionDetail
)

@JsonInclude(JsonInclude.Include.NON_NULL)
data class PublicRouteVersionDetail(
    val versionId: String,
    val summary: PublicRouteSummary,
    val mainTrackAvailability: String,
    val generationEligibility: RouteGenerationEligibility,
    val versionLabel: String? = null,
    val mainTrack: PublicRouteMainTrack? = null,
    val elevationProfile: PublicRouteElevationProfile? = null,
    val professionalAnalysis: PublicRouteProfessionalAnalysis? = null,
    val referenceDays: List<PublicRouteReferenceDay>? = null,
    val segments: List<PublicRouteSegment>? = null,
    val keyPoints: List<PublicRouteNamedPoint>? = null,
    val interestPoints: List<PublicRouteNamedPoint>? = null,
    val campsites: List<PublicRouteCampsite>? = null,
    val overnightPlaces: List<PublicRouteOvernightPlace>? = null,
    val waterSources: List<PublicRouteWaterSource>? = null,
    val supplyPoints: List<PublicRouteSupplyPoint>? = null,
    val communicationAndSafety: PublicRouteCommunicationAndSafety? = null,
    val seasonalWeather: PublicRouteSeasonalWeather? = null,
    val seasonalEquipmentRecommendations: List<PublicRouteSeasonalEquipmentRecommendation>? = null,
    val images: List<PublicRouteImage>? = null
)

@JsonInclude(JsonInclude.Include.NON_NULL)
data class PublicRouteSummary(
    val routeType: String,
    val name: String? = null,
    val region: String? = null,
    val start: PublicRoutePlace? = null,
    val end: PublicRoutePlace? = null,
    val estimatedDuration: RouteSeconds? = null,
    val difficulty: String? = null,
    val direction: String? = null,
    val distance: RouteMeters? = null,
    val ascent: RouteMeters? = null,
    val descent: RouteMeters? = null,
    val maxElevation: RouteMeters? = null,
    val suggestedDays: Int? = null,
    val tags: List<String>? = null,
    val introduction: String? = null
)

@JsonInclude(JsonInclude.Include.NON_NULL)
data class PublicRoutePlace(
    val name: String,
    val position: PublicRouteGeoPosition? = null
)

data class PublicRouteGeoPosition(
    val latitude: Double,
    val longitude: Double,
    val referenceSystem: String
)

data class PublicRouteMainTrack(
    val identity: String,
    val path: List<PublicRouteGeoPosition>
)

data class PublicRouteElevationProfile(
    val minElevation: RouteMeters,
    val maxElevation: RouteMeters,
    val samples: List<PublicRouteElevationSample>
)

data class PublicRouteElevationSample(
    val identity: String,
    val distance: RouteMeters,
    val elevation: RouteMeters
)

@JsonInclude(JsonInclude.Include.NON_NULL)
data class PublicRouteProfessionalAnalysis(
    val fitnessRequirement: QualifiedRouteText? = null,
    val experienceRequirement: QualifiedRouteText? = null,
    val mainTerrain: QualifiedRouteText? = null,
    val technicalDifficulty: QualifiedRouteText? = null,
    val highAltitudeImpact: QualifiedRouteText? = null,
    val exposedSections: QualifiedRouteText? = null,
    val dangerAreas: QualifiedRouteText? = null,
    val signalCoverage: QualifiedRouteText? = null,
    val evacuationDifficulty: QualifiedRouteText? = null
)

@JsonInclude(JsonInclude.Include.NON_NULL)
data class QualifiedRouteText(
    val value: String? = null,
    val confidence: PublicRouteInformationConfidence? = null
)

@JsonInclude(JsonInclude.Include.NON_NULL)
data class PublicRouteInformationConfidence(
    val status: String,
    val category: String? = null
)

@JsonInclude(JsonInclude.Include.NON_NULL)
data class PublicRouteReferenceDay(
    val identity: String,
    val dayNumber: Int,
    val title: String? = null,
    val start: PublicRoutePlace? = null,
    val end: PublicRoutePlace? = null,
    val distance: RouteMeters? = null,
    val estimatedDuration: RouteSeconds? = null,
    val ascent: RouteMeters? = null,
    val descent: RouteMeters? = null,
    val maxElevation: RouteMeters? = null,
    val minElevation: RouteMeters? = null,
    val accommodation: String? = null,
    val notes: String? = null
)

@JsonInclude(JsonInclude.Include.NON_NULL)
data class PublicRouteSegment(
    val identity: String,
    val order: Int,
    val name: String,
    val start: PublicRoutePlace? = null,
    val end: PublicRoutePlace? = null,
    val distance: RouteMeters? = null,
    val estimatedDuration: RouteSeconds? = null,
    val ascent: RouteMeters? = null,
    val descent: RouteMeters? = null,
    val difficulty: String? = null,
    val terrainOrRoadType: String? = null,
    val description: String? = null,
    val notes: String? = null,
    val mainTrackRange: PublicRouteMainTrackRange? = null
)

data class PublicRouteMainTrackRange(
    val startPathPosition: PublicRouteMainTrackPathPosition,
    val endPathPosition: PublicRouteMainTrackPathPosition
)

@JsonInclude(JsonInclude.Include.NON_NULL)
data class PublicRouteMainTrackPathPosition(
    val precedingPositionIndex: Int,
    val progressToNextPosition: Double? = null
)

@JsonInclude(JsonInclude.Include.NON_NULL)
data class PublicRouteNamedPoint(
    val identity: String,
    val name: String,
    val positions: List<PublicRouteGeoPosition>,
    val category: String? = null,
    val description: String? = null,
    val elevation: RouteMeters? = null
)

@JsonInclude(JsonInclude.Include.NON_NULL)
data class PublicRouteCampsite(
    val identity: String,
    val name: String,
    val positions: List<PublicRouteGeoPosition>,
    val elevation: RouteMeters? = null,
    val details: String? = null,
    val status: String? = null
)

@JsonInclude(JsonInclude.Include.NON_NULL)
data class PublicRouteOvernightPlace(
    val identity: String,
    val name: String,
    val positions: List<PublicRouteGeoPosition>,
    val elevation: RouteMeters? = null,
    val type: String? = null,
    val facilities: List<String>? = null,
    val details: String? = null,
    val status: String? = null
)

@JsonInclude(JsonInclude.Include.NON_NULL)
data class PublicRouteWaterSource(
    val identity: String,
    val name: String,
    val positions: List<PublicRouteGeoPosition>,
    val elevation: RouteMeters? = null,
    val sourceType: String? = null,
    val description: String? = null
)

@JsonInclude(JsonInclude.Include.NON_NULL)
data class PublicRouteSupplyPoint(
    val identity: String,
    val name: String,
    val positions: List<PublicRouteGeoPosition>,
    val supplyType: String? = null,
    val description: String? = null
)

data class PublicRouteCommunicationAndSafety(
    val coverageSummary: String? = null,
    val notices: List<PublicRouteSafetyNotice>? = null
)

@JsonInclude(JsonInclude.Include.NON_NULL)
data class PublicRouteSafetyNotice(
    val identity: String,
    val kind: String,
    val title: String,
    val description: String,
    val positions: List<PublicRouteGeoPosition>? = null
)

@JsonInclude(JsonInclude.Include.NON_NULL)
data class PublicRouteImage(
    val identity: String,
    val mediaReference: String,
    val role: String,
    val caption: String? = null,
    val position: PublicRouteGeoPosition? = null
)

@JsonInclude(JsonInclude.Include.NON_NULL)
data class PublicRouteSeasonalWeather(
    val bestSeasons: List<String>? = null,
    val conditions: List<PublicRouteSeasonalWeatherCondition>? = null
)

@JsonInclude(JsonInclude.Include.NON_NULL)
data class PublicRouteSeasonalWeatherCondition(
    val identity: String,
    val season: String,
    val typicalWeather: String? = null,
    val environmentalRisks: List<String>? = null,
    val notes: String? = null
)

@JsonInclude(JsonInclude.Include.NON_NULL)
data class PublicRouteSeasonalEquipmentRecommendation(
    val identity: String,
    val seasonOrCondition: String,
    val name: String,
    val level: String,
    val reason: String? = null
)

@JsonInclude(JsonInclude.Include.NON_NULL)
data class RouteGenerationEligibility(
    val eligible: Boolean,
    val missingReasons: List<String>? = null
)
