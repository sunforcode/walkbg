---
ruleType: Domain
description: Route模块API开发规范和业务逻辑标准
keywords: [Route, 路线, ApplicationService, 复杂关联对象, 分层架构]
priority: HIGH
---

# Route模块API开发规范

## 🎯 Route模块特殊约束

```
MUST_USE: RouteApplicationService 进行业务编排
MUST_SEPARATE: 简单关联对象 vs 复杂关联对象
MUST_VALIDATE: 路线创建的业务规则验证
MUST_HANDLE: 外键依赖的创建顺序
TRANSACTION: 复杂操作必须使用事务管理
ID_GENERATION: 统一使用IdGenerator生成带前缀的ID
```

## 🏗️ Route API架构模式

### **分层职责划分**
```kotlin
// Controller层 - HTTP接口处理
@RestController
class RouteController {
    // 只处理HTTP请求响应，委托给ApplicationService
}

// ApplicationService层 - 业务用例编排
@Service
class RouteApplicationService {
    // 业务用例编排、跨领域协调、DTO转换
    // 复杂关联对象的创建协调
}

// DomainService层 - 领域业务逻辑
@Service
class RouteService {
    // 业务规则验证、领域逻辑处理
}

// Repository层 - 数据访问
interface RouteRepository {
    // 纯数据访问，不包含业务逻辑
}
```

## 📋 Route API设计规范

### **标准Route API结构**
```kotlin
@RestController
@RequestMapping("/api/v1/routes")
@Tag(name = "路线管理", description = "路线相关的API接口")
@Validated
class RouteController(
    private val routeApplicationService: RouteApplicationService
) {

    // 基础CRUD操作
    @GetMapping
    fun getRoutes(/* 分页和搜索参数 */) { }

    @GetMapping("/{id}")
    fun getRouteById(@PathVariable id: String, @RequestParam(required = false) userId: String?) { }

    @PostMapping
    fun createRoute(@RequestBody @Valid request: RouteCreateRequest) { }

    @PutMapping("/{id}")
    fun updateRoute(@PathVariable id: String, @RequestBody @Valid request: RouteCreateRequest) { }

    @DeleteMapping("/{id}")
    fun deleteRoute(@PathVariable id: String) { }

    // 路线子资源操作
    @PostMapping("/{id}/favorite")
    fun favoriteRoute(@PathVariable id: String, @RequestParam userId: String) { }

    @DeleteMapping("/{id}/favorite")
    fun unfavoriteRoute(@PathVariable id: String, @RequestParam userId: String) { }

    @PostMapping("/{id}/complete")
    fun completeRoute(@PathVariable id: String, @RequestParam userId: String) { }
}
```

## 🔧 复杂关联对象处理模式

### **分阶段创建策略**
```kotlin
@Transactional
fun createCompleteRoute(request: RouteCreateRequest): RouteBasicResponse {
    // 1. 业务规则验证
    routeService.validateCompleteRouteCreation(request)

    // 2. 构建简单关联对象（无外键依赖）
    val route = buildRouteWithSimpleAssociations(request)

    // 3. 第一次保存（保存主体和简单关联对象）
    val savedRoute = routeService.createRouteWithValidation(route)

    // 4. 创建复杂关联对象（依赖已保存的对象）
    createComplexAssociationsAfterSave(savedRoute, request)

    // 5. 第二次保存（包含复杂关联对象）
    val finalRoute = routeService.updateRoute(savedRoute)

    // 6. DTO转换
    return RouteBasicResponse.fromRoute(finalRoute)
}
```

