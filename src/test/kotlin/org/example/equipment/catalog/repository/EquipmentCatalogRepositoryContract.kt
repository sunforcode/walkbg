package org.example.equipment.catalog.repository

import org.example.equipment.catalog.model.CatalogFilter
import org.example.equipment.catalog.model.CatalogType
import org.flywaydb.core.Flyway
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable
import org.springframework.core.io.ByteArrayResource
import org.springframework.dao.DataAccessException
import org.springframework.jdbc.core.JdbcTemplate
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate
import org.springframework.jdbc.datasource.DriverManagerDataSource
import org.springframework.jdbc.datasource.init.ResourceDatabasePopulator
import java.util.UUID
import javax.sql.DataSource

/** The same data invariants and read semantics execute against H2 and real MySQL. */
abstract class EquipmentCatalogRepositoryContract {
    protected lateinit var jdbc: JdbcTemplate
    private lateinit var repository: EquipmentCatalogRepository
    private val equipmentIds = mutableListOf<String>()
    private val brandIds = mutableListOf<String>()
    private val prefix = "ct-${UUID.randomUUID().toString().take(8)}-"

    abstract fun dataSource(): DataSource

    @BeforeEach
    fun setUp() {
        val source = dataSource()
        jdbc = JdbcTemplate(source)
        repository = JdbcEquipmentCatalogRepository(NamedParameterJdbcTemplate(source))
    }

    @AfterEach
    fun tearDown() {
        equipmentIds.forEach { jdbc.update("DELETE FROM equipment WHERE id = ?", it) }
        brandIds.forEach { jdbc.update("DELETE FROM brand WHERE id = ?", it) }
    }

    @Test
    fun `every controlled type has direct readable specifications and rejects wrong type or duplicate ownership`() {
        assertEquals(63, CatalogType.entries.size)
        CatalogType.entries.forEach { type ->
            val id = equipment(type.code, type.code, type.allowedCategories.first())
            specs(type.tableName, id)
            val record = requireNotNull(repository.findById(id))
            assertEquals(type.code, record.equipmentType)
            assertEquals(125.5, record.specifications?.unitWeightGrams)
            assertEquals("套", record.specifications?.unitLabel)
            assertThrows(DataAccessException::class.java) { specs(type.tableName, id) }
            assertThrows(DataAccessException::class.java) { specs(type.tableName, "${prefix}missing") }
            val wrong = CatalogType.entries.first { it != type }
            assertThrows(DataAccessException::class.java) { specs(wrong.tableName, id) }
            assertThrows(DataAccessException::class.java) {
                specs(wrong.tableName, id, ", spec_type", ", '${type.code}'")
            }
            jdbc.update("DELETE FROM equipment WHERE id = ?", id)
            assertEquals(0, jdbc.queryForObject("SELECT COUNT(*) FROM ${type.tableName} WHERE equipment_id = ?", Int::class.java, id))
        }
    }

    @Test
    fun `identity and all type codes are case sensitive while names and models are not unique keys`() {
        val lower = equipment("a", "backpack", "backpack")
        val upper = equipment("A", "backpack", "backpack")
        assertEquals(lower, repository.findById(lower)?.identity)
        assertEquals(upper, repository.findById(upper)?.identity)
        assertNull(repository.findById("$lower "))
        assertNull(repository.findById("$upper "))
        assertThrows(DataAccessException::class.java) { equipment("bad-case", "Backpack", "backpack") }
        assertThrows(DataAccessException::class.java) { equipment("bad-category-case", "backpack", "Backpack") }
        assertTrue(repository.findItems(CatalogFilter(equipmentType = "Backpack")).isEmpty())
    }

    @Test
    fun `brand associations are optional resolve once and prevent deleting a referenced brand`() {
        val brand = brand("brand", "同名品牌")
        val other = brand("brand-other", "同名品牌")
        val owned = equipment("known-brand", "backpack", "backpack", brand)
        val anonymous = equipment("no-brand", "backpack", "backpack")
        assertEquals("同名品牌", repository.findById(owned)?.brand?.name)
        assertNull(repository.findById(anonymous)?.brand)
        assertEquals(listOf(owned), repository.findItems(CatalogFilter(brandId = brand)).map { it.identity })
        assertTrue(repository.findItems(CatalogFilter(brandId = "' OR 1=1 --")).isEmpty())
        assertTrue(repository.findItems(CatalogFilter(brandId = "$brand ")).isEmpty())
        assertEquals(listOf(brand, other).sorted(), repository.findBrands().filter { it.identity.startsWith(prefix) }.map { it.identity })
        assertThrows(DataAccessException::class.java) { jdbc.update("DELETE FROM brand WHERE id = ?", brand) }
        assertThrows(DataAccessException::class.java) { equipment("missing-brand", "backpack", "backpack", "${prefix}absent") }
        assertNull(repository.findById("${prefix}absent"))
    }

