package org.example.route.controller

import org.example.common.contract.ApiContractException
import org.example.common.contract.TargetApiExceptionHandler
import org.example.config.JacksonConfig
import org.example.route.dto.PublicRouteBrowseSummary
import org.example.route.dto.PublicRouteCollectionResult
import org.example.route.dto.PublicRouteDetail
import org.example.route.dto.PublicRouteDiscoveryResult
import org.example.route.dto.PublicRouteSearchResult
import org.example.route.dto.PublicRouteSearchSummary
import org.example.route.dto.PublicRouteSummary
import org.example.route.dto.PublicRouteVersionDetail
import org.example.route.dto.RouteGenerationEligibility
import org.example.route.dto.RouteMeters
import org.example.route.dto.RouteSeconds
import org.example.route.service.PublicRouteApplicationService
import org.example.security.JwtAuthenticationFilter
import org.junit.jupiter.api.Test
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.ValueSource
import org.mockito.kotlin.whenever
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest
import org.springframework.boot.test.mock.mockito.MockBean
import org.springframework.context.annotation.Import
import org.springframework.dao.DataAccessResourceFailureException
import org.springframework.http.HttpStatus
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.status

@WebMvcTest(controllers = [PublicRouteController::class])
@AutoConfigureMockMvc(addFilters = false)
@Import(JacksonConfig::class, TargetApiExceptionHandler::class)
class PublicRouteApiContractTest {

    @Autowired
    private lateinit var mockMvc: MockMvc

    @MockBean
    private lateinit var publicRouteApplicationService: PublicRouteApplicationService

    @MockBean
    private lateinit var jwtAuthenticationFilter: JwtAuthenticationFilter

    @Test
    fun `featured routes use data envelope and browse field boundary`() {
        whenever(publicRouteApplicationService.featured()).thenReturn(
            PublicRouteDiscoveryResult(
                items = listOf(
                    PublicRouteBrowseSummary(
                        routeId = "route-1",
                        currentVersionId = "version-1",
                        cover = "media-cover-1",
                        name = "贡嘎环线",
                        region = "四川甘孜",
                        difficulty = "中等",
                        distance = RouteMeters(42_500.0),
                        ascent = RouteMeters(2_300.0),
                        estimatedDuration = RouteSeconds(259_200.0)
                    )
                )
            )
        )

        mockMvc.perform(get("/api/v1/public-routes/featured"))
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.data.items[0].routeId").value("route-1"))
            .andExpect(jsonPath("$.data.items[0].currentVersionId").value("version-1"))
            .andExpect(jsonPath("$.data.items[0].distance.meters").value(42_500.0))
            .andExpect(jsonPath("$.data.items[0].ascent.meters").value(2_300.0))
            .andExpect(jsonPath("$.data.items[0].estimatedDuration.seconds").value(259_200.0))
            .andExpect(jsonPath("$.data.items[0].popularity").doesNotExist())
            .andExpect(jsonPath("$.data.items[0].creator").doesNotExist())
            .andExpect(jsonPath("$.success").doesNotExist())
            .andExpect(jsonPath("$.error").doesNotExist())
    }

    @Test
    fun `all routes use data envelope and omit unavailable optional facts`() {
        whenever(publicRouteApplicationService.all()).thenReturn(
            PublicRouteCollectionResult(
                items = listOf(PublicRouteBrowseSummary(routeId = "route-2", currentVersionId = "version-2"))
            )
        )

        mockMvc.perform(get("/api/v1/public-routes"))
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.data.items[0].routeId").value("route-2"))
            .andExpect(jsonPath("$.data.items[0].currentVersionId").value("version-2"))
            .andExpect(jsonPath("$.data.items[0].name").doesNotExist())
            .andExpect(jsonPath("$.data.items[0].distance").doesNotExist())
            .andExpect(jsonPath("$.data.items[0].ascent").doesNotExist())
            .andExpect(jsonPath("$.error").doesNotExist())
    }

    @Test
    fun `missing search query returns initial state in data envelope`() {
        whenever(publicRouteApplicationService.search(null)).thenReturn(
            PublicRouteSearchResult(state = "initial", items = emptyList())
        )

        mockMvc.perform(get("/api/v1/public-routes/search"))
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.data.state").value("initial"))
            .andExpect(jsonPath("$.data.items").isEmpty)
            .andExpect(jsonPath("$.error").doesNotExist())
    }

