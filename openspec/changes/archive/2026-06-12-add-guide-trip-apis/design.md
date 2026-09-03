# Design: Guide and Trip APIs

## Context
前端Flutter应用已经实现了攻略(Guide)和行程(Trip)功能的UI和Service层,但后端API缺失导致无法获取数据。需要补齐这些API以完成前后端对接。

约束:
- 必须符合现有的API规范 (`specs/api-contract/spec.md`)
- 使用统一的响应格式 `ApiResponse<T>`
- 遵循分层架构: Controller → ApplicationService → DomainService → Repository
- 使用Spring Boot 3.2.3 + Kotlin + JPA

## Goals / Non-Goals

**Goals:**
- 实现 Guide 攻略相关的完整CRUD API
- 修正 Trip API路径使其符合RESTful规范
- 添加获取计划行程的专用接口
- 确保与前端已有的模型和调用逻辑兼容

**Non-Goals:**
- 不实现攻略的评论、点赞等交互功能(后续迭代)
- 不实现攻略的富文本编辑器(后续迭代)
- 不涉及行程的复杂状态流转逻辑(本次仅查询)

## Decisions

### Decision 1: Guide Entity 设计
**选择**: 创建独立的 `Guide` Entity,包含以下核心字段:
- `id`: String (主键)
- `title`: String (攻略标题)
- `content`: String (攻略内容,TEXT类型)
- `author_id`: String (作者用户ID)
- `cover_url`: String (封面图片URL)
- `tags`: String (标签,逗号分隔或JSON)
- `view_count`: Int (浏览次数)
- `like_count`: Int (点赞数)
- `status`: Int (状态: 0-草稿,1-已发布,2-已下线)
- `created_at`: Instant
- `updated_at`: Instant

**理由**:
- 符合前端 `GuideModel` 的字段定义
- 使用单向关联,避免循环依赖
- `author_id` 不创建外键,通过查询关联用户

**替代方案**: 
- 方案A: 嵌入到 Route 中作为路线攻略 → 拒绝,攻略和路线是独立实体
- 方案B: 使用MongoDB存储富文本 → 拒绝,当前技术栈是MySQL

### Decision 2: TripController 路径修改
**选择**: 将 `@RequestMapping("/api/trips")` 改为 `@RequestMapping("/api/v1/trips")`

**理由**:
- 符合API版本化规范
- 与其他Controller路径一致
- 前端已经按 `/api/v1/trips` 调用

**影响**: 
- **Breaking Change**: 如果有其他客户端使用旧路径,需要迁移
- 缓解措施: 可以临时保留旧路径的兼容性接口(标记为@Deprecated)

### Decision 3: Planned Trips 接口设计
**选择**: 添加 `GET /api/v1/trips/planned` 返回 `status=0` 的行程

**理由**:
- 语义清晰,符合RESTful风格
- 避免前端需要传递过滤参数
- 后端可以优化该查询(如添加索引)

**替代方案**:
- 方案A: 使用 `GET /api/v1/trips?status=0` → 可行,但不够语义化
- 方案B: 使用 `GET /api/v1/trips?filter=planned` → 过于通用,难以优化

### Decision 4: Guide 分页与排序
**选择**: Guide列表API支持以下参数:
- `tag`: String? (按标签筛选)
- `limit`: Int? (返回数量,默认20,最大100)
- `page`: Int? (页码,从0开始,默认0)
- `sort`: String? (排序,默认 `created_at,desc`)

**理由**:
- 符合现有的分页规范
- `tag` 参数支持前端的标签筛选需求
- `limit` 参数用于首页显示少量数据的场景

## Risks / Trade-offs

### Risk 1: TripController 路径变更影响现有客户端
**影响**: 如果有其他服务或测试脚本使用旧路径,会失败

**缓解方案**:
1. 检查所有测试用例,更新URL
2. 搜索代码库中的硬编码URL
3. 如需要,添加临时的兼容性路由(@Deprecated标记)

### Risk 2: Guide数据库表不存在
**影响**: 如果数据库未初始化 `guides` 表,启动会失败

**缓解方案**:
1. 在 `data-mysql.sql` 中添加表结构和测试数据
2. 使用 JPA `ddl-auto: update` 自动创建表
3. 在README中添加数据库迁移说明

### Trade-off: 简化的Guide实现
**选择**: 本次仅实现基础的CRUD,不包含评论、收藏等功能

**理由**: 
- 优先满足前端显示攻略列表的需求
- 交互功能可以在后续迭代中添加
- 降低初次开发的复杂度

## Migration Plan

### Phase 1: 开发与测试 (本次变更)
1. 创建 Guide 相关类
2. 修改 TripController 路径
3. 添加单元测试和集成测试
4. 本地验证前端调用

### Phase 2: 数据迁移 (如有生产数据)
1. 备份现有数据库
2. 执行 DDL 语句创建 `guides` 表
3. (可选)导入历史数据

### Phase 3: 部署与监控
1. 部署到测试环境
2. 前端集成测试
3. 监控API调用日志
4. 如发现问题,可快速回滚

**Rollback Plan**:
- 如果Guide功能有问题,可以先下线该API,不影响其他功能
- TripController路径变更如有问题,可以临时恢复旧路径

## Open Questions

1. **攻略的权限控制?**
   - 当前假设所有已发布的攻略都是公开可见
   - 后续需要考虑作者可见/粉丝可见等权限控制

2. **攻略与路线的关联?**
   - 当前设计为独立实体
   - 后续可以添加 `Guide.route_id` 字段关联路线

3. **攻略内容的富文本格式?**
   - 当前存储为纯文本
   - 后续可以使用Markdown或HTML格式
