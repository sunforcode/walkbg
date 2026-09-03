package org.example.trip.personal.service

import com.fasterxml.jackson.databind.ObjectMapper
import org.example.common.contract.ApiContractException
import org.example.equipment.model.PersonalEquipmentOwnership
import org.example.equipment.model.PersonalEquipmentRecord
import org.example.equipment.repository.PersonalEquipmentOwnershipRepository
import org.example.equipment.repository.PersonalEquipmentRecordRepository
import org.example.equipment.service.PersonalEquipmentDomainService
import org.example.route.model.RouteVersion
import org.example.route.repository.RouteVersionRepository
import org.example.trip.personal.dto.CreateTripEquipmentItemCommand
import org.example.trip.personal.dto.FrozenRouteBasisProjection
import org.example.trip.personal.dto.UpdateTripEquipmentItemCommand
import org.example.trip.personal.model.PersonalTripEquipmentItemDerivedFromSuggestion
import org.example.trip.personal.model.PersonalTripEquipmentItemRecord
import org.example.trip.personal.model.PersonalTripEquipmentSnapshotRecord
import org.example.trip.personal.model.PersonalTripOwnership
import org.example.trip.personal.model.PersonalTripRecord
import org.example.trip.personal.model.TripSuppressesEquipmentSuggestion
import org.example.trip.personal.repository.PersonalTripEquipmentItemDerivationRepository
import org.example.trip.personal.repository.PersonalTripEquipmentItemRepository
import org.example.trip.personal.repository.PersonalTripEquipmentSnapshotRepository
import org.example.trip.personal.repository.PersonalTripEquipmentSuppressionRepository
import org.example.trip.personal.repository.PersonalTripOwnershipRepository
import org.example.trip.personal.repository.PersonalTripRepository
import org.example.trip.personal.repository.TripFrozenRouteVersionRepository
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.mockito.kotlin.any
import org.mockito.kotlin.argumentCaptor
import org.mockito.kotlin.eq
import org.mockito.kotlin.never
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest
import org.springframework.transaction.PlatformTransactionManager
import org.springframework.transaction.annotation.Propagation
import org.springframework.transaction.annotation.Transactional
import org.springframework.transaction.support.TransactionTemplate
import java.time.Clock
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.util.Optional
import java.util.concurrent.CountDownLatch
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit

class TripEquipmentApplicationServiceTest {
    private val tripRepository: PersonalTripRepository = org.mockito.kotlin.mock()
    private val ownershipRepository: PersonalTripOwnershipRepository = org.mockito.kotlin.mock()
    private val routeVersionRelationRepository: TripFrozenRouteVersionRepository = org.mockito.kotlin.mock()
    private val routeVersionRepository: RouteVersionRepository = org.mockito.kotlin.mock()
    private val snapshotRepository: PersonalTripEquipmentSnapshotRepository = org.mockito.kotlin.mock()
    private val itemRepository: PersonalTripEquipmentItemRepository = org.mockito.kotlin.mock()
    private val derivationRepository: PersonalTripEquipmentItemDerivationRepository = org.mockito.kotlin.mock()
    private val suppressionRepository: PersonalTripEquipmentSuppressionRepository = org.mockito.kotlin.mock()
    private val equipmentRepository: PersonalEquipmentRecordRepository = org.mockito.kotlin.mock()
    private val equipmentOwnershipRepository: PersonalEquipmentOwnershipRepository = org.mockito.kotlin.mock()
    private val clock = Clock.fixed(Instant.parse("2026-09-01T04:00:00Z"), ZoneId.of("UTC"))
    private val service = TripEquipmentApplicationService(
        tripRepository,
        ownershipRepository,
        routeVersionRelationRepository,
        routeVersionRepository,
        snapshotRepository,
        itemRepository,
        derivationRepository,
        suppressionRepository,
        equipmentRepository,
        equipmentOwnershipRepository,
        PersonalEquipmentDomainService(),
        PersonalTripDomainService(clock),
        ObjectMapper().findAndRegisterModules(),
        clock
    )

