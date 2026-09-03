package org.example.security

import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import com.fasterxml.jackson.databind.ObjectMapper
import org.example.common.contract.ApiError
import org.example.common.contract.ErrorResponse
import org.springframework.http.HttpMethod
import org.springframework.http.MediaType
import org.springframework.security.config.annotation.web.builders.HttpSecurity
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity
import org.springframework.security.config.annotation.web.invoke
import org.springframework.security.config.http.SessionCreationPolicy
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder
import org.springframework.security.crypto.password.PasswordEncoder
import org.springframework.security.web.SecurityFilterChain
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter
import org.example.config.CorsProperties
import org.springframework.web.cors.CorsConfiguration
import org.springframework.web.cors.CorsConfigurationSource
import org.springframework.web.cors.UrlBasedCorsConfigurationSource

@Configuration
@EnableWebSecurity
class SecurityConfig(
    private val jwtAuthenticationFilter: JwtAuthenticationFilter,
    private val corsProperties: CorsProperties,
    private val objectMapper: ObjectMapper
) {

    @Bean
    fun securityFilterChain(http: HttpSecurity): SecurityFilterChain {
        http {
            csrf { disable() }
            cors { configurationSource = corsConfigurationSource() }
            sessionManagement {
                sessionCreationPolicy = SessionCreationPolicy.STATELESS
            }
            authorizeHttpRequests {
                authorize("/api/v1/auth/verification-codes", permitAll)
                authorize(HttpMethod.POST, "/api/v1/auth/session", permitAll)
                authorize("/api/v1/legacy/auth/**", permitAll)
                authorize(HttpMethod.GET, "/api/v1/public-routes/*/trip-generation-context", authenticated)
                authorize("/api/v1/public-routes/**", permitAll)
                authorize("/api/v1/account/**", authenticated)
                authorize(HttpMethod.DELETE, "/api/v1/auth/session", authenticated)
                authorize("/api/v1/trips/**", authenticated)
                authorize("/api/v1/legacy/trips/**", authenticated)
                authorize("/api/v1/personal-equipment/**", authenticated)
                authorize("/api/v1/equipment-lists/**", authenticated)
                authorize("/api/v1/legacy/equipment-lists/**", authenticated)
                authorize("/api/**", permitAll)
                authorize("/api-docs/**", permitAll)
                authorize("/swagger-ui/**", permitAll)
                authorize("/h2-console/**", permitAll)
                authorize("/actuator/**", permitAll)
                authorize(anyRequest, permitAll)
            }
            exceptionHandling {
                authenticationEntryPoint = org.springframework.security.web.AuthenticationEntryPoint { _, response, _ ->
                    response.status = 401
                    response.contentType = MediaType.APPLICATION_JSON_VALUE
                    objectMapper.writeValue(
                        response.writer,
                        ErrorResponse(ApiError("authentication_required", "需要有效的认证会话", retryable = false))
                    )
                }
            }
            headers {
                frameOptions { disable() }
            }
            addFilterBefore<UsernamePasswordAuthenticationFilter>(jwtAuthenticationFilter)
        }
        return http.build()
    }

    /**
     * 允许的跨域来源由配置项 cors.allowed-origins 决定，
     * 与 WebConfig 中的 MVC 层配置共用同一份配置，避免两处不一致。
     * 使用 allowedOriginPatterns：它在后续开启凭证携带时仍然合法，
     * 而 allowedOrigins = ["*"] 与 allowCredentials 共存会被 Spring 拒绝。
     */
    @Bean
    fun corsConfigurationSource(): CorsConfigurationSource {
        val configuration = CorsConfiguration()
        configuration.allowedOriginPatterns = corsProperties.originPatterns()
        configuration.allowedMethods = listOf("GET", "POST", "PUT", "DELETE", "PATCH", "OPTIONS")
        configuration.allowedHeaders = listOf("*")
        configuration.exposedHeaders = listOf("Authorization")
        configuration.maxAge = 3600

        val source = UrlBasedCorsConfigurationSource()
        source.registerCorsConfiguration("/**", configuration)
        return source
    }

    @Bean
    fun passwordEncoder(): PasswordEncoder {
        return BCryptPasswordEncoder()
    }
}
