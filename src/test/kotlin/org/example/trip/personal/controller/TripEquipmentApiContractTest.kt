package org.example.trip.personal.controller

import org.example.common.contract.ApiContractException
import org.example.common.contract.TargetApiExceptionHandler
import org.example.config.JacksonConfig
import org.example.equipment.dto.WeightProjection
import org.example.security.CustomUserDetails
import org.example.security.JwtAuthenticationFilter
import org.example.trip.personal.dto.*
import org.example.trip.personal.dto.TripEquipmentItemProjection
import org.example.trip.personal.dto.TripEquipmentProjection
import org.example.trip.personal.dto.TripEquipmentSnapshotProjection
import org.example.trip.personal.dto.TripEquipmentSummaryProjection
import org.example.trip.personal.dto.TripEquipmentTripProjection
import org.example.trip.personal.service.TripEquipmentApplicationService
import org.junit.jupiter.api.Test
import org.mockito.kotlin.any
import org.mockito.kotlin.isNull
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
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.status
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.header
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
    fun `equipment reads expose the same strong revision ETag`() {
        whenever(service.getEquipment("account-1", "trip-1")).thenReturn(projection())
        mockMvc.perform(get("/api/v1/trips/trip-1/equipment").principal(authentication()))
            .andExpect(status().isOk)
            .andExpect(header().string("ETag", "\"revision-1\""))
    }

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
        whenever(service.createItem(eq("account-1"), eq("trip-1"), any(), isNull())).thenReturn(projection("revision-2"))

        mockMvc.perform(
            post("/api/v1/trips/trip-1/equipment/items")
                .principal(authentication())
                .contentType(MediaType.APPLICATION_JSON)
                .content("""{"name":" 水壶 ","quantity":2,"unitWeight":{"grams":180},"note":"随身"}""")
        )
            .andExpect(status().isCreated)
            .andExpect(jsonPath("$.data.revision").value("revision-2"))
            .andExpect(jsonPath("$.data.snapshot.items").isArray)

        verify(service).createItem(eq("account-1"), eq("trip-1"), any(), isNull())
    }

    @Test
    fun `patch distinguishes omitted and explicit null fields and rejects empty body`() {
        whenever(service.updateItem(eq("account-1"), eq("trip-1"), eq("item-1"), any(), isNull()))
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

    @Test
    fun `new selection and append commands require both strict headers and return complete ETags`() {
        whenever(service.fromPersonal(eq("account-1"), eq("trip-1"), any(), eq("revision-1"), eq("key"))).thenReturn(projection("revision-2"))
        whenever(service.appendList(eq("account-1"), eq("trip-1"), any(), eq("revision-1"), eq("key"))).thenReturn(projection("revision-2"))
        listOf(
            "/api/v1/trips/trip-1/equipment/items:from-personal" to """{"personalEquipmentId":"p","quantity":2}""",
            "/api/v1/trips/trip-1/equipment:append-list" to """{"equipmentListId":"l"}"""
        ).forEach { (url, body) ->
            mockMvc.perform(post(url).principal(authentication()).contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isBadRequest)
            mockMvc.perform(post(url).principal(authentication()).header("If-Match", "\"revision-1\"")
                .contentType(MediaType.APPLICATION_JSON).content(body)).andExpect(status().isBadRequest)
            mockMvc.perform(post(url).principal(authentication()).header("If-Match", "\"revision-1\"").header("Idempotency-Key", "key")
                .contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().`is`(if (url.endsWith("from-personal")) 201 else 200))
                .andExpect(header().string("ETag", "\"revision-2\""))
        }
    }

    @Test
    fun `referenced personal and list identities reject blank values before invoking service`() {
        listOf("", "   ").forEach { identity ->
            listOf(
                post("/api/v1/trips/trip-1/equipment/items:from-personal") to """{"personalEquipmentId":"$identity","quantity":1}""",
                put("/api/v1/trips/trip-1/equipment/items/item-1/personal-equipment") to """{"personalEquipmentId":"$identity","quantity":1}""",
                post("/api/v1/trips/trip-1/equipment:append-list") to """{"equipmentListId":"$identity"}"""
            ).forEach { (request, body) ->
                mockMvc.perform(request.principal(authentication()).header("If-Match", "\"revision-1\"").header("Idempotency-Key", "key")
                    .contentType(MediaType.APPLICATION_JSON).content(body))
                    .andExpect(status().isBadRequest)
                    .andExpect(jsonPath("$.error.code").value("invalid_request"))
            }
        }
        org.mockito.kotlin.verifyNoInteractions(service)
    }

    @Test
    fun `new item extensions require If Match and preserve fractional JSON numbers`() {
        whenever(service.createItem(eq("account-1"), eq("trip-1"), any(), eq("revision-1"))).thenReturn(projection("revision-2"))
        val body = """{"name":"Socks","quantity":2,"unitWeight":{"grams":20.25},"category":"clothing","carryAllocation":{"backpackQuantity":1,"wornQuantity":1,"handheldQuantity":0}}"""
        mockMvc.perform(post("/api/v1/trips/trip-1/equipment/items").principal(authentication()).contentType(MediaType.APPLICATION_JSON).content(body))
            .andExpect(status().isBadRequest)
        mockMvc.perform(post("/api/v1/trips/trip-1/equipment/items").principal(authentication()).header("If-Match", "\"revision-1\"")
            .contentType(MediaType.APPLICATION_JSON).content(body)).andExpect(status().isCreated)
        val command = org.mockito.kotlin.argumentCaptor<CreateTripEquipmentItemCommand>()
        verify(service).createItem(eq("account-1"), eq("trip-1"), command.capture(), eq("revision-1"))
        org.junit.jupiter.api.Assertions.assertEquals(20.25, command.firstValue.unitWeightGrams)
        org.junit.jupiter.api.Assertions.assertEquals(TripCarryAllocation(1, 1, 0), command.firstValue.configuration.carryAllocation)
    }

    @Test
    fun `strict JSON rejects coercion unknown nested fields and unknown enums`() {
        listOf(
            """{"quantity":1.5}""", """{"quantity":"2"}""", """{"name":2}""",
            """{"unitWeight":{"grams":"20"}}""", """{"unitWeight":{"grams":20,"net":true}}""",
            """{"category":"unknown"}""", """{"carryAllocation":{"backpackQuantity":1,"wornQuantity":0}}""",
            """{"carryAllocation":{"backpackQuantity":1,"wornQuantity":0,"handheldQuantity":0,"other":0}}""",
            """{"supply":{"kind":"food","amount":{"value":1,"unit":"kg"},"containerMode":"included"}}""",
            """{"packedQuantity":1}"""
        ).forEach { body ->
            mockMvc.perform(patch("/api/v1/trips/trip-1/equipment/items/item-1").principal(authentication()).header("If-Match", "\"revision-1\"")
                .contentType(MediaType.APPLICATION_JSON).content(body)).andExpect(status().isBadRequest)
                .andExpect(jsonPath("$.error.code").value("invalid_request"))
        }
    }

    @Test
    fun `weak wildcard multiple empty and duplicate revision headers are rejected`() {
        listOf("W/\"revision-1\"", "*", "\"\"", "\"a\", \"b\"", "revision-1").forEach { tag ->
            mockMvc.perform(put("/api/v1/trips/trip-1/equipment/items/item-1/packing").principal(authentication()).header("If-Match", tag)
                .contentType(MediaType.APPLICATION_JSON).content("""{"packedQuantity":1}""")).andExpect(status().isBadRequest)
        }
        mockMvc.perform(put("/api/v1/trips/trip-1/equipment/items/item-1/packing").principal(authentication()).header("If-Match", "\"a\"", "\"b\"")
            .contentType(MediaType.APPLICATION_JSON).content("""{"packedQuantity":1}""")).andExpect(status().isBadRequest)
    }

    @Test
    fun `replacement packing reset and preparation use the new task endpoints`() {
        whenever(service.replacePersonal(eq("account-1"), eq("trip-1"), eq("item-1"), any(), eq("revision-1"))).thenReturn(projection("revision-2"))
        whenever(service.setPacking("account-1", "trip-1", "item-1", 1, "revision-1")).thenReturn(projection("revision-2"))
        whenever(service.resetPacking("account-1", "trip-1", "revision-1")).thenReturn(projection("revision-2"))
        listOf(
            "/api/v1/trips/trip-1/equipment/items/item-1/personal-equipment" to """{"personalEquipmentId":"p","quantity":1}""",
            "/api/v1/trips/trip-1/equipment/items/item-1/packing" to """{"packedQuantity":1}"""
        ).forEach { (url, body) ->
            mockMvc.perform(put(url).principal(authentication()).header("If-Match", "\"revision-1\"")
                .contentType(MediaType.APPLICATION_JSON).content(body)).andExpect(status().isOk).andExpect(header().string("ETag", "\"revision-2\""))
        }
        mockMvc.perform(post("/api/v1/trips/trip-1/equipment:reset-packing").principal(authentication()).header("If-Match", "\"revision-1\""))
            .andExpect(status().isOk)
        mockMvc.perform(post("/api/v1/trips/trip-1/equipment:reset-packing").principal(authentication()).header("If-Match", "\"revision-1\"").content(" "))
            .andExpect(status().isBadRequest)
        val bucket = EquipmentWeightBucket(WeightProjection(0.0), 0)
        whenever(service.preparation("account-1", "trip-1")).thenReturn(TripPreparationProjection("trip-1", "revision-1",
            TripWeightBreakdown(bucket, bucket, bucket, bucket), TripBackpackBreakdown(bucket, bucket, bucket, bucket, bucket), TripPackingProgress(0, 0, 0, 0)))
        mockMvc.perform(get("/api/v1/trips/trip-1/equipment/preparation").principal(authentication()))
            .andExpect(status().isOk).andExpect(header().string("ETag", "\"revision-1\""))
            .andExpect(jsonPath("$.data.weightBreakdown.unallocated.missingWeightItemCount").value(0))
            .andExpect(jsonPath("$.data.items").doesNotExist())
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
            summary = TripEquipmentSummaryProjection(1, WeightProjection(0.0), 1, 0, 1)
        )
    )
}
