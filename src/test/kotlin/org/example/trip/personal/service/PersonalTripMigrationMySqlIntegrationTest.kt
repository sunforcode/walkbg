package org.example.trip.personal.service

import org.example.config.AppInitializer
import org.example.trip.personal.dto.MigrateTripCommand
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.boot.test.mock.mockito.MockBean
import org.springframework.jdbc.core.JdbcTemplate
import org.springframework.test.context.ActiveProfiles

@SpringBootTest(
    webEnvironment = SpringBootTest.WebEnvironment.MOCK,
    properties = [
        "spring.datasource.url=\${WALK_MYSQL8_URL}",
        "spring.datasource.username=\${WALK_MYSQL8_USERNAME:root}",
        "spring.datasource.password=\${WALK_MYSQL8_PASSWORD:}",
        "spring.jpa.hibernate.ddl-auto=validate",
        "spring.flyway.enabled=true",
        "spring.sql.init.mode=never",
        "kml-agent.base-url=http://127.0.0.1:65535",
        "kml-agent.enabled=false",
        "jwt.secret=release-candidate-test-secret-key-at-least-256-bits-long",
        "cors.allowed-origins=",
        "account.avatar-media.directory=/tmp/walkbg-mysql8-integration-avatar"
    ]
)
@ActiveProfiles("prod")
@EnabledIfEnvironmentVariable(named = "WALK_MYSQL8_URL", matches = ".+")
class PersonalTripMigrationMySqlIntegrationTest {
    @Autowired
    private lateinit var service: PersonalTripApplicationService

    @Autowired
    private lateinit var jdbcTemplate: JdbcTemplate

    @MockBean
    private lateinit var appInitializer: AppInitializer

    @BeforeEach
    fun setUp() {
        cleanFixture()
        jdbcTemplate.update(
            """
            INSERT INTO users (
                id, username, email, password, status, created_at, updated_at
            ) VALUES (?, ?, ?, ?, 0, CURRENT_TIMESTAMP(6), CURRENT_TIMESTAMP(6))
            """.trimIndent(),
            ACCOUNT_ID,
            "rollback-user",
            "rollback-user@example.test",
            "not-used"
        )
        jdbcTemplate.update(
            """
            INSERT INTO routes (
                id, name, status, popularity, usage_count, is_loop, is_favorite,
                created_by, created_at, updated_at, is_deleted
            ) VALUES (?, ?, 1, 0, 0, b'1', b'0', ?, CURRENT_TIMESTAMP(6), CURRENT_TIMESTAMP(6), b'0')
            """.trimIndent(),
            ROUTE_ID,
            "Rollback Route",
            ACCOUNT_ID
        )
        insertRouteVersion(OLD_VERSION_ID, "old")
        insertRouteVersion(TARGET_VERSION_ID, "target")
        jdbcTemplate.update(
            "INSERT INTO route_version_publication_order (route_id, route_version_id, published_sequence) VALUES (?, ?, 1), (?, ?, 2)",
            ROUTE_ID,
            OLD_VERSION_ID,
            ROUTE_ID,
            TARGET_VERSION_ID
        )
        jdbcTemplate.update(
            "INSERT INTO route_current_public_versions (route_id, route_version_id) VALUES (?, ?)",
            ROUTE_ID,
            TARGET_VERSION_ID
        )
        jdbcTemplate.update(
            """
            INSERT INTO personal_trips (
                id, name, first_generated_at, lifecycle_state, departure_city,
                start_date, end_date, total_day_count, hiking_day_count, revision,
                frozen_route_basis_json, weather_overview_json, important_notices_json, updated_at
            ) VALUES (?, ?, CURRENT_TIMESTAMP(6), 'active', ?, '2099-01-01', '2099-01-01', 1, 1, ?, ?, NULL, NULL, CURRENT_TIMESTAMP(6))
            """.trimIndent(),
            TRIP_ID,
            "Rollback Route · 2099-01-01",
            "成都",
            OLD_REVISION,
            FROZEN_ROUTE_BASIS
        )
        jdbcTemplate.update(
            "INSERT INTO personal_trip_ownership (trip_id, account_id) VALUES (?, ?)",
            TRIP_ID,
            ACCOUNT_ID
        )
        jdbcTemplate.update(
            "INSERT INTO trip_frozen_route_versions (trip_id, route_version_id) VALUES (?, ?)",
            TRIP_ID,
            OLD_VERSION_ID
        )
        jdbcTemplate.update(
            """
            INSERT INTO personal_trip_days (
                id, trip_id, day_number, date, primary_stage, hiking_day_number, content_json
            ) VALUES (?, ?, 1, '2099-01-01', '徒步', 1, ?)
            """.trimIndent(),
            OLD_DAY_ID,
            TRIP_ID,
            OLD_DAY
        )
        jdbcTemplate.update(
            """
            INSERT INTO personal_trip_equipment_snapshots (
                id, trip_id, item_count, known_total_weight_grams,
                missing_weight_item_count, owned_item_count, unconfirmed_owned_item_count
            ) VALUES (?, ?, 1, 500, 0, 1, 0)
            """.trimIndent(),
            OLD_SNAPSHOT_ID,
            TRIP_ID
        )
        jdbcTemplate.update(
            """
            INSERT INTO personal_trip_equipment_items (
                id, snapshot_id, display_order, name, normalized_name, quantity,
                unit_weight_grams, source, ownership_status, note
            ) VALUES (?, ?, 1, '旧背包', '旧背包', 1, 500, 'user_added', 'owned', NULL)
            """.trimIndent(),
            OLD_ITEM_ID,
            OLD_SNAPSHOT_ID
        )
        jdbcTemplate.execute(
            """
            CREATE TRIGGER fail_personal_trip_migration_update
            BEFORE UPDATE ON personal_trips
            FOR EACH ROW
            SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT = 'forced migration write failure'
            """.trimIndent()
        )
    }

