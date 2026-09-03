package org.example.route.service

import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import org.example.common.contract.ApiContractException
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
import java.util.Optional

class PublicRouteDomainServiceTest {
    private val collectionRepository = mock<PublicRouteCollectionRepository>()
    private val currentVersionRepository = mock<RouteCurrentPublicVersionRepository>()
    private val versionRepository = mock<RouteVersionRepository>()
    private val imageRepository = mock<RouteVersionImageRepository>()
    private val segmentRepository = mock<RouteVersionSegmentRepository>()
    private val pointRepository = mock<RouteVersionPointRepository>()
    private val service = PublicRouteDomainService(
        collectionRepository,
        currentVersionRepository,
        versionRepository,
        imageRepository,
        segmentRepository,
        pointRepository,
        jacksonObjectMapper(),
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
