package org.example.equipment.catalog.controller

import com.fasterxml.jackson.databind.ObjectMapper
import io.swagger.v3.core.converter.ModelConverters
import org.example.common.contract.TargetApiExceptionHandler
import org.example.config.JacksonConfig
import org.example.equipment.catalog.dto.CatalogEquipmentDetailProjection
import org.example.equipment.catalog.model.*
import org.example.equipment.catalog.repository.EquipmentCatalogRepository
import org.example.equipment.catalog.service.EquipmentCatalogApplicationService
import org.example.equipment.catalog.service.EquipmentCatalogDomainService
import org.example.security.CustomUserDetails
import org.example.security.JwtAuthenticationFilter
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.mockito.kotlin.any
import org.mockito.kotlin.argumentCaptor
import org.mockito.kotlin.verify
import org.mockito.kotlin.verifyNoInteractions
import org.mockito.kotlin.whenever
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest
import org.springframework.boot.test.mock.mockito.MockBean
import org.springframework.context.annotation.Import
import org.springframework.dao.DataAccessResourceFailureException
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.*

@WebMvcTest(controllers = [EquipmentCatalogController::class])
@AutoConfigureMockMvc(addFilters = false)
@Import(JacksonConfig::class, TargetApiExceptionHandler::class, EquipmentCatalogApplicationService::class, EquipmentCatalogDomainService::class)
class EquipmentCatalogApiContractTest {
    @Autowired private lateinit var mvc: MockMvc
    @Autowired private lateinit var mapper: ObjectMapper
    @MockBean private lateinit var repository: EquipmentCatalogRepository
    @MockBean private lateinit var jwtAuthenticationFilter: JwtAuthenticationFilter

    @BeforeEach
    fun emptyCollections() {
        whenever(repository.findBrands()).thenReturn(emptyList())
        whenever(repository.findItems(any())).thenReturn(emptyList())
    }

    @Test
    fun `keyword search accepts literal normalized keyword`() {
        mvc.perform(get("$base/items").principal(auth()).param("keyword", "  TENT_50%  "))
            .andExpect(status().isOk)
    }

    @Test
    fun `brand and item lists use fixed camel case envelope sorted by opaque identity`() {
        whenever(repository.findBrands()).thenReturn(listOf(CatalogBrandRecord("b", "乙"), CatalogBrandRecord("A", "甲")))
        mvc.perform(get("$base/brands").principal(auth()))
            .andExpect(status().isOk).andExpect(jsonPath("$.data.items[0].identity").value("A"))
            .andExpect(jsonPath("$.data.items[1].name").value("乙"))
            .andExpect(jsonPath("$.success").doesNotExist())

        whenever(repository.findItems(any())).thenReturn(listOf(
            equipment("z", "sleeping_bag"),
            equipment("A", "backpack", CatalogSpecRecord(unitLabel = "件", unitWeightGrams = 735.5, weightScope = "含袋", capacityLiters = 55.0))
        ))
        val content = mvc.perform(get("$base/items").principal(auth()))
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.data.items[0].equipmentType").value("backpack"))
            .andExpect(jsonPath("$.data.items[0].specifications.unitWeight.grams").value(735.5))
            .andExpect(jsonPath("$.data.items[0].specifications.capacityLiters").doesNotExist())
            .andExpect(jsonPath("$.data.items[1].specifications").doesNotExist())
            .andExpect(jsonPath("$.data.items[0].brand").doesNotExist())
            .andExpect(jsonPath("$.data.items[0].model").doesNotExist())
            .andReturn().response.contentAsString
        val tree = mapper.readTree(content)
        assertEquals(setOf("data"), tree.fieldNames().asSequence().toSet())
        assertEquals(setOf("items"), tree["data"].fieldNames().asSequence().toSet())
        assertEquals(setOf("identity", "name", "category", "equipmentType", "specifications"), tree["data"]["items"][0].fieldNames().asSequence().toSet())
    }

