# MySQL 迁移检查清单

## ✅ 好消息：大部分代码无需修改！

你的项目使用了 **JPA/Hibernate**，这是一个数据库抽象层，大部分代码已经是数据库无关的。切换到 MySQL 只需要很少的改动。

---

## 📋 需要修改的地方

### 1. ✅ 已完成：添加 MySQL 驱动

```xml
<!-- pom.xml 中已添加 -->
<dependency>
    <groupId>com.mysql</groupId>
    <artifactId>mysql-connector-j</artifactId>
    <scope>runtime</scope>
</dependency>
```

### 2. ✅ 已完成：创建 MySQL 配置文件

已创建 `application-mysql.yml`，只需修改密码即可。

### 3. ⚠️ 需要注意：`data.sql` 初始化脚本

**问题：** `src/main/resources/data.sql` 使用了一些 H2 特定的语法。

**解决方案：** 创建 MySQL 版本的初始化脚本。

---

## 🔧 需要修改的文件

### 文件 1: `data.sql` 的 MySQL 兼容性

#### 问题点：

1. **CURRENT_TIMESTAMP vs NOW()**
   - H2: `CURRENT_TIMESTAMP`
   - MySQL: 两者都支持，但推荐 `NOW()`

2. **布尔值**
   - H2: `true/false`
   - MySQL: `1/0` 或 `true/false`（MySQL 5.7+）

3. **TEXT 类型**
   - 两者都支持，无需修改

4. **时间戳格式**
   - H2: `'2023-05-20T10:30:00Z'`
   - MySQL: `'2023-05-20 10:30:00'`

#### 解决方案：

我会为你创建一个 MySQL 兼容的初始化脚本。

---

## 📝 具体修改步骤

### 步骤 1: 禁用 H2 的 data.sql（使用 MySQL 时）

在 `application-mysql.yml` 中已经设置：
```yaml
spring:
  jpa:
    defer-datasource-initialization: false  # 禁用 data.sql
```

### 步骤 2: 创建 MySQL 专用的初始化脚本

创建 `src/main/resources/data-mysql.sql`（我会帮你创建）

### 步骤 3: 或者使用 Flyway/Liquibase（推荐生产环境）

更专业的数据库版本管理工具。

---

## 🎯 实体类检查结果

### ✅ 无需修改的地方

1. **主键策略** - 使用字符串 ID，兼容所有数据库
2. **字段类型** - 使用 JPA 标准注解，自动映射
3. **关系映射** - 标准 JPA 注解，无数据库依赖
4. **索引定义** - 使用 JPA `@Index`，自动生成
5. **约束定义** - 使用 JPA `@UniqueConstraint`，自动生成

### ⚠️ 需要注意的地方

#### 1. TEXT 字段长度

**当前代码：**
```kotlin
@Column(columnDefinition = "TEXT")
var description: String? = null
```

**说明：**
- H2: TEXT 类型无长度限制
- MySQL: TEXT 最大 65,535 字节（约 64KB）
- 如果需要更大：使用 `MEDIUMTEXT`（16MB）或 `LONGTEXT`（4GB）

**是否需要修改：** ❌ 不需要，TEXT 对大多数场景足够

#### 2. 时间戳字段

**当前代码：**
```kotlin
@Column(name = "created_at", nullable = false, updatable = false)
val createdAt: Instant = Instant.now()
```

**说明：**
- Hibernate 会自动将 `Instant` 映射为数据库的时间戳类型
- H2: TIMESTAMP
- MySQL: DATETIME 或 TIMESTAMP

**是否需要修改：** ❌ 不需要，Hibernate 自动处理

#### 3. 布尔字段

**当前代码：**
```kotlin
@Column(name = "is_loop", nullable = false)
var isLoop: Boolean = false
```

**说明：**
- Hibernate 会自动映射
- H2: BOOLEAN
- MySQL: TINYINT(1)

**是否需要修改：** ❌ 不需要，Hibernate 自动处理

---

## 🚀 迁移步骤总结

### 最简单的方式（推荐）

```bash
# 1. 安装 MySQL
brew install mysql
brew services start mysql

# 2. 创建数据库
./scripts/setup-mysql.sh

# 3. 修改密码
# 编辑 src/main/resources/application-mysql.yml
# 将 password 改为你的 MySQL 密码

# 4. 启动应用（让 Hibernate 自动创建表）
mvn spring-boot:run -Dspring-boot.run.profiles=mysql

# 5. 手动插入初始数据（可选）
# 使用 MySQL Workbench 或命令行执行 data-mysql.sql
```

### 完整步骤（包含数据迁移）

