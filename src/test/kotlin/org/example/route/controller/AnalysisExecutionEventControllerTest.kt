package org.example.route.controller

import org.example.route.dto.AnalysisExecutionEvent
import org.example.route.dto.AnalysisExecutionEventRequest
import org.example.route.sse.SseProgressEvent
import org.example.route.sse.SseTaskEventBus
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import org.mockito.kotlin.argumentCaptor
import org.mockito.kotlin.eq
import org.mockito.kotlin.mock
import org.mockito.kotlin.never
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever

class AnalysisExecutionEventControllerTest {
    @Test
    fun `ingestion publishes structured execution event to matching known task`() {
        val eventBus = mock<SseTaskEventBus>()
        whenever(eventBus.isKnownTask("task-1")).thenReturn(true)
        whenever(eventBus.publish(eq("task-1"), org.mockito.kotlin.any())).thenReturn(true)
        val controller = AnalysisExecutionEventController(eventBus)
        val event = executionEvent()

        val response = controller.ingestExecutionEvent(
            "task-1",
            AnalysisExecutionEventRequest(taskId = "task-1", executionEvent = event)
        )

        assertEquals(200, response.statusCode.value())
        val captor = argumentCaptor<SseProgressEvent>()
        verify(eventBus).publish(eq("task-1"), captor.capture())
        assertEquals(event, captor.firstValue.executionEvent)
        assertEquals(65, captor.firstValue.progress)
        assertEquals("processing", captor.firstValue.status)
    }

    @Test
    fun `ingestion returns 404 and does not publish for unknown task`() {
        val eventBus = mock<SseTaskEventBus>()
        whenever(eventBus.isKnownTask("task-missing")).thenReturn(false)
        val controller = AnalysisExecutionEventController(eventBus)

        val response = controller.ingestExecutionEvent(
            "task-missing",
            AnalysisExecutionEventRequest(taskId = "task-missing", executionEvent = executionEvent())
        )

        assertEquals(404, response.statusCode.value())
        verify(eventBus, never()).publish(eq("task-missing"), org.mockito.kotlin.any())
    }

    private fun executionEvent() = AnalysisExecutionEvent(
        timestamp = "2026-08-25T10:00:00Z",
        node = "content_agent",
        phase = "degraded",
        level = "warning",
        message = "内容生成已降级",
        progress = 65,
        details = mapOf("error_category" to "timeout")
    )
}
