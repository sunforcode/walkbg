package org.example.equipment.catalog.repository

import org.example.equipment.catalog.model.CatalogBrandRecord
import org.example.equipment.catalog.model.CatalogEquipmentRecord
import org.example.equipment.catalog.model.CatalogFilter
import org.example.equipment.catalog.model.CatalogSpecRecord
import org.example.equipment.catalog.model.CatalogType
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate
import org.springframework.stereotype.Repository
import java.sql.ResultSet
import org.example.equipment.util.normalizeEquipmentSearchText

interface EquipmentCatalogRepository {
    fun findBrands(): List<CatalogBrandRecord>
    fun findItems(filter: CatalogFilter): List<CatalogEquipmentRecord>
    fun findById(identity: String): CatalogEquipmentRecord?
}

@Repository
class JdbcEquipmentCatalogRepository(private val jdbc: NamedParameterJdbcTemplate) : EquipmentCatalogRepository {
    override fun findBrands(): List<CatalogBrandRecord> = jdbc.query(
        "SELECT id, name FROM brand", emptyMap<String, Any>()
    ) { rs, _ -> CatalogBrandRecord(rs.getString("id"), rs.getString("name")) }.sortedBy { it.identity }

    override fun findItems(filter: CatalogFilter): List<CatalogEquipmentRecord> {
        val keyword = filter.keyword?.let(::normalizeEquipmentSearchText)
        val predicates = mutableListOf<String>()
        val parameters = mutableMapOf<String, Any>()
        filter.category?.let { predicates += "e.category = :category"; parameters["category"] = it }
        filter.brandId?.let { predicates += "e.brand_id = :brandId"; parameters["brandId"] = it }
        filter.equipmentType?.let { predicates += "e.equipment_type = :equipmentType"; parameters["equipmentType"] = it }
        val where = if (predicates.isEmpty()) "" else " WHERE " + predicates.joinToString(" AND ")
        return withSpecifications(jdbc.query(EQUIPMENT_SELECT + where, parameters, ::equipmentRow))
            .filter { record ->
                (keyword == null || listOfNotNull(record.name, record.model, record.brand?.name)
                    .any { normalizeEquipmentSearchText(it).contains(keyword) }) &&
                matches(record.specifications?.unitWeightGrams, filter.weightGrams, filter.minWeightGrams, filter.maxWeightGrams) &&
                    matches(record.specifications?.comfortTemperatureCelsius, filter.comfortTemperatureCelsius, filter.minComfortTemperatureCelsius, filter.maxComfortTemperatureCelsius)
            }.sortedBy { it.identity }
    }

    override fun findById(identity: String): CatalogEquipmentRecord? = withSpecifications(
        jdbc.query(EQUIPMENT_SELECT + " WHERE e.id = :id", mapOf("id" to identity), ::equipmentRow)
    ).singleOrNull()

    private fun withSpecifications(records: List<CatalogEquipmentRecord>): List<CatalogEquipmentRecord> {
        val specifications = mutableMapOf<String, CatalogSpecRecord>()
        records.groupBy { it.equipmentType }.forEach { (code, items) ->
            // Only the controlled registry can supply a table identifier; all data values remain bound.
            val type = requireNotNull(CatalogType.fromCode(code)) { "Catalog contains an unregistered equipment type" }
            items.map { it.identity }.chunked(500).forEach { ids ->
                jdbc.query("SELECT * FROM ${type.tableName} WHERE equipment_id IN (:ids)", mapOf("ids" to ids)) { rs, _ ->
                    specifications[rs.getString("equipment_id")] = specificationRow(rs, type)
                }
            }
        }
        return records.map { it.copy(specifications = specifications[it.identity]) }
    }

    private fun specificationRow(rs: ResultSet, type: CatalogType): CatalogSpecRecord = CatalogSpecRecord(
        variant = rs.getString("variant"),
        unitLabel = rs.getString("unit_label"),
        unitWeightGrams = rs.nullableDouble("unit_weight_grams"),
        weightScope = rs.getString("weight_scope"),
        capacityLiters = if (type == CatalogType.BACKPACK) rs.nullableDouble("capacity_liters") else null,
        capacityPersons = if (type == CatalogType.TENT) (rs.getObject("capacity_persons") as Number?)?.toInt() else null,
        comfortTemperatureCelsius = if (type == CatalogType.SLEEPING_BAG) rs.nullableDouble("comfort_temperature_celsius") else null,
        limitTemperatureCelsius = if (type == CatalogType.SLEEPING_BAG) rs.nullableDouble("limit_temperature_celsius") else null,
        padType = if (type == CatalogType.SLEEPING_PAD) rs.getString("pad_type") else null,
        rValue = if (type == CatalogType.SLEEPING_PAD) rs.nullableDouble("r_value") else null,
        lengthCm = if (type == CatalogType.SLEEPING_PAD) rs.nullableDouble("length_cm") else null,
        maxWidthCm = if (type == CatalogType.SLEEPING_PAD) rs.nullableDouble("max_width_cm") else null,
        thicknessCm = if (type == CatalogType.SLEEPING_PAD) rs.nullableDouble("thickness_cm") else null
    )

    private fun equipmentRow(rs: ResultSet, rowNumber: Int): CatalogEquipmentRecord = CatalogEquipmentRecord(
        identity = rs.getString("id"), name = rs.getString("name"), category = rs.getString("category"),
        equipmentType = rs.getString("equipment_type"), model = rs.getString("model"),
        brand = rs.getString("brand_id")?.let { CatalogBrandRecord(it, rs.getString("brand_name")) }
    )

    private fun ResultSet.nullableDouble(column: String): Double? = (getObject(column) as Number?)?.toDouble()

    private fun matches(value: Double?, exact: Double?, lower: Double?, upper: Double?): Boolean {
        if (exact == null && lower == null && upper == null) return true
        if (value == null) return false
        return (exact == null || value == exact) && (lower == null || value >= lower) && (upper == null || value <= upper)
    }

    companion object {
        private const val EQUIPMENT_SELECT = "SELECT e.id, e.name, e.category, e.equipment_type, e.brand_id, e.model, b.name AS brand_name FROM equipment e LEFT JOIN brand b ON b.id = e.brand_id"
    }
}
