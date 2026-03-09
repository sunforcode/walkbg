# Spec: Route Detail Enrichment

## ADDED Requirements

### Requirement: Complete Route Detail Response Structure
系统 SHALL 返回完整的、从数据库填充的 RouteDetailResponse 对象。API MUST 在存在相应数据时将所有12个嵌套集合和字段填充为实际数据，而不是空列表。

#### Scenario: User requests complete route details

- **WHEN** 前端请求 `GET /api/v1/routes/route_12345`，其中该路线在数据库中有5个路段、3个日程、7个营地、4个水源、2个补给站和1个搭车联系人
- **THEN** 后端返回状态码 200
- **AND** 响应中 segments 字段包含5个数据项
- **AND** 响应中 daily_plans 字段包含3个数据项
- **AND** 响应中 campsites 字段包含7个数据项
- **AND** 响应中 water_sources 字段包含4个数据项
- **AND** 响应中 supplies 字段包含2个数据项
- **AND** 响应中 hitchhike_contacts 字段包含1个数据项
- **AND** 所有嵌套集合都从数据库查询得到，而不是空列表
- **AND** 距离、持续时间、海拔数据来自 MapData 表

---

### Requirement: User Favorite Management
系统 SHALL 提供用户收藏路线功能。用户 SHALL 能够收藏和取消收藏路线，系统 MUST 持久化用户收藏偏好，RouteDetailResponse 中 isFavorite 字段 MUST 反映真实的收藏状态。收藏操作 MUST 是幂等的。

#### Scenario: User favorites a route

- **WHEN** 用户调用 `POST /api/v1/routes/route_456/favorite?userId=user_123`，且用户之前未收藏该路线
- **THEN** 后端返回状态码 200
- **AND** 响应消息包含 "成功"
- **AND** 用户-路线关系被持久化到数据库
- **AND** 后续请求中 isFavorite 字段为 true

#### Scenario: User unfavorites a route

- **WHEN** 用户 user_123 已收藏路线 route_456，随后调用 `DELETE /api/v1/routes/route_456/favorite?userId=user_123`
- **THEN** 后端返回状态码 200
- **AND** 响应消息包含 "成功"
- **AND** 用户-路线关系从数据库中删除
- **AND** 后续请求中 isFavorite 字段为 false

#### Scenario: Duplicate favorite is idempotent

- **WHEN** 用户 user_123 已收藏路线 route_456，再次调用 `POST /api/v1/routes/route_456/favorite?userId=user_123`
- **THEN** 后端返回状态码 200
- **AND** 数据库中不创建重复条目
- **AND** isFavorite 保持 true

---

### Requirement: Route Completion Tracking
系统 SHALL 跟踪用户完成路线的时间。路线完成记录 MUST 带有时间戳持久化到数据库。对应路线的 usage_count MUST 在用户完成路线时增加1。完成操作 MUST 是幂等的。

#### Scenario: User marks route as completed

- **WHEN** 用户 user_123 调用 `POST /api/v1/routes/route_456/complete?userId=user_123`，且用户之前未标记该路线为已完成
- **THEN** 后端返回状态码 200
- **AND** 响应消息包含 "成功"
- **AND** 在数据库中创建完成记录，包含时间戳
- **AND** 路线的 usage_count 增加 1

#### Scenario: Duplicate completion is idempotent

- **WHEN** 用户 user_123 之前完成过路线 route_456，再次调用 `POST /api/v1/routes/route_456/complete?userId=user_123`
- **THEN** 后端返回状态码 200
- **AND** 可能创建新的完成记录，也可能更新现有记录的时间戳
- **AND** usage_count 不再增加

---

### Requirement: Personal Route Lists
系统 SHALL 提供用户获取个人路线集合的功能。用户 SHALL 能够分别查询自己创建的路线、收藏的路线和完成的路线。所有端点 MUST 返回实际从数据库查询的数据，而不是空集合。

#### Scenario: User retrieves their created routes

- **WHEN** 用户 user_123 创建了路线 route_100、route_101、route_102，用户调用 `GET /api/v1/routes/my?userId=user_123&page=0&size=10`
- **THEN** 后端返回状态码 200
- **AND** 响应包含3条路线，都是该用户创建的
- **AND** 每条路线包含完整的 RouteBasicResponse 数据
- **AND** 路线按 created_at 倒序排序

#### Scenario: User retrieves favorited routes

