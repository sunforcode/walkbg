package org.example.user.controller

import com.fasterxml.jackson.databind.ObjectMapper
import org.example.common.exception.BusinessException
import org.example.common.util.IdGenerator
import org.example.security.JwtAuthenticationFilter
import org.example.security.JwtTokenUtil
import org.example.user.dto.UserBasicResponse
import org.example.user.dto.UserCreateRequest
import org.example.user.service.UserApplicationService
import org.junit.jupiter.api.Test
import org.mockito.ArgumentMatchers.anyString
import org.mockito.Mockito.*
import org.mockito.kotlin.any
import org.mockito.kotlin.anyOrNull
import org.mockito.kotlin.whenever
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest
import org.springframework.boot.test.mock.mockito.MockBean
import org.springframework.data.domain.PageImpl
import org.springframework.data.domain.PageRequest
import org.springframework.http.MediaType
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.*
import java.time.Instant

@WebMvcTest(UserController::class)
@AutoConfigureMockMvc(addFilters = false)
class UserControllerTest {

    @Autowired
    private lateinit var mockMvc: MockMvc

    @Autowired
    private lateinit var objectMapper: ObjectMapper

    @MockBean
    private lateinit var userApplicationService: UserApplicationService

    @MockBean
    private lateinit var jwtTokenUtil: JwtTokenUtil

    @MockBean
    private lateinit var jwtAuthenticationFilter: JwtAuthenticationFilter

    // ========== GET /api/v1/users 测试 ==========

    @Test
    fun `GET - api v1 users - 有效参数返回200`() {
        val user = buildUserBasicResponse()
        val page = PageImpl(listOf(user), PageRequest.of(0, 10), 1)
        
        whenever(userApplicationService.searchUsers(anyOrNull(), anyOrNull(), any())).thenReturn(page)
        
        mockMvc.perform(
            get("/api/v1/users")
                .param("page", "0")
                .param("size", "10")
        )
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.code").value(200))
            .andExpect(jsonPath("$.success").value(true))
            .andExpect(jsonPath("$.data.content[0].username").value(user.username))
    }

    // ========== GET /api/v1/users/{id} 测试 ==========

    @Test
    fun `GET - api v1 users id - 用户存在时返回200`() {
        val userId = "test_user_001"
        val user = buildUserBasicResponse(id = userId)
        
        whenever(userApplicationService.getUserById(userId)).thenReturn(user)
        
        mockMvc.perform(get("/api/v1/users/$userId"))
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.code").value(200))
            .andExpect(jsonPath("$.data.id").value(userId))
    }

    @Test
    fun `GET - api v1 users id - 用户不存在时返回404`() {
        val userId = "non_existent"
        
        whenever(userApplicationService.getUserById(userId)).thenReturn(null)
        
        mockMvc.perform(get("/api/v1/users/$userId"))
            .andExpect(status().isNotFound)
    }

    // ========== GET /api/v1/users/username/{username} 测试 ==========

    @Test
    fun `GET - api v1 users username - 用户存在时返回200`() {
        val username = "testuser"
        val user = buildUserBasicResponse(username = username)
        
        whenever(userApplicationService.getUserByUsername(username)).thenReturn(user)
        
        mockMvc.perform(get("/api/v1/users/username/$username"))
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.code").value(200))
            .andExpect(jsonPath("$.data.username").value(username))
    }

    @Test
    fun `GET - api v1 users username - 用户不存在时返回404`() {
        val username = "non_existent"
        
        whenever(userApplicationService.getUserByUsername(username)).thenReturn(null)
        
        mockMvc.perform(get("/api/v1/users/username/$username"))
            .andExpect(status().isNotFound)
    }

    // ========== POST /api/v1/users 测试 ==========

