# WalkBG 应用

这是一个基于 Spring Boot 和 Kotlin 构建的后台服务应用。

## 技术栈

- Kotlin 1.9.23
- Spring Boot 3.2.3
- Spring Data JPA
- Spring Security
- H2 数据库
- Maven

## 功能特性

- RESTful API
- 用户管理（CRUD操作）
- 数据库集成（H2）
- 安全配置
- 静态资源服务

## 快速开始

### 前提条件

- JDK 17 或更高版本
- Maven 3.6 或更高版本

### 构建和运行

1. 克隆仓库
   \`\`\`bash
   git clone https://github.com/yourusername/walkbg.git
   cd walkbg
   \`\`\`

2. 使用Maven构建项目
   \`\`\`bash
   mvn clean package
   \`\`\`

3. 运行应用
   \`\`\`bash
   java -jar target/walkbg-1.0-SNAPSHOT.jar
   \`\`\`

4. 访问应用
   - Web界面: http://localhost:8080/walkbg
   - API端点: http://localhost:8080/walkbg/api/users
   - H2控制台: http://localhost:8080/walkbg/h2-console

## API文档

### 用户API

| 方法   | URL                   | 描述             |
|--------|----------------------|-----------------|
| GET    | /api/users           | 获取所有用户       |
| GET    | /api/users/{id}      | 获取指定ID的用户   |
| POST   | /api/users           | 创建新用户        |
| PUT    | /api/users/{id}      | 更新指定ID的用户   |
| DELETE | /api/users/{id}      | 删除指定ID的用户   |

## 数据库配置

应用使用H2数据库，默认配置如下：

- URL: `jdbc:h2:file:./data/walkbgdb`
- 用户名: `sa`
- 密码: `password`

## 项目结构

\`\`\`
walkbg/
├── src/
│   ├── main/
│   │   ├── kotlin/
│   │   │   └── org/example/
│   │   │       ├── config/       # 配置类
│   │   │       ├── controller/   # 控制器
│   │   │       ├── model/        # 数据模型
│   │   │       ├── repository/   # 数据访问层
│   │   │       ├── service/      # 业务逻辑层
│   │   │       └── WalkbgApplication.kt  # 应用入口
│   │   └── resources/
│   │       ├── static/           # 静态资源
│   │       ├── application.properties  # 应用配置
│   │       ├── schema.sql        # 数据库结构
│   │       └── data.sql          # 初始数据
│   └── test/                     # 测试代码
└── pom.xml                       # Maven配置
\`\`\`

## 许可证

[MIT](LICENSE)