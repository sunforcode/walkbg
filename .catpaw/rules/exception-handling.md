---
ruleType: Manual
description: WalkBG项目异常处理规范和最佳实践
globs:
---

# 异常处理规范

## 📋 概述

WalkBG项目采用统一的异常处理机制，确保所有API响应格式一致，日志记录规范，错误信息用户友好。

## 🎯 核心原则

1. **统一响应格式**：所有API响应都使用 `ApiResponse<T>` 格式
2. **分层异常处理**：业务异常不打印堆栈，系统异常记录完整信息
3. **语义化异常**：使用具体的异常类型而不是通用的RuntimeException
4. **请求追踪**：每个异常都包含traceId便于问题定位

## 📂 相关文件

- 全局异常处理器：[GlobalExceptionHandler.kt](md:src/main/kotlin/org/example/common/exception/GlobalExceptionHandler.kt)
- 业务异常类：[BusinessException.kt](md:src/main/kotlin/org/example/common/exception/BusinessException.kt)
- 异常工具类：[ExceptionUtil.kt](md:src/main/kotlin/org/example/common/util/ExceptionUtil.kt)
- 统一响应格式：[ApiResponse.kt](md:src/main/kotlin/org/example/common/dto/ApiResponse.kt)
- 错误响应格式：[ErrorResponse.kt](md:src/main/kotlin/org/example/common/dto/ErrorResponse.kt)

## ✅ 最佳实践

### 1. 抛出业务异常
```kotlin
// ✅ 推荐：使用语义化的异常方法
throw BusinessException.notFound("用户不存在")
throw BusinessException.badRequest("参数格式错误")
throw BusinessException.conflict("用户名已存在")

// ✅ 推荐：使用工具类进行验证
ExceptionUtil.checkResourceExists(user, "用户", userId)
ExceptionUtil.checkDuplicate(exists, "用户", "email", email)
```

### 2. Controller层异常处理
```kotlin
// ✅ 推荐：让全局异常处理器处理
@GetMapping("/{id}")
fun getUserById(@PathVariable id: String): ResponseEntity<ApiResponse<User>> {
    val user = userService.findById(id) // 可能抛出BusinessException.notFound
    return ResponseUtil.success(user, "获取用户成功")
}

// ✅ 推荐：使用工具类进行参数验证
@PostMapping
fun createUser(@Valid @RequestBody request: CreateUserRequest): ResponseEntity<ApiResponse<User>> {
    ExceptionUtil.validateEmail(request.email)
    ExceptionUtil.checkDuplicate(userService.existsByEmail(request.email), "用户", "email", request.email)

    val user = userService.createUser(request)
    return ResponseUtil.created(user, "用户创建成功")
}
```

### 3. Service层异常处理
```kotlin
// ✅ 推荐：使用ExceptionUtil进行资源检查
fun findById(id: String): User {
    return ExceptionUtil.checkResourceExists(
        userRepository.findById(id).orElse(null),
        "用户", id
    )
}

// ✅ 推荐：使用safeExecute包装可能出错的操作
fun updateUser(id: String, request: UpdateUserRequest): User {
    val user = findById(id)

    return ExceptionUtil.safeExecute(
        operation = {
            user.apply {
                name = request.name
                email = request.email
            }
            userRepository.save(user)
        },
        errorMessage = "用户更新失败"
    )
}
```

## ❌ 避免的做法

```kotlin
// ❌ 避免：直接抛出通用异常
throw RuntimeException("用户不存在")
throw IllegalArgumentException("参数错误")

// ❌ 避免：在Controller中手动处理异常
@GetMapping("/{id}")
fun getUserById(@PathVariable id: String): ResponseEntity<*> {
    return try {
        val user = userService.findById(id)
        ResponseEntity.ok(user)
    } catch (e: Exception) {
        ResponseEntity.badRequest().body("用户不存在")
    }
}

// ❌ 避免：不一致的错误响应格式
return ResponseEntity.notFound().build()
return ResponseEntity.badRequest().body("错误信息")
```

## 🔍 检查清单

在编写代码时，请检查：

- [ ] 是否使用了BusinessException的语义化方法？
- [ ] 是否让全局异常处理器统一处理异常？
- [ ] 是否使用了ExceptionUtil工具类进行验证？
- [ ] 是否所有API都返回统一的ApiResponse格式？
- [ ] 是否避免了在Controller中手动处理异常？
- [ ] 是否为业务异常提供了用户友好的错误信息？

## 📊 异常响应格式

所有异常最终都会转换为统一的响应格式：

```json
{
  "success": false,
  "message": "用户不存在",
  "code": 404,
  "data": {
    "errorCode": "USER_NOT_FOUND",
    "details": {
      "resource": "用户",
      "id": "12345"
    },
    "path": "/api/users/12345",
    "traceId": "a1b2c3d4e5f6g7h8"
  },
  "timestamp": "2024-01-15T10:30:00Z"
}
```

## 🎯 常用错误码

| 场景 | 错误码 | HTTP状态码 | 使用方法 |
|------|--------|-----------|---------|
| 资源不存在 | RESOURCE_NOT_FOUND | 404 | `BusinessException.resourceNotFound("用户", id)` |
| 参数错误 | BAD_REQUEST | 400 | `BusinessException.badRequest("参数格式错误")` |
| 资源冲突 | DUPLICATE_ERROR | 409 | `BusinessException.duplicate("用户", "email", email)` |
| 权限不足 | ACCESS_DENIED | 403 | `BusinessException.forbidden("权限不足")` |
| 系统错误 | INTERNAL_ERROR | 500 | `BusinessException.internalError("操作失败")` |
