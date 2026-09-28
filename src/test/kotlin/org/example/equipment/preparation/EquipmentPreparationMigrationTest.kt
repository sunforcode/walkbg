package org.example.equipment.preparation

import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Test

class EquipmentPreparationMigrationTest {
    @Test
    fun `approved equipment preparation has a forward migration`() {
        assertNotNull(javaClass.classLoader.getResource("db/migration/V16__extend_equipment_preparation.sql"))
    }
}
