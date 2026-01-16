package org.example.route.model

import jakarta.persistence.*
import java.time.Instant

/**
 * 路径点实体（单向关联）
 */
@Entity
@Table(
    name = "path_points",
    indexes = [
        Index(name = "idx_path_points_segment_id", columnList = "segment_id"),
        Index(name = "idx_path_points_sequence", columnList = "sequence_number"),
        Index(name = "idx_path_points_type", columnList = "point_type")
    ]
)
data class PathPoint(
    @Id
    @Column(length = 64)
    val id: String,

    @Column(name = "segment_id", length = 64, nullable = false)
    val segmentId: String,
    
    @Column(nullable = false)
    val latitude: Double,

    @Column(nullable = false)
    val longitude: Double,

    val elevation: Double? = null,

    @Column(name = "timestamp")
    val timestamp: Instant? = null,

    @Column(name = "distance_from_start")
    val distanceFromStart: Double? = null,

    @Column(name = "point_type", length = 50)
    val pointType: String? = null,

    @Column(length = 200)
    val name: String? = null,
    
    @Column(columnDefinition = "TEXT")
    val description: String? = null,
    
    @Column(length = 50)
    val type: String? = null,

    @Column(name = "sequence_number", nullable = false)
    val sequenceNumber: Int,
    
    @Column(name = "created_at", nullable = false, updatable = false)
    val createdAt: Instant = Instant.now(),

    @Column(name = "updated_at", nullable = false)
    var updatedAt: Instant = Instant.now()
) {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as PathPoint

        return id == other.id
    }

    override fun hashCode(): Int {
        return id.hashCode()
    }

    override fun toString(): String {
        return "PathPoint(id='$id', name='$name', sequenceNumber=$sequenceNumber)"
    }
}

/**
 * 路段危险点实体（单向关联）
 */
@Entity
@Table(
    name = "segment_hazards",
    indexes = [
        Index(name = "idx_segment_hazards_segment_id", columnList = "segment_id"),
        Index(name = "idx_segment_hazards_hazard", columnList = "hazard"),
        Index(name = "idx_segment_hazards_severity", columnList = "severity_level")
    ]
)
data class SegmentHazard(
    @Id
    @Column(length = 64)
    val id: String,

    @Column(name = "segment_id", length = 64, nullable = false)
    val segmentId: String,
    
    @Column(nullable = false, length = 100)
    val hazard: String,
    
    @Column(columnDefinition = "TEXT")
    val description: String? = null,

    @Column(name = "severity_level")
    val severityLevel: Int? = null, // 1-5, 1为最低，5为最高

    @Column(name = "created_at", nullable = false, updatable = false)
    val createdAt: Instant = Instant.now(),

    @Column(name = "updated_at", nullable = false)
    var updatedAt: Instant = Instant.now()
) {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as SegmentHazard

        return id == other.id
    }

    override fun hashCode(): Int {
        return id.hashCode()
    }

    override fun toString(): String {
        return "SegmentHazard(id='$id', hazard='$hazard', severityLevel=$severityLevel)"
    }
}

/**
 * 路段封闭信息实体（单向关联）
 */
@Entity
@Table(
    name = "segment_closures",
    indexes = [
        Index(name = "idx_segment_closures_segment_id", columnList = "segment_id"),
        Index(name = "idx_segment_closures_dates", columnList = "start_date, end_date"),
        Index(name = "idx_segment_closures_type", columnList = "closure_type")
    ]
)
data class SegmentClosure(
    @Id
    @Column(length = 64)
    val id: String,

    @Column(name = "segment_id", length = 64, nullable = false)
    val segmentId: String,
    
    @Column(name = "start_date")
    val startDate: Instant? = null,

    @Column(name = "end_date")
    val endDate: Instant? = null,
    
    @Column(columnDefinition = "TEXT")
    val reason: String? = null,
    
    @Column(name = "closure_type", length = 50)
    val closureType: String? = null, // temporary, permanent, seasonal

    @Column(name = "created_at", nullable = false, updatable = false)
    val createdAt: Instant = Instant.now(),

    @Column(name = "updated_at", nullable = false)
    var updatedAt: Instant = Instant.now()
) {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as SegmentClosure

        return id == other.id
    }

    override fun hashCode(): Int {
        return id.hashCode()
    }

    override fun toString(): String {
        return "SegmentClosure(id='$id', reason='$reason')"
    }
}
