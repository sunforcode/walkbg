# WalkBG 开发规范
## 📁 文件存放规范

### 目录结构

```
src/main/kotlin/org/example/
├── common/                    # 通用组件
│   ├── dto/                  # 通用DTO（ApiResponse、ErrorResponse等）
│   ├── enums/                # 通用枚举
│   ├── exception/            # 全局异常处理
│   └── util/                 # 工具类
├── config/                   # 配置类（数据库、Web、Jackson等）
└── [业务模块]/               # 业务模块（route、user、trip等）
    ├── controller/           # 控制器层
    ├── service/              # 服务层
    ├── repository/           # 数据访问层
    ├── model/                # 领域模型（JPA实体）
    └── dto/                  # 数据传输对象
```

### 命名规范

| 类型 | 命名规则 | 示例 |
|------|---------|------|
| Controller | `*Controller.kt` | `RouteController.kt` |
| ApplicationService | `*ApplicationService.kt` | `RouteApplicationService.kt` |
| DomainService | `*Service.kt`, `*ServiceImpl.kt` | `RouteService.kt` |
| Repository | `*Repository.kt` | `RouteRepository.kt` |
| Entity | 实体名称 | `Route.kt`, `User.kt` |
| 请求DTO | `*CreateRequest.kt`, `*UpdateRequest.kt` | `RouteCreateRequest.kt` |
| 响应DTO | `*Response.kt`, `*Dto.kt` | `RouteBasicResponse.kt` |

---

## 🏗️ DDD 分层架构

### 分层职责

```
Controller → ApplicationService → DomainService → Repository → Entity
```

| 层级 | 职责 | 禁止事项 |
|------|------|---------|
| **Controller** | HTTP请求/响应、参数验证、调用ApplicationService | ❌ 业务逻辑、直接访问Repository |
| **ApplicationService** | 业务用例编排、跨领域协调、DTO转换 | ❌ 直接访问Repository |
| **DomainService** | 领域业务逻辑、数据访问、业务规则 | ❌ HTTP相关处理 |
| **Repository** | 数据持久化 | ❌ 业务逻辑 |
| **Entity** | 领域模型、实体行为 | ❌ 外部依赖 |

### 代码示例

```kotlin
// Controller - 只处理HTTP
@RestController
@RequestMapping("/api/v1/routes")
class RouteController(
    private val routeApplicationService: RouteApplicationService
) {
    @GetMapping("/{id}")
    fun getRoute(@PathVariable id: String): ResponseEntity<ApiResponse<RouteDetailResponse>> {
        val route = routeApplicationService.getRouteFullDetails(id)
        return ResponseUtil.success(route)
    }
}

// ApplicationService - 业务用例编排
@Service
class RouteApplicationService(
    private val routeService: RouteService
) {
    @Transactional(readOnly = true)
    fun getRouteFullDetails(routeId: String): RouteDetailResponse? {
        val route = routeService.getRouteById(routeId) ?: return null
        return RouteDetailResponse.fromRoute(route)
    }
}

// DomainService - 领域逻辑
@Service
class RouteServiceImpl(
    private val routeRepository: RouteRepository
) : RouteService {
    override fun getRouteById(id: String): Route? {
        return routeRepository.findById(id).orElse(null)
    }
}
```

---

## 🔌 RESTful API 规范

### URL 设计

```
基础路径: /walkbg
API路径: /api/v{version}/{resource}

示例:
GET    /api/v1/routes           # 查询列表
GET    /api/v1/routes/{id}      # 查询详情
POST   /api/v1/routes           # 创建
PUT    /api/v1/routes/{id}      # 更新
DELETE /api/v1/routes/{id}      # 删除
```

### HTTP 方法

| 方法 | 用途 | 幂等性 |
|------|------|--------|
| GET | 查询 | ✅ |
| POST | 创建 | ❌ |
| PUT | 更新（全量） | ✅ |
| PATCH | 更新（部分） | ❌ |
| DELETE | 删除 | ✅ |

### 统一响应格式

#### 成功响应
```json
{
  "success": true,
  "message": "操作成功",
  "data": { ... },
  "timestamp": "2025-11-18T10:30:00Z",
  "code": 200
}
```

#### 错误响应
```json
{
  "success": false,
  "message": "错误描述",
  "data": {
    "errorCode": "BUSINESS_ERROR_CODE",
    "details": { ... },
    "path": "/api/v1/routes",
    "traceId": "abc123"
  },
  "timestamp": "2025-11-18T10:30:00Z",
  "code": 400
}
```

#### 分页响应
```json
{
  "success": true,
  "message": "查询成功",
  "data": {
    "content": [ ... ],
    "totalElements": 100,
    "totalPages": 10,
    "pageNumber": 0,
    "pageSize": 10
  },
  "code": 200
}
```

### HTTP 状态码

| 状态码 | 含义 | 使用场景 |
|--------|------|----------|
| 200 | OK | 成功 |
| 201 | Created | 创建成功 |
| 400 | Bad Request | 参数错误 |
| 401 | Unauthorized | 未认证 |
| 403 | Forbidden | 无权限 |
| 404 | Not Found | 资源不存在 |
| 409 | Conflict | 数据冲突 |
| 500 | Internal Server Error | 服务器错误 |

---

## 📝 代码规范

### Controller 规范

