package org.example.water.model

import jakarta.persistence.*

/**
 * 用水计划标签实体
 */
@Entity
@Table(name = "water_plan_tags")
data class WaterPlanTag(
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    val id: Long = 0,
    
    @Column(nullable = false, length = 50)
    val tag: String,
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "water_plan_id", nullable = false)
    var waterPlan: WaterPlan? = null
)