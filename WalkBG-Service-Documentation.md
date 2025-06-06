# WalkBG 徒步应用服务文档

## 📋 **项目概述**

WalkBG是一个基于Spring Boot的徒步旅行管理应用，提供用户管理、路线规划、行程组织、装备管理、餐食计划等功能的RESTful API服务。

---

## 🏗 **1. 整体架构与技术方案**

### **技术栈**
- **后端框架**: Spring Boot 3.2.3
- **编程语言**: Kotlin
- **数据库**: H2 (内存数据库)
- **ORM框架**: Spring Data JPA + Hibernate
- **API文档**: SpringDoc OpenAPI (Swagger)
- **构建工具**: Maven
- **JDK版本**: Java 17

### **架构模式**
采用经典的**分层架构**模式：
\`\`\`
Controller Layer (控制层)
    ↓
Service Layer (业务逻辑层)
    ↓
Repository Layer (数据访问层)
    ↓
Model Layer (数据模型层)
\`\`\`

### **项目结构**
\`\`\`
src/main/kotlin/org/example/
├── controller/          # REST控制器
├── service/            # 业务服务接口
├── service/impl/       # 业务服务实现
├── repository/         # 数据访问层
├── model/             # 数据模型
├── config/            # 配置类
└── exception/         # 异常处理
\`\`\`

### **核心技术特性**
- **RESTful API**: 标准的REST接口设计
- **分页查询**: 支持所有列表查询的分页
- **关联映射**: JPA实体间的复杂关联关系
- **事务管理**: 声明式事务处理
- **异常处理**: 全局异常处理机制
- **跨域支持**: CORS配置
- **API文档**: 自动生成的Swagger文档

---

## 🗄 **2. 数据模型与数据库结构**

### **核心实体模型**

#### **2.1 用户相关**

**User (用户表)**
\`\`\`kotlin
@Entity
@Table(name = "users")
data class User(
    @Id val id: String,                    // 用户ID (主键)
    var username: String,                  // 用户名
    var email: String,                     // 邮箱
    var nickname: String? = null,          // 昵称
    var avatar: String? = null,            // 头像URL
    var phone: String? = null,             // 手机号
    var isActive: Boolean = true,          // 是否激活
    val createdAt: Instant,                // 创建时间
    var updatedAt: Instant                 // 更新时间
)
\`\`\`

#### **2.2 路线相关**

**Route (路线表)**
\`\`\`kotlin
@Entity
@Table(name = "routes")
data class Route(
    @Id val id: String,                    // 路线ID
    var name: String,                      // 路线名称
    var description: String? = null,       // 描述
    var region: String? = null,            // 地区
    var distance: BigDecimal? = null,      // 距离(km)
    var duration: Int? = null,             // 预计用时(小时)
    var latitude: BigDecimal? = null,      // 纬度
    var longitude: BigDecimal? = null,     // 经度
    var altitude: BigDecimal? = null,      // 海拔(m)
    var elevationGain: BigDecimal? = null, // 爬升(m)
    var elevationLoss: BigDecimal? = null, // 下降(m)
    var difficulty: Int? = null,           // 难度 0:简单 1:中等 2:困难 3:极难
    var routeType: Int? = null,            // 类型 0:往返 1:环线 2:单程 3:多日
    var status: Int = 0,                   // 状态 0:规划中 1:已发布 2:已关闭
    var popularity: Int = 0,               // 热度
    var createdBy: String? = null,         // 创建者ID
    val createdAt: Instant,                // 创建时间
    var updatedAt: Instant                 // 更新时间
)
\`\`\`

#### **2.3 行程相关**

**Trip (行程表)**
\`\`\`kotlin
@Entity
@Table(name = "trips")
data class Trip(
    @Id val id: String,                    // 行程ID
    var name: String,                      // 行程名称
    var description: String? = null,       // 描述
    var startDate: Instant? = null,        // 开始时间
    var endDate: Instant? = null,          // 结束时间
    var status: Int = 0,                   // 状态 0:规划中 1:进行中 2:已完成 3:已取消
    var organizerId: String,               // 组织者ID
    var primaryRouteId: String? = null,    // 主路线ID
    var budget: BigDecimal? = null,        // 预算
    var actualCost: BigDecimal? = null,    // 实际花费
    var privacySetting: Int = 0,           // 隐私设置 0:公开 1:仅好友 2:私有
    var coverUrl: String? = null,          // 封面图片
    val createdAt: Instant,                // 创建时间
    var updatedAt: Instant                 // 更新时间
)
\`\`\`

#### **2.4 装备相关**

**EquipmentItem (装备物品表)**
\`\`\`kotlin
@Entity
@Table(name = "equipment_items")
data class EquipmentItem(
    @Id val id: String,                    // 装备ID
    var name: String,                      // 装备名称
    var description: String? = null,       // 描述
    var category: Int,                     // 分类 0:住宿 1:饮食 2:保暖 等
    var weight: BigDecimal = BigDecimal.ZERO, // 重量
    var weightUnit: Int = 0,               // 重量单位 0:克 1:千克 2:磅
    var quantity: Int = 1,                 // 数量
    var brand: String? = null,             // 品牌
    var model: String? = null,             // 型号
    var price: BigDecimal? = null,         // 价格
    var createdBy: String? = null,         // 创建者
    val createdAt: Instant,                // 创建时间
    var updatedAt: Instant                 // 更新时间
)
\`\`\`

**EquipmentList (装备清单表)**
\`\`\`kotlin
@Entity
@Table(name = "equipment_lists")
data class EquipmentList(
    @Id val id: String,                    // 清单ID
    var name: String,                      // 清单名称
    var type: Int,                         // 类型 0:个人装备 1:团队装备 2:模板装备
    var tripId: String? = null,            // 关联行程ID
    var creatorId: String? = null,         // 创建者ID
    var totalWeight: BigDecimal = BigDecimal.ZERO, // 总重量
    var personCount: Int = 1,              // 人数
    var status: Int = 0,                   // 状态 0:规划中 1:准备中 2:已完成
    val createdAt: Instant,                // 创建时间
    var updatedAt: Instant                 // 更新时间
)
\`\`\`

#### **2.5 餐食计划相关**

**MealPlan (餐食计划表)**
\`\`\`kotlin
@Entity
@Table(name = "meal_plans")
data class MealPlan(
    @Id val id: String,                    // 计划ID
    var name: String,                      // 计划名称
    var description: String? = null,       // 描述
    var tripId: String? = null,            // 关联行程ID
    var createdBy: String? = null,         // 创建者ID
    val createdAt: Instant,                // 创建时间
    var updatedAt: Instant                 // 更新时间
)
\`\`\`

**FoodItem (食物物品表)**
\`\`\`kotlin
@Entity
@Table(name = "food_items")
data class FoodItem(
    @Id val id: String,                    // 食物ID
    val name: String,                      // 食物名称
    val description: String? = null,       // 描述
    val weight: Double? = null,            // 重量(g)
    val quantity: Int = 1,                 // 数量
    val calories: Double? = null,          // 卡路里
    val protein: Double? = null,           // 蛋白质(g)
    val fat: Double? = null,               // 脂肪(g)
    val carbs: Double? = null,             // 碳水化合物(g)
    val price: Double? = null,             // 价格
    val prepared: Boolean = false,         // 是否已准备
    val isOwned: Boolean = false,          // 是否已拥有
    val createdAt: Instant,                // 创建时间
    var updatedAt: Instant                 // 更新时间
)
\`\`\`

#### **2.6 用水计划相关**

**WaterPlan (用水计划表)**
\`\`\`kotlin
@Entity
@Table(name = "water_plans")
data class WaterPlan(
    @Id val id: String,                    // 计划ID
    val name: String,                      // 计划名称
    val description: String? = null,       // 描述
    val tripId: String? = null,            // 关联行程ID
    val createdBy: String? = null,         // 创建者ID
    val createdAt: Instant,                // 创建时间
    var updatedAt: Instant                 // 更新时间
)
\`\`\`

#### **2.7 关联表**

**UserFavoriteRoute (用户收藏路线)**
\`\`\`kotlin
@Entity
@Table(name = "user_favorite_routes")
data class UserFavoriteRoute(
    val userId: String,                    // 用户ID
    val routeId: String,                   // 路线ID
    val favoritedAt: Instant               // 收藏时间
)
\`\`\`

**UserCompletedRoute (用户完成路线)**
\`\`\`kotlin
@Entity
@Table(name = "user_completed_routes")
data class UserCompletedRoute(
    val userId: String,                    // 用户ID
    val routeId: String,                   // 路线ID
    val completedAt: Instant               // 完成时间
)
\`\`\`

**TripParticipant (行程参与者)**
\`\`\`kotlin
@Entity
@Table(name = "trip_participants")
data class TripParticipant(
    val tripId: String,                    // 行程ID
    val userId: String,                    // 用户ID
    var role: Int = 0,                     // 角色 0:参与者 1:协助组织者 2:组织者
    var status: Int = 0,                   // 状态 0:待确认 1:已确认 2:已拒绝
    val joinedAt: Instant                  // 加入时间
)
\`\`\`

### **数据库索引设计**
- 主键索引：所有表的主键
- 外键索引：关联字段索引
- 查询优化索引：常用查询字段的复合索引
- 时间索引：创建时间、更新时间字段索引

---

## 🔧 **3. Repository层**

### **主要Repository接口**

#### **UserRepository**
\`\`\`kotlin
interface UserRepository : JpaRepository<User, String> {
    fun findByEmail(email: String): User?
    fun findByUsername(username: String): User?
    fun existsByEmail(email: String): Boolean
    fun existsByUsername(username: String): Boolean
    fun searchUsers(keyword: String?, isActive: Boolean?, pageable: Pageable): Page<User>
    fun getUserStatistics(): Map<String, Any>
    fun findUsersWithCompletedRoutesGreaterThan(count: Int, pageable: Pageable): Page<User>
}
\`\`\`

#### **RouteRepository**
\`\`\`kotlin
interface RouteRepository : JpaRepository<Route, String> {
    fun findByCreatedBy(createdBy: String, pageable: Pageable): Page<Route>
    fun findByDifficulty(difficulty: Int, pageable: Pageable): Page<Route>
    fun findByRegion(region: String, pageable: Pageable): Page<Route>
    fun searchRoutes(/* 多个参数 */, pageable: Pageable): Page<Route>
    fun getRouteStatistics(): Map<String, Any>
    fun findTop10PopularRoutes(pageable: Pageable): Page<Route>
}
\`\`\`

#### **TripRepository**
\`\`\`kotlin
interface TripRepository : JpaRepository<Trip, String> {
    fun findByOrganizerId(organizerId: String, pageable: Pageable): Page<Trip>
    fun findByStatus(status: Int, pageable: Pageable): Page<Trip>
    fun findUpcomingTrips(currentTime: Instant, status: Int, pageable: Pageable): Page<Trip>
    fun searchTrips(/* 多个参数 */, pageable: Pageable): Page<Trip>
    fun getTripStatistics(): Map<String, Any>
}
\`\`\`

---

## 🎯 **4. Service层**

### **服务接口设计**

#### **UserService**
\`\`\`kotlin
interface UserService {
    // 基础CRUD
    fun getAllUsers(pageable: Pageable): Page<User>
    fun getUserById(id: String): User?
    fun createUser(user: User): User
    fun updateUser(id: String, user: User): User?
    fun deleteUser(id: String): Boolean
    
    // 业务方法
    fun searchUsers(keyword: String?, isActive: Boolean?, pageable: Pageable): Page<User>
    fun getUserStatistics(): Map<String, Any>
    fun validateUser(username: String, email: String): Boolean
}
\`\`\`

#### **RouteService**
\`\`\`kotlin
interface RouteService {
    // 基础CRUD
    fun getAllRoutes(pageable: Pageable): Page<Route>
    fun getRouteById(id: String): Route?
    fun createRoute(route: Route): Route
    fun updateRoute(id: String, route: Route): Route?
    fun deleteRoute(id: String): Boolean
    
    // 业务方法
    fun searchRoutes(keyword: String?, difficulty: Int?, region: String?, pageable: Pageable): Page<Route>
    fun getPopularRoutes(): List<Route>
    fun incrementPopularity(id: String)
    fun getRouteStatistics(): Map<String, Any>
}
\`\`\`

---

## 🌐 **5. Controller层与API接口**

### **服务基础信息**
- **服务地址**: `http://localhost:8080`
- **上下文路径**: `/walkbg`
- **完整API基础URL**: `http://localhost:8080/walkbg/api`

