package org.example.route.repository

import org.example.route.model.RouteTrackReview
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.stereotype.Repository

@Repository
interface RouteTrackReviewRepository : JpaRepository<RouteTrackReview, String> {
    fun findFirstByRouteIdOrderByRevisionDesc(routeId: String): RouteTrackReview?
    fun findByRouteIdAndRequestId(routeId: String, requestId: String): RouteTrackReview?
}
