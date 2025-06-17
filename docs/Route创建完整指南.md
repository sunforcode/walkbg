# 🚀 WalkBG Route创建完整指南

## 📋 概述

WalkBG的Route创建功能支持创建包含9种关联对象的完整徒步路线，采用分步保存策略确保数据完整性。本文档详细介绍了Route创建的完整流程、数据结构、API调用方法和最佳实践。

## 🏗️ Route创建架构

### 1. 数据模型层次结构

```
Route (主实体)
├── RouteMapData (一对一) - 地图数据
├── User (多对一) - 创建者
├── 简单关联对象 (第一步创建)
│   ├── RouteTag (标签)
│   ├── RouteImage (图片)
│   ├── Waypoint (路点)
│   ├── Supply (补给点)
│   ├── Campsite (营地)
│   ├── MarkerPoint (标记点)
│   ├── DailyPlan (日程计划)
│   ├── WaterSource (水源)
│   └── HitchhikeContact (搭车联系人)
└── 复杂关联对象 (第二步创建)
    └── Segment (路段) - 依赖Waypoint
```

### 2. 分步保存策略

为了解决外键约束问题，系统采用分步保存策略：

```kotlin
@Transactional
fun createCompleteRoute(request: RouteCreateRequest): RouteBasicResponse {
    // 步骤1: 创建并保存Route主实体
    val route = request.toRoute()
    val savedRoute = routeService.createRoute(route)

    // 步骤2: 创建简单关联对象（无外键依赖）
    createSimpleAssociations(savedRoute, request)

    // 步骤3: 保存Route和简单关联对象（包括Waypoint）
    val routeWithSimpleAssociations = routeService.updateRoute(savedRoute)

    // 步骤4: 创建复杂关联对象（依赖已持久化的Waypoint）
    createComplexAssociations(routeWithSimpleAssociations, request)

    // 步骤5: 最终保存所有关联对象
    val completeRoute = routeService.updateRoute(routeWithSimpleAssociations)

    // 步骤6: 重新加载完整数据并返回
    val finalRoute = routeService.getRouteWithDetails(completeRoute.id)
    return RouteBasicResponse.fromRoute(finalRoute)
}
```

### 3. 关键技术点

- **JPA关联映射**: 使用@OneToMany, @ManyToOne等注解管理关联关系
- **级联保存**: 通过cascade = [CascadeType.ALL]实现级联操作
- **事务管理**: @Transactional确保数据一致性
- **对象引用**: 使用对象引用而非ID引用避免外键约束问题

## 🔧 Route创建方法

### 方法1: 使用脚本创建（推荐）

#### 1.1 完整路线创建流程

```bash
# 使用总入口脚本（推荐）
./scripts/create.sh route_data.json

# 指定用户名
./scripts/create.sh route_data.json john_doe

# 指定用户名和邮箱
./scripts/create.sh route_data.json john_doe john@example.com
```

**create.sh 工作流程**:
```
步骤 0/5: 验证JSON数据格式     # 🆕 使用简化验证脚本
步骤 1/5: 处理用户信息         # 查询/创建用户
步骤 2/5: 创建地图数据         # 生成地图数据
步骤 3/5: 更新路线数据         # 更新用户ID和地图ID
步骤 4/5: 验证更新后的JSON数据  # 🆕 二次验证确保完整性
步骤 5/5: 创建路线            # 调用API创建路线
```

**脚本特点**：
- ✅ **双重验证**: 创建前后都进行数据验证
- ✅ **自动用户管理**: 自动查询或创建用户
- ✅ **地图数据生成**: 自动创建地图数据
- ✅ **错误处理**: 详细的错误信息和处理
- ✅ **资源保护**: 验证失败立即停止，避免浪费资源

#### 1.2 JSON数据验证

在创建路线之前，必须先验证JSON数据格式：

```bash
# 单独验证JSON数据
./scripts/validate_route.sh route_data.json
```

**验证功能**（简化版）:
- ✅ **JSON格式**: 基础JSON语法检查
- ✅ **必需字段**: `name` 和 `created_by` 必须存在且不为空
- ✅ **类型验证**: 所有字段的数据类型必须正确
- ✅ **枚举验证**: 枚举类型字段的值必须在指定范围内
- ✅ **特殊规则**: `sequence_number > 0`, `dayNumber > 0` 等

### 方法2: 直接调用API

#### 2.1 创建RouteMapData

