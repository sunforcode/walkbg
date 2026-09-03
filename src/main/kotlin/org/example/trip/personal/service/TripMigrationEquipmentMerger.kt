package org.example.trip.personal.service

import org.example.common.contract.ApiContractException
import org.example.equipment.service.PersonalEquipmentDomainService
import org.example.route.model.RouteVersionEquipmentSuggestion
import org.example.trip.personal.model.PersonalTripEquipmentItemDerivedFromSuggestion
import org.example.trip.personal.model.PersonalTripEquipmentItemRecord
import org.example.trip.personal.model.TripSuppressesEquipmentSuggestion
import org.springframework.stereotype.Service

@Service
class TripMigrationEquipmentMerger(
    private val equipmentDomainService: PersonalEquipmentDomainService
) {
    fun merge(
        routeId: String,
        targetSuggestions: List<RouteVersionEquipmentSuggestion>,
        oldItems: List<PersonalTripEquipmentItemRecord>,
        oldDerivations: List<PersonalTripEquipmentItemDerivedFromSuggestion>,
        suppressions: List<TripSuppressesEquipmentSuggestion>,
        ownedQuantitiesByNormalizedName: Map<String, Int>
    ): List<MigrationEquipmentItem> {
        return try {
            mergeCandidate(
                routeId,
                targetSuggestions,
                oldItems,
                oldDerivations,
                suppressions,
                ownedQuantitiesByNormalizedName
            )
        } catch (exception: ApiContractException) {
            if (exception.code == MIGRATION_FAILURE_CODE) throw exception
            throw migrationFailure()
        } catch (_: Exception) {
            throw migrationFailure()
        }
    }

    private fun mergeCandidate(
        routeId: String,
        targetSuggestions: List<RouteVersionEquipmentSuggestion>,
        oldItems: List<PersonalTripEquipmentItemRecord>,
        oldDerivations: List<PersonalTripEquipmentItemDerivedFromSuggestion>,
        suppressions: List<TripSuppressesEquipmentSuggestion>,
        ownedQuantitiesByNormalizedName: Map<String, Int>
    ): List<MigrationEquipmentItem> {
        val derivationByItemId = oldDerivations.associateByUnique { it.itemId }
        val oldItemIds = oldItems.map { it.id }.toSet()
        if (oldDerivations.any { it.itemId !in oldItemIds || it.routeId != routeId }) {
            throw migrationFailure()
        }

        val candidatesByLogical = linkedMapOf<String, MigrationEquipmentItem>()
        targetSuggestions.sortedBy { it.displayOrder }.forEach { suggestion ->
            if (suggestion.routeId != routeId || candidatesByLogical.containsKey(suggestion.logicalSuggestionId)) {
                throw migrationFailure()
            }
            val normalized = equipmentDomainService.normalizeEquipmentName(suggestion.name)
            if (normalized.comparison != suggestion.normalizedName || suggestion.quantity <= 0 ||
                suggestion.unitWeightGrams?.let { it <= 0 } == true || suggestion.note?.isBlank() == true ||
                suggestion.level !in SUGGESTION_LEVELS
            ) {
                throw migrationFailure()
            }
            candidatesByLogical[suggestion.logicalSuggestionId] = MigrationEquipmentItem(
                name = normalized.display,
                normalizedName = normalized.comparison,
                quantity = suggestion.quantity,
                unitWeightGrams = suggestion.unitWeightGrams,
                note = suggestion.note,
                source = "system_suggestion",
                ownershipStatus = "unconfirmed_owned",
                logicalSuggestionId = suggestion.logicalSuggestionId,
                suggestionOccurrenceId = suggestion.id
            )
        }

        oldItems.sortedBy { it.displayOrder }.filter { it.source == "user_adjusted" }.forEach { item ->
            val derivation = derivationByItemId[item.id] ?: throw migrationFailure()
            val normalized = equipmentDomainService.normalizeEquipmentName(item.name)
            candidatesByLogical[derivation.logicalSuggestionId] = MigrationEquipmentItem(
                name = normalized.display,
                normalizedName = normalized.comparison,
                quantity = equipmentDomainService.requirePositiveQuantity(item.quantity),
                unitWeightGrams = item.unitWeightGrams?.let(equipmentDomainService::requirePositiveWeight),
                note = item.note?.trim()?.takeIf { it.isNotEmpty() } ?: item.note?.let { throw migrationFailure() },
                source = "user_adjusted",
                ownershipStatus = "unconfirmed_owned",
                logicalSuggestionId = derivation.logicalSuggestionId,
                suggestionOccurrenceId = candidatesByLogical[derivation.logicalSuggestionId]?.suggestionOccurrenceId
            )
        }

        val candidates = candidatesByLogical.values.toMutableList()
        oldItems.sortedBy { it.displayOrder }.filter { it.source == "user_added" }.forEach { item ->
            if (derivationByItemId[item.id] != null) throw migrationFailure()
            val normalized = equipmentDomainService.normalizeEquipmentName(item.name)
            candidates += MigrationEquipmentItem(
                name = normalized.display,
                normalizedName = normalized.comparison,
                quantity = equipmentDomainService.requirePositiveQuantity(item.quantity),
                unitWeightGrams = item.unitWeightGrams?.let(equipmentDomainService::requirePositiveWeight),
                note = item.note?.trim()?.takeIf { it.isNotEmpty() } ?: item.note?.let { throw migrationFailure() },
                source = "user_added",
                ownershipStatus = "unconfirmed_owned"
            )
        }

        if (oldItems.any { it.source !in ITEM_SOURCES } ||
            oldItems.filter { it.source == "system_suggestion" }.any { derivationByItemId[it.id] == null }
        ) {
            throw migrationFailure()
        }

        val suppressed = suppressions.map { suppression ->
            if (suppression.routeId != routeId) throw migrationFailure()
            suppression.logicalSuggestionId
        }.toSet()
        val visible = candidates.filterNot { it.logicalSuggestionId in suppressed }
        if (visible.map { it.normalizedName }.toSet().size != visible.size) throw migrationFailure()

        return visible.map { item ->
            item.copy(
                ownershipStatus = if ((ownedQuantitiesByNormalizedName[item.normalizedName] ?: 0) >= item.quantity) {
                    "owned"
                } else {
                    "unconfirmed_owned"
                }
            )
        }
    }

    private fun <K, V> Iterable<V>.associateByUnique(keySelector: (V) -> K): Map<K, V> {
        val result = linkedMapOf<K, V>()
        forEach { value ->
            if (result.put(keySelector(value), value) != null) throw migrationFailure()
        }
        return result
    }

    private fun migrationFailure() =
        ApiContractException.serviceUnavailable(MIGRATION_FAILURE_CODE, "无法形成完整迁移行程")

    private companion object {
        const val MIGRATION_FAILURE_CODE = "migration_generation_failed"
        val ITEM_SOURCES = setOf("system_suggestion", "user_adjusted", "user_added")
        val SUGGESTION_LEVELS = setOf("required", "recommended")
    }
}

data class MigrationEquipmentItem(
    val name: String,
    val normalizedName: String,
    val quantity: Int,
    val unitWeightGrams: Long?,
    val note: String?,
    val source: String,
    val ownershipStatus: String,
    val logicalSuggestionId: String? = null,
    val suggestionOccurrenceId: String? = null
)
