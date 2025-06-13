package org.example.water.model


import jakarta.persistence.*
import org.example.water.model.WaterSource

/**
 * 每日用水计划水源关联表
 */
@Entity
@Table(name = "day_water_plan_sources")
data class DayWaterPlanSource(
    @Id
    @Column(length = 64)
    val id: String,
    
    @Column(name = "water_day_id", length = 64, nullable = false)
    val waterDayId: String,

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "water_day_id", insertable = false, updatable = false)
    var waterDay: WaterDay? = null,
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "water_source_id")
    var waterSource: WaterSource? = null
)