### **5.1 用户管理API (`/api/users`)**

| 方法 | 路径 | 描述 | 参数 |
|------|------|------|------|
| GET | `/api/users` | 获取所有用户(分页) | page, size |
| GET | `/api/users/{id}` | 根据ID获取用户 | id |
| GET | `/api/users/email/{email}` | 根据邮箱获取用户 | email |
| GET | `/api/users/username/{username}` | 根据用户名获取用户 | username |
| POST | `/api/users` | 创建用户 | User对象 |
| PUT | `/api/users/{id}` | 更新用户 | id, User对象 |
| DELETE | `/api/users/{id}` | 删除用户 | id |
| GET | `/api/users/search` | 搜索用户 | keyword, isActive |
| GET | `/api/users/statistics` | 获取用户统计 | - |
| GET | `/api/users/{id}/stats` | 获取用户个人统计 | id |
| GET | `/api/users/most-active` | 获取最活跃用户 | page, size |
| GET | `/api/users/validate` | 验证用户名邮箱 | username, email |

### **5.2 路线管理API (`/api/routes`)**

| 方法 | 路径 | 描述 | 参数 |
|------|------|------|------|
| GET | `/api/routes` | 获取所有路线(分页) | page, size |
| GET | `/api/routes/{id}` | 根据ID获取路线 | id |
| POST | `/api/routes` | 创建路线 | Route对象 |
| PUT | `/api/routes/{id}` | 更新路线 | id, Route对象 |
| DELETE | `/api/routes/{id}` | 删除路线 | id |
| GET | `/api/routes/search` | 搜索路线 | keyword, difficulty, region |
| GET | `/api/routes/difficulty/{difficulty}` | 按难度获取路线 | difficulty |
| GET | `/api/routes/region/{region}` | 按地区获取路线 | region |
| GET | `/api/routes/popular` | 获取热门路线 | - |
| GET | `/api/routes/most-favorited` | 获取最受收藏路线 | - |
| GET | `/api/routes/most-completed` | 获取最多完成路线 | - |
| POST | `/api/routes/{id}/increment-popularity` | 增加路线热度 | id |
| GET | `/api/routes/statistics` | 获取路线统计 | - |
| GET | `/api/routes/creator/{creatorId}` | 按创建者获取路线 | creatorId |
| GET | `/api/routes/tag/{tag}` | 按标签获取路线 | tag |
| GET | `/api/routes/season/{season}` | 按季节获取路线 | season |

