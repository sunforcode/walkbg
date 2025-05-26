# 装备管家模块开发文档

## 1. 概述

装备管家是徒步规划助手应用的核心功能模块，用于帮助用户管理徒步装备、创建装备清单、分析装备重量分布，并提供智能推荐。本文档详细描述了装备管家模块的数据结构和API接口设计，供前后端开发人员参考。

## 2. 数据结构

### 2.1 核心数据模型

#### 2.1.1 装备清单模型 (EquipmentList)

\`\`\`json
{
  "id": "string",                  // 唯一标识符
  "name": "string",                // 清单名称
  "description": "string",         // 清单描述
  "type": "enum",                  // 清单类型: shortHike, longHike, camping, mountaineering, trekking, custom
  "routeId": "string?",            // 关联路线ID (可选)
  "routeName": "string?",          // 关联路线名称 (可选)
  "tripId": "string?",             // 关联行程ID (可选)
  "tripDays": "integer",           // 行程天数
  "personCount": "integer",        // 适用人数
  "seasons": ["enum"],             // 适用季节: spring, summer, autumn, winter, allSeasons
  "equipments": ["EquipmentItem"], // 装备项目列表
  "totalWeight": "double",         // 总重量(g)
  "baseWeight": "double",          // 基础重量(g)
  "consumableWeight": "double",    // 消耗品重量(g)
  "wornWeight": "double",          // 穿着重量(g)
  "creatorId": "string",           // 创建者ID
  "creatorName": "string",         // 创建者名称
  "tags": ["string"],              // 标签列表
  "isOfficial": "boolean",         // 是否官方推荐
  "isTemplate": "boolean",         // 是否为模板
  "templateId": "string?",         // 模板ID (如果是从模板创建)
  "status": "enum",                // 状态: planning, preparing, ready, inUse, completed, archived
  "lastUsedAt": "timestamp?",      // 最后使用时间 (可选)
  "createdAt": "timestamp",        // 创建时间
  "updatedAt": "timestamp"         // 更新时间
}
\`\`\`

#### 2.1.2 装备项目模型 (EquipmentItem)

\`\`\`json
{
  "id": "string",                  // 唯一标识符
  "name": "string",                // 装备名称
  "category": "enum",              // 分类: shelter, food, clothing, backpack, navigation, lighting, firstAid, tools, electronics, personalCare, other
  "description": "string?",        // 描述 (可选)
  "weight": "double",              // 重量(g)
  "weightUnit": "enum",            // 重量单位: gram, kilogram, pound, ounce
  "quantity": "integer",           // 数量
  "necessity": "enum",             // 必要性: essential, recommended, optional
  "prepared": "boolean",           // 是否已准备
  "isOwned": "boolean",            // 是否拥有
  "isShared": "boolean",           // 是否共享装备
  "sharedPersonCount": "integer?", // 共享人数 (可选)
  "brand": "string?",              // 品牌 (可选)
  "model": "string?",              // 型号 (可选)
  "price": "double?",              // 价格 (可选)
  "purchaseLink": "string?",       // 购买链接 (可选)
  "purchaseDate": "timestamp?",    // 购买日期 (可选)
  "usageCount": "integer",         // 使用次数
  "condition": "enum",             // 使用状态: new, good, fair, poor, damaged
  "alternativeIds": ["string"],    // 替代品列表
  "imageUrl": "string?",           // 装备图片URL (可选)
  "notes": "string?",              // 备注 (可选)
  "createdAt": "timestamp",        // 创建时间
  "updatedAt": "timestamp"         // 更新时间
}
\`\`\`

#### 2.1.3 装备模板模型 (EquipmentTemplate)

\`\`\`json
{
  "id": "string",                  // 唯一标识符
  "name": "string",                // 模板名称
  "description": "string",         // 模板描述
  "type": "enum",                  // 模板类型: shortHike, longHike, camping, mountaineering, trekking, custom
  "seasons": ["enum"],             // 适用季节: spring, summer, autumn, winter, allSeasons
  "equipments": ["EquipmentItem"], // 装备项目列表
  "tags": ["string"],              // 标签列表
  "isOfficial": "boolean",         // 是否官方模板
  "creatorId": "string",           // 创建者ID
  "creatorName": "string",         // 创建者名称
  "usageCount": "integer",         // 使用次数
  "rating": "double",              // 评分 (1-5)
  "createdAt": "timestamp",        // 创建时间
  "updatedAt": "timestamp"         // 更新时间
}
\`\`\`

#### 2.1.4 用户装备库模型 (UserEquipmentInventory)

\`\`\`json
{
  "userId": "string",              // 用户ID
  "equipments": ["EquipmentItem"], // 用户拥有的装备列表
  "lastUpdatedAt": "timestamp"     // 最后更新时间
}
\`\`\`

### 2.2 枚举类型

#### 2.2.1 装备清单类型 (EquipmentListType)

\`\`\`
shortHike      - 短途徒步（1-3天）
longHike       - 长途徒步（4天以上）
camping        - 露营
mountaineering - 登山
trekking       - 穿越
custom         - 自定义
\`\`\`

#### 2.2.2 装备清单状态 (EquipmentListStatus)

\`\`\`
planning   - 规划中
preparing  - 准备中
ready      - 已完成准备
inUse      - 使用中
completed  - 已完成
archived   - 已归档
\`\`\`

#### 2.2.3 装备分类 (EquipmentCategory)

\`\`\`
shelter      - 住宿装备（帐篷、睡袋、睡垫等）
food         - 饮食装备（炉具、餐具、水壶等）
clothing     - 保暖装备（衣物、手套、帽子等）
backpack     - 背包装备（背包、防雨罩等）
navigation   - 导航装备（地图、指南针、GPS等）
lighting     - 照明装备（头灯、手电筒等）
firstAid     - 急救装备（急救包、药品等）
tools        - 工具装备（刀具、绳索、修理工具等）
electronics  - 电子装备（手机、相机、充电宝等）
personalCare - 个人护理（洗漱用品、防晒用品等）
other        - 其他装备
\`\`\`

#### 2.2.4 装备必要性 (EquipmentNecessity)

\`\`\`
essential   - 必需
recommended - 推荐
optional    - 可选
\`\`\`

#### 2.2.5 装备使用状态 (EquipmentCondition)

\`\`\`
new      - 全新
good     - 良好
fair     - 一般
poor     - 较差
damaged  - 损坏
\`\`\`

#### 2.2.6 季节适用性 (SeasonSuitability)

\`\`\`
spring     - 春季
summer     - 夏季
autumn     - 秋季
winter     - 冬季
allSeasons - 四季
\`\`\`

#### 2.2.7 重量单位 (WeightUnit)

\`\`\`
gram     - 克
kilogram - 千克
pound    - 磅
ounce    - 盎司
\`\`\`

## 3. API接口

### 3.1 装备清单接口

#### 3.1.1 获取用户装备清单列表

\`\`\`
GET /api/equipment-lists
\`\`\`

**请求参数：**

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

**响应：**

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
        "type": "shortHike",
        "routeName": "莫干山徒步路线",
        "tripDays": 2,
        "personCount": 1,
        "seasons": ["spring"],
        "totalWeight": 5600,
        "totalItems": 15,
        "status": "planning",
        "isOfficial": false,
        "createdAt": "2023-04-15T08:30:00Z",
        "updatedAt": "2023-04-15T08:30:00Z"
      },
      // 更多装备清单...
    ]
  }
}
\`\`\`

#### 3.1.2 获取装备清单详情

\`\`\`
GET /api/equipment-lists/{listId}
\`\`\`

**请求参数：**

| 参数名 | 类型 | 必填 | 描述 |
|--------|------|------|------|
| listId | string | 是 | 装备清单ID |

**响应：**

\`\`\`json
{
  "code": 200,
  "message": "success",
  "data": {
    // 完整的装备清单对象，包含装备项目列表
  }
}
\`\`\`

#### 3.1.3 创建装备清单

\`\`\`
POST /api/equipment-lists
\`\`\`

**请求体：**

\`\`\`json
{
  "name": "春季徒步基础装备",
  "description": "适合春季短途徒步的基础装备清单",
  "type": "shortHike",
  "routeId": "route123",
  "routeName": "莫干山徒步路线",
  "tripDays": 2,
  "personCount": 1,
  "seasons": ["spring"],
  "equipments": [
    {
      "name": "徒步鞋",
      "category": "clothing",
      "weight": 800,
      "quantity": 1,
      "necessity": "essential",
      // 其他装备项目属性...
    },
    // 更多装备项目...
  ],
  "tags": ["短途", "春季", "入门"]
}
\`\`\`

**响应：**

\`\`\`json
{
  "code": 200,
  "message": "装备清单创建成功",
  "data": {
    "id": "list123",
    // 创建的装备清单对象
  }
}
\`\`\`

#### 3.1.4 从模板创建装备清单

\`\`\`
POST /api/equipment-lists/from-template
\`\`\`

**请求体：**

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

**响应：**

\`\`\`json
{
  "code": 200,
  "message": "装备清单创建成功",
  "data": {
    "id": "list123",
    // 创建的装备清单对象
  }
}
\`\`\`

#### 3.1.5 更新装备清单

\`\`\`
PUT /api/equipment-lists/{listId}
\`\`\`

**请求体：**

\`\`\`json
{
  "name": "更新后的装备清单名称",
  "description": "更新后的描述",
  "type": "shortHike",
  "routeId": "route123",
  "routeName": "莫干山徒步路线",
  "tripDays": 3,
  "personCount": 2,
  "seasons": ["spring", "summer"],
  "tags": ["短途", "春季", "入门"],
  "status": "preparing"
}
\`\`\`

**响应：**

\`\`\`json
{
  "code": 200,
  "message": "装备清单更新成功",
  "data": {
    "id": "list123",
    // 更新后的装备清单对象
  }
}
\`\`\`

#### 3.1.6 删除装备清单

\`\`\`
DELETE /api/equipment-lists/{listId}
\`\`\`

**请求参数：**

| 参数名 | 类型 | 必填 | 描述 |
|--------|------|------|------|
| listId | string | 是 | 装备清单ID |

**响应：**

\`\`\`json
{
  "code": 200,
  "message": "装备清单删除成功"
}
\`\`\`

#### 3.1.7 获取装备清单统计数据

\`\`\`
GET /api/equipment-lists/{listId}/stats
\`\`\`

**请求参数：**

| 参数名 | 类型 | 必填 | 描述 |
|--------|------|------|------|
| listId | string | 是 | 装备清单ID |

**响应：**

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
        "category": "shelter",
        "count": 3,
        "weight": 1500
      },
      // 更多分类...
    ],
    "heaviestItems": [
      {
        "id": "item123",
        "name": "帐篷",
        "category": "shelter",
        "weight": 2000,
        "necessity": "essential"
      },
      // 更多重量最大的装备...
    ]
  }
}
\`\`\`

### 3.2 装备项目接口

#### 3.2.1 添加装备项目到清单

\`\`\`
POST /api/equipment-lists/{listId}/items
\`\`\`

**请求体：**

\`\`\`json
{
  "name": "徒步鞋",
  "category": "clothing",
  "description": "防水透气徒步鞋",
  "weight": 800,
  "weightUnit": "gram",
  "quantity": 1,
  "necessity": "essential",
  "brand": "Salomon",
  "model": "X Ultra 3",
  "price": 899,
  "isOwned": true,
  "isShared": false,
  "condition": "good",
  "notes": "需要提前穿几次，避免起泡"
}
\`\`\`

**响应：**

\`\`\`json
{
  "code": 200,
  "message": "装备项目添加成功",
  "data": {
    "id": "item123",
    // 添加的装备项目对象
  }
}
\`\`\`

#### 3.2.2 更新装备项目

\`\`\`
PUT /api/equipment-lists/{listId}/items/{itemId}
\`\`\`

**请求体：**

\`\`\`json
{
  "name": "徒步鞋",
  "category": "clothing",
  "description": "防水透气徒步鞋",
  "weight": 850,
  "quantity": 1,
  "necessity": "essential",
  "prepared": true,
  "brand": "Salomon",
  "model": "X Ultra 3 GTX",
  "price": 999,
  "notes": "已准备好"
}
\`\`\`

**响应：**

\`\`\`json
{
  "code": 200,
  "message": "装备项目更新成功",
  "data": {
    "id": "item123",
    // 更新后的装备项目对象
  }
}
\`\`\`

#### 3.2.3 删除装备项目

\`\`\`
DELETE /api/equipment-lists/{listId}/items/{itemId}
\`\`\`

**请求参数：**

| 参数名 | 类型 | 必填 | 描述 |
|--------|------|------|------|
| listId | string | 是 | 装备清单ID |
| itemId | string | 是 | 装备项目ID |

**响应：**

\`\`\`json
{
  "code": 200,
  "message": "装备项目删除成功"
}
\`\`\`

#### 3.2.4 批量更新装备准备状态

\`\`\`
PUT /api/equipment-lists/{listId}/items/preparation
\`\`\`

**请求体：**

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
    // 更多装备项目...
  ]
}
\`\`\`

**响应：**

\`\`\`json
{
  "code": 200,
  "message": "装备准备状态更新成功",
  "data": {
    "updatedCount": 2,
    "preparationPercentage": 66.7
  }
}
\`\`\`

### 3.3 装备模板接口

#### 3.3.1 获取装备模板列表

\`\`\`
GET /api/equipment-templates
\`\`\`

**请求参数：**

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

**响应：**

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
        "type": "shortHike",
        "seasons": ["spring"],
        "isOfficial": true,
        "usageCount": 1250,
        "rating": 4.8,
        "createdAt": "2023-01-15T08:30:00Z"
      },
      // 更多模板...
    ]
  }
}
\`\`\`

#### 3.3.2 获取装备模板详情

\`\`\`
GET /api/equipment-templates/{templateId}
\`\`\`

**请求参数：**

| 参数名 | 类型 | 必填 | 描述 |
|--------|------|------|------|
| templateId | string | 是 | 模板ID |

**响应：**

\`\`\`json
{
  "code": 200,
  "message": "success",
  "data": {
    // 完整的装备模板对象，包含装备项目列表
  }
}
\`\`\`

#### 3.3.3 创建装备模板

\`\`\`
POST /api/equipment-templates
\`\`\`

**请求体：**

\`\`\`json
{
  "name": "我的春季徒步装备模板",
  "description": "适合春季短途徒步的个人装备模板",
  "type": "shortHike",
  "seasons": ["spring"],
  "equipments": [
    {
      "name": "徒步鞋",
      "category": "clothing",
      "weight": 800,
      "quantity": 1,
      "necessity": "essential",
      // 其他装备项目属性...
    },
    // 更多装备项目...
  ],
  "tags": ["短途", "春季", "个人"]
}
\`\`\`

**响应：**

\`\`\`json
{
  "code": 200,
  "message": "装备模板创建成功",
  "data": {
    "id": "template123",
    // 创建的装备模板对象
  }
}
\`\`\`

#### 3.3.4 从装备清单创建模板

\`\`\`
POST /api/equipment-templates/from-list
\`\`\`

**请求体：**

\`\`\`json
{
  "listId": "list123",
  "name": "我的春季徒步装备模板",
  "description": "基于我的装备清单创建的模板",
  "tags": ["短途", "春季", "个人"]
}
\`\`\`

**响应：**

\`\`\`json
{
  "code": 200,
  "message": "装备模板创建成功",
  "data": {
    "id": "template123",
    // 创建的装备模板对象
  }
}
\`\`\`

### 3.4 用户装备库接口

#### 3.4.1 获取用户装备库

\`\`\`
GET /api/user-equipment-inventory
\`\`\`

**请求参数：**

| 参数名 | 类型 | 必填 | 描述 |
|--------|------|------|------|
| userId | string | 是 | 用户ID |
| category | string | 否 | 分类筛选 |
| condition | string | 否 | 状态筛选 |
| search | string | 否 | 搜索关键词 |

**响应：**

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
        "category": "clothing",
        "weight": 800,
        "brand": "Salomon",
        "model": "X Ultra 3",
        "condition": "good",
        "purchaseDate": "2022-05-10T00:00:00Z",
        "usageCount": 12
      },
      // 更多装备...
    ],
    "statistics": {
      "totalItems": 45,
      "totalValue": 12500,
      "categoryDistribution": [
        {
          "category": "clothing",
          "count": 15,
          "value": 5600
        },
        // 更多分类...
      ],
      "conditionDistribution": [
        {
          "condition": "good",
          "count": 30
        },
        // 更多状态...
      ]
    }
  }
}
\`\`\`

#### 3.4.2 添加装备到用户装备库

\`\`\`
POST /api/user-equipment-inventory/items
\`\`\`

**请求体：**

\`\`\`json
{
  "name": "徒步鞋",
  "category": "clothing",
  "description": "防水透气徒步鞋",
  "weight": 800,
  "weightUnit": "gram",
  "brand": "Salomon",
  "model": "X Ultra 3",
  "price": 899,
  "purchaseDate": "2022-05-10T00:00:00Z",
  "purchaseLink": "https://example.com/shop/shoes/123",
  "condition": "good",
  "usageCount": 12,
  "imageUrl": "https://example.com/images/shoes/123.jpg",
  "notes": "非常舒适的徒步鞋"
}
\`\`\`

**响应：**

\`\`\`json
{
  "code": 200,
  "message": "装备添加成功",
  "data": {
    "id": "item123",
    // 添加的装备项目对象
  }
}
\`\`\`

## 4. 数据流转

### 4.1 创建装备清单流程

1. 用户选择创建方式：
   - 从头创建
   - 从模板创建
   - 从现有清单复制

2. 用户填写基本信息：
   - 名称、描述
   - 行程天数、适用人数
   - 适用季节
   - 关联路线（可选）

3. 用户添加装备项目：
   - 从装备库选择已有装备
   - 手动添加新装备
   - 接受系统推荐的装备

4. 系统计算重量分布：
   - 总重量、基础重量、消耗品重量、穿着重量
   - 每人每日平均重量

5. 用户保存装备清单

### 4.2 装备准备流程

1. 用户进入装备清单详情页
2. 切换到准备模式
3. 勾选已准备好的装备项目
4. 系统更新装备准备状态和进度
5. 用户完成准备后，更新清单状态为"已准备"

### 4.3 装备分析流程

1. 用户进入装备清单分析页
2. 系统展示各类分析数据：
   - 重量分布
   - 分类分布
   - 必要性分布
   - 最重装备列表
   - 准备状态

3. 用户根据分析结果优化装备清单

## 5. 注意事项

1. **重量计算**：
   - 所有重量统一以克(g)为单位存储
   - 前端可根据需要转换为千克(kg)、磅(lb)或盎司(oz)显示
   - 计算总重量时需考虑装备数量和共享状态

2. **数据一致性**：
   - 更新装备项目时，需同步更新相关的装备清单重量数据
   - 删除装备项目时，需重新计算装备清单的重量分布

3. **性能优化**：
   - 装备清单列表接口默认不返回完整的装备项目列表，减少数据传输量
   - 装备清单详情接口返回完整数据，包括所有装备项目

4. **数据安全**：
   - 用户只能访问和修改自己创建的装备清单
   - 官方模板对所有用户可见，但不可修改

5. **版本兼容**：
   - API需支持向后兼容，确保旧版本客户端仍能正常工作
   - 新增字段应设置合理的默认值

## 6. 后续优化方向

1. **智能推荐系统**：
   - 基于路线特点、季节、天气自动推荐装备
   - 根据用户历史选择优化推荐算法

2. **装备共享功能**：
   - 支持多人行程中的装备分配
   - 计算每人负重和共享装备分摊

3. **装备检查提醒**：
   - 出发前提醒检查关键装备
   - 根据装备使用状态提醒维护或更换

4. **数据导出功能**：
   - 支持导出为PDF、Excel等格式
   - 生成打印友好的装备清单

5. **社区分享功能**：
   - 用户可分享自己的装备清单和模板
   - 支持评分和评论功能