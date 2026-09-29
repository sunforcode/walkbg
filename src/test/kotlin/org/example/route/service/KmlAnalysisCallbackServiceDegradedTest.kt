package org.example.route.service

import com.fasterxml.jackson.databind.JsonNode
import com.fasterxml.jackson.databind.ObjectMapper
import org.example.route.dto.CallbackPoiPointDto
import org.example.route.dto.CallbackSegmentDto
import org.example.route.dto.CallbackSegmentSchemeDto
import org.example.route.dto.KmlAnalysisCallbackRequest
import org.example.route.dto.PoiResolveAgentResponse
import org.example.route.dto.PoiResolveAgentResultItem
import org.example.route.model.PoiLibraryItem
import org.example.route.model.PoiPoint
import org.example.route.model.Route
import org.example.route.model.RouteMapData
import org.example.route.model.Segment
import org.example.route.model.SegmentScheme
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
import org.mockito.kotlin.times
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever
import java.util.Optional

class KmlAnalysisCallbackServiceDegradedTest {
    @org.junit.jupiter.api.BeforeEach
    fun beginTransactionSynchronization() {
        org.springframework.transaction.support.TransactionSynchronizationManager.initSynchronization()
    }

    @org.junit.jupiter.api.AfterEach
    fun clearTransactionSynchronization() {
        if (org.springframework.transaction.support.TransactionSynchronizationManager.isSynchronizationActive()) {
            org.springframework.transaction.support.TransactionSynchronizationManager.clearSynchronization()
        }
    }

    private fun commitEvents() {
        org.springframework.transaction.support.TransactionSynchronizationManager.getSynchronizations().forEach { it.afterCommit() }
        org.springframework.transaction.support.TransactionSynchronizationManager.clearSynchronization()
        org.springframework.transaction.support.TransactionSynchronizationManager.initSynchronization()
    }

    @Test
    fun `completed callback propagates degraded status to SSE`() {
        val routeRepository = mock<RouteRepository>()
        val eventBus = mock<SseTaskEventBus>()
        whenever(routeRepository.findByIdForUpdate("route-1")).thenReturn(Route(
            id = "route-1",
            name = "测试路线",
            status = 3,
            analysisTaskId = "task-1", analysisStatus = "processing", analysisPreviousStatus = 0,
            createdBy = "user-1"
        ))
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
        commitEvents()

        val captor = argumentCaptor<SseProgressEvent>()
        verify(eventBus).publish(eq("task-1"), captor.capture())
        assertTrue(captor.firstValue.degraded == true)
    }

