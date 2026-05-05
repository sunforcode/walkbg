package org.example.user.service

import org.example.user.model.User
import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable

/**
 * 用户领域服务接口
 * 职责：领域业务逻辑、业务规则验证、领域对象操作
 * 专注于纯领域逻辑，不直接操作Repository
 */
interface UserService {

    // ========== 业务规则验证 ==========
    /**
     * 验证用户创建请求
     * 领域规则：验证用户创建的业务规则
     */
    fun validateUserCreation(user: User)
    
    /**
     * 检查用户名是否可用
     * 领域规则：用户名唯一性检查
     */
    fun isUsernameAvailable(username: String): Boolean
    
    /**
     * 检查邮箱是否可用
     * 领域规则：邮箱唯一性检查
     */
    fun isEmailAvailable(email: String): Boolean
    
    // ========== 用户生命周期管理 ==========
    /**
     * 创建用户（包含业务规则验证）
     * 领域逻辑：应用创建用户的业务规则
     */
    fun createUserWithValidation(user: User): User

    /**
     * 激活用户
     * 领域规则：只有未激活的用户才能激活
     */
    fun activateUser(userId: String): Boolean
    
    /**
     * 停用用户
     * 领域规则：只有激活的用户才能停用
     */
    fun deactivateUser(userId: String): Boolean
    
    /**
     * 更新用户信息
     * 领域规则：验证更新的业务规则
     */
    fun updateUser(userId: String, user: User): User
    
    // ========== 数据访问方法（遵循分层架构） ==========
    /**
     * 分页搜索用户
     * 支持关键词和状态筛选
     */
    fun searchUsers(keyword: String?, status: Int?, pageable: Pageable): Page<User>

    /**
     * 根据ID获取用户
     * 简单的数据访问
     */
    fun getUserById(userId: String): User?

    /**
     * 根据用户名获取用户
     * 简单的数据访问
     */
    fun getUserByUsername(username: String): User?

    /**
     * 根据邮箱获取用户
     * 简单的数据访问
     */
    fun getUserByEmail(email: String): User?

    // ========== 统计数据访问方法 ==========
    /**
     * 统计用户创建的路线数量
     */
    fun countUserCreatedRoutes(userId: String): Long

    /**
     * 统计用户完成的路线数量
     */
    fun countUserCompletedRoutes(userId: String): Long

    /**
     * 统计用户收藏的路线数量
     */
    fun countUserFavoriteRoutes(userId: String): Long

    /**
     * 统计用户参与的行程数量
     */
    fun countUserTripParticipations(userId: String): Long

    // ========== 用户删除 ==========
    /**
     * 删除用户
     * 领域规则：用户不存在时抛出异常
     */
    fun deleteUser(userId: String)
}