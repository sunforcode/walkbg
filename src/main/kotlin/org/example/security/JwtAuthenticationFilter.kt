package org.example.security

import jakarta.servlet.FilterChain
import jakarta.servlet.http.HttpServletRequest
import jakarta.servlet.http.HttpServletResponse
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
    private val jwtTokenUtil: JwtTokenUtil
) : OncePerRequestFilter() {

    companion object {
        private const val AUTH_HEADER = "Authorization"
        private const val TOKEN_PREFIX = "Bearer "
    }

    override fun doFilterInternal(
        request: HttpServletRequest,
        response: HttpServletResponse,
        filterChain: FilterChain
    ) {
        try {
            val jwt = getJwtFromRequest(request)

            if (jwt != null && StringUtils.hasText(jwt) && jwtTokenUtil.isTokenValidFormat(jwt)) {
                val username = jwtTokenUtil.getUsernameFromToken(jwt)
                val userId = jwtTokenUtil.getUserIdFromToken(jwt)

                if (username != null && SecurityContextHolder.getContext().authentication == null) {
                    if (!jwtTokenUtil.isTokenExpired(jwt)) {
                        val authorities = listOf(SimpleGrantedAuthority("ROLE_USER"))
                        
                        val authentication = UsernamePasswordAuthenticationToken(
                            CustomUserDetails(
                                userId = userId ?: "",
                                username = username,
                                password = "",
                                authorities = authorities
                            ),
                            null,
                            authorities
                        )
                        authentication.details = WebAuthenticationDetailsSource().buildDetails(request)

                        SecurityContextHolder.getContext().authentication = authentication
                        logger.debug("Set user authentication for: $username")
                    }
                }
            }
        } catch (e: Exception) {
            logger.error("Could not set user authentication in security context", e)
        }

        filterChain.doFilter(request, response)
    }

    /**
     * 从请求头中提取 JWT Token
     */
    private fun getJwtFromRequest(request: HttpServletRequest): String? {
        val bearerToken = request.getHeader(AUTH_HEADER)
        return if (StringUtils.hasText(bearerToken) && bearerToken.startsWith(TOKEN_PREFIX)) {
            bearerToken.substring(TOKEN_PREFIX.length)
        } else {
            null
        }
    }
}