### **关联对象分类处理**
```kotlin
// ✅ 简单关联对象（无外键依赖）
private fun createSimpleAssociations(route: Route, request: RouteCreateRequest) {
    // 标签 - 直接关联到Route
    request.tags.forEach { route.addTag(it) }

    // 路点 - 只依赖Route，无其他外键
    request.waypoints.forEach { waypointRequest ->
        val waypoint = Waypoint(
            id = IdGenerator.generateIdWithPrefix("waypoint"),
            // ... 其他属性
        )
        waypoint.route = route
        route.waypoints.add(waypoint)
    }

    // 其他简单关联对象...
}

// ✅ 复杂关联对象（有外键依赖）
private fun createComplexAssociationsAfterSave(route: Route, request: RouteCreateRequest) {
    // 路段 - 依赖已保存的Waypoint
    val waypointMap = route.waypoints.associateBy { it.sequenceNumber }

    request.segments.forEachIndexed { index, segmentRequest ->
        val segment = Segment(
            id = IdGenerator.generateIdWithPrefix("segment"),
            startPoint = waypointMap[index + 1],  // 外键依赖
            endPoint = waypointMap[index + 2],    // 外键依赖
            // ... 其他属性
        )
        route.addSegment(segment)
    }
}
```

## 🔍 Route业务规则验证

### **创建前验证**
```kotlin
fun validateCompleteRouteCreation(request: RouteCreateRequest) {
    // 基础字段验证
    if (request.name.isBlank()) {
        throw IllegalArgumentException("路线名称不能为空")
    }

    // 业务规则验证
    if (request.difficulty != null && (request.difficulty < 1 || request.difficulty > 5)) {
        throw IllegalArgumentException("难度等级必须在1-5之间")
    }

    // 关联对象一致性验证
    if (request.segments.isNotEmpty() && request.waypoints.size < 2) {
        throw IllegalArgumentException("有路段时至少需要2个路点")
    }

    // 区域内同名路线检查
    if (isRouteNameExistsInRegion(request.name, request.region)) {
        throw IllegalArgumentException("该区域已存在同名路线")
    }
}
```

### **访问权限检查**
```kotlin
fun getRouteWithAccessCheck(routeId: String, userId: String?): Route? {
    val route = getRouteById(routeId) ?: return null

    return when (route.status) {
        0 -> if (route.createdBy == userId) route else null  // 规划中只有创建者可见
        1 -> route  // 已发布所有人可见
        2 -> null   // 已关闭不可见
        else -> null
    }
}
```

## 📊 Route特有的响应处理

### **分页搜索响应**
```kotlin
@GetMapping
fun getRoutes(
    @Parameter(description = "关键词搜索") @RequestParam(required = false) keyword: String?,
    @Parameter(description = "区域ID") @RequestParam(required = false) regionId: String?,
    @Parameter(description = "难度等级") @RequestParam(required = false) difficulty: Int?,
    @Parameter(description = "路线类型") @RequestParam(required = false) routeType: Int?,
    @Parameter(description = "最小距离") @RequestParam(required = false) minDistance: Double?,
    @Parameter(description = "最大距离") @RequestParam(required = false) maxDistance: Double?,
    @Parameter(description = "用户ID") @RequestParam(required = false) userId: String?,
    @Parameter(description = "页码") @RequestParam(defaultValue = "0") page: Int,
    @Parameter(description = "每页大小") @RequestParam(defaultValue = "10") size: Int
): ResponseEntity<ApiResponse<Page<RouteBasicResponse>>> {
    return try {
        val pageable = PageRequest.of(page, size)
        val routes = routeApplicationService.searchRoutes(
            keyword, regionId, difficulty, routeType,
            minDistance, maxDistance, userId, pageable
        )
        ResponseUtil.successPage(routes)
    } catch (e: Exception) {
        ResponseUtil.error("查询路线列表失败: ${e.message}")
    }
}
```

### **详情查询响应**
```kotlin
@GetMapping("/{id}")
fun getRouteById(
    @Parameter(description = "路线ID") @PathVariable id: String,
    @Parameter(description = "用户ID") @RequestParam(required = false) userId: String?
): ResponseEntity<ApiResponse<RouteBasicResponse?>> {
    return try {
        val route = routeApplicationService.getRouteDetails(id, userId)
        if (route == null) {
            ResponseUtil.error("路线不存在")
        } else {
            ResponseUtil.success(route)
        }
    } catch (e: Exception) {
        ResponseUtil.error("查询路线详情失败: ${e.message}")
    }
}
```

