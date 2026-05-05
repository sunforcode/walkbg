package org.example.route.dto

import com.fasterxml.jackson.annotation.JsonProperty
import java.math.BigDecimal
import java.time.Instant

data class KmlAnalysisCallbackRequest(
    @JsonProperty("route_id")
    val routeId: String?,
    
    @JsonProperty("task_id")
    val taskId: String?,
    
    @JsonProperty("source_kml_url")
    val sourceKmlUrl: String?,
    
    @JsonProperty("analysis_timestamp")
    val analysisTimestamp: Instant?,
    
    @JsonProperty("quality_score")
    val qualityScore: Double?,
    
    @JsonProperty("total_distance_km")
    val totalDistanceKm: Double?,
    
    @JsonProperty("total_elevation_gain_m")
    val totalElevationGainM: Double?,
    
    @JsonProperty("total_elevation_loss_m")
    val totalElevationLossM: Double?,
    
    @JsonProperty("max_elevation")
    val maxElevation: Double?,
    
    @JsonProperty("min_elevation")
    val minElevation: Double?,
    
    @JsonProperty("is_loop")
    val isLoop: Boolean?,
    
    @JsonProperty("estimated_difficulty")
    val estimatedDifficulty: Int?,
    
    val segments: List<CallbackSegmentDto> = emptyList(),
    
    @JsonProperty("water_sources")
    val waterSources: List<CallbackWaterSourceDto> = emptyList(),
    
    val campsites: List<CallbackCampsiteDto> = emptyList(),
    
    val supplies: List<CallbackSupplyDto> = emptyList(),
    
    @JsonProperty("marker_points")
    val markerPoints: List<CallbackMarkerPointDto> = emptyList(),
    
    @JsonProperty("generated_description")
    val generatedDescription: String?,
    
    @JsonProperty("generated_highlights")
    val generatedHighlights: List<String> = emptyList(),
    
    @JsonProperty("generated_difficulties")
    val generatedDifficulties: List<String> = emptyList(),
    
    @JsonProperty("generated_safety_notes")
    val generatedSafetyNotes: List<String> = emptyList(),
    
    @JsonProperty("equipment_recommendations")
    val equipmentRecommendations: List<String> = emptyList(),
    
    val warnings: List<CallbackWarningDto> = emptyList()
)

data class CallbackSegmentDto(
    val id: String,
    val name: String,
    
    @JsonProperty("sequence_number")
    val sequenceNumber: Int,
    
    val color: String,
    val description: String?,
    
    val distance: Double,
    
    @JsonProperty("elevation_gain")
    val elevationGain: Double,
    
    @JsonProperty("elevation_loss")
    val elevationLoss: Double,
    
    @JsonProperty("estimated_time")
    val estimatedTime: Int,
    
    val difficulty: Int,
    
    @JsonProperty("track_start_index")
    val trackStartIndex: Int?,
    
    @JsonProperty("track_end_index")
    val trackEndIndex: Int?,
    
    @JsonProperty("start_point")
    val startPoint: CallbackTrackPointDto?,
    
    @JsonProperty("end_point")
    val endPoint: CallbackTrackPointDto?,
    
    @JsonProperty("segment_type")
    val segmentType: String?,
    
    @JsonProperty("slope_direction")
    val slopeDirection: String?,
    
    @JsonProperty("avg_slope_degrees")
    val avgSlopeDegrees: Double?,
    
    @JsonProperty("max_slope_degrees")
    val maxSlopeDegrees: Double?,
    
    val confidence: Double?,
    
    val notes: String?
)

data class CallbackTrackPointDto(
    val latitude: Double,
    val longitude: Double,
    val elevation: Double?
)

data class CallbackWaterSourceDto(
    val name: String?,
    val latitude: Double,
    val longitude: Double,
    val elevation: Double?,
    val description: String?,
    
    @JsonProperty("source_type")
    val sourceType: String = "unknown",
    
    val reliability: Double = 0.5,
    
    val notes: String?
)

data class CallbackCampsiteDto(
    val name: String?,
    val latitude: Double,
    val longitude: Double,
    val elevation: Double?,
    val description: String?,
    val capacity: Int?,
    
    @JsonProperty("has_water")
    val hasWater: Boolean?,
    
    @JsonProperty("has_facilities")
    val hasFacilities: Boolean?,
    
    val notes: String?
)

data class CallbackSupplyDto(
    val name: String?,
    val latitude: Double,
    val longitude: Double,
    val elevation: Double?,
    val description: String?,
    
    @JsonProperty("supply_type")
    val supplyType: String = "unknown",
    
    val notes: String?
)

data class CallbackMarkerPointDto(
    val name: String?,
    val latitude: Double,
    val longitude: Double,
    val elevation: Double?,
    val description: String?,
    
    val type: String = "viewpoint",
    
    @JsonProperty("image_url")
    val imageUrl: String?,
    
    @JsonProperty("icon_url")
    val iconUrl: String?,
    
    val notes: String?
)

data class CallbackWarningDto(
    val level: String,
    val message: String,
    val location: Map<String, Double>? = null,
    val detail: String? = null
)
