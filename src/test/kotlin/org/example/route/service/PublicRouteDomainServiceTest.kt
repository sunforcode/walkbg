package org.example.route.service

import com.fasterxml.jackson.databind.DeserializationFeature
import com.fasterxml.jackson.databind.JsonNode
import org.example.common.contract.ApiContractException
import org.example.config.JacksonConfig
import org.example.route.model.PublicRouteCollectionEntry
import org.example.route.model.RouteCurrentPublicVersion
import org.example.route.model.RouteVersion
import org.example.route.model.RouteVersionEquipmentSuggestion
import org.example.route.model.RouteVersionPoint
import org.example.route.model.RouteVersionPublicationOrder
import org.example.route.model.RouteVersionSegment
import org.example.route.repository.PublicRouteCollectionRepository
import org.example.route.repository.RouteCurrentPublicVersionRepository
import org.example.route.repository.RouteVersionEquipmentSuggestionRepository
import org.example.route.repository.RouteVersionImageRepository
import org.example.route.repository.RouteVersionPointRepository
import org.example.route.repository.RouteVersionPublicationOrderRepository
import org.example.route.repository.RouteVersionRepository
import org.example.route.repository.RouteVersionSegmentRepository
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.mockito.kotlin.mock
import org.mockito.kotlin.whenever
import java.time.Instant
import java.util.Optional

class PublicRouteDomainServiceTest {
    private val collectionRepository = mock<PublicRouteCollectionRepository>()
    private val currentVersionRepository = mock<RouteCurrentPublicVersionRepository>()
    private val versionRepository = mock<RouteVersionRepository>()
    private val imageRepository = mock<RouteVersionImageRepository>()
    private val segmentRepository = mock<RouteVersionSegmentRepository>()
    private val pointRepository = mock<RouteVersionPointRepository>()
    private val objectMapper = JacksonConfig().objectMapper()
    private val service = PublicRouteDomainService(
        collectionRepository,
        currentVersionRepository,
        versionRepository,
        imageRepository,
        segmentRepository,
        pointRepository,
        objectMapper,
        RouteVersionSummaryPlaceResolver()
    )

    @Test
    fun `missing explicit current public version is a read failure`() {
        whenever(collectionRepository.findById("route-1")).thenReturn(
            Optional.of(PublicRouteCollectionEntry("route-1", 1))
        )
        whenever(currentVersionRepository.findById("route-1")).thenReturn(Optional.empty())

        val error = assertThrows<ApiContractException> { service.findPublicVersion("route-1") }

        assertEquals("public_route_read_failed", error.code)
        assertEquals(503, error.status.value())
    }

    @Test
    fun `current version owned by another route is a read failure`() {
        whenever(collectionRepository.findById("route-1")).thenReturn(
            Optional.of(PublicRouteCollectionEntry("route-1", 1))
        )
        whenever(currentVersionRepository.findById("route-1")).thenReturn(
            Optional.of(RouteCurrentPublicVersion("route-1", "version-2"))
        )
        whenever(versionRepository.findById("version-2")).thenReturn(
            Optional.of(version(routeId = "route-2"))
        )

        val error = assertThrows<ApiContractException> { service.findPublicVersion("route-1") }

        assertEquals("public_route_read_failed", error.code)
    }

    @Test
    fun `valid main track is projected with declared reference system`() {
        val version = version(
            mainTrackAvailability = "valid",
            mainTrackReferenceSystem = "WGS84",
            mainTrackJson = "[[30.1, 102.2, 3200], [30.2, 102.3, null]]"
        )
        stubDetailCollections(version)

        val detail = service.detail(version)

        assertEquals("version-1:main-track", detail.currentVersion.mainTrack?.identity)
        assertEquals(2, detail.currentVersion.mainTrack?.path?.size)
        assertEquals("WGS84", detail.currentVersion.mainTrack?.path?.first()?.referenceSystem)
    }

    @Test
    fun `malformed payload declared as valid track is a read failure`() {
        val version = version(
            mainTrackAvailability = "valid",
            mainTrackReferenceSystem = "WGS84",
            mainTrackJson = "[[91, 102.2]]"
        )
        stubDetailCollections(version)

        val error = assertThrows<ApiContractException> { service.detail(version) }

        assertEquals("public_route_read_failed", error.code)
    }

