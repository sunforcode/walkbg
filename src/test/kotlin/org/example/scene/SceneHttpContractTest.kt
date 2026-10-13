package org.example.scene

import org.example.account.repository.AccountSessionRepository
import org.example.common.contract.ApiContractException
import org.example.config.CorsProperties
import org.example.config.JacksonConfig
import org.example.security.JwtAuthenticationFilter
import org.example.security.JwtTokenUtil
import org.example.security.SecurityConfig
import org.junit.jupiter.api.Assertions.assertArrayEquals
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.mockito.kotlin.*
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest
import org.springframework.boot.test.mock.mockito.MockBean
import org.springframework.context.annotation.Import
import org.springframework.http.MediaType
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.*

@WebMvcTest(controllers = [SceneController::class], properties = ["app.scenes.public-base-url=https://art.example/walkbg", "app.scenes.management-token=scene-test-token"])
@Import(SceneExceptionHandler::class, SceneConfiguration::class, SceneDomainService::class, JacksonConfig::class, SecurityConfig::class, JwtAuthenticationFilter::class, CorsProperties::class)
class SceneHttpContractTest {
    @Autowired private lateinit var mvc: MockMvc
    @MockBean private lateinit var service: SceneApplicationService
    @MockBean private lateinit var jwt: JwtTokenUtil
    @MockBean private lateinit var sessions: AccountSessionRepository
    private val id = "a".repeat(64)
    private val image get() = SceneImage(id, "https://art.example/walkbg/api/v1/scene-media/$id", 8, 6)
    private val management = "/api/v1/scene-management/routes/route-1/versions/version-1"

