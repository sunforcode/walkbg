package org.example.equipment.preparation

import com.fasterxml.jackson.databind.JsonNode
import com.fasterxml.jackson.databind.ObjectMapper
import org.example.config.AppInitializer
import org.example.equipment.service.CreatePersonalEquipmentCommand
import org.example.equipment.service.PersonalEquipmentApplicationService
import org.example.equipment.service.UpdatePersonalEquipmentCommand
import org.example.security.CustomUserDetails
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.boot.test.mock.mockito.MockBean
import org.springframework.http.MediaType
import org.springframework.jdbc.core.JdbcTemplate
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken
import org.springframework.test.context.ActiveProfiles
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.MvcResult
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.*
import java.util.UUID
import java.util.concurrent.CountDownLatch
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit

/** Real business transactions and receipts; only a dedicated explicitly enabled local test database. */
@SpringBootTest(
    webEnvironment = SpringBootTest.WebEnvironment.MOCK,
    properties = [
        "spring.datasource.url=\${WALK_PREPARATION_MYSQL_URL}",
        "spring.datasource.username=\${WALK_PREPARATION_MYSQL_USERNAME:root}",
        "spring.datasource.password=\${WALK_PREPARATION_MYSQL_PASSWORD:}",
        "spring.jpa.hibernate.ddl-auto=validate", "spring.flyway.enabled=true", "spring.sql.init.mode=never",
        "kml-agent.base-url=http://127.0.0.1:65535", "kml-agent.enabled=false",
        "jwt.secret=equipment-preparation-test-secret-key-at-least-256-bits",
        "cors.allowed-origins=", "account.avatar-media.directory=/tmp/walkbg-preparation-integration-avatar"
    ]
)
@ActiveProfiles("prod")
@AutoConfigureMockMvc(addFilters = false)
@EnabledIfEnvironmentVariable(named = "WALK_PREPARATION_MYSQL_URL", matches = "jdbc:mysql://127\\.0\\.0\\.1:[0-9]+/walk_preparation_test(?:\\?.*)?")
class EquipmentPreparationMySqlIntegrationTest {
    @Autowired private lateinit var jdbc: JdbcTemplate
    @Autowired private lateinit var mvc: MockMvc
    @Autowired private lateinit var mapper: ObjectMapper
    @Autowired private lateinit var personal: PersonalEquipmentApplicationService
    @MockBean private lateinit var appInitializer: AppInitializer

    private val prefix = "prep-${UUID.randomUUID().toString().take(12)}"
    private val account = "$prefix-account"
    private val trip = "$prefix-trip"
    private val route = "$prefix-route"
    private val version = "$prefix-version"
    private val snapshot = "$prefix-snapshot"
    private val catalog = "$prefix-catalog"
    private val originalRevision = "$prefix-revision"

