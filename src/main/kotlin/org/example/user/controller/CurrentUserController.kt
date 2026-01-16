package org.example.user.controller

import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.tags.Tag
import org.example.common.dto.ApiResponse
import org.example.common.exception.BusinessException
import org.example.common.util.ResponseUtil
import org.example.user.dto.UserBasicResponse
import org.example.user.service.UserApplicationService
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PutMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("/api/v1/user")
@Tag(name = "当前用户", description = "当前登录用户相关的API接口")
class CurrentUserController(
    private val userApplicationService: UserApplicationService
) {

    @GetMapping("/profile")
    @Operation(summary = "获取当前用户信息", description = "通过Token获取当前登录用户的信息")
    fun getCurrentUserProfile(): ResponseEntity<ApiResponse<UserBasicResponse>> {
        val testUserId = "user_guide_001"
        val user = userApplicationService.getUserById(testUserId)
            ?: throw BusinessException.notFound("用户不存在")
        return ResponseUtil.success(user)
    }

    @GetMapping("/stats")
    @Operation(summary = "获取当前用户统计信息")
    fun getCurrentUserStats(): ResponseEntity<ApiResponse<Map<String, Any>>> {
        val testUserId = "user_guide_001"
        val stats = userApplicationService.getUserStats(testUserId)
        return ResponseUtil.success(stats)
    }

    @PutMapping("/profile")
    @Operation(summary = "更新当前用户信息")
    fun updateCurrentUserProfile(
        @RequestBody request: org.example.user.dto.UserCreateRequest
    ): ResponseEntity<ApiResponse<UserBasicResponse>> {
        val testUserId = "user_guide_001"
        val user = userApplicationService.updateUser(testUserId, request)
        return ResponseUtil.success(user, "用户信息更新成功")
    }

    @GetMapping("/preferences")
    @Operation(summary = "获取用户偏好设置")
    fun getCurrentUserPreferences(): ResponseEntity<ApiResponse<Map<String, Any>>> {
        val preferences = mapOf(
            "theme" to "light",
            "language" to "zh_CN",
            "notifications" to true
        )
        return ResponseUtil.success(preferences)
    }

    @PutMapping("/preferences")
    @Operation(summary = "更新用户偏好设置")
    fun updateCurrentUserPreferences(
        @RequestBody preferences: Map<String, Any>
    ): ResponseEntity<ApiResponse<Map<String, Any>>> {
        return ResponseUtil.success(preferences, "偏好设置更新成功")
    }
}
