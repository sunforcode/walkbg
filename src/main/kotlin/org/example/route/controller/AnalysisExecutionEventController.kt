package org.example.route.controller

import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.tags.Tag
import org.example.common.dto.ApiResponse
import org.example.common.util.ResponseUtil
import org.example.route.dto.AnalysisExecutionEventRequest
import org.example.route.sse.SseProgressEvent
import org.example.route.sse.SseTaskEventBus
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("/api/v1/route-analysis")
@Tag(name = "KML分析事件", description = "接收 KML Agent Service 的结构化执行事件")
class AnalysisExecutionEventController(
    private val sseTaskEventBus: SseTaskEventBus
) {
    @PostMapping("/tasks/{taskId}/events")
    @Operation(summary = "接收 Agent 结构化执行事件")
    fun ingestExecutionEvent(
        @PathVariable taskId: String,
        @RequestBody request: AnalysisExecutionEventRequest
    ): ResponseEntity<ApiResponse<Any>> {
        require(request.taskId == taskId) { "taskId 与请求体不一致" }
        require(request.executionEvent.progress in 0..100) { "progress 必须在 0 到 100 之间" }
        if (!sseTaskEventBus.isKnownTask(taskId)) {
            return ResponseUtil.notFound("分析任务不存在: $taskId")
        }

        sseTaskEventBus.publish(
            taskId,
            SseProgressEvent(
                taskId = taskId,
                status = "processing",
                progress = request.executionEvent.progress,
                currentStep = request.executionEvent.message,
                executionEvent = request.executionEvent
            )
        )
        return ResponseUtil.success(mapOf("accepted" to true))
    }
}