    @BeforeEach
    fun fixture() {
        jdbc.update("INSERT INTO users (id, username, email, password, status, created_at, updated_at) VALUES (?, ?, ?, 'unused', 0, CURRENT_TIMESTAMP(6), CURRENT_TIMESTAMP(6))", account, prefix, "$prefix@example.test")
        jdbc.update("INSERT INTO routes (id, name, status, popularity, usage_count, is_loop, is_favorite, created_by, created_at, updated_at, is_deleted) VALUES (?, '准备路线', 1, 0, 0, b'0', b'0', ?, CURRENT_TIMESTAMP(6), CURRENT_TIMESTAMP(6), b'0')", route, account)
        jdbc.update("INSERT INTO route_versions (id, route_id, version_label, route_type, name, region, start_name, end_name, estimated_duration_seconds, main_track_availability, main_track_reference_system, main_track_json, created_at) VALUES (?, ?, 'v1', 'one_day', '准备路线', '四川', '起点', '终点', 3600, 'valid', 'WGS84', '[[30,101],[30.1,101.1]]', CURRENT_TIMESTAMP(6))", version, route)
        jdbc.update("INSERT INTO personal_trips (id, name, first_generated_at, lifecycle_state, departure_city, start_date, end_date, total_day_count, hiking_day_count, revision, frozen_route_basis_json, updated_at) VALUES (?, '准备行程', CURRENT_TIMESTAMP(6), 'active', '成都', '2099-01-01', '2099-01-01', 1, 1, ?, ?, CURRENT_TIMESTAMP(6))", trip, originalRevision, FROZEN_BASIS)
        jdbc.update("INSERT INTO personal_trip_ownership (trip_id, account_id) VALUES (?, ?)", trip, account)
        jdbc.update("INSERT INTO trip_frozen_route_versions (trip_id, route_version_id) VALUES (?, ?)", trip, version)
        jdbc.update("INSERT INTO personal_trip_equipment_snapshots (id, trip_id, item_count, known_total_weight_grams, missing_weight_item_count, owned_item_count, unconfirmed_owned_item_count) VALUES (?, ?, 0, 0, 0, 0, 0)", snapshot, trip)
        jdbc.update("INSERT INTO equipment (id, name, category, equipment_type) VALUES (?, '参考袜子', 'clothing', 'socks')", catalog)
        jdbc.update("INSERT INTO socks_specs (equipment_id, variant, unit_label, unit_weight_grams, weight_scope) VALUES (?, 'M', '双', 12.5, '整双')", catalog)
    }

    @AfterEach
    fun removeOwnFixture() {
        jdbc.update("DELETE FROM equipment_command_receipts WHERE account_id = ?", account)
        jdbc.update("DELETE FROM personal_trip_equipment_item_derivations WHERE trip_id = ?", trip)
        jdbc.update("DELETE FROM personal_trip_equipment_suppressions WHERE trip_id = ?", trip)
        jdbc.update("DELETE FROM personal_trip_equipment_items WHERE snapshot_id = ?", snapshot)
        jdbc.update("DELETE FROM personal_trip_equipment_snapshots WHERE trip_id = ?", trip)
        jdbc.update("DELETE FROM trip_frozen_route_versions WHERE trip_id = ?", trip)
        jdbc.update("DELETE FROM personal_trip_ownership WHERE trip_id = ?", trip)
        jdbc.update("DELETE FROM personal_trips WHERE id = ?", trip)
        val lists = jdbc.queryForList("SELECT equipment_list_id FROM equipment_list_ownership WHERE account_id = ?", String::class.java, account)
        lists.forEach { jdbc.update("DELETE FROM equipment_list_members WHERE equipment_list_id = ?", it) }
        jdbc.update("DELETE FROM equipment_list_ownership WHERE account_id = ?", account)
        lists.forEach { jdbc.update("DELETE FROM user_equipment_lists WHERE id = ?", it) }
        val equipment = jdbc.queryForList("SELECT personal_equipment_id FROM personal_equipment_ownership WHERE account_id = ?", String::class.java, account)
        jdbc.update("DELETE FROM personal_equipment_ownership WHERE account_id = ?", account)
        equipment.forEach { jdbc.update("DELETE FROM personal_equipment WHERE id = ?", it) }
        jdbc.update("DELETE FROM equipment WHERE id = ?", catalog)
        jdbc.update("DELETE FROM route_versions WHERE id = ?", version)
        jdbc.update("DELETE FROM routes WHERE id = ?", route)
        jdbc.update("DELETE FROM users WHERE id = ?", account)
    }