    @Test
    fun `generation eligibility reasons use normative order`() {
        val version = version(mainTrackAvailability = "missing")
        stubDetailCollections(version)

        val eligibility = service.detail(version).currentVersion.generationEligibility

        assertEquals(false, eligibility.eligible)
        assertEquals(
            listOf("name", "region", "estimatedDuration", "start", "end", "validMainTrack"),
            eligibility.missingReasons
        )
    }

    @Test
    fun `campsite subtype is not reinterpreted as campsite status`() {
        val version = version(mainTrackAvailability = "missing")
        stubDetailCollections(
            version,
            points = listOf(
                RouteVersionPoint(
                    id = "camp-1",
                    routeVersionId = version.id,
                    pointKind = "campsite",
                    displayOrder = 1,
                    name = "山谷营地",
                    category = "camp",
                    subCategory = "wild",
                    latitude = 30.1,
                    longitude = 102.2,
                    referenceSystem = "WGS84"
                )
            )
        )

        val campsite = service.detail(version).currentVersion.campsites?.single()

        assertNull(campsite?.status)
    }

    @Test
    fun `unknown main track availability is rejected instead of projected`() {
        val version = version(mainTrackAvailability = "available")
        stubDetailCollections(version)

        val error = assertThrows<ApiContractException> { service.detail(version) }

        assertEquals("public_route_read_failed", error.code)
    }

    @Test
    fun `detail projects stored stable optional facts and only explicit segment range`() {
        val version = version(
            mainTrackAvailability = "valid",
            mainTrackReferenceSystem = "WGS84",
            mainTrackJson = "[[30.1,102.2],[30.2,102.3]]"
        ).copy(
            routeType = "multi_day",
            startName = "起点",
            endName = "终点",
            maxElevationMeters = java.math.BigDecimal("4680"),
            suggestedDays = 2,
            tagsJson = "[\"高山\",\"环线\"]",
            professionalAnalysisJson = "{\"mainTerrain\":{\"value\":\"高山草甸\"}}",
            referenceDaysJson = "[{\"identity\":\"reference-day-1\",\"dayNumber\":1,\"title\":\"D1\"}]",
            seasonalWeatherJson = "{\"bestSeasons\":[\"秋季\"]}",
            seasonalEquipmentRecommendationsJson = "[{\"identity\":\"seasonal-equipment-1\",\"seasonOrCondition\":\"秋季\",\"name\":\"冲锋衣\",\"level\":\"recommended\"}]"
        )
        whenever(pointRepository.findByRouteVersionIdOrderByDisplayOrderAsc(version.id)).thenReturn(
            listOf(
                RouteVersionPoint("start", version.id, "start", 1, "起点", latitude = 30.1, longitude = 102.2, referenceSystem = "WGS84"),
                RouteVersionPoint("end", version.id, "end", 2, "终点", latitude = 30.2, longitude = 102.3, referenceSystem = "WGS84"),
                RouteVersionPoint("overnight", version.id, "overnight_place", 3, "山屋", subCategory = "hut", latitude = 30.15, longitude = 102.25, referenceSystem = "WGS84")
            )
        )
        whenever(imageRepository.findByRouteVersionIdOrderByDisplayOrderAsc(version.id)).thenReturn(emptyList())
        whenever(segmentRepository.findByRouteVersionIdOrderBySegmentOrderAsc(version.id)).thenReturn(
            listOf(
                RouteVersionSegment(
                    id = "segment-1",
                    routeVersionId = version.id,
                    segmentOrder = 1,
                    name = "第一段",
                    mainTrackRangeJson = "{\"startPathPosition\":{\"precedingPositionIndex\":0,\"progressToNextPosition\":0.25},\"endPathPosition\":{\"precedingPositionIndex\":1}}"
                ),
                RouteVersionSegment("segment-2", version.id, 2, "第二段")
            )
        )

        val detail = service.detail(version).currentVersion

        assertEquals(4680.0, detail.summary.maxElevation?.meters)
        assertEquals(listOf("高山", "环线"), detail.summary.tags)
        assertEquals("高山草甸", detail.professionalAnalysis?.mainTerrain?.value)
        assertEquals("reference-day-1", detail.referenceDays?.single()?.identity)
        assertEquals("山屋", detail.overnightPlaces?.single()?.name)
        assertEquals(listOf("秋季"), detail.seasonalWeather?.bestSeasons)
        assertEquals("冲锋衣", detail.seasonalEquipmentRecommendations?.single()?.name)
        assertEquals(0, detail.segments?.first()?.mainTrackRange?.startPathPosition?.precedingPositionIndex)
        assertNull(detail.segments?.last()?.mainTrackRange)
    }