### **5.3 行程管理API (`/api/trips`)**

| 方法 | 路径 | 描述 | 参数 |
|------|------|------|------|
| GET | `/api/trips` | 获取所有行程(分页) | page, size |
| GET | `/api/trips/{id}` | 根据ID获取行程 | id |
| POST | `/api/trips` | 创建行程 | Trip对象 |
| PUT | `/api/trips/{id}` | 更新行程 | id, Trip对象 |
| DELETE | `/api/trips/{id}` | 删除行程 | id |
| GET | `/api/trips/search` | 搜索行程 | keyword, status, organizerId |
| GET | `/api/trips/user/{userId}` | 获取用户的行程 | userId |
| GET | `/api/trips/upcoming` | 获取即将开始的行程 | page, size |
| GET | `/api/trips/ongoing` | 获取正在进行的行程 | page, size |
| GET | `/api/trips/completed` | 获取已完成的行程 | page, size |
| GET | `/api/trips/popular` | 获取热门行程 | - |
| GET | `/api/trips/recent` | 获取最近创建的行程 | - |
| GET | `/api/trips/statistics` | 获取行程统计 | - |
| GET | `/api/trips/participant/{userId}` | 获取用户参与的行程 | userId |
| PATCH | `/api/trips/{id}/status` | 更新行程状态 | id, status |
| GET | `/api/trips/organizer/{organizerId}` | 按组织者获取行程 | organizerId |
| GET | `/api/trips/status/{status}` | 按状态获取行程 | status |