    @Test
    fun `catalog adoption receipts preserve full initial response after changed and deleted sources`() {
        val body = """{"catalogEquipmentId":"$catalog","ownedQuantity":2}"""
        val initial = request(post("/api/v1/personal-equipment:from-catalog").header("Idempotency-Key", "adopt").json(body), 201)
        val itemId = initial["data"]["item"]["identity"].asText()
        assertEquals(12.5, initial["data"]["item"]["unitWeight"]["grams"].asDouble())
        assertEquals("catalog_reference", initial["data"]["item"]["unitWeightSource"].asText())
        jdbc.update("UPDATE socks_specs SET unit_weight_grams = 99 WHERE equipment_id = ?", catalog)
        personal.updateEquipment(account, itemId, UpdatePersonalEquipmentCommand(unitWeightGrams = 14.25))
        personal.createEquipment(account, CreatePersonalEquipmentCommand("别的装备", 1, 20.0))
        assertEquals(initial, request(post("/api/v1/personal-equipment:from-catalog").header("Idempotency-Key", "adopt").json(body), 201))
        assertEquals(14.25, personal.getEquipment(account, itemId).unitWeight?.grams)
        request(post("/api/v1/personal-equipment:from-catalog").header("Idempotency-Key", "adopt")
            .json("""{"ownedQuantity":2,"catalogEquipmentId":"$catalog","unitWeight":null}"""), 409)
        personal.deleteEquipment(account, itemId, true)
        jdbc.update("DELETE FROM equipment WHERE id = ?", catalog)
        assertEquals(initial, request(post("/api/v1/personal-equipment:from-catalog").header("Idempotency-Key", "adopt").json(body), 201))
        assertEquals(1, personal.getEquipmentCollection(account).summary.equipmentItemCount)
    }

    @Test
    fun `list copy commits independent references and replays after source deletion`() {
        val item = personal.createEquipment(account, CreatePersonalEquipmentCommand("帐篷", 2, 700.25)).item
        val source = personal.createEquipmentList(account, "原清单").equipmentList
        personal.putListMember(account, source.identity, item.identity, 2)
        val body = """{"name":"副本"}"""
        val path = "/api/v1/equipment-lists/${source.identity}:copy"
        val copied = request(post(path).header("Idempotency-Key", "copy").json(body), 201)
        val copiedId = copied["data"]["equipmentList"]["identity"].asText()
        assertNotEquals(source.identity, copiedId)
        assertEquals(item.identity, copied["data"]["equipmentList"]["members"][0]["identity"].asText())
        personal.putListMember(account, copiedId, item.identity, 1)
        assertEquals(2, personal.getEquipmentList(account, source.identity).members.single().quantity)
        personal.deleteEquipmentList(account, source.identity)
        assertEquals(copied, request(post(path).header("Idempotency-Key", "copy").json(body), 201))
        assertEquals(1, personal.getEquipmentList(account, copiedId).members.single().quantity)
        assertEquals(1, personal.getEquipmentCollection(account).summary.equipmentListCount)
    }

    @Test
    fun `select allocation packing and preparation retain decimals and guard revision`() {
        val personalItem = personal.createEquipment(account, CreatePersonalEquipmentCommand("袜子", 2, 12.5, "clothing", "socks", "M", "双", "整双")).item
        val path = "/api/v1/trips/$trip/equipment"
        val selected = request(post("$path/items:from-personal").header("Idempotency-Key", "select").revision(originalRevision)
            .json("""{"personalEquipmentId":"${personalItem.identity}","quantity":2}"""), 201)
        val selectedRevision = selected["data"]["revision"].asText()
        val itemId = selected["data"]["snapshot"]["items"][0]["identity"].asText()
        val allocated = request(patch("$path/items/$itemId").revision(selectedRevision)
            .json("""{"carryAllocation":{"backpackQuantity":1,"wornQuantity":1,"handheldQuantity":0}}"""), 200)
        val currentRevision = allocated["data"]["revision"].asText()
        val conflict = request(put("$path/items/$itemId/packing").revision(selectedRevision).json("""{"packedQuantity":1}"""), 409)
        assertEquals("concurrent_modification", conflict["error"]["code"].asText())
        val packed = request(put("$path/items/$itemId/packing").revision(currentRevision).json("""{"packedQuantity":1}"""), 200)
        val preparation = perform(get("$path/preparation"), 200)
        val prep = mapper.readTree(preparation.response.contentAsString)["data"]
        assertEquals(12.5, prep["weightBreakdown"]["backpack"]["knownWeight"]["grams"].asDouble())
        assertEquals(12.5, prep["weightBreakdown"]["worn"]["knownWeight"]["grams"].asDouble())
        assertEquals(1L, prep["packing"]["packedQuantity"].asLong())
        assertEquals("\"${packed["data"]["revision"].asText()}\"", preparation.response.getHeader("ETag"))
        personal.deleteEquipment(account, personalItem.identity, true)
        val replay = request(post("$path/items:from-personal").header("Idempotency-Key", "select").revision(originalRevision)
            .json("""{"personalEquipmentId":"${personalItem.identity}","quantity":2}"""), 201)
        assertEquals(selected, replay)
        assertEquals(1, request(get(path), 200)["data"]["snapshot"]["items"][0]["packedQuantity"].asInt())
    }

