package org.example.route.model

import com.fasterxml.jackson.annotation.JsonProperty
import jakarta.persistence.*
import java.time.Instant

/**
 * 每日计划实体
 */
@Entity
@Table(name = "daily_plans")
data class DailyPlan(
    @Id
    @Column(length = 64)
    val id: String,

    @Column(name = "day_number", nullable = false)
    val dayNumber: Int,

    @Column(nullable = false)
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

    val accommodation: String? = null,

    @Column(columnDefinition = "TEXT")
    val notes: String? = null,

    @Column(name = "created_at", nullable = false, updatable = false)
    val createdAt: Instant = Instant.now(),

    @Column(name = "updated_at", nullable = false)
    var updatedAt: Instant = Instant.now(),

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "route_id")
    var route: Route? = null,
    
    @OneToMany(mappedBy = "dailyPlan", cascade = [CascadeType.ALL], orphanRemoval = true)
    val segments: MutableList<DailyPlanSegment> = mutableListOf()
) {
    /**
     * 添加路段到每日计划
     */
    fun addSegment(segment: Segment, sequenceNumber: Int = segments.size + 1) {
        segments.add(
            DailyPlanSegment(
                id = java.util.UUID.randomUUID().toString(),
                sequenceNumber = sequenceNumber,
                dailyPlan = this,
                segment = segment
            )
        )
    }
    
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
        return "DailyPlan(id='$id', dayNumber=$dayNumber, title='$title')"
    }
}

/**
 * 每日计划-路段关联表
 */
@Entity
@Table(name = "daily_plan_segments")
data class DailyPlanSegment(
    @Id
    @Column(length = 64)
    val id: String,
    
    @Column(nullable = false)
    val sequenceNumber: Int,
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "daily_plan_id")
    var dailyPlan: DailyPlan? = null,
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "segment_id")
    var segment: Segment? = null
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
