package org.example.water.model


import jakarta.persistence.*

/**
 * 用水计划标签实体（单向关联）
 */
@Entity
@Table(
    name = "water_plan_tags",
    indexes = [
        Index(name = "idx_water_plan_tags_plan_id", columnList = "water_plan_id"),
        Index(name = "idx_water_plan_tags_tag", columnList = "tag")
    ]
)
data class WaterPlanTag(
    @Id
    @Column(length = 64)
    val id: String,

    @Column(name = "water_plan_id", length = 64, nullable = false)
    val waterPlanId: String,
    
    @Column(nullable = false, length = 50)
    val tag: String
) {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false
        other as WaterPlanTag
        return id == other.id
    }

    override fun hashCode(): Int {
        return id.hashCode()
    }

    override fun toString(): String {
        return "WaterPlanTag(id='$id', waterPlanId='$waterPlanId', tag='$tag')"
    }
}
