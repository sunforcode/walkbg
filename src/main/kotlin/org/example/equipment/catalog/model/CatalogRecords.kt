package org.example.equipment.catalog.model

/** Read projections only: every concrete specification remains stored in its own table. */
data class CatalogBrandRecord(val identity: String, val name: String)

data class CatalogEquipmentRecord(
    val identity: String,
    val name: String,
    val category: String,
    val equipmentType: String,
    val brand: CatalogBrandRecord? = null,
    val model: String? = null,
    val specifications: CatalogSpecRecord? = null
)

data class CatalogSpecRecord(
    val variant: String? = null,
    val unitLabel: String,
    val unitWeightGrams: Double? = null,
    val weightScope: String? = null,
    val capacityLiters: Double? = null,
    val capacityPersons: Int? = null,
    val comfortTemperatureCelsius: Double? = null,
    val limitTemperatureCelsius: Double? = null,
    val padType: String? = null,
    val rValue: Double? = null,
    val lengthCm: Double? = null,
    val maxWidthCm: Double? = null,
    val thicknessCm: Double? = null
)

data class CatalogFilter(
    val category: String? = null,
    val brandId: String? = null,
    val equipmentType: String? = null,
    val weightGrams: Double? = null,
    val minWeightGrams: Double? = null,
    val maxWeightGrams: Double? = null,
    val comfortTemperatureCelsius: Double? = null,
    val minComfortTemperatureCelsius: Double? = null,
    val maxComfortTemperatureCelsius: Double? = null,
    val keyword: String? = null
)
