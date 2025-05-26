# 徒步规划助手 API 文档

## 1. 概述

徒步规划助手是一个帮助用户规划徒步旅行的应用程序。本文档详细描述了徒步规划助手的API接口，包括路线管理和装备管家两个核心模块。

## 2. 基础信息

- **基础URL**: `http://localhost:8080/walkbg`
- **API版本**: v1
- **响应格式**: JSON
- **认证方式**: 暂无认证要求

## 3. 通用响应格式

所有API响应都遵循以下格式：

\`\`\`json
{
  "code": 200,
  "message": "success",
  "data": { ... }
}
\`\`\`

### 状态码说明

| 状态码 | 说明 |
|--------|------|
| 200 | 请求成功 |
| 400 | 请求参数错误 |
| 404 | 资源不存在 |
| 500 | 服务器内部错误 |

## 4. 路线管理模块

### 4.1 数据结构

#### 4.1.1 路线模型 (Route)

\`\`\`json
{
  "id": "string",
  "name": "string",
  "description": "string",
  "region": "string",
  "distance": "double",
  "duration": "string",
  "difficulty": "integer",
  "popularity": "integer",
  "seasons": ["string"],
  "tags": ["string"],
  "waypoints": [
    {
      "id": "string",
      "name": "string",
      "latitude": "double",
      "longitude": "double",
      "elevation": "double",
      "description": "string",
      "type": "string",
      "sequenceNumber": "integer"
    }
  ],
  "createdAt": "timestamp",
  "updatedAt": "timestamp"
}
\`\`\`

### 4.2 API接口

#### 4.2.1 获取所有路线

**请求**:
\`\`\`
GET /api/routes
\`\`\`

**响应示例**:
\`\`\`json
{
  "code": 200,
  "message": "success",
  "data": [
    {
      "id": "route123",
      "name": "黄山经典徒步路线",
      "description": "这条路线带您游览黄山最著名的景点，包括迎客松、光明顶和西海大峡谷。",
      "region": "黄山风景区",
      "distance": 15.5,
      "duration": "8小时",
      "difficulty": 2,
      "popularity": 10,
      "seasons": ["春季", "秋季"],
      "tags": ["山岳", "森林"],
      "createdAt": "2023-05-15T08:30:00Z",
      "updatedAt": "2023-05-15T08:30:00Z"
    }
  ]
}
\`\`\`

#### 4.2.2 获取路线详情

**请求**:
\`\`\`
GET /api/routes/{id}
\`\`\`

**路径参数**:

| 参数名 | 类型 | 必填 | 描述 |
|--------|------|------|------|
| id | string | 是 | 路线ID |

**响应示例**:
\`\`\`json
{
  "code": 200,
  "message": "success",
  "data": {
    "id": "route123",
    "name": "黄山经典徒步路线",
    "description": "这条路线带您游览黄山最著名的景点，包括迎客松、光明顶和西海大峡谷。",
    "region": "黄山风景区",
    "distance": 15.5,
    "duration": "8小时",
    "difficulty": 2,
    "popularity": 10,
    "seasons": ["春季", "秋季"],
    "tags": ["山岳", "森林"],
    "waypoints": [
      {
        "id": "wp1",
        "name": "迎客松",
        "latitude": 30.1234,
        "longitude": 118.1234,
        "elevation": 1500,
        "description": "黄山标志性景点",
        "type": "SCENIC",
        "sequenceNumber": 1
      }
    ],
    "createdAt": "2023-05-15T08:30:00Z",
    "updatedAt": "2023-05-15T08:30:00Z"
  }
}
\`\`\`

#### 4.2.3 按名称搜索路线

**请求**:
\`\`\`
GET /api/routes/search/name?name={name}
\`\`\`

**查询参数**:

| 参数名 | 类型 | 必填 | 描述 |
|--------|------|------|------|
| name | string | 是 | 路线名称关键词 |

#### 4.2.4 按地区搜索路线

**请求**:
\`\`\`
GET /api/routes/search/region?region={region}
\`\`\`

**查询参数**:

| 参数名 | 类型 | 必填 | 描述 |
|--------|------|------|------|
| region | string | 是 | 地区名称 |

#### 4.2.5 按标签搜索路线

**请求**:
\`\`\`
GET /api/routes/search/tag?tag={tag}
\`\`\`

**查询参数**:

| 参数名 | 类型 | 必填 | 描述 |
|--------|------|------|------|
| tag | string | 是 | 标签名称 |

#### 4.2.6 按季节搜索路线

**请求**:
\`\`\`
GET /api/routes/search/season?season={season}
\`\`\`

**查询参数**:

| 参数名 | 类型 | 必填 | 描述 |
|--------|------|------|------|
| season | string | 是 | 季节名称 |

#### 4.2.7 按难度范围搜索路线

**请求**:
\`\`\`
GET /api/routes/search/difficulty/max?difficulty={difficulty}
GET /api/routes/search/difficulty/min?difficulty={difficulty}
\`\`\`

**查询参数**:

| 参数名 | 类型 | 必填 | 描述 |
|--------|------|------|------|
| difficulty | integer | 是 | 难度值 |

#### 4.2.8 按距离范围搜索路线

**请求**:
\`\`\`
GET /api/routes/search/distance?minDistance={minDistance}&maxDistance={maxDistance}
\`\`\`

**查询参数**:

| 参数名 | 类型 | 必填 | 描述 |
|--------|------|------|------|
| minDistance | double | 是 | 最小距离(km) |
| maxDistance | double | 是 | 最大距离(km) |

#### 4.2.9 获取热门路线

**请求**:
\`\`\`
GET /api/routes/popular?minPopularity={minPopularity}
\`\`\`

**查询参数**:

| 参数名 | 类型 | 必填 | 描述 |
|--------|------|------|------|
| minPopularity | integer | 否 | 最小热度值，默认为0 |

#### 4.2.10 获取前10热门路线

**请求**:
\`\`\`
GET /api/routes/top10
\`\`\`

#### 4.2.11 创建路线

**请求**:
\`\`\`
POST /api/routes
\`\`\`

**请求体**:
\`\`\`json
{
  "name": "黄山经典徒步路线",
  "description": "这条路线带您游览黄山最著名的景点，包括迎客松、光明顶和西海大峡谷。",
  "region": "黄山风景区",
  "distance": 15.5,
  "duration": "8小时",
  "difficulty": 2
}
\`\`\`

#### 4.2.12 更新路线

**请求**:
\`\`\`
PUT /api/routes/{id}
\`\`\`

**路径参数**:

| 参数名 | 类型 | 必填 | 描述 |
|--------|------|------|------|
| id | string | 是 | 路线ID |

**请求体**:
\`\`\`json
{
  "name": "更新后的路线名称",
  "description": "更新后的路线描述",
  "region": "更新后的地区",
  "distance": 16.5,
  "duration": "9小时",
  "difficulty": 3
}
\`\`\`

#### 4.2.13 删除路线

**请求**:
\`\`\`
DELETE /api/routes/{id}
\`\`\`

**路径参数**:

| 参数名 | 类型 | 必填 | 描述 |
|--------|------|------|------|
| id | string | 是 | 路线ID |

#### 4.2.14 增加路线热度

**请求**:
\`\`\`
POST /api/routes/{id}/popularity
\`\`\`

**路径参数**:

| 参数名 | 类型 | 必填 | 描述 |
|--------|------|------|------|
| id | string | 是 | 路线ID |

## 5. 装备管家模块

### 5.1 数据结构

#### 5.1.1 装备清单模型 (EquipmentList)

\`\`\`json
{
  "id": "string",
  "name": "string",
  "description": "string",
  "type": "enum",
  "routeId": "string?",
  "routeName": "string?",
  "tripId": "string?",
  "tripDays": "integer",
  "personCount": "integer",
  "seasons": ["enum"],
  "equipments": ["EquipmentItem"],
  "totalWeight": "double",
  "baseWeight": "double",
  "consumableWeight": "double",
  "wornWeight": "double",
  "creatorId": "string",
  "creatorName": "string",
  "tags": ["string"],
  "isOfficial": "boolean",
  "isTemplate": "boolean",
  "templateId": "string?",
  "status": "enum",
  "lastUsedAt": "timestamp?",
  "createdAt": "timestamp",
  "updatedAt": "timestamp"
}
\`\`\`

#### 5.1.2 装备项目模型 (EquipmentItem)

\`\`\`json
{
  "id": "string",
  "name": "string",
  "category": "enum",
  "description": "string?",
  "weight": "double",
  "weightUnit": "enum",
  "quantity": "integer",
  "necessity": "enum",
  "prepared": "boolean",
  "isOwned": "boolean",
  "isShared": "boolean",
  "sharedPersonCount": "integer?",
  "brand": "string?",
  "model": "string?",
  "price": "double?",
  "condition": "enum?",
  "imageUrl": "string?",
  "notes": "string?"
}
\`\`\`

#### 5.1.3 装备模板模型 (EquipmentTemplate)

\`\`\`json
{
  "id": "string",
  "name": "string",
  "description": "string",
  "type": "enum",
  "seasons": ["enum"],
  "equipments": ["EquipmentItem"],
  "tags": ["string"],
  "isOfficial": "boolean",
  "creatorId": "string",
  "creatorName": "string",
  "usageCount": "integer",
  "rating": "double",
  "createdAt": "timestamp",
  "updatedAt": "timestamp"
}
\`\`\`

#### 5.1.4 用户装备库模型 (UserEquipmentInventory)

\`\`\`json
{
  "userId": "string",
  "equipments": ["EquipmentItem"],
  "lastUpdatedAt": "timestamp",
  "statistics": {
    "totalItems": "integer",
    "totalValue": "double",
    "categoryDistribution": [
      {
        "category": "string",
        "count": "integer",
        "value": "double"
      }
    ],
    "conditionDistribution": [
      {
        "condition": "string",
        "count": "integer"
      }
    ]
  }
}
\`\`\`

### 5.2 枚举类型

#### 5.2.1 装备清单类型 (EquipmentListType)

\`\`\`
SHORT_HIKE      - 短途徒步（1-3天）
LONG_HIKE       - 长途徒步（4天以上）
CAMPING         - 露营
MOUNTAINEERING  - 登山
TREKKING        - 穿越
CUSTOM          - 自定义
\`\`\`

#### 5.2.2 装备清单状态 (EquipmentListStatus)

\`\`\`
PLANNING   - 规划中
PREPARING  - 准备中
READY      - 已完成准备
IN_USE     - 使用中
COMPLETED  - 已完成
ARCHIVED   - 已归档
\`\`\`

#### 5.2.3 装备分类 (EquipmentCategory)

\`\`\`
SHELTER      - 住宿装备（帐篷、睡袋、睡垫等）
FOOD         - 饮食装备（炉具、餐具、水壶等）
CLOTHING     - 保暖装备（衣物、手套、帽子等）
BACKPACK     - 背包装备（背包、防雨罩等）
NAVIGATION   - 导航装备（地图、指南针、GPS等）
LIGHTING     - 照明装备（头灯、手电筒等）
FIRST_AID    - 急救装备（急救包、药品等）
TOOLS        - 工具装备（刀具、绳索、修理工具等）
ELECTRONICS  - 电子装备（手机、相机、充电宝等）
PERSONAL_CARE - 个人护理（洗漱用品、防晒用品等）
OTHER        - 其他装备
\`\`\`

#### 5.2.4 装备必要性 (EquipmentNecessity)

\`\`\`
ESSENTIAL   - 必需
RECOMMENDED - 推荐
OPTIONAL    - 可选
\`\`\`

#### 5.2.5 装备使用状态 (EquipmentCondition)

\`\`\`
NEW      - 全新
GOOD     - 良好
FAIR     - 一般
POOR     - 较差
DAMAGED  - 损坏
\`\`\`

#### 5.2.6 季节适用性 (SeasonSuitability)

\`\`\`
SPRING     - 春季
SUMMER     - 夏季
AUTUMN     - 秋季
WINTER     - 冬季
ALL_SEASONS - 四季
\`\`\`

#### 5.2.7 重量单位 (WeightUnit)

\`\`\`
GRAM     - 克
KILOGRAM - 千克
POUND    - 磅
OUNCE    - 盎司
\`\`\`

### 5.3 API接口

#### 5.3.1 装备清单接口

##### 5.3.1.1 获取用户装备清单列表

**请求**:
\`\`\`
GET /api/equipment-lists?userId={userId}&status={status}&type={type}&season={season}&search={search}&page={page}&pageSize={pageSize}&sortBy={sortBy}&sortOrder={sortOrder}
\`\`\`

**查询参数**:

| 参数名 | 类型 | 必填 | 描述 |
|--------|------|------|------|
| userId | string | 是 | 用户ID |
| status | string | 否 | 清单状态筛选 |
| type | string | 否 | 清单类型筛选 |
| season | string | 否 | 季节筛选 |
| search | string | 否 | 搜索关键词 |
| page | integer | 否 | 页码，默认1 |
| pageSize | integer | 否 | 每页数量，默认20 |
| sortBy | string | 否 | 排序字段，默认createdAt |
| sortOrder | string | 否 | 排序方式，asc或desc，默认desc |

**响应示例**:
\`\`\`json
{
  "code": 200,
  "message": "success",
  "data": {
    "total": 10,
    "page": 1,
    "pageSize": 20,
    "lists": [
      {
        "id": "list123",
        "name": "春季徒步基础装备",
        "description": "适合春季短途徒步的基础装备清单",
        "type": "SHORT_HIKE",
        "routeName": "莫干山徒步路线",
        "tripDays": 2,
        "personCount": 1,
        "seasons": ["SPRING"],
        "totalWeight": 5600,
        "totalItems": 15,
        "status": "PLANNING",
        "isOfficial": false,
        "createdAt": "2023-04-15T08:30:00Z",
        "updatedAt": "2023-04-15T08:30:00Z"
      }
    ]
  }
}
\`\`\`

##### 5.3.1.2 获取装备清单详情

**请求**:
\`\`\`
GET /api/equipment-lists/{listId}
\`\`\`

**路径参数**:

| 参数名 | 类型 | 必填 | 描述 |
|--------|------|------|------|
| listId | string | 是 | 装备清单ID |

**响应示例**:
\`\`\`json
{
  "code": 200,
  "message": "success",
  "data": {
    "id": "list123",
    "name": "春季徒步基础装备",
    "description": "适合春季短途徒步的基础装备清单",
    "type": "SHORT_HIKE",
    "routeId": "route123",
    "routeName": "莫干山徒步路线",
    "tripDays": 2,
    "personCount": 1,
    "seasons": ["SPRING"],
    "equipments": [
      {
        "id": "item123",
        "name": "徒步鞋",
        "category": "CLOTHING",
        "weight": 800,
        "weightUnit": "GRAM",
        "quantity": 1,
        "necessity": "ESSENTIAL",
        "prepared": true,
        "isOwned": true,
        "isShared": false,
        "brand": "Salomon",
        "model": "X Ultra 3",
        "notes": "已准备好"
      }
    ],
    "totalWeight": 5600,
    "baseWeight": 3200,
    "consumableWeight": 1800,
    "wornWeight": 600,
    "creatorId": "user123",
    "creatorName": "张三",
    "tags": ["短途", "春季", "入门"],
    "isOfficial": false,
    "isTemplate": false,
    "status": "PREPARING",
    "createdAt": "2023-04-15T08:30:00Z",
    "updatedAt": "2023-04-15T08:30:00Z"
  }
}
\`\`\`

##### 5.3.1.3 创建装备清单

**请求**:
\`\`\`
POST /api/equipment-lists?userId={userId}&userName={userName}
\`\`\`

**查询参数**:

| 参数名 | 类型 | 必填 | 描述 |
|--------|------|------|------|
| userId | string | 是 | 用户ID |
| userName | string | 是 | 用户名称 |

**请求体**:
\`\`\`json
{
  "name": "春季徒步基础装备",
  "description": "适合春季短途徒步的基础装备清单",
  "type": "SHORT_HIKE",
  "routeId": "route123",
  "routeName": "莫干山徒步路线",
  "tripDays": 2,
  "personCount": 1,
  "seasons": ["SPRING"],
  "equipments": [
    {
      "name": "徒步鞋",
      "category": "CLOTHING",
      "description": "防水透气徒步鞋",
      "weight": 800,
      "quantity": 1,
      "necessity": "ESSENTIAL"
    }
  ],
  "tags": ["短途", "春季", "入门"]
}
\`\`\`

**响应示例**:
\`\`\`json
{
  "code": 200,
  "message": "装备清单创建成功",
  "data": {
    "id": "list123",
    "name": "春季徒步基础装备",
    "description": "适合春季短途徒步的基础装备清单",
    "type": "SHORT_HIKE",
    "routeId": "route123",
    "routeName": "莫干山徒步路线",
    "tripDays": 2,
    "personCount": 1,
    "seasons": ["SPRING"],
    "equipments": [
      {
        "id": "item123",
        "name": "徒步鞋",
        "category": "CLOTHING",
        "description": "防水透气徒步鞋",
        "weight": 800,
        "quantity": 1,
        "necessity": "ESSENTIAL",
        "prepared": false,
        "isOwned": false,
        "isShared": false
      }
    ],
    "totalWeight": 800,
    "baseWeight": 800,
    "consumableWeight": 0,
    "wornWeight": 0,
    "creatorId": "user123",
    "creatorName": "张三",
    "tags": ["短途", "春季", "入门"],
    "isOfficial": false,
    "isTemplate": false,
    "status": "PLANNING",
    "createdAt": "2023-04-15T08:30:00Z",
    "updatedAt": "2023-04-15T08:30:00Z"
  }
}
\`\`\`

##### 5.3.1.4 从模板创建装备清单

**请求**:
\`\`\`
POST /api/equipment-lists/from-template?userId={userId}&userName={userName}
\`\`\`

**查询参数**:

| 参数名 | 类型 | 必填 | 描述 |
|--------|------|------|------|
| userId | string | 是 | 用户ID |
| userName | string | 是 | 用户名称 |

**请求体**:
\`\`\`json
{
  "templateId": "template123",
  "name": "我的春季徒步装备",
  "description": "基于官方模板修改的春季徒步装备清单",
  "routeId": "route123",
  "routeName": "莫干山徒步路线",
  "tripDays": 2,
  "personCount": 1
}
\`\`\`

##### 5.3.1.5 更新装备清单

**请求**:
\`\`\`
PUT /api/equipment-lists/{listId}
\`\`\`

**路径参数**:

| 参数名 | 类型 | 必填 | 描述 |
|--------|------|------|------|
| listId | string | 是 | 装备清单ID |

**请求体**:
\`\`\`json
{
  "name": "更新后的装备清单名称",
  "description": "更新后的描述",
  "type": "SHORT_HIKE",
  "routeId": "route123",
  "routeName": "莫干山徒步路线",
  "tripDays": 3,
  "personCount": 2,
  "seasons": ["SPRING", "SUMMER"],
  "tags": ["短途", "春季", "入门"],
  "status": "PREPARING"
}
\`\`\`

##### 5.3.1.6 删除装备清单

**请求**:
\`\`\`
DELETE /api/equipment-lists/{listId}
\`\`\`

**路径参数**:

| 参数名 | 类型 | 必填 | 描述 |
|--------|------|------|------|
| listId | string | 是 | 装备清单ID |

##### 5.3.1.7 获取装备清单统计数据

**请求**:
\`\`\`
GET /api/equipment-lists/{listId}/stats
\`\`\`

**路径参数**:

| 参数名 | 类型 | 必填 | 描述 |
|--------|------|------|------|
| listId | string | 是 | 装备清单ID |

**响应示例**:
\`\`\`json
{
  "code": 200,
  "message": "success",
  "data": {
    "totalWeight": 5600,
    "baseWeight": 3200,
    "consumableWeight": 1800,
    "wornWeight": 600,
    "weightPerPersonPerDay": 2800,
    "totalItems": 15,
    "essentialItems": 8,
    "recommendedItems": 5,
    "optionalItems": 2,
    "preparedItems": 10,
    "preparationPercentage": 66.7,
    "categoryDistribution": [
      {
        "category": "SHELTER",
        "count": 3,
        "weight": 1500
      }
    ],
    "heaviestItems": [
      {
        "id": "item123",
        "name": "帐篷",
        "category": "SHELTER",
        "weight": 2000,
        "necessity": "ESSENTIAL"
      }
    ]
  }
}
\`\`\`

#### 5.3.2 装备项目接口

##### 5.3.2.1 添加装备项目到清单

**请求**:
\`\`\`
POST /api/equipment-lists/{listId}/items
\`\`\`

**路径参数**:

| 参数名 | 类型 | 必填 | 描述 |
|--------|------|------|------|
| listId | string | 是 | 装备清单ID |

**请求体**:
\`\`\`json
{
  "name": "徒步鞋",
  "category": "CLOTHING",
  "description": "防水透气徒步鞋",
  "weight": 800,
  "weightUnit": "GRAM",
  "quantity": 1,
  "necessity": "ESSENTIAL",
  "brand": "Salomon",
  "model": "X Ultra 3",
  "price": 899,
  "isOwned": true,
  "isShared": false,
  "condition": "GOOD",
  "notes": "需要提前穿几次，避免起泡"
}
\`\`\`

##### 5.3.2.2 更新装备项目

**请求**:
\`\`\`
PUT /api/equipment-lists/{listId}/items/{itemId}
\`\`\`

**路径参数**:

| 参数名 | 类型 | 必填 | 描述 |
|--------|------|------|------|
| listId | string | 是 | 装备清单ID |
| itemId | string | 是 | 装备项目ID |

**请求体**:
\`\`\`json
{
  "name": "徒步鞋",
  "category": "CLOTHING",
  "description": "防水透气徒步鞋",
  "weight": 850,
  "quantity": 1,
  "necessity": "ESSENTIAL",
  "prepared": true,
  "brand": "Salomon",
  "model": "X Ultra 3 GTX",
  "price": 999,
  "notes": "已准备好"
}
\`\`\`

##### 5.3.2.3 删除装备项目

**请求**:
\`\`\`
DELETE /api/equipment-lists/{listId}/items/{itemId}
\`\`\`

**路径参数**:

| 参数名 | 类型 | 必填 | 描述 |
|--------|------|------|------|
| listId | string | 是 | 装备清单ID |
| itemId | string | 是 | 装备项目ID |

##### 5.3.2.4 批量更新装备准备状态

**请求**:
\`\`\`
PUT /api/equipment-lists/{listId}/items/preparation
\`\`\`

**路径参数**:

| 参数名 | 类型 | 必填 | 描述 |
|--------|------|------|------|
| listId | string | 是 | 装备清单ID |

**请求体**:
\`\`\`json
{
  "items": [
    {
      "id": "item123",
      "prepared": true
    },
    {
      "id": "item124",
      "prepared": false
    }
  ]
}
\`\`\`

#### 5.3.3 装备模板接口

##### 5.3.3.1 获取装备模板列表

**请求**:
\`\`\`
GET /api/equipment-templates?type={type}&season={season}&isOfficial={isOfficial}&search={search}&page={page}&pageSize={pageSize}&sortBy={sortBy}&sortOrder={sortOrder}
\`\`\`

**查询参数**:

| 参数名 | 类型 | 必填 | 描述 |
|--------|------|------|------|
| type | string | 否 | 模板类型筛选 |
| season | string | 否 | 季节筛选 |
| isOfficial | boolean | 否 | 是否官方模板 |
| search | string | 否 | 搜索关键词 |
| page | integer | 否 | 页码，默认1 |
| pageSize | integer | 否 | 每页数量，默认20 |
| sortBy | string | 否 | 排序字段，默认usageCount |
| sortOrder | string | 否 | 排序方式，asc或desc，默认desc |

**响应示例**:
\`\`\`json
{
  "code": 200,
  "message": "success",
  "data": {
    "total": 8,
    "page": 1,
    "pageSize": 20,
    "templates": [
      {
        "id": "template123",
        "name": "官方春季短途徒步装备",
        "description": "适合春季1-3天短途徒步的基础装备清单",
        "type": "SHORT_HIKE",
        "seasons": ["SPRING"],
        "isOfficial": true,
        "usageCount": 1250,
        "rating": 4.8,
        "createdAt": "2023-01-15T08:30:00Z"
      }
    ]
  }
}
\`\`\`

##### 5.3.3.2 获取装备模板详情

**请求**:
\`\`\`
GET /api/equipment-templates/{templateId}
\`\`\`

**路径参数**:

| 参数名 | 类型 | 必填 | 描述 |
|--------|------|------|------|
| templateId | string | 是 | 模板ID |

##### 5.3.3.3 创建装备模板

**请求**:
\`\`\`
POST /api/equipment-templates?userId={userId}&userName={userName}
\`\`\`

**查询参数**:

| 参数名 | 类型 | 必填 | 描述 |
|--------|------|------|------|
| userId | string | 是 | 用户ID |
| userName | string | 是 | 用户名称 |

**请求体**:
\`\`\`json
{
  "name": "我的春季徒步装备模板",
  "description": "适合春季短途徒步的个人装备模板",
  "type": "SHORT_HIKE",
  "seasons": ["SPRING"],
  "equipments": [
    {
      "name": "徒步鞋",
      "category": "CLOTHING",
      "weight": 800,
      "quantity": 1,
      "necessity": "ESSENTIAL"
    }
  ],
  "tags": ["短途", "春季", "个人"]
}
\`\`\`

##### 5.3.3.4 从装备清单创建模板

**请求**:
\`\`\`
POST /api/equipment-templates/from-list?userId={userId}&userName={userName}
\`\`\`

**查询参数**:

| 参数名 | 类型 | 必填 | 描述 |
|--------|------|------|------|
| userId | string | 是 | 用户ID |
| userName | string | 是 | 用户名称 |

**请求体**:
\`\`\`json
{
  "listId": "list123",
  "name": "我的春季徒步装备模板",
  "description": "基于我的装备清单创建的模板",
  "tags": ["短途", "春季", "个人"]
}
\`\`\`

#### 5.3.4 用户装备库接口

##### 5.3.4.1 获取用户装备库

**请求**:
\`\`\`
GET /api/user-equipment-inventory?userId={userId}&category={category}&condition={condition}&search={search}
\`\`\`

**查询参数**:

| 参数名 | 类型 | 必填 | 描述 |
|--------|------|------|------|
| userId | string | 是 | 用户ID |
| category | string | 否 | 分类筛选 |
| condition | string | 否 | 状态筛选 |
| search | string | 否 | 搜索关键词 |

**响应示例**:
\`\`\`json
{
  "code": 200,
  "message": "success",
  "data": {
    "userId": "user123",
    "lastUpdatedAt": "2023-04-15T08:30:00Z",
    "equipments": [
      {
        "id": "item123",
        "name": "徒步鞋",
        "category": "CLOTHING",
        "weight": 800,
        "brand": "Salomon",
        "model": "X Ultra 3",
        "condition": "GOOD",
        "purchaseDate": "2022-05-10T00:00:00Z",
        "usageCount": 12
      }
    ],
    "statistics": {
      "totalItems": 45,
      "totalValue": 12500,
      "categoryDistribution": [
        {
          "category": "CLOTHING",
          "count": 15,
          "value": 5600
        }
      ],
      "conditionDistribution": [
        {
          "condition": "GOOD",
          "count": 30
        }
      ]
    }
  }
}
\`\`\`

##### 5.3.4.2 添加装备到用户装备库

**请求**:
\`\`\`
POST /api/user-equipment-inventory/items?userId={userId}
\`\`\`

**查询参数**:

| 参数名 | 类型 | 必填 | 描述 |
|--------|------|------|------|
| userId | string | 是 | 用户ID |

**请求体**:
\`\`\`json
{
  "name": "徒步鞋",
  "category": "CLOTHING",
  "description": "防水透气徒步鞋",
  "weight": 800,
  "weightUnit": "GRAM",
  "brand": "Salomon",
  "model": "X Ultra 3",
  "price": 899,
  "purchaseDate": "2022-05-10T00:00:00Z",
  "purchaseLink": "https://example.com/shop/shoes/123",
  "condition": "GOOD",
  "usageCount": 12,
  "imageUrl": "https://example.com/images/shoes/123.jpg",
  "notes": "非常舒适的徒步鞋"
}
\`\`\`

## 6. 错误处理

所有API在遇到错误时都会返回相应的错误信息，格式如下：

\`\`\`json
{
  "code": 400,
  "message": "请求参数错误",
  "data": null
}
\`\`\`

## 7. 版本控制

当前API版本为v1，未来版本更新将通过URL路径中的版本号进行区分，例如：`/api/v2/routes`。

## 8. 安全性

目前API不需要认证，未来将添加基于JWT的认证机制。

## 9. 限流策略

为保证服务质量，API接口有以下限流策略：
- 每个IP每分钟最多60次请求
- 每个用户每分钟最多100次请求

## 10. 附录

### 10.1 状态码列表

| 状态码 | 说明 |
|--------|------|
| 200 | 请求成功 |
| 201 | 资源创建成功 |
| 204 | 请求成功，无返回内容 |
| 400 | 请求参数错误 |
| 401 | 未授权 |
| 403 | 禁止访问 |
| 404 | 资源不存在 |
| 409 | 资源冲突 |
| 429 | 请求过于频繁 |
| 500 | 服务器内部错误 |

### 10.2 常见问题

1. **如何处理分页请求？**
   - 使用page和pageSize参数控制分页
   - 响应中会包含total、page和pageSize字段

2. **如何处理排序请求？**
   - 使用sortBy和sortOrder参数控制排序
   - sortOrder可选值为asc（升序）和desc（降序）

3. **如何处理搜索请求？**
   - 使用search参数进行关键词搜索
   - 搜索范围包括名称和描述字段