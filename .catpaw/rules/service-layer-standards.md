---
ruleType: Constraint
description: Service层开发标准和最佳实践
keywords: [Service, 服务层, 业务逻辑, 事务管理, 接口设计]
priority: CRITICAL
---

<!--
=== Service层设计理念 ===
Service层是业务逻辑的核心，负责处理复杂的业务规则和流程。
必须保持清晰的职责分离和统一的设计模式。
-->

# Service层开发标准

## 🎯 强制性约束
```
MUST_USE: 接口 + 实现类模式
MUST_ANNOTATE: @Service, @Transactional
MUST_VALIDATE: 使用ExceptionUtil进行参数验证
NEVER_HANDLE: HTTP相关逻辑 (属于Controller层)
NEVER_DIRECT: 直接操作HttpServletRequest/Response
```

## 💻 标准Service模式
```kotlin
// ✅ 标准接口定义
interface RouteService {
    fun findById(id: String): Route
    fun createRoute(route: Route): Route
    fun updateRoute(id: String, route: Route): Route
    fun deleteRoute(id: String)
    fun findByStatus(status: Int, pageable: Pageable): Page<Route>
}

// ✅ 标准实现类
@Service
@Transactional
class RouteServiceImpl(
    private val routeRepository: RouteRepository
) : RouteService {

    override fun findById(id: String): Route {
        return ExceptionUtil.checkResourceExists(
            routeRepository.findById(id).orElse(null),
            "路线", id
        )
    }

    override fun createRoute(route: Route): Route {
        // 业务验证
        ExceptionUtil.assertNotBlank(route.name, "路线名称不能为空")
        ExceptionUtil.assertTrue(route.name.length <= 200, "路线名称不能超过200字符")

        return routeRepository.save(route)
    }
}
```

## 🔍 业务验证模式
```kotlin
// ✅ 使用ExceptionUtil进行验证
fun createRoute(route: Route): Route {
    // 参数验证
    ExceptionUtil.assertNotNull(route, "路线对象不能为空")
    ExceptionUtil.assertNotBlank(route.name, "路线名称不能为空")

    // 业务规则验证
    ExceptionUtil.checkDuplicate(
        routeRepository.existsByName(route.name),
        "路线", "name", route.name
    )

    // 状态验证
    ExceptionUtil.assertTrue(
        route.status in 0..2,
        "路线状态必须在0-2之间"
    )

    return routeRepository.save(route)
}
```

## 🚫 禁止的做法
```kotlin
// ❌ 禁止：直接使用Service类
@Service
class RouteService { ... }  // 必须有接口

// ❌ 禁止：处理HTTP逻辑
@Service
class RouteService {
    fun getRoute(request: HttpServletRequest): ResponseEntity<Route> { ... }
}

// ❌ 禁止：手动异常处理
@Service
class RouteService {
    fun findById(id: String): Route? {
        return try {
            routeRepository.findById(id).orElse(null)
        } catch (e: Exception) {
            null  // 应该让异常向上传播
        }
    }
}
```

## 📋 Service开发检查清单
- [ ] 是否定义了Service接口？
- [ ] 实现类是否以Impl结尾？
- [ ] 是否添加了@Service和@Transactional注解？
- [ ] 是否使用ExceptionUtil进行参数验证？
- [ ] 是否避免了HTTP相关逻辑？
- [ ] 事务边界是否合理？

<!--
=== 事务管理说明 ===
@Transactional默认在RuntimeException时回滚
对于业务异常，确保继承RuntimeException
复杂业务流程考虑拆分多个事务方法
-->