    @Test
    fun `POST - api v1 users - 有效请求返回201`() {
        val request = UserCreateRequest(
            username = "newuser",
            email = "new@example.com",
            password = "password123",
            nickname = "新用户"
        )
        val response = buildUserBasicResponse(username = "newuser")
        
        whenever(userApplicationService.createUser(any())).thenReturn(response)
        
        mockMvc.perform(
            post("/api/v1/users")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request))
        )
            .andExpect(status().isCreated)
            .andExpect(jsonPath("$.code").value(200))
            .andExpect(jsonPath("$.data.username").value("newuser"))
    }

    @Test
    fun `POST - api v1 users - 参数无效时返回400`() {
        val request = mapOf(
            "username" to "ab",
            "email" to "invalid-email"
        )
        
        mockMvc.perform(
            post("/api/v1/users")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request))
        )
            .andExpect(status().isBadRequest)
    }

    @Test
    fun `POST - api v1 users - 用户名重复时返回409`() {
        val request = UserCreateRequest(
            username = "existinguser",
            email = "test@example.com",
            password = "password123"
        )
        
        whenever(userApplicationService.createUser(any()))
            .thenThrow(BusinessException.conflict("用户名已存在"))
        
        mockMvc.perform(
            post("/api/v1/users")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request))
        )
            .andExpect(status().isConflict)
    }

    // ========== PUT /api/v1/users/{id} 测试 ==========

    @Test
    fun `PUT - api v1 users id - 有效请求返回200`() {
        val userId = "test_user_001"
        val request = UserCreateRequest(
            username = "updateduser",
            email = "updated@example.com",
            password = "password123"
        )
        val response = buildUserBasicResponse(id = userId, username = "updateduser")
        
        whenever(userApplicationService.updateUser(anyString(), any())).thenReturn(response)
        
        mockMvc.perform(
            put("/api/v1/users/$userId")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request))
        )
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.code").value(200))
            .andExpect(jsonPath("$.data.username").value("updateduser"))
    }

    @Test
    fun `PUT - api v1 users id - 用户不存在时返回404`() {
        val userId = "non_existent"
        val request = UserCreateRequest(
            username = "testuser",
            email = "test@example.com",
            password = "password123"
        )
        
        whenever(userApplicationService.updateUser(anyString(), any()))
            .thenThrow(BusinessException.notFound("用户不存在"))
        
        mockMvc.perform(
            put("/api/v1/users/$userId")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request))
        )
            .andExpect(status().isNotFound)
    }

    // ========== DELETE /api/v1/users/{id} 测试 ==========

    @Test
    fun `DELETE - api v1 users id - 用户存在时返回200`() {
        val userId = "test_user_001"
        
        doNothing().`when`(userApplicationService).deleteUser(userId)
        
        mockMvc.perform(delete("/api/v1/users/$userId"))
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.code").value(200))
            .andExpect(jsonPath("$.message").value("用户删除成功"))
    }

    @Test
    fun `DELETE - api v1 users id - 用户不存在时返回404`() {
        val userId = "non_existent"
        
        doThrow(BusinessException.notFound("用户不存在"))
            .`when`(userApplicationService).deleteUser(userId)
        
        mockMvc.perform(delete("/api/v1/users/$userId"))
            .andExpect(status().isNotFound)
    }

    // ========== GET /api/v1/users/{id}/stats 测试 ==========

    @Test
    fun `GET - api v1 users id stats - 用户存在时返回200`() {
        val userId = "test_user_001"
        val stats = mapOf(
            "userId" to userId,
            "username" to "testuser",
            "routeCount" to 5,
            "tripCount" to 3,
            "favoriteCount" to 10,
            "completedCount" to 2
        )
        
        whenever(userApplicationService.getUserStats(userId)).thenReturn(stats)
        
        mockMvc.perform(get("/api/v1/users/$userId/stats"))
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.code").value(200))
            .andExpect(jsonPath("$.data.userId").value(userId))
            .andExpect(jsonPath("$.data.routeCount").value(5))
    }

    @Test
    fun `GET - api v1 users id stats - 用户不存在时返回404`() {
        val userId = "non_existent"
        
        whenever(userApplicationService.getUserStats(userId))
            .thenThrow(BusinessException.notFound("用户不存在"))
        
        mockMvc.perform(get("/api/v1/users/$userId/stats"))
            .andExpect(status().isNotFound)
    }

    private fun buildUserBasicResponse(
        id: String = IdGenerator.generateIdWithPrefix("user"),
        username: String = "testuser",
        email: String = "test@example.com"
    ): UserBasicResponse {
        return UserBasicResponse(
            id = id,
            username = username,
            email = email,
            nickname = "测试用户",
            phone = "13800138000",
            avatarUrl = "https://example.com/avatar.jpg",
            bio = null,
            status = 0,
            lastLoginAt = null,
            createdAt = Instant.now().epochSecond
        )
    }
}
