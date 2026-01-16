# 设计文档：前后端 API 接口协议

## Context
Walk 项目采用前后端分离架构：
- **前端**：Flutter (Dart) 移动应用，使用 Dio 进行网络请求
- **后端**：Kotlin + Spring Boot RESTful API，使用 MySQL 数据库
- **当前问题**：
  - 前端已定义 15 个 API 端点规范，但与后端实际 API 不完全匹配
  - 缺少统一的数据模型映射规则（Dart 使用 camelCase，后端 JSON 使用 snake_case）
  - 枚举类型处理方式未明确（前端使用整数，后端使用字符串或整数）
  - 缺少接口变更流程，导致前后端联调困难

## Goals / Non-Goals

### Goals
1. **定义统一的 API 接口规范**，包括 URL 设计、HTTP 方法、响应格式
2. **建立数据模型映射规则**，明确前后端字段命名转换规则
3. **规范枚举类型处理**，统一前后端枚举值的序列化方式
4. **制定接口变更流程**，确保前后端同步变更

### Non-Goals
- 不修改现有 API 实现（仅定义规范）
- 不强制迁移现有代码（规范适用于新开发）
- 不涉及具体业务逻辑设计

## Decisions

### 决策 1：RESTful API 设计规范
**选择**：遵循 RESTful 约定，使用标准 HTTP 方法和状态码

**理由**：
- Spring Boot 和 Flutter/Dio 都天然支持 RESTful 风格
- 前端已定义的 `api-endpoints` 规范基于 RESTful 设计
- 行业标准，易于理解和维护

**规范细节**：
- URL 路径：`/walkbg/api/v1/{resource}`
- HTTP 方法：
  - `GET` - 查询资源
  - `POST` - 创建资源
  - `PUT` - 完整更新资源
  - `PATCH` - 部分更新资源
  - `DELETE` - 删除资源
- 状态码：
  - `200 OK` - 成功
  - `201 Created` - 创建成功
  - `400 Bad Request` - 请求参数错误
  - `401 Unauthorized` - 未认证
  - `403 Forbidden` - 无权限
  - `404 Not Found` - 资源不存在
  - `500 Internal Server Error` - 服务器错误

### 决策 2：统一响应格式
**选择**：使用 `ApiResponse<T>` 包装所有 API 响应

**现有后端实现**：
```kotlin
data class ApiResponse<T>(
    val success: Boolean,
    val message: String?,
    val data: T?,
    val error: ErrorDetails?
)
```

**前端对应模型**：
```dart
class ApiResponse<T> {
  final bool success;
  final String? message;
  final T? data;
  final ErrorDetails? error;
}
```

**规范**：
- 所有 API 响应必须使用此格式
- 成功响应：`success: true`, `data: {...}`, `error: null`
- 失败响应：`success: false`, `message: "..."`, `data: null`, `error: {...}`

### 决策 3：数据模型命名映射规则
**选择**：后端 JSON 使用 snake_case，前端 Dart 使用 camelCase，通过注解自动映射

**理由**：
- Kotlin/Java 惯例使用 camelCase，但 JSON API 惯例使用 snake_case
- Dart 惯例使用 camelCase
- 前端已使用 `@JsonKey(name: 'field_name')` 进行映射
- 后端可使用 Jackson `@JsonProperty("field_name")` 或配置全局策略

**映射示例**：

后端 Kotlin：
```kotlin
data class RouteModel(
    @JsonProperty("route_id")
    val routeId: String,
    
    @JsonProperty("route_name")
    val routeName: String,
    
    @JsonProperty("created_time")
    val createdTime: LocalDateTime
)
```

JSON：
```json
{
  "route_id": "123",
  "route_name": "五台山环线",
  "created_time": "2025-01-15T10:00:00Z"
}
```

前端 Dart：
```dart
@JsonSerializable()
class RouteModel {
  @JsonKey(name: 'route_id')
  final String routeId;
  
  @JsonKey(name: 'route_name')
  final String routeName;
  
  @JsonKey(name: 'created_time')
  final DateTime createdTime;
}
```

### 决策 4：枚举类型处理规范
**选择**：前后端统一使用整数值进行枚举序列化

**理由**：
- 前端已实现整数枚举（通过 `@JsonValue` 注解）
- 整数值更紧凑，节省带宽
- 数据库存储更高效
- 便于扩展（新增枚举值不影响旧版本）

**规范**：
- 后端枚举必须提供整数值映射
- JSON 中使用整数值
- 枚举值从 0 开始连续编号
- 不允许删除或重用枚举值（只能追加）

**示例**：

后端 Kotlin：
```kotlin
enum class RouteType(val value: Int) {
    MUD_ROAD(0),
    FARM_ROAD(1),
    STONE_ROAD(2),
    CONCRETE_ROAD(3),
    ASPHALT_ROAD(4),
    TRAIL(5),
    BOARDWALK(6);
    
    @JsonValue
    fun toJson(): Int = value
    
    companion object {
        @JsonCreator
        @JvmStatic
        fun fromJson(value: Int): RouteType = 
            values().find { it.value == value } ?: TRAIL
    }
}
```

JSON：
```json
{
  "route_type": 0
}
```

前端 Dart（已实现）：
```dart
enum RouteType {
  @JsonValue(0) mudRoad,
  @JsonValue(1) farmRoad,
  @JsonValue(2) stoneRoad,
  // ...
}
```

### 决策 5：分页参数规范
**选择**：使用标准分页参数 `page`, `size`, `sort`

