package org.example.scene

import com.fasterxml.jackson.databind.ObjectMapper
import jakarta.persistence.EntityManager
import jakarta.persistence.LockModeType
import org.example.common.contract.ApiContractException
import org.example.route.model.RouteVersion
import org.example.route.repository.RouteCurrentPublicVersionRepository
import org.example.route.repository.RouteVersionRepository
import org.example.route.repository.PublicRouteCollectionRepository
import org.springframework.http.HttpStatus
import org.example.trip.personal.model.PersonalTripRecord
import org.example.trip.personal.repository.PersonalTripDayRepository
import org.example.trip.personal.repository.PersonalTripOwnershipRepository
import org.example.trip.personal.repository.PersonalTripRepository
import org.example.trip.personal.repository.TripFrozenRouteVersionRepository
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import org.springframework.transaction.annotation.Isolation

@Service
class SceneApplicationService(
    private val media: SceneMediaRepository,
    private val routeSets: RouteSceneSetRepository,
    private val tripSets: TripSceneSetRepository,
    private val versions: RouteVersionRepository,
    private val currentVersions: RouteCurrentPublicVersionRepository,
    private val publicCollection: PublicRouteCollectionRepository,
    private val trips: PersonalTripRepository,
    private val owners: PersonalTripOwnershipRepository,
    private val adoptedVersions: TripFrozenRouteVersionRepository,
    private val days: PersonalTripDayRepository,
    private val entityManager: EntityManager,
    private val domain: SceneDomainService,
    private val configuration: SceneConfiguration,
    private val mapper: ObjectMapper
) {
    @Transactional
    fun upload(bytes: ByteArray, contentType: String): SceneImage {
        val inspected = domain.inspectMedia(bytes, contentType)
        val url = configuration.imageUrl(inspected.mediaId) // Reject absent public origin before persisting.
        media.insertImmutable(inspected.mediaId, inspected.contentType, inspected.width, inspected.height, inspected.bytes)
        return SceneImage(inspected.mediaId, url, inspected.width, inspected.height)
    }

    @Transactional(readOnly = true, isolation = Isolation.REPEATABLE_READ)
    fun readMedia(id: String): SceneMedia {
        domain.requireMediaId(id)
        return media.findById(id).orElseThrow { notFound("scene_media_not_found", "场景图片不存在") }
    }

    @Transactional(readOnly = true, isolation = Isolation.REPEATABLE_READ)
    fun publicRoute(routeId: String, versionId: String): RouteSceneProjection {
        if (!publicCollection.existsById(routeId)) throw notFound("route_not_found")
        val current = currentVersions.findById(routeId).orElseThrow { notFound("route_not_found") }
        if (current.routeVersionId != versionId) throw ApiContractException.conflict("route_version_conflict", "路线公开版本已变化")
        requireVersion(routeId, versionId)
        return routeProjection(routeId, versionId, routeSets.findById(versionId).orElse(null))
    }

    @Transactional(readOnly = true, isolation = Isolation.REPEATABLE_READ)
    fun managedRoute(routeId: String, versionId: String): ManagedSceneProjection<RouteSceneProjection> {
        requireVersion(routeId, versionId)
        val set = routeSets.findById(versionId).orElse(null)
        return ManagedSceneProjection(set?.revision ?: 0, routeProjection(routeId, versionId, set))
    }

    @Transactional
    fun replaceRoute(routeId: String, versionId: String, command: ReplaceRouteScenes): ManagedSceneProjection<RouteSceneProjection> {
        // Lock an existing parent even on first creation, so two revision-0 writes cannot both win.
        val version = entityManager.find(RouteVersion::class.java, versionId, LockModeType.PESSIMISTIC_WRITE)
            ?.takeIf { it.routeId == routeId } ?: throw notFound("route_version_not_found")
        val existing = routeSets.findById(versionId).orElse(null)
        checkRevision(command.expectedRevision, existing?.revision ?: 0)
        domain.validateBindings(command.bindings, domain.referenceDays(version).map { it.path("identity").asText("") }, "scene_reference_day_invalid")
        validateMedia(command.bindings)
        val next = RouteSceneSet(versionId, routeId, command.expectedRevision + 1, mapper.writeValueAsString(command.bindings))
        routeSets.saveAndFlush(next)
        return ManagedSceneProjection(next.revision, routeProjection(routeId, versionId, next))
    }

    @Transactional(readOnly = true, isolation = Isolation.REPEATABLE_READ)
    fun personalTrip(accountId: String, tripId: String): TripSceneProjection {
        if (owners.findByTripIdAndAccountId(tripId, accountId) == null) throw notFound("trip_not_found")
        val trip = requireTrip(tripId)
        val versionId = adoptedVersion(tripId)
        val manual = tripSets.findById(tripId).orElse(null)
        if (manual != null && validManual(manual, trip, versionId)) return manualProjection(trip, versionId, manual)
        val routeSet = routeSets.findById(versionId).orElse(null)
        val revision = "route:$versionId:${routeSet?.revision ?: 0}:binding:${manual?.revision ?: 0}:trip:${trip.revision}"
        if (routeSet == null) return TripSceneProjection(trip.id, trip.revision, versionId, revision)
        val version = versions.findById(versionId).orElseThrow { notFound("route_version_not_found") }
        val bindings = readBindings(routeSet.contentJson)
        val matches = domain.matchDays(version, days.findByTripIdOrderByDayNumberAsc(tripId))
        val byReference = bindings.days.associateBy { it.dayId }
        return TripSceneProjection(trip.id, trip.revision, versionId, revision,
            bindings.overviewMediaId?.let(::image), matches.mapNotNull { (dayId, refId) -> byReference[refId]?.let { TripDayScene(dayId, image(it.mediaId)) } })
    }

    @Transactional(readOnly = true, isolation = Isolation.REPEATABLE_READ)
    fun managedTrip(tripId: String): ManagedSceneProjection<TripSceneProjection> {
        val trip = requireTrip(tripId)
        val versionId = adoptedVersion(tripId)
        val manual = tripSets.findById(tripId).orElse(null)
        return ManagedSceneProjection(manual?.revision ?: 0, manualProjection(trip, versionId, manual))
    }

    @Transactional
    fun replaceTrip(tripId: String, command: ReplaceTripScenes): ManagedSceneProjection<TripSceneProjection> {
        val trip = trips.findByIdForUpdate(tripId) ?: throw notFound("trip_not_found")
        val versionId = adoptedVersion(tripId)
        if (command.tripRevision != trip.revision || command.routeVersionId != versionId) {
            throw ApiContractException.conflict("trip_scene_context_conflict", "行程内容或采用版本已变化")
        }
        val existing = tripSets.findById(tripId).orElse(null)
        checkRevision(command.expectedRevision, existing?.revision ?: 0)
        domain.validateBindings(command.bindings, days.findByTripIdOrderByDayNumberAsc(tripId).map { it.id }, "scene_trip_day_invalid")
        validateMedia(command.bindings)
        val next = TripSceneSet(tripId, versionId, trip.revision, command.expectedRevision + 1, mapper.writeValueAsString(command.bindings))
        tripSets.saveAndFlush(next)
        return ManagedSceneProjection(next.revision, manualProjection(trip, versionId, next))
    }

    private fun requireVersion(routeId: String, versionId: String) = versions.findById(versionId).orElse(null)
        ?.takeIf { it.routeId == routeId } ?: throw notFound("route_version_not_found")
    private fun requireTrip(tripId: String) = trips.findById(tripId).orElseThrow { notFound("trip_not_found") }
    private fun adoptedVersion(tripId: String) = adoptedVersions.findById(tripId).orElseThrow { notFound("trip_not_found") }.routeVersionId
    private fun checkRevision(expected: Long, actual: Long) {
        if (expected < 0 || expected == Long.MAX_VALUE) throw ApiContractException.invalidRequest("expectedRevision 无效")
        if (expected != actual) throw ApiContractException.conflict("scene_revision_conflict", "场景已更新，请重新读取后重试")
    }
    private fun validateMedia(bindings: SceneBindings) {
        (bindings.days.map { it.mediaId } + listOfNotNull(bindings.overviewMediaId)).distinct().forEach(::image)
    }
    private fun image(id: String): SceneImage {
        val metadata = media.metadata(id) ?: throw notFound("scene_media_not_found", "场景图片不存在")
        return SceneImage(id, configuration.imageUrl(id), metadata.width, metadata.height)
    }
    private fun notFound(code: String, message: String = "资源不存在") = ApiContractException(HttpStatus.NOT_FOUND, code, message)
    private fun readBindings(json: String) = mapper.readValue(json, SceneBindings::class.java)
    private fun routeProjection(routeId: String, versionId: String, set: RouteSceneSet?): RouteSceneProjection {
        val bindings = set?.let { readBindings(it.contentJson) } ?: SceneBindings()
        return RouteSceneProjection(routeId, versionId, (set?.revision ?: 0).toString(), bindings.overviewMediaId?.let(::image), orderedRouteBindings(versionId, bindings).map { RouteDayScene(it.dayId, image(it.mediaId)) })
    }
    private fun orderedRouteBindings(versionId: String, bindings: SceneBindings): List<SceneDayBinding> {
        val version = versions.findById(versionId).orElseThrow { notFound("route_version_not_found") }
        val byId = bindings.days.associateBy { it.dayId }
        return domain.referenceDays(version).sortedBy { it.path("dayNumber").asInt() }.mapNotNull { byId[it.path("identity").asText()] }
    }
    private fun orderedTripBindings(tripId: String, bindings: SceneBindings): List<SceneDayBinding> {
        val byId = bindings.days.associateBy { it.dayId }
        return days.findByTripIdOrderByDayNumberAsc(tripId).mapNotNull { byId[it.id] }
    }
    private fun validManual(set: TripSceneSet, trip: PersonalTripRecord, versionId: String) = set.tripRevision == trip.revision && set.routeVersionId == versionId
    private fun manualProjection(trip: PersonalTripRecord, versionId: String, set: TripSceneSet?): TripSceneProjection {
        val bindings = set?.takeIf { validManual(it, trip, versionId) }?.let { readBindings(it.contentJson) } ?: SceneBindings()
        return TripSceneProjection(trip.id, trip.revision, versionId, "trip:${set?.revision ?: 0}:${trip.revision}:$versionId",
            bindings.overviewMediaId?.let(::image), orderedTripBindings(trip.id, bindings).map { TripDayScene(it.dayId, image(it.mediaId)) })
    }
}
