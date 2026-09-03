package org.example.security

import com.fasterxml.jackson.databind.ObjectMapper
import jakarta.servlet.FilterChain
import jakarta.servlet.http.HttpServletRequest
import jakarta.servlet.http.HttpServletResponse
import org.example.account.repository.AccountSessionRepository
import org.example.common.contract.ApiError
import org.example.common.contract.ErrorResponse
import org.springframework.http.MediaType
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken
import org.springframework.security.core.authority.SimpleGrantedAuthority
import org.springframework.security.core.context.SecurityContextHolder
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource
import org.springframework.stereotype.Component
import org.springframework.util.StringUtils
import org.springframework.web.filter.OncePerRequestFilter

/**
 * JWT 认证过滤器
 * 从请求头中提取 JWT Token 并进行验证
 */
@Component
class JwtAuthenticationFilter(
    private val jwtTokenUtil: JwtTokenUtil,
    private val accountSessionRepository: AccountSessionRepository,
    private val objectMapper: ObjectMapper
) : OncePerRequestFilter() {

    companion object {
        private const val AUTH_HEADER = "Authorization"
        private const val TOKEN_PREFIX = "Bearer "
        private const val LEGACY_API_PREFIX = "/api/v1/legacy/"
    }

    override fun doFilterInternal(
        request: HttpServletRequest,
        response: HttpServletResponse,
        filterChain: FilterChain
    ) {
        val authorization = request.getHeader(AUTH_HEADER)
        if (!StringUtils.hasText(authorization)) {
            filterChain.doFilter(request, response)
            return
        }

        val authorizationParts = authorization!!.trim().split(Regex("\\s+"), limit = 2)
        if (!authorizationParts.first().equals(TOKEN_PREFIX.trim(), ignoreCase = true)) {
            filterChain.doFilter(request, response)
            return
        }
        val jwt = authorizationParts.getOrNull(1)?.takeIf(StringUtils::hasText)
        val credential = jwt?.let { validCredential(it, request.requestURI) }
        if (credential == null) {
            rejectAuthentication(response)
            return
        }

        if (SecurityContextHolder.getContext().authentication == null) {
            val authorities = listOf(SimpleGrantedAuthority("ROLE_USER"))
            val authentication = UsernamePasswordAuthenticationToken(
                CustomUserDetails(
                    userId = credential.userId,
                    username = credential.username,
                    password = "",
                    authorities = authorities,
                    sessionId = credential.sessionId
                ),
                null,
                authorities
            )
            authentication.details = WebAuthenticationDetailsSource().buildDetails(request)
            SecurityContextHolder.getContext().authentication = authentication
            logger.debug("Set user authentication for: ${credential.username}")
        }
        filterChain.doFilter(request, response)
    }

    private fun validCredential(jwt: String, requestPath: String): ValidCredential? {
        return try {
            if (!jwtTokenUtil.isTokenValidFormat(jwt) || jwtTokenUtil.isTokenExpired(jwt)) {
                null
            } else {
                val username = jwtTokenUtil.getUsernameFromToken(jwt)?.takeIf(StringUtils::hasText) ?: return null
                val userId = jwtTokenUtil.getUserIdFromToken(jwt)?.takeIf(StringUtils::hasText) ?: return null
                val tokenType = jwtTokenUtil.getTokenTypeFromToken(jwt)
                val sessionId = jwtTokenUtil.getSessionIdFromToken(jwt)
                if (requestPath.startsWith(LEGACY_API_PREFIX)) {
                    if (tokenType != null && tokenType != "legacy_access") return null
                    ValidCredential(username, userId, null)
                } else {
                    if (tokenType != "account_session" || sessionId.isNullOrBlank()) return null
                    if (!accountSessionRepository.existsByIdAndAccountIdAndRevokedAtIsNull(sessionId, userId)) return null
                    ValidCredential(username, userId, sessionId)
                }
            }
        } catch (exception: Exception) {
            logger.debug("Bearer credential validation failed", exception)
            null
        }
    }

    private fun rejectAuthentication(response: HttpServletResponse) {
        SecurityContextHolder.clearContext()
        response.status = HttpServletResponse.SC_UNAUTHORIZED
        response.contentType = MediaType.APPLICATION_JSON_VALUE
        response.characterEncoding = Charsets.UTF_8.name()
        objectMapper.writeValue(
            response.writer,
            ErrorResponse(ApiError("authentication_required", "需要有效的认证会话", retryable = false))
        )
    }

    private data class ValidCredential(
        val username: String,
        val userId: String,
        val sessionId: String?
    )
}
