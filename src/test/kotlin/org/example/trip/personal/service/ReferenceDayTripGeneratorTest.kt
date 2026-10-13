package org.example.trip.personal.service

import com.fasterxml.jackson.databind.JsonNode
import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.databind.node.ObjectNode
import org.example.config.JacksonConfig
import org.example.route.dto.PublicRouteGeoPosition
import org.example.route.dto.PublicRoutePlace
import org.example.route.dto.RouteSeconds
import org.example.route.model.RouteVersion
import org.example.trip.personal.dto.FrozenRouteBasisProjection
import org.example.trip.personal.dto.TripDayProjection
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test
import java.time.LocalDate

class ReferenceDayTripGeneratorTest {
    private val mapper: ObjectMapper = JacksonConfig().objectMapper()
    private val generator = ReferenceDayTripGenerator(mapper)
    private val path = (0..10).map { PublicRouteGeoPosition(30.0 + it * 0.01, 100.0, "WGS84") }
    private val basis = FrozenRouteBasisProjection("亚丁5日", "multi_day", "四川", PublicRoutePlace("路线起点", path.first()), PublicRoutePlace("路线终点", path.last()), RouteSeconds(94_560.0), path)
    private fun version(days: String = referenceDays()) = RouteVersion("version", "route", routeType = "multi_day", mainTrackAvailability = "valid", referenceDaysJson = days)
    private fun generated(version: RouteVersion = version()) = requireNotNull(generator.generate("trip", "成都", LocalDate.of(2026, 10, 1), version, basis))
    private fun projections(version: RouteVersion = version()) = generated(version).map { mapper.readValue(it.contentJson, TripDayProjection::class.java) }

