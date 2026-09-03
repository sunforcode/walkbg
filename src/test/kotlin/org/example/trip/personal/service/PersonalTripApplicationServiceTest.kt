package org.example.trip.personal.service

import com.fasterxml.jackson.databind.ObjectMapper
import org.example.common.contract.ApiContractException
import org.example.equipment.model.EquipmentListMember
import org.example.equipment.model.EquipmentListOwnership
import org.example.equipment.model.PersonalEquipmentOwnership
import org.example.equipment.model.PersonalEquipmentRecord
import org.example.equipment.model.UserEquipmentListRecord
import org.example.equipment.repository.EquipmentListMemberRepository
import org.example.equipment.repository.EquipmentListOwnershipRepository
import org.example.equipment.repository.PersonalEquipmentOwnershipRepository
import org.example.equipment.repository.PersonalEquipmentRecordRepository
import org.example.equipment.repository.UserEquipmentListRecordRepository
import org.example.equipment.service.PersonalEquipmentDomainService
import org.example.route.model.PublicRouteCollectionEntry
import org.example.route.model.RouteCurrentPublicVersion
import org.example.route.model.RouteVersion
import org.example.route.model.RouteVersionEquipmentSuggestion
import org.example.route.model.RouteVersionPoint
import org.example.route.model.RouteVersionPublicationOrder
import org.example.route.repository.PublicRouteCollectionRepository
import org.example.route.repository.RouteCurrentPublicVersionRepository
import org.example.route.repository.RouteVersionEquipmentSuggestionRepository
import org.example.route.repository.RouteVersionImageRepository
import org.example.route.repository.RouteVersionPointRepository
import org.example.route.repository.RouteVersionPublicationOrderRepository
import org.example.route.repository.RouteVersionRepository
import org.example.route.repository.RouteVersionSegmentRepository
import org.example.route.service.RouteVersionSummaryPlaceResolver
import org.example.trip.personal.dto.CancelTripCommand
import org.example.trip.personal.dto.GenerateTripCommand
import org.example.trip.personal.dto.GenerateTripResult
import org.example.trip.personal.dto.InformationConfidenceProjection
import org.example.trip.personal.dto.QualifiedValueProjection
import org.example.trip.personal.dto.TransportOptionProjection
import org.example.trip.personal.model.PersonalTripIdempotencyRecord
import org.example.trip.personal.model.PersonalTripRecord
import org.example.trip.personal.repository.PersonalTripDayRepository
import org.example.trip.personal.repository.PersonalTripEquipmentItemDerivationRepository
import org.example.trip.personal.repository.PersonalTripEquipmentItemRepository
import org.example.trip.personal.repository.PersonalTripEquipmentSnapshotRepository
import org.example.trip.personal.repository.PersonalTripEquipmentSuppressionRepository
import org.example.trip.personal.repository.PersonalTripIdempotencyRepository
import org.example.trip.personal.repository.PersonalTripOwnershipRepository
import org.example.trip.personal.repository.PersonalTripRepository
import org.example.trip.personal.repository.TripFrozenRouteVersionRepository
import org.example.trip.personal.repository.TripTransportSelectionRepository
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.mockito.kotlin.any
import org.mockito.kotlin.eq
import org.mockito.kotlin.mock
import org.mockito.kotlin.never
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever
import java.time.Clock
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.util.Optional

class PersonalTripApplicationServiceTest {
    private val tripRepository: PersonalTripRepository = mock()
    private val ownershipRepository: PersonalTripOwnershipRepository = mock()
    private val versionRelationRepository: TripFrozenRouteVersionRepository = mock()
    private val dayRepository: PersonalTripDayRepository = mock()
    private val snapshotRepository: PersonalTripEquipmentSnapshotRepository = mock()
    private val itemRepository: PersonalTripEquipmentItemRepository = mock()
    private val derivationRepository: PersonalTripEquipmentItemDerivationRepository = mock()
    private val suppressionRepository: PersonalTripEquipmentSuppressionRepository = mock()
    private val idempotencyRepository: PersonalTripIdempotencyRepository = mock()
    private val selectionRepository: TripTransportSelectionRepository = mock()
    private val collectionRepository: PublicRouteCollectionRepository = mock()
    private val currentVersionRepository: RouteCurrentPublicVersionRepository = mock()
    private val versionRepository: RouteVersionRepository = mock()
    private val publicationOrderRepository: RouteVersionPublicationOrderRepository = mock()
    private val imageRepository: RouteVersionImageRepository = mock()
    private val segmentRepository: RouteVersionSegmentRepository = mock()
    private val pointRepository: RouteVersionPointRepository = mock()
    private val suggestionRepository: RouteVersionEquipmentSuggestionRepository = mock()
    private val listRepository: UserEquipmentListRecordRepository = mock()
    private val listOwnershipRepository: EquipmentListOwnershipRepository = mock()
    private val memberRepository: EquipmentListMemberRepository = mock()
    private val equipmentRepository: PersonalEquipmentRecordRepository = mock()
    private val equipmentOwnershipRepository: PersonalEquipmentOwnershipRepository = mock()
    private val objectMapper = ObjectMapper().findAndRegisterModules()
    private val clock = Clock.fixed(Instant.parse("2026-09-01T04:00:00Z"), ZoneId.of("UTC"))

    @BeforeEach
    fun setUp() {
        whenever(collectionRepository.findById("route-1")).thenReturn(Optional.of(PublicRouteCollectionEntry("route-1", 1)))
        whenever(currentVersionRepository.findById("route-1")).thenReturn(Optional.of(RouteCurrentPublicVersion("route-1", "version-1")))
        whenever(versionRepository.findById("version-1")).thenReturn(Optional.of(routeVersion()))
        whenever(publicationOrderRepository.findByRouteVersionId("version-1"))
            .thenReturn(RouteVersionPublicationOrder("route-1", "version-1", 1))
        whenever(imageRepository.findByRouteVersionIdOrderByDisplayOrderAsc("version-1")).thenReturn(emptyList())
        whenever(segmentRepository.findByRouteVersionIdOrderBySegmentOrderAsc(any())).thenReturn(emptyList())
        whenever(pointRepository.findByRouteVersionIdOrderByDisplayOrderAsc("version-1")).thenReturn(emptyList())
        whenever(suggestionRepository.findByRouteVersionIdOrderByDisplayOrderAsc(any())).thenReturn(emptyList())
        whenever(derivationRepository.findByTripIdOrderByItemIdAsc(any())).thenReturn(emptyList())
        whenever(suppressionRepository.findByTripIdOrderByLogicalSuggestionIdAsc(any())).thenReturn(emptyList())
        whenever(listOwnershipRepository.findByAccountIdOrderByEquipmentListIdAsc(any())).thenReturn(emptyList())
        whenever(equipmentOwnershipRepository.findByAccountIdOrderByPersonalEquipmentIdAsc(any())).thenReturn(emptyList())
        whenever(idempotencyRepository.findByAccountIdAndOperationAndIdempotencyKey(any(), any(), any())).thenReturn(null)
        whenever(
            idempotencyRepository.insertReservation(any(), any(), any(), any(), any(), any(), any())
        ).thenReturn(1)
        whenever(idempotencyRepository.completeReservation(any(), any(), any(), any(), any(), any())).thenReturn(1)
        whenever(selectionRepository.findById(any())).thenReturn(Optional.empty())
        whenever(tripRepository.save(any<PersonalTripRecord>())).thenAnswer { it.arguments[0] as PersonalTripRecord }
        whenever(tripRepository.findByIdForUpdate(any())).thenReturn(null)
        whenever(dayRepository.saveAll(any<List<org.example.trip.personal.model.PersonalTripDayRecord>>())).thenAnswer { it.arguments[0] }
        whenever(snapshotRepository.save(any<org.example.trip.personal.model.PersonalTripEquipmentSnapshotRecord>())).thenAnswer { it.arguments[0] }
        whenever(itemRepository.saveAll(any<List<org.example.trip.personal.model.PersonalTripEquipmentItemRecord>>())).thenAnswer { it.arguments[0] }
        whenever(ownershipRepository.save(any<org.example.trip.personal.model.PersonalTripOwnership>())).thenAnswer { it.arguments[0] }
        whenever(versionRelationRepository.save(any<org.example.trip.personal.model.TripFrozenRouteVersion>())).thenAnswer { it.arguments[0] }
        whenever(selectionRepository.save(any<org.example.trip.personal.model.TripTransportSelectionRecord>())).thenAnswer { it.arguments[0] }
    }

