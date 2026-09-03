package org.example.trip.personal.controller

import org.example.common.contract.ApiContractException
import org.example.common.contract.TargetApiExceptionHandler
import org.example.config.JacksonConfig
import org.example.equipment.dto.WeightProjection
import org.example.security.CustomUserDetails
import org.example.security.JwtAuthenticationFilter
import org.example.trip.personal.dto.TripEquipmentItemProjection
import org.example.trip.personal.dto.TripEquipmentProjection
import org.example.trip.personal.dto.TripEquipmentSnapshotProjection
import org.example.trip.personal.dto.TripEquipmentSummaryProjection
import org.example.trip.personal.dto.TripEquipmentTripProjection
import org.example.trip.personal.service.TripEquipmentApplicationService
import org.junit.jupiter.api.Test
import org.mockito.kotlin.any
import org.mockito.kotlin.eq
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest
import org.springframework.boot.test.mock.mockito.MockBean
import org.springframework.context.annotation.Import
import org.springframework.dao.DataAccessResourceFailureException
import org.springframework.http.MediaType
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.status
import java.time.LocalDate

@WebMvcTest(controllers = [TripEquipmentController::class])
@AutoConfigureMockMvc(addFilters = false)
@Import(JacksonConfig::class, TargetApiExceptionHandler::class)
class TripEquipmentApiContractTest {
    @Autowired
    private lateinit var mockMvc: MockMvc

    @MockBean
    private lateinit var service: TripEquipmentApplicationService

    @MockBean
    private lateinit var jwtAuthenticationFilter: JwtAuthenticationFilter

    @Test
    fun `get returns only the bounded current snapshot projection`() {
        whenever(service.getEquipment("account-1", "trip-1")).thenReturn(projection())

        mockMvc.perform(get("/api/v1/trips/trip-1/equipment").principal(authentication()))
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.data.trip.identity").value("trip-1"))
            .andExpect(jsonPath("$.data.trip.routeName").value("贡嘎环线"))
            .andExpect(jsonPath("$.data.revision").value("revision-1"))
            .andExpect(jsonPath("$.data.editable").value(true))
            .andExpect(jsonPath("$.data.snapshot.identity").value("snapshot-1"))
            .andExpect(jsonPath("$.data.snapshot.items[0].identity").value("item-1"))
            .andExpect(jsonPath("$.data.snapshot.items[0].unitWeight").doesNotExist())
            .andExpect(jsonPath("$.data.snapshot.summary.knownTotalWeight.grams").value(0))
            .andExpect(jsonPath("$.data.owner").doesNotExist())
            .andExpect(jsonPath("$.data.snapshot.suppressions").doesNotExist())
    }

    @Test
    fun `all endpoints reject query parameters`() {
        whenever(service.getEquipment("account-1", "trip-1")).thenReturn(projection())

        mockMvc.perform(
            get("/api/v1/trips/trip-1/equipment")
                .principal(authentication())
                .param("snapshot", "snapshot-1")
        )
            .andExpect(status().isBadRequest)
            .andExpect(jsonPath("$.error.code").value("invalid_request"))

        mockMvc.perform(
            post("/api/v1/trips/trip-1/equipment/items")
                .principal(authentication())
                .param("owner", "account-1")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""{"name":"水壶","quantity":1}""")
        )
            .andExpect(status().isBadRequest)
            .andExpect(jsonPath("$.error.code").value("invalid_request"))

        mockMvc.perform(
            patch("/api/v1/trips/trip-1/equipment/items/item-1")
                .principal(authentication())
                .param("expand", "relations")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""{"quantity":2}""")
        )
            .andExpect(status().isBadRequest)
            .andExpect(jsonPath("$.error.code").value("invalid_request"))

        mockMvc.perform(
            delete("/api/v1/trips/trip-1/equipment/items/item-1")
                .principal(authentication())
                .param("confirm", "true")
        )
            .andExpect(status().isBadRequest)
            .andExpect(jsonPath("$.error.code").value("invalid_request"))

