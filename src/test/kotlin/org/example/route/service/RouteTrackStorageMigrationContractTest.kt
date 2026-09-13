package org.example.route.service

import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class RouteTrackStorageMigrationContractTest {
    @Test
    fun `route track storage supports full analyzed kml path`() {
        val migration = requireNotNull(
            javaClass.classLoader.getResource("db/migration/V11__expand_route_track_geo_json.sql")
        ).readText()

        assertTrue(
            migration.contains("MODIFY COLUMN track_geo_json LONGTEXT NULL"),
            "完整 KML 轨迹可能超过 TEXT 的 64KB 上限，迁移必须扩容为 LONGTEXT"
        )
    }
}