**规范**：
- `page`: 页码，从 1 开始（注意：有些框架从 0 开始，需统一）
- `size`: 每页数量，默认 20，最大 100
- `sort`: 排序字段和方向，格式 `field,direction`（如 `created_time,desc`）

**响应格式**：
```json
{
  "success": true,
  "data": {
    "content": [...],
    "page": 1,
    "size": 20,
    "total_elements": 100,
    "total_pages": 5,
    "is_last": false,
    "is_first": true
  }
}
```

### 决策 6：时间日期格式规范
**选择**：使用 ISO 8601 格式，时区为 UTC

**规范**：
- 日期时间：`YYYY-MM-DDTHH:mm:ss.sssZ` (如 `2025-01-15T10:00:00.000Z`)
- 仅日期：`YYYY-MM-DD` (如 `2025-01-15`)
- 仅时间：`HH:mm:ss` (如 `10:00:00`)
- 后端使用 `@JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss.SSS'Z'", timezone = "UTC")`
- 前端使用 `DateTime.parse()` 自动解析

### 决策 7：API 版本管理策略
**选择**：URL 路径版本控制 `/api/v{version}`

**理由**：
- 前端已使用 `/walkbg/api/v1`
- 明确且易于路由
- 支持多版本并存

**规范**：
- 当前版本：`v1`
- 重大变更（Breaking Changes）时升级主版本
- 旧版本至少保留 6 个月过渡期
- 在响应头中返回当前 API 版本：`X-API-Version: v1`

### 决策 8：错误响应格式
**选择**：使用结构化错误响应

**格式**：
```json
{
  "success": false,
  "message": "用户友好的错误描述",
  "error": {
    "code": "RESOURCE_NOT_FOUND",
    "details": "Route with ID 123 not found",
    "field": "route_id",
    "timestamp": "2025-01-15T10:00:00.000Z"
  }
}
```

**错误码规范**：
- `INVALID_REQUEST` - 请求参数无效
- `RESOURCE_NOT_FOUND` - 资源不存在
- `UNAUTHORIZED` - 未认证
- `FORBIDDEN` - 无权限
- `CONFLICT` - 资源冲突（如重复创建）
- `INTERNAL_ERROR` - 服务器内部错误

## Alternatives Considered

### 备选方案 1：GraphQL
**优点**：灵活查询，减少过度获取
**缺点**：学习成本高，当前团队不熟悉，前端已实现 RESTful 架构
**决策**：不采用

### 备选方案 2：枚举使用字符串值
**优点**：更易读
**缺点**：占用带宽大，前端已实现整数枚举
**决策**：不采用

### 备选方案 3：分页从 0 开始
**优点**：符合部分框架习惯（如 Spring Data）
**缺点**：对用户不友好，前端惯例从 1 开始
**决策**：统一从 1 开始，后端需转换

## Risks / Trade-offs

### 风险 1：现有 API 与规范不一致
**影响**：后端部分 API 需要调整以符合规范
**缓解**：
- 规范仅适用于新开发的 API
- 现有 API 逐步迁移，不强制立即改造
- 在文档中标注哪些 API 已遵循新规范

### 风险 2：前后端理解偏差
**影响**：可能出现实现不一致
**缓解**：
- 在 OpenSpec 中明确定义规范
- 提供示例代码
- 前后端定期同步（每周）

### 风险 3：枚举值扩展导致兼容性问题
**影响**：老版本客户端不识别新枚举值
**缓解**：
- 前端必须处理未知枚举值（使用默认值）
- 后端添加枚举值时在文档中标注版本
- 关键枚举值谨慎添加

## Migration Plan

### 阶段 1：规范定义和评审（1 周）
- [x] 创建 `api-contract` 规范提案
- [ ] 前后端团队评审规范
- [ ] 根据反馈调整规范
- [ ] 规范归档到 `specs/`

### 阶段 2：示例 API 实现（1-2 周）
- [ ] 后端：按规范实现 1-2 个示例 API（如 Routes API）
- [ ] 前端：按规范对接示例 API
- [ ] 验证规范的可行性
- [ ] 调整规范（如有必要）

### 阶段 3：新 API 开发遵循规范（持续）
- [ ] 所有新开发的 API 必须遵循此规范
- [ ] Code Review 时检查规范遵循情况
- [ ] 定期同步前后端进度

### 阶段 4：现有 API 迁移（可选，6 个月内）
- [ ] 评估现有 API 与规范的差距
- [ ] 制定迁移计划（优先级排序）
- [ ] 逐步迁移现有 API
- [ ] 保持向后兼容

## Open Questions

1. **分页起始页码**：
   - 前端习惯从 1 开始
   - Spring Data 默认从 0 开始
   - **决策**：统一从 1 开始，后端在 Controller 层转换

2. **文件上传格式**：
   - 使用 `multipart/form-data` 还是 Base64？
   - **建议**：`multipart/form-data`（更高效）
   - **决策**：multipart/form-data

3. **批量操作 API 设计**：
   - 如批量删除，使用 `DELETE /resources?ids=1,2,3` 还是 `POST /resources/batch-delete`？
   - **决策**：使用 POST（RESTful 规范 DELETE 不应有 body）

4. **认证方式**：
   - 当前使用 JWT Token？
   - Token 放在 Header 还是 Cookie？
   - **决策**：`Authorization: Bearer {token}` (Header)

5. **跨域配置**：
   - 后端是否已配置 CORS？
   - **暂不考虑**：开发环境和生产环境的 CORS 配置
