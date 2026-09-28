package org.example.equipment.controller

import org.example.common.contract.TargetApiExceptionHandler
import org.example.config.JacksonConfig
import org.example.equipment.dto.EquipmentListDetailProjection
import org.example.equipment.dto.EquipmentListSummary
import org.example.equipment.dto.PersonalEquipmentCollectionResponse
import org.example.equipment.dto.PersonalEquipmentMutationResponse
import org.example.equipment.dto.PersonalEquipmentProjection
import org.example.equipment.dto.PersonalEquipmentSummary
import org.example.equipment.dto.WeightProjection
import org.example.equipment.service.PersonalEquipmentApplicationService
import org.example.security.CustomUserDetails
import org.example.security.JwtAuthenticationFilter
import org.junit.jupiter.api.Test
import org.mockito.kotlin.argumentCaptor
import org.mockito.kotlin.verify
import org.junit.jupiter.api.Assertions.*
import org.example.equipment.dto.EquipmentListCreateResponse
import org.example.equipment.service.CreatePersonalEquipmentCommand
import org.example.equipment.service.UpdatePersonalEquipmentCommand
import org.example.equipment.service.PersonalEquipmentFilter
import org.example.equipment.service.AdoptCatalogEquipmentCommand
import org.mockito.kotlin.any
import org.mockito.kotlin.eq
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

@WebMvcTest(controllers = [PersonalEquipmentController::class, UserEquipmentListController::class])
@AutoConfigureMockMvc(addFilters = false)
@Import(JacksonConfig::class, TargetApiExceptionHandler::class)
class PersonalEquipmentApiContractTest {
    @Autowired
    private lateinit var mockMvc: MockMvc

    @MockBean
    private lateinit var service: PersonalEquipmentApplicationService

    @MockBean
    private lateinit var jwtAuthenticationFilter: JwtAuthenticationFilter

    @Test
    fun `collection uses current account data envelope and bounded projection`() {
        whenever(service.getEquipmentCollection("account-1")).thenReturn(
            PersonalEquipmentCollectionResponse(
                items = listOf(PersonalEquipmentProjection("equipment-1", "帐篷", 2, WeightProjection(500.0))),
                summary = summary(itemCount = 1, listCount = 2, grams = 1000.0)
            )
        )

        mockMvc.perform(get("/api/v1/personal-equipment").principal(authentication()))
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.data.items[0].identity").value("equipment-1"))
            .andExpect(jsonPath("$.data.items[0].ownedQuantity").value(2))
            .andExpect(jsonPath("$.data.items[0].unitWeight.grams").value(500))
            .andExpect(jsonPath("$.data.summary.knownTotalWeight.grams").value(1000))
            .andExpect(jsonPath("$.data.items[0].ownerId").doesNotExist())
            .andExpect(jsonPath("$.success").doesNotExist())
    }

    @Test
    fun `create rejects unknown owner and summary fields`() {
        mockMvc.perform(
            post("/api/v1/personal-equipment")
                .principal(authentication())
                .contentType(MediaType.APPLICATION_JSON)
                .content("""{"name":"帐篷","ownedQuantity":1,"ownerId":"other","summary":{}}""")
        )
            .andExpect(status().isBadRequest)
            .andExpect(jsonPath("$.error.code").value("invalid_request"))
            .andExpect(jsonPath("$.data").doesNotExist())
    }

    @Test
    fun `create returns 201 and omits unknown weight`() {
        whenever(service.createEquipment(eq("account-1"), any())).thenReturn(
            PersonalEquipmentMutationResponse(
                item = PersonalEquipmentProjection("equipment-1", "登山杖", 1, null),
                summary = summary(itemCount = 1, missingCount = 1)
            )
        )

        mockMvc.perform(
            post("/api/v1/personal-equipment")
                .principal(authentication())
                .contentType(MediaType.APPLICATION_JSON)
                .content("""{"name":"登山杖","ownedQuantity":1}""")
        )
            .andExpect(status().isCreated)
            .andExpect(jsonPath("$.data.item.identity").value("equipment-1"))
            .andExpect(jsonPath("$.data.item.unitWeight").doesNotExist())
            .andExpect(jsonPath("$.data.summary.knownTotalWeight.grams").value(0))
    }

