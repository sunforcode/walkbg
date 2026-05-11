-- MySQL 数据库初始化脚本

-- 创建数据库
CREATE DATABASE IF NOT EXISTS walkbg
    DEFAULT CHARACTER SET utf8mb4
    DEFAULT COLLATE utf8mb4_unicode_ci;

-- 使用数据库
USE walkbg;

-- 创建用户（可选）
-- CREATE USER IF NOT EXISTS 'walkbg_user'@'localhost' IDENTIFIED BY 'your_password';
-- GRANT ALL PRIVILEGES ON walkbg.* TO 'walkbg_user'@'localhost';
-- FLUSH PRIVILEGES;

-- 注意：表结构会由 Hibernate 自动创建（ddl-auto: update）
-- 如果需要手动创建表，可以先运行应用生成表结构，然后导出 SQL

-- 创建索引（如果 Hibernate 没有自动创建）
-- ALTER TABLE routes ADD INDEX idx_routes_region_id (region_id);
-- ALTER TABLE routes ADD INDEX idx_routes_difficulty (difficulty);
-- ALTER TABLE routes ADD INDEX idx_routes_status (status);
-- ALTER TABLE routes ADD INDEX idx_routes_created_by (created_by);

-- 迁移：为 users 表新增 status 列（如果数据库已存在且未使用 ddl-auto=update）
-- 0=正常(ACTIVE), 1=禁用(DISABLED)
ALTER TABLE users ADD COLUMN IF NOT EXISTS status INT NOT NULL DEFAULT 0;

-- 插入初始数据（可选）
-- INSERT INTO users (id, username, nickname, email, created_at, updated_at)
-- VALUES ('user-001', 'admin', '管理员', 'admin@walkbg.com', NOW(), NOW());
