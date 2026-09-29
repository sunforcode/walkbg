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
    RouteTrackReviewService::class, RoutePublicationContentService::class)
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

    private fun adoptedContent(approveTrack: Boolean = true): Route {
        val route = route()
        val started = java.time.Instant.parse("2026-07-20T00:00:00Z")
        route.trackGeoJson = objectMapper.writeValueAsString((0..9).map { listOf(30.0 + it * 0.01, 100.0 + it * 0.01, 4000.0 + it) })
        route.analysisTaskId = "content-task"
        route.analysisStartedAt = started
        route.analysisStatus = "completed"
        routes.saveAndFlush(route)
        for (kind in listOf("day", "slope")) {
            schemes.saveAndFlush(org.example.route.model.SegmentScheme("scheme-$kind", route.id, kind, kind,
                isDefault = kind == "slope", createdAt = started.plusSeconds(1)))
            for (number in listOf(3, 1, 5, 2, 4)) {
                segments.saveAndFlush(org.example.route.model.Segment(
                    id = "$kind-$number", routeId = route.id, name = "$kind 第${number}天",
                    description = "${kind}原说明$number", notes = "原轨迹提示：过河$number（待核验）",
                    distance = number * 1.25, elevationGain = number * 100.0, elevationLoss = number * 20.0,
                    estimatedTime = number * 45.0, difficulty = 1, routeType = 2,
                    sequenceNumber = number, trackStartIndex = (number - 1) * 2, trackEndIndex = number * 2 - 1,
                    schemeId = "scheme-$kind", schemeType = kind, status = "confirmed",
                    createdAt = started.plusSeconds(2), updatedAt = started.plusSeconds(2)
                ))
            }
        }
        for (number in 1..5) {
            val dayNumber = if (number == 5) 1 else number
            val name = if (number == 5) "看到甲营地" else "第${number}天营地"
            val snapshot = mapOf("camp_analysis" to mapOf(
                "status" to "pending_verification", "source" to "source_marker",
                "usage" to if (number == 5) "along_route" else "recorded_overnight",
                "evidence" to name, "associations" to listOf(mapOf(
                    "scheme_type" to "day", "day_number" to dayNumber,
                    "track_start_index" to (dayNumber - 1) * 2, "track_end_index" to dayNumber * 2 - 1,
                    "basis" to "explicit_day", "distance_meters" to 2.0
                ))
            ))
            pois.saveAndFlush(org.example.route.model.PoiPoint(
                id = "source-camp-$number", routeId = route.id, name = name,
                latitude = 30.0 + number * 0.01, longitude = 100.0 + number * 0.01, elevation = 4100.0,
                category = "camp", source = "kml_marker", confidence = 1.0, status = "confirmed",
                description = "<div>原营地标注</div><img src=\"files/photo.png\"/>",
                cardData = objectMapper.writeValueAsString(snapshot), createdAt = started.plusSeconds(3)
            ))
        }
        if (approveTrack) {
            val candidate = trackReview.read(route.id)
            trackReview.submit(route.id, org.example.route.dto.MainTrackReviewRequest(
                requireNotNull(candidate.candidateId), candidate.reviewRevision, "content-track-review", "approved", true, "WGS84"))
        }
        return route
    }

    @Test
    fun `publication includes all adopted days segments and source camps in the public version`() {
        val route = adoptedContent()
        publish(route.id)
        val detail = publicRoutes.detail(route.id).currentVersion
        val json = objectMapper.valueToTree<com.fasterxml.jackson.databind.JsonNode>(detail)
        assertEquals(5, detail.referenceDays?.size)
        assertEquals((1..5).toList(), detail.referenceDays!!.map { it.dayNumber })
        assertEquals(5, detail.segments?.size)
        assertEquals(5, detail.campsites?.size)
        for (number in 1..5) {
            val day = json.path("referenceDays")[number - 1]
            assertEquals("day原说明$number", day.path("description").asText())
            assertEquals("原轨迹提示：过河$number（待核验）", day.path("notes").asText())
            assertEquals(number * 1250.0, day.path("distance").path("meters").asDouble())
            assertEquals(number * 2700.0, day.path("estimatedDuration").path("seconds").asDouble())
            assertEquals(number * 100.0, day.path("ascent").path("meters").asDouble())
            assertEquals((number - 1) * 2, day.path("mainTrackRange").path("startPathPosition").path("precedingPositionIndex").asInt())
            assertFalse(day.has("start"), "unnamed endpoints must not be invented")
            assertFalse(day.has("segments"), "daily data is independent of slope segments")
            if (number < 5) {
                assertTrue(day.path("accommodation").asText().contains("第${number}天营地"))
                assertEquals(day.path("accommodation"), day.path("accommodationEvidence").path("value"))
                assertEquals("pending_verification", day.path("accommodationEvidence").path("confidence").path("status").asText())
            } else {
                assertFalse(day.has("accommodation"))
                assertFalse(day.has("accommodationEvidence"))
            }
        }
        assertFalse(json.path("referenceDays")[4].path("mainTrackRange").path("endPathPosition").has("progressToNextPosition"))
        assertEquals(listOf("slope原说明1", "slope原说明2", "slope原说明3", "slope原说明4", "slope原说明5"), detail.segments!!.map { it.description })
        assertTrue(detail.segments!!.all { it.difficulty == null && it.terrainOrRoadType == null })
        json.path("campsites").forEach { camp ->
            assertEquals("WGS84", camp.path("positions")[0].path("referenceSystem").asText())
            assertEquals("pending_verification", camp.path("sourceEvidence").path("confidence").path("status").asText())
            assertEquals("public_route_fact", camp.path("sourceEvidence").path("confidence").path("category").asText())
            assertTrue(camp.path("sourceEvidence").path("confidence").path("source").asText().isNotBlank())
            assertFalse(camp.has("status"), "source confidence is not a campsite business status")
            assertFalse(camp.toString().contains("track_start_index"))
            assertFalse(camp.toString().contains("photo.png"))
        }
        assertNull(detail.summary.estimatedDuration, "do not sum child durations into a summary fact")
        assertFalse(detail.generationEligibility.eligible)
    }

    @Test
    fun `republishing content creates isolated local identities without rewriting prior days`() {
        val route = adoptedContent()
        val first = requireNotNull(publish(route.id).publishedVersionId)
        val old = objectMapper.writeValueAsString(publicRoutes.detail(route.id).currentVersion)
        val edited = segments.findById("day-1").orElseThrow()
        edited.description = "人工修改后的第一天说明"
        segments.saveAndFlush(edited)
        assertEquals(first, publish(route.id).publishedVersionId)
        assertEquals(old, objectMapper.writeValueAsString(publicRoutes.detail(route.id).currentVersion))
        val second = requireNotNull(publish(route.id, "republish-content").publishedVersionId)
        assertNotEquals(first, second)
        assertTrue(publicRoutes.detail(route.id).currentVersion.referenceDays?.first()?.let {
            objectMapper.valueToTree<com.fasterxml.jackson.databind.JsonNode>(it).path("description").asText() == "人工修改后的第一天说明"
        } == true)
        val oldDays = objectMapper.readTree(versions.findById(first).orElseThrow().referenceDaysJson)
        assertEquals("day原说明1", oldDays[0].path("description").asText())
        assertNotEquals(oldDays[0].path("identity").asText(), publicRoutes.detail(route.id).currentVersion.referenceDays!!.first().identity)
    }

    @Test
    fun `unreviewed track publishes adopted text without leaking ranges or guessing campsite coordinates`() {
        val route = adoptedContent(approveTrack = false)
        publish(route.id)
        val detail = publicRoutes.detail(route.id).currentVersion
        val json = objectMapper.valueToTree<com.fasterxml.jackson.databind.JsonNode>(detail)
        assertEquals(5, detail.referenceDays?.size)
        assertEquals(5, detail.segments?.size)
        assertNull(detail.mainTrack)
        assertNull(detail.campsites)
        assertTrue(json.path("referenceDays").all { !it.has("mainTrackRange") && !it.has("accommodation") })
        assertTrue(detail.segments!!.all { it.mainTrackRange == null })
    }

    @Test
    fun `duplicate adopted day number aborts publication instead of renumbering`() {
        val route = adoptedContent()
        val day = segments.findById("day-2").orElseThrow()
        segments.saveAndFlush(day.copy(sequenceNumber = 1))
        val failure = assertThrows(org.example.common.exception.BusinessException::class.java) { publish(route.id) }
        assertEquals(org.springframework.http.HttpStatus.UNPROCESSABLE_ENTITY, failure.httpStatus)
        assertEquals(0L, versions.count())
        assertFalse(collection.existsById(route.id))
    }

    @Test
    fun `multiple adopted slope schemes are not merged implicitly`() {
        val route = adoptedContent()
        schemes.saveAndFlush(org.example.route.model.SegmentScheme("slope-other", route.id, "slope", "另一走法"))
        segments.saveAndFlush(org.example.route.model.Segment("other-segment", route.id, "其他路段",
            sequenceNumber = 1, schemeId = "slope-other", schemeType = "slope"))
        assertEquals(org.springframework.http.HttpStatus.UNPROCESSABLE_ENTITY,
            assertThrows(org.example.common.exception.BusinessException::class.java) { publish(route.id) }.httpStatus)
        assertEquals(0L, versions.count())
    }

    @Test
    fun `one day publication never leaks a reference day collection`() {
        val route = adoptedContent()
        management.changeRouteStatus(route.id, 1, null, "one_day", "one-day-content")
        val detail = publicRoutes.detail(route.id).currentVersion
        assertNull(detail.referenceDays)
        assertEquals(5, detail.segments?.size)
        assertEquals(5, detail.campsites?.size)
    }

    @Test
    fun `older adopted content cannot reuse the new analysis track ranges or camp associations`() {
        val route = adoptedContent()
        route.analysisStartedAt = requireNotNull(route.analysisStartedAt).plusSeconds(20)
        route.analysisTaskId = "new-analysis-task"
        routes.saveAndFlush(route)
        val candidate = trackReview.read(route.id)
        trackReview.submit(route.id, org.example.route.dto.MainTrackReviewRequest(
            requireNotNull(candidate.candidateId), candidate.reviewRevision, "new-track-review", "approved", true, "WGS84"))
        publish(route.id)
        val detail = publicRoutes.detail(route.id).currentVersion
        val json = objectMapper.valueToTree<com.fasterxml.jackson.databind.JsonNode>(detail)
        assertEquals(5, detail.referenceDays?.size)
        assertTrue(json.path("referenceDays").all { !it.has("mainTrackRange") && !it.has("accommodation") })
        assertTrue(detail.segments!!.all { it.mainTrackRange == null })
        assertNull(detail.campsites)
    }

    @Test
    fun `invalid current source range or evidence aborts before advancing the public version`() {
        val route = adoptedContent()
        val camp = pois.findById("source-camp-1").orElseThrow()
        pois.saveAndFlush(camp.copy(cardData = """{"camp_analysis":{"status":"verified","source":"source_marker","usage":"recorded_overnight","evidence":"不应核验","associations":[]}}"""))
        val failure = assertThrows(org.example.common.exception.BusinessException::class.java) { publish(route.id) }
        assertEquals(org.springframework.http.HttpStatus.UNPROCESSABLE_ENTITY, failure.httpStatus)
        assertEquals(0L, versions.count())
        assertFalse(collection.existsById(route.id))
    }

    @Test
    fun `overlapping or out of bounds adopted day ranges cannot be published`() {
        val route = adoptedContent()
        val original = segments.findById("day-2").orElseThrow()
        segments.saveAndFlush(original.copy(trackStartIndex = 0))
        assertEquals(org.springframework.http.HttpStatus.UNPROCESSABLE_ENTITY,
            assertThrows(org.example.common.exception.BusinessException::class.java) { publish(route.id) }.httpStatus)
        assertEquals(0L, versions.count())
    }

    @Test
    fun `negative day statistics are not silently replaced by zero`() {
        val route = adoptedContent()
        segments.saveAndFlush(segments.findById("day-2").orElseThrow().copy(estimatedTime = -1.0))
        assertEquals(org.springframework.http.HttpStatus.UNPROCESSABLE_ENTITY,
            assertThrows(org.example.common.exception.BusinessException::class.java) { publish(route.id) }.httpStatus)
        assertEquals(0L, versions.count())
    }

    @Test
    fun `unmatched old camp associations keep source evidence without inventing accommodation`() {
        val route = adoptedContent()
        val camp = pois.findById("source-camp-1").orElseThrow()
        val data = objectMapper.readTree(camp.cardData)
        (data.path("camp_analysis").path("associations")[0] as com.fasterxml.jackson.databind.node.ObjectNode)
            .put("track_end_index", 2)
        pois.saveAndFlush(camp.copy(cardData = objectMapper.writeValueAsString(data)))
        publish(route.id)
        val detail = publicRoutes.detail(route.id).currentVersion
        assertNull(detail.referenceDays!!.first().accommodation)
        assertEquals(5, detail.campsites!!.size)
    }

    @Test
    fun `public day content is returned by the actual HTTP detail controller`() {
        val route = adoptedContent()
        publish(route.id)
        val mvc = org.springframework.test.web.servlet.setup.MockMvcBuilders.standaloneSetup(
            org.example.route.controller.PublicRouteController(publicRoutes))
            .setMessageConverters(org.springframework.http.converter.json.MappingJackson2HttpMessageConverter(objectMapper)).build()
        val response = mvc.perform(org.springframework.test.web.servlet.request.MockMvcRequestBuilders
            .get("/api/v1/public-routes/${route.id}"))
            .andExpect(org.springframework.test.web.servlet.result.MockMvcResultMatchers.status().isOk).andReturn()
        val currentVersion = objectMapper.readTree(response.response.contentAsString).path("data").path("currentVersion")
        assertEquals((1..5).toList(), currentVersion.path("referenceDays").map { it.path("dayNumber").asInt() })
        assertTrue(currentVersion.path("referenceDays").all { it.path("description").asText().isNotBlank() })
        assertEquals(5, currentVersion.path("segments").size())
        assertEquals(5, currentVersion.path("campsites").size())
        assertFalse(currentVersion.path("referenceDays").any { it.has("track_start_index") || it.has("segmentIds") })
    }

    @Test
    fun `generated callback endpoint labels are not published as actual places`() {
        val route = route()
        // Reserve the fixture in this test transaction; binding uses REQUIRES_NEW.
        route.beginAnalysis("placeholder-task")
        routes.saveAndFlush(route)
        val raw = """{"route_id":"${route.id}","task_id":"placeholder-task","status":"completed",
          "track_path":[[30,100,4000],[30.1,100.1,4100]],
          "segment_schemes":[
            {"scheme_type":"day","label":"按天","segments":[
              {"id":"day","name":"第1天","sequence_number":1,"color":"#2196F3","distance":1,
               "elevation_gain":100,"elevation_loss":0,"estimated_time":30,"difficulty":1,
               "track_start_index":0,"track_end_index":1,
               "start_point":{"latitude":30,"longitude":100},"end_point":{"latitude":30.1,"longitude":100.1}}]},
            {"scheme_type":"slope","label":"按坡度","segments":[
              {"id":"slope","name":"爬升段","sequence_number":1,"color":"#2196F3","distance":1,
               "elevation_gain":100,"elevation_loss":0,"estimated_time":30,"difficulty":1,
               "track_start_index":0,"track_end_index":1,
               "start_point":{"latitude":30,"longitude":100},"end_point":{"latitude":30.1,"longitude":100.1}}]}]}"""
        callback.handleCallback(objectMapper.readValue(raw, org.example.route.dto.KmlAnalysisCallbackRequest::class.java))
        editing.adoptAllSegments(route.id)
        assertTrue(waypoints.findByRouteIdOrderBySequenceNumberAsc(route.id).any { it.name == "第1天 起点" })
        val candidate = trackReview.read(route.id)
        trackReview.submit(route.id, org.example.route.dto.MainTrackReviewRequest(
            requireNotNull(candidate.candidateId), candidate.reviewRevision, "placeholder-review", "approved", true, "WGS84"))
        publish(route.id)
        val result = publicRoutes.detail(route.id).currentVersion
        assertNull(result.referenceDays!!.single().start)
        assertNull(result.referenceDays!!.single().end)
        assertNull(result.segments!!.single().start)
        assertNull(result.segments!!.single().end)
        assertEquals(4, waypoints.findByRouteIdOrderBySequenceNumberAsc(route.id).size,
            "publication must not modify original management waypoints")
    }

    @Test
    fun `explicitly named same route waypoints remain available as segment places`() {
        val route = adoptedContent()
        waypoints.saveAndFlush(org.example.route.model.Waypoint("named-start", route.id, "洛绒牛场",
            latitude = 30.0, longitude = 100.0, type = "trailhead", sequenceNumber = 0))
        waypoints.saveAndFlush(org.example.route.model.Waypoint("named-end", route.id, "圣水门",
            latitude = 30.01, longitude = 100.01, type = "segment_split", sequenceNumber = 1))
        for (id in listOf("day-1", "slope-1")) {
            segments.saveAndFlush(segments.findById(id).orElseThrow().copy(startPointId = "named-start", endPointId = "named-end"))
        }
        publish(route.id)
        val result = publicRoutes.detail(route.id).currentVersion
        assertEquals("洛绒牛场", result.referenceDays!!.first().start?.name)
        assertEquals("圣水门", result.referenceDays!!.first().end?.name)
        assertEquals("洛绒牛场", result.segments!!.first().start?.name)
        assertEquals("圣水门", result.segments!!.first().end?.name)
    }

    @Test
    @org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable(named = "WALK_YADING_ANALYSIS_FIXTURE", matches = ".+")
    fun `real Yading analysis survives callback adoption publication and public HTTP projection`() {
        val source = java.nio.file.Path.of(requireNotNull(System.getenv("WALK_YADING_ANALYSIS_FIXTURE")))
        val raw = objectMapper.readTree(java.nio.file.Files.readString(source)) as com.fasterxml.jackson.databind.node.ObjectNode
        val route = route()
        route.beginAnalysis("yading-fixture")
        routes.saveAndFlush(route)
        raw.put("route_id", route.id).put("task_id", "yading-fixture").put("status", "completed")
        callback.handleCallback(objectMapper.treeToValue(raw, org.example.route.dto.KmlAnalysisCallbackRequest::class.java))
        editing.adoptAllSegments(route.id)
        editing.adoptAllPois(route.id)
        val candidate = trackReview.read(route.id)
        trackReview.submit(route.id, org.example.route.dto.MainTrackReviewRequest(
            requireNotNull(candidate.candidateId), candidate.reviewRevision, "yading-track-approval", "approved", true, "WGS84"))
        publish(route.id)
        val detail = publicRoutes.detail(route.id)
        val version = detail.currentVersion
        assertEquals(36838, version.mainTrack!!.path.size)
        assertEquals(5, version.referenceDays!!.size)
        assertEquals((1..5).toList(), version.referenceDays!!.map { it.dayNumber })
        assertEquals(24, version.segments!!.size)
        assertEquals(5, version.campsites!!.size)
        assertEquals(listOf(15480.0, 14940.0, 22800.0, 17040.0, 24300.0), version.referenceDays!!.map { it.estimatedDuration!!.seconds })
        val json = objectMapper.valueToTree<com.fasterxml.jackson.databind.JsonNode>(version)
        assertTrue(json.path("referenceDays").all { it.path("description").asText().isNotBlank() && it.has("mainTrackRange") })
        assertTrue(json.path("segments").all { it.path("description").asText().isNotBlank() && it.has("mainTrackRange") })
        assertEquals(4, version.referenceDays!!.count { it.accommodation != null })
        assertNull(version.referenceDays!![4].accommodation)
        assertEquals(3958.5, version.referenceDays!!.sumOf { it.ascent!!.meters }, 0.11)
        assertEquals(4023.6, version.referenceDays!!.sumOf { it.descent!!.meters }, 0.11)
        assertTrue(json.path("campsites").all { it.path("sourceEvidence").path("confidence").path("status").asText() == "pending_verification" })
        System.getenv("WALK_YADING_PUBLIC_OUTPUT")?.let { destination ->
            val target = java.nio.file.Path.of(destination)
            require(java.nio.file.Files.isDirectory(target.parent) && !java.nio.file.Files.exists(target))
            java.nio.file.Files.writeString(target, objectMapper.writerWithDefaultPrettyPrinter().writeValueAsString(mapOf("data" to detail)),
                java.nio.file.StandardOpenOption.CREATE_NEW)
        }
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
