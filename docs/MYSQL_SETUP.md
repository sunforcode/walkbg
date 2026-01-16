# MySQL 数据库配置指南
## 📋 前置要求

- MySQL 8.0 或更高版本
- 已安装 MySQL 服务

---

## 🚀 快速开始

### 1. 创建数据库

在 MySQL 中执行以下命令：

```sql
-- 创建数据库
CREATE DATABASE walkbg CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;

-- 创建用户（可选，如果使用 root 用户则跳过）
CREATE USER 'walkbg'@'localhost' IDENTIFIED BY 'walkbg_password';
GRANT ALL PRIVILEGES ON walkbg.* TO 'walkbg'@'localhost';
FLUSH PRIVILEGES;
```

### 2. 修改数据库连接配置

编辑 `src/main/resources/application.yml`：

```yaml
spring:
  datasource:
    url: jdbc:mysql://localhost:3306/walkbg?useSSL=false&serverTimezone=UTC&allowPublicKeyRetrieval=true&characterEncoding=utf8mb4
    driver-class-name: com.mysql.cj.jdbc.Driver
    username: root          # 修改为你的 MySQL 用户名
    password: root          # 修改为你的 MySQL 密码
```

### 3. 编译和运行

```bash
# 清理并编译
./gradlew clean build

# 运行应用
./gradlew bootRun
```

应用启动时会自动创建所有表结构。

---

## 🔧 配置说明

### 数据源配置

| 配置项 | 值 | 说明 |
|--------|-----|------|
| **URL** | `jdbc:mysql://localhost:3306/walkbg` | MySQL 连接地址 |
| **useSSL** | `false` | 不使用 SSL 连接 |
| **serverTimezone** | `UTC` | 服务器时区 |
| **allowPublicKeyRetrieval** | `true` | 允许公钥检索 |
| **characterEncoding** | `utf8mb4` | 字符编码 |

### 连接池配置

| 配置项 | 值 | 说明 |
|--------|-----|------|
| **最大连接数** | 20 | 最多同时连接数 |
| **最小空闲连接** | 5 | 最少保持的空闲连接 |
| **连接超时** | 30秒 | 获取连接的超时时间 |
| **空闲超时** | 10分钟 | 连接空闲多久后关闭 |
| **最大生命周期** | 30分钟 | 连接最长保持时间 |

### Hibernate 配置

| 配置项 | 值 | 说明 |
|--------|-----|------|
| **DDL 模式** | `update` | 自动更新表结构 |
| **方言** | `MySQL8Dialect` | MySQL 8.0 方言 |
| **批处理大小** | 20 | 批量插入/更新的大小 |
| **获取大小** | 50 | 一次获取的行数 |

---

## 📊 常用 MySQL 命令

### 查看数据库

```sql
-- 显示所有数据库
SHOW DATABASES;

-- 选择数据库
USE walkbg;

-- 显示所有表
SHOW TABLES;

-- 查看表结构
DESC users;
DESCRIBE routes;

-- 查看表的创建语句
SHOW CREATE TABLE users;
```

### 数据操作

```sql
-- 查看数据库大小
SELECT table_schema, 
       ROUND(SUM(data_length + index_length) / 1024 / 1024, 2) AS size_mb
FROM information_schema.tables
WHERE table_schema = 'walkbg'
GROUP BY table_schema;

-- 查看表的行数
SELECT table_name, table_rows
FROM information_schema.tables
WHERE table_schema = 'walkbg';

-- 清空表数据（谨慎使用）
TRUNCATE TABLE users;

-- 删除表
DROP TABLE users;
```

---

## 🔐 安全建议

### 生产环境配置

```yaml
spring:
  datasource:
    url: jdbc:mysql://db-server:3306/walkbg?useSSL=true&serverTimezone=UTC&characterEncoding=utf8mb4
    username: walkbg_user
    password: ${DB_PASSWORD}  # 使用环境变量
  jpa:
    hibernate:
      ddl-auto: validate  # 生产环境改为 validate
```

### 用户权限

```sql
-- 创建只读用户
CREATE USER 'walkbg_readonly'@'localhost' IDENTIFIED BY 'readonly_password';
GRANT SELECT ON walkbg.* TO 'walkbg_readonly'@'localhost';

-- 创建应用用户（读写权限）
CREATE USER 'walkbg_app'@'localhost' IDENTIFIED BY 'app_password';
GRANT SELECT, INSERT, UPDATE, DELETE ON walkbg.* TO 'walkbg_app'@'localhost';

-- 刷新权限
FLUSH PRIVILEGES;
```

---

## 🐛 常见问题

### 1. 连接被拒绝

**错误信息**：`Access denied for user 'root'@'localhost'`

**解决方案**：
- 检查用户名和密码是否正确
- 确保 MySQL 服务已启动
- 检查防火墙设置

### 2. 数据库不存在

**错误信息**：`Unknown database 'walkbg'`

**解决方案**：
```sql
CREATE DATABASE walkbg CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
```

### 3. 字符编码问题

**错误信息**：`Incorrect string value for column`

**解决方案**：
- 确保数据库使用 `utf8mb4` 编码
- 修改现有数据库：
```sql
ALTER DATABASE walkbg CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
ALTER TABLE users CONVERT TO CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
```

### 4. 连接超时

**错误信息**：`Communications link failure`

**解决方案**：
- 增加连接超时时间
- 检查网络连接
- 检查 MySQL 服务状态

---

## 📈 性能优化

### 索引优化

```sql
-- 查看表的索引
SHOW INDEX FROM users;

-- 创建索引
CREATE INDEX idx_username ON users(username);

-- 删除索引
DROP INDEX idx_username ON users;
```

### 查询优化

```sql
-- 分析查询性能
EXPLAIN SELECT * FROM users WHERE username = 'john';

-- 查看慢查询日志
SET GLOBAL slow_query_log = 'ON';
SET GLOBAL long_query_time = 2;
```

---

## 🔄 数据备份和恢复

### 备份数据库

```bash
# 备份整个数据库
mysqldump -u root -p walkbg > walkbg_backup.sql

# 备份特定表
mysqldump -u root -p walkbg users routes > walkbg_tables_backup.sql

# 压缩备份
mysqldump -u root -p walkbg | gzip > walkbg_backup.sql.gz
```

### 恢复数据库

```bash
# 恢复数据库
mysql -u root -p walkbg < walkbg_backup.sql

# 恢复压缩的备份
gunzip < walkbg_backup.sql.gz | mysql -u root -p walkbg
```

---

## 📝 配置文件位置

- **主配置文件**：`src/main/resources/application.yml`
- **日志配置**：在 `application.yml` 中的 `logging` 部分

---

## ✅ 验证配置

启动应用后，检查日志中是否有以下信息：

```
HikariPool-1 - Starting...
HikariPool-1 - Start completed.
Hibernate: create table users ...
```

如果看到这些信息，说明 MySQL 配置成功！

---

## 🚀 下一步

1. ✅ 配置 MySQL 数据库
2. ✅ 修改连接配置
3. ✅ 编译和运行应用
4. ✅ 访问 Swagger UI：`http://localhost:8080/walkbg/swagger-ui.html`
5. ✅ 开始开发！

---

## 📞 支持

如有问题，请检查：
1. MySQL 服务是否运行
2. 数据库是否创建
3. 用户权限是否正确
4. 连接字符串是否正确