    @Test
    fun `detail has only matching typed fields and missing is omitted not zero or null`() {
        val cases = listOf(
            Triple("backpack", CatalogSpecRecord(unitLabel = "件", capacityLiters = 50.5), setOf("unitLabel", "capacityLiters")),
            Triple("tent", CatalogSpecRecord(unitLabel = "套", capacityPersons = 2), setOf("unitLabel", "capacityPersons")),
            Triple("sleeping_bag", CatalogSpecRecord(unitLabel = "件", comfortTemperatureCelsius = 0.0, limitTemperatureCelsius = -12.5), setOf("unitLabel", "comfortTemperatureCelsius", "limitTemperatureCelsius")),
            Triple("sleeping_pad", CatalogSpecRecord(unitLabel = "件", padType = "air", rValue = 0.0, lengthCm = 183.5, maxWidthCm = 64.0, thicknessCm = 7.6), setOf("unitLabel", "padType", "rValue", "lengthCm", "maxWidthCm", "thicknessCm")),
            Triple("headlamp", CatalogSpecRecord(unitLabel = "件", unitWeightGrams = 55.5, weightScope = "含电池"), setOf("unitLabel", "unitWeight", "weightScope"))
        )
        cases.forEach { (type, spec, fields) ->
            whenever(repository.findById(type)).thenReturn(equipment(type, type, spec))
            val result = mvc.perform(get("$base/items/$type").principal(auth()))
                .andExpect(status().isOk).andReturn()
            val specification = mapper.readTree(result.response.contentAsString)["data"]["specifications"]
            assertEquals(fields, specification.fieldNames().asSequence().toSet())
            if (type == "sleeping_bag") {
                assertEquals(0.0, specification["comfortTemperatureCelsius"].asDouble())
                assertEquals(-12.5, specification["limitTemperatureCelsius"].asDouble())
            }
        }
        whenever(repository.findById("partial")).thenReturn(equipment("partial", "sleeping_bag", CatalogSpecRecord(unitLabel = "件", limitTemperatureCelsius = 0.0)))
        mvc.perform(get("$base/items/partial").principal(auth()))
            .andExpect(jsonPath("$.data.specifications.comfortTemperatureCelsius").doesNotExist())
            .andExpect(jsonPath("$.data.specifications.limitTemperatureCelsius").value(0.0))
    }

    @Test
    fun `query parses decimal scientific values and preserves opaque brand identity`() {
        mvc.perform(get("$base/items").principal(auth())
            .param("brandId", " Brand A ").param("category", "sleeping").param("equipmentType", "sleeping_bag")
            .param("minWeightGrams", "2.5e2").param("maxWeightGrams", "750.5")
            .param("minComfortTemperatureCelsius", "-10.5").param("maxComfortTemperatureCelsius", "0"))
            .andExpect(status().isOk).andExpect(jsonPath("$.data.items").isEmpty)
        val filter = argumentCaptor<CatalogFilter>()
        verify(repository).findItems(filter.capture())
        assertEquals(CatalogFilter(category = "sleeping", brandId = " Brand A ", equipmentType = "sleeping_bag", minWeightGrams = 250.0, maxWeightGrams = 750.5, minComfortTemperatureCelsius = -10.5, maxComfortTemperatureCelsius = 0.0), filter.firstValue)
    }

    @Test
    fun `all invalid query shapes return 400 before repository access`() {
        val cases = listOf(
            "keyword" to arrayOf("\u3000\t"), "keyword" to arrayOf("a", "b"),
            "limit" to arrayOf("10"), "category" to arrayOf("tent", "tent"), "category" to arrayOf(""),
            "category" to arrayOf("Tent"), "brandId" to arrayOf(" "), "equipmentType" to arrayOf("unknown"),
            "weightGrams" to arrayOf("NaN"), "weightGrams" to arrayOf("Infinity"), "weightGrams" to arrayOf("1e999"),
            "weightGrams" to arrayOf("12g"), "weightGrams" to arrayOf("0x1.0p2"), "weightGrams" to arrayOf(" 12 "),
            "comfortTemperatureCelsius" to arrayOf("0")
        )
        cases.forEach { (key, values) ->
            mvc.perform(get("$base/items").principal(auth()).param(key, *values))
                .andExpect(status().isBadRequest).andExpect(jsonPath("$.error.code").value("invalid_request"))
                .andExpect(jsonPath("$.error.retryable").value(false))
        }
        mvc.perform(get("$base/items").principal(auth()).param("weightGrams", "10").param("minWeightGrams", "5"))
            .andExpect(status().isBadRequest)
        mvc.perform(get("$base/items").principal(auth()).param("category", "tent").param("equipmentType", "sleeping_bag"))
            .andExpect(status().isBadRequest)
        listOf("$base/brands", "$base/items/item").forEach { path ->
            mvc.perform(get(path).principal(auth()).param("any", "value")).andExpect(status().isBadRequest)
        }
        verifyNoInteractions(repository)
    }