    private lateinit var trip: PersonalTripRecord
    private lateinit var snapshot: PersonalTripEquipmentSnapshotRecord

    @BeforeEach
    fun setUp() {
        trip = tripRecord()
        snapshot = snapshotRecord()
        whenever(ownershipRepository.findByTripIdAndAccountId("trip-1", "account-1"))
            .thenReturn(PersonalTripOwnership("trip-1", "account-1"))
        whenever(tripRepository.findById("trip-1")).thenReturn(Optional.of(trip))
        whenever(tripRepository.findByIdForUpdate("trip-1")).thenReturn(trip)
        whenever(routeVersionRelationRepository.findById("trip-1")).thenReturn(
            Optional.of(org.example.trip.personal.model.TripFrozenRouteVersion("trip-1", "version-1"))
        )
        whenever(routeVersionRepository.findById("version-1")).thenReturn(
            Optional.of(RouteVersion("version-1", "route-1", routeType = "one_day", mainTrackAvailability = "valid"))
        )
        whenever(snapshotRepository.findByTripId("trip-1")).thenReturn(snapshot)
        whenever(itemRepository.findBySnapshotIdOrderByDisplayOrderAsc("snapshot-1")).thenReturn(emptyList())
        whenever(itemRepository.existsBySnapshotIdAndNormalizedName(any(), any())).thenReturn(false)
        whenever(itemRepository.existsBySnapshotIdAndNormalizedNameAndIdNot(any(), any(), any())).thenReturn(false)
        whenever(derivationRepository.findByItemId(any())).thenReturn(null)
        whenever(suppressionRepository.findByTripIdAndLogicalSuggestionId(any(), any())).thenReturn(null)
        whenever(equipmentOwnershipRepository.findByAccountIdOrderByPersonalEquipmentIdAsc(any())).thenReturn(emptyList())
        whenever(tripRepository.save(any<PersonalTripRecord>())).thenAnswer { it.arguments[0] as PersonalTripRecord }
        whenever(snapshotRepository.save(any<PersonalTripEquipmentSnapshotRecord>())).thenAnswer { it.arguments[0] as PersonalTripEquipmentSnapshotRecord }
        whenever(itemRepository.save(any<PersonalTripEquipmentItemRecord>())).thenAnswer {
            (it.arguments[0] as PersonalTripEquipmentItemRecord).also { item -> savedItem = item }
        }
        whenever(itemRepository.saveAll(any<List<PersonalTripEquipmentItemRecord>>())).thenAnswer { it.arguments[0] }
        whenever(derivationRepository.save(any<PersonalTripEquipmentItemDerivedFromSuggestion>())).thenAnswer { it.arguments[0] }
        whenever(suppressionRepository.save(any<TripSuppressesEquipmentSuggestion>())).thenAnswer { it.arguments[0] }
    }

    @Test
    fun `get hides foreign trips and derives summary from current visible items`() {
        val items = listOf(
            item("item-2", 2, "Cook Set", "cook set", 1, null, "unconfirmed_owned"),
            item("item-1", 1, "Tent", "tent", 4, 250, "owned")
        )
        whenever(itemRepository.findBySnapshotIdOrderByDisplayOrderAsc("snapshot-1")).thenReturn(items)

        val result = service.getEquipment("account-1", "trip-1")

        assertEquals(listOf("item-1", "item-2"), result.snapshot.items.map { it.identity })
        assertEquals(2, result.snapshot.summary.itemCount)
        assertEquals(1000, result.snapshot.summary.knownTotalWeight.grams)
        assertEquals(1, result.snapshot.summary.missingWeightItemCount)
        assertEquals(1, result.snapshot.summary.ownedItemCount)
        assertEquals(1, result.snapshot.summary.unconfirmedOwnedItemCount)
        assertTrue(result.editable)

        whenever(ownershipRepository.findByTripIdAndAccountId("trip-1", "foreign-account")).thenReturn(null)
        val error = assertThrows(ApiContractException::class.java) {
            service.getEquipment("foreign-account", "trip-1")
        }
        assertEquals("resource_not_found", error.code)
    }

