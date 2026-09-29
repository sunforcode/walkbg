package org.example.scene

import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test

class SceneMigrationContractTest {
    @Test
    fun `new migration executes against SQL storage without rewriting parent rows`() {
        java.sql.DriverManager.getConnection("jdbc:h2:mem:scene-migration;MODE=MySQL").use { connection ->
            connection.createStatement().use { statement ->
                statement.execute("CREATE TABLE route_versions (id varchar(64) PRIMARY KEY)")
                statement.execute("CREATE TABLE personal_trips (id varchar(64) PRIMARY KEY)")
                statement.execute("INSERT INTO route_versions VALUES ('existing-route-version')")
                statement.execute("INSERT INTO personal_trips VALUES ('existing-trip')")
                val sql = requireNotNull(javaClass.classLoader.getResource("db/migration/V19__add_scene_artwork.sql")).readText()
                sql.split(';').filter { it.isNotBlank() }.forEach { statement.execute(it) }
                statement.executeQuery("SELECT COUNT(*) FROM route_versions").use { assertTrue(it.next()); assertEquals(1, it.getInt(1)) }
                statement.executeQuery("SELECT COUNT(*) FROM personal_trips").use { assertTrue(it.next()); assertEquals(1, it.getInt(1)) }
                assertThrows(java.sql.SQLException::class.java) { statement.execute("INSERT INTO trip_scene_sets VALUES ('missing-trip','existing-route-version','r1',1,'{}')") }
            }
        }
    }
    @Test
    fun `scene persistence is additive and leaves published facts and trips untouched`() {
        val resource = javaClass.classLoader.getResource("db/migration/V19__add_scene_artwork.sql")
        assertNotNull(resource, "artwork must have durable independent persistence")
        val sql = resource!!.readText()
        listOf("CREATE TABLE scene_media", "LONGBLOB", "CREATE TABLE route_scene_sets", "CREATE TABLE trip_scene_sets", "REFERENCES route_versions (id)", "REFERENCES personal_trips (id)").forEach { assertTrue(sql.contains(it), it) }
        assertFalse(Regex("(?i)(UPDATE|DELETE FROM)\\s+(route_versions|personal_trip_days|personal_trips)\\b").containsMatchIn(sql))
    }
}
