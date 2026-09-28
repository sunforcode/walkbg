package org.example.trip.personal.service

import com.fasterxml.jackson.databind.ObjectMapper
import org.example.common.contract.ApiContractException
import org.example.common.util.IdGenerator
import org.example.equipment.dto.WeightProjection
import org.example.equipment.model.PersonalEquipmentRecord
import org.example.equipment.repository.PersonalEquipmentOwnershipRepository
import org.example.equipment.repository.PersonalEquipmentRecordRepository
import org.example.equipment.service.PersonalEquipmentDomainService
import org.example.trip.personal.dto.*
import org.example.equipment.repository.EquipmentListOwnershipRepository
import org.example.equipment.repository.EquipmentListMemberRepository
import org.example.equipment.repository.UserEquipmentListRecordRepository
import org.example.equipment.support.EquipmentCommandStore
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
    private val commandStore: EquipmentCommandStore,
    private val listRepository: UserEquipmentListRecordRepository,
    private val listOwnershipRepository: EquipmentListOwnershipRepository,
    private val memberRepository: EquipmentListMemberRepository,
    private val clock: Clock = Clock.systemUTC()
) {
    @Transactional(readOnly = true)
    fun getEquipment(accountId: String, tripId: String): TripEquipmentProjection {
        val aggregate = requireAggregate(accountId, tripId)
        return try { projection(aggregate.trip, aggregate.snapshot, aggregate.items) }
        catch (_: ApiContractException) { throw equipmentUnavailable() }
    }

    @Transactional
    fun createItem(
        accountId: String,
        tripId: String,
        command: CreateTripEquipmentItemCommand,
        expectedRevision: String? = null
    ): TripEquipmentProjection {
        val aggregate = requireEditableAggregate(accountId, tripId, expectedRevision)
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
        TripPreparationRules.apply(item, command.configuration)
        TripPreparationRules.validate(item)
        itemRepository.save(item)
        aggregate.items += item
        return completeMutation(aggregate, aggregate.items)
    }

    @Transactional
    fun updateItem(
        accountId: String,
        tripId: String,
        itemId: String,
        command: UpdateTripEquipmentItemCommand,
        expectedRevision: String? = null
    ): TripEquipmentProjection {
        val aggregate = requireEditableAggregate(accountId, tripId, expectedRevision)
        val original = requireCurrentItem(aggregate.snapshot.id, itemId)
        val item = original.copy()
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
        val oldConfig = TripPreparationRules.configuration(original)
        val incoming = command.configuration
        val fields = command.configurationFields
        val newConfig = TripItemConfiguration(
            if ("category" in fields) incoming.category else oldConfig.category,
            if ("equipmentType" in fields) incoming.equipmentType else oldConfig.equipmentType,
            if ("variant" in fields) incoming.variant else oldConfig.variant,
            if ("unitLabel" in fields) incoming.unitLabel else oldConfig.unitLabel,
            if ("weightScope" in fields) incoming.weightScope else oldConfig.weightScope,
            if ("carryAllocation" in fields) incoming.carryAllocation else oldConfig.carryAllocation,
            if ("supply" in fields) incoming.supply else oldConfig.supply
        )
        if ((newConfig.unitLabel != oldConfig.unitLabel || newConfig.supply != oldConfig.supply) && !command.hasUnitWeight) {
            throw ApiContractException.unprocessable("validation_failed", "改变计数单位或补给配置必须明确新重量或清除重量")
        }
        TripPreparationRules.apply(item, newConfig)
        if (original.name != item.name || original.quantity != item.quantity ||
            original.unitWeightGrams != item.unitWeightGrams || TripPreparationRules.configuration(item) != oldConfig) {
            item.packedQuantity = 0
        }
        if (item.supplyKind != null) item.ownershipStatus = "unconfirmed_owned"
        TripPreparationRules.validate(item)
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
    fun deleteItem(accountId: String, tripId: String, itemId: String, expectedRevision: String? = null): TripEquipmentProjection {
        val aggregate = requireEditableAggregate(accountId, tripId, expectedRevision)
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
    fun recheckOwnership(accountId: String, tripId: String, expectedRevision: String? = null): TripEquipmentProjection {
        val aggregate = requireEditableAggregate(accountId, tripId, expectedRevision)
        val ownedByName = ownedEquipmentByNormalizedName(accountId)
        aggregate.items.forEach { item ->
            item.ownershipStatus = if (item.supplyKind == null && (ownedByName[item.normalizedName]?.ownedQuantity ?: 0) >= item.quantity) {
                "owned"
            } else {
                "unconfirmed_owned"
            }
        }
        itemRepository.saveAll(aggregate.items)
        return completeMutation(aggregate, aggregate.items)
    }

    @Transactional(readOnly = true)
    fun preparation(accountId: String, tripId: String): TripPreparationProjection {
        val aggregate = requireAggregate(accountId, tripId)
        return try {
            TripPreparationRules.preparation(tripId, aggregate.trip.revision, aggregate.items)
        } catch (_: ApiContractException) { throw equipmentUnavailable() }
    }

    @Transactional
    fun fromPersonal(accountId: String, tripId: String, command: SelectPersonalEquipmentCommand,
                     expectedRevision: String, idempotencyKey: String): TripEquipmentProjection {
        commandStore.lockAccount(accountId)
        requireOwnedTripOnly(accountId, tripId)
        return commandStore.execute(accountId, "POST:/api/v1/trips/$tripId/equipment/items:from-personal", idempotencyKey,
            mapOf("body" to command, "ifMatch" to expectedRevision), TripEquipmentProjection::class.java) {
            val aggregate = requireEditableAggregate(accountId, tripId, expectedRevision)
            val item = personalItem(accountId, aggregate, command, nextOrder(aggregate), ownedEquipmentByNormalizedName(accountId))
            requireUniqueNames(aggregate.items + item)
            itemRepository.save(item)
            aggregate.items += item
            completeMutation(aggregate, aggregate.items)
        }
    }

    @Transactional
    fun appendList(accountId: String, tripId: String, command: AppendEquipmentListCommand,
                   expectedRevision: String, idempotencyKey: String): TripEquipmentProjection {
        commandStore.lockAccount(accountId)
        requireOwnedTripOnly(accountId, tripId)
        return commandStore.execute(accountId, "POST:/api/v1/trips/$tripId/equipment:append-list", idempotencyKey,
            mapOf("body" to command, "ifMatch" to expectedRevision), TripEquipmentProjection::class.java) {
            val aggregate = requireEditableAggregate(accountId, tripId, expectedRevision)
            listOwnershipRepository.findByEquipmentListIdAndAccountId(command.equipmentListId, accountId)
                ?: throw ApiContractException.notFound()
            listRepository.findById(command.equipmentListId).orElseThrow { ApiContractException.notFound() }
            val owned = ownedEquipmentByNormalizedName(accountId)
            val firstOrder = nextOrder(aggregate)
            val candidates = memberRepository.findByEquipmentListIdOrderByPersonalEquipmentIdAsc(command.equipmentListId)
                .sortedBy { it.personalEquipmentId }.mapIndexed { index, member ->
                    personalItem(accountId, aggregate, SelectPersonalEquipmentCommand(member.personalEquipmentId, member.quantity), firstOrder + index, owned)
                }
            requireUniqueNames(aggregate.items + candidates)
            itemRepository.saveAll(candidates)
            aggregate.items += candidates
            completeMutation(aggregate, aggregate.items)
        }
    }

    @Transactional
    fun replacePersonal(accountId: String, tripId: String, itemId: String,
                        command: SelectPersonalEquipmentCommand, expectedRevision: String): TripEquipmentProjection {
        val aggregate = requireEditableAggregate(accountId, tripId, expectedRevision)
        val previous = requireCurrentItem(aggregate.snapshot.id, itemId)
        val copied = personalItem(accountId, aggregate, command, previous.displayOrder, ownedEquipmentByNormalizedName(accountId))
        val replacement = copied.copy(id = previous.id, note = previous.note, source = previous.source, packedQuantity = 0)
        when (previous.source) {
            "system_suggestion", "user_adjusted" -> {
                requireValidDerivation(tripId, previous)
                replacement.source = "user_adjusted"
            }
            "user_added" -> if (derivationRepository.findByItemId(previous.id) != null) throw relationshipInconsistent()
            else -> throw relationshipInconsistent()
        }
        requireUniqueNames(aggregate.items.filter { it.id != itemId } + replacement)
        itemRepository.save(replacement)
        replaceItem(aggregate.items, replacement)
        return completeMutation(aggregate, aggregate.items)
    }

    @Transactional
    fun setPacking(accountId: String, tripId: String, itemId: String, packedQuantity: Int,
                   expectedRevision: String): TripEquipmentProjection {
        val aggregate = requireEditableAggregate(accountId, tripId, expectedRevision)
        val item = requireCurrentItem(aggregate.snapshot.id, itemId).copy(packedQuantity = packedQuantity)
        TripPreparationRules.validate(item)
        itemRepository.save(item)
        replaceItem(aggregate.items, item)
        return completeMutation(aggregate, aggregate.items)
    }

    @Transactional
    fun resetPacking(accountId: String, tripId: String, expectedRevision: String): TripEquipmentProjection {
        val aggregate = requireEditableAggregate(accountId, tripId, expectedRevision)
        aggregate.items.forEach { it.packedQuantity = 0 }
        itemRepository.saveAll(aggregate.items)
        return completeMutation(aggregate, aggregate.items)
    }

    private fun requireOwnedTripOnly(accountId: String, tripId: String) {
        ownershipRepository.findByTripIdAndAccountId(tripId, accountId) ?: throw ApiContractException.notFound()
        if (!tripRepository.existsById(tripId)) throw ApiContractException.notFound()
    }

    private fun personalItem(accountId: String, aggregate: EquipmentAggregate, command: SelectPersonalEquipmentCommand,
                             order: Int, owned: Map<String, PersonalEquipmentRecord>): PersonalTripEquipmentItemRecord {
        equipmentOwnershipRepository.findByPersonalEquipmentIdAndAccountId(command.personalEquipmentId, accountId)
            ?: throw ApiContractException.notFound()
        val personal = equipmentRepository.findById(command.personalEquipmentId).orElseThrow { ApiContractException.notFound() }
        if (personal.category in TripPreparationRules.supplyKinds) {
            throw ApiContractException.unprocessable("validation_failed", "请在本次装备中填写实际补给量")
        }
        val name = equipmentDomainService.normalizeEquipmentName(personal.name)
        val quantity = equipmentDomainService.requirePositiveQuantity(command.quantity)
        val item = PersonalTripEquipmentItemRecord(
            id = IdGenerator.generateIdWithPrefix("tei"), snapshotId = aggregate.snapshot.id, displayOrder = order,
            name = name.display, normalizedName = name.comparison, quantity = quantity,
            unitWeightGrams = personal.unitWeightGrams?.let(equipmentDomainService::requirePositiveWeight),
            source = "user_added",
            ownershipStatus = if ((owned[name.comparison]?.ownedQuantity ?: 0) >= quantity) "owned" else "unconfirmed_owned"
        )
        TripPreparationRules.apply(item, TripItemConfiguration(personal.category, personal.equipmentType, personal.variant, personal.unitLabel, personal.weightScope))
        TripPreparationRules.validate(item)
        return item
    }

    private fun nextOrder(aggregate: EquipmentAggregate) = (aggregate.items.maxOfOrNull { it.displayOrder } ?: 0) + 1

    private fun requireUniqueNames(items: List<PersonalTripEquipmentItemRecord>) {
        if (items.map { it.normalizedName }.toSet().size != items.size) throw equipmentNameConflict()
    }

    private fun requireAggregate(accountId: String, tripId: String): EquipmentAggregate {
        ownershipRepository.findByTripIdAndAccountId(tripId, accountId) ?: throw ApiContractException.notFound()
        val trip = tripRepository.findById(tripId).orElseThrow { ApiContractException.notFound() }
        return loadAggregate(trip)
    }

    private fun requireEditableAggregate(accountId: String, tripId: String, expectedRevision: String?): EquipmentAggregate {
        commandStore.lockAccount(accountId)
        val trip = tripRepository.findByIdForUpdate(tripId) ?: throw ApiContractException.notFound()
        ownershipRepository.findByTripIdAndAccountId(tripId, accountId) ?: throw ApiContractException.notFound()
        if (expectedRevision != null && trip.revision != expectedRevision) {
            throw ApiContractException.conflict("concurrent_modification", "行程 revision 已变化")
        }
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
            try { TripPreparationRules.validate(item) } catch (_: ApiContractException) { throw equipmentUnavailable() }
            val normalized = equipmentDomainService.normalizeEquipmentName(item.name)
            if (item.snapshotId != snapshotId || item.normalizedName != normalized.comparison ||
                !normalizedNames.add(item.normalizedName) || item.quantity <= 0 ||
                item.unitWeightGrams?.let { !it.isFinite() || it <= 0 } == true || item.note?.isBlank() == true
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
            if (item.ownershipStatus !in OWNERSHIP_STATUSES || item.supplyKind != null && item.ownershipStatus != "unconfirmed_owned") throw equipmentUnavailable()
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
                        ownershipStatus = item.ownershipStatus,
                        category = item.category, equipmentType = item.equipmentType, variant = item.variant,
                        unitLabel = item.unitLabel, weightScope = item.weightScope,
                        carryAllocation = TripPreparationRules.configuration(item).carryAllocation,
                        supply = TripPreparationRules.configuration(item).supply,
                        packedQuantity = item.packedQuantity,
                        ownershipCheckApplicable = item.supplyKind == null
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
        knownTotalWeight = WeightProjection(TripPreparationRules.totalWeight(items)),
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