    @Test
    fun `one day detail omits stored reference days`() {
        val version = version().copy(
            routeType = "one_day",
            referenceDaysJson = "[{\"identity\":\"reference-day-1\",\"dayNumber\":1}]"
        )
        stubDetailCollections(version)

        assertNull(service.detail(version).currentVersion.referenceDays)
    }

    @Test
    fun `detail keeps each named point collection separate and ordered`() {
        val version = version()
        stubDetailCollections(
            version,
            points = listOf(
                point("key-1", version.id, "key", 1, "垭口"),
                point("interest-1", version.id, "interest", 2, "冰川"),
                point("camp-1", version.id, "campsite", 3, "营地"),
                point("overnight-1", version.id, "overnight_place", 4, "山屋"),
                point("water-1", version.id, "water_source", 5, "溪流"),
                point("supply-1", version.id, "supply_point", 6, "补给站"),
                point("notice-1", version.id, "safety_notice", 7, "落石区", category = "hazard", description = "快速通过")
            )
        )

        val detail = service.detail(version).currentVersion

        assertEquals(listOf("key-1"), detail.keyPoints?.map { it.identity })
        assertEquals(listOf("interest-1"), detail.interestPoints?.map { it.identity })
        assertEquals(listOf("camp-1"), detail.campsites?.map { it.identity })
        assertEquals(listOf("overnight-1"), detail.overnightPlaces?.map { it.identity })
        assertEquals(listOf("water-1"), detail.waterSources?.map { it.identity })
        assertEquals(listOf("supply-1"), detail.supplyPoints?.map { it.identity })
        assertEquals(listOf("notice-1"), detail.communicationAndSafety?.notices?.map { it.identity })
    }

    @Test
    fun `empty stored optional structures are omitted`() {
        val version = version().copy(
            professionalAnalysisJson = "{}",
            seasonalWeatherJson = "{}",
            seasonalEquipmentRecommendationsJson = "[]",
            tagsJson = "[]"
        )
        stubDetailCollections(version)

        val detail = service.detail(version).currentVersion

        assertNull(detail.professionalAnalysis)
        assertNull(detail.seasonalWeather)
        assertNull(detail.seasonalEquipmentRecommendations)
        assertNull(detail.summary.tags)
    }

    @Test
    fun `segment range outside the explicit main track is rejected`() {
        val version = version(
            mainTrackAvailability = "valid",
            mainTrackReferenceSystem = "WGS84",
            mainTrackJson = "[[30.1,102.2],[30.2,102.3]]"
        )
        whenever(pointRepository.findByRouteVersionIdOrderByDisplayOrderAsc(version.id)).thenReturn(emptyList())
        whenever(imageRepository.findByRouteVersionIdOrderByDisplayOrderAsc(version.id)).thenReturn(emptyList())
        whenever(segmentRepository.findByRouteVersionIdOrderBySegmentOrderAsc(version.id)).thenReturn(
            listOf(
                RouteVersionSegment(
                    id = "segment-1",
                    routeVersionId = version.id,
                    segmentOrder = 1,
                    name = "越界分段",
                    mainTrackRangeJson = "{\"startPathPosition\":{\"precedingPositionIndex\":0,\"progressToNextPosition\":0.5},\"endPathPosition\":{\"precedingPositionIndex\":2}}"
                )
            )
        )

        val error = assertThrows<ApiContractException> { service.detail(version) }

        assertEquals("public_route_read_failed", error.code)
    }