### **5.4 装备管理API (`/api/equipment`)**

| 方法 | 路径 | 描述 | 参数 |
|------|------|------|------|
| GET | `/api/equipment/items` | 获取所有装备物品 | page, size |
| GET | `/api/equipment/items/{id}` | 根据ID获取装备 | id |
| POST | `/api/equipment/items` | 创建装备物品 | EquipmentItem对象 |
| PUT | `/api/equipment/items/{id}` | 更新装备物品 | id, EquipmentItem对象 |
| DELETE | `/api/equipment/items/{id}` | 删除装备物品 | id |
| GET | `/api/equipment/items/category/{category}` | 按分类获取装备 | category |
| GET | `/api/equipment/items/search` | 搜索装备物品 | keyword, category, createdBy等 |
| GET | `/api/equipment/items/creator/{creatorId}` | 按创建者获取装备 | creatorId |
| GET | `/api/equipment/category-stats` | 获取装备分类统计 | - |
| GET | `/api/equipment/weight-stats` | 获取重量统计 | category |
| GET | `/api/equipment/latest` | 获取最新装备 | - |
| GET | `/api/equipment/lightest` | 获取最轻装备 | - |
| GET | `/api/equipment/heaviest` | 获取最重装备 | - |
| GET | `/api/equipment/weight-range` | 按重量范围查找装备 | minWeight, maxWeight |
| GET | `/api/equipment/similar-weight` | 查找相似重量装备 | targetWeight, tolerance, excludeId |
| GET | `/api/equipment/search-by-name` | 按名称搜索装备 | name |

