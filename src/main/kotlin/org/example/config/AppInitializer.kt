package org.example.config

import org.example.common.util.IdGenerator
import org.example.security.PasswordEncoderService
import org.example.user.model.User
import org.example.user.repository.UserRepository
import org.slf4j.LoggerFactory
import org.springframework.boot.ApplicationArguments
import org.springframework.boot.ApplicationRunner
import org.springframework.stereotype.Component
import java.time.Instant

/**
 * 应用初始化器
 * 用于在应用启动时执行初始化任务，如为现有用户设置默认密码、创建默认管理员用户
 */
@Component
class AppInitializer(
    private val userRepository: UserRepository,
    private val passwordEncoderService: PasswordEncoderService
) : ApplicationRunner {

    private val logger = LoggerFactory.getLogger(AppInitializer::class.java)

    // 默认管理员用户信息
    private val defaultAdminUsername = "admin"
    private val defaultAdminPassword = "123456"
    private val defaultAdminEmail = "admin@walkbg.com"
    private val defaultAdminNickname = "管理员"

    override fun run(args: ApplicationArguments) {
        // 1. 创建默认管理员用户（如果不存在）
        createDefaultAdminUser()
        
        // 2. 为没有密码的用户设置默认密码
        initializeUserPasswords()
    }

    /**
     * 创建默认管理员用户
     * 用于测试和开发环境
     */
    private fun createDefaultAdminUser() {
        // 检查是否已存在 admin 用户
        val existingAdmin = userRepository.findByUsername(defaultAdminUsername)
        
        if (existingAdmin != null) {
            logger.info("Default admin user already exists: $defaultAdminUsername")
            return
        }

        // 创建默认管理员用户
        try {
            val adminUser = User(
                id = IdGenerator.generateIdWithPrefix("user"),
                username = defaultAdminUsername,
                nickname = defaultAdminNickname,
                email = defaultAdminEmail,
                password = passwordEncoderService.encode(defaultAdminPassword),
                createdAt = Instant.now(),
                updatedAt = Instant.now()
            )

            userRepository.save(adminUser)
            logger.info("========================================")
            logger.info("Default admin user created successfully!")
            logger.info("  Username: $defaultAdminUsername")
            logger.info("  Password: $defaultAdminPassword")
            logger.info("  Email: $defaultAdminEmail")
            logger.info("========================================")
        } catch (e: Exception) {
            logger.error("Failed to create default admin user: ${e.message}")
        }
    }

    /**
     * 为没有密码的用户设置默认密码
     * 这是为了兼容之前没有认证机制的数据库
     */
    private fun initializeUserPasswords() {
        val users = userRepository.findAll()
        var updatedCount = 0

        for (user in users) {
            if (user.password.isNullOrBlank()) {
                // 设置默认密码 "123456"
                val defaultPassword = "123456"
                user.password = passwordEncoderService.encode(defaultPassword)
                userRepository.save(user)
                updatedCount++
                logger.info("Set default password for user: ${user.username}")
            }
        }

        if (updatedCount > 0) {
            logger.info("Initialized passwords for $updatedCount users with default password: '123456'")
        } else {
            logger.info("All users already have passwords, no initialization needed")
        }
    }
}