```bash
# 1. 导出 H2 数据
# 访问 http://localhost:8080/walkbg/h2-console
# 执行: SCRIPT TO 'h2_backup.sql'

# 2. 安装并配置 MySQL
./scripts/setup-mysql.sh

# 3. 启动应用（MySQL 模式）
mvn spring-boot:run -Dspring-boot.run.profiles=mysql

# 4. 导入数据（如果需要）
mysql -u root -p walkbg < data-mysql.sql
```

---

## 📊 配置对比

### H2 配置（当前）
```yaml
spring:
  datasource:
    url: jdbc:h2:file:./data/walkbgdb
    driver-class-name: org.h2.Driver
  jpa:
    properties:
      hibernate:
        dialect: org.hibernate.dialect.H2Dialect
```

### MySQL 配置（新）
```yaml
spring:
  datasource:
    url: jdbc:mysql://localhost:3306/walkbg
    driver-class-name: com.mysql.cj.jdbc.Driver
  jpa:
    properties:
      hibernate:
        dialect: org.hibernate.dialect.MySQLDialect
```

**差异：** 只有连接信息和方言不同，其他配置完全相同。

---

## ⚠️ 潜在问题和解决方案

### 问题 1: 字符集问题

**症状：** 中文显示为乱码

**解决：**
```sql
-- 创建数据库时指定字符集
CREATE DATABASE walkbg CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;

-- 或修改现有数据库
ALTER DATABASE walkbg CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
```

### 问题 2: 时区问题

**症状：** 时间相差 8 小时

**解决：**
在 JDBC URL 中添加时区参数（已在配置文件中添加）：
```yaml
url: jdbc:mysql://localhost:3306/walkbg?serverTimezone=Asia/Shanghai
```

### 问题 3: 表名大小写敏感

**症状：** Linux 上找不到表

**解决：**
MySQL 在 Linux 上默认区分大小写，在 Windows/Mac 上不区分。

```sql
-- 在 my.cnf 中设置（需要重启 MySQL）
[mysqld]
lower_case_table_names=1
```

或者确保代码中表名全部小写（当前代码已经是小写）。

### 问题 4: 连接数不足

**症状：** `Too many connections`

**解决：**
```sql
-- 查看当前最大连接数
SHOW VARIABLES LIKE 'max_connections';

-- 增加最大连接数
SET GLOBAL max_connections = 200;
```

或在 `my.cnf` 中设置：
```ini
[mysqld]
max_connections = 200
```

---

## 🔍 验证迁移是否成功

### 1. 检查表是否创建

```sql
USE walkbg;
SHOW TABLES;
```

应该看到所有表：
- routes
- users
- trips
- segments
- waypoints
- water_sources
- campsites
- supplies
- 等等...

### 2. 检查表结构

```sql
DESCRIBE routes;
```

### 3. 测试 API

```bash
# 查询路线列表
curl http://localhost:8080/walkbg/api/v1/routes

# 创建用户
curl -X POST http://localhost:8080/walkbg/api/v1/users \
  -H "Content-Type: application/json" \
  -d '{"username":"test","email":"test@example.com","nickname":"测试用户"}'
```

### 4. 检查日志

查看启动日志，确认使用的是 MySQL：
```
Hibernate:
    create table routes (...)
```

---

## 📈 性能优化建议

切换到 MySQL 后，可以进行以下优化：

### 1. 添加索引

```sql
-- 为常用查询字段添加索引
CREATE INDEX idx_routes_region_difficulty ON routes(region_id, difficulty);
CREATE INDEX idx_routes_created_at ON routes(created_at);
```

### 2. 优化连接池

```yaml
spring:
  datasource:
    hikari:
      maximum-pool-size: 20
      minimum-idle: 5
      connection-timeout: 30000
```

### 3. 启用查询缓存

```yaml
spring:
  jpa:
    properties:
      hibernate:
        cache:
          use_second_level_cache: true
          region:
            factory_class: org.hibernate.cache.jcache.JCacheRegionFactory
```

---

## 📚 总结

### ✅ 无需修改的代码（95%）

- ✅ 所有实体类（Entity）
- ✅ 所有 Repository
- ✅ 所有 Service
- ✅ 所有 Controller
- ✅ 所有 DTO
- ✅ 业务逻辑代码

### ⚠️ 需要修改的配置（5%）

- ⚠️ `application-mysql.yml` - 修改数据库密码
- ⚠️ `data.sql` - 使用 MySQL 版本（可选）

### 🎯 迁移难度评估

- **难度：** ⭐⭐☆☆☆（非常简单）
- **时间：** 15-30 分钟
- **风险：** 低（可以随时切回 H2）

---

## 🆘 需要帮助？

如果遇到问题：

1. 查看日志：`tail -f logs/walkbg.log`
2. 检查 MySQL 状态：`brew services list | grep mysql`
3. 测试连接：`mysql -u root -p`
4. 查看详细文档：`docs/DATABASE_GUIDE.md`

---

*最后更新: 2025-10-25*
