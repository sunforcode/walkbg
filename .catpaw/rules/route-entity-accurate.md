---
ruleType: EntityDefinition
description: Route实体准确定义 - 基于实际代码
keywords: [Route, 路线实体, 字段定义, 关联关系, 实体模型]
priority: CRITICAL
---

<!--
=== Route实体设计说明 ===
Route是WalkBG的核心业务实体，代表一条完整的徒步路线。
本定义基于实际代码 src/main/kotlin/org/example/route/model/Route.kt
确保与项目真实情况完全一致。

注意：坐标信息存储在关联的Waypoint实体中，不在Route主实体中。
-->

# Route实体准确定义

## 🏗️ 核心字段定义
```
// 基础信息
id: String (主键, length=64)
name: String (路线名称, 必填)
description: String? (路线描述, TEXT类型, 可选)
region: String? (地区名称, 可选)
regionId: String? (地区ID, 可选)

// 路线属性
difficulty: Int? (难度等级, 可选)
routeType: Int? (路线类型: 0=往返, 1=环线, 2=单程, 3=多日)
status: Int (状态: 0=规划中, 1=已发布, 2=已关闭, 默认=0)
isLoop: Boolean (是否环线, 默认=false)

// 媒体信息
coverUrl: String? (封面图片URL, length=500)
imageUrls: String? (图片URL数组, JSON字符串存储)

// 统计信息
popularity: Int (热度, 默认=0)
usageCount: Int (使用次数, 默认=0)
isFavorite: Boolean (是否收藏, 默认=false)

// 关联ID
defaultMapId: String (默认地图数据ID, 必填, 默认="")
createdBy: String? (创建者ID)

// 时间戳
createdAt: Instant (创建时间, 不可更新)
updatedAt: Instant (更新时间)
```

## 🔗 关联关系定义
```
// 核心关联
creator: ManyToOne<User> (创建者)
mapData: OneToOne<RouteMapData> (地图数据)

// 路线组成
segments: OneToMany<Segment> (路段列表)
waypoints: OneToMany<Waypoint> (路点列表)
tags: OneToMany<RouteTag> (标签列表, 注意是OneToMany不是ManyToMany)
images: OneToMany<RouteImage> (图片列表)

// 路线设施
waterSources: OneToMany<WaterSource> (水源列表)
campsites: OneToMany<Campsite> (营地列表)
supplies: OneToMany<Supply> (补给点列表)
markerPoints: OneToMany<MarkerPoint> (标记点列表)
dailyPlans: OneToMany<DailyPlan> (日程计划列表)

// 联系方式
contacts: OneToMany<Contact> (联系人列表)
hitchhikeContacts: OneToMany<HitchhikeContact> (搭车联系人列表)
routeContacts: OneToMany<RouteContact> (路线联系人列表)

// 用户关联
userFavoriteRoutes: OneToMany<UserFavoriteRoute> (用户收藏关联)
userCompletedRoutes: OneToMany<UserCompletedRoute> (用户完成关联)
tripRouteAssociations: OneToMany<TripRouteAssociation> (行程路线关联)

// 扩展信息
rating: OneToOne<RouteRating> (评分信息)
weatherInfo: OneToOne<RouteWeather> (天气信息)
```

## 💻 常用操作模式
```kotlin
// 创建Route
val route = Route(
    id = UUID.randomUUID().toString(),
    name = "路线名称",
    description = "路线描述",
    status = 0,
    defaultMapId = "map-data-id",
    createdBy = "user-id"
)

// 检查Route存在性
fun findById(id: String): Route {
    return ExceptionUtil.checkResourceExists(
        routeRepository.findById(id).orElse(null),
        "路线", id
    )
}

// 添加关联对象
route.addTag("标签名称")
route.addImage("图片URL", isCover = true)
route.addSegment(segment)
route.addMarkerPoint(markerPoint)
route.addDailyPlan(dailyPlan)

// 更新统计信息
route.incrementPopularity()
route.incrementUsageCount()
```

## 🚫 常见错误
```kotlin
// ❌ 错误：Route中没有坐标字段
route.startLatitude = 34.4889  // 字段不存在

// ❌ 错误：tags不是ManyToMany关系
@ManyToMany val tags: List<RouteTag>  // 实际是OneToMany

// ❌ 错误：忽略必填字段
Route(name = "test")  // 缺少id和defaultMapId

// ✅ 正确：坐标信息在Waypoint中
waypoint.latitude = 34.4889
waypoint.longitude = 110.0892
```

## 🔍 字段约束验证
```
REQUIRED_FIELDS: [id, name, status, defaultMapId]
OPTIONAL_FIELDS: [description, region, difficulty, routeType, coverUrl]
DEFAULT_VALUES: [status=0, popularity=0, usageCount=0, isLoop=false]

ENUM_RANGES:
- routeType: 0-3 (0=往返, 1=环线, 2=单程, 3=多日)
- status: 0-2 (0=规划中, 1=已发布, 2=已关闭)
- difficulty: 通常1-5 (具体范围需业务确认)

STRING_LENGTHS:
- id: 64字符
- coverUrl: 500字符
- name: 无明确限制 (需业务确认)
```

<!--
=== 重要提醒 ===
1. 坐标信息存储在Waypoint实体中，不在Route主实体
2. tags关系是OneToMany，不是ManyToMany
3. 图片可以通过imageUrls字段(JSON)或images关联存储
4. 所有关联关系都使用@JsonIgnore，避免序列化循环引用
5. 实体包含多个计算属性和便利方法

如果文档与此定义不符，以此定义为准！
-->
