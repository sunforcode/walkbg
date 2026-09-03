package org.example.route.service

import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class RouteVersionEvolutionMigrationContractTest {
    private val migration = requireNotNull(
        javaClass.classLoader.getResource("db/migration/V10__add_route_version_evolution_relations.sql")
    ).readText()

    @Test
    fun `migration persists explicit published order and treats only current publication as existing maximum`() {
        assertTrue(migration.contains("CREATE TABLE route_version_publication_order"))
        assertTrue(migration.contains("CONSTRAINT uk_route_version_publication_sequence UNIQUE (route_id, published_sequence)"))
        assertTrue(migration.contains("CONSTRAINT uk_route_version_publication_version UNIQUE (route_id, route_version_id)"))
        assertTrue(migration.contains("CHECK (published_sequence > 0)"))
        assertTrue(migration.contains("INSERT INTO route_version_publication_order"))
        assertTrue(migration.contains("SELECT current.route_id, current.route_version_id, 1"))
        assertTrue(migration.contains("FROM route_current_public_versions current"))
        assertFalse(migration.contains("WHERE version.id = CONCAT('rv-'"))
        assertTrue(migration.contains("CONSTRAINT fk_current_public_version_publication"))
    }

    @Test
    fun `migration persists route scoped logical identities and version suggestion occurrences`() {
        assertTrue(migration.contains("CREATE TABLE logical_equipment_suggestion_identities"))
        assertTrue(migration.contains("PRIMARY KEY (route_id, logical_suggestion_id)"))
        assertTrue(migration.contains("CREATE TABLE route_version_equipment_suggestions"))
        assertTrue(migration.contains("CONSTRAINT uk_route_version_suggestion_order UNIQUE (route_version_id, display_order)"))
        assertTrue(migration.contains("CONSTRAINT uk_route_version_suggestion_name UNIQUE (route_version_id, normalized_name)"))
        assertTrue(migration.contains("CONSTRAINT uk_route_version_suggestion_logical UNIQUE (route_version_id, logical_suggestion_id)"))
        assertTrue(migration.contains("CONSTRAINT fk_route_version_suggestion_version_scope"))
        assertTrue(migration.contains("CONSTRAINT fk_route_version_suggestion_logical_scope"))
        assertTrue(migration.contains("level IN ('required', 'recommended')"))
        assertTrue(migration.contains("quantity > 0"))
        assertTrue(migration.contains("unit_weight_grams IS NULL OR unit_weight_grams > 0"))
        assertFalse(migration.contains("INSERT INTO route_version_equipment_suggestions"))
    }
}
