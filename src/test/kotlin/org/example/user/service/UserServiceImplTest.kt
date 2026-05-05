package org.example.user.service

import org.example.common.BaseUnitTest
import org.example.common.TestDataBuilder
import org.example.common.exception.BusinessException
import org.example.user.model.User
import org.example.user.repository.UserRepository
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.mockito.InjectMocks
import org.mockito.Mock
import org.mockito.Mockito.*
import org.mockito.kotlin.any
import org.mockito.kotlin.whenever
import java.util.*

class UserServiceImplTest : BaseUnitTest() {

    @Mock
    private lateinit var userRepository: UserRepository

    @InjectMocks
    private lateinit var userService: UserServiceImpl

    // ========== createUserWithValidation 测试 ==========

    @Test
    fun `createUserWithValidation - 成功创建用户`() {
        val user = TestDataBuilder.buildUser()
        
        whenever(userRepository.existsByUsername(user.username)).thenReturn(false)
        whenever(userRepository.existsByEmail(user.email)).thenReturn(false)
        whenever(userRepository.save(any<User>())).thenReturn(user)
        
        val result = userService.createUserWithValidation(user)
        
        assertNotNull(result)
        assertEquals(user.id, result.id)
        assertEquals(user.username, result.username)
        assertEquals(user.email, result.email)
        verify(userRepository, times(1)).save(user)
    }

    @Test
    fun `createUserWithValidation - 用户名重复时抛出冲突异常`() {
        val user = TestDataBuilder.buildUser()
        
        whenever(userRepository.existsByUsername(user.username)).thenReturn(true)
        
        val exception = assertThrows<BusinessException> {
            userService.createUserWithValidation(user)
        }
        
        assertEquals("用户名已存在", exception.message)
        assertEquals(org.springframework.http.HttpStatus.CONFLICT, exception.httpStatus)
        verify(userRepository, never()).save(any<User>())
    }

    @Test
    fun `createUserWithValidation - 邮箱重复时抛出冲突异常`() {
        val user = TestDataBuilder.buildUser()
        
        whenever(userRepository.existsByUsername(user.username)).thenReturn(false)
        whenever(userRepository.existsByEmail(user.email)).thenReturn(true)
        
        val exception = assertThrows<BusinessException> {
            userService.createUserWithValidation(user)
        }
        
        assertEquals("邮箱已存在", exception.message)
        assertEquals(org.springframework.http.HttpStatus.CONFLICT, exception.httpStatus)
        verify(userRepository, never()).save(any<User>())
    }

    @Test
    fun `createUserWithValidation - 用户名过短时抛出请求异常`() {
        val user = TestDataBuilder.buildInvalidUserShortUsername()
        
        whenever(userRepository.existsByUsername(user.username)).thenReturn(false)
        whenever(userRepository.existsByEmail(user.email)).thenReturn(false)
        
        val exception = assertThrows<BusinessException> {
            userService.createUserWithValidation(user)
        }
        
        assertEquals("用户名长度不能少于3个字符", exception.message)
        assertEquals(org.springframework.http.HttpStatus.BAD_REQUEST, exception.httpStatus)
        verify(userRepository, never()).save(any<User>())
    }

    @Test
    fun `createUserWithValidation - 邮箱格式错误时抛出请求异常`() {
        val user = TestDataBuilder.buildInvalidUserBadEmail()
        
        whenever(userRepository.existsByUsername(user.username)).thenReturn(false)
        whenever(userRepository.existsByEmail(user.email)).thenReturn(false)
        
        val exception = assertThrows<BusinessException> {
            userService.createUserWithValidation(user)
        }
        
        assertEquals("邮箱格式不正确", exception.message)
        assertEquals(org.springframework.http.HttpStatus.BAD_REQUEST, exception.httpStatus)
        verify(userRepository, never()).save(any<User>())
    }

    // ========== validateUserCreation 测试 ==========

    @Test
    fun `validateUserCreation - 有效用户时不抛出异常`() {
        val user = TestDataBuilder.buildUser()
        
        whenever(userRepository.existsByUsername(user.username)).thenReturn(false)
        whenever(userRepository.existsByEmail(user.email)).thenReturn(false)
        
        assertDoesNotThrow {
            userService.validateUserCreation(user)
        }
    }

    // ========== isUsernameAvailable 测试 ==========

    @Test
    fun `isUsernameAvailable - 用户名可用时返回true`() {
        val username = "newuser"
        
        whenever(userRepository.existsByUsername(username)).thenReturn(false)
        
        val result = userService.isUsernameAvailable(username)
        
        assertTrue(result)
    }

    @Test
    fun `isUsernameAvailable - 用户名已存在时返回false`() {
        val username = "existinguser"
        
        whenever(userRepository.existsByUsername(username)).thenReturn(true)
        
        val result = userService.isUsernameAvailable(username)
        
        assertFalse(result)
    }

    // ========== isEmailAvailable 测试 ==========