```kotlin
@RestController
@RequestMapping("/api/v1/routes")
@Tag(name = "路线管理", description = "路线相关的API接口")
@Validated
class RouteController(
    private val routeApplicationService: RouteApplicationService
) {
    
    @GetMapping
    @Operation(summary = "分页查询路线列表")
    fun getRoutes(
        @Parameter(description = "页码") @RequestParam(defaultValue = "0") page: Int,
        @Parameter(description = "每页大小") @RequestParam(defaultValue = "10") size: Int
    ): ResponseEntity<ApiResponse<Page<RouteBasicResponse>>> {
        return try {
            val pageable = PageRequest.of(page, size)
            val routes = routeApplicationService.searchRoutes(pageable)
            ResponseUtil.successPage(routes)
        } catch (e: Exception) {
            ResponseUtil.error("查询失败: ${e.message}")
        }
    }
}
```

**要点**:
- 使用 `@RestController` + `@RequestMapping`
- 添加 Swagger 注解（`@Tag`, `@Operation`, `@Parameter`）
- 使用 `@Validated` 启用参数验证
- 统一使用 `ResponseUtil` 返回响应
- 异常统一捕获处理

### DTO 规范

```kotlin
// 请求DTO - 使用验证注解
data class RouteCreateRequest(
    @field:NotBlank(message = "路线名称不能为空")
    @field:Size(max = 200, message = "长度不能超过200")
    val name: String,
    
    @field:DecimalMin(value = "0.0", message = "距离不能为负数")
    val distance: BigDecimal?,
    
    @JsonProperty("created_by")
    @field:NotBlank(message = "创建者ID不能为空")
    val createdBy: String
)

// 响应DTO - 提供转换方法
data class RouteBasicResponse(
    val id: String,
    val name: String,
    @JsonProperty("created_at")
    val createdAt: Long
) {
    companion object {
        fun fromRoute(route: Route): RouteBasicResponse {
            return RouteBasicResponse(
                id = route.id,
                name = route.name,
                createdAt = route.createdAt.epochSecond
            )
        }
    }
}
```

**要点**:
- 请求DTO使用 `@field:` 验证注解
- 使用 `@JsonProperty` 处理下划线命名
- 响应DTO提供 `fromEntity` 转换方法
- 时间统一使用时间戳（秒）

### Service 规范

```kotlin
// ApplicationService - 业务用例编排
@Service
class RouteApplicationService(
    private val routeService: RouteService
) {
    @Transactional(readOnly = true)
    fun getRouteFullDetails(routeId: String, userId: String?): RouteDetailResponse? {
        // 1. 获取数据
        val route = routeService.getRouteById(routeId) ?: return null
        
        // 2. 业务逻辑协调
        routeService.recordVisit(route, userId)
        
        // 3. DTO转换
        return RouteDetailResponse.fromRoute(route)
    }
}

// DomainService - 领域逻辑
interface RouteService {
    fun getRouteById(id: String): Route?
    fun recordVisit(route: Route, userId: String?)
}

@Service
class RouteServiceImpl(
    private val routeRepository: RouteRepository
) : RouteService {
    override fun getRouteById(id: String): Route? {
        return routeRepository.findById(id).orElse(null)
    }
}
```

**要点**:
- ApplicationService 负责用例编排和DTO转换
- DomainService 负责领域逻辑和数据访问
- 使用 `@Transactional` 管理事务
- 只读操作使用 `@Transactional(readOnly = true)`

### Entity 规范

```kotlin
@Entity
@Table(
    name = "routes",
    indexes = [
        Index(name = "idx_routes_region", columnList = "region_id"),
        Index(name = "idx_routes_created_by", columnList = "created_by")
    ]
)
data class Route(
    @Id
    @Column(length = 64)
    val id: String,
    
    @Column(nullable = false, length = 200)
    var name: String,
    
    @Column(name = "created_at", nullable = false, updatable = false)
    val createdAt: Instant = Instant.now(),
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "created_by", insertable = false, updatable = false)
    var creator: User? = null
) {
    // 业务方法
    fun incrementPopularity() {
        popularity += 1
    }
}
```

**要点**:
- 使用 `@Entity` + `@Table`
- 添加必要的索引
- 使用 `FetchType.LAZY` 懒加载
- 时间使用 `Instant` 类型
- 可以包含简单的业务方法

---

## 🔧 其他规范

### 异常处理

```kotlin
// 抛出业务异常
throw BusinessException("路线不存在", "ROUTE_NOT_FOUND", HttpStatus.NOT_FOUND)

// 全局异常处理器自动捕获并返回统一格式
```

### 事务管理

```kotlin
@Transactional(readOnly = true)   // 只读事务
@Transactional                     // 读写事务
@Transactional(rollbackFor = [Exception::class])  // 指定回滚异常
```

### 日志规范

```kotlin
private val logger = LoggerFactory.getLogger(RouteController::class.java)

logger.info("查询路线: id={}", routeId)
logger.warn("路线不存在: id={}", routeId)
logger.error("创建路线失败", exception)
```

### API 文档

- 使用 SpringDoc OpenAPI 3.0 自动生成
- 访问地址: `http://localhost:8080/walkbg/swagger-ui/index.html`
- 生成脚本: `./scripts/generate-api-docs.sh`

---

## ✅ 开发检查清单

### 新增 API
- [ ] URL 符合 RESTful 规范
- [ ] 使用统一的 `ApiResponse` 响应格式
- [ ] 添加参数验证注解
- [ ] 添加 Swagger 文档注解
- [ ] 实现异常处理
- [ ] 遵循分层架构

### 新增模块
- [ ] 创建标准目录结构（controller/service/repository/model/dto）
- [ ] 遵循命名规范
- [ ] 实现统一的响应格式
- [ ] 添加必要的索引

---

**最后更新**: 2025-11-18
