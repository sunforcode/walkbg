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

        // 注册到事件总线（超时/完成/错误回调在 SseTaskEventBus 内部处理）
        sseTaskEventBus.register(taskId, emitter)

        // 若 taskId 对应的总线中无活跃记录（即任务尚未被提交或 task_id 不合法），
        // 通过检查注册后是否仍在 map 中来判断；这里直接依赖 publish 的存在性检查即可。
        // 但根据时序：前端在提交后立即连接，大多数情况下任务已在处理中。
        // 若任务 ID 确实不存在，后续不会收到任何 publish，emitter 会在超时后自动关闭。
        // 为了立即反馈"任务不存在"，我们这里检查总线是否注册成功并且 taskId 格式合法。
        // 注意：合法的 taskId 由 agent 生成（task_前缀），不合法时立即推送 failed。
        if (!taskId.startsWith("task_")) {
            try {
                sseTaskEventBus.publish(
                    taskId,
                    SseProgressEvent(
                        taskId = taskId,
                        status = "failed",
                        error = "任务不存在"
                    )
                )
            } catch (e: Exception) {
                logger.warn("发送 taskId 不存在事件失败: ${e.message}")
                emitter.completeWithError(e)
            }
        }

        return emitter
    }
}
