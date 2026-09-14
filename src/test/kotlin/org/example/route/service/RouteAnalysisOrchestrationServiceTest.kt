package org.example.route.service

import org.example.route.dto.KmlAnalysisSubmitRequest
import org.example.route.dto.TaskSubmitResponse
import org.example.route.repository.RouteMapDataRepository
import org.example.route.sse.SseProgressEvent
import org.example.route.sse.SseTaskEventBus
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.mockito.kotlin.any
import org.mockito.kotlin.argumentCaptor
import org.mockito.kotlin.eq
import org.mockito.kotlin.inOrder
import org.mockito.kotlin.mock
import org.mockito.kotlin.never
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever
import reactor.core.publisher.Mono
import java.util.Optional

class RouteAnalysisOrchestrationServiceTest {
    private fun buildService(
        routeBindingService: RouteBindingService,
        client: KmlAnalysisClientService,
        routeMapDataRepository: RouteMapDataRepository = mock(),
        kmlStorageService: KmlStorageService = mock(),
        eventBus: SseTaskEventBus = mock(),
        trackKmlFactory: RouteTrackKmlFactory = mock()
    ): RouteAnalysisOrchestrationService =
        RouteAnalysisOrchestrationService(
            routeBindingService, client, routeMapDataRepository, kmlStorageService, eventBus, trackKmlFactory
        )

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
        val service = buildService(routeBindingService, client, eventBus = eventBus)

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

    @Test
    fun `reanalysis fills kml content from stored relative kml url`() {
        val routeBindingService = mock<RouteBindingService>()
        val client = mock<KmlAnalysisClientService>()
        val routeMapDataRepository = mock<RouteMapDataRepository>()
        val kmlStorageService = mock<KmlStorageService>()
        val request = KmlAnalysisSubmitRequest(routeId = "route-1")
        whenever(routeBindingService.resolveRouteId(request)).thenReturn("route-1")
        whenever(routeMapDataRepository.findById("route-1"))
            .thenReturn(Optional.of(org.example.route.model.RouteMapData(id = "route-1", kmlUrl = "/static/kml-upload/abc.kml")))
        whenever(kmlStorageService.readStoredContent("/static/kml-upload/abc.kml")).thenReturn("<kml></kml>")
        whenever(client.submitAnalysis(any())).thenReturn(
            Mono.just(TaskSubmitResponse("task-2", "processing", "submitted", 30))
        )
        val service = buildService(routeBindingService, client, routeMapDataRepository, kmlStorageService)

        service.submitAnalysisWithRouteBinding(request).block()!!

        val captor = argumentCaptor<KmlAnalysisSubmitRequest>()
        verify(client).submitAnalysis(captor.capture())
        assertEquals("route-1", captor.firstValue.routeId)
        assertEquals("<kml></kml>", captor.firstValue.kmlContent)
        assertEquals("/static/kml-upload/abc.kml", captor.firstValue.kmlSource)
    }

    @Test
    fun `reanalysis with absolute stored kml url passes it as kml source`() {
        val routeBindingService = mock<RouteBindingService>()
        val client = mock<KmlAnalysisClientService>()
        val routeMapDataRepository = mock<RouteMapDataRepository>()
        val request = KmlAnalysisSubmitRequest(routeId = "route-1")
        whenever(routeBindingService.resolveRouteId(request)).thenReturn("route-1")
        whenever(routeMapDataRepository.findById("route-1"))
            .thenReturn(Optional.of(org.example.route.model.RouteMapData(id = "route-1", kmlUrl = "https://example.com/old.kml")))
        whenever(client.submitAnalysis(any())).thenReturn(
            Mono.just(TaskSubmitResponse("task-3", "processing", "submitted", 30))
        )
        val service = buildService(routeBindingService, client, routeMapDataRepository)

        service.submitAnalysisWithRouteBinding(request).block()!!

        val captor = argumentCaptor<KmlAnalysisSubmitRequest>()
        verify(client).submitAnalysis(captor.capture())
        assertEquals("https://example.com/old.kml", captor.firstValue.kmlSource)
    }

