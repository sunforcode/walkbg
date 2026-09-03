package org.example.trip.personal.controller

import org.example.common.contract.ApiContractException
import org.example.common.contract.TargetApiExceptionHandler
import org.example.config.JacksonConfig
import org.example.equipment.dto.EquipmentListSummary
import org.example.equipment.dto.PersonalEquipmentSummary
import org.example.equipment.dto.WeightProjection
import org.example.equipment.repository.EquipmentListRepository
import org.example.equipment.service.EquipmentListItemService
import org.example.security.CustomUserDetails
import org.example.security.JwtAuthenticationFilter
import org.example.trip.controller.TripController
import org.example.trip.personal.dto.CalendarDayProjection
import org.example.trip.personal.dto.GenerateTripResult
import org.example.trip.personal.dto.PersonalTripCalendarProjection
import org.example.trip.personal.dto.PersonalTripCollectionProjection
import org.example.trip.personal.dto.PersonalTripFocusProjection
import org.example.trip.personal.dto.TripGenerationContextProjection
import org.example.trip.personal.dto.TripGenerationEquipmentListOption
import org.example.trip.personal.dto.TripGenerationRouteProjection
import org.example.trip.personal.service.PersonalTripApplicationService
import org.example.trip.service.TripService
import org.junit.jupiter.api.Test
import org.mockito.kotlin.any
import org.mockito.kotlin.eq
import org.mockito.kotlin.whenever
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest
import org.springframework.boot.test.mock.mockito.MockBean
import org.springframework.context.annotation.Import
import org.springframework.http.MediaType
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.status
import java.time.LocalDate

@WebMvcTest(
    controllers = [
        PersonalTripController::class,
        TripGenerationContextController::class,
        TripController::class
    ]
)
@AutoConfigureMockMvc(addFilters = false)
@Import(JacksonConfig::class, TargetApiExceptionHandler::class)
class PersonalTripApiContractTest {
    @Autowired
    private lateinit var mockMvc: MockMvc

    @MockBean
    private lateinit var service: PersonalTripApplicationService

    @MockBean
    private lateinit var jwtAuthenticationFilter: JwtAuthenticationFilter

    @MockBean
    private lateinit var legacyTripService: TripService

    @MockBean
    private lateinit var legacyEquipmentListRepository: EquipmentListRepository

    @MockBean
    private lateinit var legacyEquipmentListItemService: EquipmentListItemService

    @Test
    fun `focus and collection use the authenticated account and reject unknown query`() {
        whenever(service.focus("account-1")).thenReturn(PersonalTripFocusProjection(null))
        whenever(service.collection("account-1")).thenReturn(PersonalTripCollectionProjection(emptyList(), emptyList()))

        mockMvc.perform(get("/api/v1/trips/focus").principal(authentication()))
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.data.trip").isEmpty)

        mockMvc.perform(get("/api/v1/trips").principal(authentication()))
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.data.currentTrips").isArray)
            .andExpect(jsonPath("$.data.historicalTrips").isArray)

        mockMvc.perform(get("/api/v1/trips").principal(authentication()).param("page", "0"))
            .andExpect(status().isBadRequest)
            .andExpect(jsonPath("$.error.code").value("invalid_request"))
    }

    @Test
    fun `calendar and generation context use exact bounded projections`() {
        whenever(service.calendar("account-1")).thenReturn(
            PersonalTripCalendarProjection(
                LocalDate.parse("2025-09-02"),
                LocalDate.parse("2026-09-01"),
                listOf(CalendarDayProjection(LocalDate.parse("2026-09-01"), 2))
            )
        )
        whenever(service.generationContext("account-1", "route-1")).thenReturn(
            TripGenerationContextProjection(
                route = TripGenerationRouteProjection(
                    routeId = "route-1",
                    currentPublicRouteVersionId = "version-1",
                    routeType = "one_day",
                    generationEligibility = org.example.route.dto.RouteGenerationEligibility(true),
                    name = "贡嘎环线"
                ),
                personalEquipmentSummary = PersonalEquipmentSummary(0, 1, WeightProjection(0), 0),
                equipmentLists = listOf(
                    TripGenerationEquipmentListOption(
                        equipmentListId = "list-1",
                        name = "周末",
                        summary = EquipmentListSummary(0, WeightProjection(0), 0)
                    )
                )
            )
        )

        mockMvc.perform(get("/api/v1/trips/calendar").principal(authentication()))
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.data.windowStartDate").value("2025-09-02"))
            .andExpect(jsonPath("$.data.days[0].tripCount").value(2))

        mockMvc.perform(get("/api/v1/public-routes/route-1/trip-generation-context").principal(authentication()))
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.data.route.routeId").value("route-1"))
            .andExpect(jsonPath("$.data.route.currentPublicRouteVersionId").value("version-1"))
            .andExpect(jsonPath("$.data.equipmentLists[0].equipmentListId").value("list-1"))
            .andExpect(jsonPath("$.data.equipmentLists[0].members").doesNotExist())
    }

