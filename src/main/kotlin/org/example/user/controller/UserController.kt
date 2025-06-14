package org.example.user.controller

import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.Parameter
import io.swagger.v3.oas.annotations.tags.Tag
import org.example.common.dto.ApiResponse
import org.example.common.dto.BaseQueryRequest
import org.example.common.exception.BusinessException
import org.example.common.util.ResponseUtil
import org.example.user.model.User
import org.example.user.service.UserService
import org.springframework.data.domain.Page
import org.springframework.http.ResponseEntity
import org.springframework.validation.annotation.Validated
import org.springframework.web.bind.annotation.*
import jakarta.validation.Valid

/**
 * 用户控制器
 */
@RestController
@RequestMapping("/api/users")
@Tag(name = "用户管理", description = "用户相关的API接口")
@Validated
class UserController(
    private val userService: UserService
) {

    /**
     * 获取所有用户（分页）
     */
    @GetMapping
    @Operation(summary = "获取用户列表", description = "分页获取所有用户")
    fun getAllUsers(
        @Valid @ModelAttribute request: BaseQueryRequest
    ): ResponseEntity<ApiResponse<Page<User>>> {
        val users = userService.getAllUsers(request.toPageable())
        return ResponseUtil.successPage(users, "获取用户列表成功")
    }

    /**
     * 根据ID获取用户
     */
    @GetMapping("/{id}")
    @Operation(summary = "根据ID获取用户", description = "根据用户ID获取用户信息")
    fun getUserById(
        @Parameter(description = "用户ID") @PathVariable id: String
    ): ResponseEntity<ApiResponse<User>> {
        val user = userService.getUserById(id)
        return ResponseUtil.conditional(
            data = user,
            successMessage = "获取用户信息成功",
            notFoundMessage = "用户不存在"
        )
    }

    /**
     * 根据邮箱获取用户
     */
    @GetMapping("/email/{email}")
    @Operation(summary = "根据邮箱获取用户", description = "根据邮箱地址获取用户信息")
    fun getUserByEmail(
        @Parameter(description = "邮箱地址") @PathVariable email: String
    ): ResponseEntity<ApiResponse<User>> {
        val user = userService.getUserByEmail(email)
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
    ): ResponseEntity<ApiResponse<User>> {
        val user = userService.getUserByUsername(username)
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
        @Valid @RequestBody user: User
    ): ResponseEntity<ApiResponse<User>> {
        // 业务验证
        if (user.username.isBlank()) {
            throw BusinessException.badRequest("用户名不能为空")
        }
        if (user.email.isBlank()) {
            throw BusinessException.badRequest("邮箱不能为空")
        }
        if (userService.existsByUsername(user.username)) {
            throw BusinessException.conflict("用户名已存在")
        }
        if (userService.existsByEmail(user.email)) {
            throw BusinessException.conflict("邮箱已存在")
        }

        val createdUser = userService.createUser(user)
        return ResponseUtil.created(createdUser, "创建用户成功")
    }

    /**
     * 更新用户
     */
    @PutMapping("/{id}")
    @Operation(summary = "更新用户", description = "根据ID更新用户信息")
    fun updateUser(
        @Parameter(description = "用户ID") @PathVariable id: String,
        @Valid @RequestBody user: User
    ): ResponseEntity<ApiResponse<User>> {
        val updatedUser = userService.updateUser(id, user)
        return ResponseUtil.conditional(
            data = updatedUser,
            successMessage = "更新用户成功",
            notFoundMessage = "用户不存在"
        )
    }

    /**
     * 删除用户
     */
    @DeleteMapping("/{id}")
    @Operation(summary = "删除用户", description = "根据ID删除用户")
    fun deleteUser(
        @Parameter(description = "用户ID") @PathVariable id: String
    ): ResponseEntity<ApiResponse<Nothing>> {
        val deleted = userService.deleteUser(id)
        return ResponseUtil.conditionalOperation(
            success = deleted,
            successMessage = "删除用户成功",
            failMessage = "用户不存在或删除失败"
        )
    }

    /**
     * 搜索用户
     */

    /**
     * 获取用户统计信息
     */
    @GetMapping("/statistics")
    @Operation(summary = "获取用户统计", description = "获取用户相关的统计信息")
    fun getUserStatistics(): ResponseEntity<ApiResponse<Map<String, Any>>> {
        val statistics = userService.getUserStatistics()
        return ResponseUtil.success(statistics, "获取用户统计成功")
    }

    /**
     * 获取用户个人统计信息
     */
    @GetMapping("/{id}/stats")
    @Operation(summary = "获取用户个人统计", description = "获取指定用户的个人统计信息")
    fun getUserStats(
        @Parameter(description = "用户ID") @PathVariable id: String
    ): ResponseEntity<ApiResponse<Map<String, Any>>> {
        val stats = userService.getUserStats(id)
        return ResponseUtil.success(stats, "获取用户个人统计成功")
    }

    /**
     * 获取最活跃用户
     */
    @GetMapping("/most-active")
    @Operation(summary = "获取最活跃用户", description = "获取最活跃的用户列表")
    fun getMostActiveUsers(
        @Valid @ModelAttribute request: BaseQueryRequest
    ): ResponseEntity<ApiResponse<Page<User>>> {
        val users = userService.getMostActiveUsers(request.toPageable())
        return ResponseUtil.successPage(users, "获取最活跃用户成功")
    }

    /**
     * 验证用户名和邮箱是否可用
     */
    @GetMapping("/validate")
    @Operation(summary = "验证用户信息", description = "验证用户名和邮箱是否可用")
    fun validateUser(
        @Parameter(description = "用户名") @RequestParam username: String,
        @Parameter(description = "邮箱地址") @RequestParam email: String
    ): ResponseEntity<ApiResponse<Map<String, Boolean>>> {
        if (username.isBlank()) {
            throw BusinessException.badRequest("用户名不能为空")
        }
        if (email.isBlank()) {
            throw BusinessException.badRequest("邮箱不能为空")
        }

        val isValid = userService.validateUser(username, email)
        val response = mapOf(
            "valid" to isValid,
            "usernameExists" to userService.existsByUsername(username),
            "emailExists" to userService.existsByEmail(email)
        )
        return ResponseUtil.success(response, "验证用户信息成功")
    }
}