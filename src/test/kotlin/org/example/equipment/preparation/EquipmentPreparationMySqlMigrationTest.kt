package org.example.equipment.preparation

import org.flywaydb.core.Flyway
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable
import org.springframework.dao.DataAccessException
import org.springframework.jdbc.core.JdbcTemplate
import org.springframework.jdbc.datasource.DriverManagerDataSource
import java.net.URI
import java.util.UUID

@EnabledIfEnvironmentVariable(named = "WALK_PREPARATION_MYSQL_URL", matches = ".+")
class EquipmentPreparationMySqlMigrationTest {
    @Test
    fun `V15 legacy rows survive V16 and database rejects invalid preparation facts`() {
        val url = System.getenv("WALK_PREPARATION_MYSQL_URL")
        val uri = URI(url.removePrefix("jdbc:"))
        require(uri.host in setOf("127.0.0.1", "localhost") && uri.path == "/walk_preparation_test")
        val user = System.getenv("WALK_PREPARATION_MYSQL_USERNAME") ?: "root"
        val password = System.getenv("WALK_PREPARATION_MYSQL_PASSWORD") ?: ""
        val admin = JdbcTemplate(DriverManagerDataSource(url, user, password))
        val database = "walk_preparation_migration_" + UUID.randomUUID().toString().replace("-", "")
        admin.execute("CREATE DATABASE `$database` CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_bin")
        try {
            val source = DriverManagerDataSource(url.replace("/walk_preparation_test", "/$database"), user, password)
            val jdbc = JdbcTemplate(source)
            Flyway.configure().dataSource(source).target("15").cleanDisabled(true).load().migrate()
            jdbc.update("INSERT INTO personal_equipment (id,name,owned_quantity,unit_weight_grams,created_at,updated_at) VALUES ('legacy','旧装备',2,123,CURRENT_TIMESTAMP(6),CURRENT_TIMESTAMP(6))")
            jdbc.update("""INSERT INTO personal_trips (id,name,first_generated_at,lifecycle_state,departure_city,start_date,end_date,total_day_count,hiking_day_count,revision,frozen_route_basis_json,updated_at)
                VALUES ('trip','旧行程',CURRENT_TIMESTAMP(6),'active','成都','2099-01-01','2099-01-01',1,1,'old','{}',CURRENT_TIMESTAMP(6))""")
            jdbc.update("INSERT INTO personal_trip_equipment_snapshots VALUES ('snapshot','trip',1,246,0,1,0)")
            jdbc.update("""INSERT INTO personal_trip_equipment_items (id,snapshot_id,display_order,name,normalized_name,quantity,unit_weight_grams,source,ownership_status)
                VALUES ('item','snapshot',1,'旧装备','旧装备',2,123,'user_added','owned')""")
            val flyway = Flyway.configure().dataSource(source).cleanDisabled(true).load()
            assertEquals(1, flyway.migrate().migrationsExecuted)
            assertEquals("16", flyway.info().current().version.version)
            assertEquals(0, flyway.migrate().migrationsExecuted)
            val legacy = jdbc.queryForMap("SELECT * FROM personal_equipment WHERE id='legacy'")
            assertEquals(123.0, (legacy["unit_weight_grams"] as Number).toDouble())
            listOf("category", "equipment_type", "variant", "unit_label", "weight_scope", "unit_weight_source", "catalog_equipment_id").forEach { assertNull(legacy[it], it) }
            val item = jdbc.queryForMap("SELECT * FROM personal_trip_equipment_items WHERE id='item'")
            listOf("category", "backpack_quantity", "worn_quantity", "handheld_quantity", "supply_kind", "packed_quantity").forEach { assertNull(item[it], it) }
            assertEquals(246.0, jdbc.queryForObject("SELECT known_total_weight_grams FROM personal_trip_equipment_snapshots", Double::class.java))
            jdbc.update("UPDATE personal_equipment SET unit_weight_grams=123.25 WHERE id='legacy'")
            jdbc.update("UPDATE personal_trip_equipment_items SET unit_weight_grams=123.25 WHERE id='item'")
            jdbc.update("UPDATE personal_trip_equipment_snapshots SET known_total_weight_grams=246.5 WHERE id='snapshot'")
            assertEquals(123.25, jdbc.queryForObject("SELECT unit_weight_grams FROM personal_equipment", Double::class.java))
            assertEquals(246.5, jdbc.queryForObject("SELECT known_total_weight_grams FROM personal_trip_equipment_snapshots", Double::class.java))
            fun rejects(sql: String) { assertThrows(DataAccessException::class.java) { jdbc.update(sql) } }
            rejects("UPDATE personal_equipment SET unit_weight_source='catalog_reference' WHERE id='legacy'")
            rejects("UPDATE personal_equipment SET equipment_type='backpack', category='tent' WHERE id='legacy'")
            rejects("UPDATE personal_equipment SET category='Backpack' WHERE id='legacy'")
            rejects("UPDATE personal_equipment SET unit_label='未知' WHERE id='legacy'")
            rejects("UPDATE personal_trip_equipment_items SET backpack_quantity=1 WHERE id='item'")
            rejects("UPDATE personal_trip_equipment_items SET backpack_quantity=1,worn_quantity=0,handheld_quantity=0 WHERE id='item'")
            rejects("UPDATE personal_trip_equipment_items SET packed_quantity=1 WHERE id='item'")
            jdbc.update("UPDATE personal_trip_equipment_items SET backpack_quantity=1,worn_quantity=1,handheld_quantity=0,packed_quantity=1 WHERE id='item'")
            rejects("UPDATE personal_trip_equipment_items SET packed_quantity=2 WHERE id='item'")
            rejects("UPDATE personal_trip_equipment_items SET category='food',equipment_type='food' WHERE id='item'")
            jdbc.update("""UPDATE personal_trip_equipment_items SET category='food',equipment_type='food',quantity=1,unit_label='份',weight_scope='含包装',
                supply_kind='food',supply_amount=500.5,supply_amount_unit='gram',supply_container_mode='included',
                backpack_quantity=1,worn_quantity=0,handheld_quantity=0,ownership_status='unconfirmed_owned' WHERE id='item'""")
            rejects("UPDATE personal_trip_equipment_items SET supply_amount_unit='milliliter' WHERE id='item'")
            rejects("UPDATE personal_trip_equipment_items SET weight_scope=NULL WHERE id='item'")
            rejects("UPDATE personal_trip_equipment_items SET ownership_status='owned' WHERE id='item'")
            rejects("UPDATE personal_trip_equipment_items SET backpack_quantity=0,worn_quantity=1,packed_quantity=0 WHERE id='item'")
            jdbc.update("INSERT INTO equipment(id,name,category,equipment_type) VALUES ('catalog','背包','backpack','backpack')")
            jdbc.update("UPDATE personal_equipment SET catalog_equipment_id='catalog',unit_weight_source='catalog_reference',unit_label='件',weight_scope='整件' WHERE id='legacy'")
            rejects("DELETE FROM equipment WHERE id='catalog'")
            assertTrue(jdbc.queryForObject("SELECT COUNT(*) FROM information_schema.tables WHERE table_schema=? AND table_name='equipment_command_receipts'", Int::class.java, database) == 1)
        } finally {
            // Only this test's randomly named schema is removed; existing development/test data is never cleaned.
            admin.execute("DROP DATABASE `$database`")
        }
    }
}
