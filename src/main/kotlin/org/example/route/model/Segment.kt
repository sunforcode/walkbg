package org.example.route.model

import jakarta.persistence.*
import java.time.Instant

/**
 * 路段实体（单向关联）
 */
@Entity
@Table(
    name = "segments",
    indexes = [
        Index(name = "idx_segments_route_id", columnList = "route_id"),
        Index(name = "idx_segments_start_point", columnList = "start_point_id"),
        Index(name = "idx_segments_end_point", columnList = "end_point_id")
    ]
)
data class Segment(
    @Id
    @Column(length = 64)
    val id: String,

    @Column(name = "route_id", length = 64, nullable = false)
    val routeId: String,

    @Column(nullable = false, length = 200)
    var name: String,

    @Column(columnDefinition = "TEXT")
    var description: String? = null,

    val distance: Double? = null,

    @Column(name = "elevation_gain")
    val elevationGain: Double? = null,

    @Column(name = "elevation_loss")
    val elevationLoss: Double? = null,

    @Column(name = "estimated_time")
    val estimatedTime: Double? = null,

    val difficulty: Int? = null,

    @Column(name = "route_type")
    val routeType: Int? = null,

    @Column(columnDefinition = "TEXT")
    var notes: String? = null,

    @Column(name = "start_point_id", length = 64)
    val startPointId: String? = null,

    @Column(name = "end_point_id", length = 64)
    val endPointId: String? = null,

    @Column(name = "created_at", nullable = false, updatable = false)
    val createdAt: Instant = Instant.now(),

    @Column(name = "updated_at", nullable = false)
    var updatedAt: Instant = Instant.now()
) {
    /**
     * 注意：不再持有以下关联关系的集合引用
     * - pathPoints: 通过 PathPointRepository.findBySegmentId(segmentId) 查询
     * - keypoints: 通过 SegmentKeypointRepository.findBySegmentId(segmentId) 查询
     * - closures: 通过 SegmentClosureRepository.findBySegmentId(segmentId) 查询
     */

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false
        other as Segment
        return id == other.id
    }

    override fun hashCode(): Int {
        return id.hashCode()
    }

    override fun toString(): String {
        return "Segment(id='$id', name='$name', routeId='$routeId')"
    }
}

/**
 * 路段关键点实体
 */
@Entity
@Table(name = "segment_keypoints")
data class SegmentKeypoint(
    @Id
    @Column(length = 64)
    val id: String,

    @Column(name = "waypoint_id", nullable = false, length = 64)
    val waypointId: String,

    @Column(name = "sequence_number", nullable = false)
    val sequenceNumber: Int,

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "segment_id")
    var segment: Segment? = null,

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "waypoint_id", insertable = false, updatable = false)
    var waypoint: Waypoint? = null
) {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as SegmentKeypoint

        return id == other.id
    }

    override fun hashCode(): Int {
        return id.hashCode()
    }

    override fun toString(): String {
        return "SegmentKeypoint(id='$id', waypointId='$waypointId', sequenceNumber=$sequenceNumber)"
    }
}
