package org.example.account.controller

import org.example.account.dto.AccountProjection
import org.example.account.dto.AccountResponse
import org.example.account.dto.AuthSessionResponse
import org.example.account.dto.LogoutResponse
import org.example.account.dto.ProfileProjection
import org.example.account.dto.VerificationCodeResponse
import org.example.account.service.AccountApplicationService
import org.example.common.contract.TargetApiExceptionHandler
import org.example.config.JacksonConfig
import org.example.security.CustomUserDetails
import org.example.security.JwtAuthenticationFilter
import org.junit.jupiter.api.Test
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
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.status
import java.time.Instant

@WebMvcTest(controllers = [AccountAuthController::class, AccountController::class])
@AutoConfigureMockMvc(addFilters = false)
@Import(JacksonConfig::class, TargetApiExceptionHandler::class)
class AccountApiContractTest {

    @Autowired
    private lateinit var mockMvc: MockMvc

    @MockBean
    private lateinit var accountApplicationService: AccountApplicationService

    @MockBean
    private lateinit var jwtAuthenticationFilter: JwtAuthenticationFilter

    @Test
    fun `verification code success uses the data-only envelope`() {
        val expiresAt = Instant.parse("2026-09-02T10:05:00Z")
        val resendAt = Instant.parse("2026-09-02T10:01:00Z")
        whenever(accountApplicationService.sendVerificationCode("+8613800138000"))
            .thenReturn(VerificationCodeResponse("verification-1", expiresAt, resendAt))

        mockMvc.perform(
            post("/api/v1/auth/verification-codes")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""{"phone":"+8613800138000"}""")
        )
            .andExpect(status().isCreated)
            .andExpect(jsonPath("$.data.verificationId").value("verification-1"))
            .andExpect(jsonPath("$.data.expiresAt").value("2026-09-02T10:05:00Z"))
            .andExpect(jsonPath("$.success").doesNotExist())
            .andExpect(jsonPath("$.message").doesNotExist())
            .andExpect(jsonPath("$.timestamp").doesNotExist())
            .andExpect(jsonPath("$.code").doesNotExist())
    }

    @Test
    fun `verification code rejects unknown request fields`() {
        mockMvc.perform(
            post("/api/v1/auth/verification-codes")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""{"phone":"+8613800138000","accountId":"other"}""")
        )
            .andExpect(status().isBadRequest)
            .andExpect(jsonPath("$.error.code").value("invalid_request"))
            .andExpect(jsonPath("$.error.retryable").value(false))
            .andExpect(jsonPath("$.data").doesNotExist())
    }

    @Test
    fun `session creation returns only the bearer credential projection`() {
        whenever(accountApplicationService.createSession("verification-1", "001204"))
            .thenReturn(AuthSessionResponse("access-token", "Bearer"))

        mockMvc.perform(
            post("/api/v1/auth/session")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""{"verificationId":"verification-1","code":"001204"}""")
        )
            .andExpect(status().isCreated)
            .andExpect(jsonPath("$.data.accessToken").value("access-token"))
            .andExpect(jsonPath("$.data.tokenType").value("Bearer"))
            .andExpect(jsonPath("$.data.account").doesNotExist())
    }

    @Test
    fun `current account omits unfilled optional profile fields`() {
        val authentication = accountAuthentication()
        whenever(accountApplicationService.getAccount("account-1"))
            .thenReturn(
                AccountResponse(
                    account = AccountProjection("account-1"),
                    profile = ProfileProjection(
                        identity = "profile-1",
                        maskedPhone = "+86*******8000"
                    )
                )
            )

        mockMvc.perform(get("/api/v1/account").principal(authentication))
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.data.account.identity").value("account-1"))
            .andExpect(jsonPath("$.data.profile.identity").value("profile-1"))
            .andExpect(jsonPath("$.data.profile.maskedPhone").value("+86*******8000"))
            .andExpect(jsonPath("$.data.profile.nickname").doesNotExist())
            .andExpect(jsonPath("$.data.profile.avatar").doesNotExist())
    }

    @Test
    fun `empty avatar media is rejected explicitly`() {
        mockMvc.perform(
            post("/api/v1/account/profile/avatar-media")
                .principal(accountAuthentication())
                .contentType(MediaType.IMAGE_PNG)
                .content(ByteArray(0))
        )
            .andExpect(status().isUnprocessableEntity)
            .andExpect(jsonPath("$.error.code").value("avatar_media_invalid"))
    }

    @Test
    fun `profile avatar accepts explicit null for clearing`() {
        whenever(accountApplicationService.updateProfile(eq("account-1"), org.mockito.kotlin.any()))
            .thenReturn(
                org.example.account.dto.ProfileResponse(
                    ProfileProjection("profile-1", "+86*******8000", nickname = "Walker")
                )
            )

        mockMvc.perform(
            patch("/api/v1/account/profile")
                .principal(accountAuthentication())
                .contentType(MediaType.APPLICATION_JSON)
                .content("""{"avatar":null}""")
        )
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.data.profile.avatar").doesNotExist())
    }

    @Test
    fun `logout revokes only the authenticated session`() {
        val authentication = accountAuthentication()
        whenever(accountApplicationService.logout(eq("account-1"), eq("session-1")))
            .thenReturn(LogoutResponse(loggedOut = true))

        mockMvc.perform(delete("/api/v1/auth/session").principal(authentication))
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.data.loggedOut").value(true))
    }

    @Test
    fun `target account endpoints always use the error envelope for mvc and server failures`() {
        mockMvc.perform(
            post("/api/v1/auth/session")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""{"verificationId":"broken"""")
        )
            .andExpect(status().isBadRequest)
            .andExpect(jsonPath("$.error.code").value("invalid_request"))
            .andExpect(jsonPath("$.data").doesNotExist())

        mockMvc.perform(
            patch("/api/v1/account/profile")
                .principal(accountAuthentication())
                .contentType(MediaType.TEXT_PLAIN)
                .content("nickname=Walker")
        )
            .andExpect(status().isUnsupportedMediaType)
            .andExpect(jsonPath("$.error.code").value("media_type_unsupported"))
            .andExpect(jsonPath("$.data").doesNotExist())

        mockMvc.perform(
            post("/api/v1/account/profile/avatar-media")
                .principal(accountAuthentication())
                .contentType(MediaType.APPLICATION_JSON)
                .content("{}")
        )
            .andExpect(status().isUnsupportedMediaType)
            .andExpect(jsonPath("$.error.code").value("avatar_media_type_unsupported"))

        mockMvc.perform(
            post("/api/v1/account")
                .principal(accountAuthentication())
                .contentType(MediaType.APPLICATION_JSON)
                .content("{}")
        )
            .andExpect(status().isMethodNotAllowed)
            .andExpect(jsonPath("$.error.code").value("method_not_allowed"))

        mockMvc.perform(get("/api/v1/account/not-a-target-endpoint").principal(accountAuthentication()))
            .andExpect(status().isNotFound)
            .andExpect(jsonPath("$.error.code").value("resource_not_found"))

        mockMvc.perform(get("/api/v1/auth/not-a-target-endpoint"))
            .andExpect(status().isNotFound)
            .andExpect(jsonPath("$.error.code").value("resource_not_found"))
            .andExpect(jsonPath("$.data").doesNotExist())

        whenever(accountApplicationService.getAccount("account-1")).thenThrow(IllegalStateException("secret failure"))
        mockMvc.perform(get("/api/v1/account").principal(accountAuthentication()))
            .andExpect(status().isInternalServerError)
            .andExpect(jsonPath("$.error.code").value("internal_error"))
            .andExpect(jsonPath("$.error.message").value("服务内部错误"))
            .andExpect(jsonPath("$.data").doesNotExist())
    }

    @Test
    fun `legacy unknown endpoint retains the legacy error envelope`() {
        mockMvc.perform(get("/api/v1/legacy/not-a-target-endpoint"))
            .andExpect(status().isNotFound)
            .andExpect(jsonPath("$.success").value(false))
            .andExpect(jsonPath("$.error").doesNotExist())
    }

    private fun accountAuthentication(): UsernamePasswordAuthenticationToken {
        val principal = CustomUserDetails(
            userId = "account-1",
            username = "account-1",
            password = "",
            sessionId = "session-1"
        )
        return UsernamePasswordAuthenticationToken(principal, null, emptyList())
    }
}