    @Test
    fun `core parameters and zero negative temperatures remain distinct from missing values`() {
        val pack = equipment("pack", "backpack", "backpack")
        specs("backpack_specs", pack, ", capacity_liters", ", 65.5")
        val tent = equipment("tent", "tent", "tent")
        specs("tent_specs", tent, ", capacity_persons", ", 2")
        val bag = equipment("bag", "sleeping_bag", "sleeping")
        specs("sleeping_bag_specs", bag, ", comfort_temperature_celsius, limit_temperature_celsius", ", 0, -8.5")
        val pad = equipment("pad", "sleeping_pad", "sleeping")
        specs("sleeping_pad_specs", pad, ", pad_type, r_value, length_cm, max_width_cm, thickness_cm", ", 'air', 0, 183, 64, 7.5")
        assertEquals(65.5, repository.findById(pack)?.specifications?.capacityLiters)
        assertEquals(2, repository.findById(tent)?.specifications?.capacityPersons)
        assertEquals(0.0, repository.findById(bag)?.specifications?.comfortTemperatureCelsius)
        assertEquals(-8.5, repository.findById(bag)?.specifications?.limitTemperatureCelsius)
        val padSpecs = requireNotNull(repository.findById(pad)?.specifications)
        assertEquals("air", padSpecs.padType)
        assertEquals(0.0, padSpecs.rValue)
        assertEquals(183.0, padSpecs.lengthCm)
        assertEquals(64.0, padSpecs.maxWidthCm)
        assertEquals(7.5, padSpecs.thicknessCm)
        assertNull(padSpecs.comfortTemperatureCelsius)
    }

    @Test
    fun `absent specs unknown weights and one missing temperature are retained without numeric filters`() {
        val absent = equipment("absent-spec", "sleeping_bag", "sleeping")
        val missingWeight = equipment("unknown-weight", "sleeping_bag", "sleeping")
        jdbc.update("INSERT INTO sleeping_bag_specs (equipment_id, unit_label, limit_temperature_celsius) VALUES (?, '件', -5)", missingWeight)
        assertNull(repository.findById(absent)?.specifications)
        val spec = requireNotNull(repository.findById(missingWeight)?.specifications)
        assertNull(spec.unitWeightGrams)
        assertNull(spec.comfortTemperatureCelsius)
        assertEquals(-5.0, spec.limitTemperatureCelsius)
        val all = repository.findItems(CatalogFilter()).map { it.identity }
        assertTrue(all.containsAll(listOf(absent, missingWeight)))
        assertTrue(repository.findItems(CatalogFilter(minWeightGrams = 0.0)).none { it.identity in listOf(absent, missingWeight) })
        assertTrue(repository.findItems(CatalogFilter(minComfortTemperatureCelsius = -10.0)).none { it.identity in listOf(absent, missingWeight) })
    }

    @Test
    fun `combined inclusive exact and range filters keep only matching known numbers in stable identity order`() {
        val brand = brand("filter-brand", "Filter")
        val ids = listOf("z", "A", "a").mapIndexed { i, suffix ->
            val id = equipment("filter-$suffix", "sleeping_bag", "sleeping", brand)
            jdbc.update("INSERT INTO sleeping_bag_specs (equipment_id, unit_label, unit_weight_grams, weight_scope, comfort_temperature_celsius) VALUES (?, '件', ?, '含包装', ?)", id, 100.0 + i * 50, -5.0 + i * 5)
            id
        }
        assertEquals(ids.sorted(), repository.findItems(CatalogFilter(category = "sleeping", brandId = brand, equipmentType = "sleeping_bag", minWeightGrams = 100.0, maxWeightGrams = 200.0, minComfortTemperatureCelsius = -5.0, maxComfortTemperatureCelsius = 5.0)).map { it.identity })
        assertEquals(listOf(ids[1]), repository.findItems(CatalogFilter(brandId = brand, weightGrams = 150.0, comfortTemperatureCelsius = 0.0)).map { it.identity })
        assertTrue(repository.findItems(CatalogFilter(category = "backpack", brandId = brand)).isEmpty())
        assertTrue(repository.findItems(CatalogFilter(equipmentType = "sleeping_bag' OR 1=1 --")).isEmpty())
    }

