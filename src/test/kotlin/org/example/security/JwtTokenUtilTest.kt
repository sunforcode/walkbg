package org.example.security

import com.fasterxml.jackson.databind.ObjectMapper
import org.example.account.repository.AccountSessionRepository
import org.example.common.BaseUnitTest
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.mockito.kotlin.mock
import org.mockito.kotlin.never
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever
import org.springframework.mock.web.MockHttpServletRequest
import org.springframework.mock.web.MockHttpServletResponse
import org.springframework.test.util.ReflectionTestUtils
import java.util.*

class JwtTokenUtilTest : BaseUnitTest() {

    private lateinit var jwtTokenUtil: JwtTokenUtil

    private val testSecret = "walkbg-jwt-secret-key-must-be-at-least-256-bits-long-for-hs256-testing"
    private val testUserId = "user_test_001"
    private val testUsername = "testuser"

    @BeforeEach
    override fun setUp() {
        super.setUp()
        jwtTokenUtil = JwtTokenUtil()
        ReflectionTestUtils.setField(jwtTokenUtil, "secret", testSecret)
        ReflectionTestUtils.setField(jwtTokenUtil, "expiration", 86400000L)
        ReflectionTestUtils.setField(jwtTokenUtil, "refreshExpiration", 604800000L)
        ReflectionTestUtils.invokeMethod<Unit>(jwtTokenUtil, "getSigningKey")
    }

    // ========== generateToken 测试 ==========

    @Test
    fun `generateToken - 成功生成access token`() {
        val token = jwtTokenUtil.generateToken(testUserId, testUsername)

        assertNotNull(token)
        assertTrue(token.isNotEmpty())
    }

    @Test
    fun `generateToken - 生成的token可以解析出正确信息`() {
        val token = jwtTokenUtil.generateToken(testUserId, testUsername)

        val username = jwtTokenUtil.getUsernameFromToken(token)
        val userId = jwtTokenUtil.getUserIdFromToken(token)

        assertEquals(testUsername, username)
        assertEquals(testUserId, userId)
    }

    @Test
    fun `generateToken marks new legacy access tokens explicitly`() {
        val token = jwtTokenUtil.generateToken(testUserId, testUsername)

        assertEquals("legacy_access", jwtTokenUtil.getTokenTypeFromToken(token))
    }

    @Test
    fun `account session and refresh tokens remain distinguishable`() {
        val accountSession = jwtTokenUtil.generateAccountSessionToken(testUserId, "session-1")
        val refreshToken = jwtTokenUtil.generateRefreshToken(testUserId, testUsername)

        assertEquals("account_session", jwtTokenUtil.getTokenTypeFromToken(accountSession))
        assertEquals("refresh", jwtTokenUtil.getTokenTypeFromToken(refreshToken))
    }

    // ========== generateRefreshToken 测试 ==========

    @Test
    fun `generateRefreshToken - 成功生成refresh token`() {
        val refreshToken = jwtTokenUtil.generateRefreshToken(testUserId, testUsername)

        assertNotNull(refreshToken)
        assertTrue(refreshToken.isNotEmpty())
    }

    @Test
    fun `generateRefreshToken - 生成的refresh token包含type声明`() {
        val refreshToken = jwtTokenUtil.generateRefreshToken(testUserId, testUsername)

        val username = jwtTokenUtil.getUsernameFromToken(refreshToken)
        val userId = jwtTokenUtil.getUserIdFromToken(refreshToken)

        assertEquals(testUsername, username)
        assertEquals(testUserId, userId)
    }

    // ========== getUsernameFromToken 测试 ==========

    @Test
    fun `getUsernameFromToken - 有效token返回用户名`() {
        val token = jwtTokenUtil.generateToken(testUserId, testUsername)

        val username = jwtTokenUtil.getUsernameFromToken(token)

        assertEquals(testUsername, username)
    }

