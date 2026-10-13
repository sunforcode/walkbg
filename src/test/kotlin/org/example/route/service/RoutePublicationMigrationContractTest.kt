package org.example.route.service

import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class RoutePublicationMigrationContractTest {
    @Test
    fun `publication request migration enforces version ownership and durable allocation`() {
        val resource = javaClass.classLoader.getResource("db/migration/V14__add_route_publication_requests.sql")
        assertNotNull(resource, "publication receipts and allocator must exist in a new migration")
        val sql = resource!!.readText()
        assertTrue(sql.contains("PRIMARY KEY (route_id, publication_id)"))
        assertTrue(sql.contains("REFERENCES route_version_publication_order (route_id, route_version_id)"))
        assertTrue(sql.contains("MAX(all_route_order)"))
        assertTrue(sql.contains("public_route_type IN ('one_day', 'multi_day')"))
        assertTrue(sql.contains("publication_id varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_bin NOT NULL"),
            "publication identities require NO PAD binary comparison, including trailing spaces")
    }

    @Test
    fun `track review migration keeps candidate decisions immutable and requests unique`() {
        val resource = javaClass.classLoader.getResource("db/migration/V17__add_route_track_reviews.sql")
        assertNotNull(resource)
        val sql = resource!!.readText()
        assertTrue(sql.contains("CREATE TABLE route_track_reviews"))
        assertTrue(sql.contains("UNIQUE KEY uk_route_track_review_revision (route_id, revision)"))
        assertTrue(sql.contains("UNIQUE KEY uk_route_track_review_request (route_id, request_id)"))
        assertTrue(sql.contains("request_id varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_bin NOT NULL"))
        assertTrue(sql.contains("REFERENCES routes (id)"))
        assertTrue(sql.contains("decision = 'approved' AND complete_hiking_range_confirmed = true"))
        assertTrue(sql.contains("reference_system IS NULL AND reason IS NOT NULL"))
        assertTrue(!sql.contains("UPDATE route_versions"), "migration must not approve or rewrite published tracks")
    }
}
