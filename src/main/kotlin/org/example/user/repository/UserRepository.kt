package org.example.user.repository

import org.example.user.model.User
import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Modifying
import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.query.Param
import org.springframework.stereotype.Repository
import java.time.Instant

/**
 * 用户数据访问层
 */
@Repository
interface UserRepository : JpaRepository<User, String> {

    /**
     * 根据邮箱查找用户
     */
    fun findByEmail(email: String): User?

    /**
     * 根据用户名查找用户
     */
    fun findByUsername(username: String): User?

    /**
     * 根据认证手机号查找账号
     */
    fun findByPhone(phone: String): User?

    /**
     * 检查邮箱是否存在
     */
    fun existsByEmail(email: String): Boolean

    /**
     * 检查用户名是否存在
     */
    fun existsByUsername(username: String): Boolean

    /**
     * 根据用户名或邮箱查找用户
     */
    fun findByUsernameOrEmail(username: String, email: String): User?



    /**
     * 根据昵称模糊查询
     */
    fun findByNicknameContainingIgnoreCase(nickname: String, pageable: Pageable): Page<User>

    /**
     * 查找完成路线数量大于指定值的用户
     */
    @Query("""
        SELECT u FROM User u
        WHERE u.id IN (
            SELECT ucr.userId FROM UserCompletedRoute ucr
            GROUP BY ucr.userId
            HAVING COUNT(ucr) > :count
        )
    """)
    fun findUsersWithCompletedRoutesGreaterThan(@Param("count") count: Int, pageable: Pageable): Page<User>

    /**
     * 多条件搜索用户（支持关键词和状态筛选）
     */
    @Query("""
        SELECT u FROM User u
        WHERE (:keyword IS NULL OR
               LOWER(u.username) LIKE LOWER(CONCAT('%', :keyword, '%')) OR
               LOWER(u.nickname) LIKE LOWER(CONCAT('%', :keyword, '%')) OR
               LOWER(u.email) LIKE LOWER(CONCAT('%', :keyword, '%')))
        AND (:status IS NULL OR u.status = :status)
    """)
    fun searchUsers(
        @Param("keyword") keyword: String?,
        @Param("status") status: Int?,
        pageable: Pageable
    ): Page<User>

    /**
     * 获取用户统计信息（基于关联表）
     */
    @Query("""
        SELECT new map(
            COUNT(u) as totalUsers
        )
        FROM User u
    """)
    fun getUserStatistics(): Map<String, Any>

    /**
     * 更新用户最近登录时间
     */
    @Modifying
    @Query("UPDATE User u SET u.lastLoginAt = :lastLoginAt WHERE u.id = :userId")
    fun updateLastLoginAt(@Param("userId") userId: String, @Param("lastLoginAt") lastLoginAt: Instant)

    /**
     * 查找最近注册的用户
     */
    fun findTop10ByOrderByCreatedAtDesc(): List<User>

    /**
     * 统计用户创建的路线数量
     */
    @Query("""
        SELECT COUNT(r) FROM Route r WHERE r.createdBy = :userId
    """)
    fun countUserCreatedRoutes(@Param("userId") userId: String): Long

    /**
     * 统计用户完成的路线数量
     */
    @Query("""
        SELECT COUNT(ucr) FROM UserCompletedRoute ucr WHERE ucr.userId = :userId
    """)
    fun countUserCompletedRoutes(@Param("userId") userId: String): Long

    /**
     * 统计用户收藏的路线数量
     * 注意：单向关联，直接使用userId字段，不需要JOIN
     */
    @Query("""
        SELECT COUNT(ufr) FROM UserFavoriteRoute ufr WHERE ufr.userId = :userId
    """)
    fun countUserFavoriteRoutes(@Param("userId") userId: String): Long

    /**
     * 统计用户的装备清单数量
     */
    @Query("""
        SELECT COUNT(el) FROM EquipmentList el WHERE el.creatorId = :userId
    """)
    fun countUserEquipmentLists(@Param("userId") userId: String): Long

    /**
     * 统计用户参与的行程数量
     */
    @Query("""
        SELECT COUNT(tp) FROM TripParticipant tp WHERE tp.userId = :userId
    """)
    fun countUserTripParticipations(@Param("userId") userId: String): Long

    /**
     * 查找最活跃的用户（按完成路线数排序）
     */
    @Query("""
        SELECT u, COUNT(ucr) as completedCount
        FROM User u
        LEFT JOIN UserCompletedRoute ucr ON ucr.userId = u.id
        GROUP BY u.id
        ORDER BY completedCount DESC
    """)
    fun findTop10MostActiveUsers(pageable: Pageable): Page<User>
}