    @Test
    fun `direct planner creates one atomic complete trip and freezes selected equipment`() {
        whenever(listOwnershipRepository.findByEquipmentListIdAndAccountId("list-1", "account-1"))
            .thenReturn(EquipmentListOwnership("list-1", "account-1"))
        whenever(listRepository.findById("list-1")).thenReturn(Optional.of(UserEquipmentListRecord("list-1", "周末")))
        whenever(memberRepository.findByEquipmentListIdOrderByPersonalEquipmentIdAsc("list-1"))
            .thenReturn(listOf(EquipmentListMember("list-1", "equipment-1", 2)))
        whenever(equipmentRepository.findById("equipment-1"))
            .thenReturn(Optional.of(PersonalEquipmentRecord("equipment-1", "登山杖", 2, 300)))
        whenever(equipmentOwnershipRepository.findByPersonalEquipmentIdAndAccountId("equipment-1", "account-1"))
            .thenReturn(PersonalEquipmentOwnership("equipment-1", "account-1", "登山杖"))
        whenever(equipmentOwnershipRepository.findByAccountIdOrderByPersonalEquipmentIdAsc("account-1"))
            .thenReturn(listOf(PersonalEquipmentOwnership("equipment-1", "account-1", "登山杖")))
        whenever(pointRepository.findByRouteVersionIdOrderByDisplayOrderAsc("version-1")).thenReturn(
            listOf(
                RouteVersionPoint(
                    id = "point-1",
                    routeVersionId = "version-1",
                    pointKind = "water_source",
                    displayOrder = 1,
                    name = "溪流取水点",
                    subCategory = "stream",
                    description = "需净化",
                    latitude = 30.05,
                    longitude = 101.05,
                    referenceSystem = "WGS84"
                )
            )
        )
        val service = service(DirectTripGenerationPlanner)

        val result = service.generate("account-1", "key-1", command(equipmentListId = "list-1"))

        assertTrue(result is GenerateTripResult.TripCreated)
        val detail = (result as GenerateTripResult.TripCreated).trip
        assertEquals("贡嘎环线 · 2026-09-02", detail.trip.name)
        assertEquals(1, detail.days.size)
        assertEquals("unavailable", detail.days.single().weather.condition.confidence?.status)
        assertEquals(1, detail.equipmentSnapshot.summary.itemCount)
        assertEquals(600, detail.equipmentSnapshot.summary.knownTotalWeight.grams)
        assertEquals("上海", detail.days.single().actions.first().origin?.value?.name)
        assertEquals("water_source", detail.days.single().points?.single()?.category)
        assertEquals("溪流取水点", detail.days.single().points?.single()?.name)
        verify(tripRepository).save(any())
        verify(ownershipRepository).save(any())
        verify(versionRelationRepository).save(any())
        verify(dayRepository).saveAll(any<List<org.example.trip.personal.model.PersonalTripDayRecord>>())
        verify(snapshotRepository).save(any())
        verify(itemRepository).saveAll(any<List<org.example.trip.personal.model.PersonalTripEquipmentItemRecord>>())
    }

    @Test
    fun `first generation keeps an explicit empty equipment snapshot when no suggestions or list are selected`() {
        val result = service(DirectTripGenerationPlanner).generate(
            "account-1",
            "empty-equipment-key",
            command()
        ) as GenerateTripResult.TripCreated

        assertEquals(0, result.trip.equipmentSnapshot.summary.itemCount)
        assertEquals(0, result.trip.equipmentSnapshot.summary.knownTotalWeight.grams)
        verify(itemRepository).saveAll(emptyList())
        verify(derivationRepository, never()).saveAll(any<List<org.example.trip.personal.model.PersonalTripEquipmentItemDerivedFromSuggestion>>())
    }

    @Test
    fun `first generation combines ordered adopted suggestions before selected list entries and saves derivations`() {
        whenever(suggestionRepository.findByRouteVersionIdOrderByDisplayOrderAsc("version-1")).thenReturn(
            listOf(
                RouteVersionEquipmentSuggestion(
                    id = "suggestion-2",
                    routeId = "route-1",
                    routeVersionId = "version-1",
                    logicalSuggestionId = "logical-2",
                    displayOrder = 2,
                    name = "雨衣",
                    normalizedName = "雨衣",
                    quantity = 1,
                    level = "recommended"
                ),
                RouteVersionEquipmentSuggestion(
                    id = "suggestion-1",
                    routeId = "route-1",
                    routeVersionId = "version-1",
                    logicalSuggestionId = "logical-1",
                    displayOrder = 1,
                    name = "帐篷",
                    normalizedName = "帐篷",
                    quantity = 1,
                    level = "required"
                )
            )
        )
        whenever(listOwnershipRepository.findByEquipmentListIdAndAccountId("list-1", "account-1"))
            .thenReturn(EquipmentListOwnership("list-1", "account-1"))
        whenever(listRepository.findById("list-1")).thenReturn(Optional.of(UserEquipmentListRecord("list-1", "周末")))
        whenever(memberRepository.findByEquipmentListIdOrderByPersonalEquipmentIdAsc("list-1"))
            .thenReturn(listOf(EquipmentListMember("list-1", "equipment-1", 2)))
        whenever(equipmentRepository.findById("equipment-1"))
            .thenReturn(Optional.of(PersonalEquipmentRecord("equipment-1", "水瓶", 2, 200)))
        whenever(equipmentOwnershipRepository.findByPersonalEquipmentIdAndAccountId("equipment-1", "account-1"))
            .thenReturn(PersonalEquipmentOwnership("equipment-1", "account-1", "水瓶"))
        whenever(equipmentOwnershipRepository.findByAccountIdOrderByPersonalEquipmentIdAsc("account-1"))
            .thenReturn(listOf(PersonalEquipmentOwnership("equipment-1", "account-1", "水瓶")))

        service(DirectTripGenerationPlanner).generate("account-1", "suggestions-key", command(equipmentListId = "list-1"))

        val savedItems = org.mockito.kotlin.argumentCaptor<List<org.example.trip.personal.model.PersonalTripEquipmentItemRecord>>()
        verify(itemRepository).saveAll(savedItems.capture())
        assertEquals(listOf("帐篷", "雨衣", "水瓶"), savedItems.firstValue.map { it.name })
        assertEquals(listOf("system_suggestion", "system_suggestion", "user_added"), savedItems.firstValue.map { it.source })
        assertEquals(listOf("unconfirmed_owned", "unconfirmed_owned", "owned"), savedItems.firstValue.map { it.ownershipStatus })
        val savedDerivations = org.mockito.kotlin.argumentCaptor<List<org.example.trip.personal.model.PersonalTripEquipmentItemDerivedFromSuggestion>>()
        verify(derivationRepository).saveAll(savedDerivations.capture())
        assertEquals(listOf("logical-1", "logical-2"), savedDerivations.firstValue.map { it.logicalSuggestionId })
        assertEquals(listOf("suggestion-1", "suggestion-2"), savedDerivations.firstValue.map { it.suggestionOccurrenceId })
    }

