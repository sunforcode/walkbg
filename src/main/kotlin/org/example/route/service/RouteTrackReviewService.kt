package org.example.route.service

import com.fasterxml.jackson.databind.DeserializationFeature
import com.fasterxml.jackson.databind.ObjectMapper
import org.example.common.exception.BusinessException
import org.example.route.dto.MainTrackReviewRecordDto
import org.example.route.dto.MainTrackReviewRequest
import org.example.route.dto.MainTrackReviewResponse
import org.example.route.model.Route
import org.example.route.model.RouteTrackReview
import org.example.route.repository.RouteCurrentPublicVersionRepository
import org.example.route.repository.RouteRepository
import org.example.route.repository.RouteTrackReviewRepository
import org.example.route.repository.RouteVersionRepository
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.security.MessageDigest
import java.util.UUID

/** A current candidate decision is separate from immutable public versions. */
@Service
class RouteTrackReviewService(
    private val routes: RouteRepository,
    private val reviews: RouteTrackReviewRepository,
    private val current: RouteCurrentPublicVersionRepository,
    private val versions: RouteVersionRepository,
    private val objectMapper: ObjectMapper
) {
    data class PublicationTrack(val availability: String, val json: String? = null, val referenceSystem: String? = null)
    private data class Candidate(val id: String?, val path: List<List<Double?>>, val error: String?)

    @Transactional(readOnly = true)
    fun read(routeId: String): MainTrackReviewResponse {
        val route = routes.findById(routeId).orElseThrow { BusinessException.notFound("路线不存在") }
        return response(route, candidate(route), reviews.findFirstByRouteIdOrderByRevisionDesc(routeId))
    }

    @Transactional
    fun submit(routeId: String, request: MainTrackReviewRequest): MainTrackReviewResponse {
        validateRequest(request)
        val route = routes.findByIdForUpdate(routeId) ?: throw BusinessException.notFound("路线不存在")
        if (route.hasActiveAnalysis()) throw BusinessException.conflict("路线分析中，请完成后重新读取审核候选")
        val candidate = candidate(route)
        if (candidate.id == null || candidate.error != null) {
            throw BusinessException.unprocessableEntity(candidate.error ?: "缺少候选主轨迹")
        }
        if (candidate.id != request.candidateId) throw BusinessException.conflict("候选轨迹已变化，请重新查看并审核")
        val previous = reviews.findByRouteIdAndRequestId(routeId, request.requestId)
        if (previous != null) {
            if (previous.candidateId != request.candidateId || previous.expectedRevision != request.expectedRevision ||
                previous.decision != request.decision || previous.completeHikingRangeConfirmed != request.confirmCompleteHikingRange ||
                previous.referenceSystem != request.referenceSystem || previous.reason != request.reason) {
                throw BusinessException.conflict("审核请求身份已用于其他参数，请重新读取后提交")
            }
            val latest = reviews.findFirstByRouteIdOrderByRevisionDesc(routeId)
            if (latest?.id != previous.id) throw BusinessException.conflict("审核结论已更新，请重新读取")
            return response(route, candidate, previous)
        }
        val latestRevision = reviews.findFirstByRouteIdOrderByRevisionDesc(routeId)?.revision ?: 0L
        if (request.expectedRevision != latestRevision) throw BusinessException.conflict("审核记录已更新，请重新查看后确认")
        val record = reviews.saveAndFlush(RouteTrackReview(
            id = UUID.randomUUID().toString(), routeId = routeId, candidateId = candidate.id,
            revision = Math.addExact(latestRevision, 1), expectedRevision = request.expectedRevision,
            requestId = request.requestId, decision = request.decision,
            completeHikingRangeConfirmed = request.confirmCompleteHikingRange,
            referenceSystem = request.referenceSystem, reason = request.reason
        ))
        return response(route, candidate, record)
    }

    /** Called while the publication transaction already holds the route row lock. */
    fun publicationTrack(route: Route): PublicationTrack {
        val candidate = candidate(route)
        if (candidate.id == null) return PublicationTrack("missing")
        val review = reviews.findFirstByRouteIdOrderByRevisionDesc(route.id)
        if (review?.candidateId != candidate.id) return PublicationTrack("pending_review")
        if (review.decision == "rejected") return PublicationTrack("invalidated")
        if (candidate.error != null || review.decision != "approved" || !review.completeHikingRangeConfirmed ||
            review.referenceSystem.isNullOrBlank()) return PublicationTrack("pending_review")
        return PublicationTrack("valid", route.trackGeoJson, review.referenceSystem)
    }

    private fun validateRequest(request: MainTrackReviewRequest) {
        if (request.candidateId.isBlank() || request.candidateId.length > 64 || request.expectedRevision < 0 ||
            request.requestId.isBlank() || request.requestId.length > 64) {
            throw BusinessException.badRequest("候选、审核版本或请求身份无效")
        }
        if (request.reason != null && (request.reason.isBlank() || request.reason.length > 1000)) {
            throw BusinessException.badRequest("审核原因必须为非空文字且不超过1000字")
        }
        when (request.decision) {
            "approved" -> if (!request.confirmCompleteHikingRange || request.referenceSystem.isNullOrBlank() ||
                request.referenceSystem.length > 64) {
                throw BusinessException.badRequest("通过需明确确认完整徒步范围并声明坐标参考系统")
            }
            "rejected" -> if (request.confirmCompleteHikingRange || request.referenceSystem != null || request.reason.isNullOrBlank()) {
                throw BusinessException.badRequest("驳回需填写原因，不能携带通过确认或坐标声明")
            }
            else -> throw BusinessException.badRequest("审核结论只允许approved或rejected")
        }
    }

    private fun candidate(route: Route): Candidate {
        val raw = route.trackGeoJson?.takeIf { it.isNotBlank() }
            ?: return Candidate(null, emptyList(), "没有候选主轨迹，请先提供轨迹或完成分析")
        val digest = MessageDigest.getInstance("SHA-256")
        listOf(route.id, route.analysisTaskId.orEmpty(), raw).forEach {
            digest.update(it.toByteArray(Charsets.UTF_8))
            digest.update(0.toByte())
        }
        val id = digest.digest().joinToString("") { "%02x".format(it) }
        try {
            val data = objectMapper.reader().with(DeserializationFeature.FAIL_ON_TRAILING_TOKENS)
                .readTree(raw)
            if (!data.isArray || data.isEmpty) return Candidate(id, emptyList(), "候选轨迹为空或结构不合法")
            val path = data.map { point ->
                if (!point.isArray || point.size() !in 2..3) throw IllegalArgumentException("invalid point shape")
                point.mapIndexed { index, value ->
                    if (index == 2 && value.isNull) null else {
                        if (!value.isNumber) throw IllegalArgumentException("coordinate must be numeric")
                        val number = value.asDouble()
                        if (!number.isFinite() || (index == 0 && number !in -90.0..90.0) ||
                            (index == 1 && number !in -180.0..180.0)) throw IllegalArgumentException("invalid coordinate")
                        number
                    }
                }
            }
            return Candidate(id, path, null)
        } catch (_: Exception) {
            return Candidate(id, emptyList(), "候选轨迹包含无效坐标或结构，不能删点后自动审核")
        }
    }

    private fun response(route: Route, candidate: Candidate, latest: RouteTrackReview?): MainTrackReviewResponse {
        val version = current.findById(route.id).orElse(null)?.routeVersionId
            ?.let { versions.findById(it).orElse(null) }?.takeIf { it.routeId == route.id }
        val review = latest?.takeIf { it.candidateId == candidate.id }
        return MainTrackReviewResponse(
            routeId = route.id, candidateId = candidate.id, candidatePath = candidate.path,
            geometryValid = candidate.error == null, validationError = candidate.error,
            analysisActive = route.hasActiveAnalysis(), reviewRevision = latest?.revision ?: 0L,
            review = review?.let { MainTrackReviewRecordDto(it.id, it.candidateId, it.revision,
                it.requestId, it.decision, it.completeHikingRangeConfirmed, it.referenceSystem, it.reason, it.reviewedAt) },
            publishedVersionId = version?.id, publishedMainTrackAvailability = version?.mainTrackAvailability
        )
    }
}