    @AfterEach
    fun tearDown() {
        jdbcTemplate.execute("DROP TRIGGER IF EXISTS fail_personal_trip_migration_update")
        cleanFixture()
    }

    @Test
    fun `database restores the complete aggregate when migration fails after destructive writes`() {
        assertThrows(RuntimeException::class.java) {
            service.migrate(
                ACCOUNT_ID,
                TRIP_ID,
                "rollback-key",
                MigrateTripCommand(OLD_REVISION, TARGET_VERSION_ID)
            )
        }

        assertEquals(
            OLD_REVISION,
            jdbcTemplate.queryForObject(
                "SELECT revision FROM personal_trips WHERE id = ?",
                String::class.java,
                TRIP_ID
            )
        )
        assertEquals(
            OLD_VERSION_ID,
            jdbcTemplate.queryForObject(
                "SELECT route_version_id FROM trip_frozen_route_versions WHERE trip_id = ?",
                String::class.java,
                TRIP_ID
            )
        )
        assertEquals(1, count("personal_trip_days", "trip_id", TRIP_ID))
        assertEquals(1, count("personal_trip_equipment_snapshots", "trip_id", TRIP_ID))
        assertEquals(1, count("personal_trip_equipment_items", "snapshot_id", OLD_SNAPSHOT_ID))
        assertEquals(0, count("personal_trip_idempotency", "account_id", ACCOUNT_ID))
    }

    private fun insertRouteVersion(id: String, label: String) {
        jdbcTemplate.update(
            """
            INSERT INTO route_versions (
                id, route_id, version_label, route_type, name, region, start_name, end_name,
                estimated_duration_seconds, main_track_availability,
                main_track_reference_system, main_track_json, created_at
            ) VALUES (?, ?, ?, 'one_day', ?, ?, ?, ?, 28800, 'valid', 'WGS84', ?, CURRENT_TIMESTAMP(6))
            """.trimIndent(),
            id,
            ROUTE_ID,
            label,
            "Rollback Route $label",
            "四川",
            "起点",
            "终点",
            "[[30.0,101.0],[30.1,101.1]]"
        )
    }