    @Test
    fun `first generation rejects normalized name collision between suggestion and selected list`() {
        whenever(suggestionRepository.findByRouteVersionIdOrderByDisplayOrderAsc("version-1")).thenReturn(
            listOf(
                RouteVersionEquipmentSuggestion(
                    id = "suggestion-1",
                    routeId = "route-1",
                    routeVersionId = "version-1",
                    logicalSuggestionId = "logical-1",
                    displayOrder = 1,
                    name = "Tent Bag",
                    normalizedName = "tent bag",
                    quantity = 1,
                    level = "required"
                )
            )
        )
        whenever(listOwnershipRepository.findByEquipmentListIdAndAccountId("list-1", "account-1"))
            .thenReturn(EquipmentListOwnership("list-1", "account-1"))
        whenever(listRepository.findById("list-1")).thenReturn(Optional.of(UserEquipmentListRecord("list-1", "周末")))
        whenever(memberRepository.findByEquipmentListIdOrderByPersonalEquipmentIdAsc("list-1"))
            .thenReturn(listOf(EquipmentListMember("list-1", "equipment-1", 1)))
        whenever(equipmentRepository.findById("equipment-1"))
            .thenReturn(Optional.of(PersonalEquipmentRecord("equipment-1", " tent   bag ", 1, 300)))
        whenever(equipmentOwnershipRepository.findByPersonalEquipmentIdAndAccountId("equipment-1", "account-1"))
            .thenReturn(PersonalEquipmentOwnership("equipment-1", "account-1", "tent bag"))

        val error = assertThrows(ApiContractException::class.java) {
            service(DirectTripGenerationPlanner).generate(
                "account-1",
                "collision-key",
                command(equipmentListId = "list-1")
            )
        }

        assertEquals("trip_generation_failed", error.code)
        verify(tripRepository, never()).save(any())
    }

    @Test
    fun `selected transport option is frozen on the created trip`() {
        val planner = FakeSelectionPlanner()
        whenever(selectionRepository.findById("selection-1")).thenReturn(Optional.of(planner.persistedSelection))

        service(planner).generate(
            "account-1",
            "selected-transport-key",
            command(selectionId = "selection-1", optionId = "option-a")
        )

        val savedTrip = org.mockito.kotlin.argumentCaptor<PersonalTripRecord>()
        verify(tripRepository).save(savedTrip.capture())
        assertEquals("option-a", savedTrip.firstValue.selectedTransportOptionId)
    }

    @Test
    fun `detail derives equipment summary from current visible items instead of stale snapshot cache`() {
        val trip = PersonalTripRecord(
            id = "trip-1",
            name = "贡嘎环线 · 2026-09-02",
            firstGeneratedAt = Instant.parse("2026-09-01T04:00:00Z"),
            departureCity = "上海",
            startDate = LocalDate.parse("2026-09-02"),
            endDate = LocalDate.parse("2026-09-02"),
            totalDayCount = 1,
            hikingDayCount = 1,
            revision = "revision-1",
            frozenRouteBasisJson = objectMapper.writeValueAsString(frozenBasis())
        )
        stubOwnedAggregate(trip)
        whenever(snapshotRepository.findByTripId("trip-1")).thenReturn(
            org.example.trip.personal.model.PersonalTripEquipmentSnapshotRecord(
                id = "snapshot-1",
                tripId = "trip-1",
                itemCount = 99,
                knownTotalWeightGrams = 99,
                missingWeightItemCount = 99,
                ownedItemCount = 99,
                unconfirmedOwnedItemCount = 0
            )
        )
        whenever(itemRepository.findBySnapshotIdOrderByDisplayOrderAsc("snapshot-1")).thenReturn(
            listOf(
                org.example.trip.personal.model.PersonalTripEquipmentItemRecord(
                    id = "item-1",
                    snapshotId = "snapshot-1",
                    displayOrder = 1,
                    name = "Tent",
                    normalizedName = "tent",
                    quantity = 4,
                    unitWeightGrams = 250,
                    source = "user_added",
                    ownershipStatus = "owned"
                )
            )
        )

        val result = service(DirectTripGenerationPlanner).detail("account-1", "trip-1")

        assertEquals(1, result.equipmentSnapshot.summary.itemCount)
        assertEquals(1000, result.equipmentSnapshot.summary.knownTotalWeight.grams)
        assertEquals(0, result.equipmentSnapshot.summary.missingWeightItemCount)
        assertEquals(1, result.equipmentSnapshot.summary.ownedItemCount)
    }

    @Test
    fun `generation context returns an ineligible summary instead of failing on malformed track data`() {
        whenever(versionRepository.findById("version-1")).thenReturn(
            Optional.of(
                routeVersion().copy(
                    name = null,
                    estimatedDurationSeconds = null,
                    mainTrackJson = "[[95.0,101.0]]"
                )
            )
        )
        val service = service(DirectTripGenerationPlanner)

        val context = service.generationContext("account-1", "route-1")

        assertEquals(false, context.route.generationEligibility.eligible)
        assertEquals(
            listOf("name", "estimatedDuration", "validMainTrack"),
            context.route.generationEligibility.missingReasons
        )
        assertEquals(null, context.route.name)
        assertEquals(null, context.route.estimatedDuration)
    }

    @Test
    fun `generation context and frozen basis use the same stored start and end summary facts`() {
        whenever(pointRepository.findByRouteVersionIdOrderByDisplayOrderAsc("version-1")).thenReturn(
            listOf(
                RouteVersionPoint(
                    id = "start-point",
                    routeVersionId = "version-1",
                    pointKind = "start",
                    displayOrder = 1,
                    name = "徒步起点",
                    latitude = 30.0,
                    longitude = 101.0,
                    referenceSystem = "WGS84"
                ),
                RouteVersionPoint(
                    id = "end-point",
                    routeVersionId = "version-1",
                    pointKind = "end",
                    displayOrder = 2,
                    name = "徒步终点",
                    latitude = 30.1,
                    longitude = 101.1,
                    referenceSystem = "WGS84"
                )
            )
        )
        val service = service(DirectTripGenerationPlanner)

        val context = service.generationContext("account-1", "route-1")
        val generated = service.generate("account-1", "summary-facts-key", command()) as GenerateTripResult.TripCreated

        assertEquals(context.route.start, generated.trip.trip.frozenRouteBasis.start)
        assertEquals(context.route.end, generated.trip.trip.frozenRouteBasis.end)
        assertEquals(30.0, context.route.start?.position?.latitude)
        assertEquals(101.1, context.route.end?.position?.longitude)
    }

