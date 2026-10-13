package org.example.route.service

import org.example.config.JacksonConfig
import org.example.route.model.Route
import org.example.route.model.RouteMapData
import org.example.route.repository.*
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test
import org.mockito.kotlin.any
import org.mockito.kotlin.anyOrNull
import org.mockito.kotlin.whenever
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest
import org.springframework.boot.test.mock.mockito.MockBean
import org.springframework.context.annotation.Import
import java.math.BigDecimal

@DataJpaTest(properties = [
    "spring.flyway.enabled=false",
    "spring.jpa.hibernate.ddl-auto=create-drop",
    "spring.jpa.properties.hibernate.dialect=org.hibernate.dialect.H2Dialect"
])
@Import(RouteApplicationService::class, RoutePublicationService::class, PublicRouteApplicationService::class,
    PublicRouteDomainService::class, RouteVersionSummaryPlaceResolver::class, JacksonConfig::class,
    RouteBindingService::class, RouteAnalysisOrchestrationService::class, KmlStorageService::class,
    KmlAnalysisCallbackService::class, RouteTrackKmlFactory::class, SegmentEditService::class,
    RouteTrackReviewService::class)
class RoutePublicationServiceTest {
    companion object {
        @org.junit.jupiter.api.io.TempDir @JvmField var uploadDir: java.nio.file.Path? = null
        @JvmStatic @org.springframework.test.context.DynamicPropertySource
        fun storageProperties(registry: org.springframework.test.context.DynamicPropertyRegistry) {
            registry.add("app.kml.upload-dir") { uploadDir!!.toString() }
        }
    }

    @Autowired private lateinit var management: RouteApplicationService
    @Autowired private lateinit var publicRoutes: PublicRouteApplicationService
    @Autowired private lateinit var routes: RouteRepository
    @Autowired private lateinit var maps: RouteMapDataRepository
    @Autowired private lateinit var versions: RouteVersionRepository
    @Autowired private lateinit var current: RouteCurrentPublicVersionRepository
    @Autowired private lateinit var collection: PublicRouteCollectionRepository
    @Autowired private lateinit var orders: RouteVersionPublicationOrderRepository
    @MockBean private lateinit var routeService: RouteService
    @Autowired private lateinit var configuration: RoutePublicationConfigurationRepository
    @Autowired private lateinit var trackReview: RouteTrackReviewService
    @Autowired private lateinit var trackReviewRecords: RouteTrackReviewRepository

    @org.junit.jupiter.api.BeforeEach
    fun initializeAllocator() {
        // Flyway seeds the singleton in production; this JPA slice creates only entity tables.
        configuration.saveAndFlush(org.example.route.model.RoutePublicationConfiguration())
    }

    private fun route(status: Int = 0): Route {
        val route = routes.saveAndFlush(Route(id = "route-publication", name = "亚丁测试", createdBy = "admin", status = status,
            trackGeoJson = "[[30.0,100.0],[30.1,100.1]]"))
        maps.saveAndFlush(RouteMapData(id = route.id, distance = BigDecimal("65.21"), duration = 99,
            elevationGain = BigDecimal("3923.50"), elevationLoss = BigDecimal("3930.10")))
        whenever(routeService.getRouteWithAccessCheck(any(), anyOrNull())).thenAnswer {
            routes.findById(it.getArgument(0)).orElse(null)
        }
        return route
    }

    private fun publish(id: String, requestId: String = "request-1") =
        management.changeRouteStatus(id, 1, null, "multi_day", requestId)

    @Test
    fun `management publication is readable through collection search and detail`() {
        val route = route()
        val result = publish(route.id)
        assertTrue(result.isPublic)
        assertNotNull(result.publishedVersionId)
        assertEquals(1, publicRoutes.all().items.size, "old management status must create real public membership")
        val versionId = publicRoutes.all().items.single().currentVersionId
        assertEquals(versionId, publicRoutes.search("亚丁").items.single().currentVersionId)
        val detail = publicRoutes.detail(route.id).currentVersion
        assertEquals(versionId, detail.versionId)
        assertEquals(result.publishedVersionId, versionId)
        assertEquals("multi_day", detail.summary.routeType)
        assertEquals(65210.0, detail.summary.distance?.meters)
        assertNull(detail.summary.estimatedDuration, "legacy duration has no trustworthy unit")
        assertEquals("pending_review", detail.mainTrackAvailability)
        assertNull(detail.mainTrack)
        assertFalse(detail.generationEligibility.eligible)
        assertNull(collection.findById(route.id).orElseThrow().featuredOrder)
        assertEquals(listOf(1), orders.findByRouteIdOrderByPublishedSequenceAsc(route.id).map { it.publishedSequence })
    }