    @Test
    fun `isEmailAvailable - 邮箱可用时返回true`() {
        val email = "new@example.com"
        
        whenever(userRepository.existsByEmail(email)).thenReturn(false)
        
        val result = userService.isEmailAvailable(email)
        
        assertTrue(result)
    }

    @Test
    fun `isEmailAvailable - 邮箱已存在时返回false`() {
        val email = "existing@example.com"
        
        whenever(userRepository.existsByEmail(email)).thenReturn(true)
        
        val result = userService.isEmailAvailable(email)
        
        assertFalse(result)
    }

    // ========== getUserById 测试 ==========

    @Test
    fun `getUserById - 用户存在时返回用户`() {
        val userId = "test_user_001"
        val user = TestDataBuilder.buildUserWithFixedId(id = userId)
        
        whenever(userRepository.findById(userId)).thenReturn(Optional.of(user))
        
        val result = userService.getUserById(userId)
        
        assertNotNull(result)
        assertEquals(userId, result?.id)
        assertEquals(user.username, result?.username)
    }

    @Test
    fun `getUserById - 用户不存在时返回null`() {
        val userId = "non_existent_user"
        
        whenever(userRepository.findById(userId)).thenReturn(Optional.empty())
        
        val result = userService.getUserById(userId)
        
        assertNull(result)
    }

    // ========== getUserByUsername 测试 ==========

    @Test
    fun `getUserByUsername - 用户存在时返回用户`() {
        val username = "testuser"
        val user = TestDataBuilder.buildUser(username = username)
        
        whenever(userRepository.findByUsername(username)).thenReturn(user)
        
        val result = userService.getUserByUsername(username)
        
        assertNotNull(result)
        assertEquals(username, result?.username)
    }

    @Test
    fun `getUserByUsername - 用户不存在时返回null`() {
        val username = "non_existent"
        
        whenever(userRepository.findByUsername(username)).thenReturn(null)
        
        val result = userService.getUserByUsername(username)
        
        assertNull(result)
    }

    // ========== getUserByEmail 测试 ==========

    @Test
    fun `getUserByEmail - 用户存在时返回用户`() {
        val email = "test@example.com"
        val user = TestDataBuilder.buildUser(email = email)
        
        whenever(userRepository.findByEmail(email)).thenReturn(user)
        
        val result = userService.getUserByEmail(email)
        
        assertNotNull(result)
        assertEquals(email, result?.email)
    }

    @Test
    fun `getUserByEmail - 用户不存在时返回null`() {
        val email = "non_existent@example.com"
        
        whenever(userRepository.findByEmail(email)).thenReturn(null)
        
        val result = userService.getUserByEmail(email)
        
        assertNull(result)
    }

    // ========== updateUser 测试 ==========

    @Test
    fun `updateUser - 用户不存在时抛出未找到异常`() {
        val userId = "non_existent"
        val updateUser = TestDataBuilder.buildUser()
        
        whenever(userRepository.findById(userId)).thenReturn(Optional.empty())
        
        val exception = assertThrows<BusinessException> {
            userService.updateUser(userId, updateUser)
        }
        
        assertEquals("用户不存在", exception.message)
        assertEquals(org.springframework.http.HttpStatus.NOT_FOUND, exception.httpStatus)
    }

    @Test
    fun `updateUser - 用户名变更且可用时更新成功`() {
        val userId = "test_user_001"
        val existingUser = TestDataBuilder.buildUserWithFixedId(
            id = userId,
            username = "oldusername",
            email = "old@example.com"
        )
        val updateUser = TestDataBuilder.buildUser(
            username = "newusername",
            email = "old@example.com"
        )
        
        whenever(userRepository.findById(userId)).thenReturn(Optional.of(existingUser))
        whenever(userRepository.existsByUsername("newusername")).thenReturn(false)
        whenever(userRepository.save(any<User>())).thenAnswer { it.getArgument(0) }
        
        val result = userService.updateUser(userId, updateUser)
        
        assertEquals("newusername", result.username)
        verify(userRepository, times(1)).save(any<User>())
    }

    @Test
    fun `updateUser - 用户名变更且已存在时抛出冲突异常`() {
        val userId = "test_user_001"
        val existingUser = TestDataBuilder.buildUserWithFixedId(
            id = userId,
            username = "oldusername",
            email = "old@example.com"
        )
        val updateUser = TestDataBuilder.buildUser(
            username = "existingusername",
            email = "old@example.com"
        )
        
        whenever(userRepository.findById(userId)).thenReturn(Optional.of(existingUser))
        whenever(userRepository.existsByUsername("existingusername")).thenReturn(true)
        
        val exception = assertThrows<BusinessException> {
            userService.updateUser(userId, updateUser)
        }
        
        assertEquals("用户名已存在", exception.message)
        assertEquals(org.springframework.http.HttpStatus.CONFLICT, exception.httpStatus)
    }

