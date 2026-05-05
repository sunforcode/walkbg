package org.example.common

import org.example.common.util.IdGenerator
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test

class TestDataBuilderTest : BaseUnitTest() {
    
    @Test
    fun `buildUser should create a valid User`() {
        val user = TestDataBuilder.buildUser()
        
        assertNotNull(user.id)
        assertTrue(user.username.isNotEmpty())
        assertTrue(user.email.contains("@"))
        assertEquals("测试用户", user.nickname)
    }
    
    @Test
    fun `buildUserWithFixedId should create User with specific ID`() {
        val user = TestDataBuilder.buildUserWithFixedId(
            id = "custom_id_123",
            username = "customuser",
            email = "custom@test.com"
        )
        
        assertEquals("custom_id_123", user.id)
        assertEquals("customuser", user.username)
        assertEquals("custom@test.com", user.email)
    }
    
    @Test
    fun `buildInvalidUserShortUsername should create User with short username`() {
        val user = TestDataBuilder.buildInvalidUserShortUsername()
        
        assertTrue(user.username.length < 3)
    }
    
    @Test
    fun `buildInvalidUserBadEmail should create User with invalid email`() {
        val user = TestDataBuilder.buildInvalidUserBadEmail()
        
        assertFalse(user.email.contains("@"))
    }
    
    @Test
    fun `buildRoute should create a valid Route`() {
        val route = TestDataBuilder.buildRoute()
        
        assertNotNull(route.id)
        assertEquals(TestConstants.TEST_ROUTE_NAME, route.name)
        assertEquals(2, route.difficulty)
        assertEquals(1, route.routeType)
        assertEquals(TestConstants.TEST_USER_ID, route.createdBy)
    }
    
    @Test
    fun `generateId should generate unique IDs`() {
        val ids = mutableSetOf<String>()
        
        repeat(100) {
            val id = IdGenerator.generateId()
            assertTrue(ids.add(id), "ID 重复: $id")
        }
        
        assertEquals(100, ids.size)
    }
    
    @Test
    fun `generateRandomString should generate strings of specified length`() {
        val length = 10
        val randomString = IdGenerator.generateRandomString(length)
        
        assertEquals(length, randomString.length)
    }
}
