package org.example.user.controller

import com.fasterxml.jackson.databind.ObjectMapper
import org.example.common.exception.BusinessException
import org.example.security.JwtAuthenticationFilter
import org.example.user.dto.*
import org.example.user.service.AuthApplicationService
import org.junit.jupiter.api.Test
import org.mockito.Mockito.*
import org.mockito.kotlin.any
import org.mockito.kotlin.anyOrNull
import org.mockito.kotlin.whenever
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest
import org.springframework.boot.test.mock.mockito.MockBean
import org.springframework.http.MediaType
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.*
import java.time.Instant

@WebMvcTest(AuthController::class)
@AutoConfigureMockMvc(addFilters = false)
class AuthControllerTest {

    @Autowired
    private lateinit var mockMvc: MockMvc

    @Autowired
    private lateinit var objectMapper: ObjectMapper

    @MockBean
    private lateinit var authApplicationService: AuthApplicationService

    @MockBean
    private lateinit var jwtAuthenticationFilter: JwtAuthenticationFilter

    // ========== POST /api/v1/auth/login 测试 ==========

    @Test
    fun `POST - api v1 auth login - 有效凭证返回200和token`() {
        val request = UserLoginRequest(
            username = "testuser",
            password = "password123"
        )
        val response = buildUserLoginResponse(username = "testuser", token = "test_token")

        whenever(authApplicationService.login(any())).thenReturn(response)

        mockMvc.perform(
            post("/api/v1/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request))
        )
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.code").value(200))
            .andExpect(jsonPath("$.success").value(true))
            .andExpect(jsonPath("$.data.username").value("testuser"))
            .andExpect(jsonPath("$.data.token").value("test_token"))
    }

    @Test
    fun `POST - api v1 auth login - 用户不存在返回401`() {
        val request = UserLoginRequest(
            username = "nonexistent",
            password = "password123"
        )

        whenever(authApplicationService.login(any())).thenThrow(BusinessException.unauthorized("用户名或密码错误"))

        mockMvc.perform(
            post("/api/v1/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request))
        )
            .andExpect(status().isUnauthorized)
    }

    @Test
    fun `POST - api v1 auth login - 密码错误返回401`() {
        val request = UserLoginRequest(
            username = "testuser",
            password = "wrongpassword"
        )

        whenever(authApplicationService.login(any())).thenThrow(BusinessException.unauthorized("用户名或密码错误"))

        mockMvc.perform(
            post("/api/v1/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request))
        )
            .andExpect(status().isUnauthorized)
    }

    @Test
    fun `POST - api v1 auth login - 参数无效返回400`() {
        val request = mapOf(
            "username" to "ab",
            "password" to "123"
        )

        mockMvc.perform(
            post("/api/v1/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request))
        )
            .andExpect(status().isBadRequest)
    }

    // ========== POST /api/v1/auth/register 测试 ==========

    @Test
    fun `POST - api v1 auth register - 有效请求返回201`() {
        val request = UserRegisterRequest(
            username = "newuser",
            email = "new@example.com",
            password = "password123",
            nickname = "新用户"
        )
        val response = buildUserLoginResponse(
            username = "newuser",
            email = "new@example.com",
            nickname = "新用户",
            token = "new_user_token"
        )

        whenever(authApplicationService.register(any())).thenReturn(response)

        mockMvc.perform(
            post("/api/v1/auth/register")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request))
        )
            .andExpect(status().isCreated)
            .andExpect(jsonPath("$.code").value(200))
            .andExpect(jsonPath("$.success").value(true))
            .andExpect(jsonPath("$.data.username").value("newuser"))
            .andExpect(jsonPath("$.data.email").value("new@example.com"))
            .andExpect(jsonPath("$.data.token").value("new_user_token"))
    }

    @Test
    fun `POST - api v1 auth register - 用户名重复返回409`() {
        val request = UserRegisterRequest(
            username = "existinguser",
            email = "new@example.com",
            password = "password123"
        )

        whenever(authApplicationService.register(any())).thenThrow(BusinessException.conflict("用户名已存在"))

        mockMvc.perform(
            post("/api/v1/auth/register")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request))
        )
            .andExpect(status().isConflict)
    }

