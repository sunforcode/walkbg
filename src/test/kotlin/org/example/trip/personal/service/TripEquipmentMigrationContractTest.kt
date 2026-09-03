package org.example.trip.personal.service

import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class TripEquipmentMigrationContractTest {
    private val migration = requireNotNull(
        javaClass.classLoader.getResource("db/migration/V9__add_trip_equipment_snapshot_management.sql")
    ).readText()

    @Test
    fun `migration backfills normalized names and enforces snapshot name uniqueness`() {
        assertTrue(
            migration.contains(
                "ADD COLUMN normalized_name varchar(200) CHARACTER SET utf8mb4 COLLATE utf8mb4_bin NULL"
            )
        )
        assertTrue(migration.contains("REGEXP_REPLACE"))
        assertTrue(migration.contains("CONVERT(normalized_name USING utf8mb4) COLLATE utf8mb4_bin"))
        assertTrue(
            migration.contains(
                "MODIFY COLUMN normalized_name varchar(200) CHARACTER SET utf8mb4 COLLATE utf8mb4_bin NOT NULL"
            )
        )
        assertTrue(
            migration.contains(
                "CONSTRAINT uk_personal_trip_item_normalized_name UNIQUE (snapshot_id, normalized_name)"
            )
        )
    }

    @Test
    fun `migration persists derivation and suppression relations without suggestion content`() {
        assertTrue(migration.contains("CREATE TABLE personal_trip_equipment_item_derivations"))
        assertTrue(migration.contains("logical_suggestion_id"))
        assertTrue(migration.contains("suggestion_occurrence_id"))
        assertTrue(migration.contains("CONSTRAINT uk_trip_equipment_derivation_logical UNIQUE (trip_id, logical_suggestion_id)"))
        assertTrue(migration.contains("CREATE TABLE personal_trip_equipment_suppressions"))
        assertTrue(migration.contains("PRIMARY KEY (trip_id, logical_suggestion_id)"))
        assertFalse(migration.contains("CREATE TABLE route_version_equipment_suggestions"))
        assertFalse(migration.contains("INSERT INTO route_version_equipment_suggestions"))
    }
}
