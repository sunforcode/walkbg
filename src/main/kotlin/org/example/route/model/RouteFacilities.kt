package org.example.route.model

import jakarta.persistence.*

/**
 * 路线设施信息
 */
@Entity
@Table(name = "route_facilities")
data class RouteFacilities(
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    val id: Long = 0,

    @Column(columnDefinition = "TEXT")
    val water: String? = null,

    @Column(columnDefinition = "TEXT")
    val food: String? = null,

    @Column(columnDefinition = "TEXT")
    val accommodation: String? = null,

    @Column(columnDefinition = "TEXT")
    val toilets: String? = null,

    @Column(columnDefinition = "TEXT")
    val signalCoverage: String? = null,

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "route_id")
    var route: Route? = null
)
