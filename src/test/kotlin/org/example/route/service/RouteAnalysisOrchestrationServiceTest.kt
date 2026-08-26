package org.example.route.service

import org.example.route.dto.KmlAnalysisSubmitRequest
import org.example.route.dto.TaskSubmitResponse
import org.example.route.sse.SseProgressEvent
import org.example.route.sse.SseTaskEventBus
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import org.mockito.kotlin.argumentCaptor
import org.mockito.kotlin.eq
import org.mockito.kotlin.inOrder
import org.mockito.kotlin.mock
import org.mockito.kotlin.whenever
import reactor.core.publisher.Mono

class RouteAnalysisOrchestrationServiceTest {
    @Test
    fun `successful agent submission registers task before publishing processing`() {
        val routeBindingService = mock<RouteBindingService>()
        val client = mock<KmlAnalysisClientService>()
        val eventBus = mock<SseTaskEventBus>()
        val request = KmlAnalysisSubmitRequest(kmlSource = "https://example.com/route.kml")
        whenever(routeBindingService.resolveRouteId(request)).thenReturn("route-1")
        whenever(client.submitAnalysis(request.copy(routeId = "route-1"))).thenReturn(
            Mono.just(TaskSubmitResponse("task-1", "processing", "submitted", 30))
        )
        val service = RouteAnalysisOrchestrationService(routeBindingService, client, eventBus)

        val response = service.submitAnalysisWithRouteBinding(request).block()!!

        assertEquals("task-1", response.taskId)
        val eventCaptor = argumentCaptor<SseProgressEvent>()
        inOrder(eventBus) {
            verify(eventBus).registerTask("task-1")
            verify(eventBus).publish(eq("task-1"), eventCaptor.capture())
        }
        assertEquals("processing", eventCaptor.firstValue.status)
        assertEquals(10, eventCaptor.firstValue.progress)
    }
}