    @Test
    fun `segment range requires progress for a non-final path position`() {
        val version = version(
            mainTrackAvailability = "valid",
            mainTrackReferenceSystem = "WGS84",
            mainTrackJson = "[[30.1,102.2],[30.2,102.3],[30.3,102.4]]"
        )
        whenever(pointRepository.findByRouteVersionIdOrderByDisplayOrderAsc(version.id)).thenReturn(emptyList())
        whenever(imageRepository.findByRouteVersionIdOrderByDisplayOrderAsc(version.id)).thenReturn(emptyList())
        whenever(segmentRepository.findByRouteVersionIdOrderBySegmentOrderAsc(version.id)).thenReturn(
            listOf(
                RouteVersionSegment(
                    id = "segment-1",
                    routeVersionId = version.id,
                    segmentOrder = 1,
                    name = "缺少进度分段",
                    mainTrackRangeJson = "{\"startPathPosition\":{\"precedingPositionIndex\":0},\"endPathPosition\":{\"precedingPositionIndex\":2}}"
                )
            )
        )

        val error = assertThrows<ApiContractException> { service.detail(version) }

        assertEquals("public_route_read_failed", error.code)
    }

    @Test
    fun `publication order reads an explicit sequence and refuses cross route versions`() {
        val repository = mock<RouteVersionPublicationOrderRepository>()
        whenever(repository.findByRouteVersionId("version-1")).thenReturn(
            RouteVersionPublicationOrder("route-1", "version-1", 1)
        )
        whenever(repository.findByRouteVersionId("foreign-version")).thenReturn(
            RouteVersionPublicationOrder("route-2", "foreign-version", 2)
        )

        assertEquals(1, repository.findByRouteVersionId("version-1")?.publishedSequence)
        assertEquals("route-2", repository.findByRouteVersionId("foreign-version")?.routeId)
    }

    @Test
    fun `equipment suggestions preserve version order and explicit route scoped logical identity`() {
        val repository = mock<RouteVersionEquipmentSuggestionRepository>()
        val suggestion = RouteVersionEquipmentSuggestion(
            id = "suggestion-1",
            routeId = "route-1",
            routeVersionId = "version-1",
            logicalSuggestionId = "logical-1",
            displayOrder = 1,
            name = "Tent Bag",
            normalizedName = "tent bag",
            quantity = 1,
            unitWeightGrams = 900,
            note = "四季帐",
            level = "required"
        )
        whenever(repository.findByRouteVersionIdOrderByDisplayOrderAsc("version-1"))
            .thenReturn(listOf(suggestion))

        val loaded = repository.findByRouteVersionIdOrderByDisplayOrderAsc("version-1").single()
        assertEquals("route-1", loaded.routeId)
        assertEquals("logical-1", loaded.logicalSuggestionId)
        assertEquals("tent bag", loaded.normalizedName)
        assertEquals("required", loaded.level)
    }

