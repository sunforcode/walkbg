package org.example.user.controller

import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.Parameter
import io.swagger.v3.oas.annotations.tags.Tag
import org.example.common.dto.ApiResponse
import org.example.common.util.ResponseUtil
import org.example.user.dto.UserBasicResponse
import org.example.user.dto.UserCreateRequest
import org.example.user.service.UserApplicationService
import org.springframework.http.ResponseEntity
import org.springframework.validation.annotation.Validated
import org.springframework.web.bind.annotation.*
import jakarta.validation.Valid

/**
 * 用户控制器
 * 职责：HTTP接口、请求参数验证、响应格式化
 * 严格遵循分层架构，所有业务逻辑都通过ApplicationService
 */
@RestController
@RequestMapping("/api/user")
@Tag(name = "用户管理", description = "用户相关的API接口")
@Validated
class UserController(
    private val userApplicationService: UserApplicationService
) {

    /**
     * 根据ID获取用户
     */
    @GetMapping("/{id}")
    @Operation(summary = "根据ID获取用户", description = "根据用户ID获取用户信息")
    fun getUserById(
        @Parameter(description = "用户ID") @PathVariable id: String
    ): ResponseEntity<ApiResponse<UserBasicResponse>> {
        val user = userApplicationService.getUserById(id)
        return ResponseUtil.conditional(
            data = user,
            successMessage = "获取用户信息成功",
            notFoundMessage = "用户不存在"
        )
    }

    /**
     * 根据用户名获取用户
     */
    @GetMapping("/username/{username}")
    @Operation(summary = "根据用户名获取用户", description = "根据用户名获取用户信息")
    fun getUserByUsername(
        @Parameter(description = "用户名") @PathVariable username: String
    ): ResponseEntity<ApiResponse<UserBasicResponse>> {
        val user = userApplicationService.getUserByUsername(username)
        return ResponseUtil.conditional(
            data = user,
            successMessage = "获取用户信息成功",
            notFoundMessage = "用户不存在"
        )
    }

    /**
     * 创建用户
     */
    @PostMapping
    @Operation(summary = "创建用户", description = "创建新的用户")
    fun createUser(
        @Valid @RequestBody request: UserCreateRequest
    ): ResponseEntity<ApiResponse<UserBasicResponse>> {
        val createdUser = userApplicationService.createUser(request)
        return ResponseUtil.created(createdUser, "创建用户成功")
    }
}