```bash
curl -X POST "http://localhost:8080/walkbg/api/route-map-data" \
  -H "Content-Type: application/json" \
  -d '{
    "id": "map-data-id",
    "distance": 8.5,
    "duration": 6,
    "latitude": 34.4889,
    "longitude": 110.0892,
    "altitude": 1614.0,
    "elevationGain": 468.0,
    "elevationLoss": 50.0,
    "favoriteCount": 0,
    "completionCount": 0,
    "tripCount": 0
  }'
```

#### 2.2 创建完整路线

```bash
curl -X POST "http://localhost:8080/walkbg/api/routes" \
  -H "Content-Type: application/json" \
  -d @route_data.json
```

## 📝 Route数据结构详解

### 3.1 核心字段

```json
{
  "id": "route-uuid",                    // 路线唯一标识
  "name": "路线名称",                     // 必填，路线名称
  "description": "路线描述",              // 可选，详细描述
  "region": "地区名称",                   // 可选，地理区域
  "region_id": "地区ID",                 // 可选，地区标识
  "difficulty": 4,                       // 可选，难度等级(1-5)
  "route_type": 0,                       // 可选，路线类型(0:往返,1:环线,2:单程,3:多日)
  "status": 1,                           // 状态(0:规划中,1:已发布,2:已关闭)
  "cover_url": "封面图片URL",             // 可选，封面图片
  "default_map_id": "地图数据ID",         // 必填，关联的地图数据
  "created_by": "创建者ID"                // 必填，创建者用户ID
}
```

### 3.2 关联对象详解

#### 3.2.1 标签 (tags)
```json
"tags": ["山峰", "险峻", "挑战", "一日游", "华山"]
```

#### 3.2.2 路点 (waypoints)
```json
"waypoints": [
  {
    "name": "华山北峰",                   // 路点名称
    "description": "华山北峰，海拔1614米", // 路点描述
    "latitude": 34.4889,                 // 纬度
    "longitude": 110.0892,               // 经度
    "elevation": 1614.0,                 // 海拔
    "type": "start",                     // 类型: start|waypoint|end
    "sequence_number": 1,                // 序号，必须>0
    "icon_url": "图标URL",               // 可选
    "image_url": "图片URL"               // 可选
  }
]
```

#### 3.2.3 路段 (segments)
```json
"segments": [
  {
    "name": "北峰至苍龙岭段",             // 路段名称
    "description": "险峻路段，需小心",     // 路段描述
    "distance": 4.2,                     // 距离(km)
    "elevation_gain": 186.0,             // 爬升(m)
    "elevation_loss": 0.0,               // 下降(m)
    "estimated_time": 2.5,               // 预计时间(小时)
    "difficulty": 3,                     // 难度等级(1-5)
    "terrain": "mountain",               // 地形类型
    "surface_type": "rock",              // 路面类型
    "traffic_level": 2,                  // 交通等级(0-5)
    "notes": "注意安全"                   // 备注
  }
]
```

#### 3.2.4 图片 (images)
```json
"images": [
  {
    "imageUrl": "https://example.com/image1.jpg",
    "isCover": true,                     // 是否为封面图片
    "sequenceNumber": 1                  // 序号，必须>0
  }
]
```

#### 3.2.5 标记点 (markerPoints)
```json
"markerPoints": [
  {
    "name": "观景台",
    "description": "最佳观景位置",
    "latitude": 34.4889,
    "longitude": 110.0892,
    "elevation": 1614.0,
    "marker_type": 0,                   // 标记类型: 0:景点,1:危险,2:休息
    "color": "#FF0000",                 // 标记颜色
    "icon_url": "图标URL",
    "sequenceNumber": 1                 // 序号，必须>0
  }
]
```

#### 3.2.6 日程计划 (dailyPlans)
```json
"dailyPlans": [
  {
    "title": "第一天：登顶北峰",
    "description": "从山门出发，登顶北峰",
    "dayNumber": 1,                     // 天数，必须>0
    "distance": 8.5,                   // 当日距离
    "elevation_gain": 468.0,           // 当日爬升
    "elevation_loss": 0.0,             // 当日下降
    "estimated_time": 6.0,             // 预计时间
    "notes": "早上6点出发"              // 备注
  }
]
```

#### 3.2.7 水源 (waterSources)
```json
"waterSources": [
  {
    "name": "山泉水",
    "description": "天然山泉，水质清澈",
    "latitude": 34.4889,
    "longitude": 110.0892,
    "elevation": 1614.0,
    "water_type": 0,                   // 水源类型: 0:天然,1:处理,2:瓶装,3:其他
    "water_quality": 1,                // 水质: 0:优质,1:良好,2:一般,3:较差,4:未知
    "reliability": 0.9,                // 可靠性(0.0-1.0)
    "requires_treatment": false        // 是否需要处理
  }
]
```

