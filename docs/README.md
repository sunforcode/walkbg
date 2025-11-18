# WalkBG API 文档

## 📖 文档访问

### 在线文档
- [Swagger UI (交互式)](http://localhost:8080/walkbg/swagger-ui/index.html) - 推荐用于API测试
- [API文档首页](./api-docs.html) - 文档概览和快速导航

### 文档文件
- [OpenAPI JSON规范](./openapi.json) - 标准的OpenAPI 3.0规范
- [OpenAPI YAML规范](./openapi.yaml) - YAML格式的规范文件
- [Postman Collection](./WalkBG-API.postman_collection.json) - 用于Postman的API集合

## 🚀 快速开始

### 1. 启动应用
```bash
mvn spring-boot:run
```

### 2. 访问文档
打开浏览器访问: http://localhost:8080/walkbg/swagger-ui/index.html

### 3. 测试API
在Swagger UI中可以直接测试所有API接口

## 📋 API概览

### 核心模块
- **路线管理** - 路线的CRUD操作、搜索、推荐
- **用户管理** - 用户注册、信息管理
- **水源管理** - 水源点信息管理
- **补给管理** - 补给点信息管理
- **营地管理** - 营地信息管理
- **联系人管理** - 向导、接送等联系人管理
- **行程管理** - 徒步行程规划和管理

### 路线管理API
- `GET /api/v1/routes` - 分页查询路线列表
- `GET /api/v1/routes/{id}` - 查询路线详情
- `POST /api/v1/routes` - 创建路线
- `PUT /api/v1/routes/{id}` - 更新路线
- `DELETE /api/v1/routes/{id}` - 删除路线
- `GET /api/v1/routes/my` - 查询我创建的路线
- `GET /api/v1/routes/favorites` - 查询我收藏的路线
- `GET /api/v1/routes/completed` - 查询我完成的路线
- `GET /api/v1/routes/recommendations` - 获取推荐路线
- `GET /api/v1/routes/nearby` - 获取附近的路线

## 🔧 开发信息

- **基础URL**: http://localhost:8080/walkbg
- **API版本**: v1
- **数据格式**: JSON
- **响应格式**: 统一的ApiResponse包装

## 📝 更新日志

- **2025-06-20**: 文档自动生成和更新
- **2025-06-20**: 初始版本发布

---

*文档最后更新: 2025-06-20 20:26:51*