    @Test
    fun `generation context rejects a list member not owned by the same account without exposing it`() {
        whenever(listOwnershipRepository.findByAccountIdOrderByEquipmentListIdAsc("account-1"))
            .thenReturn(listOf(EquipmentListOwnership("list-1", "account-1")))
        whenever(listRepository.findById("list-1"))
            .thenReturn(Optional.of(UserEquipmentListRecord("list-1", "周末")))
        whenever(memberRepository.findByEquipmentListIdOrderByPersonalEquipmentIdAsc("list-1"))
            .thenReturn(listOf(EquipmentListMember("list-1", "foreign-equipment", 1)))
        whenever(equipmentOwnershipRepository.findByPersonalEquipmentIdAndAccountId("foreign-equipment", "account-1"))
            .thenReturn(null)
        val service = service(DirectTripGenerationPlanner)

        val error = assertThrows(ApiContractException::class.java) {
            service.generationContext("account-1", "route-1")
        }

        assertEquals("personal_trip_read_failed", error.code)
        verify(equipmentRepository, never()).findById("foreign-equipment")
    }

    @Test
    fun `valid non empty single coordinate track can generate a trip`() {
        whenever(versionRepository.findById("version-1")).thenReturn(
            Optional.of(routeVersion().copy(mainTrackJson = "[[30.0,101.0]]"))
        )
        val service = service(DirectTripGenerationPlanner)

        val context = service.generationContext("account-1", "route-1")
        val result = service.generate("account-1", "single-coordinate-key", command())

        assertTrue(context.route.generationEligibility.eligible)
        assertTrue(result is GenerateTripResult.TripCreated)
        val section = (result as GenerateTripResult.TripCreated)
            .trip.days.single().actions.single { it.actionType == "hike" }.routeSectionSnapshot
        assertEquals(1, section?.path?.size)
    }

    @Test
    fun `multi day generation forms consecutive actual days and preserves round trip transport`() {
        whenever(versionRepository.findById("version-1")).thenReturn(
            Optional.of(
                routeVersion().copy(
                    routeType = "multi_day",
                    estimatedDurationSeconds = 172_800,
                    mainTrackJson = "[[30.0,101.0],[30.05,101.05],[30.1,101.1]]"
                )
            )
        )
        val service = service(DirectTripGenerationPlanner)

        val result = service.generate("account-1", "multi-day-key", command()) as GenerateTripResult.TripCreated

        assertEquals(2, result.trip.days.size)
        assertEquals(
            listOf(LocalDate.parse("2026-09-02"), LocalDate.parse("2026-09-03")),
            result.trip.days.map { it.date }
        )
        assertTrue(result.trip.days.all { it.actions.isNotEmpty() })
        assertEquals("上海", result.trip.days.first().actions.first().origin?.value?.name)
        assertEquals("上海", result.trip.days.last().actions.last().destination?.value?.name)
        assertEquals(2, result.trip.trip.totalDayCount)
        assertEquals(2, result.trip.trip.hikingDayCount)
    }

    @Test
    fun `selected list rejects a member not owned by the same account without exposing it`() {
        whenever(listOwnershipRepository.findByEquipmentListIdAndAccountId("list-1", "account-1"))
            .thenReturn(EquipmentListOwnership("list-1", "account-1"))
        whenever(listRepository.findById("list-1")).thenReturn(Optional.of(UserEquipmentListRecord("list-1", "周末")))
        whenever(memberRepository.findByEquipmentListIdOrderByPersonalEquipmentIdAsc("list-1"))
            .thenReturn(listOf(EquipmentListMember("list-1", "foreign-equipment", 1)))
        whenever(equipmentOwnershipRepository.findByPersonalEquipmentIdAndAccountId("foreign-equipment", "account-1"))
            .thenReturn(null)
        val service = service(DirectTripGenerationPlanner)

        val error = assertThrows(ApiContractException::class.java) {
            service.generate("account-1", "foreign-member-key", command(equipmentListId = "list-1"))
        }

        assertEquals("trip_generation_failed", error.code)
        verify(equipmentRepository, never()).findById("foreign-equipment")
        verify(tripRepository, never()).save(any())
    }

    @Test
    fun `selected list quantity beyond current ownership is frozen as unconfirmed owned`() {
        whenever(listOwnershipRepository.findByEquipmentListIdAndAccountId("list-1", "account-1"))
            .thenReturn(EquipmentListOwnership("list-1", "account-1"))
        whenever(listRepository.findById("list-1")).thenReturn(Optional.of(UserEquipmentListRecord("list-1", "周末")))
        whenever(memberRepository.findByEquipmentListIdOrderByPersonalEquipmentIdAsc("list-1"))
            .thenReturn(listOf(EquipmentListMember("list-1", "equipment-1", 2)))
        whenever(equipmentRepository.findById("equipment-1"))
            .thenReturn(Optional.of(PersonalEquipmentRecord("equipment-1", "登山杖", 1, 300)))
        whenever(equipmentOwnershipRepository.findByPersonalEquipmentIdAndAccountId("equipment-1", "account-1"))
            .thenReturn(PersonalEquipmentOwnership("equipment-1", "account-1", "登山杖"))
        whenever(equipmentOwnershipRepository.findByAccountIdOrderByPersonalEquipmentIdAsc("account-1"))
            .thenReturn(listOf(PersonalEquipmentOwnership("equipment-1", "account-1", "登山杖")))
        val service = service(DirectTripGenerationPlanner)

        val result = service.generate(
            "account-1",
            "insufficient-quantity-key",
            command(equipmentListId = "list-1")
        ) as GenerateTripResult.TripCreated

        assertEquals(0, result.trip.equipmentSnapshot.summary.ownedItemCount)
        assertEquals(1, result.trip.equipmentSnapshot.summary.unconfirmedOwnedItemCount)
        val captured = org.mockito.kotlin.argumentCaptor<List<org.example.trip.personal.model.PersonalTripEquipmentItemRecord>>()
        verify(itemRepository).saveAll(captured.capture())
        assertEquals("unconfirmed_owned", captured.firstValue.single().ownershipStatus)
    }

    @Test
    fun `first generation derives ownership from the complete account collection by normalized name`() {
        whenever(listOwnershipRepository.findByEquipmentListIdAndAccountId("list-1", "account-1"))
            .thenReturn(EquipmentListOwnership("list-1", "account-1"))
        whenever(listRepository.findById("list-1")).thenReturn(Optional.of(UserEquipmentListRecord("list-1", "周末")))
        whenever(memberRepository.findByEquipmentListIdOrderByPersonalEquipmentIdAsc("list-1"))
            .thenReturn(listOf(EquipmentListMember("list-1", "selected-equipment", 2)))
        whenever(equipmentRepository.findById("selected-equipment"))
            .thenReturn(Optional.of(PersonalEquipmentRecord("selected-equipment", " Tent   Bag ", 3, 300)))
        whenever(equipmentOwnershipRepository.findByPersonalEquipmentIdAndAccountId("selected-equipment", "account-1"))
            .thenReturn(PersonalEquipmentOwnership("selected-equipment", "account-1", "tent bag"))
        whenever(equipmentOwnershipRepository.findByAccountIdOrderByPersonalEquipmentIdAsc("account-1"))
            .thenReturn(listOf(PersonalEquipmentOwnership("selected-equipment", "account-1", "tent bag")))
        val service = service(DirectTripGenerationPlanner)

        val result = service.generate(
            "account-1",
            "account-wide-ownership-key",
            command(equipmentListId = "list-1")
        ) as GenerateTripResult.TripCreated

        assertEquals(1, result.trip.equipmentSnapshot.summary.ownedItemCount)
        val captured = org.mockito.kotlin.argumentCaptor<List<org.example.trip.personal.model.PersonalTripEquipmentItemRecord>>()
        verify(itemRepository).saveAll(captured.capture())
        assertEquals("Tent Bag", captured.firstValue.single().name)
        assertEquals("tent bag", captured.firstValue.single().normalizedName)
        assertEquals("owned", captured.firstValue.single().ownershipStatus)
    }

