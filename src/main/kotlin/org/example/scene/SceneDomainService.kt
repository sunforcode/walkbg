package org.example.scene

import com.fasterxml.jackson.databind.JsonNode
import com.fasterxml.jackson.databind.ObjectMapper
import org.example.common.contract.ApiContractException
import org.example.route.dto.PublicRouteGeoPosition
import org.example.route.model.RouteVersion
import org.example.trip.personal.model.PersonalTripDayRecord
import org.springframework.http.HttpStatus
import org.springframework.stereotype.Service
import java.io.ByteArrayInputStream
import java.security.MessageDigest
import javax.imageio.ImageIO

@Service
class SceneDomainService(private val mapper: ObjectMapper) {
    companion object {
        const val MAX_BYTES = 10 * 1024 * 1024
        const val MAX_PIXELS = 32_000_000L
    }

    fun inspectMedia(bytes: ByteArray, contentType: String): SceneMedia {
        if (bytes.size > MAX_BYTES) throw tooLarge()
        if (contentType !in setOf("image/png", "image/jpeg")) {
            throw ApiContractException(HttpStatus.UNSUPPORTED_MEDIA_TYPE, "scene_media_type_unsupported", "仅支持 PNG 或 JPEG 图片")
        }
        try {
            ImageIO.createImageInputStream(ByteArrayInputStream(bytes)).use { input ->
                val readers = ImageIO.getImageReaders(input)
                if (!readers.hasNext()) throw invalidMedia()
                val reader = readers.next()
                try {
                    reader.input = input
                    val format = reader.formatName.lowercase()
                    if ((contentType == "image/png" && format != "png") || (contentType == "image/jpeg" && format !in setOf("jpeg", "jpg"))) throw invalidMedia()
                    val width = reader.getWidth(0)
                    val height = reader.getHeight(0)
                    if (width <= 0 || height <= 0) throw invalidMedia()
                    if (width.toLong() * height > MAX_PIXELS) throw tooLarge()
                    // Header dimensions alone do not establish that the uploaded file can be decoded.
                    val decoded = reader.read(0) ?: throw invalidMedia()
                    if (decoded.width != width || decoded.height != height) throw invalidMedia()
                    decoded.flush()
                    val id = MessageDigest.getInstance("SHA-256").digest(bytes).joinToString("") { "%02x".format(it) }
                    return SceneMedia(id, contentType, width, height, bytes)
                } finally { reader.dispose() }
            }
        } catch (error: ApiContractException) { throw error }
        catch (_: Exception) { throw invalidMedia() }
    }

    fun referenceDays(version: RouteVersion): List<JsonNode> {
        if (version.referenceDaysJson.isNullOrBlank()) return emptyList()
        val tree = readStored(requireNotNull(version.referenceDaysJson))
        if (!tree.isArray) throw unavailable()
        return tree.toList()
    }

    fun validateBindings(bindings: SceneBindings, allowedDayIds: List<String>, invalidDayCode: String) {
        if (allowedDayIds.any(String::isBlank) || allowedDayIds.toSet().size != allowedDayIds.size) throw unavailable()
        val ids = bindings.days.map { it.dayId }
        if (ids.toSet().size != ids.size) throw ApiContractException.invalidRequest("每日场景身份不能重复")
        if (ids.any { it !in allowedDayIds }) throw ApiContractException.unprocessable(invalidDayCode, "每日场景不属于该版本或行程")
        (bindings.days.map { it.mediaId } + listOfNotNull(bindings.overviewMediaId)).forEach(::requireMediaId)
    }

    fun requireMediaId(id: String) {
        if (!Regex("[0-9a-f]{64}").matches(id)) throw ApiContractException.invalidRequest("mediaId 必须为 SHA-256 内容标识")
    }

    /** Frozen explicit adoption is authoritative; historical days may use exact full-path matching. */
    fun matchDays(version: RouteVersion, days: List<PersonalTripDayRecord>): Map<String, String> {
        val references = referenceDays(version)
        val referenceIds = references.map { it.path("identity").asText("") }
        if (referenceIds.any(String::isBlank) || referenceIds.toSet().size != referenceIds.size) return emptyMap()
        val explicit = mutableListOf<Pair<String, String>>()
        val historical = mutableListOf<PersonalTripDayRecord>()
        days.forEach { day ->
            val guide = readStored(day.contentJson).path("routeGuide")
            if (guide.isMissingNode || guide.isNull) historical.add(day)
            else {
                val sourceId = guide.path("sourceReferenceDayId")
                if (sourceId.isTextual && sourceId.asText() in referenceIds) explicit.add(day.id to sourceId.asText())
                // An invalid explicit relation must not silently fall back to a geometric guess.
            }
        }
        val matches = explicit + historicalMatches(version, references, historical)
        val occurrences = matches.groupingBy { it.second }.eachCount()
        return matches.filter { occurrences[it.second] == 1 }.toMap()
    }

