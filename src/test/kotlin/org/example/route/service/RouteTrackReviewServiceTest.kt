package org.example.route.service

import com.fasterxml.jackson.databind.ObjectMapper
import org.example.common.exception.BusinessException
import org.example.route.dto.MainTrackReviewRequest
import org.example.route.model.Route
import org.example.route.model.RouteTrackReview
import org.example.route.repository.*
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test
import org.mockito.kotlin.*
import org.springframework.http.HttpStatus
import java.util.Optional

class RouteTrackReviewServiceTest {
    private class Fixture {
        val route = Route(id = "route-review", name = "测试路线", createdBy = "operator",
            trackGeoJson = "[[30.0,120.0,100.0],[30.01,120.01,110.0]]", analysisTaskId = "task-a")
        val routes = mock<RouteRepository>()
        val reviews = mock<RouteTrackReviewRepository>()
        val versions = mock<RouteVersionRepository>()
        val current = mock<RouteCurrentPublicVersionRepository>()
        val stored = mutableListOf<RouteTrackReview>()
        val service = RouteTrackReviewService(routes, reviews, current, versions, ObjectMapper())

        init {
            whenever(routes.findById(route.id)).thenAnswer { Optional.of(route) }
            whenever(routes.findByIdForUpdate(route.id)).thenAnswer { route }
            whenever(current.findById(route.id)).thenReturn(Optional.empty())
            whenever(reviews.findFirstByRouteIdOrderByRevisionDesc(route.id)).thenAnswer { stored.maxByOrNull { it.revision } }
            whenever(reviews.findByRouteIdAndRequestId(eq(route.id), any())).thenAnswer { call ->
                stored.singleOrNull { it.requestId == call.getArgument<String>(1) }
            }
            whenever(reviews.saveAndFlush(any<RouteTrackReview>())).thenAnswer { call ->
                call.getArgument<RouteTrackReview>(0).also { stored.add(it) }
            }
        }

        fun approved(requestId: String = "review-1") = service.read(route.id).let {
            MainTrackReviewRequest(candidateId = requireNotNull(it.candidateId), expectedRevision = it.reviewRevision,
                requestId = requestId, decision = "approved", confirmCompleteHikingRange = true, referenceSystem = "WGS84")
        }
    }

    @Test
    fun `read exposes the exact candidate without creating approval or public version`() {
        val f = Fixture()
        val result = f.service.read(f.route.id)
        assertTrue(result.geometryValid)
        assertEquals(listOf(listOf(30.0, 120.0, 100.0), listOf(30.01, 120.01, 110.0)), result.candidatePath)
        assertNotNull(result.candidateId)
        assertNull(result.review)
        assertEquals(0L, result.reviewRevision)
        verify(f.reviews, never()).saveAndFlush(any())
        verify(f.versions, never()).saveAndFlush(any())
    }

    @Test
    fun `approve is bound to the candidate and becomes publication input without publishing`() {
        val f = Fixture()
        val result = f.service.submit(f.route.id, f.approved())
        assertEquals("approved", result.review?.decision)
        assertEquals("WGS84", result.review?.referenceSystem)
        assertTrue(result.review?.completeHikingRangeConfirmed == true)
        assertEquals(1L, result.reviewRevision)
        assertEquals("valid", f.service.publicationTrack(f.route).availability)
        assertEquals(f.route.trackGeoJson, f.service.publicationTrack(f.route).json)
        assertEquals(0, f.route.status)
        verify(f.versions, never()).saveAndFlush(any())
    }

    @Test
    fun `rejection requires a reason and never supplies public geometry`() {
        val f = Fixture()
        val approved = f.approved()
        val request = approved.copy(decision = "rejected", confirmCompleteHikingRange = false,
            referenceSystem = null, reason = "包含接驳车辆，不是完整徒步范围")
        val result = f.service.submit(f.route.id, request)
        assertEquals("rejected", result.review?.decision)
        assertEquals(request.reason, result.review?.reason)
        assertEquals("invalidated", f.service.publicationTrack(f.route).availability)
        assertNull(f.service.publicationTrack(f.route).json)
    }

    @Test
    fun `approval requires explicit complete range confirmation and coordinate system`() {
        val f = Fixture()
        val valid = f.approved()
        listOf(valid.copy(confirmCompleteHikingRange = false), valid.copy(referenceSystem = null),
            valid.copy(referenceSystem = " "), valid.copy(decision = "valid"),
            valid.copy(decision = "rejected", reason = "not suitable"),
            valid.copy(requestId = ""), valid.copy(expectedRevision = -1)).forEach { request ->
            val error = assertThrows(BusinessException::class.java) { f.service.submit(f.route.id, request) }
            assertEquals(HttpStatus.BAD_REQUEST, error.httpStatus)
        }
        assertTrue(f.stored.isEmpty())
    }

