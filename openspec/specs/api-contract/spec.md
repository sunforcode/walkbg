# api-contract Specification

## Purpose
前后端 API 接口协议规范，定义前后端分离开发必须遵循的接口契约，包括 RESTful API 设计约定、统一响应格式、数据模型命名映射规则、枚举类型处理、分页规范、时间格式、版本管理、错误处理、认证授权和跨域配置等核心规范。

## Requirements
### Requirement: RESTful API 设计规范
前后端 API SHALL 遵循 RESTful 架构约定：
- URL 路径格式：`/walkbg/api/v1/{resource}`
- 使用标准 HTTP 方法：GET（查询）、POST（创建）、PUT（完整更新）、PATCH（部分更新）、DELETE（删除）
- 使用标准 HTTP 状态码：200（成功）、201（创建成功）、400（参数错误）、401（未认证）、403（无权限）、404（不存在）、500（服务器错误）

#### Scenario: 路线列表查询
- **WHEN** 前端请求 `GET /walkbg/api/v1/routes`
- **THEN** 后端返回状态码 200 和路线列表数据
- **AND** 响应格式符合 `ApiResponse<List<RouteModel>>` 规范

#### Scenario: 创建新路线
- **WHEN** 前端请求 `POST /walkbg/api/v1/routes` 并提供路线数据
- **THEN** 后端返回状态码 201 和创建的路线数据
- **AND** 响应头包含 `Location: /walkbg/api/v1/routes/{id}`

#### Scenario: 资源不存在错误
- **WHEN** 前端请求 `GET /walkbg/api/v1/routes/non-existent-id`
- **THEN** 后端返回状态码 404
- **AND** 响应体包含错误信息 `{"success": false, "message": "...", "error": {...}}`

---

### Requirement: 统一响应格式
所有 API 响应 SHALL 使用 `ApiResponse<T>` 包装格式：
- 成功响应：`{"success": true, "data": {...}, "message": null, "error": null}`
- 失败响应：`{"success": false, "data": null, "message": "错误描述", "error": {...}}`
- 错误详情包含：`code`（错误码）、`details`（详细信息）、`field`（相关字段）、`timestamp`（时间戳）

#### Scenario: 成功响应结构
- **WHEN** API 调用成功
- **THEN** 响应必须包含 `success: true`
- **AND** `data` 字段包含实际数据
- **AND** `error` 字段为 null

#### Scenario: 失败响应结构
- **WHEN** API 调用失败（如参数错误）
- **THEN** 响应必须包含 `success: false`
- **AND** `message` 字段包含用户友好的错误描述
- **AND** `error` 字段包含结构化错误详情（code, details, field, timestamp）

---

### Requirement: 数据模型命名映射规范
前后端数据模型字段命名 SHALL 遵循以下规则：
- 后端 Kotlin 模型：使用 camelCase（如 `routeName`）
- JSON API：使用 snake_case（如 `route_name`）
- 前端 Dart 模型：使用 camelCase（如 `routeName`）
- 后端使用 `@JsonProperty("field_name")` 注解进行映射
- 前端使用 `@JsonKey(name: 'field_name')` 注解进行映射

#### Scenario: 字段命名转换
- **WHEN** 后端返回 JSON `{"route_name": "五台山", "created_time": "2025-01-15T10:00:00Z"}`
- **THEN** 前端 Dart 模型字段为 `routeName` 和 `createdTime`
- **AND** 通过 `@JsonKey` 注解自动映射

#### Scenario: 嵌套对象命名
- **WHEN** JSON 包含嵌套对象 `{"user_info": {"user_name": "张三"}}`
- **THEN** 前端模型为 `userInfo.userName`
- **AND** 所有层级字段都遵循命名映射规则

---

### Requirement: 枚举类型处理规范
前后端枚举类型 SHALL 使用整数值进行序列化：
- JSON 中使用整数值（如 `0, 1, 2`）
- 枚举值从 0 开始连续编号
- 不允许删除或重用枚举值编号
- 后端使用 `@JsonValue` 返回整数值
- 前端使用 `@JsonValue` 注解声明整数值
- 未知枚举值 SHALL 使用默认值（而非抛出异常）

#### Scenario: 枚举序列化
- **WHEN** 路线类型为 `RouteType.MUD_ROAD`（后端）或 `RouteType.mudRoad`（前端）
- **THEN** JSON 中值为 `{"route_type": 0}`
- **AND** 前后端都能正确解析该整数值

#### Scenario: 未知枚举值处理
- **WHEN** 前端收到 JSON `{"route_type": 99}`（未定义的枚举值）
- **THEN** 前端解析为默认值（如 `RouteType.trail`）
- **AND** 不抛出异常，应用继续运行

