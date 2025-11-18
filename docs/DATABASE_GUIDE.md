# 数据库配置指南

## 📋 目录
- [当前配置](#当前配置)
- [切换到 MySQL](#切换到-mysql)
- [切换到 PostgreSQL](#切换到-postgresql)
- [生产环境部署](#生产环境部署)
- [数据迁移](#数据迁移)
- [常见问题](#常见问题)

---

## 当前配置

项目默认使用 **H2 文件数据库**，数据已经是持久化的：
- 数据文件位置：`./data/walkbgdb.mv.db`
- 重启应用后数据不会丢失
- 适合开发和测试环境

### 访问 H2 控制台
```
URL: http://localhost:8080/walkbg/h2-console
JDBC URL: jdbc:h2:file:./data/walkbgdb
用户名: sa
密码: (留空)
```

---

## 切换到 MySQL

### 1. 安装 MySQL

**macOS:**
```bash
brew install mysql
brew services start mysql
```

**Ubuntu/Debian:**
```bash
sudo apt-get update
sudo apt-get install mysql-server
sudo systemctl start mysql
```

**CentOS/RHEL:**
```bash
sudo yum install mysql-server
sudo systemctl start mysqld
```

### 2. 创建数据库

**方法一：使用脚本（推荐）**
```bash
./scripts/setup-mysql.sh
```

**方法二：手动创建**
```bash
# 登录 MySQL
mysql -u root -p

# 创建数据库
CREATE DATABASE walkbg CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;

# 创建用户（可选）
CREATE USER 'walkbg_user'@'localhost' IDENTIFIED BY 'your_password';
GRANT ALL PRIVILEGES ON walkbg.* TO 'walkbg_user'@'localhost';
FLUSH PRIVILEGES;
```

### 3. 配置应用

编辑 `src/main/resources/application-mysql.yml`：
```yaml
spring:
  datasource:
    url: jdbc:mysql://localhost:3306/walkbg?useUnicode=true&characterEncoding=utf8&useSSL=false&serverTimezone=Asia/Shanghai
    username: root
    password: your_password_here  # 修改为你的密码
```

### 4. 启动应用

**方法一：使用 profile**
```bash
mvn spring-boot:run -Dspring-boot.run.profiles=mysql
```

**方法二：设置环境变量**
```bash
export SPRING_PROFILES_ACTIVE=mysql
mvn spring-boot:run
```

**方法三：在 IDEA 中配置**
- Run → Edit Configurations
- Active profiles: `mysql`

---

## 切换到 PostgreSQL

### 1. 安装 PostgreSQL

**macOS:**
```bash
brew install postgresql
brew services start postgresql
```

**Ubuntu/Debian:**
```bash
sudo apt-get install postgresql postgresql-contrib
sudo systemctl start postgresql
```

### 2. 添加依赖

在 `pom.xml` 中添加：
```xml
<dependency>
    <groupId>org.postgresql</groupId>
    <artifactId>postgresql</artifactId>
    <scope>runtime</scope>
</dependency>
```

### 3. 创建数据库

```bash
# 切换到 postgres 用户
sudo -u postgres psql

# 创建数据库
CREATE DATABASE walkbg;

# 创建用户
CREATE USER walkbg_user WITH PASSWORD 'your_password';
GRANT ALL PRIVILEGES ON DATABASE walkbg TO walkbg_user;
```

### 4. 启动应用

```bash
mvn spring-boot:run -Dspring-boot.run.profiles=postgres
```

---

## 生产环境部署

### 1. 使用环境变量

推荐在生产环境使用环境变量配置数据库：

```bash
export DB_URL="jdbc:mysql://your-db-host:3306/walkbg"
export DB_USERNAME="your_username"
export DB_PASSWORD="your_password"
export SPRING_PROFILES_ACTIVE=prod

java -jar walkbg-1.0-SNAPSHOT.jar
```

### 2. 使用外部配置文件

创建 `application-prod.yml` 并放在 jar 包同级目录：
```bash
java -jar walkbg-1.0-SNAPSHOT.jar --spring.config.location=./application-prod.yml
```

### 3. Docker 部署

创建 `docker-compose.yml`：
```yaml
version: '3.8'

services:
  mysql:
    image: mysql:8.0
    environment:
      MYSQL_ROOT_PASSWORD: root_password
      MYSQL_DATABASE: walkbg
      MYSQL_USER: walkbg_user
      MYSQL_PASSWORD: walkbg_password
    ports:
      - "3306:3306"
    volumes:
      - mysql_data:/var/lib/mysql

  app:
    build: .
    ports:
      - "8080:8080"
    environment:
      SPRING_PROFILES_ACTIVE: prod
      DB_URL: jdbc:mysql://mysql:3306/walkbg
      DB_USERNAME: walkbg_user
      DB_PASSWORD: walkbg_password
    depends_on:
      - mysql

volumes:
  mysql_data:
```

---

## 数据迁移

### 从 H2 迁移到 MySQL

#### 1. 导出 H2 数据

```bash
# 启动应用（H2模式）
mvn spring-boot:run

# 访问 H2 控制台导出数据
# http://localhost:8080/walkbg/h2-console
# 执行: SCRIPT TO 'backup.sql'
```

#### 2. 转换 SQL 语法

H2 和 MySQL 的 SQL 语法有些差异，需要手动调整：
- 时间类型：`TIMESTAMP` → `DATETIME`
- 自增主键：`IDENTITY` → `AUTO_INCREMENT`
- 布尔类型：`BOOLEAN` → `TINYINT(1)`

#### 3. 导入到 MySQL

```bash
mysql -u root -p walkbg < backup.sql
```

### 使用 Flyway 进行版本管理（推荐）

添加依赖：
```xml
<dependency>
    <groupId>org.flywaydb</groupId>
    <artifactId>flyway-core</artifactId>
</dependency>
<dependency>
    <groupId>org.flywaydb</groupId>
    <artifactId>flyway-mysql</artifactId>
</dependency>
```

创建迁移脚本：
```
src/main/resources/db/migration/
├── V1__init_schema.sql
├── V2__add_user_table.sql
└── V3__add_route_indexes.sql
```

---

## 常见问题

### 1. 连接被拒绝

**问题：** `Connection refused`

**解决：**
```bash
# 检查数据库是否运行
# MySQL
brew services list | grep mysql
sudo systemctl status mysql

# PostgreSQL
brew services list | grep postgresql
sudo systemctl status postgresql
```

### 2. 认证失败

**问题：** `Access denied for user`

**解决：**
- 检查用户名和密码是否正确
- 检查用户是否有权限访问数据库
- MySQL 8.0+ 可能需要修改认证方式：
  ```sql
  ALTER USER 'root'@'localhost' IDENTIFIED WITH mysql_native_password BY 'password';
  ```

### 3. 时区问题

**问题：** `The server time zone value 'CST' is unrecognized`

**解决：**
在 JDBC URL 中添加时区参数：
```
jdbc:mysql://localhost:3306/walkbg?serverTimezone=Asia/Shanghai
```

### 4. 字符编码问题

**问题：** 中文乱码

**解决：**
```sql
-- 检查数据库字符集
SHOW VARIABLES LIKE 'character%';

-- 修改数据库字符集
ALTER DATABASE walkbg CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
```

### 5. 表已存在

**问题：** `Table 'xxx' already exists`

**解决：**
- 开发环境：修改 `ddl-auto: update` 为 `create-drop`（会删除所有数据）
- 生产环境：使用 `ddl-auto: validate` 并手动管理表结构

---

## 性能优化建议

### 1. 连接池配置

```yaml
spring:
  datasource:
    hikari:
      maximum-pool-size: 20      # 最大连接数
      minimum-idle: 5            # 最小空闲连接
      connection-timeout: 30000  # 连接超时（毫秒）
      idle-timeout: 600000       # 空闲超时（毫秒）
      max-lifetime: 1800000      # 最大生命周期（毫秒）
```

### 2. JPA 批处理

```yaml
spring:
  jpa:
    properties:
      hibernate:
        jdbc:
          batch_size: 50         # 批处理大小
        order_inserts: true      # 排序插入
        order_updates: true      # 排序更新
```

### 3. 查询优化

- 使用索引
- 避免 N+1 查询问题
- 使用 `@EntityGraph` 或 `JOIN FETCH`
- 合理使用缓存

---

## 监控和维护

### 1. 查看连接池状态

访问：`http://localhost:8080/walkbg/actuator/metrics/hikaricp.connections`

### 2. 查看 SQL 执行情况

```yaml
logging:
  level:
    org.hibernate.SQL: DEBUG
    org.hibernate.type.descriptor.sql.BasicBinder: TRACE
```

### 3. 数据库备份

**MySQL:**
```bash
# 备份
mysqldump -u root -p walkbg > walkbg_backup_$(date +%Y%m%d).sql

# 恢复
mysql -u root -p walkbg < walkbg_backup_20251025.sql
```

**PostgreSQL:**
```bash
# 备份
pg_dump -U postgres walkbg > walkbg_backup_$(date +%Y%m%d).sql

# 恢复
psql -U postgres walkbg < walkbg_backup_20251025.sql
```

---

## 总结

| 数据库 | 适用场景 | 优点 | 缺点 |
|--------|----------|------|------|
| **H2** | 开发/测试 | 无需安装、配置简单 | 性能较弱、功能有限 |
| **MySQL** | 生产环境 | 成熟稳定、生态丰富 | 需要独立部署 |
| **PostgreSQL** | 生产环境 | 功能强大、标准兼容 | 学习曲线较陡 |

**推荐配置：**
- 开发环境：H2（默认）
- 测试环境：MySQL/PostgreSQL
- 生产环境：MySQL/PostgreSQL + 主从复制 + 读写分离

---

*最后更新: 2025-10-25*
