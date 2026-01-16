# Change: Add Guide and Trip APIs

## Why
前端应用需要获取攻略(Guide)列表和计划中的行程(Planned Trips)数据,但后端目前缺少以下接口:
1. 攻略相关的完整API (`GET /api/v1/guides`)
2. 行程API的路径不符合规范(应为 `/api/v1/trips` 而非 `/api/trips`)
3. 缺少获取计划行程的子路径接口 (`GET /api/v1/trips/planned`)

## What Changes
- **NEW**: 创建 GuideController 实现攻略列表API
- **NEW**: 创建 Guide 相关的 Entity、Repository、Service 层
- **MODIFIED**: 修改 TripController 路径从 `/api/trips` 改为 `/api/v1/trips`
- **NEW**: 添加 `/api/v1/trips/planned` 接口返回状态为0(规划中)的行程

## Impact
- **Affected specs**: `api-contract` (添加新的API端点定义)
- **Affected code**: 
  - 新增: `org.example.guide.*` 包下的所有类
  - 修改: `org.example.trip.controller.TripController` 路径注解
  - 新增: 数据库表 `guides` 及相关字段

**Breaking Changes**: ⚠️ TripController 路径变更可能影响已有客户端