    @Test
    fun `create and patch reject a unit weight object without grams`() {
        mockMvc.perform(
            post("/api/v1/personal-equipment")
                .principal(authentication())
                .contentType(MediaType.APPLICATION_JSON)
                .content("""{"name":"登山杖","ownedQuantity":1,"unitWeight":{}}""")
        )
            .andExpect(status().isBadRequest)
            .andExpect(jsonPath("$.error.code").value("invalid_request"))

        mockMvc.perform(
            patch("/api/v1/personal-equipment/equipment-1")
                .principal(authentication())
                .contentType(MediaType.APPLICATION_JSON)
                .content("""{"unitWeight":{}}""")
        )
            .andExpect(status().isBadRequest)
            .andExpect(jsonPath("$.error.code").value("invalid_request"))
    }

    @Test
    fun `patch accepts explicit null only for clearing unit weight`() {
        whenever(service.updateEquipment(eq("account-1"), eq("equipment-1"), any())).thenReturn(
            PersonalEquipmentMutationResponse(
                PersonalEquipmentProjection("equipment-1", "登山杖", 1, null),
                summary(itemCount = 1, missingCount = 1)
            )
        )

        mockMvc.perform(
            patch("/api/v1/personal-equipment/equipment-1")
                .principal(authentication())
                .contentType(MediaType.APPLICATION_JSON)
                .content("""{"unitWeight":null}""")
        )
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.data.item.unitWeight").doesNotExist())

        mockMvc.perform(
            patch("/api/v1/personal-equipment/equipment-1")
                .principal(authentication())
                .contentType(MediaType.APPLICATION_JSON)
                .content("{}")
        )
            .andExpect(status().isUnprocessableEntity)
            .andExpect(jsonPath("$.error.code").value("validation_failed"))
    }

    @Test
    fun `delete validates its only query and requires explicit true`() {
        mockMvc.perform(
            delete("/api/v1/personal-equipment/equipment-1")
                .principal(authentication())
        )
            .andExpect(status().isConflict)
            .andExpect(jsonPath("$.error.code").value("deletion_confirmation_required"))

        mockMvc.perform(
            delete("/api/v1/personal-equipment/equipment-1")
                .principal(authentication())
                .param("confirmListRemoval", "true")
                .param("owner", "account-1")
        )
            .andExpect(status().isBadRequest)
            .andExpect(jsonPath("$.error.code").value("invalid_request"))
    }

    @Test
    fun `list and member endpoints use fixed methods and reject unknown fields`() {
        val detail = EquipmentListDetailProjection(
            identity = "list-1",
            name = "周末",
            members = emptyList(),
            summary = EquipmentListSummary(0, WeightProjection(0.0), 0)
        )
        whenever(service.getEquipmentList("account-1", "list-1")).thenReturn(detail)

        mockMvc.perform(get("/api/v1/equipment-lists/list-1").principal(authentication()))
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.data.identity").value("list-1"))
            .andExpect(jsonPath("$.data.members").isEmpty)
            .andExpect(jsonPath("$.data.summary.knownTotalWeight.grams").value(0))

        mockMvc.perform(
            put("/api/v1/equipment-lists/list-1/members/equipment-1")
                .principal(authentication())
                .contentType(MediaType.APPLICATION_JSON)
                .content("""{"quantity":2,"tripId":"trip-1"}""")
        )
            .andExpect(status().isBadRequest)
            .andExpect(jsonPath("$.error.code").value("invalid_request"))
    }

    @Test
    fun `all target reads reject unknown query parameters`() {
        mockMvc.perform(
            get("/api/v1/equipment-lists")
                .principal(authentication())
                .param("limit", "20")
        )
            .andExpect(status().isBadRequest)
            .andExpect(jsonPath("$.error.code").value("invalid_request"))
    }

    @Test
    fun `personal equipment controllers map data failures to their domain error`() {
        whenever(service.getEquipmentCollection("account-1"))
            .thenThrow(DataAccessResourceFailureException("database unavailable"))
        whenever(service.getEquipmentLists("account-1"))
            .thenThrow(DataAccessResourceFailureException("database unavailable"))

        mockMvc.perform(get("/api/v1/personal-equipment").principal(authentication()))
            .andExpect(status().isServiceUnavailable)
            .andExpect(jsonPath("$.error.code").value("personal_equipment_unavailable"))
            .andExpect(jsonPath("$.data").doesNotExist())

        mockMvc.perform(get("/api/v1/equipment-lists").principal(authentication()))
            .andExpect(status().isServiceUnavailable)
            .andExpect(jsonPath("$.error.code").value("personal_equipment_unavailable"))
            .andExpect(jsonPath("$.data").doesNotExist())
    }

