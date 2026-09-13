package org.example.route.service

import com.fasterxml.jackson.databind.ObjectMapper
import org.example.route.dto.CallbackPoiPointDto
import org.example.route.dto.KmlAnalysisCallbackRequest
import org.example.route.dto.PoiResolveAgentResponse
import org.example.route.dto.PoiResolveAgentResultItem
import org.example.route.model.PoiLibraryItem
import org.example.route.model.PoiPoint
import org.example.route.model.Route
import org.example.route.repository.PoiLibraryRepository
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
import org.mockito.kotlin.any
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
            mock<PoiLibraryRepository>(),
            mock<KmlAnalysisClientService>(),
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
            mock<PoiLibraryRepository>(),
            mock<KmlAnalysisClientService>(),
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

    @Test
    fun `completed callback persists matched library id and confirms poi`() {
        val routeRepository = mock<RouteRepository>()
        val poiPointRepository = mock<PoiPointRepository>()
        val poiLibraryRepository = mock<PoiLibraryRepository>()
        val client = mock<KmlAnalysisClientService>()
        whenever(routeRepository.findById("route-1")).thenReturn(
            Optional.of(Route(
                id = "route-1",
                name = "测试路线",
                region = "五台山",
                status = 3,
                createdBy = "user-1"
            ))
        )
        whenever(poiLibraryRepository.findByStatus("active")).thenReturn(
            listOf(
                PoiLibraryItem(
                    id = "library-1",
                    name = "清凉寺",
                    latitude = 39.0,
                    longitude = 113.0,
                    category = "photo",
                    regionName = "五台山"
                )
            )
        )
        whenever(client.resolvePoiMatches(any())).thenReturn(
            PoiResolveAgentResponse(
                total = 1,
                matchedCount = 1,
                results = listOf(PoiResolveAgentResultItem(index = 0, libraryId = "library-1"))
            )
        )
        val service = KmlAnalysisCallbackService(
            routeRepository,
            mock<SegmentRepository>(),
            mock<SegmentSchemeRepository>(),
            poiPointRepository,
            poiLibraryRepository,
            client,
            mock<RouteMapDataRepository>(),
            mock<WaypointRepository>(),
            mock<SseTaskEventBus>(),
            ObjectMapper()
        )
        val request = KmlAnalysisCallbackRequest(
            routeId = "route-1",
            taskId = "task-1",
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
            poiPoints = listOf(
                CallbackPoiPointDto(
                    name = "清凉寺观景点",
                    latitude = 39.0001,
                    longitude = 113.0001,
                    category = "photo"
                )
            )
        )

        service.handleCallback(request)

        val poiCaptor = argumentCaptor<PoiPoint>()
        verify(poiPointRepository).save(poiCaptor.capture())
        assertEquals("library-1", poiCaptor.firstValue.matchedLibraryId)
        assertEquals("confirmed", poiCaptor.firstValue.status)
    }
}
