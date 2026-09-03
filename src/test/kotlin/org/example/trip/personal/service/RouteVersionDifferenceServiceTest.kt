package org.example.trip.personal.service

import org.example.route.model.RouteVersion
import org.example.route.model.RouteVersionEquipmentSuggestion
import org.example.route.model.RouteVersionSegment
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Test
import java.math.BigDecimal

class RouteVersionDifferenceServiceTest {
    private val service = RouteVersionDifferenceService()

    @Test
    fun `equal comparable values produce no placeholder differences`() {
        val version = version()

        val result = service.compare(
            version,
            version,
            beforeSegments = emptyList(),
            afterSegments = emptyList(),
            beforeSuggestions = emptyList(),
            afterSuggestions = emptyList()
        )

        assertEquals("partial", result.minimumComparisonStatus)
        assertEquals(emptyList<Any>(), result.differences)
        assertEquals(listOf("planning_support_and_safety"), result.unavailableCategories)
        assertEquals("none", result.otherRelevantChangeStatus)
        assertEquals(emptyList<String>(), result.otherRelevantChanges)
    }

    @Test
    fun `scalar changes use added removed and modified with exact before after shape`() {
        val before = version(name = "旧路线", region = null, difficulty = "moderate")
        val after = version(name = "新路线", region = "四川", difficulty = null)

        val result = service.compare(before, after, emptyList(), emptyList(), emptyList(), emptyList())

        val bySubject = result.differences.associateBy { it.subject }
        assertEquals("modified", bySubject.getValue("route_name").changeType)
        assertEquals("旧路线", bySubject.getValue("route_name").before?.value)
        assertEquals("新路线", bySubject.getValue("route_name").after?.value)
        assertEquals("added", bySubject.getValue("region").changeType)
        assertNull(bySubject.getValue("region").before)
        assertEquals("四川", bySubject.getValue("region").after?.value)
        assertEquals("no_longer_provided", bySubject.getValue("difficulty").changeType)
        assertEquals("moderate", bySubject.getValue("difficulty").before?.value)
        assertNull(bySubject.getValue("difficulty").after)
    }

    @Test
    fun `track and segments compare complete ordered structures without cross version item matching`() {
        val before = version(track = "[[30.0,101.0],[30.1,101.1]]")
        val after = version(track = "[[30.0,101.0],[30.2,101.2]]")
        val beforeSegments = listOf(segment("before-segment", "旧段"))
        val afterSegments = listOf(segment("after-segment", "新段"))

        val result = service.compare(before, after, beforeSegments, afterSegments, emptyList(), emptyList())

        assertEquals(
            listOf("main_track", "segments"),
            result.differences.filter { it.category == "track_and_segments" }.map { it.subject }
        )
        assertEquals("modified", result.differences.first { it.subject == "segments" }.changeType)
    }

    @Test
    fun `equipment comparison follows explicit logical identities and stable target order`() {
        val beforeSuggestions = listOf(
            suggestion("before-tent", "logical-tent", 1, "帐篷", 1),
            suggestion("before-stove", "logical-stove", 2, "炉头", 1)
        )
        val afterSuggestions = listOf(
            suggestion("after-tent", "logical-tent", 1, "四季帐", 1),
            suggestion("after-water", "logical-water", 2, "净水器", 1)
        )

        val result = service.compare(
            version(),
            version(id = "version-2"),
            emptyList(),
            emptyList(),
            beforeSuggestions,
            afterSuggestions
        )

        val equipment = result.differences.filter { it.category == "seasonal_preparation_and_equipment" }
        assertEquals(listOf("logical-tent", "logical-water", "logical-stove"), equipment.map { it.subject })
        assertEquals(listOf("modified", "added", "no_longer_provided"), equipment.map { it.changeType })
    }

    private fun version(
        id: String = "version-1",
        name: String? = "路线",
        region: String? = "地区",
        difficulty: String? = "easy",
        track: String? = "[[30.0,101.0],[30.1,101.1]]"
    ) = RouteVersion(
        id = id,
        routeId = "route-1",
        routeType = "one_day",
        name = name,
        region = region,
        startName = "起点",
        endName = "终点",
        estimatedDurationSeconds = 28_800,
        difficulty = difficulty,
        direction = "往返",
        distanceMeters = BigDecimal("10000"),
        ascentMeters = BigDecimal("500"),
        descentMeters = BigDecimal("500"),
        mainTrackAvailability = "valid",
        mainTrackReferenceSystem = "WGS84",
        mainTrackJson = track
    )

    private fun segment(id: String, name: String) = RouteVersionSegment(
        id = id,
        routeVersionId = if (id.startsWith("before")) "version-1" else "version-2",
        segmentOrder = 1,
        name = name
    )

    private fun suggestion(id: String, logicalId: String, order: Int, name: String, quantity: Int) =
        RouteVersionEquipmentSuggestion(
            id = id,
            routeId = "route-1",
            routeVersionId = if (id.startsWith("before")) "version-1" else "version-2",
            logicalSuggestionId = logicalId,
            displayOrder = order,
            name = name,
            normalizedName = name,
            quantity = quantity,
            level = "required"
        )
}
