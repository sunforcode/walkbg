package org.example.trip.personal.service

import org.example.trip.personal.dto.MigrateTripCommand
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.jdbc.core.JdbcTemplate
import org.springframework.test.context.ActiveProfiles
import org.springframework.test.context.DynamicPropertyRegistry
import org.springframework.test.context.DynamicPropertySource

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.MOCK)
@ActiveProfiles("prod")
@EnabledIfEnvironmentVariable(named = "WALK_MYSQL_INTEGRATION_TEST", matches = "true")
class PersonalTripMigrationRollbackMySqlIntegrationTest {
    @Autowired
    private lateinit var service: PersonalTripApplicationService

    @Autowired
    private lateinit var jdbcTemplate: JdbcTemplate

    @BeforeEach
    fun seedAggregateAndFailureTrigger() {
        cleanFixture()
        jdbcTemplate.update(
            """
            INSERT INTO users (
                status, created_at, updated_at, phone, nickname, username, id, email, password
            ) VALUES (0, CURRENT_TIMESTAMP(6), CURRENT_TIMESTAMP(6), ?, ?, ?, ?, ?, ?)
            """.trimIndent(),
            PHONE,
            "回滚测试",
            ACCOUNT_ID,
            ACCOUNT_ID,
            "$ACCOUNT_ID@example.invalid",
            "not-used"
        )
        jdbcTemplate.update(
            """
            INSERT INTO routes (
                difficulty, is_favorite, is_loop, popularity, route_type, status, usage_count,
                created_at, updated_at, created_by, id, region, name, is_deleted
            ) VALUES (2, b'0', b'0', 0, 0, 1, 0, CURRENT_TIMESTAMP(6), CURRENT_TIMESTAMP(6), ?, ?, ?, ?, b'0')
            """.trimIndent(),
            ACCOUNT_ID,
            ROUTE_ID,
            "测试地区",
            "回滚测试路线"
        )
        insertRouteVersion(ADOPTED_VERSION_ID, "旧版", "旧路线", "旧起点", "旧终点")
        insertRouteVersion(TARGET_VERSION_ID, "新版", "新版路线", "新起点", "新终点")
        jdbcTemplate.update(
            "INSERT INTO route_version_publication_order (route_id, route_version_id, published_sequence) VALUES (?, ?, 1), (?, ?, 2)",
            ROUTE_ID,
            ADOPTED_VERSION_ID,
            ROUTE_ID,
            TARGET_VERSION_ID
        )
        jdbcTemplate.update(
            "INSERT INTO route_current_public_versions (route_id, route_version_id) VALUES (?, ?)",
            ROUTE_ID,
            TARGET_VERSION_ID
        )
        jdbcTemplate.update(
            "INSERT INTO public_route_collection (route_id, all_route_order, featured_order) VALUES (?, 1, NULL)",
            ROUTE_ID
        )
        jdbcTemplate.update(
            """
            INSERT INTO personal_trips (
                id, name, first_generated_at, lifecycle_state, departure_city, start_date, end_date,
                total_day_count, hiking_day_count, revision, frozen_route_basis_json, updated_at
            ) VALUES (?, ?, CURRENT_TIMESTAMP(6), 'active', ?, '2099-09-02', '2099-09-02', 1, 1, ?, ?, CURRENT_TIMESTAMP(6))
            """.trimIndent(),
            TRIP_ID,
            "旧路线 · 2099-09-02",
            "上海",
            OLD_REVISION,
            OLD_ROUTE_BASIS
        )
        jdbcTemplate.update(
            "INSERT INTO personal_trip_ownership (trip_id, account_id) VALUES (?, ?)",
            TRIP_ID,
            ACCOUNT_ID
        )
        jdbcTemplate.update(
            "INSERT INTO trip_frozen_route_versions (trip_id, route_version_id) VALUES (?, ?)",
            TRIP_ID,
            ADOPTED_VERSION_ID
        )
        jdbcTemplate.update(
            """
            INSERT INTO personal_trip_days (
                id, trip_id, day_number, date, primary_stage, hiking_day_number, content_json
            ) VALUES (?, ?, 1, '2099-09-02', '徒步', 1, ?)
            """.trimIndent(),
            OLD_DAY_ID,
            TRIP_ID,
            OLD_DAY_JSON
        )
        jdbcTemplate.update(
            """
            INSERT INTO personal_trip_equipment_snapshots (
                id, trip_id, item_count, known_total_weight_grams, missing_weight_item_count,
                owned_item_count, unconfirmed_owned_item_count
            ) VALUES (?, ?, 1, 100, 0, 0, 1)
            """.trimIndent(),
            OLD_SNAPSHOT_ID,
            TRIP_ID
        )
        jdbcTemplate.update(
            """
            INSERT INTO personal_trip_equipment_items (
                id, snapshot_id, display_order, name, normalized_name, quantity,
                unit_weight_grams, source, ownership_status, note
            ) VALUES (?, ?, 1, '旧装备', '旧装备', 1, 100, 'user_added', 'unconfirmed_owned', NULL)
            """.trimIndent(),
            OLD_ITEM_ID,
            OLD_SNAPSHOT_ID
        )
        jdbcTemplate.execute(
            """
            CREATE TRIGGER $FAILURE_TRIGGER
            BEFORE INSERT ON personal_trip_equipment_snapshots
            FOR EACH ROW
            BEGIN
                IF NEW.trip_id = '$TRIP_ID' THEN
                    SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT = 'forced migration snapshot failure';
                END IF;
            END
            """.trimIndent()
        )
    }

