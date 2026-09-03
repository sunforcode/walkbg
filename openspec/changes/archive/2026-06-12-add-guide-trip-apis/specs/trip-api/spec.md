## ADDED Requirements

### Requirement: Trip API路径规范
Trip相关API的路径 SHALL 使用版本化的RESTful格式:
- 基础路径: `/api/v1/trips` (修改自 `/api/trips`)
- 遵循API Contract规范中的URL格式要求
- 所有子路径接口都使用统一的基础路径

#### Scenario: 获取行程列表
- **WHEN** 前端请求 `GET /walkbg/api/v1/trips`
- **THEN** 后端返回状态码 200
- **AND** 响应格式为 `ApiResponse<Page<Trip>>`

#### Scenario: 获取行程详情
- **WHEN** 前端请求 `GET /walkbg/api/v1/trips/trip_001`
- **THEN** 后端返回状态码 200
- **AND** 响应格式为 `ApiResponse<Trip>`

#### Scenario: 旧路径不可用
- **WHEN** 前端请求 `GET /walkbg/api/trips` (旧路径)
- **THEN** 后端返回状态码 404
- **AND** 提示路径已变更

### Requirement: Planned Trips API
系统 SHALL 提供获取计划中行程的专用API端点:
- 路径: `GET /api/v1/trips/planned`
- 返回所有 status=0 (规划中) 的行程
- 支持分页参数(page, size)
- 默认按创建时间倒序排序

#### Scenario: 获取计划行程列表
- **WHEN** 前端请求 `GET /walkbg/api/v1/trips/planned`
- **THEN** 后端返回状态码 200
- **AND** 响应格式为 `ApiResponse<Page<Trip>>`
- **AND** data.content 中所有行程的 status 都为 0

#### Scenario: 空的计划行程列表
- **WHEN** 前端请求 `GET /walkbg/api/v1/trips/planned`
- **AND** 数据库中没有 status=0 的行程
- **THEN** 后端返回状态码 200
- **AND** data.content 为空数组
- **AND** data.totalElements 为 0

#### Scenario: 分页获取计划行程
- **WHEN** 前端请求 `GET /walkbg/api/v1/trips/planned?page=0&size=10`
- **THEN** 后端返回最多10条记录
- **AND** 响应包含分页信息(totalPages, totalElements, number, size)

---

### Requirement: TripService Planned Trips Method
TripService SHALL 提供 `getPlannedTrips` 方法:
- 方法签名: `getPlannedTrips(pageable: Pageable): Page<Trip>`
- 查询条件: status = 0 (规划中状态)
- 排序: 默认按 createdAt 倒序
- 返回分页结果

#### Scenario: Service层查询计划行程
- **WHEN** 调用 `tripService.getPlannedTrips(PageRequest.of(0, 20))`
- **THEN** 返回 Page<Trip> 包含最多20条 status=0 的行程
- **AND** 行程按创建时间从新到旧排序

---

### Requirement: TripRepository Status Query
TripRepository SHALL 提供按状态查询方法(如不存在):
- 方法签名: `findByStatus(status: Int, pageable: Pageable): Page<Trip>`
- 支持分页和排序
- 返回指定状态的所有行程

#### Scenario: Repository按状态查询
- **WHEN** 调用 `tripRepository.findByStatus(0, pageable)`
- **THEN** 返回所有 status=0 的Trip实体
- **AND** 支持传入的分页和排序参数