    @Test
    fun `personal optional fields preserve decimal weights and forbid JSON coercion`() {
        whenever(service.createEquipment(eq("account-1"), any())).thenReturn(PersonalEquipmentMutationResponse(
            PersonalEquipmentProjection("item", "袜子", 1, WeightProjection(12.5), unitWeightSource = "user_recorded"), summary()))
        mockMvc.perform(post("/api/v1/personal-equipment").principal(authentication()).contentType(MediaType.APPLICATION_JSON)
            .content("""{"name":"袜子","ownedQuantity":1,"unitWeight":{"grams":12.5},"category":"clothing","equipmentType":"socks","unitLabel":"双"}"""))
            .andExpect(status().isCreated).andExpect(jsonPath("$.data.item.unitWeight.grams").value(12.5))
        val command = argumentCaptor<CreatePersonalEquipmentCommand>()
        verify(service).createEquipment(eq("account-1"), command.capture())
        assertEquals(12.5, command.firstValue.unitWeightGrams)
        assertEquals("双", command.firstValue.unitLabel)
        listOf(
            """{"name":"袜子","ownedQuantity":1,"hasCategory":true}""",
            """{"name":"袜子","ownedQuantity":1,"hasName":true}""",
            """{"name":42,"ownedQuantity":1}""",
            """{"name":"袜子","ownedQuantity":1.2}""",
            """{"name":"袜子","ownedQuantity":"1"}""",
            """{"name":"袜子","ownedQuantity":1,"unitWeight":{"grams":"12.5"}}""",
            """{"name":"袜子","ownedQuantity":1,"unitWeight":{"grams":1e999}}""",
            """{"name":"袜子","ownedQuantity":1,"variant":null}""",
            """{"name":"袜子","ownedQuantity":1,"category":null}""",
            """{"name":"袜子","ownedQuantity":1,"catalogEquipmentId":"catalog"}""",
            """{"name":"袜子","ownedQuantity":1,"unitWeightSource":"catalog_reference"}"""
        ).forEach { body ->
            mockMvc.perform(post("/api/v1/personal-equipment").principal(authentication()).contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isBadRequest).andExpect(jsonPath("$.error.code").value("invalid_request"))
        }
    }

    @Test
    fun `personal query parameters have strict presence and forward all filter conditions`() {
        whenever(service.getEquipmentCollection(eq("account-1"), any())).thenReturn(PersonalEquipmentCollectionResponse(emptyList(), summary(itemCount = 5)))
        mockMvc.perform(get("/api/v1/personal-equipment").principal(authentication()).param("keyword", " socks ")
            .param("category", "clothing").param("equipmentType", "socks").param("missingWeight", "true"))
            .andExpect(status().isOk).andExpect(jsonPath("$.data.summary.equipmentItemCount").value(5))
        verify(service).getEquipmentCollection("account-1", PersonalEquipmentFilter(" socks ", "clothing", "socks", true))
        listOf("keyword" to arrayOf("a", "b"), "category" to arrayOf(""), "missingWeight" to arrayOf("TRUE"), "sort" to arrayOf("name")).forEach { (key, values) ->
            mockMvc.perform(get("/api/v1/personal-equipment").principal(authentication()).param(key, *values)).andExpect(status().isBadRequest)
        }
    }

    @Test
    fun `catalog adoption uses exact colon endpoint raw identities and explicit-null flags`() {
        whenever(service.adoptCatalogEquipment(eq("account-1"), eq("key"), any())).thenReturn(PersonalEquipmentMutationResponse(
            PersonalEquipmentProjection("item", "袜子", 1, catalogEquipmentId = " catalog "), summary()))
        mockMvc.perform(post("/api/v1/personal-equipment:from-catalog").principal(authentication()).header("Idempotency-Key", "key")
            .contentType(MediaType.APPLICATION_JSON).content("""{"catalogEquipmentId":" catalog ","ownedQuantity":1,"unitWeight":null,"variant":null}"""))
            .andExpect(status().isCreated).andExpect(jsonPath("$.data.item.catalogEquipmentId").value(" catalog "))
        val command = argumentCaptor<AdoptCatalogEquipmentCommand>()
        verify(service).adoptCatalogEquipment(eq("account-1"), eq("key"), command.capture())
        assertTrue(command.firstValue.hasUnitWeight); assertTrue(command.firstValue.hasVariant)
        assertFalse(command.firstValue.hasUnitLabel); assertNull(command.firstValue.unitWeightGrams)
    }

