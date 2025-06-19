---
ruleType: EntityDefinition
description: Waypoint实体定义 - 路点坐标和信息
keywords: [Waypoint, 路点, 坐标, latitude, longitude, 位置信息]
priority: HIGH
---

<!--
=== Waypoint实体说明 ===
Waypoint存储路线的关键位置点，包含坐标信息。
这是Route实体坐标信息的实际存储位置。
基于实际代码确保准确性。
-->

# Waypoint实体定义

## 🏗️ 核心字段约束
```
REQUIRED_FIELDS: [id, name, latitude, longitude, route]
COORDINATE_FIELDS: [latitude, longitude] (Double类型)
OPTIONAL_FIELDS: [description, elevation, type, sequenceNumber]
RELATIONSHIP: ManyToOne<Route> (属于某个路线)
```

## 💻 标准使用模式
```kotlin
// 创建Waypoint
val waypoint = Waypoint(
    id = UUID.randomUUID().toString(),
    name = "路点名称",
    latitude = 34.4889,
    longitude = 110.0892,
    elevation = 1614.0,
    route = route
)

// 坐标验证
ExceptionUtil.assertTrue(
    waypoint.latitude >= -90 && waypoint.latitude <= 90,
    "纬度必须在-90到90之间"
)
ExceptionUtil.assertTrue(
    waypoint.longitude >= -180 && waypoint.longitude <= 180,
    "经度必须在-180到180之间"
)
```

## 🚫 常见错误
```kotlin
// ❌ 错误：在Route中设置坐标
route.startLatitude = 34.4889  // Route没有坐标字段

// ✅ 正确：在Waypoint中设置坐标
waypoint.latitude = 34.4889
waypoint.longitude = 110.0892
```

<!--
=== 坐标系统说明 ===
项目使用WGS84坐标系统
纬度范围：-90 到 90
经度范围：-180 到 180
海拔单位：米
-->