    @Test
    fun `same request is idempotent and changed payload cannot overwrite the decision`() {
        val f = Fixture()
        val request = f.approved()
        val first = f.service.submit(f.route.id, request)
        val repeated = f.service.submit(f.route.id, request)
        assertEquals(first.review?.reviewId, repeated.review?.reviewId)
        assertEquals(1, f.stored.size)
        val error = assertThrows(BusinessException::class.java) {
            f.service.submit(f.route.id, request.copy(referenceSystem = "GCJ02"))
        }
        assertEquals(HttpStatus.CONFLICT, error.httpStatus)
        assertEquals("WGS84", f.stored.single().referenceSystem)
    }

    @Test
    fun `stale candidate and same coordinates from a new analysis cannot reuse approval`() {
        val f = Fixture()
        val request = f.approved()
        f.service.submit(f.route.id, request)
        f.route.analysisTaskId = "task-b"
        assertNotEquals(request.candidateId, f.service.read(f.route.id).candidateId)
        assertNull(f.service.read(f.route.id).review)
        assertEquals("pending_review", f.service.publicationTrack(f.route).availability)
        assertEquals(HttpStatus.CONFLICT, assertThrows(BusinessException::class.java) {
            f.service.submit(f.route.id, request.copy(requestId = "old-page"))
        }.httpStatus)
        f.route.analysisTaskId = "task-a"
        f.route.trackGeoJson = "[[31.0,121.0],[31.1,121.1]]"
        assertEquals(HttpStatus.CONFLICT, assertThrows(BusinessException::class.java) {
            f.service.submit(f.route.id, request.copy(requestId = "changed-track"))
        }.httpStatus)
    }

    @Test
    fun `another review revision makes a previously opened page stale`() {
        val f = Fixture()
        val first = f.approved("first")
        val second = f.approved("second")
        f.service.submit(f.route.id, first)
        assertEquals(HttpStatus.CONFLICT, assertThrows(BusinessException::class.java) {
            f.service.submit(f.route.id, second)
        }.httpStatus)
        val fresh = f.approved("third").copy(decision = "rejected", referenceSystem = null,
            confirmCompleteHikingRange = false, reason = "需修正范围")
        assertEquals(2L, f.service.submit(f.route.id, fresh).reviewRevision)
        assertEquals(listOf("approved", "rejected"), f.stored.map { it.decision })
    }

    @Test
    fun `missing and malformed candidates cannot become approved by filtering points`() {
        val f = Fixture()
        listOf<String?>(null, "", "[]", "not-json", "[[30,120],[91,121]]", "[[30,120],[30]]",
            "[[\"30\",120],[30,121]]", "[[30,120,null,1]]").forEach { raw ->
            f.route.trackGeoJson = raw
            val view = f.service.read(f.route.id)
            assertFalse(view.geometryValid)
            assertNotNull(view.validationError)
            val request = MainTrackReviewRequest(view.candidateId ?: "missing", view.reviewRevision,
                "invalid", "approved", true, "WGS84")
            val error = assertThrows(BusinessException::class.java) { f.service.submit(f.route.id, request) }
            assertEquals(HttpStatus.UNPROCESSABLE_ENTITY, error.httpStatus)
        }
        assertTrue(f.stored.isEmpty())
    }

    @Test
    fun `candidate JSON with trailing content is rejected in full`() {
        val f = Fixture()
        f.route.trackGeoJson = "[[30,120],[30.01,120.01]] {\"unexpected\":true}"
        val view = f.service.read(f.route.id)
        assertFalse(view.geometryValid)
        assertTrue(view.candidatePath.isEmpty())
        assertEquals(HttpStatus.UNPROCESSABLE_ENTITY, assertThrows(BusinessException::class.java) {
            f.service.submit(f.route.id, f.approved())
        }.httpStatus)
        assertTrue(f.stored.isEmpty())
    }

    @Test
    fun `superseded request retry cannot restore an earlier approval`() {
        val f = Fixture()
        val first = f.approved("first")
        f.service.submit(f.route.id, first)
        f.service.submit(f.route.id, f.approved("second").copy(decision = "rejected",
            confirmCompleteHikingRange = false, referenceSystem = null, reason = "审核修正"))
        assertEquals(HttpStatus.CONFLICT, assertThrows(BusinessException::class.java) {
            f.service.submit(f.route.id, first)
        }.httpStatus)
        assertEquals("rejected", f.service.read(f.route.id).review?.decision)
        assertEquals(2, f.stored.size)
    }

    @Test
    fun `active analysis blocks review independently of route status integer`() {
        val f = Fixture()
        val request = f.approved()
        f.route.analysisStatus = "processing"
        assertTrue(f.service.read(f.route.id).analysisActive)
        assertEquals(HttpStatus.CONFLICT, assertThrows(BusinessException::class.java) {
            f.service.submit(f.route.id, request)
        }.httpStatus)
        assertTrue(f.stored.isEmpty())
    }
}
