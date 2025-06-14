 # Route模块架构文档

## 📋 概述

Route（路线）是WalkBG徒步应用的核心模块，负责管理徒步路线的完整生命周期，包括路线创建、查询、更新和删除等功能。Route模块采用DDD（领域驱动设计）架构，通过分层设计实现业务逻辑的清晰分离。

## 🏗️ 整体架构

```
┌─────────────────────────────────────────────────────────────┐
│                    Route模块架构                              │
├─────────────────────────────────────────────────────────────┤
│  Controller层 (HTTP接口)                                     │
│  ├── RouteController - REST API接口                         │
│  └── 请求参数验证、响应格式化                                   │
├─────────────────────────────────────────────────────────────┤
│  Application Service层 (应用服务)                            │
│  ├── RouteApplicationService - 业务用例协调                  │
│  └── 事务管理、多服务协调                                      │
├─────────────────────────────────────────────────────────────┤
│  Domain Service层 (领域服务)                                 │
│  ├── RouteService - 核心业务逻辑                             │
│  └── 业务规则验证、状态管理                                    │
├─────────────────────────────────────────────────────────────┤
│  Repository层 (数据访问)                                     │
│  ├── RouteRepository - 路线数据访问                          │
│  ├── WaypointRepository - 路点数据访问                       │
│  └── 其他关联Repository                                      │
├─────────────────────────────────────────────────────────────┤
│  Model层 (数据模型)                                          │
│  ├── Route - 路线实体                                       │
│  ├── Segment - 路段实体                                     │
│  ├── Waypoint - 路点实体                                    │
│  └── 其他关联实体                                            │
└─────────────────────────────────────────────────────────────┘
```

## 🎯 Route核心概念

### 什么是Route？
Route代表一条完整的徒步路线，包含：
- **基础信息**：名称、描述、区域、难度等级
- **地理信息**：起终点坐标、海拔、距离
- **状态管理**：规划中(0) → 已发布(1) → 已关闭(2)
- **统计数据**：热度、使用次数、收藏数

### Route的生命周期
```
创建路线 → 规划完善 → 发布上线 → 用户使用 → 数据统计 → 可能关闭
   ↓         ↓         ↓         ↓         ↓         ↓
 status=0  添加关联   status=1   记录访问   更新统计   status=2
```

## 🔗 关联关系图

```
                    Route (路线)
                        │
        ┌───────────────┼───────────────┐
        │               │               │
    核心关联          扩展关联        用户关联
        │               │               │
    ┌───┴───┐       ┌───┴───┐       ┌───┴───┐
    │       │       │       │       │       │
 Segment Waypoint Supply Campsite  User  UserFavorite
 (路段)  (路点)   (补给) (营地)   (创建者) (收藏)
    │       │       │       │
RouteTag RouteImage WaterSource MarkerPoint
(标签)   (图片)    (水源)     (标记点)
```

### 核心关联关系

1. **Route ↔ Segment (一对多)**
   - 一条路线包含多个路段
   - 路段记录距离、爬升、预计时间等

2. **Route ↔ Waypoint (通过Segment关联)**
   - 路点是路线上的关键位置
   - 包含GPS坐标、类型、描述等

3. **Route ↔ RouteTag (一对多)**
   - 路线可以有多个标签
   - 用于分类和搜索

4. **Route ↔ RouteImage (一对多)**
   - 路线可以有多张图片
   - 支持封面图片设置

### 扩展关联关系

- **Supply (补给点)**：路线上的补给站
- **Campsite (营地)**：可扎营的地点
- **WaterSource (水源)**：可取水的位置
- **MarkerPoint (标记点)**：其他重要标记

### 用户关联关系

- **User (创建者)**：路线的创建者
- **UserFavoriteRoute**：用户收藏的路线
- **UserCompletedRoute**：用户完成的路线

## 📁 代码结构

### Controller层
```kotlin
@RestController
@RequestMapping("/api/routes")
class RouteController {
    // GET /api/routes - 分页查询路线
    // GET /api/routes/{id} - 查询路线详情
    // GET /api/routes/{id}/details - 查询完整详情
    // POST /api/routes - 创建简单路线
    // POST /api/routes/complete - 创建完整路线
}
```

