# WalkBG 代码重构计划

## 🎯 **重构目标**
- 提高代码可维护性和可读性
- 按业务领域组织代码结构
- 减少代码重复和耦合
- 统一命名规范和代码风格

## 📁 **新的目录结构**

\`\`\`
src/main/kotlin/org/example/
├── WalkbgApplication.kt
├── common/                           # 公共模块
│   ├── config/                      # 配置类
│   │   ├── DatabaseConfig.kt
│   │   ├── SwaggerConfig.kt
│   │   └── CorsConfig.kt
│   ├── exception/                   # 异常处理
│   │   ├── GlobalExceptionHandler.kt
│   │   ├── BusinessException.kt
│   │   └── ErrorResponse.kt
│   ├── util/                        # 工具类
│   │   ├── DateUtil.kt
│   │   ├── ValidationUtil.kt
│   │   └── ResponseUtil.kt
│   └── constant/                    # 常量定义
│       ├── StatusConstants.kt
│       └── MessageConstants.kt
├── user/                            # 用户管理模块
│   ├── controller/
│   │   └── UserController.kt
│   ├── service/
│   │   ├── UserService.kt
│   │   └── impl/UserServiceImpl.kt
│   ├── repository/
│   │   └── UserRepository.kt
│   ├── model/
│   │   └── User.kt
│   └── dto/
│       ├── UserDto.kt
│       ├── UserCreateRequest.kt
│       └── UserUpdateRequest.kt
├── route/                           # 路线管理模块
│   ├── controller/
│   │   ├── RouteController.kt
│   │   ├── WaypointController.kt
│   │   └── SegmentController.kt
│   ├── service/
│   │   ├── RouteService.kt
│   │   ├── WaypointService.kt
│   │   ├── SegmentService.kt
│   │   └── impl/
│   │       ├── RouteServiceImpl.kt
│   │       ├── WaypointServiceImpl.kt
│   │       └── SegmentServiceImpl.kt
│   ├── repository/
│   │   ├── RouteRepository.kt
│   │   ├── WaypointRepository.kt
│   │   └── SegmentRepository.kt
│   ├── model/
│   │   ├── Route.kt
│   │   ├── Waypoint.kt
│   │   ├── Segment.kt
│   │   ├── RouteTag.kt
│   │   ├── RouteSeason.kt
│   │   ├── RouteImage.kt
│   │   └── RouteRating.kt
│   └── dto/
│       ├── RouteDto.kt
│       ├── RouteCreateRequest.kt
│       ├── RouteUpdateRequest.kt
│       ├── WaypointDto.kt
│       └── SegmentDto.kt
├── resource/                        # 资源管理模块（水源、补给、营地）
│   ├── controller/
│   │   ├── WaterSourceController.kt
│   │   ├── SupplyController.kt
│   │   └── CampsiteController.kt
│   ├── service/
│   │   ├── WaterSourceService.kt
│   │   ├── SupplyService.kt
│   │   ├── CampsiteService.kt
│   │   └── impl/
│   ├── repository/
│   ├── model/
│   │   ├── WaterSource.kt
│   │   ├── Supply.kt
│   │   └── Campsite.kt
│   └── dto/
├── trip/                            # 行程管理模块
│   ├── controller/
│   │   ├── TripController.kt
│   │   └── TripParticipantController.kt
│   ├── service/
│   ├── repository/
│   ├── model/
│   └── dto/
├── equipment/                       # 装备管理模块
│   ├── controller/
│   ├── service/
│   ├── repository/
│   ├── model/
│   └── dto/
└── meal/                           # 餐食管理模块
    ├── controller/
    ├── service/
    ├── repository/
    ├── model/
    └── dto/
\`\`\`

## 🔧 **重构步骤**

### **阶段1：创建新的包结构**
1. 创建业务模块包
2. 移动相关类到对应模块
3. 更新import语句

### **阶段2：统一DTO和转换器**
1. 为每个模块创建统一的DTO结构
2. 创建专门的Converter类
3. 统一请求/响应格式

### **阶段3：优化Service层**
1. 提取公共Service接口
2. 统一异常处理
3. 添加事务管理

### **阶段4：改进Controller层**
1. 统一API响应格式
2. 添加统一的参数验证
3. 优化Swagger文档

## 📝 **代码规范建议**

### **1. 命名规范**
- **类名**: PascalCase (UserService, RouteController)
- **方法名**: camelCase (getUserById, createRoute)
- **常量**: UPPER_SNAKE_CASE (MAX_PAGE_SIZE, DEFAULT_STATUS)
- **包名**: lowercase (user, route, common)

### **2. DTO设计规范**
\`\`\`kotlin
// 统一的DTO基类
abstract class BaseDto {
    abstract val id: String
    abstract val createdAt: Instant
    abstract val updatedAt: Instant
}

// 请求DTO
data class CreateRequest<T> {
    // 创建请求的通用字段
}

data class UpdateRequest<T> {
    // 更新请求的通用字段
}

// 响应DTO
data class ApiResponse<T>(
    val success: Boolean,
    val message: String,
    val data: T?,
    val timestamp: Instant = Instant.now()
)
\`\`\`

### **3. Service层规范**
\`\`\`kotlin
// 基础Service接口
interface BaseService<T, ID> {
    fun findById(id: ID): T?
    fun findAll(pageable: Pageable): Page<T>
    fun create(entity: T): T
    fun update(id: ID, entity: T): T?
    fun delete(id: ID): Boolean
}

// 业务Service继承基础接口
interface RouteService : BaseService<Route, String> {
    // 业务特定方法
    fun findByRegion(region: String, pageable: Pageable): Page<Route>
}
\`\`\`

### **4. Controller层规范**
\`\`\`kotlin
@RestController
@RequestMapping("/api/v1/routes")
@Tag(name = "路线管理", description = "路线相关API")
@Validated
class RouteController(
    private val routeService: RouteService
) {
    
    @GetMapping
    @Operation(summary = "获取路线列表")
    fun getRoutes(
        @Valid @ModelAttribute request: RouteQueryRequest
    ): ResponseEntity<ApiResponse<Page<RouteDto>>> {
        // 统一的响应格式
    }
}
\`\`\`

## 🚀 **立即可以改进的点**

### **1. 创建统一的响应格式**
\`\`\`kotlin
// common/dto/ApiResponse.kt
data class ApiResponse<T>(
    val success: Boolean = true,
    val message: String = "操作成功",
    val data: T? = null,
    val timestamp: Instant = Instant.now(),
    val code: Int = 200
)
\`\`\`

### **2. 创建统一的异常处理**
\`\`\`kotlin
// common/exception/GlobalExceptionHandler.kt
@RestControllerAdvice
class GlobalExceptionHandler {
    
    @ExceptionHandler(BusinessException::class)
    fun handleBusinessException(e: BusinessException): ResponseEntity<ApiResponse<Nothing>> {
        return ResponseEntity.badRequest().body(
            ApiResponse(
                success = false,
                message = e.message ?: "业务异常",
                code = 400
            )
        )
    }
}
\`\`\`

### **3. 创建基础的查询请求DTO**
\`\`\`kotlin
// common/dto/BaseQueryRequest.kt
open class BaseQueryRequest(
    @Min(0) val page: Int = 0,
    @Min(1) @Max(100) val size: Int = 10,
    val sortBy: String = "createdAt",
    val sortDir: String = "desc"
) {
    fun toPageable(): Pageable {
        val sort = if (sortDir.lowercase() == "desc") {
            Sort.by(sortBy).descending()
        } else {
            Sort.by(sortBy).ascending()
        }
        return PageRequest.of(page, size, sort)
    }
}
\`\`\`

## 🎯 **优先级建议**

### **高优先级（立即执行）**
1. ✅ 创建统一的ApiResponse格式
2. ✅ 添加全局异常处理
3. ✅ 统一分页查询参数

### **中优先级（本周内）**
1. 🔄 按业务模块重新组织包结构
2. 🔄 创建基础Service接口
3. 🔄 统一DTO命名规范

### **低优先级（下周）**
1. ⏳ 完整的模块化重构
2. ⏳ 添加单元测试
3. ⏳ 性能优化

这样的重构会让代码更加清晰、可维护，你觉得从哪个部分开始比较好？