#!/bin/bash

# MySQL 数据查看脚本
# 用于快速查看 WalkBG 数据库中的数据

echo "==================================="
echo "WalkBG MySQL 数据查看"
echo "==================================="
echo ""

# 数据库配置
DB_NAME="walkbg"
DB_USER="${DB_USERNAME:-root}"
DB_PASS="${DB_PASSWORD:-}"

# 检查 MySQL 是否运行
if ! pgrep -x "mysqld" > /dev/null; then
    echo "❌ MySQL 未运行"
    echo "请先启动 MySQL: brew services start mysql"
    exit 1
fi

echo "✅ MySQL 正在运行"
echo ""

# 检查数据库是否存在
DB_EXISTS=$(mysql -u"$DB_USER" ${DB_PASS:+-p"$DB_PASS"} -e "SHOW DATABASES LIKE '$DB_NAME';" 2>/dev/null | grep "$DB_NAME")

if [ -z "$DB_EXISTS" ]; then
    echo "❌ 数据库 '$DB_NAME' 不存在"
    echo "请先创建数据库或启动应用"
    exit 1
fi

echo "✅ 数据库 '$DB_NAME' 已连接"
echo ""

# 显示菜单
show_menu() {
    echo "==================================="
    echo "请选择要查看的数据："
    echo "==================================="
    echo "1. 查看所有表"
    echo "2. 查看路线数据 (routes)"
    echo "3. 查看用户数据 (users)"
    echo "4. 查看路径点数据 (waypoints)"
    echo "5. 查看路段数据 (segments)"
    echo "6. 查看水源数据 (water_sources)"
    echo "7. 查看营地数据 (campsites)"
    echo "8. 查看补给点数据 (supplies)"
    echo "9. 查看行程数据 (trips)"
    echo "10. 查看标签数据 (route_tags)"
    echo "11. 统计信息"
    echo "12. 自定义 SQL 查询"
    echo "0. 退出"
    echo "==================================="
    echo -n "请输入选项 (0-12): "
}

# 执行 SQL 查询
execute_query() {
    local query="$1"
    if [ -z "$DB_PASS" ]; then
        mysql -u"$DB_USER" "$DB_NAME" -e "$query"
    else
        mysql -u"$DB_USER" -p"$DB_PASS" "$DB_NAME" -e "$query"
    fi
}

# 查看所有表
view_tables() {
    echo ""
    echo "📋 数据库中的所有表："
    echo "-----------------------------------"
    execute_query "SHOW TABLES;"
    echo ""
}

# 查看路线数据
view_routes() {
    echo ""
    echo "🗺️  路线数据："
    echo "-----------------------------------"
    execute_query "SELECT id, name, region, difficulty, route_type, status, popularity, created_at FROM routes LIMIT 10;"
    echo ""
    execute_query "SELECT COUNT(*) as total_routes FROM routes;"
    echo ""
}

# 查看用户数据
view_users() {
    echo ""
    echo "👤 用户数据："
    echo "-----------------------------------"
    execute_query "SELECT id, username, email, nickname, phone, created_at FROM users LIMIT 10;"
    echo ""
    execute_query "SELECT COUNT(*) as total_users FROM users;"
    echo ""
}

# 查看路径点数据
view_waypoints() {
    echo ""
    echo "📍 路径点数据："
    echo "-----------------------------------"
    execute_query "SELECT id, name, latitude, longitude, elevation, waypoint_type, route_id FROM waypoints LIMIT 10;"
    echo ""
    execute_query "SELECT COUNT(*) as total_waypoints FROM waypoints;"
    echo ""
}

# 查看路段数据
view_segments() {
    echo ""
    echo "🛤️  路段数据："
    echo "-----------------------------------"
    execute_query "SELECT id, name, distance, elevation_gain, difficulty, route_id FROM segments LIMIT 10;"
    echo ""
    execute_query "SELECT COUNT(*) as total_segments FROM segments;"
    echo ""
}

