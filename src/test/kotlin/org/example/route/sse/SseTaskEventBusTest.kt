package org.example.route.sse

import com.fasterxml.jackson.databind.ObjectMapper
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.mockito.kotlin.any
import org.mockito.kotlin.doThrow
import org.mockito.kotlin.mock
import org.mockito.kotlin.never
import org.mockito.kotlin.times
import org.mockito.kotlin.verify
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter
import java.time.Clock
import java.time.Instant
import java.time.ZoneOffset

class SseTaskEventBusTest {
    @Test
    fun `unknown task publication is rejected and does not create a replay buffer`() {
        val eventBus = SseTaskEventBus(ObjectMapper())
        val taskId = "task-unknown"

        assertFalse(eventBus.publish(taskId, event(taskId, 10)))
        val emitter = mock<SseEmitter>()
        assertFalse(eventBus.register(taskId, emitter))
        verify(emitter, never()).send(any<SseEmitter.SseEventBuilder>())
    }

    @Test
    fun `events published before subscription are replayed for known task`() {
        val eventBus = SseTaskEventBus(ObjectMapper())
        val taskId = "task-replay"
        eventBus.registerTask(taskId)
        eventBus.publish(taskId, event(taskId, 10))
        eventBus.publish(taskId, event(taskId, 20))
        val emitter = mock<SseEmitter>()

        assertTrue(eventBus.register(taskId, emitter))

        verify(emitter, times(2)).send(any<SseEmitter.SseEventBuilder>())
    }

    @Test
    fun `active task replay buffer is bounded`() {
        val eventBus = SseTaskEventBus(ObjectMapper())
        val taskId = "task-bounded"
        eventBus.registerTask(taskId)
        repeat(101) { eventBus.publish(taskId, event(taskId, it)) }
        val emitter = mock<SseEmitter>()

        eventBus.register(taskId, emitter)

        verify(emitter, times(100)).send(any<SseEmitter.SseEventBuilder>())
    }

    @Test
    fun `terminal event without emitter is retained replayed and then cleaned`() {
        val eventBus = SseTaskEventBus(ObjectMapper())
        val taskId = "task-terminal"
        eventBus.registerTask(taskId)
        eventBus.publish(taskId, event(taskId, 20))
        assertTrue(eventBus.publish(taskId, event(taskId, 100, status = "completed")))
        assertTrue(eventBus.isKnownTask(taskId))
        val emitter = mock<SseEmitter>()

        assertTrue(eventBus.register(taskId, emitter))

        verify(emitter, times(2)).send(any<SseEmitter.SseEventBuilder>())
        verify(emitter).complete()
        assertFalse(eventBus.isKnownTask(taskId))
    }

    @Test
    fun `terminal event sent to online emitter cleans tracked task`() {
        val eventBus = SseTaskEventBus(ObjectMapper())
        val taskId = "task-online-terminal"
        eventBus.registerTask(taskId)
        val emitter = mock<SseEmitter>()
        eventBus.register(taskId, emitter)

        assertTrue(eventBus.publish(taskId, event(taskId, 100, status = "failed")))

        verify(emitter).send(any<SseEmitter.SseEventBuilder>())
        verify(emitter).complete()
        assertFalse(eventBus.isKnownTask(taskId))
    }

    @Test
    fun `terminal send failure removes emitter but retains task and buffer for reconnect`() {
        val eventBus = SseTaskEventBus(ObjectMapper())
        val taskId = "task-terminal-reconnect"
        eventBus.registerTask(taskId)
        val brokenEmitter = mock<SseEmitter> {
            on { send(any<SseEmitter.SseEventBuilder>()) } doThrow IllegalStateException("closed")
        }
        eventBus.register(taskId, brokenEmitter)

        assertTrue(eventBus.publish(taskId, event(taskId, 100, status = "completed")))
        assertFalse(eventBus.hasEmitter(taskId))
        assertTrue(eventBus.isKnownTask(taskId))

        val reconnectingEmitter = mock<SseEmitter>()
        assertTrue(eventBus.register(taskId, reconnectingEmitter))
        verify(reconnectingEmitter).send(any<SseEmitter.SseEventBuilder>())
        verify(reconnectingEmitter).complete()
        assertFalse(eventBus.isKnownTask(taskId))
    }

    @Test
    fun `ttl cleanup retains active task so later callback can publish`() {
        val base = Instant.parse("2026-08-26T00:00:00Z")
        val clock = MutableClock(base)
        val eventBus = SseTaskEventBus(ObjectMapper(), clock)
        val taskId = "task-active"
        eventBus.registerTask(taskId)

        clock.instantValue = base.plus(SseTaskEventBus.TASK_TTL).plusSeconds(1)
        eventBus.cleanupExpiredTasks()

        assertTrue(eventBus.isKnownTask(taskId))
        assertTrue(eventBus.publish(taskId, event(taskId, 100, status = "completed")))
    }

    @Test
    fun `ttl cleanup removes expired terminal task`() {
        val base = Instant.parse("2026-08-26T00:00:00Z")
        val clock = MutableClock(base)
        val eventBus = SseTaskEventBus(ObjectMapper(), clock)
        val taskId = "task-expired-terminal"
        eventBus.registerTask(taskId)
        eventBus.publish(taskId, event(taskId, 100, status = "completed"))

        clock.instantValue = base.plus(SseTaskEventBus.TASK_TTL).plusSeconds(1)
        eventBus.cleanupExpiredTasks()

        assertFalse(eventBus.isKnownTask(taskId))
    }

    @Test
    fun `capacity limit rejects newest task instead of evicting active tasks`() {
        val base = Instant.parse("2026-08-26T00:00:00Z")
        val clock = MutableClock(base)
        val eventBus = SseTaskEventBus(ObjectMapper(), clock)

        repeat(SseTaskEventBus.MAX_TRACKED_TASKS) { index ->
            clock.instantValue = base.plusSeconds(index.toLong())
            assertTrue(eventBus.registerTask("task-$index"))
        }
        val rejectedTaskId = "task-${SseTaskEventBus.MAX_TRACKED_TASKS}"

        assertFalse(eventBus.registerTask(rejectedTaskId))
        assertTrue(eventBus.isKnownTask("task-0"))
        assertTrue(eventBus.publish("task-0", event("task-0", 50)))
        assertFalse(eventBus.isKnownTask(rejectedTaskId))
    }

    private fun event(taskId: String, progress: Int, status: String = "processing") =
        SseProgressEvent(taskId = taskId, status = status, progress = progress)

    private class MutableClock(var instantValue: Instant) : Clock() {
        override fun instant(): Instant = instantValue
        override fun getZone() = ZoneOffset.UTC
        override fun withZone(zone: java.time.ZoneId): Clock = this
    }
}