#### Scenario: 枚举扩展兼容性
- **WHEN** 后端添加新枚举值 `GRAVEL_ROAD(7)`
- **THEN** 旧版本前端收到该值时使用默认值
- **AND** 新版本前端能正确显示该枚举值

---

### Requirement: 分页参数规范
分页 API SHALL 使用以下标准参数：
- `page`：页码，从 1 开始（必填，默认 1）
- `size`：每页数量（可选，默认 20，最大 100）
- `sort`：排序规则（可选，格式 `field,direction`，如 `created_time,desc`）
- 响应包含：`content`（数据列表）、`page`（当前页）、`size`（每页数量）、`total_elements`（总元素数）、`total_pages`（总页数）、`is_first`（是否首页）、`is_last`（是否末页）

#### Scenario: 分页请求
- **WHEN** 前端请求 `GET /walkbg/api/v1/routes?page=2&size=20&sort=created_time,desc`
- **THEN** 后端返回第 2 页数据，每页 20 条，按创建时间降序排列
- **AND** 响应包含完整的分页元信息

#### Scenario: 分页元信息
- **WHEN** 后端返回分页数据
- **THEN** 响应必须包含 `page`, `size`, `total_elements`, `total_pages`, `is_first`, `is_last` 字段
- **AND** 前端可据此渲染分页控件

#### Scenario: 分页边界处理
- **WHEN** 请求的页码超出范围（如 `page=999`）
- **THEN** 后端返回空列表 `content: []`
- **AND** 分页元信息正确反映总页数

---

### Requirement: 时间日期格式规范
所有时间日期字段 SHALL 使用 ISO 8601 格式：
- 日期时间：`YYYY-MM-DDTHH:mm:ss.sssZ`（UTC 时区，如 `2025-01-15T10:00:00.000Z`）
- 仅日期：`YYYY-MM-DD`（如 `2025-01-15`）
- 仅时间：`HH:mm:ss`（如 `10:00:00`）
- 后端使用 `@JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss.SSS'Z'", timezone = "UTC")`
- 前端使用 `DateTime.parse()` 自动解析

#### Scenario: 日期时间传输
- **WHEN** 后端返回创建时间
- **THEN** JSON 格式为 `{"created_time": "2025-01-15T10:00:00.000Z"}`
- **AND** 前端能正确解析为 `DateTime` 对象

#### Scenario: 时区处理
- **WHEN** 用户在不同时区使用应用
- **THEN** 后端统一使用 UTC 时区存储和传输
- **AND** 前端负责转换为用户本地时区显示

---

### Requirement: API 版本管理策略
API 版本 SHALL 通过 URL 路径管理：
- 当前版本：`/walkbg/api/v1`
- 重大变更时升级主版本（如 `/walkbg/api/v2`）
- 旧版本至少保留 6 个月过渡期
- 响应头包含 `X-API-Version: v1` 标识当前版本

#### Scenario: 版本标识
- **WHEN** 前端调用任何 API
- **THEN** 响应头必须包含 `X-API-Version` 字段
- **AND** 前端可据此判断 API 版本

#### Scenario: 版本升级兼容
- **WHEN** 后端发布 v2 API
- **THEN** v1 API 必须继续可用至少 6 个月
- **AND** 在 v1 响应中添加弃用警告头 `X-API-Deprecated: true, use v2`

---

### Requirement: 错误响应格式规范
错误响应 SHALL 包含结构化错误信息：
- `success: false`
- `message`：用户友好的错误描述（中文）
- `error.code`：机器可读的错误码（如 `RESOURCE_NOT_FOUND`）
- `error.details`：详细的技术错误信息（英文，用于调试）
- `error.field`：相关字段名（如参数校验失败时）
- `error.timestamp`：错误发生时间（ISO 8601 格式）

**标准错误码**：
- `INVALID_REQUEST` - 请求参数无效
- `RESOURCE_NOT_FOUND` - 资源不存在
- `UNAUTHORIZED` - 未认证
- `FORBIDDEN` - 无权限
- `CONFLICT` - 资源冲突
- `INTERNAL_ERROR` - 服务器内部错误

#### Scenario: 参数校验失败
- **WHEN** 前端提交无效参数（如路线名称为空）
- **THEN** 后端返回 400 状态码
- **AND** 响应包含 `{"success": false, "message": "路线名称不能为空", "error": {"code": "INVALID_REQUEST", "field": "route_name", ...}}`

#### Scenario: 资源不存在
- **WHEN** 请求不存在的资源 ID
- **THEN** 后端返回 404 状态码
- **AND** 响应包含 `{"success": false, "message": "路线不存在", "error": {"code": "RESOURCE_NOT_FOUND", ...}}`