### **5.5 餐食计划API (`/api/meal-plans`)**

| 方法 | 路径 | 描述 | 参数 |
|------|------|------|------|
| GET | `/api/meal-plans` | 获取所有餐食计划 | page, size |
| GET | `/api/meal-plans/{id}` | 根据ID获取餐食计划 | id |
| POST | `/api/meal-plans` | 创建餐食计划 | MealPlan对象 |
| PUT | `/api/meal-plans/{id}` | 更新餐食计划 | id, MealPlan对象 |
| DELETE | `/api/meal-plans/{id}` | 删除餐食计划 | id |
| GET | `/api/meal-plans/trip/{tripId}` | 按行程获取餐食计划 | tripId |
| GET | `/api/meal-plans/creator/{creatorId}` | 按创建者获取餐食计划 | creatorId |
| GET | `/api/meal-plans/search` | 搜索餐食计划 | keyword, tripId, creatorId |
| GET | `/api/meal-plans/statistics` | 获取餐食计划统计 | - |
| GET | `/api/meal-plans/latest` | 获取最新餐食计划 | - |
| GET | `/api/meal-plans/search-by-name` | 按名称搜索餐食计划 | name |
| GET | `/api/meal-plans/{id}/with-days` | 获取餐食计划详情 | id |

### **5.6 用水计划API (`/api/water-plans`)**

| 方法 | 路径 | 描述 | 参数 |
|------|------|------|------|
| GET | `/api/water-plans` | 获取所有用水计划 | page, size |
| GET | `/api/water-plans/{id}` | 根据ID获取用水计划 | id |
| POST | `/api/water-plans` | 创建用水计划 | WaterPlan对象 |
| PUT | `/api/water-plans/{id}` | 更新用水计划 | id, WaterPlan对象 |
| DELETE | `/api/water-plans/{id}` | 删除用水计划 | id |
| GET | `/api/water-plans/trip/{tripId}` | 按行程获取用水计划 | tripId |
| GET | `/api/water-plans/creator/{creatorId}` | 按创建者获取用水计划 | creatorId |
| GET | `/api/water-plans/search` | 搜索用水计划 | keyword, tripId, creatorId |
| GET | `/api/water-plans/statistics` | 获取用水计划统计 | - |
| GET | `/api/water-plans/latest` | 获取最新用水计划 | - |
| GET | `/api/water-plans/search-by-name` | 按名称搜索用水计划 | name |
| GET | `/api/water-plans/{id}/with-days` | 获取用水计划详情 | id |
| GET | `/api/water-plans/{id}/water-source-stats` | 获取水源统计 | id |

