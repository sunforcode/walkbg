# API文档更新指南

## 🚀 快速更新

### 自动更新（推荐）
```bash
# 运行自动化脚本
./scripts/generate-api-docs.sh
```

### 手动更新
```bash
# 1. 启动应用
mvn spring-boot:run

# 2. 生成OpenAPI规范
curl -s http://localhost:8080/walkbg/api-docs > docs/openapi.json
curl -s http://localhost:8080/walkbg/api-docs.yaml > docs/openapi.yaml

# 3. 访问Swagger UI
open http://localhost:8080/walkbg/swagger-ui/index.html
```

## 📋 更新流程

### 1. 代码变更后
当你修改了Controller、DTO或API相关代码后：

1. **自动触发**：推送到main/develop分支会自动触发GitHub Actions
2. **手动触发**：运行 `./scripts/generate-api-docs.sh`
3. **本地开发**：启动应用后直接访问Swagger UI

### 2. 文档格式

生成的文档包含以下格式：
- **Swagger UI** - 交互式API文档（推荐用于测试）
- **OpenAPI JSON** - 标准规范文件
- **OpenAPI YAML** - YAML格式规范
- **Redoc HTML** - 静态HTML文档
- **Postman Collection** - 用于Postman的API集合

### 3. 访问方式

| 格式 | 本地访问 | 线上访问 |
|------|----------|----------|
| Swagger UI | http://localhost:8080/walkbg/swagger-ui/index.html | - |
| 文档首页 | ./docs/api-docs.html | GitHub Pages |
| OpenAPI JSON | ./docs/openapi.json | GitHub Pages |
| Redoc HTML | ./docs/redoc.html | GitHub Pages |

## ⚙️ 配置说明

### Spring Boot配置
```yaml
# application.yml
springdoc:
  api-docs:
    path: /api-docs
  swagger-ui:
    path: /swagger-ui.html
    enabled: true
```

### 环境控制
```yaml
# application-prod.yml - 生产环境关闭文档
springdoc:
  api-docs:
    enabled: false
  swagger-ui:
    enabled: false
```

## 🔧 自定义配置

### 1. 修改API信息
在 `OpenApiConfig.kt` 中修改：
```kotlin
@OpenAPIDefinition(
    info = Info(
        title = "WalkBG API",
        version = "v1.0",
        description = "徒步旅行助手API"
    )
)
```

### 2. 添加API示例
在Controller方法中添加：
```kotlin
@Operation(
    summary = "查询路线详情",
    description = "根据路线ID获取详细信息",
    responses = [
        ApiResponse(responseCode = "200", description = "查询成功"),
        ApiResponse(responseCode = "404", description = "路线不存在")
    ]
)
```

### 3. 完善数据模型
在DTO类中添加：
```kotlin
@Schema(description = "路线基本信息")
data class RouteBasicResponse(
    @Schema(description = "路线ID", example = "route-123")
    val id: String,

    @Schema(description = "路线名称", example = "香山红叶步道")
    val name: String
)
```

## 📝 最佳实践

### 1. 注解规范
- 所有Controller类添加 `@Tag` 注解
- 所有API方法添加 `@Operation` 注解
- 所有参数添加 `@Parameter` 注解
- 所有DTO添加 `@Schema` 注解

### 2. 描述规范
- 使用中文描述，便于理解
- 提供具体的示例值
- 说明参数的约束条件
- 描述可能的错误情况

### 3. 版本管理
- 使用语义化版本号
- 重大变更时更新版本号
- 在更新日志中记录变更

## 🚨 注意事项

### 1. 安全考虑
- 生产环境关闭Swagger UI
- 不要暴露敏感信息
- 使用适当的认证机制

### 2. 性能考虑
- 文档生成会增加启动时间
- 大型项目考虑分模块文档
- 定期清理旧版本文档

### 3. 团队协作
- 统一注解规范
- 定期更新文档
- 及时同步API变更

## 🛠️ 故障排除

### 常见问题

1. **应用启动失败**
   ```bash
   # 检查端口占用
   lsof -i :8080

   # 清理数据库锁
   rm -f data/walkbgdb.mv.db.lock
   ```

2. **文档生成失败**
   ```bash
   # 检查应用健康状态
   curl http://localhost:8080/walkbg/actuator/health

   # 检查API文档端点
   curl http://localhost:8080/walkbg/api-docs
   ```

3. **GitHub Actions失败**
   - 检查Java版本配置
   - 确认依赖安装成功
   - 查看应用启动日志

### 获取帮助
- 查看应用日志：`tail -f logs/app.log`
- 运行脚本帮助：`./scripts/generate-api-docs.sh --help`
- 检查GitHub Actions日志

---

*最后更新: 2025-06-20*
