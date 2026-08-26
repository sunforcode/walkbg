package org.example.route.sse

import com.fasterxml.jackson.annotation.JsonProperty
import org.example.route.dto.AnalysisExecutionEvent

/**
 * SSE 进度事件 data class
 *
 * 由 SseTaskEventBus 发布，携带 KML 分析任务的状态信息。
 * 字段与前端约定的 JSON 格式一致（snake_case）。
 */
data class SseProgressEvent(
    @JsonProperty("task_id")
    val taskId: String,

    /** pending | processing | completed | failed */
    val status: String,

    /** 进度百分比 0-100 */
    val progress: Int = 0,

    @JsonProperty("current_step")
    val currentStep: String? = null,

    /** 完成事件携带 routeId，供前端跳转 */
    @JsonProperty("route_id")
    val routeId: String? = null,

    /** 失败事件携带错误信息 */
    val error: String? = null,

    @JsonProperty("execution_event")
    val executionEvent: AnalysisExecutionEvent? = null,

    /** 完成事件是否包含 fallback 降级 */
    val degraded: Boolean? = null
)