    @Test
    fun `trip mutation repository uses a pessimistic write lock`() {
        val method = PersonalTripRepository::class.java.getMethod("findByIdForUpdate", String::class.java)
        assertEquals(
            jakarta.persistence.LockModeType.PESSIMISTIC_WRITE,
            method.getAnnotation(org.springframework.data.jpa.repository.Lock::class.java).value
        )
    }

    @Test
    fun `create normalizes name initializes user added and increments revision once`() {
        whenever(itemRepository.findBySnapshotIdOrderByDisplayOrderAsc("snapshot-1"))
            .thenAnswer { listOfNotNull(savedItem) }

        val result = service.createItem(
            "account-1",
            "trip-1",
            CreateTripEquipmentItemCommand("  Tent   Bag  ", 2, 300, "  dry bag  ")
        )

        val saved = requireNotNull(savedItem)
        assertEquals("Tent Bag", saved.name)
        assertEquals("tent bag", saved.normalizedName)
        assertEquals("user_added", saved.source)
        assertEquals("unconfirmed_owned", saved.ownershipStatus)
        assertEquals("dry bag", saved.note)
        assertEquals(1, saved.displayOrder)
        assertEquals(saved.id, result.snapshot.items.single().identity)
        assertTrue(result.revision != "revision-1")
        assertEquals(result.revision, trip.revision)
        verify(tripRepository).save(trip)
        verify(snapshotRepository).save(snapshot)
        verify(equipmentRepository, never()).save(any())
    }

    @Test
    fun `create rejects normalized name conflicts and invalid domain values`() {
        whenever(itemRepository.existsBySnapshotIdAndNormalizedName("snapshot-1", "tent bag")).thenReturn(true)

        val conflict = assertThrows(ApiContractException::class.java) {
            service.createItem(
                "account-1",
                "trip-1",
                CreateTripEquipmentItemCommand(" Tent  Bag ", 1, null, null)
            )
        }
        assertEquals("equipment_name_conflict", conflict.code)

        val invalid = assertThrows(ApiContractException::class.java) {
            service.createItem(
                "account-1",
                "trip-1",
                CreateTripEquipmentItemCommand("帐篷", 0, null, null)
            )
        }
        assertEquals("validation_failed", invalid.code)
        verify(itemRepository, never()).save(any())
    }

    @Test
    fun `create accepts nonempty note without an undocumented application length limit`() {
        val note = "a".repeat(2_001)

        val result = service.createItem(
            "account-1",
            "trip-1",
            CreateTripEquipmentItemCommand("帐篷", 1, null, note)
        )

        assertEquals(note, requireNotNull(savedItem).note)
        assertEquals(note, result.snapshot.items.single().note)
    }

    @Test
    fun `update clears optional fields preserves identity and ownership and keeps derivation`() {
        val existing = item(
            id = "item-1",
            order = 1,
            name = "System Tent",
            normalized = "system tent",
            quantity = 1,
            weight = 500,
            ownership = "owned",
            source = "system_suggestion",
            note = "旧备注"
        )
        whenever(itemRepository.findByIdAndSnapshotId("item-1", "snapshot-1")).thenReturn(existing)
        whenever(itemRepository.findBySnapshotIdOrderByDisplayOrderAsc("snapshot-1"))
            .thenReturn(listOf(existing))
        whenever(derivationRepository.findByItemId("item-1")).thenReturn(
            PersonalTripEquipmentItemDerivedFromSuggestion("item-1", "trip-1", "logical-1", "occurrence-1")
        )

        val result = service.updateItem(
            "account-1",
            "trip-1",
            "item-1",
            UpdateTripEquipmentItemCommand(
                name = null,
                quantity = null,
                unitWeightGrams = null,
                note = null,
                hasName = false,
                hasQuantity = false,
                hasUnitWeight = true,
                hasNote = true
            )
        )

        assertEquals("item-1", existing.id)
        assertEquals("user_adjusted", existing.source)
        assertEquals("owned", existing.ownershipStatus)
        assertNull(existing.unitWeightGrams)
        assertNull(existing.note)
        assertEquals("logical-1", derivationRepository.findByItemId("item-1")?.logicalSuggestionId)
        assertEquals(1, result.snapshot.summary.missingWeightItemCount)
    }