    @BeforeEach fun session() {
        whenever(jwt.isTokenValidFormat("valid-account")).thenReturn(true)
        whenever(jwt.isTokenExpired("valid-account")).thenReturn(false)
        whenever(jwt.getUsernameFromToken("valid-account")).thenReturn("sample-user")
        whenever(jwt.getUserIdFromToken("valid-account")).thenReturn("owner-1")
        whenever(jwt.getTokenTypeFromToken("valid-account")).thenReturn("account_session")
        whenever(jwt.getSessionIdFromToken("valid-account")).thenReturn("session-1")
        whenever(sessions.existsByIdAndAccountIdAndRevokedAtIsNull("session-1", "owner-1")).thenReturn(true)
    }
    @Test fun `unconfigured management rejects HTTP access with service unavailable`() {
        val closed = org.springframework.test.web.servlet.setup.MockMvcBuilders.standaloneSetup(
            SceneController(service, SceneConfiguration("", ""), SceneDomainService(JacksonConfig().objectMapper()))
        ).setControllerAdvice(SceneExceptionHandler()).build()
        closed.perform(get(management)).andExpect(status().isServiceUnavailable).andExpect(jsonPath("$.error.code").value("scene_management_unavailable"))
        verifyNoInteractions(service)
    }
    @Test fun `dedicated management credential is required and ordinary account token cannot upload`() {
        mvc.perform(post("/api/v1/scene-management/media").contentType("image/png").content(sceneImageBytes()))
            .andExpect(status().isUnauthorized).andExpect(jsonPath("$.error.code").value("scene_management_authentication_required"))
        mvc.perform(post("/api/v1/scene-management/media").header("Authorization", "Bearer valid-account").contentType("image/png").content(sceneImageBytes()))
            .andExpect(status().isUnauthorized)
        mvc.perform(get(management).header("X-Scene-Management-Token", "wrong"))
            .andExpect(status().isUnauthorized)
        verifyNoInteractions(service)
    }
    @Test fun `upload returns data envelope and only reads bounded allowed media`() {
        whenever(service.upload(any(), eq("image/png"))).thenReturn(image)
        mvc.perform(post("/api/v1/scene-management/media").header("X-Scene-Management-Token", "scene-test-token").contentType("image/png").content(sceneImageBytes()))
            .andExpect(status().isCreated).andExpect(jsonPath("$.data.mediaId").value(id)).andExpect(jsonPath("$.data.imageUrl").value(image.imageUrl))
        mvc.perform(post("/api/v1/scene-management/media").header("X-Scene-Management-Token", "scene-test-token").contentType("image/png").content(ByteArray(SceneDomainService.MAX_BYTES + 1)))
            .andExpect(status().isUnprocessableEntity).andExpect(jsonPath("$.error.code").value("scene_image_too_large"))
        mvc.perform(post("/api/v1/scene-management/media").header("X-Scene-Management-Token", "scene-test-token").contentType("image/svg+xml").content("<svg/>"))
            .andExpect(status().isUnsupportedMediaType)
        verify(service, times(1)).upload(any(), any())
    }
    @Test fun `public media returns original bytes content cache and conditional headers`() {
        val bytes = sceneImageBytes()
        whenever(service.readMedia(id)).thenReturn(SceneMedia(id, "image/png", 8, 6, bytes))
        val response = mvc.perform(get("/api/v1/scene-media/$id"))
            .andExpect(status().isOk).andExpect(content().contentType("image/png"))
            .andExpect(header().string("ETag", "\"$id\""))
            .andExpect(header().string("X-Content-Type-Options", "nosniff"))
            .andExpect(header().string("Cache-Control", "public, max-age=31536000, immutable"))
            .andExpect(header().string("Content-Length", bytes.size.toString())).andReturn().response
        assertArrayEquals(bytes, response.contentAsByteArray)
        mvc.perform(get("/api/v1/scene-media/$id").header("If-None-Match", "W/\"$id\""))
            .andExpect(status().isNotModified).andExpect(content().bytes(ByteArray(0)))
            .andExpect(header().string("ETag", "\"$id\""))
        mvc.perform(get("/api/v1/scene-media/$id").param("download", "1")).andExpect(status().isBadRequest)
        mvc.perform(get("/api/v1/scene-media/$id").header("Authorization", "Bearer invalid")).andExpect(status().isUnauthorized)
    }
    @Test fun `route read requires explicit single version and rejects invalid bearer`() {
        whenever(service.publicRoute("r", "v")).thenReturn(RouteSceneProjection("r", "v", "0"))
        mvc.perform(get("/api/v1/public-routes/r/scenes").param("routeVersionId", "v"))
            .andExpect(status().isOk).andExpect(jsonPath("$.data.days").isEmpty).andExpect(jsonPath("$.data.overview").doesNotExist())
        mvc.perform(get("/api/v1/public-routes/r/scenes")).andExpect(status().isBadRequest)
        mvc.perform(get("/api/v1/public-routes/r/scenes").param("routeVersionId", "v", "w")).andExpect(status().isBadRequest)
        mvc.perform(get("/api/v1/public-routes/r/scenes").param("routeVersionId", "v").param("x", "1")).andExpect(status().isBadRequest)
        mvc.perform(get("/api/v1/public-routes/r/scenes").param("routeVersionId", "v").header("Authorization", "Bearer invalid"))
            .andExpect(status().isUnauthorized)
    }
    @Test fun `strict replacement rejects null unknown nested fields coercion missing and duplicate query`() {
        listOf(
            """{"expectedRevision":0,"days":[],"foo":true}""",
            """{"expectedRevision":0,"days":[],"overviewMediaId":null}""",
            """{"expectedRevision":"0","days":[]}""",
            """{"expectedRevision":0.1,"days":[]}""",
            """{"expectedRevision":-1,"days":[]}""",
            """{"expectedRevision":0}""",
            """{"expectedRevision":0,"days":[{"referenceDayId":"d","mediaId":"$id","unknown":1}]}"""
        ).forEach { json ->
            mvc.perform(put(management).header("X-Scene-Management-Token", "scene-test-token").contentType(MediaType.APPLICATION_JSON).content(json))
                .andExpect(status().isBadRequest).andExpect(jsonPath("$.error.code").value("invalid_request"))
        }
        mvc.perform(get(management).header("X-Scene-Management-Token", "scene-test-token").param("any", "1"))
            .andExpect(status().isBadRequest)
        verifyNoInteractions(service)
    }
    @Test fun `personal scene uses real security chain ownership and cannot use management credentials`() {
        whenever(service.personalTrip("owner-1", "trip-1")).thenReturn(TripSceneProjection("trip-1", "revision-1", "v", "0"))
        whenever(service.personalTrip("owner-1", "other-trip")).thenThrow(ApiContractException(org.springframework.http.HttpStatus.NOT_FOUND, "trip_not_found", "行程不存在"))
        mvc.perform(get("/api/v1/trips/trip-1/scenes")).andExpect(status().isUnauthorized)
        mvc.perform(get("/api/v1/trips/trip-1/scenes").header("X-Scene-Management-Token", "scene-test-token")).andExpect(status().isUnauthorized)
        mvc.perform(get("/api/v1/trips/trip-1/scenes").header("Authorization", "Bearer valid-account"))
            .andExpect(status().isOk).andExpect(jsonPath("$.data.tripRevision").value("revision-1"))
        mvc.perform(get("/api/v1/trips/other-trip/scenes").header("Authorization", "Bearer valid-account"))
            .andExpect(status().isNotFound).andExpect(jsonPath("$.error.code").value("trip_not_found"))
    }
}