### Application Service层
```kotlin
@Service
class RouteApplicationService {
    // 读操作：getRouteDetails, searchRoutes, getRouteWithFullDetails
    // 写操作：createRoute, createCompleteRoute
    // 协调多个领域服务，处理完整业务用例
}
```

### Domain Service层
```kotlin
interface RouteService {
    // 基础CRUD：createRoute, updateRoute, deleteRoute
    // 查询操作：getRouteWithDetails, searchRoutes
    // 业务操作：publishRoute, favoriteRoute, recordRouteVisit
    // 状态管理：publishRoute, closeRoute
}
```

### Repository层
```kotlin
interface RouteRepository : JpaRepository<Route, String> {
    // 基础查询：findById, findAll
    // 条件查询：searchRoutes, findUserFavoriteRoutes
    // 统计查询：isRouteFavoritedByUser
}
```

### Model层
```kotlin
@Entity
data class Route(
    val id: String,
    var name: String,
    var description: String?,
    var difficulty: Int?,
    var status: Int = 0,
    // ... 其他字段

    // 关联关系
    val segments: MutableList<Segment>,
    val tags: MutableList<RouteTag>,
    val images: MutableList<RouteImage>
    // ... 其他关联
)
```

### DTO层
```kotlin
// 请求DTO
data class RouteCreateRequest(...)

// 响应DTO
data class RouteBasicResponse(...)
data class RouteDetailResponse(...)
```

## 🚀 主要功能

### 1. 路线查询
- **分页查询**：支持关键词、区域、难度等多条件筛选
- **详情查询**：基础信息 vs 完整信息（包含所有关联对象）
- **用户相关**：收藏状态、完成状态等个性化信息

### 2. 路线创建
- **简单创建**：只创建基础路线信息
- **完整创建**：一次性创建路线及所有关联对象
- **事务保证**：确保数据一致性

### 3. 路线管理
- **状态管理**：规划中 → 已发布 → 已关闭
- **数据统计**：访问记录、热度计算
- **用户交互**：收藏、完成记录

## 🔧 如何扩展

### 添加新的关联实体
1. 创建新的Entity类
2. 在Route中添加关联关系
3. 创建对应的Repository
4. 在RouteApplicationService中添加处理逻辑
5. 更新DTO和Controller接口

### 添加新的业务功能
1. 在RouteService接口中定义新方法
2. 在RouteServiceImpl中实现业务逻辑
3. 在RouteApplicationService中协调调用
4. 在RouteController中暴露HTTP接口

### 添加新的查询条件
1. 在RouteRepository中添加查询方法
2. 更新searchRoutes方法参数
3. 修改Controller的查询接口

## 📊 数据流向

### 查询流程
```
HTTP请求 → Controller → ApplicationService → DomainService → Repository → Database
                                                                    ↓
HTTP响应 ← Controller ← ApplicationService ← DomainService ← Repository ← Database
```

### 创建流程
```
HTTP请求(JSON) → Controller → ApplicationService → DomainService → Repository → Database
                                    ↓
                            创建关联对象 → 各Repository → Database
                                    ↓
HTTP响应 ← Controller ← ApplicationService ← 重新加载完整数据 ← Repository ← Database
```

## 🎯 设计原则

1. **单一职责**：每层只负责自己的职责
2. **依赖倒置**：高层模块不依赖低层模块
3. **开闭原则**：对扩展开放，对修改关闭
4. **事务一致性**：确保数据操作的原子性
5. **领域驱动**：以业务领域为核心设计

## 🔍 关键技术点

- **JPA关联映射**：@OneToMany, @ManyToOne等关联关系
- **事务管理**：@Transactional确保数据一致性
- **分页查询**：Spring Data的Pageable支持
- **DTO转换**：实体与传输对象的转换
- **参数验证**：@Valid, @NotBlank等验证注解

## 📈 性能考虑

- **懒加载**：关联对象使用FetchType.LAZY
- **查询优化**：避免N+1查询问题
- **分页查询**：大数据量时的性能保证
- **缓存策略**：热门路线的缓存机制（待实现）

这个架构设计确保了Route模块的可维护性、可扩展性和高性能，为WalkBG应用提供了稳定可靠的路线管理功能。
