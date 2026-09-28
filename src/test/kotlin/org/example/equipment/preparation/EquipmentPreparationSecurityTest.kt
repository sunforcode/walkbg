package org.example.equipment.preparation

import org.example.account.repository.AccountSessionRepository
import org.example.common.contract.TargetApiExceptionHandler
import org.example.config.CorsProperties
import org.example.config.JacksonConfig
import org.example.equipment.controller.PersonalEquipmentController
import org.example.equipment.controller.UserEquipmentListController
import org.example.equipment.service.PersonalEquipmentApplicationService
import org.example.security.JwtAuthenticationFilter
import org.example.security.JwtTokenUtil
import org.example.security.SecurityConfig
import org.example.trip.personal.controller.TripEquipmentController
import org.example.trip.personal.service.TripEquipmentApplicationService
import org.junit.jupiter.api.Test
import org.mockito.kotlin.any
import org.mockito.kotlin.verifyNoInteractions
import org.mockito.kotlin.whenever
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest
import org.springframework.boot.test.mock.mockito.MockBean
import org.springframework.context.annotation.Import
import org.springframework.dao.DataAccessResourceFailureException
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.*

@WebMvcTest(controllers = [PersonalEquipmentController::class, UserEquipmentListController::class, TripEquipmentController::class])
@Import(SecurityConfig::class, JwtAuthenticationFilter::class, JacksonConfig::class, TargetApiExceptionHandler::class, CorsProperties::class)
class EquipmentPreparationSecurityTest {
    @Autowired private lateinit var mvc: MockMvc
    @MockBean private lateinit var personal: PersonalEquipmentApplicationService
    @MockBean private lateinit var trip: TripEquipmentApplicationService
    @MockBean private lateinit var jwt: JwtTokenUtil
    @MockBean private lateinit var sessions: AccountSessionRepository

    @Test
    fun `all preparation commands require authentication before parsing input`() {
        val paths = listOf(
            "POST" to "/api/v1/personal-equipment:from-catalog",
            "POST" to "/api/v1/equipment-lists/list:copy",
            "POST" to "/api/v1/trips/trip/equipment/items:from-personal",
            "PUT" to "/api/v1/trips/trip/equipment/items/item/personal-equipment",
            "POST" to "/api/v1/trips/trip/equipment:append-list",
            "GET" to "/api/v1/trips/trip/equipment/preparation",
            "PUT" to "/api/v1/trips/trip/equipment/items/item/packing",
            "POST" to "/api/v1/trips/trip/equipment:reset-packing"
        )
        for ((method, path) in paths) for (context in listOf("", "/walkbg")) {
            mvc.perform(request(org.springframework.http.HttpMethod.valueOf(method), context + path).contextPath(context))
                .andExpect(status().isUnauthorized).andExpect(jsonPath("$.error.code").value("authentication_required"))
        }
        verifyNoInteractions(personal, trip)
    }

    @Test
    fun `deployed context keeps correct service unavailable and method error envelopes`() {
        validSession()
        whenever(personal.getEquipmentCollection(any(), any())).thenThrow(DataAccessResourceFailureException("unavailable"))
        mvc.perform(get("/walkbg/api/v1/personal-equipment").contextPath("/walkbg").header("Authorization", "Bearer valid"))
            .andExpect(status().isServiceUnavailable).andExpect(jsonPath("$.error.code").value("personal_equipment_unavailable"))
        mvc.perform(get("/walkbg/api/v1/personal-equipment:from-catalog").contextPath("/walkbg").header("Authorization", "Bearer valid"))
            .andExpect(status().isMethodNotAllowed).andExpect(jsonPath("$.error.code").value("method_not_allowed"))
    }

    @Test
    fun `browser clients can read revision etags`() {
        mvc.perform(options("/api/v1/trips/trip/equipment/preparation")
            .header("Origin", "https://walk.example").header("Access-Control-Request-Method", "GET"))
            .andExpect(status().isOk).andExpect(header().string("Access-Control-Expose-Headers", "Authorization, ETag"))
    }

    private fun validSession() {
        whenever(jwt.isTokenValidFormat("valid")).thenReturn(true)
        whenever(jwt.isTokenExpired("valid")).thenReturn(false)
        whenever(jwt.getUsernameFromToken("valid")).thenReturn("account")
        whenever(jwt.getUserIdFromToken("valid")).thenReturn("account")
        whenever(jwt.getTokenTypeFromToken("valid")).thenReturn("account_session")
        whenever(jwt.getSessionIdFromToken("valid")).thenReturn("session")
        whenever(sessions.existsByIdAndAccountIdAndRevokedAtIsNull("session", "account")).thenReturn(true)
    }
}
