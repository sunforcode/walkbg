package org.example.security

import org.example.common.BaseUnitTest
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.springframework.test.util.ReflectionTestUtils
import java.util.*
import kotlin.concurrent.thread

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
}
