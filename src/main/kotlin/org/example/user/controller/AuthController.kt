package org.example.user.controller

import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.tags.Tag
import jakarta.validation.Valid
import org.example.common.dto.ApiResponse
import org.example.common.util.ResponseUtil
import org.example.user.dto.*
import org.example.user.service.AuthApplicationService
import org.springframework.http.ResponseEntity
import org.springframework.validation.annotation.Validated
import org.springframework.web.bind.annotation.*

/**
 * 认证控制器
 * 职责：HTTP接口、请求参数验证、响应格式化
 * 严格遵循分层架构，所有业务逻辑都通过 ApplicationService
 */
@RestController
@RequestMapping("/api/v1/legacy/auth")
@Tag(name = "认证管理", description = "用户认证相关的API接口")
@Validated
class AuthController(
    private val authApplicationService: AuthApplicationService
) {

    /**
     * 用户登录
     */
    @PostMapping("/login")
    @Operation(summary = "用户登录", description = "使用用户名和密码登录，返回JWT Token")
    fun login(@RequestBody @Valid request: UserLoginRequest): ResponseEntity<ApiResponse<UserLoginResponse>> {
        val response = authApplicationService.login(request)
        return ResponseUtil.success(response, "登录成功")
    }

    /**
     * 用户注册
     * 注册成功后自动登录，返回包含Token的响应
     */
    @PostMapping("/register")
    @Operation(summary = "用户注册", description = "创建新用户账户，注册成功后自动登录并返回Token")
    fun register(@RequestBody @Valid request: UserRegisterRequest): ResponseEntity<ApiResponse<UserLoginResponse>> {
        val response = authApplicationService.register(request)
        return ResponseUtil.created(response, "注册成功")
    }

    /**
     * 刷新 Token
     */
    @PostMapping("/refresh")
    @Operation(summary = "刷新Token", description = "使用刷新Token获取新的访问Token")
    fun refreshToken(@RequestBody request: TokenRefreshRequest): ResponseEntity<ApiResponse<TokenRefreshResponse>> {
        val response = authApplicationService.refreshToken(request)
        return ResponseUtil.success(response, "Token刷新成功")
    }

    /**
     * 用户登出
     */
    @PostMapping("/logout")
    @Operation(summary = "用户登出", description = "使当前Token失效")
    fun logout(): ResponseEntity<ApiResponse<Nothing>> {
        return ResponseUtil.success(null, "登出成功")
    }

    /**
     * 检查用户名是否可用
     */
    @GetMapping("/check-username/{username}")
    @Operation(summary = "检查用户名是否可用", description = "检查用户名是否已被注册")
    fun checkUsername(
        @PathVariable username: String
    ): ResponseEntity<ApiResponse<Map<String, Any>>> {
        val available = authApplicationService.checkUsernameAvailability(username)
        val result = mapOf(
            "username" to username,
            "available" to available
        )
        return ResponseUtil.success(result)
    }

    /**
     * 检查邮箱是否可用
     */
    @GetMapping("/check-email/{email}")
    @Operation(summary = "检查邮箱是否可用", description = "检查邮箱是否已被注册")
    fun checkEmail(
        @PathVariable email: String
    ): ResponseEntity<ApiResponse<Map<String, Any>>> {
        val available = authApplicationService.checkEmailAvailability(email)
        val result = mapOf(
            "email" to email,
            "available" to available
        )
        return ResponseUtil.success(result)
    }
}