#### Scenario: 服务器内部错误
- **WHEN** 后端发生未预期的异常
- **THEN** 返回 500 状态码
- **AND** 响应包含 `{"success": false, "message": "服务器错误，请稍后重试", "error": {"code": "INTERNAL_ERROR", ...}}`
- **AND** 不暴露敏感的堆栈跟踪信息（生产环境）

---

### Requirement: 接口变更流程规范
API 接口变更 SHALL 遵循以下流程：
1. **设计阶段**：在 OpenSpec 中创建接口变更提案（包含 proposal.md, design.md, spec deltas）
2. **评审阶段**：前后端团队共同评审提案，确认接口定义
3. **实现阶段**：前后端依据提案独立开发
4. **联调阶段**：前后端联合测试接口
5. **微调阶段**：如需调整，更新 OpenSpec 提案并重新评审

**变更类型**：
- **新增接口**：在 OpenSpec 中添加新的端点定义
- **修改接口**（非破坏性）：更新 OpenSpec，保持向后兼容
- **废弃接口**：标记为 Deprecated，保留 6 个月后删除
- **破坏性变更**：创建新版本 API（如 v2）

#### Scenario: 新增 API 接口
- **WHEN** 需要添加新的 API 接口（如"获取路线天气预报"）
- **THEN** 必须先在 OpenSpec 中创建变更提案
- **AND** 前后端评审通过后再开始实现
- **AND** 接口定义包含：URL、HTTP方法、请求参数、响应格式、错误码

#### Scenario: 修改现有接口
- **WHEN** 需要修改现有接口（如添加新的查询参数）
- **THEN** 必须更新 OpenSpec 中的接口定义
- **AND** 评估是否为破坏性变更
- **AND** 如为破坏性变更，需创建新版本或使用新端点

#### Scenario: 前后端联调发现问题
- **WHEN** 联调时发现接口定义不合理
- **THEN** 更新 OpenSpec 提案文档
- **AND** 前后端确认调整方案后重新实现
- **AND** 避免口头约定，必须在 OpenSpec 中记录变更

---

### Requirement: 认证和授权规范
需要认证的 API SHALL 遵循以下规范：
- 使用 JWT Token 进行身份认证
- Token 放在 HTTP Header 中：`Authorization: Bearer {token}`
- 未认证请求返回 401 状态码
- 无权限请求返回 403 状态码
- 用户身份通过 Token 识别，不在 URL 中硬编码用户 ID

#### Scenario: 认证 API 调用
- **WHEN** 前端调用需要认证的 API（如获取当前用户信息）
- **THEN** 请求头必须包含 `Authorization: Bearer {token}`
- **AND** 后端从 Token 中提取用户身份
- **AND** 不在 URL 中包含用户 ID（如 `/user/profile` 而非 `/users/{id}/profile`）

#### Scenario: Token 过期处理
- **WHEN** 前端使用过期的 Token 调用 API
- **THEN** 后端返回 401 状态码
- **AND** 错误响应包含 `{"success": false, "error": {"code": "UNAUTHORIZED", "message": "Token expired"}}`
- **AND** 前端自动跳转到登录页或刷新 Token

#### Scenario: 权限不足
- **WHEN** 用户尝试访问无权限的资源（如删除他人的路线）
- **THEN** 后端返回 403 状态码
- **AND** 错误响应包含 `{"success": false, "message": "无权限操作", "error": {"code": "FORBIDDEN"}}`

---

### Requirement: 跨域资源共享（CORS）配置
后端 SHALL 配置正确的 CORS 策略：
- 开发环境：允许所有来源（`Access-Control-Allow-Origin: *`）
- 生产环境：仅允许前端域名
- 允许的 HTTP 方法：GET, POST, PUT, PATCH, DELETE, OPTIONS
- 允许的请求头：`Content-Type`, `Authorization`, `X-Requested-With`
- 允许携带凭证：`Access-Control-Allow-Credentials: true`
- 预检请求缓存：`Access-Control-Max-Age: 3600`

#### Scenario: 开发环境跨域请求
- **WHEN** 前端在本地开发环境（如 `http://localhost:3000`）调用后端 API
- **THEN** 后端响应头包含 `Access-Control-Allow-Origin: *`
- **AND** 浏览器允许跨域请求

#### Scenario: 生产环境跨域限制
- **WHEN** 生产环境的前端（如 `https://walk.app.com`）调用 API
- **THEN** 后端仅允许该域名的跨域请求
- **AND** 其他域名的请求被拒绝

#### Scenario: 预检请求处理
- **WHEN** 浏览器发送 OPTIONS 预检请求
- **THEN** 后端返回 200 状态码和正确的 CORS 头
- **AND** 后续实际请求能成功发送
