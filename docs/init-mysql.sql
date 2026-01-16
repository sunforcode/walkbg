-- WalkBG MySQL 初始化脚本
-- 执行方式: mysql -u root -p < init-mysql.sql
-- 创建数据库
CREATE DATABASE IF NOT EXISTS walkbg 
CHARACTER SET utf8mb4 
COLLATE utf8mb4_unicode_ci;

-- 使用数据库
USE walkbg;

-- 创建用户（可选）
-- CREATE USER IF NOT EXISTS 'walkbg'@'localhost' IDENTIFIED BY 'walkbg_password';
-- GRANT ALL PRIVILEGES ON walkbg.* TO 'walkbg'@'localhost';
-- FLUSH PRIVILEGES;

-- 显示创建结果
SELECT 'Database walkbg created successfully!' AS status;
SELECT DATABASE() AS current_database;
