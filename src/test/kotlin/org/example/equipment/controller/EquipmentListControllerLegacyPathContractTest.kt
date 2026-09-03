package org.example.equipment.controller

import org.junit.jupiter.api.Assertions.assertArrayEquals
import org.junit.jupiter.api.Test
import org.springframework.web.bind.annotation.RequestMapping

class EquipmentListControllerLegacyPathContractTest {
    @Test
    fun `legacy equipment list controller does not occupy target collection path`() {
        val mapping = EquipmentListController::class.java.getAnnotation(RequestMapping::class.java)

        assertArrayEquals(arrayOf("/api/v1/legacy/equipment-lists"), mapping.value)
    }
}
