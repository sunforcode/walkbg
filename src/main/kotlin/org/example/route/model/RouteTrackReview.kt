package org.example.route.model

import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.Id
import jakarta.persistence.Table
import jakarta.persistence.UniqueConstraint
import java.time.Instant

@Entity
@Table(name = "route_track_reviews", uniqueConstraints = [
    UniqueConstraint(name = "uk_route_track_review_revision", columnNames = ["route_id", "revision"]),
    UniqueConstraint(name = "uk_route_track_review_request", columnNames = ["route_id", "request_id"])
])
data class RouteTrackReview(
    @Id @Column(length = 64) val id: String,
    @Column(name = "route_id", nullable = false, length = 64) val routeId: String,
    @Column(name = "candidate_id", nullable = false, length = 64) val candidateId: String,
    @Column(nullable = false) val revision: Long,
    @Column(name = "expected_revision", nullable = false) val expectedRevision: Long,
    @Column(name = "request_id", nullable = false, length = 64) val requestId: String,
    @Column(nullable = false, length = 16) val decision: String,
    @Column(name = "complete_hiking_range_confirmed", nullable = false) val completeHikingRangeConfirmed: Boolean,
    @Column(name = "reference_system", length = 64) val referenceSystem: String?,
    @Column(length = 1000) val reason: String?,
    @Column(name = "reviewed_at", nullable = false, updatable = false) val reviewedAt: Instant = Instant.now()
)
