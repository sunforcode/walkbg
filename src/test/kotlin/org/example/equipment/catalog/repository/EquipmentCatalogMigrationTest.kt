package org.example.equipment.catalog.repository

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Test

class EquipmentCatalogMigrationTest {
    @Test
    fun `migration creates catalog identities and sixty three direct specification tables`() {
        val resource = javaClass.classLoader.getResource("db/migration/V15__add_equipment_catalog.sql")
        assertNotNull(resource, "Approved catalog requires an executable Flyway migration")
        val sql = requireNotNull(resource).readText()
        val tables = Regex("CREATE TABLE ([a-z_]+)").findAll(sql).map { it.groupValues[1] }.toSet()
        assertEquals(65, tables.size)
        assertEquals(63, tables.count { it.endsWith("_specs") })
        assertFalse(tables.contains("equipment_specs"))
        assertFalse(Regex("(?i)(ALTER|DROP|INSERT INTO)\\s+(personal_equipment|equipment_items)").containsMatchIn(sql))
        val equipment = sql.substringAfter("CREATE TABLE equipment (").substringBefore(") ENGINE")
        assertFalse(equipment.contains("unit_weight"))
    }
}
