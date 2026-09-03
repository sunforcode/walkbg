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
                items = listOf(PersonalEquipmentProjection("equipment-1", "帐篷", 2, WeightProjection(500))),
                summary = summary(itemCount = 1, listCount = 2, grams = 1000)
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
            summary = EquipmentListSummary(0, WeightProjection(0), 0)
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

    private fun authentication(): UsernamePasswordAuthenticationToken = UsernamePasswordAuthenticationToken(
        CustomUserDetails(userId = "account-1", username = "account-1", password = ""),
        null,
        emptyList()
    )

    private fun summary(
        itemCount: Int = 0,
        listCount: Int = 0,
        grams: Long = 0,
        missingCount: Int = 0
    ) = PersonalEquipmentSummary(itemCount, listCount, WeightProjection(grams), missingCount)
}
