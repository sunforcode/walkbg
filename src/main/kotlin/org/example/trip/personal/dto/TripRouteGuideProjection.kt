package org.example.trip.personal.dto

import com.fasterxml.jackson.annotation.JsonInclude
import org.example.route.dto.PublicRouteGeoPosition
import org.example.route.dto.RouteMeters
import org.example.route.dto.RouteSeconds

/** Frozen source material for the explicitly adopted reference day, not pure-hike facts. */
@JsonInclude(JsonInclude.Include.NON_NULL)
data class TripRouteGuideProjection(
    val sourceReferenceDayId: String,
    val description: QualifiedValueProjection<String>? = null,
    val notes: QualifiedValueProjection<String>? = null,
    val accommodationReference: QualifiedValueProjection<String>? = null,
    val referenceTrack: TripReferenceTrackProjection? = null
)

@JsonInclude(JsonInclude.Include.NON_NULL)
data class TripReferenceTrackProjection(
    val path: List<PublicRouteGeoPosition>? = null,
    val distance: RouteMeters? = null,
    val ascent: RouteMeters? = null,
    val descent: RouteMeters? = null,
    val estimatedDuration: RouteSeconds? = null,
    val confidence: InformationConfidenceProjection
)