    @Test
    fun `selection planner does not create trip before a valid continuation`() {
        val planner = FakeSelectionPlanner()
        val service = service(planner)

        val first = service.generate("account-1", "key-1", command())

        assertTrue(first is GenerateTripResult.TransportSelectionRequired)
        verify(tripRepository, never()).save(any())

        whenever(selectionRepository.findById("selection-1")).thenReturn(Optional.of(planner.persistedSelection))
        val created = service.generate(
            "account-1",
            "key-2",
            command(selectionId = "selection-1", optionId = "option-a")
        )
        assertTrue(created is GenerateTripResult.TripCreated)
        verify(tripRepository).save(any())
    }

    @Test
    fun `invalid route version date and equipment ownership fail with contract errors`() {
        val service = service(DirectTripGenerationPlanner)

        val versionError = assertThrows(ApiContractException::class.java) {
            service.generate("account-1", "key-version", command(routeVersionId = "old-version"))
        }
        assertEquals("route_version_conflict", versionError.code)

        val dateError = assertThrows(ApiContractException::class.java) {
            service.generate("account-1", "key-date", command(startDate = LocalDate.parse("2026-08-31")))
        }
        assertEquals("trip_start_date_invalid", dateError.code)

        whenever(listOwnershipRepository.findByEquipmentListIdAndAccountId("other-list", "account-1")).thenReturn(null)
        val listError = assertThrows(ApiContractException::class.java) {
            service.generate("account-1", "key-list", command(equipmentListId = "other-list"))
        }
        assertEquals("equipment_list_not_found", listError.code)
    }

    @Test
    fun `same idempotency request replays frozen response and different request conflicts`() {
        val service = service(DirectTripGenerationPlanner)
        val first = service.generate("account-1", "same-key", command())
        val requestHash = org.mockito.kotlin.argumentCaptor<String>()
        val responseType = org.mockito.kotlin.argumentCaptor<String>()
        val responseJson = org.mockito.kotlin.argumentCaptor<String>()
        verify(idempotencyRepository).completeReservation(
            eq("account-1"),
            eq("generate_trip"),
            eq("same-key"),
            requestHash.capture(),
            responseType.capture(),
            responseJson.capture()
        )
        whenever(idempotencyRepository.findByAccountIdAndOperationAndIdempotencyKey("account-1", "generate_trip", "same-key"))
            .thenReturn(
                PersonalTripIdempotencyRecord(
                    id = "committed-id",
                    accountId = "account-1",
                    operation = "generate_trip",
                    idempotencyKey = "same-key",
                    requestHash = requestHash.firstValue,
                    responseType = responseType.firstValue,
                    responseJson = responseJson.firstValue
                )
            )

        val replay = service.generate("account-1", "same-key", command())
        assertEquals(first, replay)

        val conflict = assertThrows(ApiContractException::class.java) {
            service.generate("account-1", "same-key", command(departureCity = "北京"))
        }
        assertEquals("idempotency_conflict", conflict.code)
    }

    @Test
    fun `concurrent idempotency uniqueness collision replays the committed response`() {
        val service = service(DirectTripGenerationPlanner)
        val first = service.generate("account-1", "first-key", command())
        val requestHash = org.mockito.kotlin.argumentCaptor<String>()
        val responseType = org.mockito.kotlin.argumentCaptor<String>()
        val responseJson = org.mockito.kotlin.argumentCaptor<String>()
        verify(idempotencyRepository).completeReservation(
            eq("account-1"),
            eq("generate_trip"),
            eq("first-key"),
            requestHash.capture(),
            responseType.capture(),
            responseJson.capture()
        )
        val committed = PersonalTripIdempotencyRecord(
            id = "committed-id",
            accountId = "account-1",
            operation = "generate_trip",
            idempotencyKey = "race-key",
            requestHash = requestHash.firstValue,
            responseType = responseType.firstValue,
            responseJson = responseJson.firstValue
        )
        whenever(
            idempotencyRepository.insertReservation(
                any(),
                eq("account-1"),
                eq("generate_trip"),
                eq("race-key"),
                any(),
                any(),
                any()
            )
        ).thenReturn(0)
        whenever(
            idempotencyRepository.findClaimForUpdate(
                "account-1",
                "generate_trip",
                "race-key"
            )
        ).thenReturn(committed)

        val replay = service.generate("account-1", "race-key", command())

        assertEquals(first, replay)
    }

    @Test
    fun `cancellation requires planned current revision and preserves frozen aggregate`() {
        val trip = PersonalTripRecord(
            id = "trip-1",
            name = "贡嘎环线 · 2026-09-02",
            firstGeneratedAt = Instant.parse("2026-09-01T04:00:00Z"),
            departureCity = "上海",
            startDate = LocalDate.parse("2026-09-02"),
            endDate = LocalDate.parse("2026-09-02"),
            totalDayCount = 1,
            hikingDayCount = 1,
            revision = "revision-1",
            frozenRouteBasisJson = objectMapper.writeValueAsString(frozenBasis())
        )
        stubOwnedAggregate(trip)
        val service = service(DirectTripGenerationPlanner)

        val cancelled = service.cancel(
            "account-1",
            "trip-1",
            "cancel-key",
            org.example.trip.personal.dto.CancelTripCommand("revision-1", true)
        )

        assertEquals("cancelled", cancelled.trip.status)
        verify(tripRepository).findByIdForUpdate("trip-1")
        assertTrue(cancelled.revision != "revision-1")
        assertEquals("trip-1", cancelled.trip.identity)
        assertEquals(1, cancelled.days.size)
        val frozenRevision = cancelled.revision
        val frozenResponse = org.mockito.kotlin.argumentCaptor<String>()
        verify(idempotencyRepository).completeReservation(
            eq("account-1"),
            eq("cancel_trip:trip-1"),
            eq("cancel-key"),
            any(),
            eq("personal_trip_detail"),
            frozenResponse.capture()
        )
        whenever(
            idempotencyRepository.findByAccountIdAndOperationAndIdempotencyKey(
                "account-1",
                "cancel_trip:trip-1",
                "cancel-key"
            )
        ).thenReturn(
            PersonalTripIdempotencyRecord(
                id = "cancelled-id",
                accountId = "account-1",
                operation = "cancel_trip:trip-1",
                idempotencyKey = "cancel-key",
                requestHash = requestHash(CancelTripCommand("revision-1", true)),
                responseType = "personal_trip_detail",
                responseJson = frozenResponse.firstValue
            )
        )
        trip.revision = "changed-after-first-response"

        val replay = service.cancel(
            "account-1",
            "trip-1",
            "cancel-key",
            CancelTripCommand("revision-1", true)
        )

        assertEquals(frozenRevision, replay.revision)
        assertEquals(cancelled, replay)

        trip.lifecycleState = "active"
        trip.revision = "revision-2"
        val conflict = assertThrows(ApiContractException::class.java) {
            service.cancel(
                "account-1",
                "trip-1",
                "cancel-key-2",
                org.example.trip.personal.dto.CancelTripCommand("revision-1", true)
            )
        }
        assertEquals("concurrent_modification", conflict.code)
    }