    // publish-adopted-route-day-content: public-route-api / route-schema.
    @Test
    fun `reference days preserve descriptions evidence ranges and RFC3339 metadata`() {
        val version = referenceVersion("""[
            {"identity":"day-1","dayNumber":1,"title":"第一天","description":"第一天原路线说明",
             "notes":"原记录提示涉水","accommodation":"原记录：甲营地",
             "accommodationEvidence":{"value":"原记录：甲营地","confidence":{
                 "status":"pending_verification","category":"public_route_fact","source":"原轨迹标注",
                 "updatedAt":"2026-09-29T08:00:00+08:00"}},
             "mainTrackRange":${rangeJson(0, 1)}},
            {"identity":"day-2","dayNumber":2,"description":"第二天独立说明",
             "mainTrackRange":${rangeJson(1, 4)}}
        ]""")
        stubDetailCollections(version)

        val detail = service.detail(version).currentVersion
        val days = requireNotNull(detail.referenceDays)
        assertEquals(listOf(1, 2), days.map { it.dayNumber })
        assertEquals("第一天原路线说明", days.first().description)
        assertEquals("原记录提示涉水", days.first().notes)
        assertEquals(days.first().accommodation, days.first().accommodationEvidence?.value)
        assertEquals("pending_verification", days.first().accommodationEvidence?.confidence?.status)
        assertEquals("public_route_fact", days.first().accommodationEvidence?.confidence?.category)
        assertEquals("原轨迹标注", days.first().accommodationEvidence?.confidence?.source)
        assertEquals(Instant.parse("2026-09-29T00:00:00Z"), days.first().accommodationEvidence?.confidence?.updatedAt)
        assertEquals(days.first().mainTrackRange?.endPathPosition, days.last().mainTrackRange?.startPathPosition)
        assertNull(days.last().mainTrackRange?.endPathPosition?.progressToNextPosition)
        val json = objectMapper.valueToTree<JsonNode>(detail)
        assertEquals("2026-09-29T00:00:00Z", json["referenceDays"][0]["accommodationEvidence"]["confidence"]["updatedAt"].textValue())
        assertEquals(false, json["referenceDays"][1].has("accommodationEvidence"))
    }

    @Test
    fun `campsite source evidence stays qualified instead of becoming campsite status`() {
        val version = version()
        val evidence = """{"value":"原记录：甲营地，途经而非过夜","confidence":{
            "status":"pending_verification","category":"public_route_fact","source":"原轨迹标注"}}"""
        val camp = point("camp-1", version.id, "campsite", 1, "甲营地", description = "原记录营地说明")
            .copy(sourceEvidenceJson = evidence)
        stubDetailCollections(version, listOf(camp))

        val result = requireNotNull(service.detail(version).currentVersion.campsites).single()
        assertEquals("原记录营地说明", result.details)
        assertEquals("原记录：甲营地，途经而非过夜", result.sourceEvidence?.value)
        assertEquals("pending_verification", result.sourceEvidence?.confidence?.status)
        assertEquals("原轨迹标注", result.sourceEvidence?.confidence?.source)
        assertNull(result.sourceEvidence?.confidence?.updatedAt)
        assertNull(result.status)
        assertEquals(objectMapper.readTree(evidence), objectMapper.valueToTree<JsonNode>(result.sourceEvidence))
    }

    @Test
    fun `legacy reference days and campsites omit the newly optional fields`() {
        val version = referenceVersion("""[{"identity":"day-1","dayNumber":1,"title":"旧标题",
            "accommodation":"旧住宿说明","notes":"旧注意事项"}]""").copy(
            mainTrackAvailability = "pending_review", mainTrackJson = null, mainTrackReferenceSystem = null
        )
        stubDetailCollections(version, listOf(point("camp-1", version.id, "campsite", 1, "旧营地")))

        val detail = service.detail(version).currentVersion
        val day = requireNotNull(detail.referenceDays).single()
        assertEquals("旧住宿说明", day.accommodation)
        assertEquals("旧注意事项", day.notes)
        assertNull(day.description)
        assertNull(day.accommodationEvidence)
        assertNull(day.mainTrackRange)
        val json = objectMapper.valueToTree<JsonNode>(detail)
        for (field in listOf("description", "accommodationEvidence", "mainTrackRange")) {
            assertEquals(false, json["referenceDays"][0].has(field), field)
        }
        assertEquals(false, json["campsites"][0].has("sourceEvidence"))
    }

    @Test
    fun `reference day range requires a valid main track`() {
        for (availability in listOf("missing", "processing", "pending_review", "invalidated")) {
            val version = referenceVersion("""[{"identity":"day-1","dayNumber":1,
                "mainTrackRange":${rangeJson(0, 4)}}]""").copy(mainTrackAvailability = availability)
            assertDetailReadFailure(version)
        }
    }

