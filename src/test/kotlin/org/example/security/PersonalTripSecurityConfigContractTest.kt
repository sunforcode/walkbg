package org.example.security

import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import java.nio.charset.StandardCharsets
import java.nio.file.Files
import java.nio.file.Paths

class PersonalTripSecurityConfigContractTest {
    private val source = String(
        Files.readAllBytes(Paths.get(System.getProperty("user.dir"), "src/main/kotlin/org/example/security/SecurityConfig.kt")),
        StandardCharsets.UTF_8
    )

    @Test
    fun `personal generation context and legacy resources remain protected before broader api rules`() {
        val generationContextRule = source.indexOf(
            "authorize(HttpMethod.GET, \"/api/v1/public-routes/*/trip-generation-context\", authenticated)"
        )
        val publicRouteRule = source.indexOf("authorize(\"/api/v1/public-routes/**\", permitAll)")
        val legacyTripRule = source.indexOf("authorize(\"/api/v1/legacy/trips/**\", authenticated)")
        val legacyEquipmentListRule = source.indexOf(
            "authorize(\"/api/v1/legacy/equipment-lists/**\", authenticated)"
        )
        val apiFallbackRule = source.indexOf("authorize(\"/api/**\", permitAll)")

        assertTrue(generationContextRule >= 0 && generationContextRule < publicRouteRule)
        assertTrue(legacyTripRule >= 0 && legacyTripRule < apiFallbackRule)
        assertTrue(legacyEquipmentListRule >= 0 && legacyEquipmentListRule < apiFallbackRule)
    }
}
