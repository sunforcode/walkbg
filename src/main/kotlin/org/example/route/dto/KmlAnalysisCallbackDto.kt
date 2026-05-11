package org.example.route.dto

import com.fasterxml.jackson.annotation.JsonIgnoreProperties
import com.fasterxml.jackson.annotation.JsonProperty
import java.math.BigDecimal

@JsonIgnoreProperties(ignoreUnknown = true)
data class KmlAnalysisCallbackRequest(
    @JsonProperty("route_id")
    val routeId: String?,
    
    @JsonProperty("task_id")
    val taskId: String?,
    
    @JsonProperty("source_kml_url")
    val sourceKmlUrl: String?,
    
    @JsonProperty("analysis_timestamp")
    val analysisTimestamp: String?,  // agent 返回无时区后缀的字符串，用 String 接收
    
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
    
    @JsonProperty("segment_schemes")
    val segmentSchemes: List<CallbackSegmentSchemeDto> = emptyList(),

    @JsonProperty("poi_points")
    val poiPoints: List<CallbackPoiPointDto> = emptyList(),

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

/**
 * 分段方案 DTO
 * 一个方案包含一种维度的多个分段
 */
data class CallbackSegmentSchemeDto(
    @JsonProperty("scheme_type")
    val schemeType: String,               // slope | day | terrain | road_type

    val label: String,                    // 展示标签，如"按坡度"

    @JsonProperty("is_default")
    val isDefault: Boolean = false,

    val segments: List<CallbackSegmentDto> = emptyList()
)

/**
 * 统一附属信息点 DTO
 * 包含 category/card_data 的灵活结构，取代旧的四张表
 */
data class CallbackPoiPointDto(
    val name: String,
    val latitude: Double,
    val longitude: Double,
    val elevation: Double? = null,

    /**
     * POI 类型：water | camp | supply | photo | pass | valley | weather | danger | start | end
     */
    val category: String,

    @JsonProperty("sub_category")
    val subCategory: String? = null,

    /**
     * 数据来源：kml_marker | algorithm | osm | weather_api | experience
     */
    val source: String = "kml_marker",

    val description: String? = null,
    val confidence: Double? = null,

    /**
     * 各 category 的扩展属性 JSON，walkbg 不解析，直接透传给 App
     */
    @JsonProperty("card_data")
    val cardData: Map<String, Any?>? = null
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

data class CallbackWarningDto(
    val level: String,
    val message: String,
    val location: Map<String, Double>? = null,
    val detail: String? = null
)