    @Test
    fun `failed callback publishes callback error and falls back when absent`() {
        val routeRepository = mock<RouteRepository>()
        val eventBus = mock<SseTaskEventBus>()
        whenever(routeRepository.findByIdForUpdate("route-1")).thenReturn(Route(
            id = "route-1",
            name = "测试路线",
            status = 3,
            analysisTaskId = "task-1", analysisStatus = "processing", analysisPreviousStatus = 0,
            createdBy = "user-1"
        ))
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
        commitEvents()

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
        whenever(routeRepository.findByIdForUpdate("route-1")).thenReturn(
            Route(
                id = "route-1",
                name = "测试路线",
                region = "五台山",
                status = 3,
                analysisTaskId = "task-1", analysisStatus = "processing", analysisPreviousStatus = 0,
                createdBy = "user-1"
            )
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

    private class DeliveryFixture(taskId: String = "task-1", taskStatus: String = "processing") {
        val route = Route(id = "route-1", name = "原路线", status = 3, createdBy = "user-1", trackGeoJson = "[[1,2,3]]")
        val routes = org.mockito.Mockito.mock(RouteRepository::class.java) { invocation ->
            when (invocation.method.name) {
                "findById" -> Optional.of(route)
                "findByIdForUpdate" -> route
                "save" -> invocation.arguments[0]
                else -> org.mockito.Mockito.RETURNS_DEFAULTS.answer(invocation)
            }
        }
        val segments = mock<SegmentRepository>()
        val schemes = mock<SegmentSchemeRepository>()
        val pois = mock<PoiPointRepository>()
        val maps = mock<RouteMapDataRepository>()
        val events = mock<SseTaskEventBus>()
        val service = KmlAnalysisCallbackService(routes, segments, schemes, pois, mock(), mock(), maps, mock(), events, ObjectMapper())

        init {
            route.analysisTaskId = taskId
            route.analysisStatus = taskStatus
            route.analysisPreviousStatus = 2
        }

        fun request(taskId: String? = "task-1", status: String = "completed") = KmlAnalysisCallbackRequest(
            routeId = route.id, taskId = taskId, status = status,
            sourceKmlUrl = null, analysisTimestamp = null, qualityScore = null,
            totalDistanceKm = null, totalElevationGainM = null, totalElevationLossM = null,
            maxElevation = null, minElevation = null, isLoop = null, estimatedDifficulty = null,
            generatedDescription = "本次结果", trackPath = listOf(listOf(30.0, 120.0, 100.0)),
            poiPoints = listOf(CallbackPoiPointDto(name = "点位", latitude = 30.0, longitude = 120.0, category = "photo"))
        )
    }

    @Test
    fun `completed callback preserves day and slope content and camp analysis in management detail`() {
        // enrich-grounded-segment-content: route-analysis-schema / route-management-api.
        val f = DeliveryFixture()
        val mapper = ObjectMapper()
        val daySegment = CallbackSegmentDto(
            id = "agent-day-1",
            name = "第1天",
            sequenceNumber = 1,
            color = "#3388FF",
            description = "计算统计：0.15公里、爬升10米。原记录：第一天营地。",
            distance = 0.15,
            elevationGain = 10.0,
            elevationLoss = 0.0,
            estimatedTime = 5,
            difficulty = 1,
            trackStartIndex = 0,
            trackEndIndex = 1,
            startPoint = null,
            endPoint = null,
            segmentType = null,
            slopeDirection = null,
            avgSlopeDegrees = null,
            maxSlopeDegrees = null,
            confidence = 0.8,
            notes = "原作者提示：第一天过河处留意原路标记。"
        )
        val slopeSegment = daySegment.copy(
            id = "agent-slope-1",
            name = "路段1",
            description = "路段统计：0.15公里、爬升10米。原轨迹标注：第一天营地。",
            notes = "原标注提示：横切处路迹不明。"
        )
        val campAnalysis = mapOf(
            "status" to "pending_verification",
            "source" to "source_marker",
            "usage" to "recorded_overnight",
            "evidence" to "第一天营地",
            "associations" to listOf(
                mapOf(
                    "scheme_type" to "day",
                    "track_start_index" to 0,
                    "track_end_index" to 1,
                    "day_number" to 1,
                    "basis" to "explicit_day",
                    "distance_meters" to 2.0
                ),
                mapOf(
                    "scheme_type" to "slope",
                    "track_start_index" to 0,
                    "track_end_index" to 1,
                    "basis" to "explicit_day",
                    "distance_meters" to 2.0
                )
            )
        )
        val campCardData = mapOf("camp_analysis" to campAnalysis)
        val camp = CallbackPoiPointDto(
            name = "第一天营地",
            latitude = 30.001018,
            longitude = 120.001,
            elevation = 110.0,
            category = "camp",
            source = "kml_marker",
            description = "第一天营地",
            confidence = 1.0,
            cardData = campCardData
        )
        val request = f.request().copy(
            trackPath = listOf(listOf(30.0, 120.0, 100.0), listOf(30.001, 120.001, 110.0)),
            segmentSchemes = listOf(
                CallbackSegmentSchemeDto(schemeType = "day", label = "按天", segments = listOf(daySegment)),
                CallbackSegmentSchemeDto(schemeType = "slope", label = "按坡度", isDefault = true, segments = listOf(slopeSegment))
            ),
            poiPoints = listOf(camp)
        )

        f.service.handleCallback(request)
        commitEvents()

        val savedRoute = argumentCaptor<Route>()
        val savedMap = argumentCaptor<RouteMapData>()
        val savedSchemes = argumentCaptor<SegmentScheme>()
        val savedSegments = argumentCaptor<Segment>()
        val savedPois = argumentCaptor<PoiPoint>()
        verify(f.routes).save(savedRoute.capture())
        verify(f.maps).save(savedMap.capture())
        verify(f.schemes, times(2)).save(savedSchemes.capture())
        verify(f.segments, times(2)).save(savedSegments.capture())
        verify(f.pois).save(savedPois.capture())
        val expectedCardData = mapper.valueToTree<JsonNode>(campCardData)
        assertEquals(expectedCardData, mapper.readTree(requireNotNull(savedPois.firstValue.cardData)))

        // 详情只能读回真实回调捕获的实体，不能用请求或手组 DTO 替代保存结果。
        whenever(f.maps.findById(f.route.id)).thenReturn(Optional.of(savedMap.firstValue))
        whenever(f.schemes.findByRouteId(f.route.id)).thenReturn(savedSchemes.allValues)
        whenever(f.segments.findByRouteId(f.route.id)).thenReturn(savedSegments.allValues)
        whenever(f.pois.findByRouteId(f.route.id)).thenReturn(savedPois.allValues)
        val routeService = mock<RouteService>()
        whenever(routeService.getRouteWithAccessCheck(f.route.id, null)).thenReturn(savedRoute.firstValue)
        val applicationService = RouteApplicationService(
            routeService = routeService,
            waypointRepository = mock(),
            segmentRepository = f.segments,
            routeTagRepository = mock(),
            dailyPlanRepository = mock(),
            hitchhikeContactRepository = mock(),
            routeImageRepository = mock(),
            routeMapDataRepository = f.maps,
            routeRatingRepository = mock(),
            userRepository = mock(),
            segmentSchemeRepository = f.schemes,
            poiPointRepository = f.pois,
            routeRepository = f.routes,
            tripRouteAssociationRepository = mock(),
            tripRepository = mock(),
            objectMapper = mapper,
            routePublicationService = mock()
        )
        val detail = requireNotNull(applicationService.getRouteFullDetails(f.route.id))

        assertEquals(setOf("day", "slope"), detail.segmentSchemes.map { it.schemeType }.toSet())
        assertEquals(2, detail.segmentSchemes.size)
        assertEquals(request.trackPath, detail.trackPath)
        assertEquals(2, savedSegments.allValues.map { it.id }.toSet().size)
        for ((schemeType, input) in mapOf("day" to daySegment, "slope" to slopeSegment)) {
            val stored = savedSegments.allValues.single { it.schemeType == schemeType }
            val storedScheme = savedSchemes.allValues.single { it.schemeType == schemeType }
            assertEquals(storedScheme.id, stored.schemeId)
            assertTrue(stored.id.isNotBlank() && stored.id != input.id, "后端身份不能复用 Agent 临时段 ID")
            assertEquals(input.description, stored.description)
            assertTrue(requireNotNull(stored.notes).contains(requireNotNull(input.notes)))
            assertTrue(requireNotNull(stored.notes).contains("分析置信度: 80%"))
            val scheme = detail.segmentSchemes.single { it.schemeType == schemeType }
            val segment = scheme.segments.single()
            assertEquals(storedScheme.id, scheme.id)
            assertEquals(stored.id, segment.id)
            assertEquals(input.description, segment.description)
            assertEquals(stored.notes, segment.notes)
            assertEquals(input.trackStartIndex, segment.trackStartIndex)
            assertEquals(input.trackEndIndex, segment.trackEndIndex)
        }
        val detailCamp = detail.poiPoints.single()
        assertTrue(savedPois.firstValue.id.startsWith("poi_"))
        assertEquals(savedPois.firstValue.id, detailCamp.id)
        assertEquals("camp", detailCamp.category)
        assertEquals("draft", detailCamp.status)
        assertEquals(camp.confidence, detailCamp.confidence)
        // 完整 JSON 相等证明快照仍是原范围证据，未被改写成数据库外键或已核验状态。
        assertEquals(expectedCardData, mapper.valueToTree<JsonNode>(detailCamp.cardData))
    }

    @Test
    fun `empty callback source cannot erase reserved input reference`() {
        assertReservedSourcePreserved("")
    }

    @Test
    fun `different callback source cannot replace reserved input reference`() {
        assertReservedSourcePreserved("https://example.test/unrelated.kml")
    }

    private fun assertReservedSourcePreserved(callbackSource: String) {
        val f = DeliveryFixture()
        val reservedSource = "/static/kml-upload/reserved.kml"
        whenever(f.maps.findById(f.route.id)).thenReturn(Optional.of(
            RouteMapData(id = f.route.id, kmlUrl = reservedSource)
        ))

        f.service.handleCallback(f.request().copy(sourceKmlUrl = callbackSource))

        val savedMap = argumentCaptor<RouteMapData>()
        verify(f.maps).save(savedMap.capture())
        assertEquals(reservedSource, savedMap.firstValue.kmlUrl, "任务预占时绑定的原文来源不可被回调元数据改写")
        assertEquals("completed", f.route.analysisStatus, "来源元数据不可信不应阻断当前任务结果完成")
    }

    @Test
    fun `repeated terminal callback performs no second result write`() {
        val f = DeliveryFixture()
        val request = f.request()
        f.service.handleCallback(request)
        commitEvents()
        org.mockito.Mockito.clearInvocations(f.routes, f.segments, f.schemes, f.pois, f.maps, f.events)

        f.service.handleCallback(request)

        verify(f.routes, org.mockito.kotlin.never()).save(any<Route>())
        org.mockito.Mockito.verifyNoInteractions(f.segments, f.schemes, f.pois, f.maps, f.events)
    }

    @Test
    fun `late callback from superseded task cannot replace latest track`() {
        val f = DeliveryFixture(taskId = "task-new")
        val original = f.route.trackGeoJson

        f.service.handleCallback(f.request(taskId = "task-old"))

        assertEquals(original, f.route.trackGeoJson)
        assertEquals(3, f.route.status)
        verify(f.routes, org.mockito.kotlin.never()).save(any<Route>())
        org.mockito.Mockito.verifyNoInteractions(f.segments, f.schemes, f.pois, f.maps, f.events)
    }

    @Test
    fun `late success after submission failure has no write eligibility`() {
        val f = DeliveryFixture(taskStatus = "failed")
        f.route.status = 2
        val original = f.route.trackGeoJson

        f.service.handleCallback(f.request())

        assertEquals(original, f.route.trackGeoJson)
        assertEquals(2, f.route.status)
        org.mockito.Mockito.verifyNoInteractions(f.segments, f.schemes, f.pois, f.maps, f.events)
    }

    @Test
    fun `late failed callback cannot terminate newer analysis`() {
        val f = DeliveryFixture(taskId = "task-new")

        f.service.handleCallback(f.request(taskId = "task-old", status = "failed"))

        assertEquals(3, f.route.status)
        verify(f.routes, org.mockito.kotlin.never()).save(any<Route>())
        org.mockito.Mockito.verifyNoInteractions(f.events)
    }

    @Test
    fun `failed callback restores actual preanalysis status`() {
        val f = DeliveryFixture()
        f.service.handleCallback(f.request(status = "failed"))
        assertEquals(2, f.route.status)
    }

    @Test
    fun `terminal event waits for transaction commit`() {
        val f = DeliveryFixture()
        try {
            f.service.handleCallback(f.request())
            org.mockito.Mockito.verifyNoInteractions(f.events)
            val callbacks = org.springframework.transaction.support.TransactionSynchronizationManager.getSynchronizations()
            assertTrue(callbacks.isNotEmpty(), "必须注册事务提交后通知")
            callbacks.forEach { it.afterCommit() }
            verify(f.events).publish(eq("task-1"), any())
        } finally {
            org.springframework.transaction.support.TransactionSynchronizationManager.clearSynchronization()
        }
    }

    @Test
    fun `rolled back transaction never emits completed event`() {
        val f = DeliveryFixture()
        try {
            f.service.handleCallback(f.request())
            org.springframework.transaction.support.TransactionSynchronizationManager.getSynchronizations().forEach {
                it.afterCompletion(org.springframework.transaction.support.TransactionSynchronization.STATUS_ROLLED_BACK)
            }
            org.mockito.Mockito.verifyNoInteractions(f.events)
        } finally {
            org.springframework.transaction.support.TransactionSynchronizationManager.clearSynchronization()
        }
    }

    @Test
    fun `SSE failure does not escape committed callback`() {
        val f = DeliveryFixture()
        org.mockito.kotlin.doThrow(IllegalStateException("SSE unavailable")).whenever(f.events).publish(any(), any())
        try {
            org.junit.jupiter.api.Assertions.assertDoesNotThrow {
                f.service.handleCallback(f.request())
                org.springframework.transaction.support.TransactionSynchronizationManager.getSynchronizations().forEach { it.afterCommit() }
            }
        } finally {
            org.springframework.transaction.support.TransactionSynchronizationManager.clearSynchronization()
        }
    }

    @Test
    fun `callback rejects missing identity and unknown status before writing`() {
        val f = DeliveryFixture()
        org.junit.jupiter.api.Assertions.assertThrows(IllegalArgumentException::class.java) {
            f.service.handleCallback(f.request(taskId = " "))
        }
        org.junit.jupiter.api.Assertions.assertThrows(IllegalArgumentException::class.java) {
            f.service.handleCallback(f.request(status = "processing"))
        }
        org.mockito.Mockito.verifyNoInteractions(f.segments, f.schemes, f.pois, f.maps, f.events)
    }

    @Test
    fun `transient callback database failure returns retryable 503`() {
        val service = mock<KmlAnalysisCallbackService>()
        org.mockito.kotlin.doThrow(org.springframework.dao.CannotAcquireLockException("busy")).whenever(service).handleCallback(any())
        val response = org.example.route.controller.KmlAnalysisCallbackController(service).handleCallback(DeliveryFixture().request())
        assertEquals(503, response.statusCode.value())
    }

    @Test
    fun `unclassified callback failure returns 500 rather than permanent 400`() {
        val service = mock<KmlAnalysisCallbackService>()
        org.mockito.kotlin.doThrow(IllegalStateException("internal")).whenever(service).handleCallback(any())
        val response = org.example.route.controller.KmlAnalysisCallbackController(service).handleCallback(DeliveryFixture().request())
        assertEquals(500, response.statusCode.value())
    }
}