    @Test
    fun `POST - api v1 auth register - 邮箱重复返回409`() {
        val request = UserRegisterRequest(
            username = "newuser",
            email = "existing@example.com",
            password = "password123"
        )

        whenever(authApplicationService.register(any())).thenThrow(BusinessException.conflict("邮箱已被注册"))

        mockMvc.perform(
            post("/api/v1/auth/register")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request))
        )
            .andExpect(status().isConflict)
    }

    @Test
    fun `POST - api v1 auth register - 参数无效返回400`() {
        val request = mapOf(
            "username" to "ab",
            "email" to "invalid-email",
            "password" to "123"
        )

        mockMvc.perform(
            post("/api/v1/auth/register")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request))
        )
            .andExpect(status().isBadRequest)
    }

    // ========== POST /api/v1/auth/refresh 测试 ==========

    @Test
    fun `POST - api v1 auth refresh - 有效refresh token返回新token`() {
        val request = TokenRefreshRequest(refreshToken = "valid_refresh_token")
        val response = TokenRefreshResponse(
            token = "new_access_token",
            refreshToken = "new_refresh_token",
            expiresAt = Instant.now()
        )

        whenever(authApplicationService.refreshToken(any())).thenReturn(response)

        mockMvc.perform(
            post("/api/v1/auth/refresh")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request))
        )
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.code").value(200))
            .andExpect(jsonPath("$.data.token").value("new_access_token"))
    }

    @Test
    fun `POST - api v1 auth refresh - 空refresh token返回400`() {
        val request = TokenRefreshRequest(refreshToken = "")

        whenever(authApplicationService.refreshToken(any())).thenThrow(BusinessException.badRequest("刷新Token不能为空"))

        mockMvc.perform(
            post("/api/v1/auth/refresh")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request))
        )
            .andExpect(status().isBadRequest)
    }

    @Test
    fun `POST - api v1 auth refresh - 过期refresh token返回401`() {
        val request = TokenRefreshRequest(refreshToken = "expired_token")

        whenever(authApplicationService.refreshToken(any())).thenThrow(BusinessException.unauthorized("刷新Token已过期"))

        mockMvc.perform(
            post("/api/v1/auth/refresh")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request))
        )
            .andExpect(status().isUnauthorized)
    }

    @Test
    fun `POST - api v1 auth refresh - 无效refresh token返回401`() {
        val request = TokenRefreshRequest(refreshToken = "invalid_token")

        whenever(authApplicationService.refreshToken(any())).thenThrow(BusinessException.unauthorized("无效的刷新Token"))

        mockMvc.perform(
            post("/api/v1/auth/refresh")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request))
        )
            .andExpect(status().isUnauthorized)
    }

    // ========== POST /api/v1/auth/logout 测试 ==========

    @Test
    fun `POST - api v1 auth logout - 成功返回200`() {
        mockMvc.perform(
            post("/api/v1/auth/logout")
                .contentType(MediaType.APPLICATION_JSON)
        )
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.code").value(200))
            .andExpect(jsonPath("$.message").value("登出成功"))
    }

    // ========== GET /api/v1/auth/check-username 测试 ==========

    @Test
    fun `GET - api v1 auth check-username - 可用返回true`() {
        val username = "newuser"

        whenever(authApplicationService.checkUsernameAvailability(username)).thenReturn(true)

        mockMvc.perform(
            get("/api/v1/auth/check-username/{username}", username)
        )
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.code").value(200))
            .andExpect(jsonPath("$.data.username").value(username))
            .andExpect(jsonPath("$.data.available").value(true))
    }

    @Test
    fun `GET - api v1 auth check-username - 已存在返回false`() {
        val username = "existinguser"

        whenever(authApplicationService.checkUsernameAvailability(username)).thenReturn(false)

        mockMvc.perform(
            get("/api/v1/auth/check-username/{username}", username)
        )
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.data.available").value(false))
    }

    // ========== GET /api/v1/auth/check-email 测试 ==========

    @Test
    fun `GET - api v1 auth check-email - 可用返回true`() {
        val email = "new@example.com"

        whenever(authApplicationService.checkEmailAvailability(email)).thenReturn(true)

        mockMvc.perform(
            get("/api/v1/auth/check-email/{email}", email)
        )
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.data.available").value(true))
    }

    @Test
    fun `GET - api v1 auth check-email - 已存在返回false`() {
        val email = "existing@example.com"

        whenever(authApplicationService.checkEmailAvailability(email)).thenReturn(false)

        mockMvc.perform(
            get("/api/v1/auth/check-email/{email}", email)
        )
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.data.available").value(false))
    }

    private fun buildUserLoginResponse(
        id: String = "test_user_001",
        username: String = "testuser",
        email: String = "test@example.com",
        nickname: String = "测试用户",
        token: String = "test_access_token",
        refreshToken: String = "test_refresh_token"
    ): UserLoginResponse {
        return UserLoginResponse(
            id = id,
            username = username,
            email = email,
            nickname = nickname,
            phone = "13800138000",
            avatarUrl = "https://example.com/avatar.jpg",
            token = token,
            refreshToken = refreshToken,
            expiresAt = Instant.now()
        )
    }

    private fun buildUserBasicResponse(
        id: String = "test_user_001",
        username: String = "testuser",
        email: String = "test@example.com",
        nickname: String = "测试用户"
    ): UserBasicResponse {
        return UserBasicResponse(
            id = id,
            username = username,
            email = email,
            nickname = nickname,
            phone = "13800138000",
            avatarUrl = "https://example.com/avatar.jpg",
            bio = null,
            status = 0,
            lastLoginAt = null,
            createdAt = Instant.now().epochSecond
        )
    }
}