    @Test
    fun `append list validates whole batch before writes and retries once after source changes`() {
        val socks = personal.createEquipment(account, CreatePersonalEquipmentCommand("袜子", 2, 12.5, "clothing", "socks")).item
        val fuel = personal.createEquipment(account, CreatePersonalEquipmentCommand("参考燃料", 1, 230.0, "fuel", "fuel")).item
        val list = personal.createEquipmentList(account, "清单").equipmentList
        personal.putListMember(account, list.identity, socks.identity, 2)
        personal.putListMember(account, list.identity, fuel.identity, 1)
        val path = "/api/v1/trips/$trip/equipment:append-list"
        val body = """{"equipmentListId":"${list.identity}"}"""
        request(post(path).header("Idempotency-Key", "append").revision(originalRevision).json(body), 422)
        assertEquals(0, count("SELECT COUNT(*) FROM personal_trip_equipment_items WHERE snapshot_id = ?", snapshot))
        assertEquals(0, count("SELECT COUNT(*) FROM equipment_command_receipts WHERE account_id = ?", account))
        personal.removeListMember(account, list.identity, fuel.identity)
        val appended = request(post(path).header("Idempotency-Key", "append").revision(originalRevision).json(body), 200)
        assertEquals(25.0, appended["data"]["snapshot"]["summary"]["knownTotalWeight"]["grams"].asDouble())
        personal.deleteEquipmentList(account, list.identity)
        assertEquals(appended, request(post(path).header("Idempotency-Key", "append").revision(originalRevision).json(body), 200))
        assertEquals(1, count("SELECT COUNT(*) FROM personal_trip_equipment_items WHERE snapshot_id = ?", snapshot))
    }

    @Test
    fun `concurrent same key catalog commands commit one resource and identical complete response`() {
        val executor = Executors.newFixedThreadPool(2)
        val start = CountDownLatch(1)
        try {
            val jobs = (1..2).map {
                executor.submit<JsonNode> {
                    assertTrue(start.await(10, TimeUnit.SECONDS))
                    request(post("/api/v1/personal-equipment:from-catalog").header("Idempotency-Key", "concurrent")
                        .json("""{"catalogEquipmentId":"$catalog","ownedQuantity":1}"""), 201)
                }
            }
            start.countDown()
            val responses = jobs.map { it.get(20, TimeUnit.SECONDS) }
            assertEquals(responses[0], responses[1])
            assertEquals(1, personal.getEquipmentCollection(account).summary.equipmentItemCount)
            assertEquals(1, count("SELECT COUNT(*) FROM equipment_command_receipts WHERE account_id = ?", account))
        } finally {
            executor.shutdownNow()
            assertTrue(executor.awaitTermination(5, TimeUnit.SECONDS))
        }
    }