    @Test
    fun `already published legacy status can acquire missing public membership`() {
        val route = route(status = 1)
        publish(route.id)
        assertEquals(route.id, publicRoutes.all().items.single().routeId)
    }

    @Test
    fun `approved candidate is published only in a new version and old snapshot is unchanged`() {
        val route = route()
        val oldVersionId = requireNotNull(publish(route.id).publishedVersionId)
        val candidate = trackReview.read(route.id)
        val review = trackReview.submit(route.id, org.example.route.dto.MainTrackReviewRequest(
            requireNotNull(candidate.candidateId), candidate.reviewRevision, "track-approval", "approved", true, "WGS84"
        ))
        assertEquals("approved", review.review?.decision)
        assertEquals("pending_review", publicRoutes.detail(route.id).currentVersion.mainTrackAvailability)
        assertEquals(oldVersionId, publicRoutes.detail(route.id).currentVersion.versionId)

        val newVersionId = requireNotNull(publish(route.id, "publication-after-review").publishedVersionId)

        assertNotEquals(oldVersionId, newVersionId)
        val detail = publicRoutes.detail(route.id).currentVersion
        assertEquals("valid", detail.mainTrackAvailability)
        assertEquals(listOf(30.0, 30.1), requireNotNull(detail.mainTrack).path.map { it.latitude })
        assertTrue(requireNotNull(detail.mainTrack).path.all { it.referenceSystem == "WGS84" })
        assertFalse(detail.generationEligibility.eligible, "other required summary fields remain missing")
        assertFalse(detail.generationEligibility.missingReasons.orEmpty().contains("validMainTrack"))
        val old = versions.findById(oldVersionId).orElseThrow()
        assertEquals("pending_review", old.mainTrackAvailability)
        assertNull(old.mainTrackJson)
        assertEquals(listOf(1, 2), orders.findByRouteIdOrderByPublishedSequenceAsc(route.id).map { it.publishedSequence })
    }

    @Test
    fun `rejected and stale candidates are never published as valid geometry`() {
        val route = route()
        val candidate = trackReview.read(route.id)
        trackReview.submit(route.id, org.example.route.dto.MainTrackReviewRequest(
            requireNotNull(candidate.candidateId), candidate.reviewRevision, "reject-track", "rejected", false,
            reason = "含车辆接驳，完整徒步范围尚未确认"
        ))
        publish(route.id)
        val rejected = publicRoutes.detail(route.id).currentVersion
        assertEquals("invalidated", rejected.mainTrackAvailability)
        assertNull(rejected.mainTrack)

        route.trackGeoJson = "[[31.0,101.0],[31.1,101.1]]"
        route.analysisTaskId = "new-input"
        routes.saveAndFlush(route)
        publish(route.id, "new-candidate-publication")
        assertEquals("pending_review", publicRoutes.detail(route.id).currentVersion.mainTrackAvailability)
        assertNull(publicRoutes.detail(route.id).currentVersion.mainTrack)
    }

    @Test
    fun `same publication identity does not mutate an old snapshot after review`() {
        val route = route()
        val versionId = publish(route.id).publishedVersionId
        val candidate = trackReview.read(route.id)
        trackReview.submit(route.id, org.example.route.dto.MainTrackReviewRequest(
            requireNotNull(candidate.candidateId), candidate.reviewRevision, "approval-for-retry", "approved", true, "WGS84"
        ))
        assertEquals(versionId, publish(route.id).publishedVersionId)
        assertEquals("pending_review", publicRoutes.detail(route.id).currentVersion.mainTrackAvailability)
        assertEquals(1L, versions.count())
        assertEquals(1L, trackReviewRecords.count())
    }

    @Test
    fun `same publication request reuses version and withdrawal retains immutable history`() {
        val route = route()
        publish(route.id)
        val first = current.findById(route.id).orElseThrow().routeVersionId
        publish(route.id)
        assertEquals(first, current.findById(route.id).orElseThrow().routeVersionId)
        assertEquals(1L, versions.count())
        management.changeRouteStatus(route.id, 0, null)
        assertTrue(publicRoutes.all().items.isEmpty())
        assertFalse(current.existsById(route.id))
        assertTrue(versions.existsById(first))
        assertEquals(1, orders.findByRouteIdOrderByPublishedSequenceAsc(route.id).size)
    }

    @Test
    fun `missing optional distance does not block public display`() {
        val route = route()
        maps.deleteById(route.id)
        maps.flush()
        publish(route.id)
        assertEquals(route.id, publicRoutes.detail(route.id).routeId)
        assertNull(publicRoutes.all().items.single().distance)
    }

