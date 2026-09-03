package org.example.security

import org.example.account.repository.AccountSessionRepository
import org.example.config.CorsProperties
import org.example.config.JacksonConfig
import org.example.equipment.controller.EquipmentListController
import org.example.equipment.repository.EquipmentListRepository
import org.example.equipment.service.EquipmentListItemService
import org.example.equipment.service.EquipmentService
import org.example.trip.controller.TripController
import org.example.common.contract.TargetApiExceptionHandler
import org.example.trip.personal.controller.PersonalTripController
import org.example.trip.personal.controller.TripEquipmentController
import org.example.trip.personal.controller.TripGenerationContextController
import org.example.trip.personal.service.PersonalTripApplicationService
import org.example.trip.personal.service.TripEquipmentApplicationService
import org.example.trip.service.TripService
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.mockito.kotlin.whenever
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest
import org.springframework.boot.test.mock.mockito.MockBean
import org.springframework.context.annotation.Import
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.status

@WebMvcTest(
    controllers = [
        PersonalTripController::class,
        TripEquipmentController::class,
        TripGenerationContextController::class,
        TripController::class,
        EquipmentListController::class
    ]
)
@Import(
    SecurityConfig::class,
    JwtAuthenticationFilter::class,
    JacksonConfig::class,
        TargetApiExceptionHandler::class
)
class PersonalTripSecurityIntegrationTest {
    @Autowired
    private lateinit var mockMvc: MockMvc

    @MockBean
    private lateinit var service: PersonalTripApplicationService

    @MockBean
    private lateinit var tripEquipmentService: TripEquipmentApplicationService

    @MockBean
    private lateinit var tripService: TripService

    @MockBean
    private lateinit var equipmentListRepository: EquipmentListRepository

    @MockBean
    private lateinit var equipmentListItemService: EquipmentListItemService

    @MockBean
    private lateinit var equipmentService: EquipmentService

    @MockBean
    private lateinit var jwtTokenUtil: JwtTokenUtil

    @MockBean
    private lateinit var accountSessionRepository: AccountSessionRepository

    @MockBean
    private lateinit var corsProperties: CorsProperties

    @BeforeEach
    fun setUp() {
        whenever(corsProperties.originPatterns()).thenReturn(listOf("*"))
    }

    @Test
    fun `personal trip generation context and legacy endpoints reject anonymous requests`() {
        listOf(
            "/api/v1/trips",
            "/api/v1/trips/trip-1/equipment",
            "/api/v1/public-routes/route-1/trip-generation-context",
            "/api/v1/legacy/trips",
            "/api/v1/legacy/equipment-lists"
        ).forEach { path ->
            mockMvc.perform(get(path))
                .andExpect(status().isUnauthorized)
                .andExpect(jsonPath("$.error.code").value("authentication_required"))
        }
    }
}