#### 3.2.8 补给点 (supplies)
```json
"supplies": [
  {
    "name": "山顶小卖部",
    "description": "提供食物和饮用水",
    "latitude": 34.4889,
    "longitude": 110.0892,
    "elevation": 1614.0,
    "supply_type": 0,                  // 补给类型: 0:商店,1:餐厅,2:自动售货机
    "price": 50.0,                     // 平均价格
    "last_verified": "user_id",        // 最后验证者
    "updated_by": "user_id"            // 更新者
  }
]
```

#### 3.2.9 营地 (campsites)
```json
"campsites": [
  {
    "name": "北峰露营区",
    "description": "可搭帐篷，有基础设施",
    "latitude": 34.4889,
    "longitude": 110.0892,
    "elevation": 1614.0,
    "campsite_type": 1,               // 营地类型: 0:官方,1:非官方,2:野营
    "capacity": 20,                   // 容量
    "notes": "需要预约"                // 备注
  }
]
```

#### 3.2.10 搭车联系人 (hitchhikeContacts)
```json
"hitchhikeContacts": [
  {
    "name": "华山交通服务",
    "description": "提供接送服务",
    "phone": "13800138000",
    "location": "华山游客中心",
    "price": 100.0,                   // 价格
    "verified": true                  // 是否已验证
  }
]
```

## 🎯 数据验证要求

### 4.1 字段类型验证

| 字段类型 | 验证规则 | 错误示例 | 正确示例 |
|---------|---------|----------|----------|
| 字符串 | 必须是string类型 | `123` | `"路线名称"` |
| 数字 | 必须是number类型 | `"123.45"` | `123.45` |
| 整数 | 必须是整数 | `3.14` | `3` |
| 布尔值 | 必须是boolean | `"true"` | `true` |
| 数组 | 必须是array类型 | `"tag1,tag2"` | `["tag1", "tag2"]` |

### 4.2 枚举字段范围

| 字段名 | 范围 | 说明 |
|--------|------|------|
| difficulty | 1-5 | 难度等级 |
| route_type | 0-3 | 路线类型 |
| route_direction | 0-360 | 路线方向 |
| status | 0-2 | 状态 |
| water_type | 0-3 | 水源类型 |
| water_quality | 0-4 | 水质等级 |
| traffic_level | 0-5 | 交通等级 |

### 4.3 特殊规则

| 字段 | 规则 | 说明 |
|------|------|------|
| sequence_number | > 0 | 序号必须大于0 |
| dayNumber | > 0 | 日程编号必须大于0 |
| reliability | 0.0-1.0 | 可靠性范围 |

## 🛠️ 完整创建示例

### 5.1 五台山顺朝路线示例

```json
{
  "name": "五台山顺朝徒步路线",
  "description": "五台山顺朝是佛教徒朝拜五台山的经典路线",
  "region": "山西省忻州市五台县",
  "difficulty": 4,
  "route_type": 1,
  "status": 1,
  "created_by": "wutaishan_guide",
  "tags": ["佛教朝圣", "五台山", "顺朝", "多日徒步"],
  "waypoints": [
    {
      "name": "鸿门岩",
      "description": "五台山顺朝起点",
      "latitude": 39.0167,
      "longitude": 113.5833,
      "elevation": 2400.0,
      "type": "start",
      "sequence_number": 1
    }
  ],
  "segments": [
    {
      "name": "鸿门岩至东台段",
      "distance": 8.5,
      "elevation_gain": 395.0,
      "estimated_time": 3.5,
      "difficulty": 3
    }
  ],
  "dailyPlans": [
    {
      "title": "第一天：鸿门岩至东台",
      "dayNumber": 1,
      "distance": 8.5,
      "elevation_gain": 395.0,
      "estimated_time": 3.5
    }
  ]
}
```

### 5.2 创建命令

```bash
# 1. 验证JSON数据
./scripts/validate_route.sh wutaishan_route.json

# 2. 创建完整路线
./scripts/create.sh wutaishan_route.json

# 3. 验证创建结果
curl -X GET "http://localhost:8080/walkbg/api/routes/{route_id}/details" | jq '.data | {
  segments: (.segments | length),
  waypoints: (.waypoints | length),
  tags: (.tags | length),
  images: (.image_urls | length),
  supplies: (.supplies | length),
  campsites: (.campsites | length),
  daily_plans: (.daily_plans | length),
  marker_points: (.marker_points | length),
  water_sources: (.water_sources | length),
  hitchhike_contacts: (.hitchhike_contacts | length)
}'
```