    @Test
    fun `getUsernameFromToken - 无效token返回null`() {
        val username = jwtTokenUtil.getUsernameFromToken("invalid_token")

        assertNull(username)
    }

    @Test
    fun `getUsernameFromToken - 空字符串返回null`() {
        val username = jwtTokenUtil.getUsernameFromToken("")

        assertNull(username)
    }

    // ========== getUserIdFromToken 测试 ==========

    @Test
    fun `getUserIdFromToken - 有效token返回用户ID`() {
        val token = jwtTokenUtil.generateToken(testUserId, testUsername)

        val userId = jwtTokenUtil.getUserIdFromToken(token)

        assertEquals(testUserId, userId)
    }

    @Test
    fun `getUserIdFromToken - 无效token返回null`() {
        val userId = jwtTokenUtil.getUserIdFromToken("invalid_token")

        assertNull(userId)
    }

    // ========== getExpirationDateFromToken 测试 ==========

    @Test
    fun `getExpirationDateFromToken - 有效token返回过期时间`() {
        val token = jwtTokenUtil.generateToken(testUserId, testUsername)

        val expiration = jwtTokenUtil.getExpirationDateFromToken(token)

        assertNotNull(expiration)
        assertTrue(expiration!!.after(Date()))
    }

    @Test
    fun `getExpirationDateFromToken - 无效token返回null`() {
        val expiration = jwtTokenUtil.getExpirationDateFromToken("invalid_token")

        assertNull(expiration)
    }

    // ========== isTokenExpired 测试 ==========

    @Test
    fun `isTokenExpired - 新生成的token未过期`() {
        val token = jwtTokenUtil.generateToken(testUserId, testUsername)

        val isExpired = jwtTokenUtil.isTokenExpired(token)

        assertFalse(isExpired)
    }

    @Test
    fun `isTokenExpired - 无效token返回true`() {
        val isExpired = jwtTokenUtil.isTokenExpired("invalid_token")

        assertTrue(isExpired)
    }

    // ========== validateToken 测试 ==========

    @Test
    fun `validateToken - 正确用户名的token验证通过`() {
        val token = jwtTokenUtil.generateToken(testUserId, testUsername)

        val isValid = jwtTokenUtil.validateToken(token, testUsername)

        assertTrue(isValid)
    }

    @Test
    fun `validateToken - 错误用户名的token验证失败`() {
        val token = jwtTokenUtil.generateToken(testUserId, testUsername)

        val isValid = jwtTokenUtil.validateToken(token, "wronguser")

        assertFalse(isValid)
    }

    @Test
    fun `validateToken - 无效token验证失败`() {
        val isValid = jwtTokenUtil.validateToken("invalid_token", testUsername)

        assertFalse(isValid)
    }

    // ========== isTokenValidFormat 测试 ==========

    @Test
    fun `isTokenValidFormat - 有效token返回true`() {
        val token = jwtTokenUtil.generateToken(testUserId, testUsername)

        val isValid = jwtTokenUtil.isTokenValidFormat(token)

        assertTrue(isValid)
    }

    @Test
    fun `isTokenValidFormat - 无效token返回false`() {
        val isValid = jwtTokenUtil.isTokenValidFormat("invalid_token")

        assertFalse(isValid)
    }

    @Test
    fun `isTokenValidFormat - 空字符串返回false`() {
        val isValid = jwtTokenUtil.isTokenValidFormat("")

        assertFalse(isValid)
    }

    // ========== Access Token 与 Refresh Token 对比测试 ==========

    @Test
    fun `generateToken 与 generateRefreshToken - 生成的token不同`() {
        val accessToken = jwtTokenUtil.generateToken(testUserId, testUsername)
        val refreshToken = jwtTokenUtil.generateRefreshToken(testUserId, testUsername)

        assertNotEquals(accessToken, refreshToken)
    }

