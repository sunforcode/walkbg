package org.example.route.model

import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.Id
import jakarta.persistence.IdClass
import jakarta.persistence.Table
import java.io.Serializable

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

@Entity
@Table(name = "route_publication_requests")
@IdClass(RoutePublicationRequestKey::class)
data class RoutePublicationRequestReceipt(
    @Id
    @Column(name = "route_id", length = 64)
    val routeId: String,

    @Id
    @Column(name = "publication_id", length = 64)
    val publicationId: String,

    @Column(name = "route_version_id", nullable = false, length = 64)
    val routeVersionId: String,

    @Column(name = "public_route_type", nullable = false, length = 32)
    val publicRouteType: String
)

data class RoutePublicationRequestKey(
    val routeId: String = "",
    val publicationId: String = ""
) : Serializable

@Entity
@Table(name = "route_publication_configuration")
data class RoutePublicationConfiguration(
    @Id
    val id: Int = 1,

    @Column(name = "last_all_route_order", nullable = false)
    var lastAllRouteOrder: Int = 0
)