## ⚠️ 注意事项和常见问题

### 5.1 数据格式要求

| 字段 | 要求 | 错误示例 | 正确示例 |
|------|------|----------|----------|
| water_quality | 整数(0-4) | `"good"` | `1` |
| requires_treatment | 布尔值 | `"false"` | `false` |
| dayNumber | 大于0的整数 | `0` | `1` |
| sequenceNumber | 大于0的整数 | `0` | `1` |
| 经纬度 | 数字类型 | `"34.4889"` | `34.4889` |

### 5.2 外键约束问题

**问题**: Segment创建时引用不存在的Waypoint ID
```
FK36EM0FFRO2F5G8D76K9H4NM6V:
PUBLIC.SEGMENTS FOREIGN KEY(END_POINT_ID) REFERENCES PUBLIC.WAYPOINTS(ID)
```

**解决方案**:
- 使用分步保存策略
- 先保存Waypoint，再创建Segment
- 使用对象引用而非ID引用

### 5.3 字段映射问题

**常见错误**:
- JSON字段名与DTO字段名不匹配
- 忽略@JsonProperty注解的字段名
- 验证注解约束不满足

**解决方法**:
- 检查DTO定义中的@JsonProperty注解
- 确保JSON字段名与注解一致
- 满足所有验证约束条件

### 5.4 常见错误码

| HTTP状态码 | 原因 | 解决方法 |
|------------|------|----------|
| 400 | 参数验证失败 | 检查数据格式和必填字段 |
| 404 | 创建者用户不存在 | 先创建用户或使用有效用户ID |
| 500 | 服务器内部错误 | 检查外键约束和数据完整性 |

## 🎯 最佳实践

### 6.1 开发建议

1. **使用脚本**: 推荐使用提供的脚本进行测试和开发
2. **数据验证**: 创建前必须验证所有字段和格式
3. **分步创建**: 复杂路线建议分步创建，先基础信息后关联对象
4. **错误处理**: 注意查看HTTP状态码和错误信息
5. **性能考虑**: 大量关联对象时考虑分批处理

### 6.2 数据设计原则

1. **唯一性**: 确保ID字段的唯一性
2. **完整性**: 必填字段不能为空
3. **一致性**: 相关数据保持逻辑一致
4. **规范性**: 遵循字段格式要求

### 6.3 测试策略

1. **单元测试**: 测试每个关联对象的创建
2. **集成测试**: 测试完整路线创建流程
3. **边界测试**: 测试极限值和异常情况
4. **性能测试**: 测试大量数据的处理能力

## 🚀 快速开始

### 7.1 5分钟快速体验

```bash
# 1. 启动应用
mvn spring-boot:run

# 2. 验证JSON数据
./scripts/validate_route.sh my_route.json

# 3. 创建完整路线
./scripts/create.sh my_route.json

# 4. 验证结果
curl "http://localhost:8080/walkbg/api/routes" | jq '.data.totalElements'
```

### 7.2 自定义路线创建

```bash
# 1. 复制示例JSON
cp wutaishan_correct.json my_route.json

# 2. 修改路线数据
vim my_route.json

# 3. 验证数据格式
./scripts/validate_route.sh my_route.json

# 4. 创建路线
./scripts/create.sh my_route.json
```

## 📊 系统能力总览

### 支持的关联对象类型

| 类型 | 数量限制 | 主要用途 | 验证状态 |
|------|----------|----------|---------|
| Tags | 20个 | 路线分类和搜索 | ✅ |
| Waypoints | 100个 | 路线关键节点 | ✅ |
| Segments | 50个 | 路线分段信息 | ✅ |
| Images | 20张 | 路线图片展示 | ✅ |
| MarkerPoints | 100个 | 特殊标记点 | ✅ |
| DailyPlans | 30个 | 日程安排 | ✅ |
| WaterSources | 50个 | 水源信息 | ✅ |
| Supplies | 50个 | 补给点信息 | ✅ |
| Campsites | 30个 | 营地信息 | ✅ |
| HitchhikeContacts | 20个 | 搭车联系人 | ✅ |

### 验证脚本能力

- **类型检查**: 字符串、数字、整数、布尔值、数组
- **枚举验证**: 所有枚举字段的范围检查
- **特殊规则**: sequence_number > 0, dayNumber > 0 等
- **错误定位**: 精确到字段级别的错误报告
- **性能优化**: 快速验证，早期失败

---

**文档版本**: v4.0
**最后更新**: 2024-06-17
**维护者**: WalkBG开发团队
