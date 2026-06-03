# 后端 API 最小实现审计

本文档用于把当前后端接口从“能扫描到的接口集合”收敛成“几端共同遵循的最小 API 合同”。它不直接要求马上删除代码，而是先明确哪些接口可以作为标准保留，哪些应下线、合并或停止对外声明，哪些需要补实现。

审计日期：2026-06-02

## 审计口径

最小实现只保留能支撑当前产品闭环的接口：

- Flutter C 端：登录注册、当前用户、路线发现和详情、路线收藏和完成、行程基础管理、攻略浏览、装备清单和模板。
- React 管理端：登录、用户/路线/行程/攻略/装备/分析任务的列表、详情和必要管理操作。
- 后端内部或 Agent：KML 路线分析提交、任务查询、回调和必要的 SSE。

统一判断规则：

- 保留：已经有明确前端使用、业务闭环需要、返回结构相对稳定的接口。
- 删除/合并：旧路径、重复查询入口、实体裸返回、没有当前业务闭环、或者可由更高层聚合接口覆盖的接口。
- 补充：前端已经调用但后端没有、当前实现是空结果/未实现/硬编码、或者路径和响应合同不一致的接口。

## 当前暴露面

当前后端共有 21 个 Controller，209 个 Mapping 方法。主要问题不是数量，而是接口层级和可靠性不一致：

- `/api/v1/...` 和 `/api/...` 混用。
- 部分接口返回 JPA Entity，部分返回 DTO。
- 部分接口直接在 Controller 访问 Repository。
- 部分接口是空实现、硬编码或抛出“暂未实现”。
- Flutter 和 React Admin 已声明的路径与后端实际路径不完全一致。

## Controller 级结论

| 模块 | Controller | 当前基础路径 | Mapping 数 | 最小结论 |
| --- | --- | --- | ---: | --- |
| 认证 | `AuthController` | `/api/v1/auth` | 6 | 保留登录、注册、刷新、登出；用户名/邮箱检查可保留为注册辅助。 |
| 当前用户 | `CurrentUserController` | `/api/v1/user` | 5 | 保留 profile/stats；preferences 目前是硬编码，应删除或补持久化。 |
| 用户管理 | `UserController` | `/api/v1/users` | 7 | 保留管理端列表、详情、创建、更新、删除、统计；`/username/{username}` 可合并到列表查询。 |
| 路线 | `RouteController` | `/api/v1/routes` | 19 | 保留路线列表/详情/创建、收藏/完成、热门/新晋/季节/周末、segments/pois；未实现和空结果接口需下线或补齐。 |
| 路线分析 | `RouteAnalysisController` | `/api/v1/route-analysis` | 3 | 保留 health/analyze/tasks。 |
| 路线分析回调 | `KmlAnalysisCallbackController` | `/api/v1/route-analysis` | 2 | 保留 callback；callback health 应标记内部或合并到 health。 |
| 路线分析 SSE | `SseAnalysisController` | `/api/v1/route-analysis` | 1 | React Admin 已使用，保留 `/tasks/{taskId}/stream`。 |
| 路线地图数据 | `RouteMapDataController` | `/api/route-map-data` | 4 | 删除或合并到路线详情/分析结果，不作为公开最小 API。 |
| 营地 | `CampsiteController` | `/api/campsites` | 13 | 不保留独立公开 CRUD；路线详情或 `/routes/{id}/pois` 覆盖。 |
| 补给 | `SupplyController` | `/api/supplies` | 10 | 不保留独立公开 CRUD；路线详情或 `/routes/{id}/pois` 覆盖。 |
| 联系方式 | `ContactController` | `/api/contacts` | 16 | 不保留独立公开 CRUD；路线搭车/联系人信息应作为路线详情字段或 v1 路线子资源。 |
| 水源 | `WaterSourceController` | `/api/water-sources` | 11 | 不保留独立公开 CRUD；路线详情或 `/routes/{id}/pois?category=water` 覆盖。 |
| 行程 | `TripController` | `/api/v1/trips` | 18 | 保留 CRUD、状态更新、统计、列表查询；重复的 user/organizer/status/planned/upcoming 等入口应合并成查询参数。 |
| 行程参与者 | `TripParticipantController` | `/api/trip-participants` | 16 | 删除独立旧路径；合并到 `/api/v1/trips/{tripId}/participants`。 |
| 装备物品 | `EquipmentController` | `/api/equipment` | 16 | 需要迁移到 `/api/v1/equipment`；保留物品 CRUD、列表、搜索、分类统计。 |
| 装备清单 | `EquipmentListController` | `/api/v1/equipment-lists` | 12 | 保留；需补当前用户身份，移除 `admin` 硬编码，收敛 Map 请求/响应。 |
| 装备模板 | `EquipmentTemplateController` | `/api/v1/equipment-templates` | 7 | 保留列表、详情、创建、从清单创建、官方模板；popular/recommended 当前语义不清，应删除或重做。 |
| 用户装备库 | `UserEquipmentInventoryController` | `/api/v1/user-equipment` | 9 | 可保留为 C 端能力；需和 Flutter 的 `/user-equipment-inventories` 统一，并改为当前用户语义。 |
| 餐食计划 | `MealPlanController` | `/api/meal-plans` | 12 | 最小实现删除/暂缓；如要保留，应改为行程子资源。 |
| 饮水计划 | `WaterPlanController` | `/api/water-plans` | 13 | 最小实现删除/暂缓；如要保留，应改为行程子资源。 |
| 攻略 | `GuideController` | `/api/v1/guides` | 9 | 保留列表、详情、管理端增删改、发布/下线；like/unlike 需补用户级幂等。 |

