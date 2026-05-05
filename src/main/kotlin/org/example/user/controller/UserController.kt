package org.example.user.controller

import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.Parameter
import io.swagger.v3.oas.annotations.tags.Tag
import org.example.common.dto.ApiResponse
import org.example.common.exception.BusinessException
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
@RequestMapping("/api/v1/users")
@Tag(name = "用户管理", description = "用户相关的API接口")
@Validated
class UserController(
    private val userApplicationService: UserApplicationService
) {

    /**
     * 分页查询用户列表
     */
    @GetMapping
    @Operation(summary = "分页查询用户列表", description = "获取用户列表，支持分页和搜索")
    fun getUsers(
        @Parameter(description = "页码，从0开始") @RequestParam(defaultValue = "0") page: Int,
        @Parameter(description = "每页大小") @RequestParam(defaultValue = "10") size: Int,
        @Parameter(description = "关键词搜索") @RequestParam(required = false) keyword: String?,
        @Parameter(description = "用户状态") @RequestParam(required = false) status: Int?
    ): ResponseEntity<ApiResponse<org.springframework.data.domain.Page<UserBasicResponse>>> {
        val pageable = org.springframework.data.domain.PageRequest.of(page, size)
        val users = userApplicationService.searchUsers(keyword, status, pageable)
        return ResponseUtil.successPage(users)
    }

    /**
     * 根据ID获取用户详情
     */
    @GetMapping("/{id}")
    @Operation(summary = "查询用户详情", description = "根据用户ID获取详细信息")
    fun getUserById(
        @Parameter(description = "用户ID") @PathVariable id: String
    ): ResponseEntity<ApiResponse<UserBasicResponse>> {
        val user = userApplicationService.getUserById(id)
            ?: throw BusinessException.notFound("用户不存在")
        return ResponseUtil.success(user)
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
            ?: throw BusinessException.notFound("用户不存在")
        return ResponseUtil.success(user)
    }

    /**
     * 创建用户
     */
    @PostMapping
    @Operation(summary = "创建用户", description = "创建新的用户")
    fun createUser(
        @RequestBody @Valid request: UserCreateRequest
    ): ResponseEntity<ApiResponse<UserBasicResponse>> {
        val user = userApplicationService.createUser(request)
        return ResponseUtil.created(user, "用户创建成功")
    }

    /**
     * 更新用户
     */
    @PutMapping("/{id}")
    @Operation(summary = "更新用户", description = "更新指定ID的用户信息")
    fun updateUser(
        @Parameter(description = "用户ID") @PathVariable id: String,
        @RequestBody @Valid request: UserCreateRequest
    ): ResponseEntity<ApiResponse<UserBasicResponse>> {
        val user = userApplicationService.updateUser(id, request)
        return ResponseUtil.success(user, "用户更新成功")
    }

    /**
     * 删除用户
     */
    @DeleteMapping("/{id}")
    @Operation(summary = "删除用户", description = "删除指定ID的用户")
    fun deleteUser(
        @Parameter(description = "用户ID") @PathVariable id: String
    ): ResponseEntity<ApiResponse<Nothing>> {
        userApplicationService.deleteUser(id)
        return ResponseUtil.noContent("用户删除成功")
    }

    /**
     * 获取用户统计信息
     */
    @GetMapping("/{id}/stats")
    @Operation(summary = "获取用户统计信息", description = "获取用户的路线、行程等统计数据")
    fun getUserStats(
        @Parameter(description = "用户ID") @PathVariable id: String
    ): ResponseEntity<ApiResponse<Map<String, Any>>> {
        val stats = userApplicationService.getUserStats(id)
        return ResponseUtil.success(stats)
    }
}