    @Test
    fun `different concurrent commands with one old revision cannot both commit`() {
        val ids = listOf("袜子", "帽子").map { personal.createEquipment(account, CreatePersonalEquipmentCommand(it, 1, 12.5)).item.identity }
        val executor = Executors.newFixedThreadPool(2)
        val start = CountDownLatch(1)
        try {
            val jobs = ids.mapIndexed { index, id ->
                executor.submit<MvcResult> {
                    assertTrue(start.await(10, TimeUnit.SECONDS))
                    mvc.perform(post("/api/v1/trips/$trip/equipment/items:from-personal")
                        .principal(UsernamePasswordAuthenticationToken(CustomUserDetails(account, prefix, ""), null, emptyList()))
                        .header("Idempotency-Key", "revision-$index").revision(originalRevision)
                        .json("""{"personalEquipmentId":"$id","quantity":1}""")).andReturn()
                }
            }
            start.countDown()
            val results = jobs.map { it.get(20, TimeUnit.SECONDS) }
            assertEquals(listOf(201, 409), results.map { it.response.status }.sorted())
            assertEquals("concurrent_modification", mapper.readTree(results.single { it.response.status == 409 }.response.contentAsString)["error"]["code"].asText())
            assertEquals(1, count("SELECT COUNT(*) FROM personal_trip_equipment_items WHERE snapshot_id = ?", snapshot))
            assertEquals(1, count("SELECT COUNT(*) FROM equipment_command_receipts WHERE account_id = ?", account))
        } finally {
            executor.shutdownNow()
            assertTrue(executor.awaitTermination(5, TimeUnit.SECONDS))
        }
    }

    @Test
    fun `overflowed finite input rolls back writes and existing invalid aggregate fails closed`() {
        val failed = request(post("/api/v1/personal-equipment").json("""{"name":"过重","ownedQuantity":2,"unitWeight":{"grams":1e308}}"""), 422)
        assertEquals("validation_failed", failed["error"]["code"].asText())
        assertEquals(0, personal.getEquipmentCollection(account).summary.equipmentItemCount)
        val item = personal.createEquipment(account, CreatePersonalEquipmentCommand("合法单件", 1, 1e308)).item
        val list = personal.createEquipmentList(account, "溢出清单").equipmentList
        request(put("/api/v1/equipment-lists/${list.identity}/members/${item.identity}").json("""{"quantity":2}"""), 422)
        assertTrue(personal.getEquipmentList(account, list.identity).members.isEmpty())
        jdbc.update("UPDATE personal_equipment SET owned_quantity = 2 WHERE id = ?", item.identity)
        val unavailable = request(get("/api/v1/personal-equipment"), 503)
        assertEquals("personal_equipment_unavailable", unavailable["error"]["code"].asText())
    }

    private fun perform(builder: MockHttpServletRequestBuilder, expectedStatus: Int): MvcResult = mvc.perform(builder.principal(
        UsernamePasswordAuthenticationToken(CustomUserDetails(account, prefix, ""), null, emptyList())
    )).andExpect(status().`is`(expectedStatus)).andReturn()

    private fun request(builder: MockHttpServletRequestBuilder, expectedStatus: Int): JsonNode =
        mapper.readTree(perform(builder, expectedStatus).response.contentAsString)

    private fun MockHttpServletRequestBuilder.json(body: String) = contentType(MediaType.APPLICATION_JSON).content(body)
    private fun MockHttpServletRequestBuilder.revision(value: String) = header("If-Match", "\"$value\"")
    private fun count(sql: String, value: String): Int = requireNotNull(jdbc.queryForObject(sql, Int::class.java, value))

    companion object {
        private const val FROZEN_BASIS = """{"routeName":"准备路线","routeType":"one_day","region":"四川","start":{"name":"起点"},"end":{"name":"终点"},"estimatedDuration":{"seconds":3600.0},"mainTrackPath":[{"latitude":30.0,"longitude":101.0,"referenceSystem":"WGS84"},{"latitude":30.1,"longitude":101.1,"referenceSystem":"WGS84"}],"versionLabel":"v1"}"""
    }
}