    @Test
    fun `new publication advances order without mutating previous snapshot`() {
        val route = route()
        publish(route.id)
        val first = current.findById(route.id).orElseThrow().routeVersionId
        val position = collection.findById(route.id).orElseThrow().allRouteOrder
        route.name = "新的名称"
        routes.saveAndFlush(route)
        publish(route.id, "request-2")
        val second = current.findById(route.id).orElseThrow().routeVersionId
        assertNotEquals(first, second)
        assertEquals("亚丁测试", versions.findById(first).orElseThrow().name)
        assertEquals("新的名称", publicRoutes.detail(route.id).currentVersion.summary.name)
        assertEquals(position, collection.findById(route.id).orElseThrow().allRouteOrder)
        assertEquals(listOf(1, 2), orders.findByRouteIdOrderByPublishedSequenceAsc(route.id).map { it.publishedSequence })
    }

    @Test
    fun `withdrawn request cannot reopen an old public version`() {
        val route = route()
        publish(route.id)
        management.changeRouteStatus(route.id, 0, null)
        val error = assertThrows(org.example.common.exception.BusinessException::class.java) { publish(route.id) }
        assertEquals(org.springframework.http.HttpStatus.CONFLICT, error.httpStatus)
        assertFalse(collection.existsById(route.id))
        assertEquals(1L, versions.count())
    }

    @Test
    fun `same identity with changed type and superseded identity are rejected`() {
        val route = route()
        publish(route.id)
        val changed = assertThrows(org.example.common.exception.BusinessException::class.java) {
            management.changeRouteStatus(route.id, 1, null, "one_day", "request-1")
        }
        assertEquals(org.springframework.http.HttpStatus.CONFLICT, changed.httpStatus)
        publish(route.id, "request-2")
        val latest = current.findById(route.id).orElseThrow().routeVersionId
        val stale = assertThrows(org.example.common.exception.BusinessException::class.java) { publish(route.id) }
        assertEquals(org.springframework.http.HttpStatus.CONFLICT, stale.httpStatus)
        assertEquals(latest, current.findById(route.id).orElseThrow().routeVersionId)
        assertEquals(2L, versions.count())
    }

    @Test
    fun `publication identity preserves case and trailing spaces`() {
        val route = route()
        val first = publish(route.id, "release-1").publishedVersionId
        val spaced = publish(route.id, "release-1 ").publishedVersionId
        val upper = publish(route.id, "Release-1").publishedVersionId
        assertNotEquals(first, spaced)
        assertNotEquals(spaced, upper)
        assertEquals(3L, receipts.count())
        assertEquals(3L, versions.count())
    }

    @Test
    fun `missing publication parameters are rejected without publishing`() {
        val route = route()
        listOf(null to "request-1", "loop" to "request-1", "one_day" to null,
            "multi_day" to " ", "one_day" to "x".repeat(65)).forEach { (type, id) ->
            val error = assertThrows(org.example.common.exception.BusinessException::class.java) {
                management.changeRouteStatus(route.id, 1, null, type, id)
            }
            assertEquals(org.springframework.http.HttpStatus.BAD_REQUEST, error.httpStatus)
        }
        assertEquals(0L, versions.count())
        assertFalse(collection.existsById(route.id))
        assertEquals(0, route.status)
    }

    @Test
    fun `withdrawal rejects publication parameters without changing visibility`() {
        val route = route()
        publish(route.id)
        val error = assertThrows(org.example.common.exception.BusinessException::class.java) {
            management.changeRouteStatus(route.id, 0, null, "one_day", "request-1")
        }
        assertEquals(org.springframework.http.HttpStatus.BAD_REQUEST, error.httpStatus)
        assertTrue(collection.existsById(route.id))
        assertEquals(1, route.status)
    }

    @Test
    fun `analysis and unadopted drafts prevent publication`() {
        val route = route(status = 3)
        val analyzing = assertThrows(org.example.common.exception.BusinessException::class.java) { publish(route.id) }
        assertEquals(org.springframework.http.HttpStatus.CONFLICT, analyzing.httpStatus)
        route.status = 0
        routes.saveAndFlush(route)
        segments.saveAndFlush(org.example.route.model.Segment(id = "draft-segment", routeId = route.id,
            name = "待采纳", status = "draft"))
        val draft = assertThrows(org.example.common.exception.BusinessException::class.java) { publish(route.id) }
        assertEquals(org.springframework.http.HttpStatus.UNPROCESSABLE_ENTITY, draft.httpStatus)
        assertEquals(0L, versions.count())
    }

