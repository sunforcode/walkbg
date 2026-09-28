package org.example.equipment.catalog.service

import org.example.common.contract.ApiContractException
import org.example.equipment.catalog.model.CatalogBrandRecord
import org.example.equipment.catalog.model.CatalogEquipmentRecord
import org.example.equipment.catalog.model.CatalogFilter
import org.example.equipment.catalog.model.CatalogSpecRecord
import org.example.equipment.catalog.model.CatalogType
import org.springframework.stereotype.Service
import org.example.equipment.util.normalizeEquipmentSearchText

@Service
class EquipmentCatalogDomainService {
    fun validateFilter(filter: CatalogFilter) {
        if (filter.keyword != null && normalizeEquipmentSearchText(filter.keyword).isEmpty()) invalid()
        val type = filter.equipmentType?.let { CatalogType.fromCode(it) ?: invalid() }
        if (filter.category != null && filter.category !in CatalogType.categories) invalid()
        if (type != null && filter.category != null && filter.category !in type.allowedCategories) invalid()
        if (filter.brandId != null && filter.brandId.isBlank()) invalid()
        val weights = listOfNotNull(filter.weightGrams, filter.minWeightGrams, filter.maxWeightGrams)
        val temperatures = listOfNotNull(filter.comfortTemperatureCelsius, filter.minComfortTemperatureCelsius, filter.maxComfortTemperatureCelsius)
        if ((weights + temperatures).any { !it.isFinite() }) invalid()
        if (filter.weightGrams != null && (filter.minWeightGrams != null || filter.maxWeightGrams != null)) invalid()
        if (filter.comfortTemperatureCelsius != null && (filter.minComfortTemperatureCelsius != null || filter.maxComfortTemperatureCelsius != null)) invalid()
        if (temperatures.isNotEmpty() && filter.equipmentType != "sleeping_bag") invalid()
        if (weights.any { it <= 0.0 } || reversed(filter.minWeightGrams, filter.maxWeightGrams) ||
            reversed(filter.minComfortTemperatureCelsius, filter.maxComfortTemperatureCelsius)) {
            throw ApiContractException.unprocessable("validation_failed", "数值范围无效")
        }
    }

    fun validateBrand(brand: CatalogBrandRecord) {
        validRecord(brand.identity.isNotBlank() && cleanedText(brand.name))
    }

    fun validateRecord(record: CatalogEquipmentRecord) {
        val type = CatalogType.fromCode(record.equipmentType) ?: unavailable()
        validRecord(record.identity.isNotBlank() && cleanedText(record.name) &&
            record.name == record.name.replace(Regex("[\\p{javaWhitespace}\\p{Z}]+"), " ") &&
            record.category in type.allowedCategories && optionalText(record.model))
        record.brand?.let(::validateBrand)
        record.specifications?.let { validateSpec(record.equipmentType, it) }
    }

    fun validateUniqueIdentities(identities: List<String>) = validRecord(identities.size == identities.toSet().size)

    private fun validateSpec(type: String, spec: CatalogSpecRecord) {
        validRecord(spec.unitLabel in CatalogType.unitLabels && optionalText(spec.variant) && optionalText(spec.weightScope))
        validRecord(positive(spec.unitWeightGrams) && (spec.unitWeightGrams == null || spec.weightScope != null))
        validRecord(type == "backpack" || spec.capacityLiters == null)
        validRecord(type == "tent" || spec.capacityPersons == null)
        validRecord(type == "sleeping_bag" || (spec.comfortTemperatureCelsius == null && spec.limitTemperatureCelsius == null))
        validRecord(type == "sleeping_pad" || (spec.padType == null && spec.rValue == null && spec.lengthCm == null && spec.maxWidthCm == null && spec.thicknessCm == null))
        validRecord(positive(spec.capacityLiters) && (spec.capacityPersons == null || spec.capacityPersons > 0))
        validRecord(type != "tent" || spec.unitLabel == "套")
        validRecord(finite(spec.comfortTemperatureCelsius) && finite(spec.limitTemperatureCelsius))
        validRecord(spec.padType == null || spec.padType in setOf("closed_cell_foam", "air", "self_inflating"))
        validRecord(spec.rValue == null || (spec.rValue.isFinite() && spec.rValue >= 0.0))
        validRecord(positive(spec.lengthCm) && positive(spec.maxWidthCm) && positive(spec.thicknessCm))
    }

    private fun cleanedText(value: String): Boolean = value.isNotBlank() && value == value.trim()
    private fun optionalText(value: String?): Boolean = value == null || cleanedText(value)
    private fun positive(value: Double?): Boolean = value == null || (value.isFinite() && value > 0.0)
    private fun finite(value: Double?): Boolean = value == null || value.isFinite()
    private fun reversed(min: Double?, max: Double?): Boolean = min != null && max != null && min > max
    private fun invalid(): Nothing = throw ApiContractException.invalidRequest("目录筛选参数无效")
    private fun validRecord(condition: Boolean) { if (!condition) unavailable() }
    private fun unavailable(): Nothing = throw ApiContractException.serviceUnavailable("equipment_catalog_unavailable", "装备目录暂不可用")
}
