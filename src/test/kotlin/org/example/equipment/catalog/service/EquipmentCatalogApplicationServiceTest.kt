package org.example.equipment.catalog.service

import org.example.common.contract.ApiContractException
import org.example.equipment.catalog.dto.SimpleSpecificationsProjection
import org.example.equipment.catalog.dto.SleepingBagSpecificationsProjection
import org.example.equipment.catalog.model.CatalogBrandRecord
import org.example.equipment.catalog.model.CatalogEquipmentRecord
import org.example.equipment.catalog.model.CatalogFilter
import org.example.equipment.catalog.model.CatalogSpecRecord
import org.example.equipment.catalog.repository.EquipmentCatalogRepository
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.mockito.kotlin.any
import org.mockito.kotlin.mock
import org.mockito.kotlin.verifyNoInteractions
import org.mockito.kotlin.whenever

class EquipmentCatalogApplicationServiceTest {
    private val repository = mock<EquipmentCatalogRepository>()
    private val service = EquipmentCatalogApplicationService(repository, EquipmentCatalogDomainService())

    @Test
    fun `invalid filters never read storage`() {
        val failure = assertThrows<ApiContractException> { service.getItems(CatalogFilter(weightGrams = 0.0)) }
        assertEquals(422, failure.status.value())
        verifyNoInteractions(repository)
    }

    @Test
    fun `unknown weight and independent temperatures survive projections`() {
        val bag = CatalogEquipmentRecord("bag", "睡袋", "sleeping", "sleeping_bag",
            specifications = CatalogSpecRecord(unitLabel = "件", limitTemperatureCelsius = -5.0))
        whenever(repository.findById("bag")).thenReturn(bag)
        val detail = service.getItem("bag").specifications as SleepingBagSpecificationsProjection
        assertNull(detail.unitWeight)
        assertNull(detail.comfortTemperatureCelsius)
        assertEquals(-5.0, detail.limitTemperatureCelsius)
        whenever(repository.findItems(any())).thenReturn(listOf(bag))
        val summary = service.getItems(CatalogFilter()).items.single().specifications
        assertInstanceOf(SimpleSpecificationsProjection::class.java, summary)
        assertNull(summary!!.unitWeight)
    }

    @Test
    fun `duplicate identities and malformed records fail the entire collection`() {
        val item = CatalogEquipmentRecord("same", "背包", "backpack", "backpack")
        whenever(repository.findItems(any())).thenReturn(listOf(item, item))
        assertUnavailable { service.getItems(CatalogFilter()) }
        whenever(repository.findBrands()).thenReturn(listOf(CatalogBrandRecord("same", "品牌"), CatalogBrandRecord("same", "其他品牌")))
        assertUnavailable { service.getBrands() }
        whenever(repository.findItems(any())).thenReturn(listOf(item, item.copy(identity = "bad", name = "未清理  名称")))
        assertUnavailable { service.getItems(CatalogFilter()) }
    }

    @Test
    fun `read exceptions never turn into empty successful collections`() {
        whenever(repository.findBrands()).thenThrow(IllegalStateException("internal path and password"))
        val failure = assertUnavailable { service.getBrands() }
        assertEquals("装备目录暂不可用", failure.message)
        assertNull(failure.details)
        whenever(repository.findById("missing")).thenReturn(null)
        assertEquals(404, assertThrows<ApiContractException> { service.getItem("missing") }.status.value())
        whenever(repository.findById("requested")).thenReturn(CatalogEquipmentRecord("different", "背包", "backpack", "backpack"))
        assertUnavailable { service.getItem("requested") }
    }

    private fun assertUnavailable(block: () -> Any): ApiContractException {
        val failure = assertThrows<ApiContractException> { block() }
        assertEquals(503, failure.status.value())
        assertEquals("equipment_catalog_unavailable", failure.code)
        assertTrue(failure.retryable)
        return failure
    }
}
