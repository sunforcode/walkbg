package org.example.water.model

import jakarta.persistence.*
import org.example.water.model.WaterPlan

/**
 * 每日用水表
 */
@Entity
@Table(
    name = "water_days",
    indexes = [
        Index(name = "idx_water_days_water_plan_id", columnList = "water_plan_id"),
        Index(name = "idx_water_days_day_number", columnList = "day_number")
    ]
)
data class WaterDay(
    @Id
    @Column(length = 64)
    val id: String,

    @Column(name = "water_plan_id", length = 64, nullable = false)
    val waterPlanId: String,

    @Column(name = "day_number", nullable = false)
    val dayNumber: Int,

    @Column(columnDefinition = "TEXT")
    var notes: String? = null,

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "water_plan_id", insertable = false, updatable = false)
    var waterPlan: WaterPlan? = null,

    @OneToMany(mappedBy = "waterDay", cascade = [CascadeType.ALL], orphanRemoval = true, fetch = FetchType.LAZY)
    val waterSources: MutableList<WaterSourceItem> = mutableListOf()
) {
    fun addWaterSource(waterSource: WaterSourceItem) {
        waterSources.add(waterSource)
        waterSource.waterDay = this
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as WaterDay

        return id == other.id
    }

    override fun hashCode(): Int {
        return id.hashCode()
    }

    override fun toString(): String {
        return "WaterDay(id='$id', dayNumber=$dayNumber)"
    }
}