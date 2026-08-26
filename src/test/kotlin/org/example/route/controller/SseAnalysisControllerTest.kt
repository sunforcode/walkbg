package org.example.route.controller

import com.fasterxml.jackson.databind.ObjectMapper
import org.example.route.sse.SseProgressEvent
import org.example.route.sse.SseTaskEventBus
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.mockito.kotlin.any
import org.mockito.kotlin.mock
import org.mockito.kotlin.whenever
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.asyncDispatch
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.request
import org.springframework.test.web.servlet.setup.MockMvcBuilders
import java.nio.charset.StandardCharsets

class SseAnalysisControllerTest {
    private val eventBus = mock<SseTaskEventBus>()
    private val mockMvc: MockMvc = MockMvcBuilders
        .standaloneSetup(SseAnalysisController(eventBus))
        .build()

    @Test
    fun `known task emits Chinese event data as UTF-8 bytes`() {
        val realEventBus = SseTaskEventBus(ObjectMapper())
        val taskId = "task-chinese"
        realEventBus.registerTask(taskId)
        realEventBus.publish(
            taskId,
            SseProgressEvent(
                taskId = taskId,
                status = "completed",
                progress = 100,
                currentStep = "路线分析完成"
            )
        )
        val utf8MockMvc = MockMvcBuilders
            .standaloneSetup(SseAnalysisController(realEventBus))
            .build()

        val result = utf8MockMvc.perform(get("/api/v1/route-analysis/tasks/$taskId/stream"))
            .andExpect(request().asyncStarted())
            .andReturn()

        val response = utf8MockMvc.perform(asyncDispatch(result)).andReturn().response
        val body = String(response.contentAsByteArray, StandardCharsets.UTF_8)
        assertTrue(body.contains("路线分析完成"), body)
    }

    @Test
    fun `well formed unknown task immediately emits failed event and completes`() {
        whenever(eventBus.register(any(), any())).thenReturn(false)

        val result = mockMvc.perform(get("/api/v1/route-analysis/tasks/task_unknown/stream"))
            .andExpect(request().asyncStarted())
            .andReturn()

        val response = mockMvc.perform(asyncDispatch(result)).andReturn().response
        val body = response.getContentAsString(StandardCharsets.UTF_8)
        assertTrue(body.contains("event:progress"))
        assertTrue(body.contains("\"status\":\"failed\""))
        assertTrue(body.contains("\"error\":\"任务不存在\""))
    }
}
