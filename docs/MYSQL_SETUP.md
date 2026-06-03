# MySQL 数据库配置

## 前置要求

- MySQL 8.0+
- 已启动 MySQL 服务

## 快速开始

```sql
-- 创建数据库
CREATE DATABASE walkbg CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;

-- 创建应用用户（可选，也可用 root）
CREATE USER 'walkbg'@'localhost' IDENTIFIED BY 'walkbg_password';
GRANT ALL PRIVILEGES ON walkbg.* TO 'walkbg'@'localhost';
FLUSH PRIVILEGES;
```

## 连接配置

编辑 `src/main/resources/application.yml`：

```yaml
spring:
  datasource:
    url: jdbc:mysql://localhost:3306/walkbg?useSSL=false&serverTimezone=UTC&allowPublicKeyRetrieval=true&characterEncoding=utf8mb4
    username: root
    password: root
```

启动应用后会自动创建表结构（DDL 模式为 `update`）。

## 环境 Profile

| Profile | 数据库 | DDL 策略 | 用途 |
|---------|--------|----------|------|
| 默认 | MySQL localhost | update | 开发 |
| h2 | H2 内存 | create-drop | 快速测试 |
| prod | 环境变量注入 | validate | 生产 |

生产环境使用环境变量 `${DB_PASSWORD}` 注入密码，DDL 改为 `validate`。

## 常用脚本

```bash
# 一键初始化（创建库 + 用户 + 示例数据）
./scripts/setup-mysql.sh

# 带 MySQL 启动后端
./scripts/start-with-mysql.sh

# 查看数据
./scripts/view-mysql-data.sh
```

## 备份与恢复

```bash
mysqldump -u root -p walkbg > walkbg_backup.sql
mysql -u root -p walkbg < walkbg_backup.sql
```

## 常见问题

- **Access denied**: 检查用户名密码和 MySQL 服务状态。
- **Unknown database 'walkbg'**: 执行上面的 CREATE DATABASE 语句。
- **Incorrect string value**: 确保数据库使用 `utf8mb4` 编码。
- **Communications link failure**: 检查 MySQL 服务是否运行、网络是否通。