## 应保留的最小 API 合同

### 认证和用户

保留：

- `POST /api/v1/auth/login`
- `POST /api/v1/auth/register`
- `POST /api/v1/auth/refresh`
- `POST /api/v1/auth/logout`
- `GET /api/v1/auth/check-username/{username}`
- `GET /api/v1/auth/check-email/{email}`
- `GET /api/v1/user/profile`
- `PUT /api/v1/user/profile`
- `GET /api/v1/user/stats`
- `GET /api/v1/users`
- `GET /api/v1/users/{id}`
- `POST /api/v1/users`
- `PUT /api/v1/users/{id}`
- `DELETE /api/v1/users/{id}`
- `GET /api/v1/users/{id}/stats`

调整：

- `/api/v1/user/preferences` 目前未落库，应先移出标准合同，或补充偏好表和 DTO。
- Flutter 声明了 `uploadAvatar`，后端未提供，应补 `POST /api/v1/user/avatar` 或删除客户端声明。
- Flutter 声明了 forgot/reset password，后端未提供。若 MVP 不做找回密码，应从客户端常量移除；若要做，补 `POST /api/v1/auth/forgot-password` 和 `POST /api/v1/auth/reset-password`。

### 路线

保留：

- `GET /api/v1/routes`
- `GET /api/v1/routes/{id}`
- `POST /api/v1/routes`
- `POST /api/v1/routes/{id}/favorite`
- `DELETE /api/v1/routes/{id}/favorite`
- `POST /api/v1/routes/{id}/complete`
- `GET /api/v1/routes/my`
- `GET /api/v1/routes/favorites`
- `GET /api/v1/routes/completed`
- `GET /api/v1/routes/popular`
- `GET /api/v1/routes/new`
- `GET /api/v1/routes/seasonal`
- `GET /api/v1/routes/weekend`
- `GET /api/v1/routes/{id}/segments`
- `GET /api/v1/routes/{id}/pois`

删除或暂停曝光：

- `PUT /api/v1/routes/{id}`：当前直接抛“更新功能暂未实现”。
- `DELETE /api/v1/routes/{id}`：当前直接抛“删除功能暂未实现”。
- `GET /api/v1/routes/recommendations`：当前返回空 Page。
- `GET /api/v1/routes/nearby`：当前返回空 Page。