### **5.7 行程参与者API (`/api/trip-participants`)**

| 方法 | 路径 | 描述 | 参数 |
|------|------|------|------|
| POST | `/api/trip-participants` | 添加行程参与者 | tripId, userId, role, status |
| DELETE | `/api/trip-participants` | 移除行程参与者 | tripId, userId |
| GET | `/api/trip-participants/trip/{tripId}` | 获取行程参与者 | tripId |
| GET | `/api/trip-participants/user/{userId}` | 获取用户参与的行程 | userId |
| GET | `/api/trip-participants/trip/{tripId}/role/{role}` | 按角色获取参与者 | tripId, role |
| GET | `/api/trip-participants/trip/{tripId}/status/{status}` | 按状态获取参与者 | tripId, status |
| PATCH | `/api/trip-participants/role` | 更新参与者角色 | tripId, userId, role |
| PATCH | `/api/trip-participants/status` | 更新参与者状态 | tripId, userId, status |
| GET | `/api/trip-participants/check` | 检查用户是否参与 | tripId, userId |
| GET | `/api/trip-participants/count/trip/{tripId}` | 统计行程参与者数量 | tripId |
| GET | `/api/trip-participants/count/user/{userId}` | 统计用户参与行程数量 | userId |
| GET | `/api/trip-participants/trip/{tripId}/organizers` | 获取行程组织者 | tripId |
| GET | `/api/trip-participants/check-organizer` | 检查是否为组织者 | tripId, userId |
| GET | `/api/trip-participants/recent/user/{userId}` | 获取用户最近参与 | userId |
| POST | `/api/trip-participants/batch` | 批量添加参与者 | tripId, userIds, role |
| DELETE | `/api/trip-participants/batch` | 批量移除参与者 | tripId, userIds |

---

## 🔧 **6. 配置与部署**

### **应用配置**
- **端口**: 8080
- **上下文路径**: `/walkbg`
- **数据库**: H2内存数据库
- **JPA**: 自动建表模式
- **日志**: DEBUG级别
- **CORS**: 支持跨域访问

### **重要端点**
- **API文档**: `http://localhost:8080/walkbg/swagger-ui.html`
- **数据库控制台**: `http://localhost:8080/walkbg/h2-console`
- **健康检查**: `http://localhost:8080/walkbg/actuator/health`

### **API测试示例**
\`\`\`bash
# 获取用户列表
curl -X GET "http://localhost:8080/walkbg/api/users?page=0&size=5"

# 创建用户
curl -X POST "http://localhost:8080/walkbg/api/users" \
  -H "Content-Type: application/json" \
  -d '{"id": "user001", "username": "testuser", "email": "test@example.com"}'

# 获取路线列表
curl -X GET "http://localhost:8080/walkbg/api/routes?page=0&size=5"

# 创建路线
curl -X POST "http://localhost:8080/walkbg/api/routes" \
  -H "Content-Type: application/json" \
  -d '{"id": "route001", "name": "测试路线", "difficulty": 1, "createdBy": "user001"}'
\`\`\`

---

## 📊 **7. 数据类型说明**

### **状态码映射**
- **Trip.status**: 0=规划中, 1=进行中, 2=已完成, 3=已取消
- **Route.difficulty**: 0=简单, 1=中等, 2=困难, 3=极难
- **Route.routeType**: 0=往返, 1=环线, 2=单程, 3=多日
- **EquipmentList.type**: 0=个人装备, 1=团队装备, 2=模板装备
- **TripParticipant.role**: 0=参与者, 1=协助组织者, 2=组织者

### **数据精度**
- **BigDecimal字段**: precision=8-10, scale=2-6
- **Double字段**: 仅precision，无scale
- **时间字段**: 使用Instant类型，UTC时间

---

## 🚀 **8. 后续扩展方向**

### **功能扩展**
- 实时聊天功能
- 地图集成
- 天气预报集成
- 图片上传管理
- 用户权限系统
- 消息通知系统

### **技术优化**
- Redis缓存集成
- MySQL数据库迁移
- Docker容器化部署
- 微服务架构拆分
- 性能监控集成

---

**文档版本**: v1.0  
**最后更新**: 2024-06-06  
**维护者**: WalkBG开发团队