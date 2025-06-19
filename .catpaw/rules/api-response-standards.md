---
ruleType: Manual
description: WalkBG项目API响应格式标准和最佳实践
globs:
---

# API响应标准

## 📋 概述

WalkBG项目采用统一的API响应格式，确保前后端交互的一致性和可预测性。所有API都应该返回标准化的响应结构。

## 🎯 核心原则

1. **统一响应格式**：所有API都使用 `ApiResponse<T>` 包装
2. **一致的字段命名**：使用 `success`、`message`、`code`、`data`、`timestamp`
3. **语义化状态码**：HTTP状态码与业务状态保持一致
4. **用户友好消息**：提供清晰的成功/失败信息

## 📂 相关文件

- 统一响应格式：[ApiResponse.kt](md:src/main/kotlin/org/example/common/dto/ApiResponse.kt)
- 响应工具类：[ResponseUtil.kt](md:src/main/kotlin/org/example/common/util/ResponseUtil.kt)
- 错误响应格式：[ErrorResponse.kt](md:src/main/kotlin/org/example/common/dto/ErrorResponse.kt)
- 分页查询基类：[BaseQueryRequest.kt](md:src/main/kotlin/org/example/common/dto/BaseQueryRequest.kt)

## ✅ 最佳实践

### 1. 成功响应
```kotlin
// ✅ 推荐：使用ResponseUtil工具类
@GetMapping("/{id}")
fun getUserById(@PathVariable id: String): ResponseEntity<ApiResponse<User>> {
    val user = userService.findById(id)
    return ResponseUtil.success(user, "获取用户成功")
}

// ✅ 推荐：分页响应
@GetMapping
fun getUsers(@Valid @ModelAttribute request: BaseQueryRequest): ResponseEntity<ApiResponse<Page<User>>> {
    val users = userService.getAllUsers(request.toPageable())
    return ResponseUtil.successPage(users, "获取用户列表成功")
}

// ✅ 推荐：创建响应
@PostMapping
fun createUser(@Valid @RequestBody user: User): ResponseEntity<ApiResponse<User>> {
    val createdUser = userService.createUser(user)
    return ResponseUtil.created(createdUser, "用户创建成功")
}
```

### 2. 条件响应
```kotlin
// ✅ 推荐：使用conditional方法处理可能为空的情况
@GetMapping("/{id}")
fun getUserById(@PathVariable id: String): ResponseEntity<ApiResponse<User>> {
    val user = userService.findById(id)
    return ResponseUtil.conditional(
        data = user,
        successMessage = "获取用户成功",
        notFoundMessage = "用户不存在"
    )
}
```

### 3. 分页查询
```kotlin
// ✅ 推荐：使用BaseQueryRequest统一分页参数
@GetMapping
fun getRoutes(
    @Valid @ModelAttribute request: BaseQueryRequest,
    @RequestParam(required = false) region: String?
): ResponseEntity<ApiResponse<Page<Route>>> {
    val routes = routeService.findByRegion(region, request.toPageable())
    return ResponseUtil.successPage(routes, "获取路线列表成功")
}
```

## ❌ 避免的做法

```kotlin
// ❌ 避免：直接返回原始数据
@GetMapping("/{id}")
fun getUserById(@PathVariable id: String): ResponseEntity<User> {
    val user = userService.findById(id)
    return ResponseEntity.ok(user)
}

// ❌ 避免：不一致的响应格式
@GetMapping("/{id}")
fun getUserById(@PathVariable id: String): ResponseEntity<*> {
    val user = userService.findById(id)
    return if (user != null) {
        ResponseEntity.ok(user)
    } else {
        ResponseEntity.notFound().build()
    }
}

// ❌ 避免：手动构造响应
@GetMapping("/{id}")
fun getUserById(@PathVariable id: String): ResponseEntity<Map<String, Any>> {
    val user = userService.findById(id)
    val response = mapOf(
        "success" to true,
        "data" to user,
        "message" to "成功"
    )
    return ResponseEntity.ok(response)
}
```

## 📊 标准响应格式

### 成功响应
```json
{
  "success": true,
  "message": "获取用户成功",
  "code": 200,
  "data": {
    "id": "12345",
    "name": "张三",
    "email": "zhangsan@example.com"
  },
  "timestamp": "2024-01-15T10:30:00Z"
}
```

### 分页响应
```json
{
  "success": true,
  "message": "获取用户列表成功",
  "code": 200,
  "data": {
    "content": [...],
    "pageable": {
      "pageNumber": 0,
      "pageSize": 10
    },
    "totalElements": 100,
    "totalPages": 10
  },
  "timestamp": "2024-01-15T10:30:00Z"
}
```

### 错误响应
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

## 🔍 检查清单

在编写API时，请检查：

- [ ] 是否所有API都返回 `ApiResponse<T>` 格式？
- [ ] 是否使用了 `ResponseUtil` 工具类？
- [ ] 是否提供了用户友好的成功/失败消息？
- [ ] 是否使用了正确的HTTP状态码？
- [ ] 分页查询是否使用了 `BaseQueryRequest`？
- [ ] 是否避免了直接返回原始数据类型？

## 🎯 ResponseUtil 方法速查

| 场景 | 方法 | 示例 |
|------|------|------|
| 成功响应 | `success()` | `ResponseUtil.success(user, "获取成功")` |
| 创建响应 | `created()` | `ResponseUtil.created(user, "创建成功")` |
| 分页响应 | `successPage()` | `ResponseUtil.successPage(page, "查询成功")` |
| 条件响应 | `conditional()` | `ResponseUtil.conditional(user, "成功", "不存在")` |
| 无数据响应 | `success()` | `ResponseUtil.success(null, "操作成功")` |

## 📈 HTTP状态码使用规范

| 状态码 | 场景 | 使用方法 |
|--------|------|---------|
| 200 | 查询成功 | `ResponseUtil.success()` |
| 201 | 创建成功 | `ResponseUtil.created()` |
| 400 | 参数错误 | 由全局异常处理器处理 |
| 404 | 资源不存在 | 由全局异常处理器处理 |
| 409 | 资源冲突 | 由全局异常处理器处理 |
| 500 | 系统错误 | 由全局异常处理器处理 |
