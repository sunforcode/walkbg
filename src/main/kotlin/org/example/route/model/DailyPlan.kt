package org.example.route.model

import com.fasterxml.jackson.annotation.JsonProperty
import jakarta.persistence.*
import java.time.Instant

/**
 * 每日计划实体（单向关联）
 */
@Entity
@Table(
    name = "daily_plans",
    indexes = [
        Index(name = "idx_daily_plans_route_id", columnList = "route_id"),
        Index(name = "idx_daily_plans_day_number", columnList = "day_number")
    ]
)
data class DailyPlan(
    @Id
    @Column(length = 64)
    val id: String,

    @Column(name = "route_id", length = 64, nullable = false)
    val routeId: String,

    @Column(name = "day_number", nullable = false)
    val dayNumber: Int,

    @Column(nullable = false, length = 200)
    val title: String,
    
    @Column(columnDefinition = "TEXT")
    val description: String? = null,
    
    val distance: Double? = null,

    @Column(name = "estimated_time")
    val estimatedTime: Double? = null,

    @Column(name = "elevation_gain")
    val elevationGain: Int? = null,

    @Column(name = "elevation_loss")
    val elevationLoss: Double? = null,

    @Column(name = "max_elevation")
    val maxElevation: Double? = null,

    @Column(name = "min_elevation")
    val minElevation: Double? = null,

    @Column(length = 200)
    val accommodation: String? = null,

    @Column(columnDefinition = "TEXT")
    val notes: String? = null,

    @Column(name = "created_at", nullable = false, updatable = false)
    val createdAt: Instant = Instant.now(),

    @Column(name = "updated_at", nullable = false)
    var updatedAt: Instant = Instant.now()
) {
    /**
     * 注意：不再持有 segments 集合
     * 通过 DailyPlanSegmentRepository.findByDailyPlanId(dailyPlanId) 查询
     */
    
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as DailyPlan

        return id == other.id
    }
    
    override fun hashCode(): Int {
        return id.hashCode()
    }
    
    override fun toString(): String {
        return "DailyPlan(id='$id', routeId='$routeId', dayNumber=$dayNumber, title='$title')"
    }
}

/**
 * 每日计划-路段关联表（单向关联）
 */
@Entity
@Table(
    name = "daily_plan_segments",
    indexes = [
        Index(name = "idx_daily_plan_segments_plan_id", columnList = "daily_plan_id"),
        Index(name = "idx_daily_plan_segments_segment_id", columnList = "segment_id")
    ]
)
data class DailyPlanSegment(
    @Id
    @Column(length = 64)
    val id: String,

    @Column(name = "daily_plan_id", length = 64, nullable = false)
    val dailyPlanId: String,

    @Column(name = "segment_id", length = 64, nullable = false)
    val segmentId: String,
    
    @Column(nullable = false)
    val sequenceNumber: Int
) {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as DailyPlanSegment

        return id == other.id
    }

    override fun hashCode(): Int {
        return id.hashCode()
    }

    override fun toString(): String {
        return "DailyPlanSegment(id='$id', sequenceNumber=$sequenceNumber)"
    }
}