    @Test
    fun `new commands reject missing repeated and blank idempotency headers`() {
        val tasks = listOf(
            "/api/v1/personal-equipment:from-catalog" to """{"catalogEquipmentId":"c","ownedQuantity":1}""",
            "/api/v1/equipment-lists/list:copy" to """{"name":"副本"}"""
        )
        tasks.forEach { (path, body) ->
            listOf(emptyArray(), arrayOf(" "), arrayOf("one", "two")).forEach { keys ->
                val request = post(path).principal(authentication()).contentType(MediaType.APPLICATION_JSON).content(body)
                if (keys.isNotEmpty()) request.header("Idempotency-Key", *keys)
                mockMvc.perform(request).andExpect(status().isBadRequest).andExpect(jsonPath("$.error.code").value("invalid_request"))
            }
        }
        mockMvc.perform(post("/api/v1/personal-equipment:from-catalog").principal(authentication()).header("Idempotency-Key", "key")
            .contentType(MediaType.APPLICATION_JSON).content("""{"catalogEquipmentId":"c","ownedQuantity":1,"category":"clothing"}"""))
            .andExpect(status().isBadRequest)
    }

    @Test
    fun `copy returns new list task result without a generic mutation endpoint`() {
        val list = EquipmentListDetailProjection("copy", "副本", emptyList(), EquipmentListSummary(0, WeightProjection(0.0), 0))
        whenever(service.copyEquipmentList("account-1", "source", "copy-key", "副本")).thenReturn(EquipmentListCreateResponse(list, summary(listCount = 2)))
        mockMvc.perform(post("/api/v1/equipment-lists/source:copy").principal(authentication()).header("Idempotency-Key", "copy-key")
            .contentType(MediaType.APPLICATION_JSON).content("""{"name":"副本"}"""))
            .andExpect(status().isCreated).andExpect(jsonPath("$.data.equipmentList.identity").value("copy"))
            .andExpect(jsonPath("$.data.personalEquipmentSummary.equipmentListCount").value(2))
    }

    @Test
    fun `patch retains presence for clearing optional configuration and rejects readonly source`() {
        whenever(service.updateEquipment(eq("account-1"), eq("item"), any())).thenReturn(PersonalEquipmentMutationResponse(PersonalEquipmentProjection("item", "袜子", 1), summary()))
        mockMvc.perform(patch("/api/v1/personal-equipment/item").principal(authentication()).contentType(MediaType.APPLICATION_JSON)
            .content("""{"category":null,"equipmentType":null,"variant":null}"""))
            .andExpect(status().isOk)
        val command = argumentCaptor<UpdatePersonalEquipmentCommand>()
        verify(service).updateEquipment(eq("account-1"), eq("item"), command.capture())
        assertTrue(command.firstValue.hasCategory); assertTrue(command.firstValue.hasEquipmentType); assertTrue(command.firstValue.hasVariant)
        assertFalse(command.firstValue.hasUnitWeight)
        mockMvc.perform(patch("/api/v1/personal-equipment/item").principal(authentication()).contentType(MediaType.APPLICATION_JSON)
            .content("""{"catalogEquipmentId":"other"}""")).andExpect(status().isBadRequest)
    }

    private fun authentication(): UsernamePasswordAuthenticationToken = UsernamePasswordAuthenticationToken(
        CustomUserDetails(userId = "account-1", username = "account-1", password = ""),
        null,
        emptyList()
    )

    private fun summary(
        itemCount: Int = 0,
        listCount: Int = 0,
        grams: Double = 0.0,
        missingCount: Int = 0
    ) = PersonalEquipmentSummary(itemCount, listCount, WeightProjection(grams), missingCount)
}
