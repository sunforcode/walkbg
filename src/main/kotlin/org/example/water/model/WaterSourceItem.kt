package org.example.water.model

import jakarta.persistence.*

/**
 * 水源表
 */
@Entity
@Table(
    name = "water_source_items",
    indexes = [
        Index(name = "idx_water_source_items_water_day_id", columnList = "water_day_id"),
        Index(name = "idx_water_source_items_type", columnList = "type")
    ]
)
data class WaterSourceItem(
    @Id
    @Column(length = 64)
    val id: String,

    @Column(name = "water_day_id", length = 64, nullable = false)
    val waterDayId: String,

    @Column(name = "source_name", length = 100, nullable = false)
    var sourceName: String,

    @Column
    var type: Int? = null, // 0: 天然水源, 1: 人工水源, 2: 携带水源

    @Column(name = "estimated_volume")
    var estimatedVolume: Int? = null, // 预计补水量(ml)

    @Column(columnDefinition = "TEXT")
    var notes: String? = null,

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "water_day_id", insertable = false, updatable = false)
    var waterDay: WaterDay? = null
) {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as WaterSourceItem

        return id == other.id
    }

    override fun hashCode(): Int {
        return id.hashCode()
    }

    override fun toString(): String {
        return "WaterSourceItem(id='$id', sourceName='$sourceName')"
    }
}