    @Test fun `five reference days become five consecutive frozen days rather than two elapsed duration days`() {
        val days = generated()
        assertEquals(5, days.size)
        assertEquals((1..5).toList(), days.map { it.dayNumber })
        assertEquals((1..5).map { LocalDate.of(2026, 10, it) }, days.map { it.date })
        assertEquals(5, days.map { it.id }.toSet().size)
    }
    @Test fun `source guide preserves full range all middle points metrics and source confidence without upgrading hike`() {
        val first = projections().first()
        val guide = requireNotNull(first.routeGuide)
        assertEquals("ref-1", guide.sourceReferenceDayId)
        assertEquals("步行和电瓶车，原记录待核验", guide.description?.value)
        assertEquals("注意过河，原记录待核验", guide.notes?.value)
        assertEquals("原记录营地", guide.accommodationReference?.value)
        assertEquals("原作者历史记录", guide.accommodationReference?.confidence?.source)
        assertEquals(path.subList(0, 3), guide.referenceTrack?.path)
        assertEquals(9240.0, guide.referenceTrack?.distance?.meters)
        assertEquals(15480.0, guide.referenceTrack?.estimatedDuration?.seconds)
        val hike = first.actions.single { it.actionType == "hike" }
        assertNull(hike.routeSectionSnapshot)
        assertEquals("unavailable", hike.routeSectionConfidence?.status)
        listOf(hike.start, hike.end, hike.distance, hike.ascent, hike.estimatedDuration).forEach { assertNull(it?.value); assertEquals("unavailable", it?.confidence?.status) }
        assertEquals("成都", first.actions.first().origin?.value?.name)
        assertNull(first.actions.first().origin?.confidence, "known transport endpoints must not carry unavailable confidence")
        assertNull(first.actions.first().destination?.confidence)
        assertEquals("成都", projections().last().actions.last().destination?.value?.name)
        assertTrue(projections().all { it.points == null }, "route-level POIs must not all be assigned to first day")
    }
    @Test fun `missing optional sources remain absent and unavailable range is never guessed`() {
        val data = """[{"identity":"one","dayNumber":1,"title":"原参考日","notes":"原提示"}]"""
        val guide = requireNotNull(projections(version(data)).single().routeGuide)
        assertNull(guide.description)
        assertNull(guide.accommodationReference)
        assertNull(guide.referenceTrack)
    }
    @Test fun `no reference scheme leaves existing generator strategy available`() {
        assertNull(generator.generate("trip", "成都", LocalDate.of(2026,10,1), version().copy(referenceDaysJson = null), basis))
    }
    @Test fun `duplicate identity or discontinuous day numbers are not silently repaired`() {
        val duplicate = """[{"identity":"one","dayNumber":1},{"identity":"one","dayNumber":2}]"""
        assertThrows(org.example.common.contract.ApiContractException::class.java) { generated(version(duplicate)) }
        val gap = """[{"identity":"one","dayNumber":1},{"identity":"two","dayNumber":3}]"""
        assertThrows(org.example.common.contract.ApiContractException::class.java) { generated(version(gap)) }
    }
    @Test fun `old day JSON without guide remains readable and no historical content is rewritten`() {
        val fresh = projections().first()
        val old = mapper.valueToTree<com.fasterxml.jackson.databind.node.ObjectNode>(fresh).apply { remove("routeGuide") }
        assertNull(mapper.treeToValue(old, TripDayProjection::class.java).routeGuide)
        assertEquals(old, mapper.readTree(mapper.writeValueAsString(mapper.treeToValue(old, TripDayProjection::class.java))))
    }
    @Test fun `frozen route text and elevation profile roundtrip while legacy missing and null fields stay absent`() {
        val fields = listOf("introduction", "routeOrientation", "elevationProfile")
        val old = mapper.valueToTree<ObjectNode>(basis)
        fields.forEach { assertFalse(old.has(it), "$it must not be synthesized") }
        assertEquals(old, mapper.readTree(mapper.writeValueAsString(mapper.treeToValue(old, FrozenRouteBasisProjection::class.java))))
        val withNulls = old.deepCopy().apply { fields.forEach { putNull(it) } }
        assertEquals(old, mapper.readTree(mapper.writeValueAsString(mapper.treeToValue(withNulls, FrozenRouteBasisProjection::class.java))))

        val enriched = old.deepCopy().apply {
            put("introduction", "生成时冻结的路线概述")
            put("routeOrientation", "沿原记录起点，经垭口到终点")
            set<JsonNode>("elevationProfile", mapper.readTree("""
                {
                  "minElevation": {"meters": 3900.0},
                  "maxElevation": {"meters": 4700.0},
                  "samples": [
                    {"distance": {"meters": 0.0}, "elevation": {"meters": 4000.0}},
                    {"distance": {"meters": 1500.5}, "elevation": {"meters": 4700.0}},
                    {"distance": {"meters": 3200.75}, "elevation": {"meters": 3900.0}},
                    {"distance": {"meters": 5000.0}, "elevation": {"meters": 4200.0}}
                  ]
                }
            """.trimIndent()))
        }
        val roundtrip = mapper.readTree(mapper.writeValueAsString(mapper.treeToValue(enriched, FrozenRouteBasisProjection::class.java)))
        assertEquals(enriched, roundtrip, "frozen text, every ordered sample and extrema must survive without sample identities")
    }
    @Test fun `qualified reference guide and elevation profile roundtrip while legacy missing and null fields stay absent`() {
        val fields = listOf("title", "start", "end")
        val old = mapper.valueToTree<ObjectNode>(projections().first())
        val oldGuide = old.get("routeGuide") as ObjectNode
        fields.forEach { assertFalse(oldGuide.has(it), "$it must not be synthesized") }
        assertFalse(oldGuide.get("referenceTrack").has("elevationProfile"))
        assertEquals(old, mapper.readTree(mapper.writeValueAsString(mapper.treeToValue(old, TripDayProjection::class.java))))
        val withNulls = old.deepCopy().apply {
            val guide = get("routeGuide") as ObjectNode
            fields.forEach { guide.putNull(it) }
            (guide.get("referenceTrack") as ObjectNode).putNull("elevationProfile")
        }
        assertEquals(old, mapper.readTree(mapper.writeValueAsString(mapper.treeToValue(withNulls, TripDayProjection::class.java))))

        val enriched = old.deepCopy().apply {
            val guide = get("routeGuide") as ObjectNode
            guide.set<JsonNode>("title", mapper.readTree("""
                {"value": "已采纳的完整参考日", "confidence": {
                  "status": "pending_verification", "category": "public_route_fact",
                  "source": "原记录含步行和电瓶车，采纳完整范围及算法估时，非纯徒步实测"
                }}
            """.trimIndent()))
            guide.set<JsonNode>("start", mapper.readTree("""
                {"value": {"name": "原记录轨迹起点", "position": {
                  "latitude": 30.0, "longitude": 100.0, "referenceSystem": "WGS84"
                }}, "confidence": {
                  "status": "pending_verification", "category": "public_route_fact", "source": "原参考日起点，未实地核验"
                }}
            """.trimIndent()))
            guide.set<JsonNode>("end", mapper.readTree("""
                {"value": {"name": "原记录轨迹终点", "position": {
                  "latitude": 30.02, "longitude": 100.0, "referenceSystem": "WGS84"
                }}, "confidence": {
                  "status": "pending_verification", "category": "public_route_fact", "source": "原参考日终点，未实地核验"
                }}
            """.trimIndent()))
            (guide.get("referenceTrack") as ObjectNode).set<JsonNode>("elevationProfile", mapper.readTree("""
                {
                  "minElevation": {"meters": 4000.0},
                  "maxElevation": {"meters": 4250.0},
                  "samples": [
                    {"distance": {"meters": 0.0}, "elevation": {"meters": 4000.0}},
                    {"distance": {"meters": 500.25}, "elevation": {"meters": 4250.0}},
                    {"distance": {"meters": 950.5}, "elevation": {"meters": 4100.0}}
                  ]
                }
            """.trimIndent()))
        }
        val roundtrip = mapper.readTree(mapper.writeValueAsString(mapper.treeToValue(enriched, TripDayProjection::class.java)))
        assertEquals(enriched, roundtrip, "qualified title and endpoints, source confidence, ordered samples and existing day content must survive unchanged")
    }
    @Test fun `long valid reference title cannot reject generation or truncate guide content`() {
        val description = "历史记录完整说明".repeat(80)
        val notes = "原记录注意事项".repeat(80)
        val refs = mapper.writeValueAsString(listOf(mapOf("identity" to "long-ref", "dayNumber" to 1,
            "title" to "山".repeat(101), "description" to description, "notes" to notes)))
        val day = projections(version(refs)).single()
        assertEquals("参考行程", day.primaryStage)
        assertEquals(description, day.routeGuide?.description?.value)
        assertEquals(notes, day.routeGuide?.notes?.value)
        assertEquals("long-ref", day.routeGuide?.sourceReferenceDayId)
    }
    @Test fun `stage length uses Unicode code points and does not reject valid supplementary characters`() {
        val title = "🏔".repeat(100)
        val refs = mapper.writeValueAsString(listOf(mapOf("identity" to "unicode-ref", "dayNumber" to 1, "title" to title)))
        assertEquals(title, projections(version(refs)).single().primaryStage)
    }
    private fun referenceDays(): String = mapper.writeValueAsString((1..5).map { day -> buildMap<String, Any> {
        put("identity", "ref-$day"); put("dayNumber", day); put("title", "第${day}天")
        put("description", "步行和电瓶车，原记录待核验"); put("notes", "注意过河，原记录待核验")
        put("distance", mapOf("meters" to 9240.0)); put("estimatedDuration", mapOf("seconds" to 15480.0))
        put("accommodation", "原记录营地")
        put("accommodationEvidence", mapOf("value" to "原记录营地", "confidence" to mapOf("status" to "pending_verification", "category" to "public_route_fact", "source" to "原作者历史记录")))
        put("mainTrackRange", mapOf("startPathPosition" to mapOf("precedingPositionIndex" to (day-1)*2, "progressToNextPosition" to 0.0), "endPathPosition" to if(day==5) mapOf("precedingPositionIndex" to 10) else mapOf("precedingPositionIndex" to day*2, "progressToNextPosition" to 0.0)))
    } })
}