    @Test
    fun `literal keyword matches one normalized field and combines with other filters`() {
        val brand = brand("keyword-brand", " ALPHA\u3000  BRAND ")
        val nameMatch = equipment("keyword-name", "backpack", "backpack")
        val modelMatch = equipment("keyword-model", "backpack", "backpack")
        val brandMatch = equipment("keyword-brand-item", "sleeping_bag", "sleeping", brand)
        jdbc.update("UPDATE equipment SET name = ' TENT_50% ', model = NULL WHERE id = ?", nameMatch)
        jdbc.update("UPDATE equipment SET model = 'TENT_50%' WHERE id = ?", modelMatch)
        assertEquals(listOf(nameMatch, modelMatch).sorted(), repository.findItems(CatalogFilter(keyword = "  tent_50%  ")).map { it.identity })
        assertEquals(listOf(brandMatch), repository.findItems(CatalogFilter(keyword = " alpha brand ", category = "sleeping")).map { it.identity })
        assertTrue(repository.findItems(CatalogFilter(keyword = "alpha brand", category = "backpack")).isEmpty())
        assertTrue(repository.findItems(CatalogFilter(keyword = "tentX50a")).isEmpty())
        assertTrue(repository.findItems(CatalogFilter(keyword = "相同名称 tent_50%")).isEmpty())
        jdbc.update("UPDATE equipment SET name = 'Ä', model = NULL WHERE id = ?", nameMatch)
        assertTrue(repository.findItems(CatalogFilter(keyword = "ä")).isEmpty())
    }

    @Test
    fun `database enforces category mapping single storage category and prevents type mutation with specs`() {
        assertThrows(DataAccessException::class.java) { equipment("bad-map", "sleeping_bag", "backpack") }
        assertThrows(DataAccessException::class.java) { equipment("unknown-type", "unknown", "miscellaneous") }
        assertThrows(DataAccessException::class.java) { equipment("unknown-category", "food", "unknown") }
        equipment("clothing-storage", "storage_bag", "clothing")
        equipment("food-storage", "storage_bag", "dining_equipment")
        assertThrows(DataAccessException::class.java) { equipment("bad-storage", "storage_bag", "food") }
        val id = equipment("fixed-type", "sleeping_bag", "sleeping")
        specs("sleeping_bag_specs", id)
        assertThrows(DataAccessException::class.java) { jdbc.update("UPDATE equipment SET equipment_type = 'sleeping_pad' WHERE id = ?", id) }
    }

    @Test
    fun `every specification table enforces units positive scoped weight and optional nonblank text`() {
        CatalogType.entries.forEach { type ->
            val id = equipment("invalid-${type.code}", type.code, type.allowedCategories.first())
            listOf("0", "-1").forEach { value ->
                assertThrows(DataAccessException::class.java) { jdbc.update("INSERT INTO ${type.tableName} (equipment_id, unit_label, unit_weight_grams, weight_scope) VALUES (?, '套', $value, '完整')", id) }
            }
            assertThrows(DataAccessException::class.java) { jdbc.update("INSERT INTO ${type.tableName} (equipment_id, unit_label, unit_weight_grams) VALUES (?, '套', 1)", id) }
            assertThrows(DataAccessException::class.java) { jdbc.update("INSERT INTO ${type.tableName} (equipment_id, unit_label, unit_weight_grams, weight_scope) VALUES (?, '套', 1, '  ')", id) }
            assertThrows(DataAccessException::class.java) { jdbc.update("INSERT INTO ${type.tableName} (equipment_id, unit_label, variant) VALUES (?, '套', '  ')", id) }
            assertThrows(DataAccessException::class.java) { jdbc.update("INSERT INTO ${type.tableName} (equipment_id, unit_label) VALUES (?, '千克')", id) }
        }
    }

    @Test
    fun `core parameter constraints reject invalid values without inventing temperature order rules`() {
        val tests = listOf(
            Triple("backpack", "capacity_liters", "0"),
            Triple("tent", "capacity_persons", "0"),
            Triple("sleeping_pad", "r_value", "-0.1"),
            Triple("sleeping_pad", "length_cm", "0"),
            Triple("sleeping_pad", "max_width_cm", "-1"),
            Triple("sleeping_pad", "thickness_cm", "0"),
            Triple("sleeping_pad", "pad_type", "'unknown'")
        )
        tests.forEachIndexed { index, (code, field, value) ->
            val type = requireNotNull(CatalogType.fromCode(code))
            val id = equipment("bad-parameter-$index", code, type.allowedCategories.first())
            assertThrows(DataAccessException::class.java) { specs(type.tableName, id, ", $field", ", $value") }
        }
        val tent = equipment("tent-unit", "tent", "tent")
        assertThrows(DataAccessException::class.java) { jdbc.update("INSERT INTO tent_specs (equipment_id, unit_label) VALUES (?, '件')", tent) }
        val bag = equipment("unrestricted-temperatures", "sleeping_bag", "sleeping")
        specs("sleeping_bag_specs", bag, ", comfort_temperature_celsius, limit_temperature_celsius", ", -10, 10")
        assertEquals(-10.0, repository.findById(bag)?.specifications?.comfortTemperatureCelsius)
    }