补充：

- Flutter 使用 `/api/v1/routes/search`，后端当前用 `GET /api/v1/routes?keyword=...`。建议保留一个标准：优先使用列表查询参数，并删除客户端 `searchRoutes` 专用路径；如保留搜索路径，则后端补别名。
- Flutter 声明了 region/difficulty/duration/ratings/tags/waypoints/related/comments 等路径，后端未提供。最小实现不补，先从客户端标准端点移除或标记为未来能力。
- 收藏当前是路线子资源，Flutter 声明为 `/api/v1/favorites/routes`。建议统一到路线子资源，删除 Flutter 的 favorites 独立路径；如果产品偏独立收藏模块，再补 FavoriteController。
- `favorite/complete/my/favorites/completed/segments/pois` 目前有 Controller 直连 Repository 的情况，应迁移到 ApplicationService。

### 路线分析和 Agent

保留：

- `GET /api/v1/route-analysis/health`
- `POST /api/v1/route-analysis/analyze`
- `GET /api/v1/route-analysis/tasks/{taskId}`
- `GET /api/v1/route-analysis/tasks/{taskId}/stream`
- `POST /api/v1/route-analysis/callback`

调整：

- `GET /api/v1/route-analysis/callback/health` 不建议公开给客户端；可合并到 health 或仅作为内部诊断。
- OpenAPI 中应明确 analyze 请求支持 `kml_source`、`kml_content`、`route_id`、`region_name` 等字段，避免管理端和 Agent 服务各自猜结构。

### 行程

保留：

- `GET /api/v1/trips`
- `GET /api/v1/trips/{id}`
- `POST /api/v1/trips`
- `PUT /api/v1/trips/{id}`
- `DELETE /api/v1/trips/{id}`
- `PATCH /api/v1/trips/{id}/status`
- `GET /api/v1/trips/statistics`

合并：

- `GET /api/v1/trips/search`
- `GET /api/v1/trips/user/{userId}`
- `GET /api/v1/trips/participant/{userId}`
- `GET /api/v1/trips/organizer/{organizerId}`
- `GET /api/v1/trips/status/{status}`
- `GET /api/v1/trips/planned`
- `GET /api/v1/trips/upcoming`
- `GET /api/v1/trips/ongoing`
- `GET /api/v1/trips/completed`
- `GET /api/v1/trips/popular`
- `GET /api/v1/trips/recent`

建议收敛为：

- `GET /api/v1/trips?keyword=&status=&organizerId=&participantId=&scope=&sort=`

补充：

- 当前多个行程接口返回 `Trip` Entity。最小合同应补 `TripCreateRequest`、`TripUpdateRequest`、`TripBasicResponse`、`TripDetailResponse`。
- `/api/trip-participants` 应迁移为 `/api/v1/trips/{tripId}/participants`，最小只保留参与者列表、添加、移除、状态更新、是否参与检查。
- Flutter 声明 `/api/v1/trip-plans`，后端没有。最小实现不建议新增独立 TripPlan，先由 Trip 承载；如确实要做，单独立 OpenSpec。

### 装备

保留并迁移：

- `GET /api/v1/equipment/items`
- `GET /api/v1/equipment/items/{id}`
- `POST /api/v1/equipment/items`
- `PUT /api/v1/equipment/items/{id}`
- `DELETE /api/v1/equipment/items/{id}`
- `GET /api/v1/equipment/items/search`
- `GET /api/v1/equipment/items/category/{category}`
- `GET /api/v1/equipment/category-stats`
- `GET /api/v1/equipment-lists`
- `GET /api/v1/equipment-lists/{id}`
- `POST /api/v1/equipment-lists`
- `PUT /api/v1/equipment-lists/{id}`
- `DELETE /api/v1/equipment-lists/{id}`
- `GET /api/v1/equipment-lists/{id}/items`
- `POST /api/v1/equipment-lists/{id}/items`
- `PUT /api/v1/equipment-lists/{id}/items/{itemId}`
- `DELETE /api/v1/equipment-lists/{id}/items/{itemId}`
- `GET /api/v1/equipment-lists/{id}/weight-stats`
- `PATCH /api/v1/equipment-lists/{id}/status`
- `GET /api/v1/equipment-templates`
- `GET /api/v1/equipment-templates/{id}`
- `POST /api/v1/equipment-templates`
- `POST /api/v1/equipment-templates/from-list`
- `GET /api/v1/equipment-templates/official`

