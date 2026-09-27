package org.example.route.controller

import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import com.fasterxml.jackson.module.kotlin.readValue
import org.example.route.dto.RouteStatusUpdateRequest
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

class RoutePublicationRequestTest {
    @Test
    fun `status request retains explicit public type and publication identity`() {
        val mapper = jacksonObjectMapper().disable(com.fasterxml.jackson.databind.DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES)
        val request = mapper.readValue<RouteStatusUpdateRequest>(
            """{"target_status":1,"public_route_type":"multi_day","publication_id":"request-1"}"""
        )
        val json = mapper.valueToTree<com.fasterxml.jackson.databind.JsonNode>(request)
        assertEquals("multi_day", json.path("public_route_type").asText())
        assertEquals("request-1", json.path("publication_id").asText())
    }
}
