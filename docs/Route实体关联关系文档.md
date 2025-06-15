# Route实体关联关系文档

## 概述
Route是徒步路线管理系统的核心实体，包含丰富的关联关系，支持完整的路线管理功能。

## 1. Route核心属性

### 基本信息
- `id`: String - 路线唯一标识
- `name`: String - 路线名称
- `description`: String? - 路线描述
- `region`: String? - 地区
- `regionId`: String? - 地区ID
- `difficulty`: Int? - 难度等级
- `routeType`: Int? - 路线类型（0:往返, 1:环线, 2:单程, 3:多日）
- `status`: Int - 状态（0:规划中, 1:已发布, 2:已关闭）

### 媒体信息
- `coverUrl`: String? - 封面图片URL
- `imageUrls`: String? - 图片URL数组（JSON字符串）
- `defaultMapId`: String - 默认地图ID

### 统计信息
- `popularity`: Int - 热度
- `usageCount`: Int - 使用次数
- `isLoop`: Boolean - 是否环线
- `isFavorite`: Boolean - 是否收藏

### 审计信息
- `createdAt`: Instant - 创建时间
- `updatedAt`: Instant - 更新时间
- `createdBy`: String? - 创建者ID

## 2. 关联实体详细说明

### 2.1 路线结构相关（@OneToMany）

#### Segment（路段）
```kotlin
@OneToMany(mappedBy = "route", cascade = [CascadeType.ALL], orphanRemoval = true)
val segments: MutableList<Segment>
```
- **关系**: 一对多
- **说明**: 路线由多个路段组成
- **级联**: 删除路线时删除所有路段
- **实体属性**:
  - 距离、海拔增减、预估时间、难度
  - 起点和终点Waypoint
  - 包含多个PathPoint（路径点）
  - 关键点、危险点、封闭信息

#### Waypoint（路径点）
```kotlin
@OneToMany(mappedBy = "route", cascade = [CascadeType.ALL], orphanRemoval = true)
val waypoints: MutableList<Waypoint>
```
- **关系**: 一对多
- **说明**: 路线上的重要节点
- **级联**: 删除路线时删除所有路径点
- **实体属性**:
  - 经纬度、海拔、类型
  - 图标URL、图片URL
  - 序号排序

### 2.2 路线设施相关（@OneToMany）

#### WaterSource（水源）
```kotlin
@OneToMany(mappedBy = "route", cascade = [CascadeType.ALL])
val waterSources: MutableList<WaterSource>
```
- **关系**: 一对多
- **说明**: 路线相关的水源信息
- **实体属性**:
  - 水源类型：天然、处理过、瓶装水等
  - 水质等级：优质、良好、一般、较差、未知
  - 可靠性和处理需求

#### Campsite（营地）
```kotlin
@OneToMany(mappedBy = "route", cascade = [CascadeType.ALL])
val campsites: MutableList<Campsite>
```
- **关系**: 一对多
- **说明**: 路线相关的营地信息
- **实体属性**:
  - 营地类型：官方指定、非官方、野营、避难所等
  - 位置信息和验证状态

#### Supply（补给点）
```kotlin
@OneToMany(mappedBy = "route", cascade = [CascadeType.ALL])
val supplies: MutableList<Supply>
```
- **关系**: 一对多
- **说明**: 路线相关的补给点信息
- **实体属性**:
  - 补给类型：商店、餐厅、自动售货机、紧急补给点等
  - 位置、价格、验证信息

#### Contact（联系人）
```kotlin
@OneToMany(mappedBy = "route", cascade = [CascadeType.ALL])
val contacts: MutableList<Contact>
```
- **关系**: 一对多
- **说明**: 路线相关的联系人信息
- **实体属性**:
  - 姓名、电话、位置、价格
  - 验证状态

#### HitchhikeContact（搭车联系人）
```kotlin
@OneToMany(mappedBy = "route", cascade = [CascadeType.ALL])
val hitchhikeContacts: MutableList<HitchhikeContact>
```
- **关系**: 一对多
- **说明**: 专门的搭车服务联系人
- **实体属性**:
  - 价格和验证状态

### 2.3 路线标记相关（@OneToMany）

#### RouteTag（路线标签）
```kotlin
@OneToMany(mappedBy = "route", cascade = [CascadeType.ALL], orphanRemoval = true)
val tags: MutableList<RouteTag>
```
- **关系**: 一对多
- **说明**: 用于路线分类和搜索
- **级联**: 删除路线时删除所有标签

#### RouteImage（路线图片）
```kotlin
@OneToMany(mappedBy = "route", cascade = [CascadeType.ALL], orphanRemoval = true)
val images: MutableList<RouteImage>
```
- **关系**: 一对多
- **说明**: 路线相关图片管理
- **级联**: 删除路线时删除所有图片
- **实体属性**:
  - 支持封面图片和序号排序

#### MarkerPoint（标记点）
```kotlin
@OneToMany(mappedBy = "route", cascade = [CascadeType.ALL])
val markerPoints: MutableList<MarkerPoint>
```
- **关系**: 一对多
- **说明**: 路线上的特殊标记点
- **实体属性**:
  - 标记类型：景点、观景点、危险区域、休息区、水源、食物、避难所等
  - 位置、颜色、图标

