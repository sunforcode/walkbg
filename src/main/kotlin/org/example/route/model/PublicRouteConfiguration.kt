package org.example.route.model

import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.Id
import jakarta.persistence.Table

@Entity
@Table(name = "route_current_public_versions")
data class RouteCurrentPublicVersion(
    @Id
    @Column(name = "route_id", length = 64)
    val routeId: String,

    @Column(name = "route_version_id", nullable = false, unique = true, length = 64)
    val routeVersionId: String
)

@Entity
@Table(name = "public_route_collection")
data class PublicRouteCollectionEntry(
    @Id
    @Column(name = "route_id", length = 64)
    val routeId: String,

    @Column(name = "all_route_order", nullable = false, unique = true)
    val allRouteOrder: Int,

    @Column(name = "featured_order", unique = true)
    val featuredOrder: Int? = null
)