    @Test
    fun `version status and migration keep identity while switching the complete route basis`() {
        val trip = PersonalTripRecord(
            id = "trip-1",
            name = "贡嘎环线 · 2026-09-02",
            firstGeneratedAt = Instant.parse("2026-09-01T04:00:00Z"),
            departureCity = "上海",
            startDate = LocalDate.parse("2026-09-02"),
            endDate = LocalDate.parse("2026-09-02"),
            totalDayCount = 1,
            hikingDayCount = 1,
            revision = "revision-1",
            frozenRouteBasisJson = objectMapper.writeValueAsString(frozenBasis())
        )
        val relation = org.example.trip.personal.model.TripFrozenRouteVersion("trip-1", "version-1")
        stubOwnedAggregate(trip, relation)
        val target = routeVersion().copy(id = "version-2", versionLabel = "2027 春季版", name = "贡嘎新版")
        whenever(currentVersionRepository.findById("route-1"))
            .thenReturn(Optional.of(RouteCurrentPublicVersion("route-1", "version-2")))
        whenever(currentVersionRepository.findByRouteIdForUpdate("route-1"))
            .thenReturn(RouteCurrentPublicVersion("route-1", "version-2"))
        whenever(versionRepository.findById("version-2")).thenReturn(Optional.of(target))
        whenever(publicationOrderRepository.findByRouteVersionId("version-2"))
            .thenReturn(RouteVersionPublicationOrder("route-1", "version-2", 2))
        whenever(imageRepository.findByRouteVersionIdOrderByDisplayOrderAsc("version-2")).thenReturn(emptyList())
        whenever(pointRepository.findByRouteVersionIdOrderByDisplayOrderAsc("version-2")).thenReturn(emptyList())
        val service = service(DirectTripGenerationPlanner)

        val status = service.routeVersionStatus("account-1", "trip-1")
        assertEquals("newer_public_version_available", status.versionRelationship)
        assertTrue(status.migrationEligible)

        val migrated = service.migrate(
            "account-1",
            "trip-1",
            "migration-key",
            org.example.trip.personal.dto.MigrateTripCommand("revision-1", "version-2")
        )
        assertEquals("trip-1", migrated.trip.identity)
        verify(tripRepository).findByIdForUpdate("trip-1")
        verify(currentVersionRepository).findByRouteIdForUpdate("route-1")
        assertEquals("version-2", migrated.routeReference.adoptedRouteVersionId)
        assertEquals("贡嘎新版", migrated.trip.frozenRouteBasis.routeName)
        assertTrue(migrated.revision != "revision-1")
        assertEquals("version-2", relation.routeVersionId)
        assertEquals(trip.firstGeneratedAt, migrated.trip.firstGeneratedAt)
    }

    @Test
    fun `migration builds equipment candidate by explicit logical identity before replacing persistence`() {
        val trip = PersonalTripRecord(
            id = "trip-1",
            name = "贡嘎环线 · 2026-09-02",
            firstGeneratedAt = Instant.parse("2026-09-01T04:00:00Z"),
            departureCity = "上海",
            startDate = LocalDate.parse("2026-09-02"),
            endDate = LocalDate.parse("2026-09-02"),
            totalDayCount = 1,
            hikingDayCount = 1,
            revision = "revision-1",
            frozenRouteBasisJson = objectMapper.writeValueAsString(frozenBasis())
        )
        val relation = org.example.trip.personal.model.TripFrozenRouteVersion("trip-1", "version-1")
        stubOwnedAggregate(trip, relation)
        val adjusted = org.example.trip.personal.model.PersonalTripEquipmentItemRecord(
            id = "old-adjusted",
            snapshotId = "snapshot-1",
            displayOrder = 1,
            name = "我的帐篷",
            normalizedName = "我的帐篷",
            quantity = 2,
            unitWeightGrams = 800,
            source = "user_adjusted",
            ownershipStatus = "unconfirmed_owned"
        )
        val added = org.example.trip.personal.model.PersonalTripEquipmentItemRecord(
            id = "old-added",
            snapshotId = "snapshot-1",
            displayOrder = 2,
            name = "水瓶",
            normalizedName = "水瓶",
            quantity = 1,
            unitWeightGrams = 100,
            source = "user_added",
            ownershipStatus = "owned"
        )
        whenever(itemRepository.findBySnapshotIdOrderByDisplayOrderAsc("snapshot-1"))
            .thenReturn(listOf(adjusted, added))
        whenever(derivationRepository.findByTripIdOrderByItemIdAsc("trip-1")).thenReturn(
            listOf(
                org.example.trip.personal.model.PersonalTripEquipmentItemDerivedFromSuggestion(
                    itemId = "old-adjusted",
                    tripId = "trip-1",
                    logicalSuggestionId = "logical-tent",
                    suggestionOccurrenceId = "old-tent",
                    routeId = "route-1"
                )
            )
        )
        whenever(suppressionRepository.findByTripIdOrderByLogicalSuggestionIdAsc("trip-1")).thenReturn(
            listOf(org.example.trip.personal.model.TripSuppressesEquipmentSuggestion("trip-1", "logical-water", "route-1"))
        )
        val target = routeVersion().copy(id = "version-2", name = "贡嘎新版")
        whenever(currentVersionRepository.findById("route-1"))
            .thenReturn(Optional.of(RouteCurrentPublicVersion("route-1", "version-2")))
        whenever(currentVersionRepository.findByRouteIdForUpdate("route-1"))
            .thenReturn(RouteCurrentPublicVersion("route-1", "version-2"))
        whenever(versionRepository.findById("version-2")).thenReturn(Optional.of(target))
        whenever(publicationOrderRepository.findByRouteVersionId("version-2"))
            .thenReturn(RouteVersionPublicationOrder("route-1", "version-2", 2))
        whenever(imageRepository.findByRouteVersionIdOrderByDisplayOrderAsc("version-2")).thenReturn(emptyList())
        whenever(pointRepository.findByRouteVersionIdOrderByDisplayOrderAsc("version-2")).thenReturn(emptyList())
        whenever(suggestionRepository.findByRouteVersionIdOrderByDisplayOrderAsc("version-2")).thenReturn(
            listOf(
                org.example.route.model.RouteVersionEquipmentSuggestion(
                    id = "target-tent",
                    routeId = "route-1",
                    routeVersionId = "version-2",
                    logicalSuggestionId = "logical-tent",
                    displayOrder = 1,
                    name = "目标帐篷",
                    normalizedName = "目标帐篷",
                    quantity = 1,
                    unitWeightGrams = 600,
                    level = "required"
                ),
                org.example.route.model.RouteVersionEquipmentSuggestion(
                    id = "target-water",
                    routeId = "route-1",
                    routeVersionId = "version-2",
                    logicalSuggestionId = "logical-water",
                    displayOrder = 2,
                    name = "净水器",
                    normalizedName = "净水器",
                    quantity = 1,
                    level = "recommended"
                )
            )
        )
        whenever(equipmentOwnershipRepository.findByAccountIdOrderByPersonalEquipmentIdAsc("account-1")).thenReturn(
            listOf(org.example.equipment.model.PersonalEquipmentOwnership("owned-tent", "account-1", "我的帐篷"))
        )
        whenever(equipmentRepository.findById("owned-tent")).thenReturn(
            Optional.of(PersonalEquipmentRecord("owned-tent", "我的帐篷", 2, 800))
        )
        val service = service(DirectTripGenerationPlanner)

        service.migrate(
            "account-1",
            "trip-1",
            "migration-equipment-key",
            org.example.trip.personal.dto.MigrateTripCommand("revision-1", "version-2")
        )

        val savedItems = org.mockito.kotlin.argumentCaptor<List<org.example.trip.personal.model.PersonalTripEquipmentItemRecord>>()
        verify(itemRepository).saveAll(savedItems.capture())
        assertEquals(listOf("我的帐篷", "水瓶"), savedItems.firstValue.map { it.name })
        assertEquals(listOf("user_adjusted", "user_added"), savedItems.firstValue.map { it.source })
        assertEquals(listOf("owned", "unconfirmed_owned"), savedItems.firstValue.map { it.ownershipStatus })
        val savedRelations = org.mockito.kotlin.argumentCaptor<List<org.example.trip.personal.model.PersonalTripEquipmentItemDerivedFromSuggestion>>()
        verify(derivationRepository).saveAll(savedRelations.capture())
        assertEquals(listOf("logical-tent"), savedRelations.firstValue.map { it.logicalSuggestionId })
        assertEquals(listOf("target-tent"), savedRelations.firstValue.map { it.suggestionOccurrenceId })
    }

