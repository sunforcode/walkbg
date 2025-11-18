#!/bin/bash

# 快速查询脚本 - 一行命令查看数据

DB_NAME="walkbg"
DB_USER="${DB_USERNAME:-root}"
DB_PASS="${DB_PASSWORD:-}"

# 执行查询
query() {
    if [ -z "$DB_PASS" ]; then
        mysql -u"$DB_USER" "$DB_NAME" -e "$1"
    else
        mysql -u"$DB_USER" -p"$DB_PASS" "$DB_NAME" -e "$1"
    fi
}

# 根据参数执行不同的查询
case "$1" in
    routes|r)
        echo "📍 路线列表："
        query "SELECT id, name, region, difficulty, status FROM routes;"
        ;;
    users|u)
        echo "👤 用户列表："
        query "SELECT id, username, email, nickname FROM users;"
        ;;
    waypoints|w)
        echo "🗺️  路径点列表："
        query "SELECT id, name, latitude, longitude, route_id FROM waypoints;"
        ;;
    stats|s)
        echo "📊 统计信息："
        query "SELECT
            (SELECT COUNT(*) FROM routes) as routes,
            (SELECT COUNT(*) FROM users) as users,
            (SELECT COUNT(*) FROM waypoints) as waypoints,
            (SELECT COUNT(*) FROM trips) as trips;"
        ;;
    tables|t)
        echo "📋 所有表："
        query "SHOW TABLES;"
        ;;
    *)
        echo "用法: $0 [选项]"
        echo ""
        echo "选项："
        echo "  routes, r      - 查看路线"
        echo "  users, u       - 查看用户"
        echo "  waypoints, w   - 查看路径点"
        echo "  stats, s       - 查看统计"
        echo "  tables, t      - 查看所有表"
        echo ""
        echo "示例："
        echo "  $0 routes      # 查看所有路线"
        echo "  $0 stats       # 查看统计信息"
        ;;
esac
