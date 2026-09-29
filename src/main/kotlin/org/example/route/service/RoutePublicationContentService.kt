package org.example.route.service

import com.fasterxml.jackson.databind.DeserializationFeature
import com.fasterxml.jackson.databind.JsonNode
import com.fasterxml.jackson.databind.ObjectMapper
import org.example.common.exception.BusinessException
import org.example.route.dto.*
import org.example.route.model.*
import org.example.route.repository.RouteVersionPointRepository
import org.example.route.repository.RouteVersionSegmentRepository
import org.example.route.repository.SegmentSchemeRepository
import org.example.route.repository.WaypointRepository
import org.springframework.stereotype.Service
import org.springframework.web.util.HtmlUtils
import java.math.BigDecimal
import java.math.RoundingMode
import java.time.Instant
import java.util.UUID

/** Prepare an immutable publication snapshot from adopted, explicitly typed source records. */
@Service
class RoutePublicationContentService(
    private val schemes: SegmentSchemeRepository,
    private val waypoints: WaypointRepository,
    private val versionSegments: RouteVersionSegmentRepository,
    private val versionPoints: RouteVersionPointRepository,
    private val mapper: ObjectMapper
) {
    data class Content(
        val days: List<PublicRouteReferenceDay>,
        val segments: List<RouteVersionSegment>,
        val campsites: List<RouteVersionPoint>
    )

    private data class SourceScheme(val scheme: SegmentScheme, val segments: List<Segment>)
    private data class CampAssociation(val kind: String, val start: Int, val end: Int, val dayNumber: Int?)
    private data class CampEvidence(val usage: String, val text: String, val associations: List<CampAssociation>)

    fun prepare(
        versionId: String,
        route: Route,
        routeType: String,
        mainTrack: RouteTrackReviewService.PublicationTrack,
        sourceSegments: List<Segment>,
        sourcePois: List<PoiPoint>
    ): Content {
        val sourceSchemes = schemes.findByRouteId(route.id).associateBy { it.id }
        val slope = selectScheme(route, "slope", sourceSegments, sourceSchemes)
        val day = if (routeType == "multi_day") selectScheme(route, "day", sourceSegments, sourceSchemes) else null
        if (day != null && day.segments.map { it.sequenceNumber } != (1..day.segments.size).toList()) {
            invalid("参考日序必须从1连续且无重复，请先整理已采纳日方案")
        }
        val reviewedPath = if (mainTrack.availability == "valid" && mainTrack.json == route.trackGeoJson &&
            !mainTrack.referenceSystem.isNullOrBlank()) readJson(requireNotNull(mainTrack.json)) else null
        val pathSize = reviewedPath?.takeIf { it.isArray }?.size() ?: 0
        val places = waypoints.findByRouteIdOrderBySequenceNumberAsc(route.id).associateBy { it.id }
        fun place(id: String?): PublicRoutePlace? {
            val point = id?.let(places::get) ?: return null
            if (point.routeId != route.id) invalid("路段起终点不属于当前路线")
            val name = point.name.nonBlank() ?: return null
            val generated = when (point.type) {
                "segment_start" -> name.endsWith(" 起点")
                "segment_end" -> name.endsWith(" 终点")
                "segment_split" -> name.endsWith(" 拆分点")
                else -> false
            }
            if (generated) return null
            // Waypoint has no coordinate-system declaration; only its actual name is reused.
            return PublicRoutePlace(name)
        }
        fun range(segment: Segment, scheme: SourceScheme): PublicRouteMainTrackRange? {
            if (pathSize == 0 || !currentSource(route, segment.createdAt) || segment.createdAt.isBefore(scheme.scheme.createdAt)) return null
            val start = segment.trackStartIndex
            val end = segment.trackEndIndex
            if (start == null && end == null) return null
            if (start == null || end == null || start < 0 || end < start || end >= pathSize) {
                invalid("已采纳路段的轨迹范围无效，请重新核对")
            }
            if (start == end) return null
            fun position(index: Int) = PublicRouteMainTrackPathPosition(index, if (index < pathSize - 1) 0.0 else null)
            return PublicRouteMainTrackRange(position(start), position(end))
        }
        val days = day?.segments.orEmpty().map { segment ->
            PublicRouteReferenceDay(
                identity = UUID.randomUUID().toString(), dayNumber = segment.sequenceNumber,
                title = segment.name.nonBlank(), description = segment.description.nonBlank(),
                start = place(segment.startPointId), end = place(segment.endPointId),
                distance = meters(segment.distance, "每日距离", BigDecimal("1000"))?.toDouble()?.let(::RouteMeters),
                estimatedDuration = seconds(segment.estimatedTime)?.toDouble()?.let(::RouteSeconds),
                ascent = meters(segment.elevationGain, "每日爬升")?.toDouble()?.let(::RouteMeters),
                descent = meters(segment.elevationLoss, "每日下降")?.toDouble()?.let(::RouteMeters),
                notes = segment.notes.nonBlank(), mainTrackRange = range(segment, requireNotNull(day))
            )
        }
        val presentRanges = days.mapNotNull { it.mainTrackRange }
        if (presentRanges.zipWithNext().any { (a, b) ->
                b.startPathPosition.precedingPositionIndex < a.endPathPosition.precedingPositionIndex }) {
            invalid("参考日的轨迹范围倒序或重叠，请重新核对")
        }
        val publishedSegments = slope?.segments.orEmpty().map { segment ->
            RouteVersionSegment(
                id = UUID.randomUUID().toString(), routeVersionId = versionId,
                segmentOrder = segment.sequenceNumber, name = segment.name.nonBlank() ?: invalid("公共路段名称为空"),
                startName = place(segment.startPointId)?.name, endName = place(segment.endPointId)?.name,
                distanceMeters = meters(segment.distance, "路段距离", BigDecimal("1000")),
                estimatedDurationSeconds = seconds(segment.estimatedTime),
                ascentMeters = meters(segment.elevationGain, "路段爬升"), descentMeters = meters(segment.elevationLoss, "路段下降"),
                description = segment.description.nonBlank(), notes = segment.notes.nonBlank(),
                mainTrackRangeJson = range(segment, requireNotNull(slope))?.let(mapper::writeValueAsString)
            )
        }
        val overnight = mutableMapOf<Int, MutableList<String>>()
        val campsites = sourcePois.filter { it.status == "confirmed" && it.category == "camp" }
            .sortedWith(compareBy<PoiPoint> { it.createdAt }.thenBy { it.id }).mapNotNull { poi ->
                if (poi.routeId != route.id) invalid("营地来源不属于当前路线")
                if (poi.source != "kml_marker" || !currentSource(route, poi.createdAt) || pathSize == 0) return@mapNotNull null
                val name = poi.name.nonBlank() ?: invalid("已采纳营地没有名称")
                if (!poi.latitude.isFinite() || !poi.longitude.isFinite() || poi.latitude !in -90.0..90.0 ||
                    poi.longitude !in -180.0..180.0 || poi.elevation?.isFinite() == false) invalid("营地坐标或高程无效")
                val evidence = parseCampEvidence(poi.cardData)
                val sourceText = evidence?.text ?: name
                val qualified = sourceEvidence(sourceText)
                if (evidence?.usage == "recorded_overnight") {
                    evidence.associations.filter { it.kind == "day" }.forEach { association ->
                        val sourceDay = day?.segments?.singleOrNull {
                            it.sequenceNumber == association.dayNumber && it.trackStartIndex == association.start && it.trackEndIndex == association.end
                        }
                        val publishedDay = days.singleOrNull { it.dayNumber == association.dayNumber }
                        if (sourceDay != null && publishedDay?.mainTrackRange != null) {
                            overnight.getOrPut(sourceDay.sequenceNumber, ::mutableListOf).add("$name（原记录，待核验）")
                        }
                    }
                }
                RouteVersionPoint(
                    id = UUID.randomUUID().toString(), routeVersionId = versionId, pointKind = "campsite",
                    displayOrder = 0, name = name, category = "camp", description = plainText(poi.description),
                    latitude = poi.latitude, longitude = poi.longitude, elevation = poi.elevation,
                    referenceSystem = requireNotNull(mainTrack.referenceSystem), sourceEvidenceJson = mapper.writeValueAsString(qualified)
                )
            }.mapIndexed { index, point -> point.copy(displayOrder = index + 1) }
        val completedDays = days.map { referenceDay ->
            val text = overnight[referenceDay.dayNumber]?.distinct()?.takeIf { it.isNotEmpty() }
                ?.joinToString("；", prefix = "原记录住宿参考：")
            referenceDay.copy(accommodation = text, accommodationEvidence = text?.let(::sourceEvidence))
        }
        return Content(completedDays, publishedSegments, campsites)
    }

    fun persist(content: Content) {
        if (content.segments.isNotEmpty()) versionSegments.saveAllAndFlush(content.segments)
        if (content.campsites.isNotEmpty()) versionPoints.saveAllAndFlush(content.campsites)
    }

    private fun selectScheme(route: Route, kind: String, all: List<Segment>, available: Map<String, SegmentScheme>): SourceScheme? {
        val selected = all.filter { it.status == "confirmed" && it.schemeType == kind }
        if (selected.isEmpty()) return null
        selected.forEach { segment ->
            val owner = segment.schemeId?.let(available::get)
            if (segment.routeId != route.id || owner == null || owner.routeId != route.id || owner.schemeType != kind) {
                invalid("已采纳${kind}路段的所属方案不一致")
            }
        }
        val identities = selected.map { it.schemeId }.distinct()
        if (identities.size != 1) invalid("存在多套已采纳${kind}方案，请先明确唯一发布方案")
        val sorted = selected.sortedBy { it.sequenceNumber }
        if (sorted.any { it.sequenceNumber < 1 } || sorted.map { it.sequenceNumber }.distinct().size != sorted.size) {
            invalid("已采纳${kind}方案序号无效或重复")
        }
        return SourceScheme(available.getValue(requireNotNull(identities.single())), sorted)
    }

    private fun currentSource(route: Route, createdAt: Instant): Boolean =
        route.analysisStatus == "completed" && !route.analysisTaskId.isNullOrBlank() &&
            route.analysisStartedAt?.let { !createdAt.isBefore(it) } == true

    private fun meters(value: Double?, label: String, multiplier: BigDecimal = BigDecimal.ONE): BigDecimal? {
        if (value == null) return null
        if (!value.isFinite() || value < 0) invalid("${label}不是有效的非负数")
        val result = BigDecimal.valueOf(value).multiply(multiplier)
        if (result >= BigDecimal("100000000000")) invalid("${label}超出可保存范围")
        return result
    }

    private fun seconds(minutes: Double?): Long? {
        if (minutes == null) return null
        if (!minutes.isFinite() || minutes < 0) invalid("预计行进时间不是有效的非负分钟数")
        return try {
            BigDecimal.valueOf(minutes).multiply(BigDecimal("60")).setScale(0, RoundingMode.HALF_UP).longValueExact()
        } catch (_: ArithmeticException) { invalid("预计行进时间超出可保存范围") }
    }

    private fun sourceEvidence(value: String) = QualifiedRouteText(
        value = value, confidence = PublicRouteInformationConfidence(
            status = "pending_verification", category = "public_route_fact", source = "原轨迹营地标注（人工采纳，未实地核验）"
        )
    )

    private fun parseCampEvidence(raw: String?): CampEvidence? {
        if (raw.isNullOrBlank()) return null
        val root = readJson(raw)
        if (!root.isObject) invalid("营地来源资料不是有效对象")
        val node = root.get("camp_analysis") ?: return null
        checkFields(node, setOf("status", "source", "usage", "evidence", "associations", "unresolved_reason"))
        if (text(node, "status") != "pending_verification" || text(node, "source") != "source_marker") invalid("营地来源可信状态不符合合同")
        val usage = text(node, "usage")
        if (usage !in setOf("recorded_overnight", "along_route", "unknown")) invalid("营地来源用途不符合合同")
        val evidence = text(node, "evidence")
        val array = node.get("associations")?.takeIf { it.isArray } ?: invalid("营地来源缺少关联集合")
        val associations = array.map { item ->
            checkFields(item, setOf("scheme_type", "track_start_index", "track_end_index", "basis", "distance_meters", "day_number"))
            val kind = text(item, "scheme_type")
            if (kind !in setOf("day", "slope") || text(item, "basis") !in setOf("explicit_day", "time_and_position", "position")) invalid("营地关联类型无效")
            val start = integer(item, "track_start_index")
            val end = integer(item, "track_end_index")
            if (start < 0 || end < start) invalid("营地关联范围无效")
            val day = if (kind == "day") integer(item, "day_number").also { if (it < 1) invalid("营地日序无效") }
                else null.also { if (item.has("day_number")) invalid("坡度营地关联不能包含日序") }
            val distance = item.get("distance_meters")
            if (distance == null || !distance.isNumber || !distance.asDouble().isFinite() || distance.asDouble() < 0) invalid("营地关联距离无效")
            CampAssociation(kind, start, end, day)
        }
        if (associations.map { Triple(it.kind, it.start, it.end) }.distinct().size != associations.size) invalid("营地关联不能重复")
        if (usage == "recorded_overnight" && associations.none { it.kind == "day" }) invalid("原记录当晚营地缺少日归属")
        if (node.has("unresolved_reason")) text(node, "unresolved_reason")
        if (associations.isEmpty() && (usage != "unknown" || !node.has("unresolved_reason"))) invalid("未关联营地必须保留待确认原因")
        return CampEvidence(usage, evidence, associations)
    }

    private fun readJson(raw: String): JsonNode = try {
        mapper.reader().with(DeserializationFeature.FAIL_ON_TRAILING_TOKENS).readTree(raw)
    } catch (_: Exception) { invalid("来源JSON结构损坏，无法形成公共版本") }

    private fun checkFields(node: JsonNode, allowed: Set<String>) {
        if (!node.isObject || node.fieldNames().asSequence().any { it !in allowed }) invalid("营地来源包含未定义字段")
    }
    private fun text(node: JsonNode, key: String): String = node.get(key)?.takeIf { it.isTextual }
        ?.asText()?.nonBlank() ?: invalid("营地来源字段${key}无效")
    private fun integer(node: JsonNode, key: String): Int = node.get(key)?.takeIf { it.isIntegralNumber && it.canConvertToInt() }
        ?.asInt() ?: invalid("营地来源字段${key}无效")
    private fun plainText(value: String?): String? = value?.let {
        HtmlUtils.htmlUnescape(it).replace(Regex("(?is)<(script|style)\\b[^>]*>.*?</\\1\\s*>"), "")
            .replace(Regex("<[^>]*>"), "\n").lineSequence().map(String::trim).filter(String::isNotEmpty).joinToString("\n").nonBlank()
    }
    private fun String?.nonBlank(): String? = this?.takeIf(String::isNotBlank)
    private fun invalid(message: String): Nothing = throw BusinessException.unprocessableEntity(message)
}