    @Test
    fun `generateToken 与 generateRefreshToken - 都包含用户信息`() {
        val accessToken = jwtTokenUtil.generateToken(testUserId, testUsername)
        val refreshToken = jwtTokenUtil.generateRefreshToken(testUserId, testUsername)

        val accessUsername = jwtTokenUtil.getUsernameFromToken(accessToken)
        val refreshUsername = jwtTokenUtil.getUsernameFromToken(refreshToken)

        assertEquals(testUsername, accessUsername)
        assertEquals(testUsername, refreshUsername)
    }

    // ========== 边界值测试 ==========

    @Test
    fun `generateToken - 空用户ID也能生成token`() {
        val token = jwtTokenUtil.generateToken("", testUsername)

        assertNotNull(token)
        assertTrue(token.isNotEmpty())
    }

    @Test
    fun `generateToken - 空用户名也能生成token`() {
        val token = jwtTokenUtil.generateToken(testUserId, "")

        assertNotNull(token)
        assertTrue(token.isNotEmpty())
    }

    // ========== getClaimFromToken 测试 ==========

    @Test
    fun `getClaimFromToken - 可以获取自定义claim`() {
        val token = jwtTokenUtil.generateToken(testUserId, testUsername)

        val username = jwtTokenUtil.getClaimFromToken(token) { it.subject }
        val userId = jwtTokenUtil.getClaimFromToken(token) { it["userId", String::class.java] }

        assertEquals(testUsername, username)
        assertEquals(testUserId, userId)
    }

    @Test
    fun `JWT filter rejects an invalid bearer token before a public endpoint`() {
        val sessionRepository = mock<AccountSessionRepository>()
        val filter = JwtAuthenticationFilter(jwtTokenUtil, sessionRepository, ObjectMapper())
        val request = MockHttpServletRequest("GET", "/api/v1/public-routes/featured").apply {
            addHeader("Authorization", "Bearer invalid-token")
        }
        val response = MockHttpServletResponse()
        val chain = mock<jakarta.servlet.FilterChain>()

        filter.doFilter(request, response, chain)

        assertEquals(401, response.status)
        assertEquals(
            "authentication_required",
            ObjectMapper().readTree(response.contentAsString).path("error").path("code").asText()
        )
        assertFalse(ObjectMapper().readTree(response.contentAsString).path("error").path("retryable").asBoolean(true))
        verify(chain, never()).doFilter(request, response)
    }

    @Test
    fun `JWT filter rejects an account session not bound to the token account`() {
        val sessionRepository = mock<AccountSessionRepository>()
        whenever(sessionRepository.existsByIdAndAccountIdAndRevokedAtIsNull("session-1", testUserId)).thenReturn(false)
        val filter = JwtAuthenticationFilter(jwtTokenUtil, sessionRepository, ObjectMapper())
        val request = MockHttpServletRequest("GET", "/api/v1/public-routes/featured").apply {
            addHeader("Authorization", "Bearer ${jwtTokenUtil.generateAccountSessionToken(testUserId, "session-1")}")
        }
        val response = MockHttpServletResponse()
        val chain = mock<jakarta.servlet.FilterChain>()

        filter.doFilter(request, response, chain)

        assertEquals(401, response.status)
        verify(sessionRepository).existsByIdAndAccountIdAndRevokedAtIsNull("session-1", testUserId)
        verify(chain, never()).doFilter(request, response)
    }

    @Test
    fun `JWT filter accepts account session only when session and account are active together`() {
        val sessionRepository = mock<AccountSessionRepository>()
        whenever(sessionRepository.existsByIdAndAccountIdAndRevokedAtIsNull("session-1", testUserId)).thenReturn(true)
        val filter = JwtAuthenticationFilter(jwtTokenUtil, sessionRepository, ObjectMapper())
        val request = MockHttpServletRequest("GET", "/api/v1/account/profile").apply {
            addHeader("Authorization", "Bearer ${jwtTokenUtil.generateAccountSessionToken(testUserId, "session-1")}")
        }
        val response = MockHttpServletResponse()
        val chain = mock<jakarta.servlet.FilterChain>()

        filter.doFilter(request, response, chain)

        assertEquals(200, response.status)
        verify(chain).doFilter(request, response)
    }