        mockMvc.perform(
            post("/api/v1/trips/trip-1/equipment:recheck-ownership")
                .principal(authentication())
                .param("force", "true")
        )
            .andExpect(status().isBadRequest)
            .andExpect(jsonPath("$.error.code").value("invalid_request"))
    }

    @Test
    fun `create rejects forbidden fields and requires its two fields`() {
        mockMvc.perform(
            post("/api/v1/trips/trip-1/equipment/items")
                .principal(authentication())
                .contentType(MediaType.APPLICATION_JSON)
                .content("""{"name":"水壶","quantity":1,"source":"user_added","ownershipStatus":"owned","summary":{}}""")
        )
            .andExpect(status().isBadRequest)
            .andExpect(jsonPath("$.error.code").value("invalid_request"))

        mockMvc.perform(
            post("/api/v1/trips/trip-1/equipment/items")
                .principal(authentication())
                .contentType(MediaType.APPLICATION_JSON)
                .content("""{"name":"水壶"}""")
        )
            .andExpect(status().isBadRequest)
            .andExpect(jsonPath("$.error.code").value("invalid_request"))
    }

    @Test
    fun `create accepts bounded body and returns complete projection`() {
        whenever(service.createItem(eq("account-1"), eq("trip-1"), any())).thenReturn(projection("revision-2"))

        mockMvc.perform(
            post("/api/v1/trips/trip-1/equipment/items")
                .principal(authentication())
                .contentType(MediaType.APPLICATION_JSON)
                .content("""{"name":" 水壶 ","quantity":2,"unitWeight":{"grams":180},"note":"随身"}""")
        )
            .andExpect(status().isCreated)
            .andExpect(jsonPath("$.data.revision").value("revision-2"))
            .andExpect(jsonPath("$.data.snapshot.items").isArray)

        verify(service).createItem(eq("account-1"), eq("trip-1"), any())
    }

    @Test
    fun `patch distinguishes omitted and explicit null fields and rejects empty body`() {
        whenever(service.updateItem(eq("account-1"), eq("trip-1"), eq("item-1"), any()))
            .thenReturn(projection("revision-2"))

        mockMvc.perform(
            patch("/api/v1/trips/trip-1/equipment/items/item-1")
                .principal(authentication())
                .contentType(MediaType.APPLICATION_JSON)
                .content("""{"unitWeight":null,"note":null}""")
        )
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.data.revision").value("revision-2"))

        mockMvc.perform(
            patch("/api/v1/trips/trip-1/equipment/items/item-1")
                .principal(authentication())
                .contentType(MediaType.APPLICATION_JSON)
                .content("{}")
        )
            .andExpect(status().isUnprocessableEntity)
            .andExpect(jsonPath("$.error.code").value("validation_failed"))

        mockMvc.perform(
            patch("/api/v1/trips/trip-1/equipment/items/item-1")
                .principal(authentication())
                .contentType(MediaType.APPLICATION_JSON)
                .content("""{"name":null}""")
        )
            .andExpect(status().isBadRequest)
            .andExpect(jsonPath("$.error.code").value("invalid_request"))
    }

    @Test
    fun `delete returns complete projection and does not expose suppression`() {
        whenever(service.deleteItem("account-1", "trip-1", "item-1")).thenReturn(projection("revision-2"))

        mockMvc.perform(
            delete("/api/v1/trips/trip-1/equipment/items/item-1")
                .principal(authentication())
        )
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.data.revision").value("revision-2"))
            .andExpect(jsonPath("$.data.snapshot.suppressions").doesNotExist())
    }

    @Test
    fun `delete rejects an undefined request body`() {
        mockMvc.perform(
            delete("/api/v1/trips/trip-1/equipment/items/item-1")
                .principal(authentication())
                .contentType(MediaType.APPLICATION_JSON)
                .content("{}")
        )
            .andExpect(status().isBadRequest)
            .andExpect(jsonPath("$.error.code").value("invalid_request"))
    }

    @Test
    fun `recheck requires a zero byte body`() {
        whenever(service.recheckOwnership("account-1", "trip-1")).thenReturn(projection("revision-2"))

        mockMvc.perform(
            post("/api/v1/trips/trip-1/equipment:recheck-ownership")
                .principal(authentication())
        )
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.data.revision").value("revision-2"))

        mockMvc.perform(
            post("/api/v1/trips/trip-1/equipment:recheck-ownership")
                .principal(authentication())
                .contentType(MediaType.APPLICATION_JSON)
                .content("{}")
        )
            .andExpect(status().isBadRequest)
            .andExpect(jsonPath("$.error.code").value("invalid_request"))
    }

    @Test
    fun `controller scoped errors use the common error envelope`() {
        whenever(service.getEquipment("account-1", "foreign-trip")).thenThrow(ApiContractException.notFound())

        mockMvc.perform(get("/api/v1/trips/foreign-trip/equipment").principal(authentication()))
            .andExpect(status().isNotFound)
            .andExpect(jsonPath("$.error.code").value("resource_not_found"))
            .andExpect(jsonPath("$.data").doesNotExist())
    }

    @Test
    fun `trip equipment data failures use the trip equipment error code`() {
        whenever(service.getEquipment("account-1", "trip-1"))
            .thenThrow(DataAccessResourceFailureException("database unavailable"))

        mockMvc.perform(get("/api/v1/trips/trip-1/equipment").principal(authentication()))
            .andExpect(status().isServiceUnavailable)
            .andExpect(jsonPath("$.error.code").value("trip_equipment_unavailable"))
            .andExpect(jsonPath("$.data").doesNotExist())
    }

    @Test
    fun `missing authentication is rejected before service invocation`() {
        mockMvc.perform(get("/api/v1/trips/trip-1/equipment"))
            .andExpect(status().isUnauthorized)
            .andExpect(jsonPath("$.error.code").value("authentication_required"))
    }

    private fun authentication() = UsernamePasswordAuthenticationToken(
        CustomUserDetails(userId = "account-1", username = "account-1", password = ""),
        null,
        emptyList()
    )

    private fun projection(revision: String = "revision-1") = TripEquipmentProjection(
        trip = TripEquipmentTripProjection(
            identity = "trip-1",
            status = "planned",
            routeName = "贡嘎环线",
            startDate = LocalDate.parse("2026-09-02"),
            endDate = LocalDate.parse("2026-09-03")
        ),
        revision = revision,
        editable = true,
        snapshot = TripEquipmentSnapshotProjection(
            identity = "snapshot-1",
            items = listOf(
                TripEquipmentItemProjection(
                    identity = "item-1",
                    name = "水壶",
                    quantity = 1,
                    source = "user_added",
                    ownershipStatus = "unconfirmed_owned"
                )
            ),
            summary = TripEquipmentSummaryProjection(1, WeightProjection(0), 1, 0, 1)
        )
    )
}
