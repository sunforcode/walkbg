package org.example.route.dto

import com.fasterxml.jackson.annotation.JsonProperty
import java.time.Instant

/** Internal candidate review; it never represents an automatic public approval. */
data class MainTrackReviewRequest(
    @JsonProperty("candidate_id") val candidateId: String,
    @JsonProperty("expected_revision") val expectedRevision: Long,
    @JsonProperty("request_id") val requestId: String,
    val decision: String,
    @JsonProperty("confirm_complete_hiking_range") val confirmCompleteHikingRange: Boolean,
    @JsonProperty("reference_system") val referenceSystem: String? = null,
    val reason: String? = null
)

data class MainTrackReviewRecordDto(
    @JsonProperty("review_id") val reviewId: String,
    @JsonProperty("candidate_id") val candidateId: String,
    val revision: Long,
    @JsonProperty("request_id") val requestId: String,
    val decision: String,
    @JsonProperty("complete_hiking_range_confirmed") val completeHikingRangeConfirmed: Boolean,
    @JsonProperty("reference_system") val referenceSystem: String?,
    val reason: String?,
    @JsonProperty("reviewed_at") val reviewedAt: Instant
)

data class MainTrackReviewResponse(
    @JsonProperty("route_id") val routeId: String,
    @JsonProperty("candidate_id") val candidateId: String?,
    @JsonProperty("candidate_path") val candidatePath: List<List<Double?>>,
    @JsonProperty("geometry_valid") val geometryValid: Boolean,
    @JsonProperty("validation_error") val validationError: String?,
    @JsonProperty("analysis_active") val analysisActive: Boolean,
    @JsonProperty("review_revision") val reviewRevision: Long,
    val review: MainTrackReviewRecordDto?,
    @JsonProperty("published_version_id") val publishedVersionId: String?,
    @JsonProperty("published_main_track_availability") val publishedMainTrackAvailability: String?
)
