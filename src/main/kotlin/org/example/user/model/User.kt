package org.example.user.model

import jakarta.persistence.*
import java.time.Instant

/**
 * 用户领域模型
 * 
 * 设计原则：
 * 1. 单向关联：不持有其他实体的集合引用，避免循环依赖和 N+1 查询
 * 2. 按需查询：需要关联数据时通过 Repository 查询
 * 3. 富领域模型：包含业务行为，而不仅仅是数据容器
 */
@Entity
@Table(
    name = "users",
    indexes = [
        Index(name = "idx_users_email", columnList = "email", unique = true),
        Index(name = "idx_users_username", columnList = "username", unique = true)
    ]
)
data class User(
    @Id
    @Column(length = 64)
    val id: String,

    @Column(nullable = false, unique = true, length = 50)
    var username: String = "",

    @Column(nullable = false, length = 50)
    var nickname: String = "",

    @Column(nullable = false, unique = true, length = 100)
    var email: String = "",

    @Column(name = "avatar_url", length = 500)
    var avatarUrl: String? = null,

    @Column(name = "phone", length = 20)
    var phone: String? = null,

    @Column(name = "password", nullable = false, length = 255)
    var password: String = "",

    @Column(name = "bio", columnDefinition = "TEXT")
    var bio: String? = null,

    @Column(name = "status", nullable = false, columnDefinition = "INT DEFAULT 0")
    var status: Int = 0,

    @Column(name = "created_at", nullable = false, updatable = false)
    val createdAt: Instant = Instant.now(),

    @Column(name = "last_login_at")
    var lastLoginAt: Instant? = null,

    @Column(name = "updated_at", nullable = false)
    var updatedAt: Instant = Instant.now()
) {
    /**
     * 注意：不再持有以下关联关系的集合引用
     * - organizedTrips: 通过 TripRepository.findByOrganizerId(userId) 查询
     * - createdRoutes: 通过 RouteRepository.findByCreatedBy(userId) 查询
     * - tripParticipations: 通过 TripParticipantRepository.findByUserId(userId) 查询
     * - favoriteRoutes: 通过 UserFavoriteRouteRepository.findByUserId(userId) 查询
     * - completedRoutes: 通过 UserCompletedRouteRepository.findByUserId(userId) 查询
     * - equipmentItems: 通过 UserEquipmentItemRepository.findByUserId(userId) 查询
     * - createdContacts: 通过 ContactRepository.findByCreatedBy(userId) 查询
     * 
     * 优势：
     * 1. 避免 N+1 查询问题
     * 2. 减少内存占用
     * 3. 避免序列化死循环
     * 4. 提高查询灵活性（按需加载）
     */

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as User

        return id == other.id
    }

    override fun hashCode(): Int {
        return id.hashCode()
    }

    override fun toString(): String {
        return "User(id='$id', username='$username', email='$email')"
    }
}
