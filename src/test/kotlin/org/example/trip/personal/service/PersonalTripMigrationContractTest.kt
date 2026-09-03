package org.example.trip.personal.service

import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class PersonalTripMigrationContractTest {
    private val migration = requireNotNull(
        javaClass.classLoader.getResource("db/migration/V8__add_personal_trips.sql")
    ).readText()

    @Test
    fun `migration creates every personal trip aggregate table without importing legacy trips`() {
        listOf(
            "personal_trips",
            "personal_trip_ownership",
            "trip_frozen_route_versions",
            "personal_trip_days",
            "personal_trip_equipment_snapshots",
            "personal_trip_equipment_items",
            "personal_trip_idempotency",
            "trip_transport_selections"
        ).forEach { table ->
            assertTrue(migration.contains("CREATE TABLE $table"), "missing table $table")
        }
        assertFalse(migration.contains("INSERT INTO personal_trips"))
    }

    @Test
    fun `migration enforces aggregate cardinality idempotency and value constraints`() {
        assertTrue(migration.contains("CONSTRAINT uk_personal_trip_day_number UNIQUE (trip_id, day_number)"))
        assertTrue(migration.contains("CONSTRAINT uk_personal_trip_snapshot_trip UNIQUE (trip_id)"))
        assertTrue(migration.contains("CONSTRAINT uk_personal_trip_item_order UNIQUE (snapshot_id, display_order)"))
        assertTrue(
            migration.contains(
                "CONSTRAINT uk_personal_trip_idempotency UNIQUE (account_id, operation_name, idempotency_key)"
            )
        )
        assertTrue(migration.contains("quantity > 0"))
        assertTrue(migration.contains("unit_weight_grams IS NULL OR unit_weight_grams > 0"))
        assertTrue(migration.contains("source IN ('system_suggestion', 'user_added', 'user_adjusted')"))
        assertTrue(migration.contains("ownership_status IN ('owned', 'unconfirmed_owned')"))
        assertTrue(migration.contains("end_date >= start_date"))
        assertTrue(migration.contains("hiking_day_count <= total_day_count"))
    }
}