    @AfterEach
    fun cleanUp() {
        cleanFixture()
    }

    @Test
    fun `repository failure after destructive migration writes rolls back the complete aggregate`() {
        assertThrows(RuntimeException::class.java) {
            service.migrate(
                ACCOUNT_ID,
                TRIP_ID,
                "rollback-migration-key",
                MigrateTripCommand(OLD_REVISION, TARGET_VERSION_ID)
            )
        }

        assertEquals(
            ADOPTED_VERSION_ID,
            jdbcTemplate.queryForObject(
                "SELECT route_version_id FROM trip_frozen_route_versions WHERE trip_id = ?",
                String::class.java,
                TRIP_ID
            )
        )
        assertEquals(
            OLD_REVISION,
            jdbcTemplate.queryForObject(
                "SELECT revision FROM personal_trips WHERE id = ?",
                String::class.java,
                TRIP_ID
            )
        )
        assertEquals(
            OLD_ROUTE_BASIS,
            jdbcTemplate.queryForObject(
                "SELECT frozen_route_basis_json FROM personal_trips WHERE id = ?",
                String::class.java,
                TRIP_ID
            )
        )
        assertEquals(
            listOf(OLD_DAY_ID),
            jdbcTemplate.queryForList(
                "SELECT id FROM personal_trip_days WHERE trip_id = ? ORDER BY day_number",
                String::class.java,
                TRIP_ID
            )
        )
        assertEquals(
            listOf(OLD_SNAPSHOT_ID),
            jdbcTemplate.queryForList(
                "SELECT id FROM personal_trip_equipment_snapshots WHERE trip_id = ?",
                String::class.java,
                TRIP_ID
            )
        )
        assertEquals(
            listOf(OLD_ITEM_ID),
            jdbcTemplate.queryForList(
                "SELECT id FROM personal_trip_equipment_items WHERE snapshot_id = ? ORDER BY display_order",
                String::class.java,
                OLD_SNAPSHOT_ID
            )
        )
        assertEquals(
            0,
            jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM personal_trip_idempotency WHERE account_id = ? AND operation_name = ?",
                Int::class.java,
                ACCOUNT_ID,
                "migrate_trip:$TRIP_ID"
            )
        )
    }

    private fun insertRouteVersion(id: String, label: String, name: String, start: String, end: String) {
        jdbcTemplate.update(
            """
            INSERT INTO route_versions (
                id, route_id, version_label, route_type, name, region, start_name, end_name,
                estimated_duration_seconds, main_track_availability, main_track_reference_system,
                main_track_json, created_at
            ) VALUES (?, ?, ?, 'one_day', ?, '测试地区', ?, ?, 28800, 'valid', 'WGS84', ?, CURRENT_TIMESTAMP(6))
            """.trimIndent(),
            id,
            ROUTE_ID,
            label,
            name,
            start,
            end,
            "[[30.0,101.0],[30.1,101.1]]"
        )
    }

    private fun cleanFixture() {
        jdbcTemplate.execute("DROP TRIGGER IF EXISTS $FAILURE_TRIGGER")
        jdbcTemplate.update("DELETE FROM personal_trip_idempotency WHERE account_id = ?", ACCOUNT_ID)
        jdbcTemplate.update("DELETE FROM personal_trip_equipment_item_derivations WHERE trip_id = ?", TRIP_ID)
        jdbcTemplate.update("DELETE FROM personal_trip_equipment_suppressions WHERE trip_id = ?", TRIP_ID)
        jdbcTemplate.update(
            "DELETE FROM personal_trip_equipment_items WHERE snapshot_id IN (SELECT id FROM personal_trip_equipment_snapshots WHERE trip_id = ?)",
            TRIP_ID
        )
        jdbcTemplate.update("DELETE FROM personal_trip_equipment_snapshots WHERE trip_id = ?", TRIP_ID)
        jdbcTemplate.update("DELETE FROM personal_trip_days WHERE trip_id = ?", TRIP_ID)
        jdbcTemplate.update("DELETE FROM trip_frozen_route_versions WHERE trip_id = ?", TRIP_ID)
        jdbcTemplate.update("DELETE FROM personal_trip_ownership WHERE trip_id = ?", TRIP_ID)
        jdbcTemplate.update("DELETE FROM personal_trips WHERE id = ?", TRIP_ID)
        jdbcTemplate.update("DELETE FROM route_current_public_versions WHERE route_id = ?", ROUTE_ID)
        jdbcTemplate.update("DELETE FROM route_version_equipment_suggestions WHERE route_id = ?", ROUTE_ID)
        jdbcTemplate.update("DELETE FROM logical_equipment_suggestion_identities WHERE route_id = ?", ROUTE_ID)
        jdbcTemplate.update("DELETE FROM route_version_publication_order WHERE route_id = ?", ROUTE_ID)
        jdbcTemplate.update("DELETE FROM public_route_collection WHERE route_id = ?", ROUTE_ID)
        jdbcTemplate.update("DELETE FROM route_versions WHERE route_id = ?", ROUTE_ID)
        jdbcTemplate.update("DELETE FROM routes WHERE id = ?", ROUTE_ID)
        jdbcTemplate.update("DELETE FROM account_sessions WHERE account_id = ?", ACCOUNT_ID)
        jdbcTemplate.update("DELETE FROM users WHERE id = ?", ACCOUNT_ID)
    }

    companion object {
        private const val ACCOUNT_ID = "rollback-account"
        private const val PHONE = "+8613800000001"
        private const val ROUTE_ID = "rollback-route"
        private const val ADOPTED_VERSION_ID = "rollback-version-1"
        private const val TARGET_VERSION_ID = "rollback-version-2"
        private const val TRIP_ID = "rollback-trip"
        private const val OLD_REVISION = "rollback-revision-1"
        private const val OLD_DAY_ID = "rollback-day-1"
        private const val OLD_SNAPSHOT_ID = "rollback-snapshot-1"
        private const val OLD_ITEM_ID = "rollback-item-1"
        private const val FAILURE_TRIGGER = "fail_rollback_trip_snapshot_insert"
        private val OLD_ROUTE_BASIS = """
            {"routeName":"旧路线","routeType":"one_day","region":"测试地区","start":{"name":"旧起点"},"end":{"name":"旧终点"},"estimatedDuration":{"seconds":28800.0},"mainTrackPath":[{"latitude":30.0,"longitude":101.0,"referenceSystem":"WGS84"}],"versionLabel":"旧版"}
        """.trimIndent()
        private val OLD_DAY_JSON = """
            {"identity":"rollback-day-1","dayNumber":1,"date":"2099-09-02","primaryStage":"徒步","hikingDayNumber":1,"actions":[{"sequence":1,"actionType":"hike"}],"weather":{"condition":{"confidence":{"status":"unavailable","category":"dynamic_external_information"}},"temperatureRange":{"confidence":{"status":"unavailable","category":"dynamic_external_information"}},"precipitation":{"confidence":{"status":"unavailable","category":"dynamic_external_information"}},"wind":{"confidence":{"status":"unavailable","category":"dynamic_external_information"}},"placeContext":{"confidence":{"status":"unavailable","category":"dynamic_external_information"}},"routeSectionContext":{"confidence":{"status":"unavailable","category":"dynamic_external_information"}}}}
        """.trimIndent()

        @JvmStatic
        @DynamicPropertySource
        fun mysqlProperties(registry: DynamicPropertyRegistry) {
            registry.add("spring.datasource.url") { requiredEnvironment("WALK_MYSQL_TEST_URL") }
            registry.add("spring.datasource.username") { requiredEnvironment("WALK_MYSQL_TEST_USERNAME") }
            registry.add("spring.datasource.password") { requiredEnvironment("WALK_MYSQL_TEST_PASSWORD") }
            registry.add("spring.jpa.hibernate.ddl-auto") { "validate" }
            registry.add("spring.flyway.enabled") { "true" }
            registry.add("kml-agent.base-url") { "http://127.0.0.1:18001" }
            registry.add("kml-agent.enabled") { "false" }
            registry.add("jwt.secret") { "walk-mysql-integration-test-secret-at-least-256-bits" }
            registry.add("cors.allowed-origins") { "http://127.0.0.1" }
            registry.add("account.avatar-media.directory") { "/private/tmp/walk-mysql-integration-avatar-media" }
        }

        private fun requiredEnvironment(name: String): String =
            requireNotNull(System.getenv(name)) { "$name must be set when WALK_MYSQL_INTEGRATION_TEST=true" }
    }
}