    @Test
    fun `update rejects a normalized name conflict without changing the item`() {
        val existing = item("item-1", 1, "Tent", "tent", 1, 500, "owned")
        whenever(itemRepository.findByIdAndSnapshotId("item-1", "snapshot-1")).thenReturn(existing)
        whenever(itemRepository.existsBySnapshotIdAndNormalizedNameAndIdNot("snapshot-1", "tent bag", "item-1"))
            .thenReturn(true)

        val error = assertThrows(ApiContractException::class.java) {
            service.updateItem(
                "account-1",
                "trip-1",
                "item-1",
                UpdateTripEquipmentItemCommand(
                    name = " Tent  Bag ",
                    quantity = null,
                    unitWeightGrams = null,
                    note = null,
                    hasName = true,
                    hasQuantity = false,
                    hasUnitWeight = false,
                    hasNote = false
                )
            )
        }

        assertEquals("equipment_name_conflict", error.code)
        assertEquals("Tent", existing.name)
        verify(itemRepository, never()).save(any())
    }

    @Test
    fun `update hides items outside the current snapshot`() {
        whenever(itemRepository.findByIdAndSnapshotId("old-item", "snapshot-1")).thenReturn(null)

        val error = assertThrows(ApiContractException::class.java) {
            service.updateItem(
                "account-1",
                "trip-1",
                "old-item",
                UpdateTripEquipmentItemCommand(
                    name = "新名称",
                    quantity = null,
                    unitWeightGrams = null,
                    note = null,
                    hasName = true,
                    hasQuantity = false,
                    hasUnitWeight = false,
                    hasNote = false
                )
            )
        }

        assertEquals("resource_not_found", error.code)
        verify(itemRepository, never()).save(any())
    }

    @Test
    fun `delete suggestion requires valid derivation before any mutation`() {
        val existing = item("item-1", 1, "Tent", "tent", 1, 500, "owned", "system_suggestion")
        whenever(itemRepository.findByIdAndSnapshotId("item-1", "snapshot-1")).thenReturn(existing)

        val error = assertThrows(ApiContractException::class.java) {
            service.deleteItem("account-1", "trip-1", "item-1")
        }

        assertEquals("relationship_inconsistent", error.code)
        verify(suppressionRepository, never()).save(any())
        verify(itemRepository, never()).delete(any())
        verify(tripRepository, never()).save(any())
    }

    @Test
    fun `delete suggestion preserves suppression before removing item and derivation`() {
        val existing = item("item-1", 1, "Tent", "tent", 1, 500, "owned", "user_adjusted")
        val relation = PersonalTripEquipmentItemDerivedFromSuggestion(
            itemId = "item-1",
            tripId = "trip-1",
            logicalSuggestionId = "logical-1",
            suggestionOccurrenceId = null
        )
        whenever(itemRepository.findByIdAndSnapshotId("item-1", "snapshot-1")).thenReturn(existing)
        whenever(derivationRepository.findByItemId("item-1")).thenReturn(relation)
        whenever(itemRepository.findBySnapshotIdOrderByDisplayOrderAsc("snapshot-1")).thenReturn(emptyList())

        val result = service.deleteItem("account-1", "trip-1", "item-1")

        verify(suppressionRepository).save(TripSuppressesEquipmentSuggestion("trip-1", "logical-1", "route-1"))
        verify(derivationRepository).delete(relation)
        verify(itemRepository).delete(existing)
        assertTrue(result.snapshot.items.isEmpty())
        assertEquals(0, result.snapshot.summary.itemCount)
        assertTrue(result.revision != "revision-1")
    }