    @Test
    fun `reanalysis falls back to synthesized kml from stored track points`() {
        val routeBindingService = mock<RouteBindingService>()
        val client = mock<KmlAnalysisClientService>()
        val routeMapDataRepository = mock<RouteMapDataRepository>()
        val trackKmlFactory = mock<RouteTrackKmlFactory>()
        val request = KmlAnalysisSubmitRequest(routeId = "route-1")
        whenever(routeBindingService.resolveRouteId(request)).thenReturn("route-1")
        whenever(routeMapDataRepository.findById("route-1")).thenReturn(Optional.empty())
        whenever(trackKmlFactory.synthesizeCurrentTrackKml("route-1")).thenReturn("<kml><LineString/></kml>")
        whenever(client.submitAnalysis(any())).thenReturn(
            Mono.just(TaskSubmitResponse("task-4", "processing", "submitted", 30))
        )
        val service = buildService(
            routeBindingService, client, routeMapDataRepository, trackKmlFactory = trackKmlFactory
        )

        service.submitAnalysisWithRouteBinding(request).block()!!

        val captor = argumentCaptor<KmlAnalysisSubmitRequest>()
        verify(client).submitAnalysis(captor.capture())
        assertEquals("route-1", captor.firstValue.routeId)
        assertEquals("<kml><LineString/></kml>", captor.firstValue.kmlContent)
        // Agent 合同要求 kml_source 必填，回退时携带标识来源的伪 URL
        assertEquals("db-track://route-1", captor.firstValue.kmlSource)
    }

    @Test
    fun `reanalysis falls back to track points when stored kml file unreadable`() {
        val routeBindingService = mock<RouteBindingService>()
        val client = mock<KmlAnalysisClientService>()
        val routeMapDataRepository = mock<RouteMapDataRepository>()
        val kmlStorageService = mock<KmlStorageService>()
        val trackKmlFactory = mock<RouteTrackKmlFactory>()
        val request = KmlAnalysisSubmitRequest(routeId = "route-1")
        whenever(routeBindingService.resolveRouteId(request)).thenReturn("route-1")
        whenever(routeMapDataRepository.findById("route-1"))
            .thenReturn(Optional.of(org.example.route.model.RouteMapData(id = "route-1", kmlUrl = "lost-file.kml")))
        whenever(kmlStorageService.readStoredContent("lost-file.kml")).thenReturn(null)
        whenever(trackKmlFactory.synthesizeCurrentTrackKml("route-1")).thenReturn("<kml><LineString/></kml>")
        whenever(client.submitAnalysis(any())).thenReturn(
            Mono.just(TaskSubmitResponse("task-5", "processing", "submitted", 30))
        )
        val service = buildService(
            routeBindingService, client, routeMapDataRepository, kmlStorageService,
            trackKmlFactory = trackKmlFactory
        )

        service.submitAnalysisWithRouteBinding(request).block()!!

        val captor = argumentCaptor<KmlAnalysisSubmitRequest>()
        verify(client).submitAnalysis(captor.capture())
        assertEquals("<kml><LineString/></kml>", captor.firstValue.kmlContent)
        assertEquals("db-track://route-1", captor.firstValue.kmlSource)
    }

    @Test
    fun `reanalysis rejects route without any usable location data`() {
        val routeBindingService = mock<RouteBindingService>()
        val client = mock<KmlAnalysisClientService>()
        val routeMapDataRepository = mock<RouteMapDataRepository>()
        val trackKmlFactory = mock<RouteTrackKmlFactory>()
        val request = KmlAnalysisSubmitRequest(routeId = "route-1")
        whenever(routeBindingService.resolveRouteId(request)).thenReturn("route-1")
        whenever(routeMapDataRepository.findById("route-1")).thenReturn(Optional.empty())
        whenever(trackKmlFactory.synthesizeCurrentTrackKml("route-1")).thenReturn(null)
        val service = buildService(
            routeBindingService, client, routeMapDataRepository, trackKmlFactory = trackKmlFactory
        )

        val error = assertThrows(IllegalArgumentException::class.java) {
            service.submitAnalysisWithRouteBinding(request).block()
        }
        assertTrue(error.message!!.contains("没有可用的位置数据"))
        verify(client, never()).submitAnalysis(any())
    }
}
