package org.example.security

import io.jsonwebtoken.Claims
import io.jsonwebtoken.ExpiredJwtException
import io.jsonwebtoken.Jwts
import io.jsonwebtoken.security.Keys
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Component
import java.util.*
import javax.crypto.SecretKey

/**
 * JWT Token 工具类
 * 负责生成、解析和验证 JWT Token
 */
@Component
class JwtTokenUtil {

    @Value("\${jwt.secret:walkbg-jwt-secret-key-must-be-at-least-256-bits-long-for-hs256}")
    private lateinit var secret: String

    @Value("\${jwt.expiration:86400000}")
    private var expiration: Long = 86400000

    @Value("\${jwt.refresh-expiration:604800000}")
    private var refreshExpiration: Long = 604800000

    private val signingKey: SecretKey by lazy {
        Keys.hmacShaKeyFor(secret.toByteArray(Charsets.UTF_8))
    }

    /**
     * 从 Token 中获取用户名
     */
    fun getUsernameFromToken(token: String): String? {
        return getClaimFromToken(token) { it.subject }
    }

    /**
     * 从 Token 中获取用户ID
     */
    fun getUserIdFromToken(token: String): String? {
        return getClaimFromToken(token) { it["userId", String::class.java] }
    }

    /**
     * 从 Token 中获取过期时间
     */
    fun getExpirationDateFromToken(token: String): Date? {
        return getClaimFromToken(token) { it.expiration }
    }

    /**
     * 从 Token 中获取指定的 Claim
     */
    fun <T> getClaimFromToken(token: String, claimsResolver: (Claims) -> T): T? {
        return try {
            val claims = getAllClaimsFromToken(token)
            claimsResolver(claims)
        } catch (e: Exception) {
            null
        }
    }

    /**
     * 从 Token 中获取所有 Claims
     */
    private fun getAllClaimsFromToken(token: String): Claims {
        return Jwts.parser()
            .verifyWith(signingKey)
            .build()
            .parseSignedClaims(token)
            .payload
    }

    /**
     * 检查 Token 是否过期
     */
    fun isTokenExpired(token: String): Boolean {
        return try {
            val expiration = getExpirationDateFromToken(token)
            expiration?.before(Date()) ?: true
        } catch (e: ExpiredJwtException) {
            true
        }
    }

    /**
     * 生成 Access Token
     */
    fun generateToken(userId: String, username: String): String {
        val claims = mutableMapOf<String, Any>()
        claims["userId"] = userId
        claims["username"] = username
        return doGenerateToken(claims, username, expiration)
    }

    /**
     * 生成 Refresh Token
     */
    fun generateRefreshToken(userId: String, username: String): String {
        val claims = mutableMapOf<String, Any>()
        claims["userId"] = userId
        claims["type"] = "refresh"
        return doGenerateToken(claims, username, refreshExpiration)
    }

    /**
     * 实际生成 Token 的方法
     */
    private fun doGenerateToken(claims: Map<String, Any>, subject: String, expirationTime: Long): String {
        val now = Date()
        val expirationDate = Date(now.time + expirationTime)

        return Jwts.builder()
            .claims(claims)
            .subject(subject)
            .issuedAt(now)
            .expiration(expirationDate)
            .signWith(signingKey)
            .compact()
    }

    /**
     * 验证 Token 是否有效
     */
    fun validateToken(token: String, username: String): Boolean {
        val tokenUsername = getUsernameFromToken(token)
        return (tokenUsername == username && !isTokenExpired(token))
    }

    /**
     * 验证 Token 格式是否正确
     */
    fun isTokenValidFormat(token: String): Boolean {
        return try {
            getAllClaimsFromToken(token)
            true
        } catch (e: Exception) {
            false
        }
    }
}
