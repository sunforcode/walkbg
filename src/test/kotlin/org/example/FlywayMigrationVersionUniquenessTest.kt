package org.example

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import java.nio.file.Files
import java.nio.file.Paths

class FlywayMigrationVersionUniquenessTest {
    @Test
    fun `flyway migration versions are unique`() {
        val migrationDir = Paths.get("src/main/resources/db/migration")
        val migrations = Files.list(migrationDir).use { paths ->
            paths
                .filter { Files.isRegularFile(it) && it.fileName.toString().matches(Regex("V\\d+__.+\\.sql")) }
                .map { it.fileName.toString() }
                .toList()
        }
        val versions = migrations.groupBy { it.substringAfter('V').substringBefore("__") }
        val duplicates = versions.filterValues { it.size > 1 }

        assertEquals(emptyMap<String, List<String>>(), duplicates, "Flyway 迁移版本号必须唯一")
    }
}