    @Test
    fun `JWT filter rejects a bearer scheme without a token`() {
        val sessionRepository = mock<AccountSessionRepository>()
        val filter = JwtAuthenticationFilter(jwtTokenUtil, sessionRepository, ObjectMapper())
        val request = MockHttpServletRequest("GET", "/api/v1/public-routes/featured").apply {
            addHeader("Authorization", "Bearer ")
        }
        val response = MockHttpServletResponse()
        val chain = mock<jakarta.servlet.FilterChain>()

        filter.doFilter(request, response, chain)

        assertEquals(401, response.status)
        verify(chain, never()).doFilter(request, response)
    }

    @Test
    fun `JWT filter accepts a valid legacy access token only on the legacy namespace`() {
        val sessionRepository = mock<AccountSessionRepository>()
        val filter = JwtAuthenticationFilter(jwtTokenUtil, sessionRepository, ObjectMapper())
        val request = MockHttpServletRequest("GET", "/api/v1/legacy/trips").apply {
            addHeader("Authorization", "Bearer ${jwtTokenUtil.generateToken(testUserId, testUsername)}")
        }
        val response = MockHttpServletResponse()
        val chain = mock<jakarta.servlet.FilterChain>()

        filter.doFilter(request, response, chain)

        assertEquals(200, response.status)
        verify(sessionRepository, never()).existsByIdAndAccountIdAndRevokedAtIsNull(org.mockito.kotlin.any(), org.mockito.kotlin.any())
        verify(chain).doFilter(request, response)
    }

    @Test
    fun `JWT filter rejects refresh tokens on legacy paths`() {
        val sessionRepository = mock<AccountSessionRepository>()
        val filter = JwtAuthenticationFilter(jwtTokenUtil, sessionRepository, ObjectMapper())
        val request = MockHttpServletRequest("GET", "/api/v1/legacy/trips").apply {
            addHeader("Authorization", "Bearer ${jwtTokenUtil.generateRefreshToken(testUserId, testUsername)}")
        }
        val response = MockHttpServletResponse()
        val chain = mock<jakarta.servlet.FilterChain>()

        filter.doFilter(request, response, chain)

        assertEquals(401, response.status)
        verify(chain, never()).doFilter(request, response)
    }

    @Test
    fun `JWT filter rejects a legacy token on every target namespace including public reads`() {
        listOf(
            "/api/v1/account/profile",
            "/api/v1/public-routes/featured",
            "/api/v1/personal-equipment",
            "/api/v1/equipment-lists",
            "/api/v1/trips"
        ).forEach { path ->
            val sessionRepository = mock<AccountSessionRepository>()
            val filter = JwtAuthenticationFilter(jwtTokenUtil, sessionRepository, ObjectMapper())
            val request = MockHttpServletRequest("GET", path).apply {
                addHeader("Authorization", "Bearer ${jwtTokenUtil.generateToken(testUserId, testUsername)}")
            }
            val response = MockHttpServletResponse()
            val chain = mock<jakarta.servlet.FilterChain>()

            filter.doFilter(request, response, chain)

            assertEquals(401, response.status, path)
            verify(chain, never()).doFilter(request, response)
        }
    }

    @Test
    fun `JWT filter allows a public request without authorization`() {
        val sessionRepository = mock<AccountSessionRepository>()
        val filter = JwtAuthenticationFilter(jwtTokenUtil, sessionRepository, ObjectMapper())
        val request = MockHttpServletRequest("GET", "/api/v1/public-routes/featured")
        val response = MockHttpServletResponse()
        val chain = mock<jakarta.servlet.FilterChain>()

        filter.doFilter(request, response, chain)

        assertEquals(200, response.status)
        verify(chain).doFilter(request, response)
    }
}
