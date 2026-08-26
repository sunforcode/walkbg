package org.example.route.dto

import com.fasterxml.jackson.annotation.JsonProperty

data class AnalysisExecutionEventRequest(
    @JsonProperty("task_id")
    val taskId: String,

    @JsonProperty("execution_event")
    val executionEvent: AnalysisExecutionEvent
)

data class AnalysisExecutionEvent(
    val timestamp: String,
    val node: String,
    val phase: String,
    val level: String,
    val message: String,
    val progress: Int,
    val details: Map<String, Any?>? = null
)
