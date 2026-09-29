package org.example.scene

import org.example.common.contract.ApiContractException
import org.example.config.JacksonConfig
import org.example.route.model.RouteVersion
import org.example.trip.personal.model.PersonalTripDayRecord
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test
import java.awt.image.BufferedImage
import java.io.ByteArrayOutputStream
import java.time.LocalDate
import javax.imageio.ImageIO

class SceneDomainServiceTest {
    private val mapper = JacksonConfig().objectMapper()
    private val domain = SceneDomainService(mapper)

    @Test fun `decodes png and jpeg and computes immutable content identity`() {
        val bytes = sceneImageBytes()
        val first = domain.inspectMedia(bytes, "image/png")
        val second = domain.inspectMedia(bytes.copyOf(), "image/png")
        assertEquals(first.mediaId, second.mediaId)
        assertEquals(64, first.mediaId.length)
        assertEquals(8, first.width)
        assertEquals(6, first.height)
        assertArrayEquals(bytes, first.bytes)
        assertEquals("image/jpeg", domain.inspectMedia(sceneImageBytes("jpeg"), "image/jpeg").contentType)
    }
    @Test fun `rejects empty corrupt mismatched or truncated images and unsupported media`() {
        listOf(ByteArray(0), "not an image".toByteArray(), sceneImageBytes().copyOf(30)).forEach {
            assertEquals("invalid_scene_image", assertThrows(ApiContractException::class.java) { domain.inspectMedia(it, "image/png") }.code)
        }
        assertEquals("invalid_scene_image", assertThrows(ApiContractException::class.java) { domain.inspectMedia(sceneImageBytes(), "image/jpeg") }.code)
        assertEquals(415, assertThrows(ApiContractException::class.java) { domain.inspectMedia(sceneImageBytes(), "image/svg+xml") }.status.value())
    }
    @Test fun `byte and decoded pixel limits are enforced`() {
        assertEquals("scene_image_too_large", assertThrows(ApiContractException::class.java) { domain.inspectMedia(ByteArray(SceneDomainService.MAX_BYTES + 1), "image/png") }.code)
        // Large header is rejected before allocating/decompressing a raster.
        val oversized = sceneImageBytes().copyOf().apply imageBytes@ {
            java.nio.ByteBuffer.wrap(this, 16, 8).putInt(8000).putInt(8000)
            val crc = java.util.zip.CRC32().apply { update(this@imageBytes, 12, 17) }.value
            java.nio.ByteBuffer.wrap(this, 29, 4).putInt(crc.toInt())
        }
        assertEquals("scene_image_too_large", assertThrows(ApiContractException::class.java) { domain.inspectMedia(oversized, "image/png") }.code)
    }
    @Test fun `only the complete ordered coordinate path matches a reference day`() {
        val good = day("actual-4", listOf(point(30.0), point(30.1), point(30.2)))
        assertEquals(mapOf("actual-4" to "ref-1"), domain.matchDays(version(), listOf(good)))
        val changedMiddle = day("actual-4", listOf(point(30.0), point(30.15), point(30.2)))
        assertTrue(domain.matchDays(version(), listOf(changedMiddle)).isEmpty())
        assertTrue(domain.matchDays(version(), listOf(day("actual-4", listOf(point(30.2), point(30.1), point(30.0))))).isEmpty())
        val wrongSystem = listOf(point(30.0), point(30.1), point(30.2)).map { it + ("referenceSystem" to "GCJ02") }
        assertTrue(domain.matchDays(version(), listOf(day("actual-4", wrongSystem))).isEmpty())
    }
    @Test fun `does not guess missing ranges duplicate matches or transport days`() {
        val good = day("actual-4", listOf(point(30.0), point(30.1), point(30.2)))
        assertTrue(domain.matchDays(version("""[{"identity":"ref-1","dayNumber":1}]"""), listOf(good)).isEmpty())
        assertTrue(domain.matchDays(version("""[${reference("ref-1")},${reference("ref-2")}]"""), listOf(good)).isEmpty())
        assertTrue(domain.matchDays(version(), listOf(good, good.copy(id = "second"))).isEmpty())
        val transfer = good.copy(contentJson = good.contentJson.replace("hike", "transport"))
        assertTrue(domain.matchDays(version(), listOf(transfer)).isEmpty())
    }
    @Test fun `multiple hike actions cannot be concatenated into automatic scenes`() {
        val action = mapOf("sequence" to 1, "actionType" to "hike", "routeSectionSnapshot" to mapOf("path" to listOf(point(30.0), point(30.1))))
        val second = action + mapOf("sequence" to 2, "routeSectionSnapshot" to mapOf("path" to listOf(point(30.1), point(30.2))))
        val multiple = day("multi", emptyList()).copy(contentJson = mapper.writeValueAsString(mapOf("actions" to listOf(action, second))))
        assertTrue(domain.matchDays(version(), listOf(multiple)).isEmpty())
    }
    @Test fun `fractional boundaries interpolate while preserving every interior point`() {
        val ref = """[{"identity":"fraction","dayNumber":1,"mainTrackRange":{"startPathPosition":{"precedingPositionIndex":0,"progressToNextPosition":0.5},"endPathPosition":{"precedingPositionIndex":1,"progressToNextPosition":0.5}}}]"""
        val start = 30.0 + (30.1 - 30.0) * 0.5
        val end = 30.1 + (30.2 - 30.1) * 0.5
        assertEquals(mapOf("fraction-day" to "fraction"), domain.matchDays(version(ref), listOf(day("fraction-day", listOf(point(start), point(30.1), point(end))))))
    }
    @Test fun `management is closed without credentials and URL never trusts a request host`() {
        assertEquals(503, assertThrows(ApiContractException::class.java) { SceneConfiguration("", "").authorize(null) }.status.value())
        assertEquals("scene_management_authentication_required", assertThrows(ApiContractException::class.java) { SceneConfiguration("", "test-token").authorize("wrong") }.code)
        SceneConfiguration("", "test-token").authorize("test-token")
        listOf("", "/relative", "ftp://example.org", "https://user:pass@example.org", "https://example.org?x=1", "https://example.org/#x", "https://example.org/../other").forEach {
            assertEquals("scene_media_unavailable", assertThrows(ApiContractException::class.java) { SceneConfiguration(it, "").imageUrl("abc") }.code)
        }
        assertEquals("https://example.org/walkbg/api/v1/scene-media/abc", SceneConfiguration("https://example.org/walkbg/", "").imageUrl("abc"))
    }
    @Test fun `explicit frozen adoption matches correct reference identity without interpreting mixed journey as hike`() {
        fun adopted(id: String, ref: String) = day(id, emptyList()).copy(contentJson = mapper.writeValueAsString(mapOf(
            "routeGuide" to mapOf("sourceReferenceDayId" to ref), "actions" to listOf(mapOf("sequence" to 1, "actionType" to "hike", "routeSectionConfidence" to mapOf("status" to "unavailable")))
        )))
        assertEquals(mapOf("day-transport" to "ref-1"), domain.matchDays(version(), listOf(adopted("day-transport", "ref-1"))))
        assertTrue(domain.matchDays(version(), listOf(adopted("foreign", "another-version-ref"))).isEmpty())
        assertTrue(domain.matchDays(version(), listOf(adopted("one", "ref-1"), adopted("two", "ref-1"))).isEmpty())
        val invalid = day("with-hike", listOf(point(30.0), point(30.1), point(30.2)))
        val tree = mapper.readTree(invalid.contentJson) as com.fasterxml.jackson.databind.node.ObjectNode
        tree.set<com.fasterxml.jackson.databind.JsonNode>("routeGuide", mapper.valueToTree(mapOf("sourceReferenceDayId" to "foreign")))
        assertTrue(domain.matchDays(version(), listOf(invalid.copy(contentJson = mapper.writeValueAsString(tree)))).isEmpty())
    }
    private fun version(ref: String = "[${reference("ref-1")}]") = RouteVersion("v", "r", routeType = "multi_day", referenceDaysJson = ref, mainTrackAvailability = "valid", mainTrackReferenceSystem = "WGS84", mainTrackJson = "[[30.0,100.0],[30.1,100.0],[30.2,100.0]]")
    private fun reference(id: String) = """{"identity":"$id","dayNumber":1,"mainTrackRange":{"startPathPosition":{"precedingPositionIndex":0,"progressToNextPosition":0},"endPathPosition":{"precedingPositionIndex":2}}}"""
    private fun point(lat: Double): Map<String, Any> = mapOf("latitude" to lat, "longitude" to 100.0, "referenceSystem" to "WGS84")
    private fun day(id: String, path: List<Map<String, Any>>) = PersonalTripDayRecord(id, "trip", 4, LocalDate.of(2026, 10, 4), "山谷", 1,
        mapper.writeValueAsString(mapOf("actions" to listOf(mapOf("sequence" to 1, "actionType" to "hike", "routeSectionSnapshot" to mapOf("path" to path))))))
}

internal fun sceneImageBytes(format: String = "png"): ByteArray = ByteArrayOutputStream().use { output ->
    ImageIO.write(BufferedImage(8, 6, BufferedImage.TYPE_INT_RGB), format, output)
    output.toByteArray()
}
