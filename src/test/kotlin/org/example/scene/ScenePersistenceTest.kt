package org.example.scene

import com.fasterxml.jackson.databind.ObjectMapper
import jakarta.persistence.EntityManager
import org.example.common.contract.ApiContractException
import org.example.config.JacksonConfig
import org.example.route.model.*
import org.example.route.repository.*
import org.example.trip.personal.model.*
import org.example.trip.personal.repository.*
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest
import org.springframework.context.annotation.Import
import org.springframework.transaction.PlatformTransactionManager
import org.springframework.transaction.annotation.Propagation
import org.springframework.transaction.annotation.Transactional
import org.springframework.transaction.support.TransactionTemplate
import java.time.Instant
import java.time.LocalDate
import java.util.concurrent.CountDownLatch
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit

@DataJpaTest(properties = ["spring.flyway.enabled=false", "spring.jpa.hibernate.ddl-auto=create-drop", "spring.jpa.properties.hibernate.dialect=org.hibernate.dialect.H2Dialect", "spring.datasource.url=jdbc:h2:mem:scene-test;MODE=MySQL;DB_CLOSE_DELAY=-1", "spring.datasource.driver-class-name=org.h2.Driver", "spring.datasource.username=sa", "spring.datasource.password=", "app.scenes.public-base-url=http://localhost:8080/walkbg", "app.scenes.management-token=test-scene-only"])
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@Import(SceneApplicationService::class, SceneDomainService::class, SceneConfiguration::class, JacksonConfig::class)
class ScenePersistenceTest {
    @Autowired private lateinit var service: SceneApplicationService
    @Autowired private lateinit var versions: RouteVersionRepository
    @Autowired private lateinit var current: RouteCurrentPublicVersionRepository
    @Autowired private lateinit var collection: PublicRouteCollectionRepository
    @Autowired private lateinit var media: SceneMediaRepository
    @Autowired private lateinit var routeSets: RouteSceneSetRepository
    @Autowired private lateinit var tripSets: TripSceneSetRepository
    @Autowired private lateinit var trips: PersonalTripRepository
    @Autowired private lateinit var owners: PersonalTripOwnershipRepository
    @Autowired private lateinit var adopted: TripFrozenRouteVersionRepository
    @Autowired private lateinit var days: PersonalTripDayRepository
    @Autowired private lateinit var mapper: ObjectMapper
    @Autowired private lateinit var em: EntityManager
    @Autowired private lateinit var manager: PlatformTransactionManager
    private val dayDate = LocalDate.of(2026, 10, 1)

    @BeforeEach fun seed() {
        // NOT_SUPPORTED race test performs only its own committed seed.
        if (!org.springframework.transaction.support.TransactionSynchronizationManager.isActualTransactionActive()) return
        versions.saveAndFlush(routeVersion())
        current.saveAndFlush(RouteCurrentPublicVersion("route-1", "version-1"))
        collection.saveAndFlush(PublicRouteCollectionEntry("route-1", 1, 1))
        trips.saveAndFlush(PersonalTripRecord("trip-1", "保留的计划", Instant.parse("2026-09-01T00:00:00Z"), departureCity = "成都", startDate = dayDate, endDate = dayDate.plusDays(1), totalDayCount = 2, hikingDayCount = 2, revision = "trip-r1", frozenRouteBasisJson = "{\"immutable\":true}"))
        owners.saveAndFlush(PersonalTripOwnership("trip-1", "owner-1"))
        adopted.saveAndFlush(TripFrozenRouteVersion("trip-1", "version-1"))
        listOf("day-1", "day-2").forEachIndexed { index, id -> days.saveAndFlush(PersonalTripDayRecord(id, "trip-1", index + 1, dayDate.plusDays(index.toLong()), "山谷", index + 1, content(index))) }
    }