    @Test
    fun `finite invalid business ranges return 422`() {
        listOf("0", "-0", "-1.5").forEach { value ->
            mvc.perform(get("$base/items").principal(auth()).param("weightGrams", value))
                .andExpect(status().isUnprocessableEntity).andExpect(jsonPath("$.error.code").value("validation_failed"))
        }
        mvc.perform(get("$base/items").principal(auth()).param("minWeightGrams", "20").param("maxWeightGrams", "10"))
            .andExpect(status().isUnprocessableEntity)
        mvc.perform(get("$base/items").principal(auth()).param("equipmentType", "sleeping_bag").param("minComfortTemperatureCelsius", "5").param("maxComfortTemperatureCelsius", "-5"))
            .andExpect(status().isUnprocessableEntity)
        verifyNoInteractions(repository)
    }

    @Test
    fun `missing detail is 404 and backend failures are safe 503 with deployment prefix`() {
        mvc.perform(get("$base/items/missing").principal(auth()))
            .andExpect(status().isNotFound).andExpect(jsonPath("$.error.code").value("resource_not_found"))
        whenever(repository.findItems(any())).thenThrow(DataAccessResourceFailureException("SELECT secret FROM private_table password=secret"))
        val response = mvc.perform(get("/walkbg$base/items").contextPath("/walkbg").principal(auth()))
            .andExpect(status().isServiceUnavailable).andExpect(jsonPath("$.error.code").value("equipment_catalog_unavailable"))
            .andExpect(jsonPath("$.error.retryable").value(true))
            .andExpect(jsonPath("$.error.details").doesNotExist()).andExpect(jsonPath("$.data").doesNotExist())
            .andReturn().response.contentAsString
        assertFalse(response.contains("SELECT")); assertFalse(response.contains("secret")); assertFalse(response.contains("Exception"))
        whenever(repository.findById("bad")).thenReturn(equipment("bad", "backpack", CatalogSpecRecord(unitLabel = "件", capacityLiters = -1.0)))
        mvc.perform(get("$base/items/bad").principal(auth())).andExpect(status().isServiceUnavailable)
    }

    @Test
    fun `openapi schema explicitly describes typed detail specifications without maps`() {
        val schemas = ModelConverters.getInstance().readAll(CatalogEquipmentDetailProjection::class.java)
        val detail = schemas.getValue("CatalogEquipmentDetailProjection")
        assertTrue(detail.properties.keys.containsAll(listOf("identity", "name", "equipmentType", "specifications")))
        val specs = schemas.getValue("CatalogDetailSpecifications")
        assertEquals(5, specs.oneOf.size)
        assertEquals(setOf("unitLabel", "variant", "unitWeight", "weightScope", "capacityLiters"), schemas.getValue("BackpackSpecificationsProjection").properties.keys)
        assertNull(schemas.getValue("BackpackSpecificationsProjection").additionalProperties)
        assertTrue("rValue" in schemas.getValue("SleepingPadSpecificationsProjection").properties)
        assertFalse("rvalue" in schemas.getValue("SleepingPadSpecificationsProjection").properties)
    }

    private fun equipment(id: String, type: String, spec: CatalogSpecRecord? = null) = CatalogEquipmentRecord(
        id, "装备资料", CatalogType.fromCode(type)!!.allowedCategories.first(), type, specifications = spec
    )
    private fun auth() = UsernamePasswordAuthenticationToken(CustomUserDetails("account-1", "account-1", ""), null, emptyList())
    companion object { private const val base = "/api/v1/equipment-catalog" }
}