    @Test
    fun `delete user added item does not create suppression`() {
        val existing = item("item-1", 1, "Bottle", "bottle", 1, null, "unconfirmed_owned")
        whenever(itemRepository.findByIdAndSnapshotId("item-1", "snapshot-1")).thenReturn(existing)
        whenever(itemRepository.findBySnapshotIdOrderByDisplayOrderAsc("snapshot-1")).thenReturn(emptyList())

        service.deleteItem("account-1", "trip-1", "item-1")

        verify(suppressionRepository, never()).save(any())
        verify(derivationRepository, never()).delete(any())
        verify(itemRepository).delete(existing)
    }

    @Test
    fun `recheck uses all account equipment and changes only ownership`() {
        val owned = item("item-1", 1, " Tent  Bag ", "tent bag", 2, 300, "unconfirmed_owned", note = "保留")
        val short = item("item-2", 2, "Gloves", "gloves", 2, null, "owned", source = "user_adjusted")
        whenever(itemRepository.findBySnapshotIdOrderByDisplayOrderAsc("snapshot-1"))
            .thenReturn(listOf(owned, short))
        whenever(derivationRepository.findByItemId("item-2")).thenReturn(
            PersonalTripEquipmentItemDerivedFromSuggestion("item-2", "trip-1", "logical-2")
        )
        whenever(equipmentOwnershipRepository.findByAccountIdOrderByPersonalEquipmentIdAsc("account-1"))
            .thenReturn(
                listOf(
                    PersonalEquipmentOwnership("equipment-1", "account-1", "tent bag"),
                    PersonalEquipmentOwnership("equipment-2", "account-1", "gloves")
                )
            )
        whenever(equipmentRepository.findById("equipment-1"))
            .thenReturn(Optional.of(PersonalEquipmentRecord("equipment-1", "Tent Bag", 2, 200)))
        whenever(equipmentRepository.findById("equipment-2"))
            .thenReturn(Optional.of(PersonalEquipmentRecord("equipment-2", "Gloves", 1, 50)))

        val result = service.recheckOwnership("account-1", "trip-1")

        assertEquals("owned", owned.ownershipStatus)
        assertEquals("unconfirmed_owned", short.ownershipStatus)
        assertEquals(" Tent  Bag ", owned.name)
        assertEquals(300, owned.unitWeightGrams)
        assertEquals("保留", owned.note)
        assertEquals("user_added", owned.source)
        assertEquals(1, result.snapshot.summary.ownedItemCount)
        assertEquals(1, result.snapshot.summary.unconfirmedOwnedItemCount)
        assertTrue(result.revision != "revision-1")
    }

    @Test
    fun `mutations reject every non planned state without writes`() {
        trip.lifecycleState = "cancelled"

        val error = assertThrows(ApiContractException::class.java) {
            service.recheckOwnership("account-1", "trip-1")
        }

        assertEquals("trip_equipment_read_only", error.code)
        verify(itemRepository, never()).saveAll(any<List<PersonalTripEquipmentItemRecord>>())
        verify(tripRepository, never()).save(any())
    }

    private var savedItem: PersonalTripEquipmentItemRecord? = null

