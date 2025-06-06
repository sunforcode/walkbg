package org.example.controller

import org.example.model.User
import org.example.service.UserService
import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.*

/**
 * 用户控制器
 */
@RestController
@RequestMapping("/api/users")
@CrossOrigin(origins = ["*"])
class UserController(
    private val userService: UserService
) {

    /**
     * 获取所有用户（分页）
     */
    @GetMapping
    fun getAllUsers(pageable: Pageable): ResponseEntity<Page<User>> {
        val users = userService.getAllUsers(pageable)
        return ResponseEntity.ok(users)
    }

    /**
     * 根据ID获取用户
     */
    @GetMapping("/{id}")
    fun getUserById(@PathVariable id: String): ResponseEntity<User> {
        val user = userService.getUserById(id)
        return if (user != null) {
            ResponseEntity.ok(user)
        } else {
            ResponseEntity.notFound().build()
        }
    }

    /**
     * 根据邮箱获取用户
     */
    @GetMapping("/email/{email}")
    fun getUserByEmail(@PathVariable email: String): ResponseEntity<User> {
        val user = userService.getUserByEmail(email)
        return if (user != null) {
            ResponseEntity.ok(user)
        } else {
            ResponseEntity.notFound().build()
        }
    }

    /**
     * 根据用户名获取用户
     */
    @GetMapping("/username/{username}")
    fun getUserByUsername(@PathVariable username: String): ResponseEntity<User> {
        val user = userService.getUserByUsername(username)
        return if (user != null) {
            ResponseEntity.ok(user)
        } else {
            ResponseEntity.notFound().build()
        }
    }

    /**
     * 创建用户
     */
    @PostMapping
    fun createUser(@RequestBody user: User): ResponseEntity<User> {
        return try {
            val createdUser = userService.createUser(user)
            ResponseEntity.status(HttpStatus.CREATED).body(createdUser)
        } catch (e: Exception) {
            ResponseEntity.badRequest().build()
        }
    }

    /**
     * 更新用户
     */
    @PutMapping("/{id}")
    fun updateUser(@PathVariable id: String, @RequestBody user: User): ResponseEntity<User> {
        val updatedUser = userService.updateUser(id, user)
        return if (updatedUser != null) {
            ResponseEntity.ok(updatedUser)
        } else {
            ResponseEntity.notFound().build()
        }
    }

    /**
     * 删除用户
     */
    @DeleteMapping("/{id}")
    fun deleteUser(@PathVariable id: String): ResponseEntity<Void> {
        val deleted = userService.deleteUser(id)
        return if (deleted) {
            ResponseEntity.noContent().build()
        } else {
            ResponseEntity.notFound().build()
        }
    }

    /**
     * 搜索用户
     */
    @GetMapping("/search")
    fun searchUsers(
        @RequestParam(required = false) keyword: String?,
        @RequestParam(required = false) isActive: Boolean?,
        pageable: Pageable
    ): ResponseEntity<Page<User>> {
        val users = userService.searchUsers(keyword, isActive, pageable)
        return ResponseEntity.ok(users)
    }

    /**
     * 获取用户统计信息
     */
    @GetMapping("/statistics")
    fun getUserStatistics(): ResponseEntity<Map<String, Any>> {
        val statistics = userService.getUserStatistics()
        return ResponseEntity.ok(statistics)
    }

    /**
     * 获取用户个人统计信息
     */
    @GetMapping("/{id}/stats")
    fun getUserStats(@PathVariable id: String): ResponseEntity<Map<String, Any>> {
        val stats = userService.getUserStats(id)
        return ResponseEntity.ok(stats)
    }

    /**
     * 获取最活跃用户
     */
    @GetMapping("/most-active")
    fun getMostActiveUsers(pageable: Pageable): ResponseEntity<Page<User>> {
        val users = userService.getMostActiveUsers(pageable)
        return ResponseEntity.ok(users)
    }

    /**
     * 验证用户名和邮箱是否可用
     */
    @GetMapping("/validate")
    fun validateUser(
        @RequestParam username: String,
        @RequestParam email: String
    ): ResponseEntity<Map<String, Boolean>> {
        val isValid = userService.validateUser(username, email)
        val response = mapOf(
            "valid" to isValid,
            "usernameExists" to userService.existsByUsername(username),
            "emailExists" to userService.existsByEmail(email)
        )
        return ResponseEntity.ok(response)
    }
}