    @Test
    fun `malformed reference day ranges fail instead of being omitted or coerced`() {
        val ranges = listOf(
            rangeJson(0, 5), rangeJson(0, 0), rangeJson(3, 1), rangeJson(-1, 4),
            """{"startPathPosition":{"precedingPositionIndex":0},"endPathPosition":{"precedingPositionIndex":4}}""",
            """{"startPathPosition":{"precedingPositionIndex":0,"progressToNextPosition":0},"endPathPosition":{"precedingPositionIndex":4,"progressToNextPosition":0}}""",
            rangeJson(0, 4).replace("\"progressToNextPosition\":0", "\"progressToNextPosition\":1"),
            rangeJson(0, 4).replace("\"progressToNextPosition\":0", "\"progressToNextPosition\":-0.1"),
            rangeJson(0, 4).replace("\"progressToNextPosition\":0", "\"progressToNextPosition\":1e309"),
            rangeJson(0, 4).replace("\"precedingPositionIndex\":0", "\"precedingPositionIndex\":0.5"),
            rangeJson(0, 4).replace("\"precedingPositionIndex\":0", "\"precedingPositionIndex\":\"0\""),
            rangeJson(0, 4).replace("\"precedingPositionIndex\":0", "\"precedingPositionIndex\":null"),
            rangeJson(0, 4).replace("\"progressToNextPosition\":0", "\"progressToNextPosition\":\"0\""),
            rangeJson(0, 4).replace("\"precedingPositionIndex\":4", "\"precedingPositionIndex\":4,\"segmentId\":\"internal\""),
            "{}", "[]"
        )
        for (range in ranges) {
            assertDetailReadFailure(referenceVersion("""[{"identity":"day-1","dayNumber":1,"mainTrackRange":$range}]"""))
        }
    }

    @Test
    fun `reference day ranges cannot overlap or reverse even across days without a range`() {
        val invalidDays = listOf(
            """[{"identity":"day-1","dayNumber":1,"mainTrackRange":${rangeJson(0, 2)}},
                {"identity":"day-2","dayNumber":2,"mainTrackRange":${rangeJson(1, 4)}}]""",
            """[{"identity":"day-1","dayNumber":1,"mainTrackRange":${rangeJson(3, 4)}},
                {"identity":"day-2","dayNumber":2,"mainTrackRange":${rangeJson(0, 1)}}]""",
            """[{"identity":"day-1","dayNumber":1,"mainTrackRange":${rangeJson(0, 2)}},
                {"identity":"day-2","dayNumber":2},
                {"identity":"day-3","dayNumber":3,"mainTrackRange":${rangeJson(1, 4)}}]"""
        )
        invalidDays.forEach { assertDetailReadFailure(referenceVersion(it)) }
    }

    @Test
    fun `reference day partial ranges preserve gaps without inventing a missing range`() {
        val version = referenceVersion("""[
            {"identity":"day-1","dayNumber":1,"mainTrackRange":${rangeJson(0, 1)}},
            {"identity":"day-2","dayNumber":2,"description":"只有来源说明"},
            {"identity":"day-3","dayNumber":3,"mainTrackRange":${rangeJson(2, 4)}}]""")
        stubDetailCollections(version)

        val days = requireNotNull(service.detail(version).currentVersion.referenceDays)
        assertNull(days[1].mainTrackRange)
        assertEquals("只有来源说明", days[1].description)
        assertEquals(1, days.first().mainTrackRange?.endPathPosition?.precedingPositionIndex)
        assertEquals(2, days.last().mainTrackRange?.startPathPosition?.precedingPositionIndex)
    }

    @Test
    fun `reference day identity and order are validated without renumbering`() {
        val invalidDays = listOf(
            """[{"identity":"day-0","dayNumber":0}]""",
            """[{"identity":"day-1","dayNumber":1},{"identity":"day-3","dayNumber":3}]""",
            """[{"identity":"day-2","dayNumber":2},{"identity":"day-1","dayNumber":1}]""",
            """[{"identity":"day-1","dayNumber":1},{"identity":"day-2","dayNumber":1}]""",
            """[{"identity":"same","dayNumber":1},{"identity":"same","dayNumber":2}]""",
            """[{"identity":" ","dayNumber":1}]""",
            """[{"identity":"day-1","dayNumber":1.5}]""",
            """[{"identity":"day-1","dayNumber":"1"}]""",
            """[{"identity":"day-1","dayNumber":null}]"""
        )
        invalidDays.forEach { assertDetailReadFailure(referenceVersion(it)) }
    }