### 2.4 路线计划相关（@OneToMany）

#### DailyPlan（每日计划）
```kotlin
@OneToMany(mappedBy = "route", cascade = [CascadeType.ALL])
val dailyPlans: MutableList<DailyPlan>
```
- **关系**: 一对多
- **说明**: 多日路线的日程安排
- **实体属性**:
  - 距离、时间、海拔变化、住宿
  - 关联多个路段

### 2.5 用户关联（@OneToMany）

#### UserFavoriteRoute（用户收藏路线）
```kotlin
@OneToMany(mappedBy = "route", cascade = [CascadeType.ALL])
val userFavoriteRoutes: MutableList<UserFavoriteRoute>
```
- **关系**: 一对多
- **说明**: 用户与路线的收藏关系
- **实体属性**:
  - 记录收藏时间

#### UserCompletedRoute（用户完成路线）
```kotlin
@OneToMany(mappedBy = "route", cascade = [CascadeType.ALL])
val userCompletedRoutes: MutableList<UserCompletedRoute>
```
- **关系**: 一对多
- **说明**: 用户与路线的完成关系
- **实体属性**:
  - 记录完成时间
  - 使用复合主键

### 2.6 行程关联（@OneToMany）

#### TripRouteAssociation（行程路线关联）
```kotlin
@OneToMany(mappedBy = "route", cascade = [CascadeType.ALL])
val tripRouteAssociations: MutableList<TripRouteAssociation>
```
- **关系**: 一对多
- **说明**: 行程与路线的关联关系
- **实体属性**:
  - 支持主要路线标记

### 2.7 联系人关联（@OneToMany）

#### RouteContact（路线联系人关联）
```kotlin
@OneToMany(mappedBy = "route", cascade = [CascadeType.ALL])
val routeContacts: MutableList<RouteContact>
```
- **关系**: 一对多
- **说明**: 路线与联系人的关联关系
- **实体属性**:
  - 联系人类型：向导、接送服务、住宿联系人、紧急联系人等
  - 支持优先级排序

### 2.8 单一关联（@OneToOne/@ManyToOne）

#### User（创建者）
```kotlin
@ManyToOne(fetch = FetchType.LAZY)
@JoinColumn(name = "created_by", insertable = false, updatable = false)
var creator: User?
```
- **关系**: 多对一
- **说明**: 路线创建者

#### RouteMapData（地图数据）
```kotlin
@OneToOne(fetch = FetchType.LAZY)
@JoinColumn(name = "default_map_id", insertable = false, updatable = false)
var mapData: RouteMapData?
```
- **关系**: 一对一
- **说明**: 路线的地理统计信息
- **实体属性**:
  - 距离、时长、海拔增减
  - 统计收藏数、完成数、行程数

#### RouteRating（路线评分）
```kotlin
@OneToOne(mappedBy = "route", cascade = [CascadeType.ALL])
var rating: RouteRating?
```
- **关系**: 一对一
- **说明**: 路线评分信息
- **实体属性**:
  - 综合评分：整体、风景、难度、体验、设施
  - 评分统计

#### RouteWeather（天气信息）
```kotlin
@OneToOne(mappedBy = "route", cascade = [CascadeType.ALL])
var weatherInfo: RouteWeather?
```
- **关系**: 一对一
- **说明**: 路线天气信息
- **实体属性**:
  - 天气描述和注意事项
  - 最佳季节信息
  - 季节性天气详情

## 3. 实用方法

### 3.1 统计方法
- `incrementPopularity()`: 增加热度
- `incrementUsageCount()`: 增加使用次数

### 3.2 关联管理方法
- `addSegment(segment)`: 添加路段
- `addTag(tag)`: 添加标签
- `addImage(imageUrl, isCover, sequenceNumber)`: 添加图片
- `addMarkerPoint(markerPoint)`: 添加标记点
- `addDailyPlan(dailyPlan)`: 添加每日计划

### 3.3 计算属性
- `createdById`: 获取创建者ID
- `isFavoriteByUser`: 是否被当前用户收藏（需要在查询时设置）

## 4. 数据模型特点

1. **层次化结构**: Route -> Segment -> PathPoint 形成三层路径结构
2. **丰富的关联**: 支持用户、行程、设施、标记等多维度关联
3. **类型安全**: 使用枚举定义各种类型和状态
4. **审计支持**: 大部分实体包含创建时间、更新时间
5. **验证机制**: 支持用户验证和最后验证时间
6. **统计功能**: 内置热度、使用次数等统计字段
7. **索引优化**: 关键查询字段都建立了数据库索引
8. **级联操作**: 合理的级联删除和更新策略
9. **懒加载**: 使用LAZY加载策略优化性能
10. **JSON序列化**: 使用@JsonIgnore避免循环引用

## 5. 使用建议

1. **查询优化**: 根据需要使用JOIN FETCH避免N+1问题
2. **级联操作**: 注意级联删除的影响范围
3. **事务管理**: 复杂操作需要适当的事务边界
4. **性能考虑**: 大量关联数据时考虑分页和懒加载
5. **数据一致性**: 注意维护统计字段的一致性
