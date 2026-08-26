package org.example.route.service

import com.fasterxml.jackson.databind.ObjectMapper
import org.example.route.dto.KmlAnalysisCallbackRequest
import org.example.route.model.Route
import org.example.route.repository.PoiPointRepository
import org.example.route.repository.RouteMapDataRepository
import org.example.route.repository.RouteRepository
import org.example.route.repository.SegmentRepository
import org.example.route.repository.SegmentSchemeRepository
import org.example.route.repository.WaypointRepository
import org.example.route.sse.SseProgressEvent
import org.example.route.sse.SseTaskEventBus
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.mockito.kotlin.argumentCaptor
import org.mockito.kotlin.eq
import org.mockito.kotlin.mock
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever
import java.util.Optional

class KmlAnalysisCallbackServiceDegradedTest {
    @Test
    fun `completed callback propagates degraded status to SSE`() {
        val routeRepository = mock<RouteRepository>()
        val eventBus = mock<SseTaskEventBus>()
        whenever(routeRepository.findById("route-1")).thenReturn(Optional.of(Route(
            id = "route-1",
            name = "测试路线",
            status = 3,
            createdBy = "user-1"
        )))
        val service = KmlAnalysisCallbackService(
            routeRepository,
            mock<SegmentRepository>(),
            mock<SegmentSchemeRepository>(),
            mock<PoiPointRepository>(),
            mock<RouteMapDataRepository>(),
            mock<WaypointRepository>(),
            eventBus,
            ObjectMapper()
        )
        val request = KmlAnalysisCallbackRequest(
            routeId = "route-1",
            taskId = "task-1",
            status = "completed",
            sourceKmlUrl = null,
            analysisTimestamp = null,
            qualityScore = null,
            totalDistanceKm = null,
            totalElevationGainM = null,
            totalElevationLossM = null,
            maxElevation = null,
            minElevation = null,
            isLoop = null,
            estimatedDifficulty = null,
            generatedDescription = null,
            degraded = true
        )

        service.handleCallback(request)

        val captor = argumentCaptor<SseProgressEvent>()
        verify(eventBus).publish(eq("task-1"), captor.capture())
        assertTrue(captor.firstValue.degraded == true)
    }

    @Test
    fun `failed callback publishes callback error and falls back when absent`() {
        val routeRepository = mock<RouteRepository>()
        val eventBus = mock<SseTaskEventBus>()
        whenever(routeRepository.findById("route-1")).thenReturn(Optional.of(Route(
            id = "route-1",
            name = "测试路线",
            status = 3,
            createdBy = "user-1"
        )))
        val service = KmlAnalysisCallbackService(
            routeRepository,
            mock<SegmentRepository>(),
            mock<SegmentSchemeRepository>(),
            mock<PoiPointRepository>(),
            mock<RouteMapDataRepository>(),
            mock<WaypointRepository>(),
            eventBus,
            ObjectMapper()
        )
        val request = KmlAnalysisCallbackRequest(
            routeId = "route-1",
            taskId = "task-1",
            status = "failed",
            error = null,
            sourceKmlUrl = null,
            analysisTimestamp = null,
            qualityScore = null,
            totalDistanceKm = null,
            totalElevationGainM = null,
            totalElevationLossM = null,
            maxElevation = null,
            minElevation = null,
            isLoop = null,
            estimatedDifficulty = null,
            generatedDescription = null
        )

        service.handleCallback(request)

        val captor = argumentCaptor<SseProgressEvent>()
        verify(eventBus).publish(eq("task-1"), captor.capture())
        assertEquals("分析失败", captor.firstValue.error)
    }
}