    @Autowired private lateinit var segments: SegmentRepository
    @Autowired private lateinit var images: RouteImageRepository
    @Autowired private lateinit var tags: RouteTagRepository
    @Autowired private lateinit var versionImages: RouteVersionImageRepository
    @Autowired private lateinit var objectMapper: com.fasterxml.jackson.databind.ObjectMapper
    @Autowired private lateinit var favorites: UserRouteFavoriteRepository
    @Autowired private lateinit var completions: UserRouteCompletionRepository
    @Autowired private lateinit var schemes: SegmentSchemeRepository
    @Autowired private lateinit var pois: PoiPointRepository
    @Autowired private lateinit var transactionManager: org.springframework.transaction.PlatformTransactionManager
    @Autowired private lateinit var receipts: RoutePublicationRequestRepository
    @Autowired private lateinit var storage: KmlStorageService
    @Autowired private lateinit var analysis: RouteAnalysisOrchestrationService
    @Autowired private lateinit var callback: KmlAnalysisCallbackService
    @Autowired private lateinit var binding: RouteBindingService
    @Autowired private lateinit var editing: SegmentEditService
    @Autowired private lateinit var storedInputs: KmlStoredInputRepository
    @Autowired private lateinit var waypoints: WaypointRepository
    @MockBean private lateinit var agentClient: KmlAnalysisClientService
    @MockBean private lateinit var eventBus: org.example.route.sse.SseTaskEventBus

    @Test
    fun `allocation failure after snapshot writes rolls the whole publication back`() {
        org.springframework.test.context.transaction.TestTransaction.end()
        val transaction = org.springframework.transaction.support.TransactionTemplate(transactionManager)
        try {
            transaction.executeWithoutResult {
                configuration.saveAndFlush(org.example.route.model.RoutePublicationConfiguration(lastAllRouteOrder = Int.MAX_VALUE))
                route()
            }
            assertThrows(ArithmeticException::class.java) { publish("route-publication") }
            transaction.executeWithoutResult {
                assertEquals(0, routes.findById("route-publication").orElseThrow().status)
                assertEquals(0L, versions.count())
                assertEquals(0L, orders.count())
                assertEquals(0L, current.count())
                assertEquals(0L, collection.count())
                assertEquals(0L, receipts.count())
            }
        } finally {
            transaction.executeWithoutResult {
                maps.deleteById("route-publication")
                routes.deleteById("route-publication")
                configuration.deleteAll()
            }
        }
    }

    @Test
    fun `concurrent same request serializes to one committed version`() {
        org.springframework.test.context.transaction.TestTransaction.end()
        val transaction = org.springframework.transaction.support.TransactionTemplate(transactionManager)
        val executor = java.util.concurrent.Executors.newFixedThreadPool(2)
        try {
            transaction.executeWithoutResult {
                configuration.saveAndFlush(org.example.route.model.RoutePublicationConfiguration())
                route()
            }
            val barrier = java.util.concurrent.CyclicBarrier(2)
            val futures = (1..2).map {
                executor.submit<String> {
                    barrier.await(10, java.util.concurrent.TimeUnit.SECONDS)
                    publish("route-publication").publishedVersionId!!
                }
            }
            val results = futures.map { it.get(15, java.util.concurrent.TimeUnit.SECONDS) }
            assertEquals(results[0], results[1])
            transaction.executeWithoutResult {
                assertEquals(1L, versions.count())
                assertEquals(1L, orders.count())
                assertEquals(1L, collection.count())
                assertEquals(1L, receipts.count())
            }
        } finally {
            executor.shutdownNow()
            transaction.executeWithoutResult {
                receipts.deleteAll()
                collection.deleteAll()
                current.deleteAll()
                versionImages.deleteAll()
                orders.deleteAll()
                versions.deleteAll()
                maps.deleteById("route-publication")
                routes.deleteById("route-publication")
                configuration.deleteAll()
            }
        }
    }

    @Test
    fun `concurrent first publications append distinct collection positions`() {
        org.springframework.test.context.transaction.TestTransaction.end()
        val transaction = org.springframework.transaction.support.TransactionTemplate(transactionManager)
        val executor = java.util.concurrent.Executors.newFixedThreadPool(2)
        val ids = listOf("parallel-route-a", "parallel-route-b")
        try {
            transaction.executeWithoutResult {
                configuration.saveAndFlush(org.example.route.model.RoutePublicationConfiguration(lastAllRouteOrder = 20))
                ids.forEach { routes.saveAndFlush(Route(id = it, name = it, createdBy = "admin")) }
            }
            val barrier = java.util.concurrent.CyclicBarrier(2)
            val futures = ids.map { id -> executor.submit<String> {
                barrier.await(10, java.util.concurrent.TimeUnit.SECONDS)
                publish(id).publishedVersionId!!
            } }
            val results = futures.map { it.get(15, java.util.concurrent.TimeUnit.SECONDS) }
            assertNotEquals(results[0], results[1])
            transaction.executeWithoutResult {
                assertEquals(listOf(21, 22), collection.findAllByOrderByAllRouteOrderAsc().map { it.allRouteOrder })
                assertTrue(collection.findAll().all { it.featuredOrder == null })
                assertEquals(2L, versions.count())
            }
        } finally {
            executor.shutdownNow()
            transaction.executeWithoutResult {
                receipts.deleteAll()
                collection.deleteAll()
                current.deleteAll()
                versionImages.deleteAll()
                orders.deleteAll()
                versions.deleteAll()
                routes.deleteAllById(ids)
                configuration.deleteAll()
            }
        }
    }

