package org.example.user.service

import org.example.common.exception.BusinessException
import org.example.user.model.User
import org.example.user.repository.UserRepository
import org.springframework.cache.annotation.CacheEvict
import org.springframework.cache.annotation.Cacheable
import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

/**
 * 用户领域服务实现
 * 职责：实现领域业务逻辑、业务规则验证、协调数据访问
 * 
 * 缓存策略：
 * - 读操作：使用 @Cacheable 缓存用户数据
 * - 写操作：使用 @CacheEvict 清除缓存，确保数据一致性
 */
@Service
class UserServiceImpl(
    private val userRepository: UserRepository
) : UserService {

    // ========== 业务规则验证 ==========

    override fun validateUserCreation(user: User) {
        // 验证用户名唯一性
        if (!isUsernameAvailable(user.username)) {
            throw BusinessException.conflict("用户名已存在")
        }

        // 验证邮箱唯一性
        if (!isEmailAvailable(user.email)) {
            throw BusinessException.conflict("邮箱已存在")
        }

        // 其他业务规则验证可以在这里添加
        validateUserBusinessRules(user)
    }

    override fun isUsernameAvailable(username: String): Boolean {
        return !userRepository.existsByUsername(username)
    }

    override fun isEmailAvailable(email: String): Boolean {
        return !userRepository.existsByEmail(email)
    }

    /**
     * 验证用户业务规则
     */
    private fun validateUserBusinessRules(user: User) {
        // 用户名长度和格式验证（这里可以添加更复杂的业务规则）
        if (user.username.length < 3) {
            throw BusinessException.badRequest("用户名长度不能少于3个字符")
        }

        // 邮箱格式验证（基本验证，更复杂的验证在DTO层）
        if (!user.email.contains("@")) {
            throw BusinessException.badRequest("邮箱格式不正确")
        }
    }

    // ========== 用户生命周期管理 ==========

    @Transactional
    @CacheEvict(value = ["users"], allEntries = true)
    override fun createUserWithValidation(user: User): User {
        // 1. 业务规则验证
        validateUserCreation(user)

        // 2. 数据持久化
        return userRepository.save(user)
    }

    @Transactional
    @CacheEvict(value = ["users"], key = "#userId")
    override fun activateUser(userId: String): Boolean {
        val user = userRepository.findById(userId).orElse(null) ?: return false

        val updatedUser = user.copy()
        userRepository.save(updatedUser)
        return true
    }

    @Transactional
    @CacheEvict(value = ["users"], key = "#userId")
    override fun deactivateUser(userId: String): Boolean {
        val user = userRepository.findById(userId).orElse(null) ?: return false

        val updatedUser = user.copy()
        userRepository.save(updatedUser)
        return true
    }

    @Transactional
    @CacheEvict(value = ["users"], key = "#userId")
    override fun updateUser(userId: String, user: User): User {
        // 1. 检查用户是否存在
        val existingUser = userRepository.findById(userId).orElse(null)
            ?: throw BusinessException.notFound("用户不存在")

        // 2. 验证业务规则（如果用户名或邮箱变更，需要检查唯一性）
        if (user.username != existingUser.username && !isUsernameAvailable(user.username)) {
            throw BusinessException.conflict("用户名已存在")
        }
        
        if (user.email != existingUser.email && !isEmailAvailable(user.email)) {
            throw BusinessException.conflict("邮箱已存在")
        }

        // 3. 更新用户信息
        val updatedUser = existingUser.copy(
            username = user.username,
            nickname = user.nickname,
            email = user.email,
            avatarUrl = user.avatarUrl,
            phone = user.phone,
            updatedAt = java.time.Instant.now()
        )

        // 4. 保存更新
        return userRepository.save(updatedUser)
    }

    // ========== 数据访问方法 ==========

    @Transactional(readOnly = true)
    override fun searchUsers(keyword: String?, status: Int?, pageable: Pageable): Page<User> {
        // 注意：当前UserRepository的searchUsers方法不支持status参数
        // TODO: 如果需要按状态筛选，需要在UserRepository中添加相应的查询方法
        return userRepository.searchUsers(keyword, pageable)
    }

    @Transactional(readOnly = true)
    @Cacheable(value = ["users"], key = "#userId")
    override fun getUserById(userId: String): User? {
        return userRepository.findById(userId).orElse(null)
    }

    @Transactional(readOnly = true)
    @Cacheable(value = ["users"], key = "#username")
    override fun getUserByUsername(username: String): User? {
        return userRepository.findByUsername(username)
    }

    @Transactional(readOnly = true)
    override fun getUserByEmail(email: String): User? {
        return userRepository.findByEmail(email)
    }

    // ========== 统计数据访问方法 ==========

    @Transactional(readOnly = true)
    override fun countUserCreatedRoutes(userId: String): Long {
        return userRepository.countUserCreatedRoutes(userId)
    }

    @Transactional(readOnly = true)
    override fun countUserCompletedRoutes(userId: String): Long {
        return userRepository.countUserCompletedRoutes(userId)
    }

    @Transactional(readOnly = true)
    override fun countUserFavoriteRoutes(userId: String): Long {
        return userRepository.countUserFavoriteRoutes(userId)
    }

    @Transactional(readOnly = true)
    override fun countUserTripParticipations(userId: String): Long {
        return userRepository.countUserTripParticipations(userId)
    }

    @Transactional
    @CacheEvict(value = ["users"], key = "#userId")
    override fun deleteUser(userId: String) {
        if (!userRepository.existsById(userId)) {
            throw BusinessException.notFound("用户不存在")
        }
        userRepository.deleteById(userId)
    }
}