    @Test
    fun `invalid qualified evidence fails for both accommodation and campsite sources`() {
        val invalidEvidence = listOf(
            "{}", "[]", "null", " ",
            """{"value":""}""", """{"value":"  "}""", """{"value":123}""",
            """{"value":"原记录","confidence":{"status":"verified","category":"public_route_fact"}}""",
            """{"value":"原记录","confidence":{"status":"pending_verification"}}""",
            """{"value":"原记录","confidence":{"status":"pending_verification","category":"camp"}}""",
            """{"confidence":{"status":"pending_verification","category":"public_route_fact"}}""",
            """{"confidence":{"status":"stale","category":"public_route_fact"}}""",
            """{"value":"原记录","confidence":{"status":"pending_verification","category":"public_route_fact","source":" "}}""",
            """{"value":"原记录","confidence":{"status":"pending_verification","category":"public_route_fact","source":1}}""",
            """{"value":"原记录","confidence":{"status":"pending_verification","category":"public_route_fact","updatedAt":"yesterday"}}""",
            """{"value":"原记录","confidence":{"status":"pending_verification","category":"public_route_fact","updatedAt":"2026-09-29T08:00:00"}}""",
            """{"value":"原记录","confidence":{"status":"pending_verification","category":"public_route_fact","updatedAt":1790000000}}""",
            """{"value":"原记录","confidence":[]}""",
            """{"value":"原记录","campId":"internal"}""",
            """{"value":"原记录","confidence":{"status":"pending_verification","category":"public_route_fact","requestId":"internal"}}""",
            """{"value":"原记录","value":"替代文字"}""",
            """{"value":"原记录"} {"unexpected":true}"""
        )
        for (evidence in invalidEvidence) {
            val version = version()
            val camp = point("camp-1", version.id, "campsite", 1, "营地").copy(sourceEvidenceJson = evidence)
            assertDetailReadFailure(version, listOf(camp))
            // Missing optional evidence is compatible; an explicit corrupt payload is not.
            if (evidence != "null") {
                assertDetailReadFailure(referenceVersion("""[{"identity":"day-1","dayNumber":1,
                    "accommodation":"原记录","accommodationEvidence":$evidence}]"""))
            }
        }
    }

    @Test
    fun `qualified source evidence supports shared statuses and optional metadata without defaults`() {
        for (category in listOf("public_route_fact", "dynamic_external_information", "generated_suggestion")) {
            for (status in listOf("unknown", "missing", "pending_verification", "stale", "unavailable")) {
                val version = version()
                val evidence = """{"value":"原记录","confidence":{"status":"$status","category":"$category"}}"""
                stubDetailCollections(version, listOf(point("camp-1", version.id, "campsite", 1, "营地").copy(sourceEvidenceJson = evidence)))
                val loaded = requireNotNull(service.detail(version).currentVersion.campsites).single().sourceEvidence
                assertEquals(status, loaded?.confidence?.status)
                assertEquals(category, loaded?.confidence?.category)
                assertNull(loaded?.confidence?.source)
                assertNull(loaded?.confidence?.updatedAt)
            }
        }
        for (evidence in listOf("""{"value":"只有原文"}""", """{"confidence":{"status":"unknown","category":"public_route_fact"}}""")) {
            val version = version()
            stubDetailCollections(version, listOf(point("camp-1", version.id, "campsite", 1, "营地").copy(sourceEvidenceJson = evidence)))
            val loaded = requireNotNull(service.detail(version).currentVersion.campsites).single().sourceEvidence
            assertEquals(objectMapper.readTree(evidence), objectMapper.valueToTree<JsonNode>(loaded))
        }
    }

