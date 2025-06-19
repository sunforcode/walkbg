---
ruleType: Manual
description: WalkBG项目整体架构和设计模式指南
globs:
---

# 项目架构指南

## 📋 概述

WalkBG是一个基于Spring Boot + Kotlin的徒步旅行助手后台服务，采用DDD（领域驱动设计）+ 分层架构模式，提供路线管理、装备规划、行程安排等核心功能。

## 🏗️ 技术栈

- **框架**：Spring Boot 3.x
- **语言**：Kotlin
- **数据库**：JPA + Hibernate
- **构建工具**：Maven
- **API文档**：Swagger/OpenAPI 3
- **日志**：SLF4J + Logback

## 📂 项目结构

```
src/main/kotlin/org/example/
├── WalkbgApplication.kt              # 应用启动类
├── common/                           # 公共模块
│   ├── config/                      # 配置类
│   ├── dto/                         # 通用DTO
│   ├── exception/                   # 异常处理
│   └── util/                        # 工具类
├── user/                            # 用户管理模块
├── route/                           # 路线管理模块
├── equipment/                       # 装备管理模块
├── meal/                           # 餐食规划模块
├── trip/                           # 行程管理模块
└── resource/                       # 资源管理模块
```

## 🎯 架构模式

### 1. 分层架构
```
┌─────────────────────────────────────┐
│  Controller Layer (控制层)           │  ← HTTP请求处理
├─────────────────────────────────────┤
│  Application Service Layer (应用服务) │  ← 业务用例编排
├─────────────────────────────────────┤
│  Domain Service Layer (领域服务)     │  ← 核心业务逻辑
├─────────────────────────────────────┤
│  Repository Layer (仓储层)          │  ← 数据访问抽象
├─────────────────────────────────────┤
│  Model/Entity Layer (实体层)        │  ← 领域模型
└─────────────────────────────────────┘
```

### 2. 模块化设计
每个业务模块都遵循相同的内部结构：
```
module/
├── controller/     # REST API控制器
├── service/        # 业务服务层
├── repository/     # 数据访问层
├── model/          # 实体模型
└── dto/           # 数据传输对象
```

## 📂 相关文件

- 应用启动类：[WalkbgApplication.kt](md:src/main/kotlin/org/example/WalkbgApplication.kt)
- 全局配置：[pom.xml](md:pom.xml)
- 用户模块示例：[UserController.kt](md:src/main/kotlin/org/example/user/controller/UserController.kt)
- 路线模块示例：[RouteController.kt](md:src/main/kotlin/org/example/route/controller/RouteController.kt)

## ✅ 最佳实践

### 1. Controller层设计
```kotlin
// ✅ 推荐：标准的Controller结构
@RestController
@RequestMapping("/api/users")
@Tag(name = "用户管理", description = "用户相关API")
@Validated
class UserController(
    private val userService: UserService
) {

    @GetMapping("/{id}")
    @Operation(summary = "获取用户信息")
    fun getUserById(@PathVariable id: String): ResponseEntity<ApiResponse<User>> {
        val user = userService.findById(id)
        return ResponseUtil.success(user, "获取用户成功")
    }
}
```

### 2. Service层设计
```kotlin
// ✅ 推荐：接口 + 实现的方式
interface UserService {
    fun findById(id: String): User
    fun createUser(user: User): User
    fun updateUser(id: String, user: User): User
    fun deleteUser(id: String)
}

@Service
@Transactional
class UserServiceImpl(
    private val userRepository: UserRepository
) : UserService {

    override fun findById(id: String): User {
        return ExceptionUtil.checkResourceExists(
            userRepository.findById(id).orElse(null),
            "用户", id
        )
    }
}
```

### 3. Repository层设计
```kotlin
// ✅ 推荐：继承JpaRepository并添加自定义查询
interface UserRepository : JpaRepository<User, String> {

    fun findByEmail(email: String): User?

    fun existsByEmail(email: String): Boolean

    @Query("SELECT u FROM User u WHERE u.name LIKE %:name%")
    fun findByNameContaining(@Param("name") name: String, pageable: Pageable): Page<User>
}
```

### 4. Entity设计
```kotlin
// ✅ 推荐：使用JPA注解和Kotlin特性
@Entity
@Table(name = "users")
data class User(
    @Id
    @Column(name = "id", length = 36)
    val id: String = UUID.randomUUID().toString(),

    @Column(name = "name", nullable = false, length = 100)
    var name: String,

    @Column(name = "email", nullable = false, unique = true, length = 255)
    var email: String,

    @CreationTimestamp
    @Column(name = "created_at")
    val createdAt: Instant = Instant.now(),

    @UpdateTimestamp
    @Column(name = "updated_at")
    var updatedAt: Instant = Instant.now()
)
```

## ❌ 避免的做法

```kotlin
// ❌ 避免：Controller直接调用Repository
@RestController
class UserController(
    private val userRepository: UserRepository  // 违反分层原则
) {
    @GetMapping("/{id}")
    fun getUser(@PathVariable id: String) = userRepository.findById(id)
}

// ❌ 避免：Service层处理HTTP相关逻辑
@Service
class UserService {
    fun getUser(request: HttpServletRequest): ResponseEntity<User> {  // 违反职责分离
        // ...
    }
}

// ❌ 避免：在Entity中包含业务逻辑
@Entity
class User {
    fun sendEmail() {  // 违反单一职责原则
        // 发送邮件逻辑不应该在Entity中
    }
}
```

## 🔍 检查清单

在开发新功能时，请检查：

- [ ] 是否遵循了分层架构原则？
- [ ] Controller是否只处理HTTP相关逻辑？
- [ ] Service是否包含了核心业务逻辑？
- [ ] Repository是否只负责数据访问？
- [ ] Entity是否只包含数据和简单的业务方法？
- [ ] 是否使用了统一的异常处理？
- [ ] 是否使用了统一的响应格式？
- [ ] 是否添加了适当的API文档注解？

## 🎯 核心业务模块

### 1. 用户管理 (User)
- 用户注册、登录、信息管理
- 用户偏好设置
- 用户关系管理

### 2. 路线管理 (Route)
- 路线创建、编辑、发布
- 路线搜索、筛选
- 路线评价、收藏

### 3. 装备管理 (Equipment)
- 装备清单管理
- 装备推荐
- 装备重量计算

### 4. 行程管理 (Trip)
- 行程规划、安排
- 参与者管理
- 行程状态跟踪

### 5. 餐食规划 (Meal)
- 餐食计划制定
- 营养需求计算
- 食材采购清单

## 📊 数据流向

```
HTTP Request → Controller → Service → Repository → Database
     ↓             ↓          ↓           ↓
   参数验证    → 业务逻辑  → 数据访问  → 持久化存储
     ↓             ↓          ↓           ↓
HTTP Response ← DTO转换 ← 异常处理 ← 事务管理
```

## 🔧 开发规范

1. **命名规范**：使用有意义的英文名称，遵循Kotlin命名约定
2. **注释规范**：关键业务逻辑必须添加注释
3. **异常处理**：统一使用BusinessException和全局异常处理器
4. **日志记录**：重要操作必须记录日志
5. **测试覆盖**：核心业务逻辑必须有单元测试
6. **API文档**：所有公开API必须有Swagger注解