    @Test
    fun `search summaries never expose ascent`() {
        whenever(publicRouteApplicationService.search("贡嘎")).thenReturn(
            PublicRouteSearchResult(
                state = "completed",
                items = listOf(
                    PublicRouteSearchSummary(
                        routeId = "route-1",
                        currentVersionId = "version-1",
                        name = "贡嘎环线",
                        distance = RouteMeters(42_500.0),
                        estimatedDuration = RouteSeconds(259_200.0)
                    )
                )
            )
        )

        mockMvc.perform(get("/api/v1/public-routes/search").param("query", "贡嘎"))
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.data.state").value("completed"))
            .andExpect(jsonPath("$.data.items[0].routeId").value("route-1"))
            .andExpect(jsonPath("$.data.items[0].ascent").doesNotExist())
    }

    @Test
    fun `route detail uses bounded current version projection`() {
        whenever(publicRouteApplicationService.detail("route-1")).thenReturn(
            PublicRouteDetail(
                routeId = "route-1",
                currentVersion = PublicRouteVersionDetail(
                    versionId = "version-1",
                    summary = PublicRouteSummary(
                        routeType = "multi_day",
                        name = "贡嘎环线",
                        region = "四川甘孜"
                    ),
                    mainTrackAvailability = "missing",
                    generationEligibility = RouteGenerationEligibility(
                        eligible = false,
                        missingReasons = listOf("estimatedDuration", "start", "end", "validMainTrack")
                    )
                )
            )
        )

        mockMvc.perform(get("/api/v1/public-routes/route-1"))
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.data.routeId").value("route-1"))
            .andExpect(jsonPath("$.data.currentVersion.versionId").value("version-1"))
            .andExpect(jsonPath("$.data.currentVersion.summary.routeType").value("multi_day"))
            .andExpect(jsonPath("$.data.currentVersion.mainTrackAvailability").value("missing"))
            .andExpect(jsonPath("$.data.currentVersion.generationEligibility.missingReasons[0]").value("estimatedDuration"))
            .andExpect(jsonPath("$.data.currentVersion.favorite").doesNotExist())
            .andExpect(jsonPath("$.data.currentVersion.ratings").doesNotExist())
            .andExpect(jsonPath("$.data.currentVersion.creator").doesNotExist())
            .andExpect(jsonPath("$.data.currentVersion.relatedRoutes").doesNotExist())
            .andExpect(jsonPath("$.data.currentVersion.equipmentSuggestions").doesNotExist())
    }

    @Test
    fun `missing or non-public route returns indistinguishable route not found error`() {
        whenever(publicRouteApplicationService.detail("private-route")).thenThrow(
            ApiContractException(HttpStatus.NOT_FOUND, "route_not_found", "路线不存在")
        )

        mockMvc.perform(get("/api/v1/public-routes/private-route"))
            .andExpect(status().isNotFound)
            .andExpect(jsonPath("$.error.code").value("route_not_found"))
            .andExpect(jsonPath("$.error.retryable").value(false))
            .andExpect(jsonPath("$.data").doesNotExist())
    }

    @ParameterizedTest
    @ValueSource(strings = ["page", "sort", "filter", "expand"])
    fun `all routes reject unknown query parameters`(parameter: String) {
        mockMvc.perform(get("/api/v1/public-routes").param(parameter, "unexpected"))
            .andExpect(status().isBadRequest)
            .andExpect(jsonPath("$.error.code").value("invalid_request"))
            .andExpect(jsonPath("$.data").doesNotExist())
    }

    @Test
    fun `required public route read failure uses explicit target error`() {
        whenever(publicRouteApplicationService.featured()).thenThrow(
            DataAccessResourceFailureException("database unavailable")
        )

        mockMvc.perform(get("/api/v1/public-routes/featured"))
            .andExpect(status().isServiceUnavailable)
            .andExpect(jsonPath("$.error.code").value("public_route_read_failed"))
            .andExpect(jsonPath("$.error.retryable").value(true))
            .andExpect(jsonPath("$.data").doesNotExist())
    }
}
