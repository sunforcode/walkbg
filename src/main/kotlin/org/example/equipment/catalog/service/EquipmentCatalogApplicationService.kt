package org.example.equipment.catalog.service

import org.example.common.contract.ApiContractException
import org.example.equipment.catalog.dto.*
import org.example.equipment.catalog.model.CatalogBrandRecord
import org.example.equipment.catalog.model.CatalogEquipmentRecord
import org.example.equipment.catalog.model.CatalogFilter
import org.example.equipment.catalog.model.CatalogSpecRecord
import org.example.equipment.catalog.repository.EquipmentCatalogRepository
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
@Transactional(readOnly = true)
class EquipmentCatalogApplicationService(
    private val repository: EquipmentCatalogRepository,
    private val domain: EquipmentCatalogDomainService
) {
    private val logger = LoggerFactory.getLogger(javaClass)

    fun getBrands(): CatalogBrandCollection = read {
        val records = repository.findBrands()
        records.forEach(domain::validateBrand)
        domain.validateUniqueIdentities(records.map { it.identity })
        CatalogBrandCollection(records.sortedBy { it.identity }.map(::brand))
    }

    fun getItems(filter: CatalogFilter): CatalogEquipmentCollection {
        domain.validateFilter(filter)
        return read {
            val records = repository.findItems(filter)
            records.forEach(domain::validateRecord)
            domain.validateUniqueIdentities(records.map { it.identity })
            CatalogEquipmentCollection(records.sortedBy { it.identity }.map { item ->
                CatalogEquipmentSummaryProjection(item.identity, item.name, item.category, item.equipmentType,
                    item.brand?.let(::brand), item.model, item.specifications?.let(::summary))
            })
        }
    }

    fun getRecord(identity: String): CatalogEquipmentRecord = read {
        val item = repository.findById(identity) ?: throw ApiContractException.notFound()
        domain.validateRecord(item)
        if (item.identity != identity) throw ApiContractException.serviceUnavailable("equipment_catalog_unavailable", "装备目录暂不可用")
        item
    }

    fun getItem(identity: String): CatalogEquipmentDetailProjection = read {
        val item = getRecord(identity)
        CatalogEquipmentDetailProjection(item.identity, item.name, item.category, item.equipmentType,
            item.brand?.let(::brand), item.model, item.specifications?.let { detail(item, it) })
    }

    private fun brand(value: CatalogBrandRecord) = CatalogBrandProjection(value.identity, value.name)
    private fun summary(value: CatalogSpecRecord) = SimpleSpecificationsProjection(
        value.unitLabel, value.variant, value.unitWeightGrams?.let(::CatalogWeightProjection), value.weightScope
    )

    private fun detail(item: CatalogEquipmentRecord, value: CatalogSpecRecord): CatalogDetailSpecifications {
        val weight = value.unitWeightGrams?.let(::CatalogWeightProjection)
        return when (item.equipmentType) {
            "backpack" -> BackpackSpecificationsProjection(value.unitLabel, value.variant, weight, value.weightScope, value.capacityLiters)
            "tent" -> TentSpecificationsProjection(value.unitLabel, value.variant, weight, value.weightScope, value.capacityPersons)
            "sleeping_bag" -> SleepingBagSpecificationsProjection(value.unitLabel, value.variant, weight, value.weightScope, value.comfortTemperatureCelsius, value.limitTemperatureCelsius)
            "sleeping_pad" -> SleepingPadSpecificationsProjection(value.unitLabel, value.variant, weight, value.weightScope, value.padType, value.rValue, value.lengthCm, value.maxWidthCm, value.thicknessCm)
            else -> summary(value)
        }
    }

    private fun <T> read(block: () -> T): T = try {
        block()
    } catch (exception: ApiContractException) {
        throw exception
    } catch (exception: Exception) {
        logger.error("Equipment catalog read failed", exception)
        throw ApiContractException.serviceUnavailable("equipment_catalog_unavailable", "装备目录暂不可用")
    }
}