    private fun count(table: String, column: String, value: String): Int =
        requireNotNull(jdbcTemplate.queryForObject("SELECT COUNT(*) FROM $table WHERE $column = ?", Int::class.java, value))

    private fun cleanFixture() {
        jdbcTemplate.execute("DROP TRIGGER IF EXISTS fail_personal_trip_migration_update")
        jdbcTemplate.update("DELETE FROM personal_trip_idempotency WHERE account_id = ?", ACCOUNT_ID)
        jdbcTemplate.update("DELETE FROM personal_trip_equipment_item_derivations WHERE trip_id = ?", TRIP_ID)
        jdbcTemplate.update("DELETE FROM personal_trip_equipment_items WHERE snapshot_id = ?", OLD_SNAPSHOT_ID)
        jdbcTemplate.update("DELETE FROM personal_trip_equipment_snapshots WHERE trip_id = ?", TRIP_ID)
        jdbcTemplate.update("DELETE FROM personal_trip_days WHERE trip_id = ?", TRIP_ID)
        jdbcTemplate.update("DELETE FROM trip_frozen_route_versions WHERE trip_id = ?", TRIP_ID)
        jdbcTemplate.update("DELETE FROM personal_trip_ownership WHERE trip_id = ?", TRIP_ID)
        jdbcTemplate.update("DELETE FROM personal_trips WHERE id = ?", TRIP_ID)
        jdbcTemplate.update("DELETE FROM route_current_public_versions WHERE route_id = ?", ROUTE_ID)
        jdbcTemplate.update("DELETE FROM route_version_publication_order WHERE route_id = ?", ROUTE_ID)
        jdbcTemplate.update("DELETE FROM route_versions WHERE route_id = ?", ROUTE_ID)
        jdbcTemplate.update("DELETE FROM routes WHERE id = ?", ROUTE_ID)
        jdbcTemplate.update("DELETE FROM users WHERE id = ?", ACCOUNT_ID)
    }

    private companion object {
        const val ACCOUNT_ID = "mysql8-rollback-account"
        const val ROUTE_ID = "mysql8-rollback-route"
        const val OLD_VERSION_ID = "mysql8-route-version-old"
        const val TARGET_VERSION_ID = "mysql8-route-version-target"
        const val TRIP_ID = "mysql8-rollback-trip"
        const val OLD_REVISION = "mysql8-revision-old"
        const val OLD_DAY_ID = "mysql8-day-old"
        const val OLD_SNAPSHOT_ID = "mysql8-snapshot-old"
        const val OLD_ITEM_ID = "mysql8-item-old"

        const val FROZEN_ROUTE_BASIS = """{"routeName":"Rollback Route old","routeType":"one_day","region":"四川","start":{"name":"起点"},"end":{"name":"终点"},"estimatedDuration":{"seconds":28800.0},"mainTrackPath":[{"latitude":30.0,"longitude":101.0,"referenceSystem":"WGS84"},{"latitude":30.1,"longitude":101.1,"referenceSystem":"WGS84"}],"versionLabel":"old"}"""
        const val OLD_DAY = """{"identity":"mysql8-day-old","dayNumber":1,"date":"2099-01-01","primaryStage":"徒步","hikingDayNumber":1,"actions":[{"sequence":1,"actionType":"hike"}],"weather":{"condition":{"confidence":{"status":"unavailable","category":"dynamic_external_information"}},"temperatureRange":{"confidence":{"status":"unavailable","category":"dynamic_external_information"}},"precipitation":{"confidence":{"status":"unavailable","category":"dynamic_external_information"}},"wind":{"confidence":{"status":"unavailable","category":"dynamic_external_information"}},"placeContext":{"confidence":{"status":"unavailable","category":"dynamic_external_information"}}}}"""
    }
}