    @Test
    fun `nonfinite numerical values never become unknown or valid parameters`() {
        val fields = mapOf(
            "backpack" to listOf("unit_weight_grams", "capacity_liters"),
            "sleeping_bag" to listOf("comfort_temperature_celsius", "limit_temperature_celsius"),
            "sleeping_pad" to listOf("r_value", "length_cm", "max_width_cm", "thickness_cm")
        )
        fields.forEach { (code, columns) ->
            val type = requireNotNull(CatalogType.fromCode(code))
            val id = equipment("nonfinite-$code", code, type.allowedCategories.first())
            columns.forEach { column ->
                listOf(Double.NaN, Double.POSITIVE_INFINITY, Double.NEGATIVE_INFINITY).forEach { invalid ->
                    assertThrows(DataAccessException::class.java) {
                        jdbc.update("INSERT INTO ${type.tableName} (equipment_id, unit_label, weight_scope, $column) VALUES (?, '套', '完整', ?)", id, invalid)
                    }
                }
            }
        }
    }

    @Test
    fun `blank text and whitespace only measurement scope are invalid`() {
        val id = equipment("blank-text", "backpack", "backpack")
        listOf("", "   ", "\t\r\n").forEach { blank ->
            assertThrows(DataAccessException::class.java) { jdbc.update("UPDATE equipment SET name = ? WHERE id = ?", blank, id) }
            assertThrows(DataAccessException::class.java) { jdbc.update("UPDATE equipment SET model = ? WHERE id = ?", blank, id) }
            assertThrows(DataAccessException::class.java) { jdbc.update("INSERT INTO backpack_specs (equipment_id, unit_label, unit_weight_grams, weight_scope) VALUES (?, '件', 1, ?)", id, blank) }
        }
    }

    private fun equipment(suffix: String, type: String, category: String, brand: String? = null): String {
        val id = prefix + suffix
        jdbc.update("INSERT INTO equipment (id, name, category, equipment_type, brand_id, model) VALUES (?, '相同名称', ?, ?, ?, '相同型号')", id, category, type, brand)
        equipmentIds += id
        return id
    }

    private fun brand(suffix: String, name: String): String {
        val id = prefix + suffix
        jdbc.update("INSERT INTO brand (id, name) VALUES (?, ?)", id, name)
        brandIds += id
        return id
    }

    private fun specs(table: String, id: String, columns: String = "", values: String = "") {
        jdbc.update("INSERT INTO $table (equipment_id, unit_label, unit_weight_grams, weight_scope$columns) VALUES (?, '套', 125.5, '完整配置含包装'$values)", id)
    }
}

class JdbcEquipmentCatalogRepositoryTest : EquipmentCatalogRepositoryContract() {
    override fun dataSource(): DataSource {
        val source = DriverManagerDataSource("jdbc:h2:mem:catalog-${UUID.randomUUID()};MODE=MySQL;DB_CLOSE_DELAY=-1", "sa", "")
        val original = requireNotNull(javaClass.classLoader.getResource("db/migration/V15__add_equipment_catalog.sql")).readText()
        // H2 does not implement MySQL collation declarations; its VARCHAR comparisons are already case sensitive.
        val compatible = original.replace(" DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_bin", "")
        ResourceDatabasePopulator(ByteArrayResource(compatible.toByteArray(Charsets.UTF_8))).execute(source)
        return source
    }
}

@EnabledIfEnvironmentVariable(named = "WALK_CATALOG_MYSQL_URL", matches = ".+")
class EquipmentCatalogMySqlIntegrationTest : EquipmentCatalogRepositoryContract() {
    override fun dataSource(): DataSource {
        val url = requireNotNull(System.getenv("WALK_CATALOG_MYSQL_URL"))
        require(Regex("jdbc:mysql://127\\.0\\.0\\.1:[0-9]+/walk_catalog_test(?:\\?.*)?").matches(url)) {
            "Only an explicitly supplied isolated localhost walk_catalog_test database may run this fixture"
        }
        val source = DriverManagerDataSource(url, System.getenv("WALK_CATALOG_MYSQL_USERNAME") ?: "root", System.getenv("WALK_CATALOG_MYSQL_PASSWORD") ?: "")
        Flyway.configure().dataSource(source).locations("classpath:db/migration").target("15").cleanDisabled(true).load().migrate()
        return source
    }

    @Test
    fun `real MySQL applies all migrations through V15 with clean validation`() {
        assertEquals(15, jdbc.queryForObject("SELECT COUNT(*) FROM flyway_schema_history WHERE success = 1 AND CAST(version AS UNSIGNED) BETWEEN 1 AND 15", Int::class.java))
        assertEquals(0, jdbc.queryForObject("SELECT COUNT(*) FROM flyway_schema_history WHERE success = 0", Int::class.java))
    }
}
