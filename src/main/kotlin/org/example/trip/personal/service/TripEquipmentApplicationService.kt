package org.example.trip.personal.service

import com.fasterxml.jackson.databind.ObjectMapper
import org.example.common.contract.ApiContractException
import org.example.common.util.IdGenerator
import org.example.equipment.dto.WeightProjection
import org.example.equipment.model.PersonalEquipmentRecord
import org.example.equipment.repository.PersonalEquipmentOwnershipRepository
import org.example.equipment.repository.PersonalEquipmentRecordRepository
import org.example.equipment.service.PersonalEquipmentDomainService
import org.example.trip.personal.dto.CreateTripEquipmentItemCommand
import org.example.trip.personal.dto.FrozenRouteBasisProjection
import org.example.trip.personal.dto.TripEquipmentItemProjection
import org.example.trip.personal.dto.TripEquipmentProjection
import org.example.trip.personal.dto.TripEquipmentSnapshotProjection
import org.example.trip.personal.dto.TripEquipmentSummaryProjection
import org.example.trip.personal.dto.TripEquipmentTripProjection
import org.example.trip.personal.dto.UpdateTripEquipmentItemCommand
import org.example.trip.personal.model.PersonalTripEquipmentItemRecord
import org.example.trip.personal.model.PersonalTripEquipmentSnapshotRecord
import org.example.trip.personal.model.PersonalTripRecord
import org.example.trip.personal.model.TripSuppressesEquipmentSuggestion
import org.example.trip.personal.repository.PersonalTripEquipmentItemDerivationRepository
import org.example.trip.personal.repository.TripFrozenRouteVersionRepository
import org.example.route.repository.RouteVersionRepository
import org.example.trip.personal.repository.PersonalTripEquipmentItemRepository
import org.example.trip.personal.repository.PersonalTripEquipmentSnapshotRepository
import org.example.trip.personal.repository.PersonalTripEquipmentSuppressionRepository
import org.example.trip.personal.repository.PersonalTripOwnershipRepository
import org.example.trip.personal.repository.PersonalTripRepository
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.time.Clock

