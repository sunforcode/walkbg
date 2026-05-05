package org.example.security

import org.example.common.BaseUnitTest
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test

class PasswordEncoderServiceTest : BaseUnitTest() {

    private val passwordEncoderService = PasswordEncoderService()

    // ========== encode 测试 ==========

    @Test
    fun `encode - 成功加密密码`() {
        val rawPassword = "password123"

        val encoded = passwordEncoderService.encode(rawPassword)

        assertNotNull(encoded)
        assertTrue(encoded.isNotEmpty())
        assertNotEquals(rawPassword, encoded)
    }

    @Test
    fun `encode - 相同密码每次生成不同的hash`() {
        val rawPassword = "password123"

        val encoded1 = passwordEncoderService.encode(rawPassword)
        val encoded2 = passwordEncoderService.encode(rawPassword)

        assertNotEquals(encoded1, encoded2)
    }

    @Test
    fun `encode - 空字符串也能加密`() {
        val rawPassword = ""

        val encoded = passwordEncoderService.encode(rawPassword)

        assertNotNull(encoded)
        assertTrue(encoded.isNotEmpty())
    }

    @Test
    fun `encode - 长密码也能加密`() {
        val rawPassword = "a".repeat(100)

        val encoded = passwordEncoderService.encode(rawPassword)

        assertNotNull(encoded)
        assertTrue(encoded.isNotEmpty())
    }

    @Test
    fun `encode - 特殊字符密码也能加密`() {
        val rawPassword = "P@ssw0rd!@#$%^&*()"

        val encoded = passwordEncoderService.encode(rawPassword)

        assertNotNull(encoded)
        assertTrue(encoded.isNotEmpty())
    }

    @Test
    fun `encode - 中文密码也能加密`() {
        val rawPassword = "密码123测试"

        val encoded = passwordEncoderService.encode(rawPassword)

        assertNotNull(encoded)
        assertTrue(encoded.isNotEmpty())
    }

    // ========== matches 测试 ==========

    @Test
    fun `matches - 正确密码返回true`() {
        val rawPassword = "password123"
        val encoded = passwordEncoderService.encode(rawPassword)

        val matches = passwordEncoderService.matches(rawPassword, encoded)

        assertTrue(matches)
    }

    @Test
    fun `matches - 错误密码返回false`() {
        val rawPassword = "password123"
        val wrongPassword = "wrongpassword"
        val encoded = passwordEncoderService.encode(rawPassword)

        val matches = passwordEncoderService.matches(wrongPassword, encoded)

        assertFalse(matches)
    }

    @Test
    fun `matches - 不同hash但相同密码都匹配`() {
        val rawPassword = "password123"
        val encoded1 = passwordEncoderService.encode(rawPassword)
        val encoded2 = passwordEncoderService.encode(rawPassword)

        val matches1 = passwordEncoderService.matches(rawPassword, encoded1)
        val matches2 = passwordEncoderService.matches(rawPassword, encoded2)

        assertTrue(matches1)
        assertTrue(matches2)
        assertNotEquals(encoded1, encoded2)
    }

    @Test
    fun `matches - 空字符串密码匹配`() {
        val rawPassword = ""
        val encoded = passwordEncoderService.encode(rawPassword)

        val matches = passwordEncoderService.matches(rawPassword, encoded)

        assertTrue(matches)
    }

    @Test
    fun `matches - 特殊字符密码匹配`() {
        val rawPassword = "P@ssw0rd!@#$%^&*()"
        val encoded = passwordEncoderService.encode(rawPassword)

        val matches = passwordEncoderService.matches(rawPassword, encoded)

        assertTrue(matches)
    }

    @Test
    fun `matches - 中文密码匹配`() {
        val rawPassword = "密码123测试"
        val encoded = passwordEncoderService.encode(rawPassword)

        val matches = passwordEncoderService.matches(rawPassword, encoded)

        assertTrue(matches)
    }

    @Test
    fun `matches - 空的encoded密码返回false`() {
        val matches = passwordEncoderService.matches("password123", "")

        assertFalse(matches)
    }

    // ========== BCrypt 特性测试 ==========

    @Test
    fun `encode - 生成的hash以BCrypt前缀开头`() {
        val rawPassword = "password123"
        val encoded = passwordEncoderService.encode(rawPassword)

        assertTrue(encoded.startsWith("\$2a\$") || encoded.startsWith("\$2b\$"))
    }

    @Test
    fun `encode - 生成的hash长度固定`() {
        val encoded1 = passwordEncoderService.encode("short")
        val encoded2 = passwordEncoderService.encode("a".repeat(100))

        assertEquals(encoded1.length, encoded2.length)
        assertTrue(encoded1.length > 50)
    }

    // ========== 边界值测试 ==========

    @Test
    fun `matches - 大小写敏感`() {
        val rawPassword = "Password123"
        val encoded = passwordEncoderService.encode(rawPassword)

        val matchesLowercase = passwordEncoderService.matches("password123", encoded)
        val matchesUppercase = passwordEncoderService.matches("PASSWORD123", encoded)
        val matchesExact = passwordEncoderService.matches("Password123", encoded)

        assertFalse(matchesLowercase)
        assertFalse(matchesUppercase)
        assertTrue(matchesExact)
    }

    @Test
    fun `matches - 空格敏感`() {
        val rawPassword = "password 123"
        val encoded = passwordEncoderService.encode(rawPassword)

        val matchesWithSpace = passwordEncoderService.matches("password 123", encoded)
        val matchesWithoutSpace = passwordEncoderService.matches("password123", encoded)

        assertTrue(matchesWithSpace)
        assertFalse(matchesWithoutSpace)
    }

    // ========== 登录场景模拟测试 ==========

    @Test
    fun `完整登录流程 - 注册后登录成功`() {
        val username = "testuser"
        val password = "SecurePass123!"

        val encodedPassword = passwordEncoderService.encode(password)

        val loginSuccess = passwordEncoderService.matches(password, encodedPassword)

        assertTrue(loginSuccess)
    }

    @Test
    fun `完整登录流程 - 错误密码登录失败`() {
        val username = "testuser"
        val correctPassword = "SecurePass123!"
        val wrongPassword = "WrongPass456!"

        val encodedPassword = passwordEncoderService.encode(correctPassword)

        val loginSuccess = passwordEncoderService.matches(wrongPassword, encodedPassword)

        assertFalse(loginSuccess)
    }
}