    @Test
    fun `migration candidate failure happens before any aggregate replacement`() {
        val trip = PersonalTripRecord(
            id = "trip-1",
            name = "贡嘎环线 · 2026-09-02",
            firstGeneratedAt = Instant.parse("2026-09-01T04:00:00Z"),
            departureCity = "上海",
            startDate = LocalDate.parse("2026-09-02"),
            endDate = LocalDate.parse("2026-09-02"),
            totalDayCount = 1,
            hikingDayCount = 1,
            revision = "revision-1",
            frozenRouteBasisJson = objectMapper.writeValueAsString(frozenBasis())
        )
        val relation = org.example.trip.personal.model.TripFrozenRouteVersion("trip-1", "version-1")
        stubOwnedAggregate(trip, relation)
        val target = routeVersion().copy(id = "version-2", name = "贡嘎新版")
        whenever(currentVersionRepository.findById("route-1"))
            .thenReturn(Optional.of(RouteCurrentPublicVersion("route-1", "version-2")))
        whenever(currentVersionRepository.findByRouteIdForUpdate("route-1"))
            .thenReturn(RouteCurrentPublicVersion("route-1", "version-2"))
        whenever(versionRepository.findById("version-2")).thenReturn(Optional.of(target))
        whenever(publicationOrderRepository.findByRouteVersionId("version-2"))
            .thenReturn(RouteVersionPublicationOrder("route-1", "version-2", 2))
        whenever(imageRepository.findByRouteVersionIdOrderByDisplayOrderAsc("version-2")).thenReturn(emptyList())
        whenever(pointRepository.findByRouteVersionIdOrderByDisplayOrderAsc("version-2")).thenReturn(emptyList())
        whenever(suggestionRepository.findByRouteVersionIdOrderByDisplayOrderAsc("version-2")).thenReturn(
            listOf(
                org.example.route.model.RouteVersionEquipmentSuggestion(
                    id = "suggestion-1",
                    routeId = "route-1",
                    routeVersionId = "version-2",
                    logicalSuggestionId = "logical-1",
                    displayOrder = 1,
                    name = "Tent Bag",
                    normalizedName = "tent bag",
                    quantity = 1,
                    level = "required"
                ),
                org.example.route.model.RouteVersionEquipmentSuggestion(
                    id = "suggestion-2",
                    routeId = "route-1",
                    routeVersionId = "version-2",
                    logicalSuggestionId = "logical-2",
                    displayOrder = 2,
                    name = "  tent   bag ",
                    normalizedName = "tent bag",
                    quantity = 1,
                    level = "recommended"
                )
            )
        )
        val service = service(DirectTripGenerationPlanner)

        val error = assertThrows(ApiContractException::class.java) {
            service.migrate(
                "account-1",
                "trip-1",
                "migration-failure-key",
                org.example.trip.personal.dto.MigrateTripCommand("revision-1", "version-2")
            )
        }

        assertEquals("migration_generation_failed", error.code)
        assertEquals("version-1", relation.routeVersionId)
        assertEquals("revision-1", trip.revision)
        verify(derivationRepository, never()).deleteByTripId(any())
        verify(itemRepository, never()).deleteBySnapshotId(any())
        verify(snapshotRepository, never()).deleteByTripId(any())
        verify(dayRepository, never()).deleteByTripId(any())
    }

    @Test
    fun `migration target change exposes only typed requested and current identities`() {
        val trip = PersonalTripRecord(
            id = "trip-1",
            name = "贡嘎环线 · 2026-09-02",
            firstGeneratedAt = Instant.parse("2026-09-01T04:00:00Z"),
            departureCity = "上海",
            startDate = LocalDate.parse("2026-09-02"),
            endDate = LocalDate.parse("2026-09-02"),
            totalDayCount = 1,
            hikingDayCount = 1,
            revision = "revision-1",
            frozenRouteBasisJson = objectMapper.writeValueAsString(frozenBasis())
        )
        stubOwnedAggregate(trip)
        whenever(currentVersionRepository.findById("route-1"))
            .thenReturn(Optional.of(RouteCurrentPublicVersion("route-1", "version-3")))
        whenever(currentVersionRepository.findByRouteIdForUpdate("route-1"))
            .thenReturn(RouteCurrentPublicVersion("route-1", "version-3"))
        val service = service(DirectTripGenerationPlanner)

        val error = assertThrows(ApiContractException::class.java) {
            service.migrate(
                "account-1",
                "trip-1",
                "migration-changed-key",
                org.example.trip.personal.dto.MigrateTripCommand("revision-1", "version-2")
            )
        }

        assertEquals("migration_target_changed", error.code)
        assertEquals(
            org.example.trip.personal.dto.MigrationTargetChangedDetails("version-2", "version-3"),
            error.details
        )
        verify(dayRepository, never()).deleteByTripId(any())
    }

    @Test
    fun `different version is unavailable when explicit publication order does not prove newer`() {
        val trip = PersonalTripRecord(
            id = "trip-1",
            name = "贡嘎环线 · 2026-09-02",
            firstGeneratedAt = Instant.parse("2026-09-01T04:00:00Z"),
            departureCity = "上海",
            startDate = LocalDate.parse("2026-09-02"),
            endDate = LocalDate.parse("2026-09-02"),
            totalDayCount = 1,
            hikingDayCount = 1,
            revision = "revision-1",
            frozenRouteBasisJson = objectMapper.writeValueAsString(frozenBasis())
        )
        stubOwnedAggregate(trip)
        whenever(currentVersionRepository.findById("route-1"))
            .thenReturn(Optional.of(RouteCurrentPublicVersion("route-1", "version-2")))
        whenever(versionRepository.findById("version-2"))
            .thenReturn(Optional.of(routeVersion().copy(id = "version-2")))
        whenever(publicationOrderRepository.findByRouteVersionId("version-2"))
            .thenReturn(RouteVersionPublicationOrder("route-1", "version-2", 1))

        val status = service(DirectTripGenerationPlanner).routeVersionStatus("account-1", "trip-1")

        assertEquals("current_public_version_unavailable", status.versionRelationship)
        assertEquals(null, status.currentPublicVersion)
        assertEquals(null, status.difference)
        assertEquals(false, status.migrationEligible)
    }

