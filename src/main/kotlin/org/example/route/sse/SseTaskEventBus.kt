package org.example.route.sse

import com.fasterxml.jackson.databind.ObjectMapper
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Service
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter
import java.time.Clock
import java.time.Duration
import java.time.Instant
import java.util.ArrayDeque
import java.util.concurrent.ConcurrentHashMap

/**
 * 内存 SSE 任务事件总线。
 *
 * 只有提交成功后显式注册的任务可以接收事件。活动任务事件使用有界缓冲，订阅建立后按接收顺序回放；
 * 无连接时到达的终态会保留到首次订阅，终态发送后清理该任务的全部内存状态。
 */
@Service
class SseTaskEventBus internal constructor(
    private val objectMapper: ObjectMapper,
    private val clock: Clock
) {
    @Autowired
    constructor(objectMapper: ObjectMapper) : this(objectMapper, Clock.systemUTC())
    private val logger = LoggerFactory.getLogger(SseTaskEventBus::class.java)
    private val emitters = ConcurrentHashMap<String, SseEmitter>()
    private val tasks = ConcurrentHashMap<String, TaskState>()

    @Synchronized
    fun registerTask(taskId: String): Boolean {
        cleanupExpiredTasks()
        tasks[taskId]?.let {
            it.updatedAt = clock.instant()
            return true
        }
        cleanupTerminalTasksForCapacity()
        if (tasks.size >= MAX_TRACKED_TASKS) {
            logger.warn("SSE 任务容量已满，拒绝注册新任务，taskId=$taskId")
            return false
        }
        tasks[taskId] = TaskState(updatedAt = clock.instant())
        return true
    }

    @Synchronized
    fun register(taskId: String, emitter: SseEmitter): Boolean {
        cleanupExpiredTasks()
        val task = tasks[taskId] ?: return false
        task.updatedAt = clock.instant()

        emitters[taskId]?.let { old ->
            try {
                old.complete()
            } catch (_: Exception) {
                // 旧连接关闭失败不影响新连接注册。
            }
        }

        emitters[taskId] = emitter
        configureCallbacks(taskId, emitter)

        try {
            task.events.forEach { send(emitter, it) }
        } catch (e: Exception) {
            logger.warn("SSE 缓冲事件回放失败，保留缓冲等待重连，taskId=$taskId: ${e.message}")
            emitters.remove(taskId, emitter)
            return true
        }
        if (task.terminal) {
            removeTask(taskId)
            try {
                emitter.complete()
            } catch (_: Exception) {
                // 终态已成功回放并清理，连接关闭异常无需恢复任务。
            }
        }

        logger.info("SSE emitter 已注册，taskId=$taskId，当前连接数=${emitters.size}")
        return true
    }

    @Synchronized
    fun publish(taskId: String, event: SseProgressEvent): Boolean {
        cleanupExpiredTasks()
        val task = tasks[taskId] ?: return false
        task.updatedAt = clock.instant()
        buffer(task, event)

        val emitter = emitters[taskId]
        if (event.isTerminal()) {
            task.terminal = true
            if (emitter == null) {
                return true
            }
            try {
                send(emitter, event)
            } catch (e: Exception) {
                logger.warn("SSE 终态事件发送失败，保留缓冲等待重连，taskId=$taskId: ${e.message}")
                emitters.remove(taskId, emitter)
                return true
            }
            removeTask(taskId)
            try {
                emitter.complete()
            } catch (_: Exception) {
                // 终态已成功发送并清理，连接关闭异常无需恢复任务。
            }
            return true
        }

        if (emitter != null) {
            try {
                send(emitter, event)
            } catch (e: Exception) {
                logger.warn("SSE 事件发送失败，taskId=$taskId: ${e.message}")
                emitters.remove(taskId, emitter)
            }
        }
        return true
    }

    fun hasEmitter(taskId: String): Boolean = emitters.containsKey(taskId)

    @Synchronized
    fun isKnownTask(taskId: String): Boolean {
        cleanupExpiredTasks()
        return tasks.containsKey(taskId)
    }

    @Synchronized
    internal fun cleanupExpiredTasks() {
        val cutoff = clock.instant().minus(TASK_TTL)
        tasks.entries
            .filter { (_, state) -> state.terminal && state.updatedAt.isBefore(cutoff) }
            .map { it.key }
            .forEach(::removeTask)
    }

    private fun buffer(task: TaskState, event: SseProgressEvent) {
        if (task.events.size == MAX_BUFFERED_EVENTS) {
            task.events.removeFirst()
        }
        task.events.addLast(event)
    }

    private fun cleanupTerminalTasksForCapacity() {
        while (tasks.size >= MAX_TRACKED_TASKS) {
            val oldestTerminalTaskId = tasks.entries
                .filter { (_, state) -> state.terminal }
                .minByOrNull { it.value.updatedAt }
                ?.key
                ?: return
            removeTask(oldestTerminalTaskId)
        }
    }

    private fun removeTask(taskId: String) {
        emitters.remove(taskId)
        tasks.remove(taskId)
    }

    private fun send(emitter: SseEmitter, event: SseProgressEvent) {
        val json = objectMapper.writeValueAsString(event)
        emitter.send(SseEmitter.event().name("progress").data(json))
    }

    private fun configureCallbacks(taskId: String, emitter: SseEmitter) {
        emitter.onTimeout { emitters.remove(taskId, emitter) }
        emitter.onCompletion { emitters.remove(taskId, emitter) }
        emitter.onError { emitters.remove(taskId, emitter) }
    }

    private fun SseProgressEvent.isTerminal(): Boolean = status == "completed" || status == "failed"

    private data class TaskState(
        val events: ArrayDeque<SseProgressEvent> = ArrayDeque(),
        var terminal: Boolean = false,
        var updatedAt: Instant
    )

    companion object {
        internal const val MAX_BUFFERED_EVENTS = 100
        internal const val MAX_TRACKED_TASKS = 1_000
        internal val TASK_TTL: Duration = Duration.ofHours(1)
    }
}