    @Test
    fun `generate requires idempotency key rejects unknown fields and preserves result status`() {
        whenever(service.generate(eq("account-1"), eq("command-1"), any())).thenReturn(
            GenerateTripResult.TransportSelectionRequired(
                selection = org.example.trip.personal.dto.TransportSelectionProjection(
                    selectionId = "selection-1",
                    options = listOf(
                        org.example.trip.personal.dto.TransportOptionProjection(
                            transportOptionId = "option-a",
                            transferCount = unavailableValue(),
                            estimatedArrivalAt = unavailableValue(),
                            estimatedDuration = unavailableValue(),
                            verificationItems = emptyList()
                        ),
                        org.example.trip.personal.dto.TransportOptionProjection(
                            transportOptionId = "option-b",
                            transferCount = unavailableValue(),
                            estimatedArrivalAt = unavailableValue(),
                            estimatedDuration = unavailableValue(),
                            verificationItems = listOf("核验接驳")
                        )
                    )
                )
            )
        )
        val body = """{"routeId":"route-1","routeVersionId":"version-1","departureCity":"上海","startDate":"2026-09-02"}"""

        mockMvc.perform(
            post("/api/v1/trips")
                .principal(authentication())
                .contentType(MediaType.APPLICATION_JSON)
                .content(body)
        )
            .andExpect(status().isBadRequest)
            .andExpect(jsonPath("$.error.code").value("invalid_request"))

        mockMvc.perform(
            post("/api/v1/trips")
                .principal(authentication())
                .header("Idempotency-Key", "command-1")
                .contentType(MediaType.APPLICATION_JSON)
                .content(body.replace("}", ",\"name\":\"forbidden\"}"))
        )
            .andExpect(status().isBadRequest)
            .andExpect(jsonPath("$.error.code").value("invalid_request"))

        mockMvc.perform(
            post("/api/v1/trips")
                .principal(authentication())
                .header("Idempotency-Key", "command-1")
                .contentType(MediaType.APPLICATION_JSON)
                .content(body)
        )
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.data.resultType").value("transport_selection_required"))
            .andExpect(jsonPath("$.data.selection.options.length()").value(2))
    }

    @Test
    fun `foreign trip is hidden behind trip not found`() {
        whenever(service.detail("account-1", "other-trip")).thenThrow(
            ApiContractException(org.springframework.http.HttpStatus.NOT_FOUND, "trip_not_found", "行程不存在")
        )

        mockMvc.perform(get("/api/v1/trips/other-trip").principal(authentication()))
            .andExpect(status().isNotFound)
            .andExpect(jsonPath("$.error.code").value("trip_not_found"))
    }

    @Test
    fun `cancellation validates idempotency key before request fields`() {
        mockMvc.perform(
            post("/api/v1/trips/trip-1/cancellations")
                .principal(authentication())
                .contentType(MediaType.APPLICATION_JSON)
                .content("""{"unexpected":true}""")
        )
            .andExpect(status().isBadRequest)
            .andExpect(jsonPath("$.error.code").value("invalid_request"))
            .andExpect(jsonPath("$.error.message").value("缺少 Idempotency-Key"))
    }

    @Test
    fun `cancellation rejects unknown fields after accepting an idempotency key`() {
        mockMvc.perform(
            post("/api/v1/trips/trip-1/cancellations")
                .principal(authentication())
                .header("Idempotency-Key", "cancel-1")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""{"expectedRevision":"revision-1","confirmed":true,"owner_id":"forbidden"}""")
        )
            .andExpect(status().isBadRequest)
            .andExpect(jsonPath("$.error.code").value("invalid_request"))
            .andExpect(jsonPath("$.error.message").value("请求包含未定义字段"))
    }

    private fun unavailableValue() = org.example.trip.personal.dto.QualifiedValueProjection<Any>(
        confidence = org.example.trip.personal.dto.InformationConfidenceProjection(
            status = "unavailable",
            category = "dynamic_external_information"
        )
    )

    private fun authentication() = UsernamePasswordAuthenticationToken(
        CustomUserDetails(userId = "account-1", username = "account-1", password = ""),
        null,
        emptyList()
    )
}
