# Implementation Tasks

## 1. Guide API 实现
- [x] 1.1 创建 Guide Entity (`org.example.guide.model.Guide`)
- [x] 1.2 创建 GuideRepository 接口
- [x] 1.3 创建 GuideService 接口和实现
- [x] 1.4 创建 GuideController 实现 GET /api/v1/guides
- [x] 1.5 添加 Guide DTO 类 (GuideBasicResponse)
- [x] 1.6 配置数据库表 `guides` (如需要)
- [x] 1.7 添加测试数据到 data-mysql.sql

## 2. Trip API 路径修正
- [x] 2.1 修改 TripController @RequestMapping 为 "/api/v1/trips"
- [x] 2.2 添加 GET /api/v1/trips/planned 接口
- [x] 2.3 在 TripService 中添加 getPlannedTrips 方法
- [x] 2.4 在 TripRepository 中添加按状态查询方法(如不存在)

## 3. 测试与验证
- [x] 3.1 单元测试 GuideService
- [x] 3.2 集成测试 GuideController
- [x] 3.3 测试 TripController 新路径
- [x] 3.4 验证前端调用成功

## 4. 文档更新
- [x] 4.1 更新 Swagger 文档
- [x] 4.2 验证 openspec validate --strict 通过