    @Test
    fun `republish retains featured position and creates immutable deduplicated images`() {
        val route = route()
        route.coverUrl = "https://example.test/cover.jpg"
        routes.saveAndFlush(route)
        images.saveAndFlush(org.example.route.model.RouteImage("image-1", route.id, "https://example.test/cover.jpg", sequenceNumber = 1))
        images.saveAndFlush(org.example.route.model.RouteImage("image-2", route.id, "https://example.test/scene.jpg", sequenceNumber = 2))
        tags.saveAndFlush(org.example.route.model.RouteTag("tag-1", route.id, "徒步"))
        val result = publish(route.id)
        assertTrue(result.isPublic)
        val first = result.publishedVersionId!!
        val position = collection.findById(route.id).orElseThrow().allRouteOrder
        collection.saveAndFlush(org.example.route.model.PublicRouteCollectionEntry(route.id, position, 1))
        publish(route.id, "request-2")
        assertEquals(1, collection.findById(route.id).orElseThrow().featuredOrder)
        assertEquals(2, versionImages.findByRouteVersionIdOrderByDisplayOrderAsc(first).size)
        assertEquals(listOf("cover", "environment"), versionImages.findByRouteVersionIdOrderByDisplayOrderAsc(first).map { it.role })
        assertEquals(listOf("徒步"), objectMapper.readTree(versions.findById(first).orElseThrow().tagsJson).map { it.asText() })
    }

    @Test
    fun `close delete and repeated withdrawal remove public access but not history`() {
        val route = route()
        publish(route.id)
        val first = current.findById(route.id).orElseThrow().routeVersionId
        assertFalse(management.changeRouteStatus(route.id, 2, null).isPublic)
        assertFalse(management.changeRouteStatus(route.id, 2, null).isPublic)
        assertFalse(management.changeRouteStatus(route.id, 0, null).isPublic)
        assertFalse(management.changeRouteStatus(route.id, 0, null).isPublic)
        publish(route.id, "request-2")
        management.deleteRoute(route.id, false)
        assertFalse(collection.existsById(route.id))
        assertFalse(current.existsById(route.id))
        assertTrue(versions.existsById(first))
        assertEquals(2, orders.findByRouteIdOrderByPublishedSequenceAsc(route.id).size)
    }

    @Test
    fun `callback persistence failure rolls back result and terminal state without SSE`() {
        org.springframework.test.context.transaction.TestTransaction.end()
        val transaction = org.springframework.transaction.support.TransactionTemplate(transactionManager)
        try {
            transaction.executeWithoutResult { route() }
            binding.resolveRouteId(org.example.route.dto.KmlAnalysisSubmitRequest(
                routeId = "route-publication", taskId = "rollback-task", kmlSource = "/static/kml-upload/original.kml"))
            org.mockito.Mockito.clearInvocations(eventBus)
            val payload = objectMapper.readValue("""{"route_id":"route-publication","task_id":"rollback-task",
                "status":"completed","generated_description":"must rollback","track_path":[[31,101],[32,102]]}""",
                org.example.route.dto.KmlAnalysisCallbackRequest::class.java).copy(totalDistanceKm = Double.NaN)
            assertThrows(NumberFormatException::class.java) { callback.handleCallback(payload) }
            transaction.executeWithoutResult {
                val retained = routes.findById("route-publication").orElseThrow()
                assertEquals("submitting", retained.analysisStatus)
                assertEquals(3, retained.status)
                assertNull(retained.description)
                assertEquals("[[30.0,100.0],[30.1,100.1]]", retained.trackGeoJson)
                assertEquals("/static/kml-upload/original.kml", maps.findById(retained.id).orElseThrow().kmlUrl)
            }
            org.mockito.Mockito.verifyNoInteractions(eventBus)
        } finally {
            transaction.executeWithoutResult { maps.deleteAll(); routes.deleteAll() }
        }
    }

