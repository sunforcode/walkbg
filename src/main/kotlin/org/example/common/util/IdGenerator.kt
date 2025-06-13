package org.example.common.util

import java.security.SecureRandom
import java.time.Instant
import java.util.*

/**
 * ID生成器工具类
 */
object IdGenerator {
    
    private val random = SecureRandom()
    private val chars = "0123456789ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz"
    
    /**
     * 生成基于时间戳的唯一ID
     */
    fun generateId(): String {
        val timestamp = Instant.now().toEpochMilli()
        val randomPart = generateRandomString(8)
        return "${timestamp}_$randomPart"
    }
    
    /**
     * 生成UUID
     */
    fun generateUUID(): String {
        return UUID.randomUUID().toString()
    }
    
    /**
     * 生成指定长度的随机字符串
     */
    fun generateRandomString(length: Int): String {
        val sb = StringBuilder(length)
        repeat(length) {
            sb.append(chars[random.nextInt(chars.length)])
        }
        return sb.toString()
    }
    
    /**
     * 生成带前缀的ID
     */
    fun generateIdWithPrefix(prefix: String): String {
        return "${prefix}_${generateId()}"
    }
}