@Service
class TripEquipmentApplicationService(
    private val tripRepository: PersonalTripRepository,
    private val ownershipRepository: PersonalTripOwnershipRepository,
    private val routeVersionRelationRepository: TripFrozenRouteVersionRepository,
    private val routeVersionRepository: RouteVersionRepository,
    private val snapshotRepository: PersonalTripEquipmentSnapshotRepository,
    private val itemRepository: PersonalTripEquipmentItemRepository,
    private val derivationRepository: PersonalTripEquipmentItemDerivationRepository,
    private val suppressionRepository: PersonalTripEquipmentSuppressionRepository,
    private val equipmentRepository: PersonalEquipmentRecordRepository,
    private val equipmentOwnershipRepository: PersonalEquipmentOwnershipRepository,
    private val equipmentDomainService: PersonalEquipmentDomainService,
    private val tripDomainService: PersonalTripDomainService,
    private val objectMapper: ObjectMapper,
    private val clock: Clock = Clock.systemUTC()
) {
    @Transactional(readOnly = true)
    fun getEquipment(accountId: String, tripId: String): TripEquipmentProjection {
        val aggregate = requireAggregate(accountId, tripId)
        return projection(aggregate.trip, aggregate.snapshot, aggregate.items)
    }

    @Transactional
    fun createItem(
        accountId: String,
        tripId: String,
        command: CreateTripEquipmentItemCommand
    ): TripEquipmentProjection {
        val aggregate = requireEditableAggregate(accountId, tripId)
        val normalizedName = equipmentDomainService.normalizeEquipmentName(command.name)
        val quantity = equipmentDomainService.requirePositiveQuantity(command.quantity)
        val unitWeight = command.unitWeightGrams?.let(equipmentDomainService::requirePositiveWeight)
        val note = normalizeOptionalNote(command.note)
        if (itemRepository.existsBySnapshotIdAndNormalizedName(aggregate.snapshot.id, normalizedName.comparison)) {
            throw equipmentNameConflict()
        }
        val nextOrder = (aggregate.items.maxOfOrNull { it.displayOrder } ?: 0) + 1
        val item = PersonalTripEquipmentItemRecord(
            id = IdGenerator.generateIdWithPrefix("tei"),
            snapshotId = aggregate.snapshot.id,
            displayOrder = nextOrder,
            name = normalizedName.display,
            normalizedName = normalizedName.comparison,
            quantity = quantity,
            unitWeightGrams = unitWeight,
            source = "user_added",
            ownershipStatus = "unconfirmed_owned",
            note = note
        )
        itemRepository.save(item)
        aggregate.items += item
        return completeMutation(aggregate, aggregate.items)
    }

    @Transactional
    fun updateItem(
        accountId: String,
        tripId: String,
        itemId: String,
        command: UpdateTripEquipmentItemCommand
    ): TripEquipmentProjection {
        val aggregate = requireEditableAggregate(accountId, tripId)
        val item = requireCurrentItem(aggregate.snapshot.id, itemId)
        if (command.hasName) {
            val normalizedName = equipmentDomainService.normalizeEquipmentName(command.name)
            if (itemRepository.existsBySnapshotIdAndNormalizedNameAndIdNot(
                    aggregate.snapshot.id,
                    normalizedName.comparison,
                    item.id
                )
            ) {
                throw equipmentNameConflict()
            }
            item.name = normalizedName.display
            item.normalizedName = normalizedName.comparison
        }
        if (command.hasQuantity) {
            item.quantity = equipmentDomainService.requirePositiveQuantity(command.quantity)
        }
        if (command.hasUnitWeight) {
            item.unitWeightGrams = command.unitWeightGrams?.let(equipmentDomainService::requirePositiveWeight)
        }
        if (command.hasNote) {
            item.note = command.note?.let(::requireNote)
        }
        when (item.source) {
            "system_suggestion" -> {
                requireValidDerivation(tripId, item)
                item.source = "user_adjusted"
            }
            "user_adjusted" -> requireValidDerivation(tripId, item)
            "user_added" -> {
                if (derivationRepository.findByItemId(item.id) != null) throw relationshipInconsistent()
            }
            else -> throw relationshipInconsistent()
        }
        itemRepository.save(item)
        replaceItem(aggregate.items, item)
        return completeMutation(aggregate, aggregate.items)
    }

    @Transactional
    fun deleteItem(accountId: String, tripId: String, itemId: String): TripEquipmentProjection {
        val aggregate = requireEditableAggregate(accountId, tripId)
        val item = requireCurrentItem(aggregate.snapshot.id, itemId)
        when (item.source) {
            "user_added" -> {
                if (derivationRepository.findByItemId(item.id) != null) throw relationshipInconsistent()
            }
            "system_suggestion", "user_adjusted" -> {
                val relation = requireValidDerivation(tripId, item)
                if (suppressionRepository.findByTripIdAndLogicalSuggestionId(
                        tripId,
                        relation.logicalSuggestionId
                    ) == null
                ) {
                    suppressionRepository.save(
                        TripSuppressesEquipmentSuggestion(
                            tripId = tripId,
                            logicalSuggestionId = relation.logicalSuggestionId,
                            routeId = requireRouteId(tripId)
                        )
                    )
                }
                derivationRepository.delete(relation)
            }
            else -> throw relationshipInconsistent()
        }
        itemRepository.delete(item)
        aggregate.items.removeAll { it.id == item.id }
        return completeMutation(aggregate, aggregate.items)
    }

    @Transactional
    fun recheckOwnership(accountId: String, tripId: String): TripEquipmentProjection {
        val aggregate = requireEditableAggregate(accountId, tripId)
        val ownedByName = ownedEquipmentByNormalizedName(accountId)
        aggregate.items.forEach { item ->
            item.ownershipStatus = if ((ownedByName[item.normalizedName]?.ownedQuantity ?: 0) >= item.quantity) {
                "owned"
            } else {
                "unconfirmed_owned"
            }
        }
        itemRepository.saveAll(aggregate.items)
        return completeMutation(aggregate, aggregate.items)
    }

    private fun requireAggregate(accountId: String, tripId: String): EquipmentAggregate {
        ownershipRepository.findByTripIdAndAccountId(tripId, accountId) ?: throw ApiContractException.notFound()
        val trip = tripRepository.findById(tripId).orElseThrow { ApiContractException.notFound() }
        return loadAggregate(trip)
    }

    private fun requireEditableAggregate(accountId: String, tripId: String): EquipmentAggregate {
        val trip = tripRepository.findByIdForUpdate(tripId) ?: throw ApiContractException.notFound()
        ownershipRepository.findByTripIdAndAccountId(tripId, accountId) ?: throw ApiContractException.notFound()
        val aggregate = loadAggregate(trip)
        if (tripDomainService.status(aggregate.trip) != "planned") {
            throw ApiContractException.conflict("trip_equipment_read_only", "当前行程装备只读")
        }
        return aggregate
    }

    private fun loadAggregate(trip: PersonalTripRecord): EquipmentAggregate {
        val snapshot = snapshotRepository.findByTripId(trip.id) ?: throw equipmentUnavailable()
        val items = itemRepository.findBySnapshotIdOrderByDisplayOrderAsc(snapshot.id).toMutableList()
        validateVisibleItems(trip.id, snapshot.id, items)
        return EquipmentAggregate(trip, snapshot, items)
    }

    private fun requireCurrentItem(snapshotId: String, itemId: String): PersonalTripEquipmentItemRecord =
        itemRepository.findByIdAndSnapshotId(itemId, snapshotId) ?: throw ApiContractException.notFound()

    private fun requireRouteId(tripId: String): String {
        val relation = routeVersionRelationRepository.findById(tripId).orElseThrow(::relationshipInconsistent)
        return routeVersionRepository.findById(relation.routeVersionId)
            .orElseThrow(::relationshipInconsistent)
            .routeId
    }

    private fun requireValidDerivation(tripId: String, item: PersonalTripEquipmentItemRecord) =
        derivationRepository.findByItemId(item.id)?.takeIf { it.tripId == tripId }
            ?: throw relationshipInconsistent()

    private fun validateVisibleItems(
        tripId: String,
        snapshotId: String,
        items: List<PersonalTripEquipmentItemRecord>
    ) {
        val normalizedNames = mutableSetOf<String>()
        val logicalSuggestionIds = mutableSetOf<String>()
        items.forEach { item ->
            val normalized = equipmentDomainService.normalizeEquipmentName(item.name)
            if (item.snapshotId != snapshotId || item.normalizedName != normalized.comparison ||
                !normalizedNames.add(item.normalizedName) || item.quantity <= 0 ||
                item.unitWeightGrams?.let { it <= 0 } == true || item.note?.isBlank() == true
            ) {
                throw equipmentUnavailable()
            }
            val relation = derivationRepository.findByItemId(item.id)
            when (item.source) {
                "user_added" -> if (relation != null) throw equipmentUnavailable()
                "system_suggestion", "user_adjusted" -> {
                    if (relation == null || relation.tripId != tripId ||
                        !logicalSuggestionIds.add(relation.logicalSuggestionId)
                    ) {
                        throw equipmentUnavailable()
                    }
                }
                else -> throw equipmentUnavailable()
            }
            if (item.ownershipStatus !in OWNERSHIP_STATUSES) throw equipmentUnavailable()
        }
    }

    private fun completeMutation(
        aggregate: EquipmentAggregate,
        currentItems: List<PersonalTripEquipmentItemRecord>
    ): TripEquipmentProjection {
        val items = currentItems.sortedBy { it.displayOrder }
        validateVisibleItems(aggregate.trip.id, aggregate.snapshot.id, items)
        synchronizeSummary(aggregate.snapshot, items)
        snapshotRepository.save(aggregate.snapshot)
        aggregate.trip.revision = newRevision()
        aggregate.trip.updatedAt = clock.instant()
        tripRepository.save(aggregate.trip)
        return projection(aggregate.trip, aggregate.snapshot, items)
    }

    private fun projection(
        trip: PersonalTripRecord,
        snapshot: PersonalTripEquipmentSnapshotRecord,
        items: List<PersonalTripEquipmentItemRecord>
    ): TripEquipmentProjection {
        val summary = summary(items)
        val basis = try {
            objectMapper.readValue(trip.frozenRouteBasisJson, FrozenRouteBasisProjection::class.java)
        } catch (_: Exception) {
            throw equipmentUnavailable()
        }
        val status = tripDomainService.status(trip)
        return TripEquipmentProjection(
            trip = TripEquipmentTripProjection(
                identity = trip.id,
                status = status,
                routeName = basis.routeName,
                startDate = trip.startDate,
                endDate = trip.endDate
            ),
            revision = trip.revision,
            editable = status == "planned",
            snapshot = TripEquipmentSnapshotProjection(
                identity = snapshot.id,
                items = items.sortedBy { it.displayOrder }.map { item ->
                    TripEquipmentItemProjection(
                        identity = item.id,
                        name = item.name,
                        quantity = item.quantity,
                        unitWeight = item.unitWeightGrams?.let(::WeightProjection),
                        note = item.note,
                        source = item.source,
                        ownershipStatus = item.ownershipStatus
                    )
                },
                summary = summary
            )
        )
    }

    private fun synchronizeSummary(
        snapshot: PersonalTripEquipmentSnapshotRecord,
        items: List<PersonalTripEquipmentItemRecord>
    ) {
        val summary = summary(items)
        snapshot.itemCount = summary.itemCount
        snapshot.knownTotalWeightGrams = summary.knownTotalWeight.grams
        snapshot.missingWeightItemCount = summary.missingWeightItemCount
        snapshot.ownedItemCount = summary.ownedItemCount
        snapshot.unconfirmedOwnedItemCount = summary.unconfirmedOwnedItemCount
    }

    private fun summary(items: List<PersonalTripEquipmentItemRecord>) = TripEquipmentSummaryProjection(
        itemCount = items.size,
        knownTotalWeight = WeightProjection(items.sumOf { (it.unitWeightGrams ?: 0L) * it.quantity.toLong() }),
        missingWeightItemCount = items.count { it.unitWeightGrams == null },
        ownedItemCount = items.count { it.ownershipStatus == "owned" },
        unconfirmedOwnedItemCount = items.count { it.ownershipStatus == "unconfirmed_owned" }
    )

    private fun ownedEquipmentByNormalizedName(accountId: String): Map<String, PersonalEquipmentRecord> =
        equipmentOwnershipRepository.findByAccountIdOrderByPersonalEquipmentIdAsc(accountId).associate { ownership ->
            ownership.normalizedName to equipmentRepository.findById(ownership.personalEquipmentId)
                .orElseThrow(::equipmentUnavailable)
        }

    private fun normalizeOptionalNote(note: String?): String? = note?.let(::requireNote)

    private fun requireNote(note: String): String {
        val normalized = note.trim()
        if (normalized.isEmpty()) {
            throw ApiContractException.unprocessable("validation_failed", "备注不能为空")
        }
        return normalized
    }

    private fun replaceItem(items: MutableList<PersonalTripEquipmentItemRecord>, item: PersonalTripEquipmentItemRecord) {
        val index = items.indexOfFirst { it.id == item.id }
        if (index >= 0) items[index] = item else items += item
    }

    private fun newRevision(): String = IdGenerator.generateIdWithPrefix("rev")

    private fun equipmentNameConflict() =
        ApiContractException.conflict("equipment_name_conflict", "当前快照已存在同名装备")

    private fun relationshipInconsistent() =
        ApiContractException.conflict("relationship_inconsistent", "装备建议关系不一致")

    private fun equipmentUnavailable() =
        ApiContractException.serviceUnavailable("trip_equipment_unavailable", "本次装备暂时无法读取")

    private data class EquipmentAggregate(
        val trip: PersonalTripRecord,
        val snapshot: PersonalTripEquipmentSnapshotRecord,
        val items: MutableList<PersonalTripEquipmentItemRecord>
    )

    private companion object {
        val OWNERSHIP_STATUSES = setOf("owned", "unconfirmed_owned")
    }
}
