package org.example.trip.personal.service

import org.example.common.contract.ApiContractException
import org.example.equipment.service.PersonalEquipmentDomainService
import org.example.route.model.RouteVersionEquipmentSuggestion
import org.example.trip.personal.model.PersonalTripEquipmentItemDerivedFromSuggestion
import org.example.trip.personal.model.PersonalTripEquipmentItemRecord
import org.example.trip.personal.model.TripSuppressesEquipmentSuggestion
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows

class TripMigrationEquipmentMergerTest {
    private val merger = TripMigrationEquipmentMerger(PersonalEquipmentDomainService())

    @Test
    fun `merge applies target baseline adjustments additions suppressions then current ownership`() {
        val target = listOf(
            suggestion("target-tent", "logical-tent", 1, "目标帐篷", 1),
            suggestion("target-stove", "logical-stove", 2, "炉头", 1),
            suggestion("target-water", "logical-water", 3, "净水器", 1)
        )
        val adjusted = item("old-tent", 1, "我的帐篷", "我的帐篷", 2, "user_adjusted")
        val added = item("old-bottle", 2, "水瓶", "水瓶", 1, "user_added")
        val oldSystem = item("old-stove", 3, "旧炉头", "旧炉头", 1, "system_suggestion")
        val relations = listOf(
            derivation("old-tent", "logical-tent", "old-tent-occurrence"),
            derivation("old-stove", "logical-stove", "old-stove-occurrence")
        )

        val result = merger.merge(
            routeId = "route-1",
            targetSuggestions = target,
            oldItems = listOf(adjusted, added, oldSystem),
            oldDerivations = relations,
            suppressions = listOf(TripSuppressesEquipmentSuggestion("trip-1", "logical-water", "route-1")),
            ownedQuantitiesByNormalizedName = mapOf("我的帐篷" to 2, "水瓶" to 0, "炉头" to 1)
        )

        assertEquals(listOf("我的帐篷", "炉头", "水瓶"), result.map { it.name })
        assertEquals(listOf("user_adjusted", "system_suggestion", "user_added"), result.map { it.source })
        assertEquals(listOf("owned", "owned", "unconfirmed_owned"), result.map { it.ownershipStatus })
        assertEquals("logical-tent", result[0].logicalSuggestionId)
        assertEquals("target-tent", result[0].suggestionOccurrenceId)
        assertEquals("logical-stove", result[1].logicalSuggestionId)
        assertNull(result[2].logicalSuggestionId)
    }

    @Test
    fun `adjustment remains linked to logical identity when target has no occurrence`() {
        val adjusted = item("old-gloves", 1, "防水手套", "防水手套", 1, "user_adjusted")

        val result = merger.merge(
            routeId = "route-1",
            targetSuggestions = emptyList(),
            oldItems = listOf(adjusted),
            oldDerivations = listOf(derivation("old-gloves", "logical-gloves", "old-occurrence")),
            suppressions = emptyList(),
            ownedQuantitiesByNormalizedName = emptyMap()
        ).single()

        assertEquals("logical-gloves", result.logicalSuggestionId)
        assertNull(result.suggestionOccurrenceId)
        assertEquals("user_adjusted", result.source)
    }

    @Test
    fun `normalized name conflict fails the whole migration candidate instead of merging`() {
        val target = listOf(suggestion("target-tent", "logical-tent", 1, "Tent Bag", 1))
        val added = item("old-added", 1, "  tent   bag ", "tent bag", 1, "user_added")

        val error = assertThrows<ApiContractException> {
            merger.merge(
                routeId = "route-1",
                targetSuggestions = target,
                oldItems = listOf(added),
                oldDerivations = emptyList(),
                suppressions = emptyList(),
                ownedQuantitiesByNormalizedName = emptyMap()
            )
        }

        assertEquals("migration_generation_failed", error.code)
        assertEquals(503, error.status.value())
    }

    private fun suggestion(
        id: String,
        logicalId: String,
        order: Int,
        name: String,
        quantity: Int
    ) = RouteVersionEquipmentSuggestion(
        id = id,
        routeId = "route-1",
        routeVersionId = "version-2",
        logicalSuggestionId = logicalId,
        displayOrder = order,
        name = name,
        normalizedName = name,
        quantity = quantity,
        unitWeightGrams = 500,
        level = "required"
    )

    private fun item(
        id: String,
        order: Int,
        name: String,
        normalized: String,
        quantity: Int,
        source: String
    ) = PersonalTripEquipmentItemRecord(
        id = id,
        snapshotId = "snapshot-1",
        displayOrder = order,
        name = name,
        normalizedName = normalized,
        quantity = quantity,
        unitWeightGrams = 400,
        source = source,
        ownershipStatus = "unconfirmed_owned"
    )

    private fun derivation(itemId: String, logicalId: String, occurrenceId: String) =
        PersonalTripEquipmentItemDerivedFromSuggestion(
            itemId = itemId,
            tripId = "trip-1",
            logicalSuggestionId = logicalId,
            suggestionOccurrenceId = occurrenceId,
            routeId = "route-1"
        )
}