    private fun stubOwnedAggregate(
        trip: PersonalTripRecord,
        relation: org.example.trip.personal.model.TripFrozenRouteVersion =
            org.example.trip.personal.model.TripFrozenRouteVersion("trip-1", "version-1")
    ) {
        whenever(ownershipRepository.findByTripIdAndAccountId("trip-1", "account-1"))
            .thenReturn(org.example.trip.personal.model.PersonalTripOwnership("trip-1", "account-1"))
        whenever(tripRepository.findById("trip-1")).thenReturn(Optional.of(trip))
        whenever(tripRepository.findByIdForUpdate("trip-1")).thenReturn(trip)
        whenever(versionRelationRepository.findById("trip-1")).thenReturn(Optional.of(relation))
        whenever(dayRepository.findByTripIdOrderByDayNumberAsc("trip-1")).thenReturn(
            listOf(
                org.example.trip.personal.model.PersonalTripDayRecord(
                    id = "day-1",
                    tripId = "trip-1",
                    dayNumber = 1,
                    date = LocalDate.parse("2026-09-02"),
                    primaryStage = "徒步",
                    hikingDayNumber = 1,
                    contentJson = objectMapper.writeValueAsString(generatedDay())
                )
            )
        )
        whenever(snapshotRepository.findByTripId("trip-1")).thenReturn(
            org.example.trip.personal.model.PersonalTripEquipmentSnapshotRecord(
                id = "snapshot-1",
                tripId = "trip-1",
                itemCount = 0,
                knownTotalWeightGrams = 0,
                missingWeightItemCount = 0,
                ownedItemCount = 0,
                unconfirmedOwnedItemCount = 0
            )
        )
        whenever(itemRepository.findBySnapshotIdOrderByDisplayOrderAsc("snapshot-1")).thenReturn(emptyList())
    }

    private fun frozenBasis() = org.example.trip.personal.dto.FrozenRouteBasisProjection(
        routeName = "贡嘎环线",
        routeType = "one_day",
        region = "四川甘孜",
        start = org.example.route.dto.PublicRoutePlace("徒步起点"),
        end = org.example.route.dto.PublicRoutePlace("徒步终点"),
        estimatedDuration = org.example.route.dto.RouteSeconds(28_800.0),
        mainTrackPath = listOf(
            org.example.route.dto.PublicRouteGeoPosition(30.0, 101.0, "WGS84"),
            org.example.route.dto.PublicRouteGeoPosition(30.1, 101.1, "WGS84")
        ),
        versionLabel = "2026 秋季版"
    )

    private fun generatedDay() = org.example.trip.personal.dto.TripDayProjection(
        identity = "day-1",
        dayNumber = 1,
        date = LocalDate.parse("2026-09-02"),
        primaryStage = "徒步",
        hikingDayNumber = 1,
        actions = listOf(
            org.example.trip.personal.dto.TripActionProjection(
                sequence = 1,
                actionType = "hike"
            )
        ),
        weather = org.example.trip.personal.dto.TripDayWeatherProjection(
            condition = unavailableValue(),
            temperatureRange = unavailableValue(),
            precipitation = unavailableValue(),
            wind = unavailableValue(),
            placeContext = unavailableValue(),
            routeSectionContext = unavailableValue()
        )
    )

    private fun unavailableValue() = QualifiedValueProjection<Any>(
        confidence = InformationConfidenceProjection("unavailable", "dynamic_external_information")
    )

    private fun requestHash(value: Any): String {
        val normalized = objectMapper.writeValueAsString(value)
        val digest = java.security.MessageDigest.getInstance("SHA-256")
            .digest(normalized.toByteArray(java.nio.charset.StandardCharsets.UTF_8))
        return digest.joinToString("") { "%02x".format(it) }
    }

    private fun service(planner: TripGenerationPlanner) = PersonalTripApplicationService(
        tripRepository,
        ownershipRepository,
        versionRelationRepository,
        dayRepository,
        snapshotRepository,
        itemRepository,
        derivationRepository,
        suppressionRepository,
        idempotencyRepository,
        selectionRepository,
        collectionRepository,
        currentVersionRepository,
        versionRepository,
        publicationOrderRepository,
        imageRepository,
        segmentRepository,
        pointRepository,
        RouteVersionSummaryPlaceResolver(),
        suggestionRepository,
        listRepository,
        listOwnershipRepository,
        memberRepository,
        equipmentRepository,
        equipmentOwnershipRepository,
        PersonalEquipmentDomainService(),
        PersonalTripDomainService(clock),
        RouteVersionDifferenceService(),
        TripMigrationEquipmentMerger(PersonalEquipmentDomainService()),
        planner,
        objectMapper,
        clock
    )

    private fun command(
        routeVersionId: String = "version-1",
        departureCity: String = "上海",
        startDate: LocalDate = LocalDate.parse("2026-09-02"),
        equipmentListId: String? = null,
        selectionId: String? = null,
        optionId: String? = null
    ) = GenerateTripCommand(
        routeId = "route-1",
        routeVersionId = routeVersionId,
        departureCity = departureCity,
        startDate = startDate,
        equipmentListId = equipmentListId,
        transportSelection = selectionId?.let {
            org.example.trip.personal.dto.TransportSelectionCommand(it, requireNotNull(optionId))
        }
    )

    private fun routeVersion() = RouteVersion(
        id = "version-1",
        routeId = "route-1",
        versionLabel = "2026 秋季版",
        routeType = "one_day",
        name = "贡嘎环线",
        region = "四川甘孜",
        startName = "徒步起点",
        endName = "徒步终点",
        estimatedDurationSeconds = 28_800,
        mainTrackAvailability = "valid",
        mainTrackReferenceSystem = "WGS84",
        mainTrackJson = "[[30.0,101.0],[30.1,101.1]]"
    )

    private class FakeSelectionPlanner : TripGenerationPlanner {
        val persistedSelection = org.example.trip.personal.model.TripTransportSelectionRecord(
            selectionId = "selection-1",
            accountId = "account-1",
            requestHash = "placeholder",
            contextJson = "placeholder",
            optionsJson = "placeholder"
        )

        override fun plan(input: TripPlanningInput): TripPlanningDecision =
            if (input.transportOptionId == null) {
                TripPlanningDecision.SelectionRequired(
                    selectionId = "selection-1",
                    options = listOf(option("option-a"), option("option-b"))
                )
            } else {
                TripPlanningDecision.Ready
            }

        private fun option(id: String) = TransportOptionProjection(
            transportOptionId = id,
            transferCount = unavailable(),
            estimatedArrivalAt = unavailable(),
            estimatedDuration = unavailable(),
            verificationItems = emptyList()
        )

        private fun <T> unavailable() = QualifiedValueProjection<T>(
            confidence = InformationConfidenceProjection("unavailable", "dynamic_external_information")
        )
    }
}
