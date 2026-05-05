package org.example.common

import org.example.common.util.IdGenerator
import org.example.route.model.Route
import org.example.user.model.User
import java.time.Instant

object TestDataBuilder {
    
    fun buildUser(
        id: String = IdGenerator.generateId(),
        username: String = "testuser_${IdGenerator.generateRandomString(6)}",
        nickname: String = "测试用户",
        email: String = "${IdGenerator.generateRandomString(8)}@example.com",
        avatarUrl: String? = "https://example.com/avatar.jpg",
        phone: String? = "13800138000",
        bio: String? = "这是一个测试用户",
        createdAt: Instant = Instant.now(),
        updatedAt: Instant = Instant.now()
    ): User {
        return User(
            id = id,
            username = username,
            nickname = nickname,
            email = email,
            avatarUrl = avatarUrl,
            phone = phone,
            bio = bio,
            createdAt = createdAt,
            updatedAt = updatedAt
        )
    }
    
    fun buildUserWithFixedId(
        id: String = TestConstants.TEST_USER_ID,
        username: String = TestConstants.TEST_USERNAME,
        nickname: String = "测试用户",
        email: String = TestConstants.TEST_EMAIL
    ): User {
        return buildUser(
            id = id,
            username = username,
            nickname = nickname,
            email = email
        )
    }
    
    fun buildInvalidUserShortUsername(): User {
        return buildUser(
            username = "ab"
        )
    }
    
    fun buildInvalidUserBadEmail(): User {
        return buildUser(
            email = "invalid-email"
        )
    }
    
    fun buildRoute(
        id: String = IdGenerator.generateId(),
        name: String = TestConstants.TEST_ROUTE_NAME,
        description: String? = "这是一条测试路线",
        difficulty: Int? = 2,
        routeType: Int? = 1,
        createdBy: String = TestConstants.TEST_USER_ID
    ): Route {
        return Route(
            id = id,
            name = name,
            description = description,
            difficulty = difficulty,
            routeType = routeType,
            createdBy = createdBy
        )
    }
}