- **WHEN** 用户 user_123 收藏了路线 route_100 和 route_102，调用 `GET /api/v1/routes/favorites?userId=user_123&page=0&size=10`
- **THEN** 后端返回状态码 200
- **AND** 响应包含2条路线，都是该用户收藏的
- **AND** 每条路线的 isFavorite 字段为 true
- **AND** 分页参数被正确使用

#### Scenario: User retrieves completed routes

- **WHEN** 用户 user_123 完成了路线 route_100、route_101、route_102，调用 `GET /api/v1/routes/completed?userId=user_123&page=0&size=10`
- **THEN** 后端返回状态码 200
- **AND** 响应包含3条路线，都是该用户完成的
- **AND** 路线按完成日期倒序排序
- **AND** 完成详情在响应中可用

---

## MODIFIED Requirements

### Requirement: Route Detail Response Data Population
之前 RouteDetailResponse.fromRoute() 将所有嵌套集合初始化为空列表。现在系统 SHALL 从相关存储库中填充所有嵌套集合。响应结构 MUST 保持不变（相同的字段名和类型），仅内容从空列表变为实际数据。

#### Scenario: Response structure remains backward compatible

- **WHEN** 前端代码期望接收包含以下字段的 RouteDetailResponse: segments, dailyPlans, campsites, waterSources, supplies, hitchhikeContacts, tags, imageUrls, markerPoints
- **THEN** 新实现返回的响应中所有字段名保持相同
- **AND** 所有字段类型保持相同（List<SegmentDto> 仍为 List<SegmentDto>）
- **AND** 前端无需修改代码即可处理新响应
- **AND** 原本是空列表的集合现在包含实际数据

#### Scenario: Data completeness improves

- **WHEN** 路线在数据库中有关联的路段、营地、水源
- **THEN** 新实现返回的响应包含所有这些关联数据
- **AND** 前端可以显示完整的路线信息
- **AND** 无需代码变更即可使用新数据

---

### Requirement: RouteController Endpoints Full Implementation
之前6个端点返回空数据或带有 TODO 注释的虚拟响应。现在这些端点 MUST 实现完整的业务逻辑。所有端点 MUST 从数据库查询、持久化更改，并返回填充的实际响应。

#### Scenario: Favorite endpoint transitions to full implementation

- **WHEN** 调用 `POST /api/v1/routes/{id}/favorite?userId={userId}`
- **THEN** 收藏关系被真正持久化到数据库
- **AND** isFavorite 状态被正确更新
- **AND** 数据库状态反映该操作

#### Scenario: Personal list endpoints return actual data

- **WHEN** 调用 `GET /api/v1/routes/my?userId={userId}` 或 `GET /api/v1/routes/favorites?userId={userId}` 或 `GET /api/v1/routes/completed?userId={userId}`
- **THEN** 路线从数据库真正查询得到
- **AND** 分页正确工作
- **AND** 结果包含完整的路由数据
- **AND** 不返回空集合或虚拟数据

---

## REMOVED Requirements

(No requirements removed in this change)

---

## Data Model Changes

### 可能新增的数据表（如果尚未存在）
- `user_route_favorite` (user_id, route_id, created_at)
- `user_route_completion` (user_id, route_id, completed_at, duration_minutes)

### 修改的数据表
- `routes`: usage_count 字段（在用户完成路线时增加）
- 可能需要在 user_id 字段上添加索引以提高性能

### 无需更改的表
- Route 核心实体结构
- RouteDetailResponse 字段名称（仅内容变化）
- API 合约结构

---

## Implementation Dependencies

This spec depends on:
- `api-contract` spec: 定义响应结构和字段名称
- `backend-architecture` spec: 定义分层架构和 DI 模式
- Database schema: RouteTag, Campsite, Supply, WaterSource, MarkerPoint, DailyPlan 表

This spec enables:
- 前端路线详情页面：可显示完整的路线信息
- 路线推荐系统：获取路线属性进行匹配
- 行程规划功能：可使用完整的路线详情

---

## Validation Strategy

### Unit Testing
- Test enrichRouteDetail() 方法使用 mock repositories
- Test 推荐算法在各种输入下的行为
- Test 距离计算

### Integration Testing
- 完整路线详情响应（真实数据库）
- 收藏/取消收藏流程端到端测试
- 个人路线列表的分页测试
- 完成跟踪功能测试

### Performance Validation
- 路线详情响应时间：<500ms
- 个人路线列表查询：<200ms
- 无 N+1 查询问题

### Manual QA
- 前端集成测试
- 边界情况测试（不存在的路线、无效的用户）
- 数据库一致性检查