删除或暂停曝光：

- 当前 `EquipmentController` 是 `/api/equipment`，和 React Admin、Flutter 的 `/api/v1/equipment` 不一致。最小实现必须迁移或增加 v1 路径。
- `GET /api/equipment/latest`
- `GET /api/equipment/lightest`
- `GET /api/equipment/heaviest`
- `GET /api/equipment/weight-range`
- `GET /api/equipment/similar-weight`
- `GET /api/equipment/search-by-name`
- `GET /api/v1/equipment-templates/popular`：当前返回热门装备统计，不是模板。
- `GET /api/v1/equipment-templates/recommended`：当前返回装备 item id，不是模板。

补充：

- 装备物品接口应返回 DTO，不直接返回 `EquipmentItem` Entity。
- 装备清单创建和查询当前默认 `admin`，应改为当前登录用户或管理端显式 creator。
- Flutter 声明 `/api/v1/user-equipment-inventories`，后端是 `/api/v1/user-equipment`。二选一统一；建议使用 `/api/v1/user-equipment` 并在 Flutter 中迁移。
- 用户装备库如保留，应避免 path 中传任意 `userId`，优先 `GET /api/v1/user-equipment` 表示当前用户，管理端另设 `/api/v1/users/{userId}/equipment`。

### 攻略

保留：

- `GET /api/v1/guides`
- `GET /api/v1/guides/{id}`
- `POST /api/v1/guides`
- `PUT /api/v1/guides/{id}`
- `DELETE /api/v1/guides/{id}`
- `POST /api/v1/guides/{id}/publish`
- `POST /api/v1/guides/{id}/offline`

调整：

- `POST /api/v1/guides/{id}/like`
- `POST /api/v1/guides/{id}/unlike`

点赞/取消点赞如果保留，必须补用户级幂等记录，否则重复调用会直接改变计数。

补充：

- Flutter 声明 `/api/v1/guides/popular` 和 `/api/v1/guides/categories`，后端未提供。最小实现可以不补，先从客户端标准端点移除；如果首页需要，补 `popular` 和 `categories`。

### 路线附属资源

最小实现不保留这些独立公开路径：

- `/api/campsites/**`
- `/api/water-sources/**`
- `/api/supplies/**`
- `/api/contacts/**`
- `/api/route-map-data/**`

推荐替代：

- 路线详情中返回必要附属数据：营地、水源、补给点、联系人、轨迹、分段、POI。
- 读接口使用 `GET /api/v1/routes/{id}/pois?category=...`。
- 管理端如确实需要维护这些资源，再补 v1 路线子资源，例如 `/api/v1/routes/{routeId}/campsites`，不要继续扩散 `/api/...` 旧路径。

### 餐食和饮水计划

最小实现不保留这些独立公开路径：

- `/api/meal-plans/**`
- `/api/water-plans/**`

理由：

- 当前路径不是 v1。
- 当前不是 Flutter 和 React Admin 的必要闭环。
- 行程计划能力还没有和 Trip 合同统一。

如果产品要做“行程计划”，建议先定义一个 TripPlan 能力，而不是同时公开 MealPlan、WaterPlan、TripPlan 三套入口。推荐路径：