    @Test
    fun `accommodation evidence requires an identical existing accommodation value`() {
        val evidence = """{"value":"原记录：甲营地","confidence":{"status":"pending_verification","category":"public_route_fact"}}"""
        for (accommodation in listOf("", "\"accommodation\":null,", "\"accommodation\":\"乙营地\",", "\"accommodation\":\"原记录：甲营地 \",")) {
            assertDetailReadFailure(referenceVersion("""[{"identity":"day-1","dayNumber":1,
                $accommodation "accommodationEvidence":$evidence}]"""))
        }
        assertDetailReadFailure(referenceVersion("""[{"identity":"day-1","dayNumber":1,"accommodation":"甲营地",
            "accommodationEvidence":{"confidence":{"status":"unknown","category":"public_route_fact"}}}]"""))
    }

    @Test
    fun `new reference day payload rejects unknown fields trailing JSON and invalid description`() {
        objectMapper.disable(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES)
        val invalidDays = listOf(
            "{}", "null", "[null]",
            """[{"identity":"day-1","dayNumber":1,"description":" "}]""",
            """[{"identity":"day-1","dayNumber":1,"description":123}]""",
            """[{"identity":"day-1","dayNumber":1,"campId":"internal"}]""",
            """[{"identity":"day-1","dayNumber":1}] {"unexpected":true}"""
        )
        invalidDays.forEach { assertDetailReadFailure(referenceVersion(it)) }
    }

    @Test
    fun `new evidence validation does not tighten legacy professional analysis`() {
        val version = version().copy(
            professionalAnalysisJson = """{"mainTerrain":{"value":"历史地形说明","confidence":{"status":"pending_verification"}}}"""
        )
        stubDetailCollections(version)
        val terrain = service.detail(version).currentVersion.professionalAnalysis?.mainTerrain
        assertEquals("历史地形说明", terrain?.value)
        assertNull(terrain?.confidence?.category)
    }

    private fun referenceVersion(days: String) = version(
        mainTrackAvailability = "valid",
        mainTrackReferenceSystem = "WGS84",
        mainTrackJson = "[[30,120],[30.1,120.1],[30.2,120.2],[30.3,120.3],[30.4,120.4]]"
    ).copy(routeType = "multi_day", referenceDaysJson = days)

    private fun rangeJson(start: Int, end: Int): String {
        fun position(index: Int) = if (index == 4) """{"precedingPositionIndex":$index}"""
            else """{"precedingPositionIndex":$index,"progressToNextPosition":0}"""
        return """{"startPathPosition":${position(start)},"endPathPosition":${position(end)}}"""
    }

    private fun assertDetailReadFailure(version: RouteVersion, points: List<RouteVersionPoint> = emptyList()) {
        stubDetailCollections(version, points)
        val error = assertThrows<ApiContractException> { service.detail(version) }
        assertEquals("public_route_read_failed", error.code)
        assertEquals(503, error.status.value())
    }

    private fun point(
        id: String,
        routeVersionId: String,
        kind: String,
        order: Int,
        name: String,
        category: String? = null,
        description: String? = null
    ) = RouteVersionPoint(
        id = id,
        routeVersionId = routeVersionId,
        pointKind = kind,
        displayOrder = order,
        name = name,
        category = category,
        description = description,
        latitude = 30.0 + order / 100.0,
        longitude = 101.0 + order / 100.0,
        referenceSystem = "WGS84"
    )

    private fun stubDetailCollections(
        version: RouteVersion,
        points: List<RouteVersionPoint> = emptyList()
    ) {
        whenever(pointRepository.findByRouteVersionIdOrderByDisplayOrderAsc(version.id)).thenReturn(points)
        whenever(imageRepository.findByRouteVersionIdOrderByDisplayOrderAsc(version.id)).thenReturn(emptyList())
        whenever(segmentRepository.findByRouteVersionIdOrderBySegmentOrderAsc(version.id)).thenReturn(emptyList())
    }

    private fun version(
        routeId: String = "route-1",
        mainTrackAvailability: String = "missing",
        mainTrackReferenceSystem: String? = null,
        mainTrackJson: String? = null
    ) = RouteVersion(
        id = "version-1",
        routeId = routeId,
        routeType = "one_day",
        mainTrackAvailability = mainTrackAvailability,
        mainTrackReferenceSystem = mainTrackReferenceSystem,
        mainTrackJson = mainTrackJson
    )
}