    @Test
    fun `concurrent reservations admit only one active analysis`() {
        org.springframework.test.context.transaction.TestTransaction.end()
        val transaction = org.springframework.transaction.support.TransactionTemplate(transactionManager)
        val executor = java.util.concurrent.Executors.newFixedThreadPool(2)
        try {
            transaction.executeWithoutResult { route(status = 2) }
            val barrier = java.util.concurrent.CyclicBarrier(2)
            val futures = (1..2).map { index -> executor.submit<String> {
                barrier.await(10, java.util.concurrent.TimeUnit.SECONDS)
                try {
                    binding.resolveRouteId(org.example.route.dto.KmlAnalysisSubmitRequest(
                        routeId = "route-publication", taskId = "parallel-task-$index", kmlSource = "/static/kml-upload/$index.kml"))
                    "accepted"
                } catch (error: org.example.common.exception.BusinessException) {
                    assertEquals(org.springframework.http.HttpStatus.CONFLICT, error.httpStatus)
                    "conflict"
                }
            } }
            assertEquals(listOf("accepted", "conflict"), futures.map { it.get(15, java.util.concurrent.TimeUnit.SECONDS) }.sorted())
            transaction.executeWithoutResult {
                val retained = routes.findById("route-publication").orElseThrow()
                assertEquals(3, retained.status)
                assertEquals(2, retained.analysisPreviousStatus)
                assertEquals("submitting", retained.analysisStatus)
                val winner = retained.analysisTaskId!!.substringAfterLast('-')
                assertEquals("/static/kml-upload/$winner.kml", maps.findById(retained.id).orElseThrow().kmlUrl)
            }
        } finally {
            executor.shutdownNow()
            transaction.executeWithoutResult { maps.deleteAll(); routes.deleteAll() }
        }
    }

    @Test
    fun `HTTP upload analysis callback adoption and publication form a committed chain`() {
        org.springframework.test.context.transaction.TestTransaction.end()
        val transaction = org.springframework.transaction.support.TransactionTemplate(transactionManager)
        val submitted = java.util.concurrent.atomic.AtomicReference<org.example.route.dto.KmlAnalysisSubmitRequest>()
        whenever(agentClient.submitAnalysis(any())).thenAnswer {
            val input = it.getArgument<org.example.route.dto.KmlAnalysisSubmitRequest>(0)
            submitted.set(input)
            reactor.core.publisher.Mono.just(org.example.route.dto.TaskSubmitResponse(input.taskId!!, "pending", "accepted", 1))
        }
        val mvc = org.springframework.test.web.servlet.setup.MockMvcBuilders.standaloneSetup(
            org.example.route.controller.RouteAnalysisController(agentClient, analysis, storage),
            org.example.route.controller.KmlAnalysisCallbackController(callback),
            org.example.route.controller.RouteController(management, routeService, favorites, completions,
                routes, schemes, segments, pois, editing),
            org.example.route.controller.PublicRouteController(publicRoutes))
            .setMessageConverters(org.springframework.http.converter.json.MappingJackson2HttpMessageConverter(objectMapper))
            .setControllerAdvice(org.example.common.exception.GlobalExceptionHandler()).build()
        fun jsonPost(url: String, body: String, status: Int = 200): com.fasterxml.jackson.databind.JsonNode {
            val result = mvc.perform(org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post(url)
                .contentType(org.springframework.http.MediaType.APPLICATION_JSON).content(body))
                .andExpect(org.springframework.test.web.servlet.result.MockMvcResultMatchers.status().`is`(status)).andReturn()
            return objectMapper.readTree(result.response.contentAsString).path("data")
        }
        try {
            transaction.executeWithoutResult {
                configuration.saveAndFlush(org.example.route.model.RoutePublicationConfiguration())
            }
            val original = "<kml><Document><Placemark><LineString><coordinates>100,30 100.1,30.1</coordinates></LineString></Placemark></Document></kml>"
            val upload = mvc.perform(org.springframework.test.web.servlet.request.MockMvcRequestBuilders
                .multipart("/api/v1/route-analysis/kml/upload")
                .file(org.springframework.mock.web.MockMultipartFile("file", "route.kml", "application/xml", original.toByteArray())))
                .andExpect(org.springframework.test.web.servlet.result.MockMvcResultMatchers.status().isOk).andReturn()
            val source = objectMapper.readTree(upload.response.contentAsString).path("data").path("kml_url").asText()
            assertTrue(source.startsWith("/static/kml-upload/"))
            java.nio.file.Files.deleteIfExists(uploadDir!!.resolve(source.substringAfterLast('/')))
            assertEquals(original, storage.readStoredContent(source))
            val async = mvc.perform(org.springframework.test.web.servlet.request.MockMvcRequestBuilders
                .post("/api/v1/route-analysis/analyze").contentType(org.springframework.http.MediaType.APPLICATION_JSON)
                .content("""{"kml_source":"$source","region_name":"亚丁链路"}"""))
                .andExpect(org.springframework.test.web.servlet.result.MockMvcResultMatchers.request().asyncStarted()).andReturn()
            val accepted = mvc.perform(org.springframework.test.web.servlet.request.MockMvcRequestBuilders.asyncDispatch(async))
                .andExpect(org.springframework.test.web.servlet.result.MockMvcResultMatchers.status().isOk).andReturn()
            val response = objectMapper.readTree(accepted.response.contentAsString).path("data")
            val routeId = response.path("route_id").asText()
            val taskId = response.path("task_id").asText()
            assertTrue(routeId.isNotBlank())
            assertEquals(routeId, submitted.get().routeId)
            assertEquals(taskId, submitted.get().taskId)
            assertEquals(original, submitted.get().kmlContent)
            assertEquals(3, routes.findById(routeId).orElseThrow().status)
            val resultBody = """{"route_id":"$routeId","task_id":"$taskId","status":"completed",
                "total_distance_km":12.5,"track_path":[[30,100],[30.1,100.1]],
                "segment_schemes":[{"scheme_type":"slope","label":"坡度","segments":[
                  {"id":"agent-segment","name":"第一段","sequence_number":1,"color":"blue",
                   "distance":12.5,"elevation_gain":200,"elevation_loss":100,"estimated_time":180,"difficulty":2}]}],
                "poi_points":[{"name":"营地","latitude":30.1,"longitude":100.1,"category":"camp"}]}"""
            jsonPost("/api/v1/route-analysis/callback", resultBody)
            assertEquals("completed", routes.findById(routeId).orElseThrow().analysisStatus)
            assertEquals(1, segments.findByRouteIdAndStatus(routeId, "draft").size)
            assertEquals(1, pois.findByRouteIdAndStatus(routeId, "draft").size)
            val publication = """{"target_status":1,"public_route_type":"multi_day","publication_id":"chain-publication"}"""
            jsonPost("/api/v1/routes/$routeId/status", publication, 422)
            assertEquals(1, jsonPost("/api/v1/routes/$routeId/segments/adopt-all", "{}").path("adopted").asInt())
            assertEquals(1, jsonPost("/api/v1/routes/$routeId/pois/adopt-all", "{}").path("adopted").asInt())
            // A duplicate callback must not reintroduce drafts after manual adoption.
            jsonPost("/api/v1/route-analysis/callback", resultBody)
            assertEquals(1L, segments.count())
            assertEquals(1L, pois.count())
            val published = jsonPost("/api/v1/routes/$routeId/status", publication)
            assertTrue(published.path("is_public").asBoolean())
            val versionId = published.path("published_version_id").asText()
            val detail = publicRoutes.detail(routeId).currentVersion
            assertEquals(versionId, detail.versionId)
            assertEquals(12500.0, detail.summary.distance?.meters)
            assertEquals("pending_review", detail.mainTrackAvailability)
            assertNull(detail.mainTrack)
            assertEquals(versionId, publicRoutes.all().items.single().currentVersionId)
            assertEquals(versionId, publicRoutes.search("亚丁链路").items.single().currentVersionId)
        } finally {
            transaction.executeWithoutResult {
                receipts.deleteAll(); collection.deleteAll(); current.deleteAll(); versionImages.deleteAll()
                orders.deleteAll(); versions.deleteAll(); pois.deleteAll(); segments.deleteAll(); schemes.deleteAll()
                waypoints.deleteAll(); maps.deleteAll(); routes.deleteAll(); storedInputs.deleteAll(); configuration.deleteAll()
            }
        }
    }

