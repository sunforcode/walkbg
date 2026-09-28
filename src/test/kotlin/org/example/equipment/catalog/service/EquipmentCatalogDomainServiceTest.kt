package org.example.equipment.catalog.service

import org.example.common.contract.ApiContractException
import org.example.equipment.catalog.model.CatalogBrandRecord
import org.example.equipment.catalog.model.CatalogEquipmentRecord
import org.example.equipment.catalog.model.CatalogFilter
import org.example.equipment.catalog.model.CatalogSpecRecord
import org.example.equipment.catalog.model.CatalogType
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows

class EquipmentCatalogDomainServiceTest {
    private val service = EquipmentCatalogDomainService()

    @Test
    fun `unknown specs and partially known sleeping bag temperatures remain valid`() {
        service.validateRecord(item(spec = null))
        service.validateRecord(item(spec = CatalogSpecRecord(unitLabel = "件")))
        service.validateRecord(item(spec = CatalogSpecRecord(unitLabel = "件", comfortTemperatureCelsius = 0.0)))
        service.validateRecord(item(spec = CatalogSpecRecord(unitLabel = "件", limitTemperatureCelsius = -12.5)))
        service.validateRecord(item(spec = CatalogSpecRecord(
            unitLabel = "件", unitWeightGrams = 735.5, weightScope = "含原配压缩袋",
            comfortTemperatureCelsius = -2.5, limitTemperatureCelsius = -9.0
        )))
    }

    @Test
    fun `all 63 registered types accept only their own known weight without invented parameters`() {
        assertEquals(63, CatalogType.entries.size)
        CatalogType.entries.forEach { type ->
            service.validateRecord(CatalogEquipmentRecord(
                "identity-${type.code}", "已知装备", type.allowedCategories.first(), type.code,
                specifications = CatalogSpecRecord(
                    unitLabel = if (type.code == "tent") "套" else "件",
                    unitWeightGrams = 1.5, weightScope = "所述完整配置"
                )
            ))
        }
    }

    @Test
    fun `malformed catalog records fail closed as retryable unavailable`() {
        val invalid = listOf(
            item().copy(identity = ""), item().copy(name = "  "),
            item().copy(category = "not-a-category"), item().copy(equipmentType = "other"),
            item().copy(category = "tent"), item().copy(model = " "),
            item().copy(brand = CatalogBrandRecord("brand", "")),
            item(spec = CatalogSpecRecord(unitLabel = "kg")),
            item(spec = CatalogSpecRecord(unitLabel = "件", variant = " ")),
            item(spec = CatalogSpecRecord(unitLabel = "件", unitWeightGrams = 0.0, weightScope = "配置")),
            item(spec = CatalogSpecRecord(unitLabel = "件", unitWeightGrams = -1.0, weightScope = "配置")),
            item(spec = CatalogSpecRecord(unitLabel = "件", unitWeightGrams = Double.NaN, weightScope = "配置")),
            item(spec = CatalogSpecRecord(unitLabel = "件", unitWeightGrams = 1.0)),
            item(spec = CatalogSpecRecord(unitLabel = "件", weightScope = " ")),
            item(spec = CatalogSpecRecord(unitLabel = "件", comfortTemperatureCelsius = Double.POSITIVE_INFINITY)),
            item(spec = CatalogSpecRecord(unitLabel = "件", capacityLiters = 50.0)),
            item(spec = CatalogSpecRecord(unitLabel = "件", padType = "air"))
        )
        invalid.forEach { record ->
            val failure = assertThrows<ApiContractException> { service.validateRecord(record) }
            assertEquals(503, failure.status.value(), record.toString())
            assertEquals("equipment_catalog_unavailable", failure.code)
            assertTrue(failure.retryable)
        }
    }

    @Test
    fun `core parameters preserve their own units and enforce declared domains`() {
        fun core(type: String, spec: CatalogSpecRecord) = CatalogEquipmentRecord(
            "item", "装备", CatalogType.fromCode(type)!!.allowedCategories.first(), type, specifications = spec
        )
        val valid = listOf(
            core("backpack", CatalogSpecRecord(unitLabel = "件", capacityLiters = 50.5)),
            core("tent", CatalogSpecRecord(unitLabel = "套", capacityPersons = 2)),
            core("sleeping_pad", CatalogSpecRecord(unitLabel = "件", rValue = 0.0, padType = "air", lengthCm = 180.5, maxWidthCm = 60.0, thicknessCm = 7.6))
        )
        valid.forEach(service::validateRecord)
        listOf(
            core("backpack", CatalogSpecRecord(unitLabel = "件", capacityLiters = 0.0)),
            core("tent", CatalogSpecRecord(unitLabel = "件", capacityPersons = 2)),
            core("tent", CatalogSpecRecord(unitLabel = "套", capacityPersons = 0)),
            core("sleeping_pad", CatalogSpecRecord(unitLabel = "件", rValue = -0.1)),
            core("sleeping_pad", CatalogSpecRecord(unitLabel = "件", padType = "unknown")),
            core("sleeping_pad", CatalogSpecRecord(unitLabel = "件", maxWidthCm = Double.POSITIVE_INFINITY)),
            core("sleeping_pad", CatalogSpecRecord(unitLabel = "件", thicknessCm = 0.0)),
            core("food", CatalogSpecRecord(unitLabel = "包", comfortTemperatureCelsius = 0.0))
        ).forEach { assertThrows<ApiContractException> { service.validateRecord(it) } }
    }

    @Test
    fun `filters reject unknown codes contradictory type and ambiguous or nonfinite numbers`() {
        val malformed = listOf(
            CatalogFilter(category = "other"), CatalogFilter(equipmentType = "quilt"),
            CatalogFilter(category = "tent", equipmentType = "sleeping_bag"),
            CatalogFilter(comfortTemperatureCelsius = 0.0),
            CatalogFilter(equipmentType = "tent", maxComfortTemperatureCelsius = -5.0),
            CatalogFilter(weightGrams = 10.0, minWeightGrams = 5.0),
            CatalogFilter(equipmentType = "sleeping_bag", comfortTemperatureCelsius = 0.0, maxComfortTemperatureCelsius = 2.0),
            CatalogFilter(weightGrams = Double.NaN), CatalogFilter(maxWeightGrams = Double.POSITIVE_INFINITY)
        )
        malformed.forEach { assertEquals(400, assertThrows<ApiContractException> { service.validateFilter(it) }.status.value()) }
        val invalidValues = listOf(
            CatalogFilter(weightGrams = 0.0), CatalogFilter(minWeightGrams = -1.0),
            CatalogFilter(minWeightGrams = 20.0, maxWeightGrams = 10.0),
            CatalogFilter(equipmentType = "sleeping_bag", minComfortTemperatureCelsius = 5.0, maxComfortTemperatureCelsius = -5.0)
        )
        invalidValues.forEach { assertEquals(422, assertThrows<ApiContractException> { service.validateFilter(it) }.status.value()) }
        service.validateFilter(CatalogFilter(equipmentType = "sleeping_bag", minComfortTemperatureCelsius = -10.0, maxComfortTemperatureCelsius = 0.0))
        service.validateFilter(CatalogFilter(category = "clothing", equipmentType = "storage_bag", brandId = "opaque value"))
        service.validateFilter(CatalogFilter(category = "dining_equipment", equipmentType = "storage_bag"))
    }

    private fun item(spec: CatalogSpecRecord? = null) = CatalogEquipmentRecord(
        "bag-1", "睡袋", "sleeping", "sleeping_bag", specifications = spec
    )
}
