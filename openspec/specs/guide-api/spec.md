# guide-api Specification

## Purpose
TBD - created by archiving change add-guide-trip-apis. Update Purpose after archive.
## Requirements
### Requirement: Guide Entity and Repository
系统 SHALL 提供 Guide(攻略) 实体和数据访问层:
- Guide Entity 包含字段: id, title, content, authorId, coverUrl, tags, viewCount, likeCount, status, createdAt, updatedAt
- GuideRepository 继承 JpaRepository 提供基础CRUD
- GuideRepository 提供按状态查询方法 `findByStatus(status: Int, pageable: Pageable)`
- GuideRepository 提供按标签查询方法 `findByTagsContaining(tag: String, pageable: Pageable)`

#### Scenario: 创建Guide实体
- **WHEN** 系统启动时
- **THEN** JPA自动映射 Guide 实体到 `guides` 表
- **AND** 表结构包含所有必需字段

#### Scenario: 按状态查询已发布攻略
- **WHEN** 调用 `guideRepository.findByStatus(1, pageable)`
- **THEN** 返回所有 status=1 的攻略
- **AND** 支持分页和排序

---

### Requirement: Guide Service Layer
系统 SHALL 提供 GuideService 实现业务逻辑:
- `getGuides(tag: String?, limit: Int, pageable: Pageable): Page<Guide>` 获取攻略列表
- `getGuideById(id: String): Guide?` 获取攻略详情
- `createGuide(guide: Guide): Guide` 创建攻略
- `updateGuide(id: String, guide: Guide): Guide?` 更新攻略
- `deleteGuide(id: String): Boolean` 删除攻略

#### Scenario: 获取已发布攻略列表
- **WHEN** 调用 `guideService.getGuides(null, 20, pageable)`
- **THEN** 返回最多20条 status=1 的攻略
- **AND** 按创建时间倒序排序

#### Scenario: 按标签筛选攻略
- **WHEN** 调用 `guideService.getGuides("登山技巧", 10, pageable)`
- **THEN** 返回包含"登山技巧"标签的攻略
- **AND** 最多返回10条记录

---

### Requirement: Guide API Endpoints
系统 SHALL 提供以下 RESTful API 端点:
- `GET /api/v1/guides` 获取攻略列表
- `GET /api/v1/guides/{id}` 获取攻略详情
- `POST /api/v1/guides` 创建攻略
- `PUT /api/v1/guides/{id}` 更新攻略
- `DELETE /api/v1/guides/{id}` 删除攻略

#### Scenario: 获取攻略列表
- **WHEN** 前端请求 `GET /walkbg/api/v1/guides?tag=登山&limit=10`
- **THEN** 后端返回状态码 200
- **AND** 响应格式为 `ApiResponse<Page<GuideBasicResponse>>`
- **AND** data.content 包含攻略数组

#### Scenario: 获取攻略详情
- **WHEN** 前端请求 `GET /walkbg/api/v1/guides/guide_001`
- **THEN** 后端返回状态码 200
- **AND** 响应格式为 `ApiResponse<GuideBasicResponse>`
- **AND** data 包含完整的攻略信息

#### Scenario: 攻略不存在
- **WHEN** 前端请求 `GET /walkbg/api/v1/guides/non-existent-id`
- **THEN** 后端返回状态码 404
- **AND** 响应包含错误信息 "攻略不存在"

---

### Requirement: Guide DTO Mapping
系统 SHALL 提供 GuideBasicResponse DTO 用于API响应:
- 字段映射符合 API Contract 规范(snake_case)
- 使用 `@JsonProperty` 注解进行字段映射
- 提供 `fromGuide(guide: Guide)` 静态方法转换

#### Scenario: Entity转DTO
- **WHEN** Guide实体字段为 `authorId="user_001", createdAt=Instant`
- **THEN** JSON输出为 `{"author_id": "user_001", "created_at": "2025-01-15T10:00:00Z"}`
- **AND** 所有字段使用snake_case命名

---

### Requirement: Guide Test Data
系统 SHALL 在 data-mysql.sql 中提供测试数据:
- 至少包含3条不同类型的攻略记录
- 包含不同状态(草稿、已发布)的攻略
- 包含不同标签的攻略

#### Scenario: 初始化测试数据
- **WHEN** 数据库初始化时
- **THEN** guides 表包含至少3条记录
- **AND** 每条记录字段完整且符合Entity定义