- `GET /api/v1/trips/{tripId}/plan`
- `PUT /api/v1/trips/{tripId}/plan`
- `GET /api/v1/trips/{tripId}/meal-plan`
- `PUT /api/v1/trips/{tripId}/meal-plan`
- `GET /api/v1/trips/{tripId}/water-plan`
- `PUT /api/v1/trips/{tripId}/water-plan`

## 前后端不一致清单

| 客户端声明 | 后端现状 | 处理建议 |
| --- | --- | --- |
| `/api/v1/equipment/items` | 后端是 `/api/equipment/items` | P0，后端迁移到 `/api/v1/equipment` 或补兼容路由。 |
| `/api/v1/equipment/category-stats` | 后端是 `/api/equipment/category-stats` | P0，同上。 |
| `/api/v1/routes/search` | 后端是 `/api/v1/routes?keyword=` | P1，统一为查询参数，删除客户端专用路径。 |
| `/api/v1/favorites/routes/**` | 后端是 `/api/v1/routes/{id}/favorite` | P1，统一为路线子资源。 |
| `/api/v1/user-equipment-inventories` | 后端是 `/api/v1/user-equipment` | P1，统一命名。 |
| `/api/v1/guides/popular` | 后端无 | P1，如果首页需要则补；否则删客户端声明。 |
| `/api/v1/guides/categories` | 后端无 | P1，如果筛选需要则补；否则删客户端声明。 |
| `/api/v1/trip-plans` | 后端无 | P2，先不补，等行程计划规格明确。 |
| `/api/v1/weather/**` | 后端无统一 Controller | P2，Flutter 若直接接第三方天气，则删除后端端点声明。 |
| `/api/v1/upload/**` | 后端未提供 | P1/P2，头像和路线文件上传需要单独定义。 |
| `/api/system/health` | 后端未见对应 Controller | P1，补统一 health 或移除网络检测调用。 |

## P0 必须先处理

1. 统一公开路径到 `/api/v1`，尤其是装备物品接口。
2. 从标准合同中移除或补齐 `PUT/DELETE /api/v1/routes/{id}`、`routes/recommendations`、`routes/nearby`。
3. 移除 `CurrentUserController` 的硬编码 preferences，或补持久化。
4. 行程和装备物品不要直接返回 Entity，补 DTO。
5. `EquipmentListController` 不应硬编码 creator 为 `admin`。
6. 更新 `docs/openapi.yaml` 和 `docs/openapi.json`，让它们只描述标准合同。

## P1 收敛项

1. 把 Trip 的多个重复集合入口收敛为 `GET /api/v1/trips` 查询参数。
2. 把 `/api/trip-participants` 合并到 `/api/v1/trips/{tripId}/participants`。
3. 把营地、水源、补给、联系人、地图数据合并到路线详情或路线子资源。
4. 统一 Flutter `ApiEndpoints`、React Admin `src/services/api.js` 和后端 OpenAPI。
5. 所有公开接口使用 `ApiResponse<T>`，分页使用统一 Page 结构。
6. Controller 只做参数和响应编排，业务逻辑进入 ApplicationService/DomainService。

## P2 可延后能力

1. 路线推荐、附近路线、个性化推荐。
2. 攻略热门、攻略分类、攻略点赞用户记录。
3. 天气、上传、系统配置、反馈。
4. 餐食计划、饮水计划、完整 TripPlan。
5. 装备高级排行、相似重量、推荐装备。

## 建议落地顺序

1. 新建 OpenSpec 变更：`stabilize-backend-minimum-api-contract`。
2. 写 `specs/backend-api/spec.md`，先定义上面“应保留的最小 API 合同”。
3. 后端先做 P0：路径统一、假接口下线、DTO 和身份修复。
4. Flutter 和 React Admin 只保留标准合同路径，删除未来端点常量或标记为未启用。
5. 重新生成 OpenAPI，并把 OpenAPI 作为几端对齐的唯一接口入口。
6. 后续新增接口必须先改 OpenSpec/OpenAPI，再改后端和客户端。
