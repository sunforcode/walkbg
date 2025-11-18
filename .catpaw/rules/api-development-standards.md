---
ruleType: Design
description: API接口设计规范和RESTful URL设计标准
keywords: [API设计, RESTful, URL设计, HTTP方法]
priority: HIGH
---

# API接口设计规范

## 🎯 API设计原则

```
FOCUS: RESTful URL设计和HTTP方法使用
SCOPE: API接口的对外设计，不涉及内部实现
GOAL: 统一的API设计风格和用户体验
```

## 📋 RESTful URL设计规范

### **资源命名规则**
```
✅ 正确示例:
GET    /api/v1/routes              # 路线列表
GET    /api/v1/routes/{id}         # 路线详情
POST   /api/v1/routes              # 创建路线
PUT    /api/v1/routes/{id}         # 更新路线
DELETE /api/v1/routes/{id}         # 删除路线

❌ 错误示例:
GET    /api/v1/getRoutes           # 动词形式
POST   /api/v1/route/create        # 混合单复数
GET    /api/v1/hiking-routes       # 过于具体
```

### **子资源设计**
```
✅ 正确示例:
POST   /api/v1/routes/{id}/favorite    # 收藏路线
DELETE /api/v1/routes/{id}/favorite    # 取消收藏
GET    /api/v1/routes/{id}/waypoints   # 路线的路点
GET    /api/v1/users/{id}/routes       # 用户的路线

❌ 错误示例:
POST   /api/v1/favorite/routes/{id}    # 资源层次颠倒
GET    /api/v1/route-waypoints         # 平铺结构
```

## 🔧 HTTP方法使用规范

| 方法 | 用途 | 幂等性 | 安全性 | 示例 |
|------|------|--------|--------|------|
| GET | 查询资源 | ✅ | ✅ | 获取列表/详情 |
| POST | 创建资源/非幂等操作 | ❌ | ❌ | 创建/收藏/完成 |
| PUT | 完整更新资源 | ✅ | ❌ | 更新整个资源 |
| PATCH | 部分更新资源 | ❌ | ❌ | 更新部分字段 |
| DELETE | 删除资源 | ✅ | ❌ | 删除资源 |

## 📊 响应格式标准

### **统一响应结构**
```kotlin
// 成功响应
{
  "success": true,
  "message": "操作成功",
  "data": { ... },
  "timestamp": 1750394007,
  "code": 200
}

// 错误响应
{
  "success": false,
  "message": "错误描述",
  "data": null,
  "timestamp": 1750394007,
  "code": 400
}
```

### **ResponseUtil使用规范**
```kotlin
// ✅ 正确使用
return ResponseUtil.success(data, "操作成功")
return ResponseUtil.successPage(pageData)
return ResponseUtil.error("错误信息")
return ResponseUtil.conditional(data, "成功", "不存在")

// ❌ 错误使用
return ResponseEntity.ok(data)  // 不统一
throw new RuntimeException()    // 不处理异常
```

## 🔍 参数验证规范

### **请求参数验证**
```kotlin
// 路径参数
@PathVariable @NotBlank id: String

// 查询参数
@RequestParam(defaultValue = "0") @Min(0) page: Int
@RequestParam(required = false) @Size(max = 100) keyword: String?

// 请求体验证
@RequestBody @Valid request: CreateRequest
```

### **DTO验证注解**
```kotlin
data class CreateRequest(
    @field:NotBlank(message = "名称不能为空")
    @field:Size(max = 200, message = "名称长度不能超过200字符")
    val name: String,

    @field:Min(value = 1, message = "难度等级最小为1")
    @field:Max(value = 5, message = "难度等级最大为5")
    val difficulty: Int?
)
```

## 📝 文档注解规范

### **Swagger注解使用**
```kotlin
@Tag(name = "资源管理", description = "资源相关的API接口")
class ResourceController {

    @Operation(summary = "创建资源", description = "创建新的资源实例")
    @Parameter(description = "资源ID", example = "resource_123")
    fun createResource() { ... }
}
```

## 🚫 禁止的做法

```kotlin
// ❌ 在Controller中处理业务逻辑
@PostMapping
fun createRoute(@RequestBody request: RouteRequest) {
    val route = Route(...)  // 直接构建实体
    routeRepository.save(route)  // 直接调用Repository
    return ResponseEntity.ok(route)
}

// ❌ 不统一的异常处理
@GetMapping("/{id}")
fun getRoute(@PathVariable id: String) {
    try {
        // ...
    } catch (Exception e) {
        return ResponseEntity.badRequest().body(e.message)
    }
}

// ❌ 不规范的URL设计
@PostMapping("/createRoute")  // 动词形式
@GetMapping("/route-list")    // 不一致命名
```

## ✅ 推荐的做法

```kotlin
// ✅ 标准Controller实现
@RestController
@RequestMapping("/api/v1/routes")
@Tag(name = "路线管理")
@Validated
class RouteController(
    private val routeApplicationService: RouteApplicationService
) {

    @PostMapping
    @Operation(summary = "创建路线")
    fun createRoute(@RequestBody @Valid request: RouteCreateRequest) {
        return try {
            val route = routeApplicationService.createRoute(request)
            ResponseUtil.success(route, "创建成功")
        } catch (e: Exception) {
            ResponseUtil.error("创建失败: ${e.message}")
        }
    }
}
```

## 📋 开发检查清单

- [ ] 是否遵循RESTful URL设计？
- [ ] 是否使用了正确的HTTP方法？
- [ ] 是否添加了完整的Swagger注解？
- [ ] 是否使用了统一的响应格式？
- [ ] 是否进行了参数验证？
- [ ] 是否避免了业务逻辑处理？
- [ ] 是否使用了事务注解？
- [ ] 是否有适当的异常处理？

## 🎯 性能和安全考虑

### **分页查询**
```kotlin
// 强制分页，防止大数据量查询
@RequestParam(defaultValue = "0") page: Int,
@RequestParam(defaultValue = "10") @Max(100) size: Int
```

### **参数校验**
```kotlin
// 防止恶意输入
@Size(max = 1000) keyword: String?,
@Min(0) @Max(100) page: Int
```

### **访问控制**
```kotlin
// 用户权限检查
@Parameter(description = "用户ID") userId: String?
