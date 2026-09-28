package org.example.equipment.catalog.security

import org.example.account.repository.AccountSessionRepository
import org.example.common.contract.TargetApiExceptionHandler
import org.example.config.CorsProperties
import org.example.config.JacksonConfig
import org.example.equipment.catalog.controller.EquipmentCatalogController
import org.example.equipment.catalog.dto.CatalogBrandCollection
import org.example.equipment.catalog.dto.CatalogEquipmentCollection
import org.example.equipment.catalog.dto.CatalogEquipmentDetailProjection
import org.example.equipment.catalog.service.EquipmentCatalogApplicationService
import org.example.security.JwtAuthenticationFilter
import org.example.security.JwtTokenUtil
import org.example.security.SecurityConfig
import org.junit.jupiter.api.Test
import org.mockito.kotlin.any
import org.mockito.kotlin.verifyNoInteractions
import org.mockito.kotlin.whenever
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest
import org.springframework.boot.test.mock.mockito.MockBean
import org.springframework.context.annotation.Import
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.*

@WebMvcTest(controllers = [EquipmentCatalogController::class])
@Import(SecurityConfig::class, JwtAuthenticationFilter::class, JacksonConfig::class, TargetApiExceptionHandler::class, CorsProperties::class)
class EquipmentCatalogSecurityIntegrationTest {
    @Autowired private lateinit var mvc: MockMvc
    @MockBean private lateinit var service: EquipmentCatalogApplicationService
    @MockBean private lateinit var jwtTokenUtil: JwtTokenUtil
    @MockBean private lateinit var sessions: AccountSessionRepository

    @Test
    fun `real security chain protects all reads including deployed context path`() {
        paths.forEach { path ->
            mvc.perform(get(path)).andExpect(status().isUnauthorized)
                .andExpect(jsonPath("$.error.code").value("authentication_required"))
            mvc.perform(get("/walkbg$path").contextPath("/walkbg"))
                .andExpect(status().isUnauthorized).andExpect(jsonPath("$.data").doesNotExist())
        }
        verifyNoInteractions(service)
    }

    @Test
    fun `valid persisted session reads all shared catalog projections`() {
        validSession()
        whenever(service.getBrands()).thenReturn(CatalogBrandCollection(emptyList()))
        whenever(service.getItems(any())).thenReturn(CatalogEquipmentCollection(emptyList()))
        whenever(service.getItem("item")).thenReturn(CatalogEquipmentDetailProjection("item", "背包", "backpack", "backpack"))
        paths.forEach { path ->
            mvc.perform(get("/walkbg$path").contextPath("/walkbg").header("Authorization", "Bearer valid"))
                .andExpect(status().isOk).andExpect(jsonPath("$.data").exists())
        }
    }

    @Test
    fun `expired revoked malformed and legacy credentials cannot access catalog`() {
        validSession()
        whenever(jwtTokenUtil.isTokenValidFormat("malformed")).thenReturn(false)
        whenever(jwtTokenUtil.isTokenValidFormat("expired")).thenReturn(true)
        whenever(jwtTokenUtil.isTokenExpired("expired")).thenReturn(true)
        whenever(sessions.existsByIdAndAccountIdAndRevokedAtIsNull("session", "account")).thenReturn(false)
        listOf("malformed", "expired", "valid").forEach { token ->
            mvc.perform(get(paths.first()).header("Authorization", "Bearer $token"))
                .andExpect(status().isUnauthorized).andExpect(jsonPath("$.error.retryable").value(false))
        }
        whenever(sessions.existsByIdAndAccountIdAndRevokedAtIsNull("session", "account")).thenReturn(true)
        whenever(jwtTokenUtil.getTokenTypeFromToken("valid")).thenReturn("legacy_access")
        mvc.perform(get(paths.first()).header("Authorization", "Bearer valid")).andExpect(status().isUnauthorized)
        verifyNoInteractions(service)
    }

    @Test
    fun `read access never opens catalog mutations and unknown routes retain error envelope`() {
        validSession()
        listOf(post(paths[1]), put(paths[2]), patch(paths[2]), delete(paths[2])).forEach { request ->
            mvc.perform(request.header("Authorization", "Bearer valid"))
                .andExpect(status().isMethodNotAllowed).andExpect(jsonPath("$.error.code").value("method_not_allowed"))
        }
        mvc.perform(post("/walkbg${paths[1]}").contextPath("/walkbg").header("Authorization", "Bearer valid"))
            .andExpect(status().isMethodNotAllowed).andExpect(jsonPath("$.error.code").value("method_not_allowed"))
        mvc.perform(get("/walkbg/api/v1/equipment-catalog/unknown").contextPath("/walkbg").header("Authorization", "Bearer valid"))
            .andExpect(status().isNotFound).andExpect(jsonPath("$.error.code").value("resource_not_found"))
        verifyNoInteractions(service)
    }

    private fun validSession() {
        whenever(jwtTokenUtil.isTokenValidFormat("valid")).thenReturn(true)
        whenever(jwtTokenUtil.isTokenExpired("valid")).thenReturn(false)
        whenever(jwtTokenUtil.getUsernameFromToken("valid")).thenReturn("account")
        whenever(jwtTokenUtil.getUserIdFromToken("valid")).thenReturn("account")
        whenever(jwtTokenUtil.getTokenTypeFromToken("valid")).thenReturn("account_session")
        whenever(jwtTokenUtil.getSessionIdFromToken("valid")).thenReturn("session")
        whenever(sessions.existsByIdAndAccountIdAndRevokedAtIsNull("session", "account")).thenReturn(true)
    }

    companion object {
        private val paths = listOf("/api/v1/equipment-catalog/brands", "/api/v1/equipment-catalog/items", "/api/v1/equipment-catalog/items/item")
    }
}