    @Test
    fun `updateUser - 邮箱变更且可用时更新成功`() {
        val userId = "test_user_001"
        val existingUser = TestDataBuilder.buildUserWithFixedId(
            id = userId,
            username = "testuser",
            email = "old@example.com"
        )
        val updateUser = TestDataBuilder.buildUser(
            username = "testuser",
            email = "new@example.com"
        )
        
        whenever(userRepository.findById(userId)).thenReturn(Optional.of(existingUser))
        whenever(userRepository.existsByEmail("new@example.com")).thenReturn(false)
        whenever(userRepository.save(any<User>())).thenAnswer { it.getArgument(0) }
        
        val result = userService.updateUser(userId, updateUser)
        
        assertEquals("new@example.com", result.email)
        verify(userRepository, times(1)).save(any<User>())
    }

    @Test
    fun `updateUser - 邮箱变更且已存在时抛出冲突异常`() {
        val userId = "test_user_001"
        val existingUser = TestDataBuilder.buildUserWithFixedId(
            id = userId,
            username = "testuser",
            email = "old@example.com"
        )
        val updateUser = TestDataBuilder.buildUser(
            username = "testuser",
            email = "existing@example.com"
        )
        
        whenever(userRepository.findById(userId)).thenReturn(Optional.of(existingUser))
        whenever(userRepository.existsByEmail("existing@example.com")).thenReturn(true)
        
        val exception = assertThrows<BusinessException> {
            userService.updateUser(userId, updateUser)
        }
        
        assertEquals("邮箱已存在", exception.message)
        assertEquals(org.springframework.http.HttpStatus.CONFLICT, exception.httpStatus)
    }

    @Test
    fun `updateUser - 用户名和邮箱不变更时更新成功`() {
        val userId = "test_user_001"
        val existingUser = TestDataBuilder.buildUserWithFixedId(
            id = userId,
            username = "testuser",
            email = "test@example.com",
            nickname = "旧昵称"
        )
        val updateUser = TestDataBuilder.buildUser(
            id = userId,
            username = "testuser",
            email = "test@example.com",
            nickname = "新昵称"
        )
        
        whenever(userRepository.findById(userId)).thenReturn(Optional.of(existingUser))
        whenever(userRepository.save(any<User>())).thenAnswer { it.getArgument(0) }
        
        val result = userService.updateUser(userId, updateUser)
        
        assertEquals("新昵称", result.nickname)
        assertEquals("testuser", result.username)
        assertEquals("test@example.com", result.email)
        verify(userRepository, times(1)).save(any<User>())
    }

    // ========== 统计方法测试 ==========

    @Test
    fun `countUserCreatedRoutes - 返回统计数量`() {
        val userId = "test_user_001"
        val expectedCount = 5L
        
        whenever(userRepository.countUserCreatedRoutes(userId)).thenReturn(expectedCount)
        
        val result = userService.countUserCreatedRoutes(userId)
        
        assertEquals(expectedCount, result)
    }

    @Test
    fun `countUserCompletedRoutes - 返回统计数量`() {
        val userId = "test_user_001"
        val expectedCount = 3L
        
        whenever(userRepository.countUserCompletedRoutes(userId)).thenReturn(expectedCount)
        
        val result = userService.countUserCompletedRoutes(userId)
        
        assertEquals(expectedCount, result)
    }

    @Test
    fun `countUserFavoriteRoutes - 返回统计数量`() {
        val userId = "test_user_001"
        val expectedCount = 10L
        
        whenever(userRepository.countUserFavoriteRoutes(userId)).thenReturn(expectedCount)
        
        val result = userService.countUserFavoriteRoutes(userId)
        
        assertEquals(expectedCount, result)
    }

    @Test
    fun `countUserTripParticipations - 返回统计数量`() {
        val userId = "test_user_001"
        val expectedCount = 7L
        
        whenever(userRepository.countUserTripParticipations(userId)).thenReturn(expectedCount)
        
        val result = userService.countUserTripParticipations(userId)
        
        assertEquals(expectedCount, result)
    }

    // ========== deleteUser 测试 ==========

    @Test
    fun `deleteUser - 用户存在时删除成功`() {
        val userId = "test_user_001"
        
        whenever(userRepository.existsById(userId)).thenReturn(true)
        
        assertDoesNotThrow {
            userService.deleteUser(userId)
        }
        
        verify(userRepository, times(1)).deleteById(userId)
    }

    @Test
    fun `deleteUser - 用户不存在时抛出未找到异常`() {
        val userId = "non_existent_user"
        
        whenever(userRepository.existsById(userId)).thenReturn(false)
        
        val exception = assertThrows<BusinessException> {
            userService.deleteUser(userId)
        }
        
        assertEquals("用户不存在", exception.message)
        assertEquals(org.springframework.http.HttpStatus.NOT_FOUND, exception.httpStatus)
        verify(userRepository, never()).deleteById(any<String>())
    }
}