    private fun historicalMatches(version: RouteVersion, references: List<JsonNode>, days: List<PersonalTripDayRecord>): List<Pair<String, String>> {
        if (days.isEmpty() || version.mainTrackAvailability != "valid" || version.mainTrackJson.isNullOrBlank() || version.mainTrackReferenceSystem.isNullOrBlank()) return emptyList()
        val raw = readStored(requireNotNull(version.mainTrackJson))
        if (!raw.isArray) return emptyList()
        val path = raw.map { node ->
            if (!node.isArray || node.size() < 2 || !node[0].isNumber || !node[1].isNumber) return emptyList()
            position(node[0].asDouble(), node[1].asDouble(), requireNotNull(version.mainTrackReferenceSystem)) ?: return emptyList()
        }
        val candidates = references.mapNotNull { ref ->
            slice(path, ref.path("mainTrackRange"))?.let { ref.path("identity").asText() to it }
        }
        return days.mapNotNull { day ->
            val hikePath = hikePath(day) ?: return@mapNotNull null
            val matching = candidates.filter { it.second == hikePath }
            if (matching.size == 1) day.id to matching.single().first else null
        }
    }

    private fun hikePath(day: PersonalTripDayRecord): List<PublicRouteGeoPosition>? {
        val actions = readStored(day.contentJson).path("actions")
        if (!actions.isArray) return null
        val hikes = actions.filter { it.path("actionType").asText() == "hike" }
        if (hikes.size != 1) return null
        if (hikes.any { !it.path("sequence").isIntegralNumber } || hikes.map { it.path("sequence").asInt() }.toSet().size != hikes.size) return null
        val result = mutableListOf<PublicRouteGeoPosition>()
        for (hike in hikes.sortedBy { it.path("sequence").asInt() }) {
            val nodes = hike.path("routeSectionSnapshot").path("path")
            if (!nodes.isArray || nodes.size() < 2) return null
            val points = nodes.map { node ->
                if (!node.path("latitude").isNumber || !node.path("longitude").isNumber || !node.path("referenceSystem").isTextual) return null
                position(node.path("latitude").asDouble(), node.path("longitude").asDouble(), node.path("referenceSystem").asText()) ?: return null
            }
            if (result.isEmpty()) result.addAll(points)
            else {
                if (result.last() != points.first()) return null
                result.addAll(points.drop(1))
            }
        }
        return result
    }

    private fun slice(path: List<PublicRouteGeoPosition>, range: JsonNode): List<PublicRouteGeoPosition>? {
        if (!range.isObject || path.size < 2) return null
        fun endpoint(node: JsonNode): Pair<Double, PublicRouteGeoPosition>? {
            if (!node.path("precedingPositionIndex").isIntegralNumber || !node.path("precedingPositionIndex").canConvertToInt()) return null
            val index = node.path("precedingPositionIndex").asInt()
            if (index !in path.indices) return null
            val fractionNode = node.path("progressToNextPosition")
            val final = index == path.lastIndex
            if (final && !(fractionNode.isMissingNode || fractionNode.isNull)) return null
            if (!final && !fractionNode.isNumber) return null
            val fraction = if (final) 0.0 else fractionNode.asDouble()
            if (!fraction.isFinite() || fraction < 0 || fraction >= 1) return null
            val p = path[index]
            val interpolated = if (fraction == 0.0) p else {
                val next = path[index + 1]
                PublicRouteGeoPosition(p.latitude + (next.latitude - p.latitude) * fraction, p.longitude + (next.longitude - p.longitude) * fraction, p.referenceSystem)
            }
            return index + fraction to interpolated
        }
        val start = endpoint(range.path("startPathPosition")) ?: return null
        val end = endpoint(range.path("endPathPosition")) ?: return null
        if (start.first >= end.first) return null
        return buildList {
            add(start.second)
            path.indices.filter { it.toDouble() > start.first && it.toDouble() < end.first }.forEach { add(path[it]) }
            add(end.second)
        }
    }

    private fun position(lat: Double, lon: Double, system: String): PublicRouteGeoPosition? =
        if (!lat.isFinite() || !lon.isFinite() || lat !in -90.0..90.0 || lon !in -180.0..180.0 || system.isBlank()) null else PublicRouteGeoPosition(lat, lon, system)

    private fun readStored(json: String): JsonNode = try { mapper.readTree(json) ?: throw unavailable() } catch (_: Exception) { throw unavailable() }
    fun tooLarge() = ApiContractException(HttpStatus.UNPROCESSABLE_ENTITY, "scene_image_too_large", "图片超过 10 MiB 或 3200 万像素限制")
    private fun invalidMedia() = ApiContractException(HttpStatus.BAD_REQUEST, "invalid_scene_image", "图片内容无效或与声明格式不符")
    private fun unavailable() = ApiContractException.serviceUnavailable("scene_read_failed", "场景关联资料暂时无法读取")
}
