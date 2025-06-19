---
ruleType: Constraint
description: Controller层开发标准和REST API设计规范
keywords: [Controller, REST API, HTTP, 请求处理, 响应格式]
priority: CRITICAL
---

<!--
=== Controller层职责 ===
Controller层负责处理HTTP请求，参数验证，调用Service层业务逻辑，
返回统一格式的响应。不应包含业务逻辑。
-->

# Controller层开发标准

## 🎯 强制性约束
```
MUST_USE: ResponseUtil返回统一响应格式
MUST_ANNOTATE: @RestController, @RequestMapping, @Validated
MUST_DELEGATE: 业务逻辑委托给Service层
NEVER_HANDLE: 业务逻辑处理
NEVER_CATCH: 手动捕获和处理异常
RESPONSE_FORMAT: 统一使用ApiResponse<T>
```

## 💻 标准Controller模式
```kotlin
// ✅ 标准Controller结构
@RestController
@RequestMapping("/api/routes")
@Tag(name = "路线管理", description = "路线相关API")
@Validated
class RouteController(
    private val routeService: RouteService
) {

    @GetMapping("/{id}")
    @Operation(summary = "获取路线详情")
    fun getRouteById(
        @Parameter(description = "路线ID") @PathVariable id: String
    ): ResponseEntity<ApiResponse<Route>> {
        val route = routeService.findById(id)
        return ResponseUtil.success(route, "获取路线成功")
    }

    @PostMapping
    @Operation(summary = "创建路线")
    fun createRoute(
        @Valid @RequestBody request: CreateRouteRequest
    ): ResponseEntity<ApiResponse<Route>> {
        val route = request.toEntity()
        val createdRoute = routeService.createRoute(route)
        return ResponseUtil.created(createdRoute, "路线创建成功")
    }

    @GetMapping
    @Operation(summary = "获取路线列表")
    fun getRoutes(
        @Valid @ModelAttribute request: BaseQueryRequest,
        @RequestParam(required = false) status: Int?
    ): ResponseEntity<ApiResponse<Page<Route>>> {
        val routes = routeService.findByStatus(status ?: 1, request.toPageable())
        return ResponseUtil.successPage(routes, "获取路线列表成功")
    }
}
```

## 🔍 参数验证模式
```kotlin
// ✅ 使用Bean Validation
data class CreateRouteRequest(
    @field:NotBlank(message = "路线名称不能为空")
    @field:Size(min = 2, max = 200, message = "路线名称长度必须在2-200字符之间")
    val name: String,

    @field:Size(max = 1000, message = "路线描述不能超过1000字符")
    val description: String?,

    @field:Min(value = 1, message = "难度等级最小为1")
    @field:Max(value = 5, message = "难度等级最大为5")
    val difficulty: Int?
) {
    fun toEntity(): Route {
        return Route(
            id = UUID.randomUUID().toString(),
            name = name,
            description = description,
            difficulty = difficulty,
            defaultMapId = ""
        )
    }
}

// ✅ 使用@Valid触发验证
@PostMapping
fun createRoute(@Valid @RequestBody request: CreateRouteRequest): ResponseEntity<ApiResponse<Route>> {
    // 验证失败会自动被全局异常处理器捕获
    val route = routeService.createRoute(request.toEntity())
    return ResponseUtil.created(route, "路线创建成功")
}
```

## 🔄 分页查询模式
```kotlin
// ✅ 使用BaseQueryRequest统一分页
@GetMapping
fun getRoutes(
    @Valid @ModelAttribute request: BaseQueryRequest,
    @RequestParam(required = false) region: String?,
    @RequestParam(required = false) difficulty: Int?
): ResponseEntity<ApiResponse<Page<Route>>> {
    val routes = routeService.findRoutes(region, difficulty, request.toPageable())
    return ResponseUtil.successPage(routes, "获取路线列表成功")
}
```

## 🚫 禁止的做法
```kotlin
// ❌ 禁止：直接返回实体
@GetMapping("/{id}")
fun getRoute(@PathVariable id: String): Route {
    return routeService.findById(id)  // 缺少统一响应格式
}

// ❌ 禁止：手动处理异常
@GetMapping("/{id}")
fun getRoute(@PathVariable id: String): ResponseEntity<*> {
    return try {
        val route = routeService.findById(id)
        ResponseEntity.ok(route)
    } catch (e: Exception) {
        ResponseEntity.badRequest().body("路线不存在")  // 应该让全局异常处理器处理
    }
}

// ❌ 禁止：包含业务逻辑
@PostMapping
fun createRoute(@RequestBody request: CreateRouteRequest): ResponseEntity<ApiResponse<Route>> {
    // 业务逻辑应该在Service层
    if (request.name.isBlank()) {
        throw BusinessException.badRequest("路线名称不能为空")
    }

    val route = routeService.createRoute(request.toEntity())
    return ResponseUtil.created(route, "路线创建成功")
}

// ❌ 禁止：自定义分页参数
@GetMapping
fun getRoutes(
    @RequestParam(defaultValue = "0") page: Int,
    @RequestParam(defaultValue = "10") size: Int
): ResponseEntity<ApiResponse<Page<Route>>> {
    // 应该使用BaseQueryRequest
}
```

## 📋 Controller开发检查清单
- [ ] 是否使用了@RestController和@RequestMapping？
- [ ] 是否添加了Swagger注解(@Tag, @Operation)？
- [ ] 是否使用ResponseUtil返回统一响应格式？
- [ ] 是否使用@Valid进行参数验证？
- [ ] 分页查询是否使用BaseQueryRequest？
- [ ] 是否避免了业务逻辑处理？
- [ ] 是否避免了手动异常处理？

## 🎯 HTTP方法使用规范
```
GET: 查询操作，幂等，无副作用
POST: 创建操作，非幂等
PUT: 完整更新操作，幂等
PATCH: 部分更新操作
DELETE: 删除操作，幂等
```

## 📊 响应状态码规范
```
200 OK: 查询成功
201 Created: 创建成功
400 Bad Request: 参数错误（自动处理）
404 Not Found: 资源不存在（自动处理）
409 Conflict: 资源冲突（自动处理）
500 Internal Server Error: 系统错误（自动处理）
```

<!--
=== REST API设计原则 ===
1. 使用名词而不是动词作为URL路径
2. 使用HTTP方法表示操作类型
3. 使用复数形式的资源名称
4. 保持URL层次结构清晰
5. 使用查询参数进行过滤和分页
-->
