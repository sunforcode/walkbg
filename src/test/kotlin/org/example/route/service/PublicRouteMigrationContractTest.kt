package org.example.route.service

import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class PublicRouteMigrationContractTest {
    private val migration = requireNotNull(
        javaClass.classLoader.getResource("db/migration/V6__add_public_route_versions.sql")
    ).readText()

    @Test
    fun `legacy snapshot only maps explicitly supported poi categories`() {
        assertTrue(migration.contains("AND p.category IN ('start', 'end', 'camp', 'water', 'supply', 'danger', 'pass', 'valley', 'photo')"))
        assertFalse(migration.contains("ELSE 'interest'"))
    }

    @Test
    fun `migration preserves legacy routes without inventing target route type or publishing ineligible snapshots`() {
        assertTrue(migration.contains("FROM routes r\nLEFT JOIN route_map_data md ON md.id = r.id;"))
        assertTrue(migration.contains("'initial',\n    NULL,"))
        assertFalse(migration.contains("CASE WHEN r.route_type = 3 THEN 'multi_day' ELSE 'one_day' END"))
        assertTrue(migration.contains("AND version.route_type IN ('one_day', 'multi_day')"))
        assertTrue(migration.contains("AND version.start_name IS NOT NULL"))
        assertTrue(migration.contains("AND version.end_name IS NOT NULL"))
        assertTrue(migration.contains("AND version.main_track_availability = 'valid'"))
        assertTrue(migration.contains("JOIN route_current_public_versions current_version ON current_version.route_id = r.id"))
        assertTrue(migration.contains("ORDER BY r.created_at ASC, r.id ASC"))
    }

    @Test
    fun `legacy route duration minutes are converted to seconds`() {
        assertTrue(migration.contains("md.duration * 60"))
        assertFalse(migration.contains("md.duration * 86400"))
    }
}