    @Test fun `media is content addressed immutable and reloads original persisted bytes`() {
        val png = sceneImageBytes()
        val one = service.upload(png, "image/png")
        val two = service.upload(png, "image/png")
        assertEquals(one, two)
        assertEquals(1, media.count())
        em.flush(); em.clear()
        assertArrayEquals(png, service.readMedia(one.mediaId).bytes)
        assertEquals("image/png", service.readMedia(one.mediaId).contentType)
        assertThrows(ApiContractException::class.java) { service.readMedia("a".repeat(64)) }.also { assertEquals("scene_media_not_found", it.code) }
    }
    @Test fun `route replacement persists normalized day order and leaves published facts untouched`() {
        // Compare persisted snapshots: database timestamp precision may differ from the seed Instant.
        em.flush(); em.clear()
        val initialVersion = versions.findById("version-1").orElseThrow()
        val artwork = service.upload(sceneImageBytes(), "image/png")
        assertEquals(0, service.managedRoute("route-1", "version-1").revision)
        val value = service.replaceRoute("route-1", "version-1", ReplaceRouteScenes(0, SceneBindings(artwork.mediaId, listOf(SceneDayBinding("ref-2", artwork.mediaId), SceneDayBinding("ref-1", artwork.mediaId)))))
        assertEquals(1, value.revision)
        assertEquals(listOf("ref-1", "ref-2"), value.projection.days.map { it.referenceDayId })
        em.flush(); em.clear()
        assertEquals(value.projection, service.publicRoute("route-1", "version-1"))
        assertEquals(initialVersion, versions.findById("version-1").orElseThrow())
    }
    @Test fun `CAS rejects stale updates and invalid partial changes never persist`() {
        val art = service.upload(sceneImageBytes(), "image/png")
        service.replaceRoute("route-1", "version-1", ReplaceRouteScenes(0, SceneBindings(art.mediaId)))
        assertEquals("scene_revision_conflict", assertThrows(ApiContractException::class.java) { service.replaceRoute("route-1", "version-1", ReplaceRouteScenes(0, SceneBindings())) }.code)
        assertEquals("scene_media_not_found", assertThrows(ApiContractException::class.java) { service.replaceRoute("route-1", "version-1", ReplaceRouteScenes(1, SceneBindings(art.mediaId, listOf(SceneDayBinding("ref-1", "a".repeat(64)))))) }.code)
        assertEquals(1, service.managedRoute("route-1", "version-1").revision)
        assertEquals(art.mediaId, service.publicRoute("route-1", "version-1").overview?.mediaId)
    }
    @Test fun `route identity reference membership and public visibility are enforced`() {
        val art = service.upload(sceneImageBytes(), "image/png")
        assertEquals("route_version_not_found", assertThrows(ApiContractException::class.java) { service.replaceRoute("other-route", "version-1", ReplaceRouteScenes(0, SceneBindings())) }.code)
        assertEquals("scene_reference_day_invalid", assertThrows(ApiContractException::class.java) { service.replaceRoute("route-1", "version-1", ReplaceRouteScenes(0, SceneBindings(days = listOf(SceneDayBinding("foreign", art.mediaId))))) }.code)
        assertEquals("invalid_request", assertThrows(ApiContractException::class.java) { service.replaceRoute("route-1", "version-1", ReplaceRouteScenes(0, SceneBindings(days = listOf(SceneDayBinding("ref-1", art.mediaId), SceneDayBinding("ref-1", art.mediaId))))) }.code)
        assertEquals("route_version_conflict", assertThrows(ApiContractException::class.java) { service.publicRoute("route-1", "old-version") }.code)
        collection.deleteById("route-1"); collection.flush()
        assertEquals("route_not_found", assertThrows(ApiContractException::class.java) { service.publicRoute("route-1", "version-1") }.code)
    }
    @Test fun `personal auto projection matches complete paths and hides ownership`() {
        val art = service.upload(sceneImageBytes(), "image/png")
        service.replaceRoute("route-1", "version-1", ReplaceRouteScenes(0, SceneBindings(art.mediaId, listOf(SceneDayBinding("ref-1", art.mediaId), SceneDayBinding("ref-2", art.mediaId)))))
        val value = service.personalTrip("owner-1", "trip-1")
        assertEquals(listOf("day-1", "day-2"), value.days.map { it.tripDayId })
        assertNotNull(value.overview)
        assertEquals("trip_not_found", assertThrows(ApiContractException::class.java) { service.personalTrip("outsider", "trip-1") }.code)
        assertEquals("trip_not_found", assertThrows(ApiContractException::class.java) { service.personalTrip("owner-1", "missing") }.code)
        assertTrue(service.managedTrip("trip-1").projection.days.isEmpty(), "management must not expose auto fallback as a manual set")
    }
    @Test fun `manual trip binds context orders days and never changes frozen trip content`() {
        val initialTrip = trips.findById("trip-1").orElseThrow().copy()
        val initialDays = days.findByTripIdOrderByDayNumberAsc("trip-1")
        val art = service.upload(sceneImageBytes(), "image/png")
        val result = service.replaceTrip("trip-1", ReplaceTripScenes(0, "trip-r1", "version-1", SceneBindings(art.mediaId, listOf(SceneDayBinding("day-2", art.mediaId), SceneDayBinding("day-1", art.mediaId)))))
        assertEquals(listOf("day-1", "day-2"), result.projection.days.map { it.tripDayId })
        em.flush(); em.clear()
        assertEquals(result.projection, service.personalTrip("owner-1", "trip-1"))
        assertEquals(initialTrip, trips.findById("trip-1").orElseThrow())
        assertEquals(initialDays, days.findByTripIdOrderByDayNumberAsc("trip-1"))
        assertEquals("trip_scene_context_conflict", assertThrows(ApiContractException::class.java) { service.replaceTrip("trip-1", ReplaceTripScenes(1, "old", "version-1", SceneBindings())) }.code)
        assertEquals("scene_trip_day_invalid", assertThrows(ApiContractException::class.java) { service.replaceTrip("trip-1", ReplaceTripScenes(1, "trip-r1", "version-1", SceneBindings(days = listOf(SceneDayBinding("not-in-trip", art.mediaId))))) }.code)
    }
    @Test fun `manual empty set overrides auto and revision change invalidates the entire manual set`() {
        val art = service.upload(sceneImageBytes(), "image/png")
        service.replaceRoute("route-1", "version-1", ReplaceRouteScenes(0, SceneBindings(art.mediaId, listOf(SceneDayBinding("ref-1", art.mediaId)))))
        val auto = service.personalTrip("owner-1", "trip-1")
        service.replaceTrip("trip-1", ReplaceTripScenes(0, "trip-r1", "version-1", SceneBindings()))
        val cleared = service.personalTrip("owner-1", "trip-1")
        assertNull(cleared.overview)
        assertTrue(cleared.days.isEmpty())
        assertNotEquals(auto.sceneRevision, cleared.sceneRevision)
        trips.findById("trip-1").orElseThrow().revision = "trip-r2"
        em.flush()
        val restored = service.personalTrip("owner-1", "trip-1")
        assertNotNull(restored.overview)
        assertEquals("trip-r2", restored.tripRevision)
        assertNotEquals(cleared.sceneRevision, restored.sceneRevision)
        assertEquals(1, service.managedTrip("trip-1").revision)
        assertNull(service.managedTrip("trip-1").projection.overview)
    }
    @Test fun `personal artwork uses adopted historical version rather than latest public version`() {
        val art = service.upload(sceneImageBytes(), "image/png")
        service.replaceRoute("route-1", "version-1", ReplaceRouteScenes(0, SceneBindings(art.mediaId)))
        versions.saveAndFlush(routeVersion().copy(id = "version-2"))
        current.saveAndFlush(RouteCurrentPublicVersion("route-1", "version-2"))
        assertNotNull(service.personalTrip("owner-1", "trip-1").overview)
        assertNull(service.publicRoute("route-1", "version-2").overview)
    }
    @Test
    @Transactional(propagation = Propagation.NOT_SUPPORTED)
    fun `concurrent first replacements allow exactly one revision zero writer`() {
        val tx = TransactionTemplate(manager)
        tx.executeWithoutResult { versions.saveAndFlush(routeVersion().copy(id = "race-version", routeId = "race-route")) }
        val pool = Executors.newFixedThreadPool(2)
        val gate = CountDownLatch(1)
        try {
            val results = (1..2).map { pool.submit<String> {
                gate.await()
                try { service.replaceRoute("race-route", "race-version", ReplaceRouteScenes(0, SceneBindings())); "ok" }
                catch (ex: ApiContractException) { ex.code }
            } }
            gate.countDown()
            assertEquals(listOf("ok", "scene_revision_conflict"), results.map { it.get(20, TimeUnit.SECONDS) }.sorted())
            assertEquals(1, service.managedRoute("race-route", "race-version").revision)
        } finally {
            pool.shutdownNow()
            tx.executeWithoutResult { routeSets.deleteById("race-version"); routeSets.flush(); versions.deleteById("race-version") }
        }
    }
    private fun routeVersion() = RouteVersion("version-1", "route-1", createdAt = Instant.parse("2026-09-29T20:00:00.077745211Z"), routeType = "multi_day", mainTrackAvailability = "valid", mainTrackReferenceSystem = "WGS84", mainTrackJson = "[[30.0,100.0],[30.1,100.0],[30.2,100.0]]", referenceDaysJson = """[{"identity":"ref-1","dayNumber":1,"mainTrackRange":{"startPathPosition":{"precedingPositionIndex":0,"progressToNextPosition":0},"endPathPosition":{"precedingPositionIndex":1,"progressToNextPosition":0}}},{"identity":"ref-2","dayNumber":2,"mainTrackRange":{"startPathPosition":{"precedingPositionIndex":1,"progressToNextPosition":0},"endPathPosition":{"precedingPositionIndex":2}}}]""")
    private fun content(index: Int) = mapper.writeValueAsString(mapOf("actions" to listOf(mapOf("actionType" to "hike", "sequence" to 1, "routeSectionSnapshot" to mapOf("path" to (index..index + 1).map { mapOf("latitude" to 30.0 + it * 0.1, "longitude" to 100.0, "referenceSystem" to "WGS84") } )))))
}
