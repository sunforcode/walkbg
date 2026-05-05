package org.example.security

import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder
import org.springframework.stereotype.Service

/**
 * 密码加密服务
 * 使用 BCrypt 算法进行密码加密和验证
 */
@Service
class PasswordEncoderService {

    private val passwordEncoder = BCryptPasswordEncoder()

    /**
     * 加密原始密码
     * @param rawPassword 原始密码
     * @return 加密后的密码
     */
    fun encode(rawPassword: String): String {
        return passwordEncoder.encode(rawPassword)
    }

    /**
     * 验证原始密码是否与加密密码匹配
     * @param rawPassword 原始密码
     * @param encodedPassword 加密后的密码
     * @return 是否匹配
     */
    fun matches(rawPassword: String, encodedPassword: String): Boolean {
        return passwordEncoder.matches(rawPassword, encodedPassword)
    }
}
