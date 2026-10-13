package org.example.trip.personal.service

import com.fasterxml.jackson.databind.JsonNode
import com.fasterxml.jackson.databind.ObjectMapper
import org.example.common.contract.ApiContractException
import org.example.common.util.IdGenerator
import org.example.route.dto.PublicRouteGeoPosition
import org.example.route.dto.PublicRoutePlace
import org.example.route.dto.RouteMeters
import org.example.route.dto.RouteSeconds
import org.example.route.model.RouteVersion
import org.example.trip.personal.dto.*
import org.example.trip.personal.model.PersonalTripDayRecord
import java.time.LocalDate

/** Adopts a published day scheme only while creating a new frozen result. Never runs during reads. */
class ReferenceDayTripGenerator(private val mapper: ObjectMapper) {
    private val unavailable = InformationConfidenceProjection("unavailable", "generated_suggestion")
    private val source = InformationConfidenceProjection("pending_verification", "public_route_fact", "采用路线版本的参考分日资料")

    fun generate(tripId: String, departureCity: String, startDate: LocalDate, version: RouteVersion, basis: FrozenRouteBasisProjection): List<PersonalTripDayRecord>? {
        if (version.routeType != "multi_day" || version.referenceDaysJson.isNullOrBlank()) return null
        val references = try { mapper.readTree(version.referenceDaysJson) } catch (_: Exception) { throw failure() }
        if (!references.isArray) throw failure()
        if (references.isEmpty) return null
        val refs = references.toList()
        val ids = refs.map { text(it, "identity") ?: throw failure() }
        if (ids.toSet().size != ids.size || refs.withIndex().any { (index, ref) -> !ref.path("dayNumber").isIntegralNumber || ref.path("dayNumber").asLong() != index + 1L }) throw failure()
        var precedingEnd: Double? = null
        return refs.mapIndexed { index, ref ->
            val id = IdGenerator.generateIdWithPrefix("ptd")
            val dayNumber = index + 1
            val title = text(ref, "title")?.takeIf { it.codePointCount(0, it.length) <= 100 } ?: "参考行程"
            val range = range(basis.mainTrackPath, ref.path("mainTrackRange"))
            if (range != null) {
                if (precedingEnd?.let { range.first < it } == true) throw failure()
                precedingEnd = range.second
            }
            val track = TripReferenceTrackProjection(
                path = range?.third,
                distance = meters(ref, "distance"), ascent = meters(ref, "ascent"), descent = meters(ref, "descent"),
                estimatedDuration = scalar(ref, "estimatedDuration", "seconds")?.let(::RouteSeconds),
                confidence = source
            ).takeIf { it.path != null || it.distance != null || it.ascent != null || it.descent != null || it.estimatedDuration != null }
            val guide = TripRouteGuideProjection(ids[index], qualifiedText(ref, "description"), qualifiedText(ref, "notes"), accommodation(ref), track)
            val actions = buildList {
                var sequence = 1
                if (index == 0) add(transport(sequence++, PublicRoutePlace(departureCity), basis.start))
                // A reference-day journey can include access transport; it does not establish a pure hike.
                add(TripActionProjection(sequence = sequence++, actionType = "hike", routeSectionConfidence = unavailable,
                    start = unknown(), end = unknown(), distance = unknown(), ascent = unknown(), estimatedDuration = unknown()))
                if (index == refs.lastIndex) add(transport(sequence, basis.end, PublicRoutePlace(departureCity)))
            }
            val day = TripDayProjection(identity = id, dayNumber = dayNumber, date = startDate.plusDays(index.toLong()), primaryStage = title,
                hikingDayNumber = dayNumber, actions = actions,
                weather = TripDayWeatherProjection(unknown(), unknown(), unknown(), unknown(), unknown()), routeGuide = guide)
            PersonalTripDayRecord(id, tripId, dayNumber, day.date, title, dayNumber, mapper.writeValueAsString(day))
        }
    }

    private fun accommodation(ref: JsonNode): QualifiedValueProjection<String>? {
        val value = text(ref, "accommodation") ?: return null
        val evidence = ref.path("accommodationEvidence")
        if (!evidence.isMissingNode && !evidence.isNull) {
            if (text(evidence, "value") != value) throw failure()
            val confidence = evidence.get("confidence")?.takeUnless { it.isNull }?.let { raw ->
                try { mapper.treeToValue(raw, InformationConfidenceProjection::class.java) } catch (_: Exception) { throw failure() }
            }
            return QualifiedValueProjection(value, confidence)
        }
        return QualifiedValueProjection(value, source)
    }
    private fun qualifiedText(node: JsonNode, field: String) = text(node, field)?.let { QualifiedValueProjection(it, source) }
    private fun text(node: JsonNode, field: String): String? = node.get(field)?.let {
        if (it.isNull) return@let null
        if (!it.isTextual || it.asText().isBlank()) throw failure()
        it.asText()
    }
    private fun scalar(node: JsonNode, field: String, unit: String): Double? = node.get(field)?.let {
        if (it.isNull) return@let null
        val value = it.path(unit)
        if (!value.isNumber || !value.asDouble().isFinite() || value.asDouble() < 0) throw failure()
        value.asDouble()
    }
    private fun meters(node: JsonNode, field: String) = scalar(node, field, "meters")?.let(::RouteMeters)
    private fun <T> unknown(): QualifiedValueProjection<T> = QualifiedValueProjection(confidence = unavailable)
    private fun transport(sequence: Int, origin: PublicRoutePlace, destination: PublicRoutePlace) = TripActionProjection(sequence = sequence,
        actionType = "long_distance_transport", origin = QualifiedValueProjection(value = origin), destination = QualifiedValueProjection(value = destination),
        mode = unknown(), keyTimes = unknown(), estimatedDuration = unknown(), transferNotes = unknown())

    private fun range(path: List<PublicRouteGeoPosition>, node: JsonNode): Triple<Double, Double, List<PublicRouteGeoPosition>>? {
        if (node.isMissingNode || node.isNull) return null
        fun endpoint(position: JsonNode): Pair<Double, PublicRouteGeoPosition> {
            val raw = position.path("precedingPositionIndex")
            if (!raw.isIntegralNumber || !raw.canConvertToInt()) throw failure()
            val index = raw.asInt()
            if (index !in path.indices) throw failure()
            val rawFraction = position.path("progressToNextPosition")
            val final = index == path.lastIndex
            if ((final && !rawFraction.isMissingNode && !rawFraction.isNull) || (!final && !rawFraction.isNumber)) throw failure()
            val fraction = if (final) 0.0 else rawFraction.asDouble()
            if (!fraction.isFinite() || fraction < 0 || fraction >= 1) throw failure()
            val p = path[index]
            val point = if (fraction == 0.0) p else path[index + 1].let { next ->
                if (next.referenceSystem != p.referenceSystem) throw failure()
                PublicRouteGeoPosition(p.latitude + (next.latitude - p.latitude) * fraction, p.longitude + (next.longitude - p.longitude) * fraction, p.referenceSystem)
            }
            return index + fraction to point
        }
        val start = endpoint(node.path("startPathPosition")); val end = endpoint(node.path("endPathPosition"))
        if (start.first >= end.first) throw failure()
        return Triple(start.first, end.first, buildList {
            add(start.second)
            path.indices.filter { it.toDouble() > start.first && it.toDouble() < end.first }.forEach { add(path[it]) }
            add(end.second)
        })
    }
    private fun failure() = ApiContractException.serviceUnavailable("trip_generation_failed", "参考分日资料无法形成一致的行程结果")
}
