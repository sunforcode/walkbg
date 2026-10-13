# Trip Weather API Backend Mapping

## Authority

本文件只承接根仓当前权威：

- `../app/api/personal-trip-api.md`
- `../app/api/api-common-contract.md`
- `../app/data/weather-schema.md`
- `../app/data/trip-schema.md`
- `../app/data/trip-composition-relations.md`

冲突时以上根仓权威优先。本文件不新增业务行为、Data 字段、端点或天气目标选择算法。

## Current implementation scope

本阶段只补齐 `GET /api/v1/trips/{tripId}/weather` 的后端接口承载：

- 定义当前合同的 Kotlin DTO；
- 保持 Controller 的认证、所有权隐藏与未知查询参数拒绝；
- 保持 `DataResponse<T>` / `ErrorResponse` 封套；
- 在尚无冻结 `TripDayWeatherAssociation` 与 `WeatherForecast` 关系数据时，返回 `503 trip_weather_projection_unavailable`，`retryable=true` 且不返回 `details`；
- 不从旧 `TripDay.weather`、`weatherOverviewJson`、路线区段或当前日期推导新响应；
- 不返回旧天气响应形状。

## Deferred implementation

以下内容必须等待后续经批准的规范和实现批次：

- 天气目标地点与目标时间范围的生成策略；
- WeatherForecast 查询、供应商、刷新和缓存；
- `TripDayWeatherAssociation`、Forecast 及 hike action 关系持久化；
- 生成、取消和路线迁移中的关系原子写入；
- 已有行程旧天气数据迁移。

在这些能力缺失期间，返回 503 比构造默认目标、转换旧天气副本或返回空成功集合更符合当前共享合同。