    @Test
    fun `HTTP candidate approval is explicit and only becomes public after a new publication`() {
        val route = route()
        val mvc = org.springframework.test.web.servlet.setup.MockMvcBuilders.standaloneSetup(
            org.example.route.controller.RouteTrackReviewController(trackReview))
            .setMessageConverters(org.springframework.http.converter.json.MappingJackson2HttpMessageConverter(objectMapper))
            .setControllerAdvice(org.example.common.exception.GlobalExceptionHandler()).build()
        val response = mvc.perform(org.springframework.test.web.servlet.request.MockMvcRequestBuilders
            .get("/api/v1/routes/${route.id}/main-track-review"))
            .andExpect(org.springframework.test.web.servlet.result.MockMvcResultMatchers.status().isOk).andReturn()
        val candidate = objectMapper.readTree(response.response.contentAsString).path("data")
        assertTrue(candidate.path("geometry_valid").asBoolean())
        assertEquals(2, candidate.path("candidate_path").size())
        val candidateId = candidate.path("candidate_id").asText()
        val body = """{"candidate_id":"$candidateId","expected_revision":0,"request_id":"http-review",
            "decision":"approved","confirm_complete_hiking_range":true,"reference_system":"WGS84"}"""
        val saved = mvc.perform(org.springframework.test.web.servlet.request.MockMvcRequestBuilders
            .post("/api/v1/routes/${route.id}/main-track-review")
            .contentType(org.springframework.http.MediaType.APPLICATION_JSON).content(body))
            .andExpect(org.springframework.test.web.servlet.result.MockMvcResultMatchers.status().isOk).andReturn()
        assertEquals("approved", objectMapper.readTree(saved.response.contentAsString).path("data").path("review").path("decision").asText())
        assertFalse(collection.existsById(route.id), "review must not publish implicitly")
        mvc.perform(org.springframework.test.web.servlet.request.MockMvcRequestBuilders
            .post("/api/v1/routes/${route.id}/main-track-review")
            .contentType(org.springframework.http.MediaType.APPLICATION_JSON).content(body))
            .andExpect(org.springframework.test.web.servlet.result.MockMvcResultMatchers.status().isOk)
        assertEquals(1L, trackReviewRecords.count())
        publish(route.id)
        assertNotNull(publicRoutes.detail(route.id).currentVersion.mainTrack)
    }

