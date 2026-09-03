package org.example.route.repository

import org.example.route.model.LogicalEquipmentSuggestionIdentity
import org.example.route.model.LogicalEquipmentSuggestionIdentityKey
import org.example.route.model.PublicRouteCollectionEntry
import org.example.route.model.RouteCurrentPublicVersion
import org.example.route.model.RouteVersion
import org.example.route.model.RouteVersionEquipmentSuggestion
import org.example.route.model.RouteVersionImage
import org.example.route.model.RouteVersionPoint
import org.example.route.model.RouteVersionPublicationOrder
import org.example.route.model.RouteVersionPublicationOrderKey
import org.example.route.model.RouteVersionSegment
import jakarta.persistence.LockModeType
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Lock
import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.query.Param
import org.springframework.stereotype.Repository

@Repository
interface RouteVersionRepository : JpaRepository<RouteVersion, String>

@Repository
interface RouteCurrentPublicVersionRepository : JpaRepository<RouteCurrentPublicVersion, String> {
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select current from RouteCurrentPublicVersion current where current.routeId = :routeId")
    fun findByRouteIdForUpdate(@Param("routeId") routeId: String): RouteCurrentPublicVersion?
}

@Repository
interface PublicRouteCollectionRepository : JpaRepository<PublicRouteCollectionEntry, String> {
    fun findAllByOrderByAllRouteOrderAsc(): List<PublicRouteCollectionEntry>
    fun findAllByFeaturedOrderIsNotNullOrderByFeaturedOrderAsc(): List<PublicRouteCollectionEntry>
}

@Repository
interface RouteVersionImageRepository : JpaRepository<RouteVersionImage, String> {
    fun findByRouteVersionIdOrderByDisplayOrderAsc(routeVersionId: String): List<RouteVersionImage>
}

@Repository
interface RouteVersionSegmentRepository : JpaRepository<RouteVersionSegment, String> {
    fun findByRouteVersionIdOrderBySegmentOrderAsc(routeVersionId: String): List<RouteVersionSegment>
}

@Repository
interface RouteVersionPointRepository : JpaRepository<RouteVersionPoint, String> {
    fun findByRouteVersionIdOrderByDisplayOrderAsc(routeVersionId: String): List<RouteVersionPoint>
}

@Repository
interface RouteVersionPublicationOrderRepository : JpaRepository<RouteVersionPublicationOrder, RouteVersionPublicationOrderKey> {
    fun findByRouteVersionId(routeVersionId: String): RouteVersionPublicationOrder?
    fun findByRouteIdOrderByPublishedSequenceAsc(routeId: String): List<RouteVersionPublicationOrder>
}

@Repository
interface LogicalEquipmentSuggestionIdentityRepository :
    JpaRepository<LogicalEquipmentSuggestionIdentity, LogicalEquipmentSuggestionIdentityKey>

@Repository
interface RouteVersionEquipmentSuggestionRepository : JpaRepository<RouteVersionEquipmentSuggestion, String> {
    fun findByRouteVersionIdOrderByDisplayOrderAsc(routeVersionId: String): List<RouteVersionEquipmentSuggestion>
}
