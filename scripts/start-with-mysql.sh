#!/bin/bash

# WalkBG MySQL 启动脚本
# 用于快速启动使用 MySQL 数据库的应用

echo "==================================="
echo "WalkBG - 使用 MySQL 启动"
echo "==================================="
echo ""

# 检查 MySQL 是否运行
if ! pgrep -x "mysqld" > /dev/null; then
    echo "⚠️  MySQL 未运行"
    echo ""
    read -p "是否启动 MySQL？(y/n) " -n 1 -r
    echo ""
    if [[ $REPLY =~ ^[Yy]$ ]]; then
        if [[ "$OSTYPE" == "darwin"* ]]; then
            brew services start mysql
        else
            sudo systemctl start mysql
        fi
        echo "✅ MySQL 已启动"
        sleep 2
    else
        echo "❌ 请先启动 MySQL"
        exit 1
    fi
else
    echo "✅ MySQL 正在运行"
fi

echo ""

# 检查数据库是否存在
echo "检查数据库..."
DB_EXISTS=$(mysql -u root -e "SHOW DATABASES LIKE 'walkbg';" 2>/dev/null | grep walkbg)

if [ -z "$DB_EXISTS" ]; then
    echo "⚠️  数据库 'walkbg' 不存在"
    echo ""
    read -p "是否创建数据库？(y/n) " -n 1 -r
    echo ""
    if [[ $REPLY =~ ^[Yy]$ ]]; then
        mysql -u root -e "CREATE DATABASE walkbg CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;"
        if [ $? -eq 0 ]; then
            echo "✅ 数据库创建成功"
        else
            echo "❌ 数据库创建失败，请检查 MySQL 权限"
            exit 1
        fi
    else
        echo "❌ 请先创建数据库"
        exit 1
    fi
else
    echo "✅ 数据库 'walkbg' 已存在"
fi

echo ""
echo "==================================="
echo "启动应用..."
echo "==================================="
echo ""

# 设置环境变量（如果需要）
# export DB_USERNAME=root
# export DB_PASSWORD=your_password

# 启动应用
mvn spring-boot:run -Dspring-boot.run.profiles=mysql

echo ""
echo "==================================="
echo "应用已停止"
echo "==================================="