# 查看水源数据
view_water_sources() {
    echo ""
    echo "💧 水源数据："
    echo "-----------------------------------"
    execute_query "SELECT id, name, water_type, water_quality, reliability, route_id FROM water_sources LIMIT 10;"
    echo ""
    execute_query "SELECT COUNT(*) as total_water_sources FROM water_sources;"
    echo ""
}

# 查看营地数据
view_campsites() {
    echo ""
    echo "⛺ 营地数据："
    echo "-----------------------------------"
    execute_query "SELECT id, name, campsite_type, capacity, facilities, route_id FROM campsites LIMIT 10;"
    echo ""
    execute_query "SELECT COUNT(*) as total_campsites FROM campsites;"
    echo ""
}

# 查看补给点数据
view_supplies() {
    echo ""
    echo "🏪 补给点数据："
    echo "-----------------------------------"
    execute_query "SELECT id, name, supply_type, latitude, longitude, route_id FROM supplies LIMIT 10;"
    echo ""
    execute_query "SELECT COUNT(*) as total_supplies FROM supplies;"
    echo ""
}

# 查看行程数据
view_trips() {
    echo ""
    echo "🎒 行程数据："
    echo "-----------------------------------"
    execute_query "SELECT id, name, status, start_date, end_date, organizer_id FROM trips LIMIT 10;"
    echo ""
    execute_query "SELECT COUNT(*) as total_trips FROM trips;"
    echo ""
}

# 查看标签数据
view_tags() {
    echo ""
    echo "🏷️  标签数据："
    echo "-----------------------------------"
    execute_query "SELECT id, tag, route_id FROM route_tags LIMIT 20;"
    echo ""
    execute_query "SELECT COUNT(*) as total_tags FROM route_tags;"
    echo ""
}

# 统计信息
view_statistics() {
    echo ""
    echo "📊 数据库统计信息："
    echo "-----------------------------------"
    execute_query "
        SELECT
            (SELECT COUNT(*) FROM routes) as routes_count,
            (SELECT COUNT(*) FROM users) as users_count,
            (SELECT COUNT(*) FROM waypoints) as waypoints_count,
            (SELECT COUNT(*) FROM segments) as segments_count,
            (SELECT COUNT(*) FROM water_sources) as water_sources_count,
            (SELECT COUNT(*) FROM campsites) as campsites_count,
            (SELECT COUNT(*) FROM supplies) as supplies_count,
            (SELECT COUNT(*) FROM trips) as trips_count;
    "
    echo ""

    echo "📈 路线难度分布："
    execute_query "SELECT difficulty, COUNT(*) as count FROM routes GROUP BY difficulty;"
    echo ""

    echo "🌍 路线区域分布："
    execute_query "SELECT region, COUNT(*) as count FROM routes GROUP BY region LIMIT 10;"
    echo ""
}

# 自定义查询
custom_query() {
    echo ""
    echo "💻 自定义 SQL 查询"
    echo "-----------------------------------"
    echo "请输入 SQL 查询语句（输入 'exit' 返回）："
    read -r query

    if [ "$query" != "exit" ] && [ ! -z "$query" ]; then
        echo ""
        execute_query "$query"
        echo ""
    fi
}

# 主循环
while true; do
    show_menu
    read -r choice

    case $choice in
        1) view_tables ;;
        2) view_routes ;;
        3) view_users ;;
        4) view_waypoints ;;
        5) view_segments ;;
        6) view_water_sources ;;
        7) view_campsites ;;
        8) view_supplies ;;
        9) view_trips ;;
        10) view_tags ;;
        11) view_statistics ;;
        12) custom_query ;;
        0)
            echo ""
            echo "👋 再见！"
            exit 0
            ;;
        *)
            echo ""
            echo "❌ 无效选项，请重新选择"
            echo ""
            ;;
    esac

    echo ""
    read -p "按 Enter 继续..."
    clear
done
