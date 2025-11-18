#!/bin/bash

# MySQL 数据库设置脚本

echo "==================================="
echo "WalkBG MySQL 数据库设置"
echo "==================================="

# 配置
DB_NAME="walkbg"
DB_USER="root"
DB_PASSWORD=""

# 检查 MySQL 是否安装
if ! command -v mysql &> /dev/null; then
    echo "❌ MySQL 未安装"
    echo ""
    echo "请先安装 MySQL："
    echo "  macOS: brew install mysql"
    echo "  Ubuntu: sudo apt-get install mysql-server"
    echo "  CentOS: sudo yum install mysql-server"
    exit 1
fi

echo "✅ MySQL 已安装"

# 检查 MySQL 是否运行
if ! pgrep -x "mysqld" > /dev/null; then
    echo "⚠️  MySQL 未运行，正在启动..."

    # macOS
    if [[ "$OSTYPE" == "darwin"* ]]; then
        brew services start mysql
    # Linux
    else
        sudo systemctl start mysql
    fi

    sleep 3
fi

echo "✅ MySQL 正在运行"

# 提示输入密码
echo ""
read -sp "请输入 MySQL root 密码（直接回车如果没有密码）: " DB_PASSWORD
echo ""

# 创建数据库
echo ""
echo "正在创建数据库..."

if [ -z "$DB_PASSWORD" ]; then
    mysql -u"$DB_USER" < scripts/init-mysql.sql
else
    mysql -u"$DB_USER" -p"$DB_PASSWORD" < scripts/init-mysql.sql
fi

if [ $? -eq 0 ]; then
    echo "✅ 数据库创建成功"
else
    echo "❌ 数据库创建失败"
    exit 1
fi

# 更新配置文件
echo ""
echo "请更新 application-mysql.yml 中的数据库密码"
echo ""
echo "然后使用以下命令启动应用："
echo "  mvn spring-boot:run -Dspring-boot.run.profiles=mysql"
echo ""
echo "或者设置环境变量："
echo "  export SPRING_PROFILES_ACTIVE=mysql"
echo "  mvn spring-boot:run"
echo ""
echo "==================================="
echo "✅ 设置完成"
echo "==================================="
