package org.example.route.model

import jakarta.persistence.*
import java.time.Instant

/**
 * 路段实体
 */
@Entity
@Table(name = "segments")
data class Segment(
    @Id
    @Column(length = 64)
    val id: String,

    @Column(nullable = false, length = 200)
    var name: String,

    @Column(columnDefinition = "TEXT")
    var description: String? = null,

    val distance: Double? = null,
    val elevationGain: Double? = null,
    val elevationLoss: Double? = null,
    val estimatedTime: Double? = null,
    val difficulty: Int? = null,

    @Column(name = "route_type")
    val routeType: Int? = null, // 路线类型

    @Column(columnDefinition = "TEXT")
    var notes: String? = null,

    @Column(name = "created_at", nullable = false, updatable = false)
    val createdAt: Instant = Instant.now(),

    @Column(name = "updated_at", nullable = false)
    var updatedAt: Instant = Instant.now(),

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "route_id")
    var route: Route? = null,

    @Column(name = "start_point_id", length = 64)
    var startPointId: String? = null,

    @Column(name = "end_point_id", length = 64)
    var endPointId: String? = null,

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "start_point_id", insertable = false, updatable = false)
    var startPoint: Waypoint? = null,

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "end_point_id", insertable = false, updatable = false)
    var endPoint: Waypoint? = null,

    @OneToMany(mappedBy = "segment", cascade = [CascadeType.ALL], orphanRemoval = true)
    val pathPoints: MutableList<PathPoint> = mutableListOf(),

    @OneToMany(mappedBy = "segment", cascade = [CascadeType.ALL], orphanRemoval = true)
    val keypoints: MutableList<SegmentKeypoint> = mutableListOf(),

    @OneToMany(mappedBy = "segment", cascade = [CascadeType.ALL], orphanRemoval = true)
    val closures: MutableList<SegmentClosure> = mutableListOf()
) {
    fun addPathPoint(pathPoint: PathPoint) {
        pathPoints.add(pathPoint)
        pathPoint.segment = this
    }

    fun addKeypoint(waypointId: String, sequenceNumber: Int = keypoints.size + 1) {
        keypoints.add(SegmentKeypoint(
            id = java.util.UUID.randomUUID().toString(),
            waypointId = waypointId,
            sequenceNumber = sequenceNumber,
            segment = this
        ))
    }

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
        return "Segment(id='$id', name='$name')"
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
