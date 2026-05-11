package org.example.route.sse

import com.fasterxml.jackson.databind.ObjectMapper
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter
import java.util.concurrent.ConcurrentHashMap

/**
 * SSE 任务事件总线
 *
 * 内存单例，以 taskId 为 key 维护 SSE 连接。
 * - register(taskId, emitter): 注册新连接，设置超时/完成/错误时自动移除
 * - publish(taskId, event): 向对应连接推送 JSON 事件
 *
 * 注意：仅支持同一 taskId 的单连接（后注册的覆盖前一个，前一个发送 complete）。
 * 服务重启后历史事件丢失，EventSource 会自动重连，前端降级展示"分析中，请稍候"。
 */
@Service
class SseTaskEventBus(
    private val objectMapper: ObjectMapper
) {
    private val logger = LoggerFactory.getLogger(SseTaskEventBus::class.java)
    private val emitters = ConcurrentHashMap<String, SseEmitter>()

    /**
     * 注册 SSE Emitter
     *
     * 若已存在同 taskId 的连接，先关闭旧连接再注册新的。
     */
    fun register(taskId: String, emitter: SseEmitter) {
        // 若已有旧连接，先安静地 complete 它
        emitters[taskId]?.let { old ->
            try { old.complete() } catch (e: Exception) { /* 忽略 */ }
        }

        emitters[taskId] = emitter

        // 超时/完成/错误时自动从 map 中移除
        emitter.onTimeout {
            logger.debug("SSE emitter 超时，移除 taskId=$taskId")
            emitters.remove(taskId, emitter)
        }
        emitter.onCompletion {
            logger.debug("SSE emitter 完成，移除 taskId=$taskId")
            emitters.remove(taskId, emitter)
        }
        emitter.onError { ex ->
            logger.debug("SSE emitter 错误，移除 taskId=$taskId: ${ex.message}")
            emitters.remove(taskId, emitter)
        }

        logger.info("SSE emitter 已注册，taskId=$taskId，当前连接数=${emitters.size}")
    }

    /**
     * 向指定 taskId 的 SSE 连接发布事件
     *
     * 若连接不存在（未订阅或已断开）则静默忽略。
     * 发送完成事件（status=completed/failed）后自动关闭连接。
     */
    fun publish(taskId: String, event: SseProgressEvent) {
        val emitter = emitters[taskId] ?: run {
            logger.debug("publish 时 taskId=$taskId 无活跃 emitter，忽略事件 status=${event.status}")
            return
        }

        try {
            val json = objectMapper.writeValueAsString(event)
            emitter.send(
                SseEmitter.event()
                    .name("progress")
                    .data(json)
            )
            logger.info("SSE 事件已发布，taskId=$taskId, status=${event.status}, progress=${event.progress}")

            // 终态事件（completed/failed）发送后自动关闭连接
            if (event.status == "completed" || event.status == "failed") {
                try {
                    emitter.complete()
                } catch (e: Exception) {
                    logger.debug("关闭 SSE emitter 时忽略异常: ${e.message}")
                }
                emitters.remove(taskId, emitter)
            }
        } catch (e: Exception) {
            logger.warn("SSE 事件发送失败，taskId=$taskId: ${e.message}")
            emitters.remove(taskId, emitter)
        }
    }

    /**
     * 检查指定 taskId 是否有活跃的 SSE 连接
     */
    fun hasEmitter(taskId: String): Boolean = emitters.containsKey(taskId)
}
