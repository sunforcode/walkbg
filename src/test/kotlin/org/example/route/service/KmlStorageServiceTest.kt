package org.example.route.service

import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir
import java.nio.file.Path

class KmlStorageServiceTest {
    @TempDir
    lateinit var uploadDir: Path

    @Test
    fun `readStoredContent reads legacy classpath kml`() {
        val service = KmlStorageService(uploadDir.toString())

        val content = service.readStoredContent("/static/kml/wutaishan.kml")

        assertNotNull(content)
        assertTrue(content!!.contains("<kml", ignoreCase = true))
    }
}