## 🔧 ID生成规范

### **统一ID前缀**
```kotlin
// Route相关实体ID前缀
route_xxx      // 路线主体
waypoint_xxx   // 路点
segment_xxx    // 路段
supply_xxx     // 补给点
campsite_xxx   // 营地
marker_xxx     // 标记点
plan_xxx       // 日程计划
water_xxx      // 水源
contact_xxx    // 搭车联系人

// 使用方式
val routeId = IdGenerator.generateIdWithPrefix("route")
val waypointId = IdGenerator.generateIdWithPrefix("waypoint")
```

## 🚫 Route模块禁止的做法

```kotlin
// ❌ 在Controller中直接处理复杂关联对象
@PostMapping
fun createRoute(@RequestBody request: RouteCreateRequest) {
    val route = Route(...)
    request.waypoints.forEach { ... }  // 不应该在Controller处理
    return ResponseEntity.ok(route)
}

// ❌ 不考虑外键依赖顺序
fun createRoute(request: RouteCreateRequest) {
    // 先创建Segment，但Waypoint还没保存
    val segment = Segment(startPoint = waypoint, ...)  // 会失败
    val waypoint = Waypoint(...)
}

// ❌ 不进行业务规则验证
fun createRoute(request: RouteCreateRequest) {
    val route = request.toRoute()
    routeRepository.save(route)  // 直接保存，没有验证
}
```

## ✅ Route模块推荐做法

```kotlin
// ✅ 标准Route创建流程
@Transactional
fun createCompleteRoute(request: RouteCreateRequest): RouteBasicResponse {
    // 1. 验证
    routeService.validateCompleteRouteCreation(request)

    // 2. 分阶段构建
    val route = buildRouteWithSimpleAssociations(request)
    val savedRoute = routeService.createRouteWithValidation(route)
    createComplexAssociationsAfterSave(savedRoute, request)
    val finalRoute = routeService.updateRoute(savedRoute)

    // 3. 转换返回
    return RouteBasicResponse.fromRoute(finalRoute)
}

// ✅ 正确的关联对象处理
private fun createSimpleAssociations(route: Route, request: RouteCreateRequest) {
    request.waypoints.forEach { waypointRequest ->
        val waypoint = Waypoint(
            id = IdGenerator.generateIdWithPrefix("waypoint"),
            name = waypointRequest.name,
            // ... 设置属性
        )
        waypoint.route = route  // 设置关联
        route.waypoints.add(waypoint)  // 双向关联
    }
}
```

## 📋 Route开发检查清单

- [ ] 是否使用了RouteApplicationService进行业务编排？
- [ ] 是否正确分离了简单和复杂关联对象？
- [ ] 是否进行了完整的业务规则验证？
- [ ] 是否考虑了外键依赖的创建顺序？
- [ ] 是否使用了统一的ID生成策略？
- [ ] 是否正确处理了访问权限检查？
- [ ] 是否使用了事务管理？
- [ ] 是否进行了适当的DTO转换？

## 🎯 Route性能优化建议

### **批量操作优化**
```kotlin
// ✅ 批量创建关联对象
route.waypoints.addAll(waypoints)  // 批量添加
routeRepository.save(route)        // 一次性保存

// ❌ 逐个保存
waypoints.forEach { waypointRepository.save(it) }  // 多次数据库操作
```

### **懒加载配置**
```kotlin
// 根据使用场景配置合适的加载策略
@OneToMany(fetch = FetchType.LAZY)  // 大集合使用懒加载
val waypoints: MutableList<Waypoint>

@ManyToOne(fetch = FetchType.EAGER) // 小对象使用急加载
val creator: User
