package org.example.route.controller

import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.tags.Tag
import org.example.route.sse.SseProgressEvent
import org.example.route.sse.SseTaskEventBus
import org.slf4j.LoggerFactory
import org.springframework.http.MediaType
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter

/**
 * SSE 分析进度流控制器
 *
 * 提供 GET /api/v1/route-analysis/tasks/{taskId}/stream 端点。
 * Admin 前端提交任务后订阅此端点，实时接收任务进度事件，替代原有轮询方案。
 *
 * 超时设置为 300 秒（5 分钟），分析任务最长预计 60 秒。
 */
@RestController
@RequestMapping("/api/v1/route-analysis")
@Tag(name = "KML分析服务", description = "通过后端代理调用 KML Agent Service 进行路线分析")
class SseAnalysisController(
    private val sseTaskEventBus: SseTaskEventBus
) {
    private val logger = LoggerFactory.getLogger(SseAnalysisController::class.java)

    companion object {
        const val SSE_TIMEOUT_MS = 300_000L  // 5 分钟
    }

    /**
     * 订阅 KML 分析任务进度 SSE 流
     *
     * Admin 前端提交任务后调用此接口建立长连接，接收以下事件：
     * - { status: "processing", progress: 10, current_step: "..." }  任务提交成功
     * - { status: "completed", progress: 100, route_id: "...", current_step: "分析完成" } 落库完成
     * - { status: "failed", error: "..." }  任务失败
     *
     * taskId 不存在时立即推送 { status: "failed", error: "任务不存在" } 并关闭连接。
     */
    @GetMapping(
        "/tasks/{taskId}/stream",
        produces = [MediaType.TEXT_EVENT_STREAM_VALUE]
    )
    @Operation(
        summary = "订阅 KML 分析任务 SSE 进度流",
        description = "Admin 提交分析任务后调用此接口，实时接收任务进度事件（替代轮询）"
    )
    fun streamTaskProgress(@PathVariable taskId: String): SseEmitter {
        logger.info("新建 SSE 连接，taskId=$taskId")

        val emitter = SseEmitter(SSE_TIMEOUT_MS)

        // 只有提交成功后登记过的任务才能注册；格式合法但未知的 taskId 也必须立即失败。
        if (!sseTaskEventBus.register(taskId, emitter)) {
            try {
                emitter.send(
                    SseEmitter.event()
                        .name("progress")
                        .data(
                            SseProgressEvent(
                                taskId = taskId,
                                status = "failed",
                                error = "任务不存在"
                            )
                        )
                )
                emitter.complete()
            } catch (e: Exception) {
                logger.warn("发送 taskId 不存在事件失败: ${e.message}")
                emitter.completeWithError(e)
            }
        }

        return emitter
    }
}