    @Test
    fun `concurrent review submissions admit one revision and never overwrite the winner`() {
        org.springframework.test.context.transaction.TestTransaction.end()
        val transaction = org.springframework.transaction.support.TransactionTemplate(transactionManager)
        val executor = java.util.concurrent.Executors.newFixedThreadPool(2)
        try {
            transaction.executeWithoutResult { route() }
            val candidate = trackReview.read("route-publication")
            val barrier = java.util.concurrent.CyclicBarrier(2)
            val futures = (1..2).map { index -> executor.submit<String> {
                barrier.await(10, java.util.concurrent.TimeUnit.SECONDS)
                try {
                    trackReview.submit("route-publication", org.example.route.dto.MainTrackReviewRequest(
                        requireNotNull(candidate.candidateId), candidate.reviewRevision, "parallel-review-$index",
                        "approved", true, "WGS84"))
                    "accepted"
                } catch (error: org.example.common.exception.BusinessException) {
                    assertEquals(org.springframework.http.HttpStatus.CONFLICT, error.httpStatus)
                    "conflict"
                }
            } }
            assertEquals(listOf("accepted", "conflict"), futures.map { it.get(15, java.util.concurrent.TimeUnit.SECONDS) }.sorted())
            transaction.executeWithoutResult {
                assertEquals(1L, trackReviewRecords.count())
                assertEquals(1L, trackReviewRecords.findFirstByRouteIdOrderByRevisionDesc("route-publication")?.revision)
            }
        } finally {
            executor.shutdownNow()
            transaction.executeWithoutResult { trackReviewRecords.deleteAll(); maps.deleteAll(); routes.deleteAll() }
        }
    }

    @Test
    fun `HTTP management publish and public collection search detail share a persisted version`() {
        val route = route()
        val controller = org.example.route.controller.RouteController(management, routeService, favorites,
            completions, routes, schemes, segments, pois, org.mockito.kotlin.mock<SegmentEditService>())
        val mvc = org.springframework.test.web.servlet.setup.MockMvcBuilders.standaloneSetup(
            controller, org.example.route.controller.PublicRouteController(publicRoutes))
            .setMessageConverters(org.springframework.http.converter.json.MappingJackson2HttpMessageConverter(objectMapper))
            .setControllerAdvice(org.example.common.exception.GlobalExceptionHandler()).build()
        val published = mvc.perform(org.springframework.test.web.servlet.request.MockMvcRequestBuilders
            .post("/api/v1/routes/${route.id}/status").contentType(org.springframework.http.MediaType.APPLICATION_JSON)
            .content("""{"target_status":1,"public_route_type":"multi_day","publication_id":"http-request"}"""))
            .andExpect(org.springframework.test.web.servlet.result.MockMvcResultMatchers.status().isOk).andReturn()
        val result = objectMapper.readTree(published.response.contentAsString).path("data")
        assertTrue(result.path("is_public").asBoolean())
        val versionId = result.path("published_version_id").asText()
        assertTrue(versionId.isNotBlank())
        val detail = mvc.perform(org.springframework.test.web.servlet.request.MockMvcRequestBuilders
            .get("/api/v1/public-routes/${route.id}"))
            .andExpect(org.springframework.test.web.servlet.result.MockMvcResultMatchers.status().isOk).andReturn()
        assertEquals(versionId, objectMapper.readTree(detail.response.contentAsString).path("data").path("currentVersion").path("versionId").asText())
        listOf("/api/v1/public-routes", "/api/v1/public-routes/search?query=亚丁").forEach { url ->
            val response = mvc.perform(org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get(url))
                .andExpect(org.springframework.test.web.servlet.result.MockMvcResultMatchers.status().isOk).andReturn()
            assertEquals(versionId, objectMapper.readTree(response.response.contentAsString).path("data").path("items")[0].path("currentVersionId").asText())
        }
    }
}
