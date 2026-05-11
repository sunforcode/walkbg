package org.example.route.dto

import com.fasterxml.jackson.annotation.JsonProperty
import com.fasterxml.jackson.annotation.JsonIgnoreProperties

data class KmlAnalysisSubmitRequest(
    @JsonProperty("route_id")
    val routeId: String? = null,
    
    @JsonProperty("kml_source")
    val kmlSource: String,
    
    @JsonProperty("kml_content")
    val kmlContent: String? = null,
    
    @JsonProperty("enable_content_generation")
    val enableContentGeneration: Boolean = true,
    
    @JsonProperty("enable_poi_query")
    val enablePoiQuery: Boolean = true,
    
    @JsonProperty("poi_search_radius")
    val poiSearchRadius: Int = 500,
    
    @JsonProperty("region_name")
    val regionName: String? = null,
    
    @JsonProperty("estimated_difficulty")
    val estimatedDifficulty: Int? = null,
    
    @JsonProperty("user_notes")
    val userNotes: String? = null
)

data class TaskSubmitResponse(
    @JsonProperty("task_id")
    val taskId: String,
    
    val status: String,
    
    val message: String,
    
    @JsonProperty("estimated_seconds")
    val estimatedSeconds: Int
)

data class TaskStatusResponse(
    @JsonProperty("task_id")
    val taskId: String,
    
    val status: String,
    
    val progress: Int? = 0,
    
    @JsonProperty("current_step")
    val currentStep: String? = null,
    
    val message: String? = null,
    
    val result: Map<String, Any>? = null,
    
    val error: String? = null,
    
    @JsonProperty("quality_score")
    val qualityScore: Double? = null
)

@JsonIgnoreProperties(ignoreUnknown = true)
data class HealthCheckResponse(
    val status: String,
    val version: String,
    val timestamp: String? = null,  // agent 返回无时区后缀的字符串，用 String 接收
    val checks: Map<String, String>? = null
)