    private fun tripRecord() = PersonalTripRecord(
        id = "trip-1",
        name = "贡嘎行程",
        firstGeneratedAt = Instant.parse("2026-09-01T04:00:00Z"),
        departureCity = "成都",
        startDate = LocalDate.parse("2026-09-02"),
        endDate = LocalDate.parse("2026-09-03"),
        totalDayCount = 2,
        hikingDayCount = 2,
        revision = "revision-1",
        frozenRouteBasisJson = ObjectMapper().findAndRegisterModules().writeValueAsString(
            FrozenRouteBasisProjection(
                routeName = "贡嘎环线",
                routeType = "multi_day",
                region = "四川甘孜",
                start = org.example.route.dto.PublicRoutePlace("起点"),
                end = org.example.route.dto.PublicRoutePlace("终点"),
                estimatedDuration = org.example.route.dto.RouteSeconds(172_800.0),
                mainTrackPath = emptyList()
            )
        )
    )

    private fun snapshotRecord() = PersonalTripEquipmentSnapshotRecord(
        id = "snapshot-1",
        tripId = "trip-1",
        itemCount = 99,
        knownTotalWeightGrams = 99,
        missingWeightItemCount = 99,
        ownedItemCount = 99,
        unconfirmedOwnedItemCount = 0
    )

    private fun item(
        id: String,
        order: Int,
        name: String,
        normalized: String,
        quantity: Int,
        weight: Long?,
        ownership: String,
        source: String = "user_added",
        note: String? = null
    ) = PersonalTripEquipmentItemRecord(
        id = id,
        snapshotId = "snapshot-1",
        displayOrder = order,
        name = name,
        normalizedName = normalized,
        quantity = quantity,
        unitWeightGrams = weight,
        source = source,
        ownershipStatus = ownership,
        note = note
    )
}

@DataJpaTest(
    properties = [
        "spring.flyway.enabled=false",
        "spring.jpa.hibernate.ddl-auto=create-drop",
        "spring.jpa.properties.hibernate.dialect=org.hibernate.dialect.H2Dialect",
        "spring.datasource.url=jdbc:h2:mem:trip-revision-concurrency;MODE=MySQL;DB_CLOSE_DELAY=-1;LOCK_TIMEOUT=10000"
    ]
)
@Transactional(propagation = Propagation.NOT_SUPPORTED)
class PersonalTripRevisionConcurrencyIntegrationTest {
    @Autowired
    private lateinit var tripRepository: PersonalTripRepository

    @Autowired
    private lateinit var transactionManager: PlatformTransactionManager

    @BeforeEach
    fun seedTrip() {
        tripRepository.deleteAll()
        tripRepository.saveAndFlush(
            PersonalTripRecord(
                id = "concurrent-trip",
                name = "并发行程",
                firstGeneratedAt = Instant.parse("2026-09-01T04:00:00Z"),
                departureCity = "成都",
                startDate = LocalDate.parse("2026-09-02"),
                endDate = LocalDate.parse("2026-09-03"),
                totalDayCount = 2,
                hikingDayCount = 2,
                revision = "shared-revision",
                frozenRouteBasisJson = "{}"
            )
        )
    }

    @Test
    fun `two commands with the same expected revision cannot both succeed`() {
        val ready = CountDownLatch(2)
        val start = CountDownLatch(1)
        val executor = Executors.newFixedThreadPool(2)
        try {
            val attempts = (1..2).map { attempt ->
                executor.submit<Boolean> {
                    ready.countDown()
                    start.await(5, TimeUnit.SECONDS)
                    TransactionTemplate(transactionManager).execute {
                        val trip = requireNotNull(tripRepository.findByIdForUpdate("concurrent-trip"))
                        if (trip.revision != "shared-revision") {
                            false
                        } else {
                            trip.revision = "revision-$attempt"
                            tripRepository.saveAndFlush(trip)
                            true
                        }
                    } ?: false
                }
            }
            assertTrue(ready.await(5, TimeUnit.SECONDS))
            start.countDown()

            assertEquals(1, attempts.count { it.get(10, TimeUnit.SECONDS) })
            assertTrue(requireNotNull(tripRepository.findById("concurrent-trip").orElse(null)).revision != "shared-revision")
        } finally {
            executor.shutdownNow()
        }
    }
}
