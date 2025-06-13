package org.example.route.model

import jakarta.persistence.*

/**
 * 路线评分实体
 */
@Entity
@Table(name = "route_ratings")
data class RouteRating(
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    val id: Long = 0,
    
    val overall: Double? = null,
    val scenery: Double? = null,
    val difficulty: Double? = null,
    val experience: Double? = null,
    val facilities: Double? = null,
    val ratingCount: Int = 0,
    
    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "route_id")
    var route: Route? = null
)