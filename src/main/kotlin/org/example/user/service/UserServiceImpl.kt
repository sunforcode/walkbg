package org.example.user.service

import org.example.common.exception.BusinessException
import org.example.user.model.User
import org.example.user.repository.UserRepository
import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

/**
 * 用户领域服务实现
 * 职责：实现领域业务逻辑、业务规则验证、协调数据访问
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
    override fun createUserWithValidation(user: User): User {
        // 1. 业务规则验证
        validateUserCreation(user)

        // 2. 数据持久化
        return userRepository.save(user)
    }

    @Transactional
    override fun activateUser(userId: String): Boolean {
        val user = userRepository.findById(userId).orElse(null) ?: return false

        val updatedUser = user.copy()
        userRepository.save(updatedUser)
        return true
    }

    @Transactional
    override fun deactivateUser(userId: String): Boolean {
        val user = userRepository.findById(userId).orElse(null) ?: return false

        val updatedUser = user.copy()
        userRepository.save(updatedUser)
        return true
    }

    // ========== 数据访问方法 ==========

    @Transactional(readOnly = true)
    override fun searchUsers(keyword: String?, status: Int?, pageable: Pageable): Page<User> {
        // 注意：当前UserRepository的searchUsers方法不支持status参数
        // TODO: 如果需要按状态筛选，需要在UserRepository中添加相应的查询方法
        return userRepository.searchUsers(keyword, pageable)
    }

    @Transactional(readOnly = true)
    override fun getUserById(userId: String): User? {
        return userRepository.findById(userId).orElse(null)
    }

    @Transactional(readOnly = true)
    override fun getUserByUsername(username: String): User? {
        return userRepository.findByUsername(username)
    }

    @Transactional(readOnly = true)
    override fun getUserByEmail(email: String): User? {
        return userRepository.findByEmail(email)
    }
}