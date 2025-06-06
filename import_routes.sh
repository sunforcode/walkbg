#!/bin/bash

# WalkBG 路线数据导入脚本 (Bash版本)
# 使用curl命令调用API导入数据

BASE_URL="http://localhost:8080/walkbg/api"
JSON_FILE="routes.json"

echo "🌟 WalkBG 路线数据导入工具 (Bash版)"
echo "================================================"

# 检查服务是否运行
echo "🔍 检查服务连接..."
if ! curl -s -f "${BASE_URL}/users?page=0&size=1" > /dev/null; then
    echo "❌ 无法连接到服务，请确保服务正在运行"
    exit 1
fi
echo "✅ 服务连接正常"

# 检查JSON文件是否存在
if [ ! -f "$JSON_FILE" ]; then
    echo "❌ 找不到JSON文件: $JSON_FILE"
    exit 1
fi

echo "📁 找到JSON文件: $JSON_FILE"

# 创建用户
echo "👤 创建用户..."
user_response=$(curl -s -X POST "${BASE_URL}/users" \
  -H "Content-Type: application/json" \
  -d '{
    "id": "user_001",
    "username": "route_creator",
    "email": "creator@example.com",
    "nickname": "路线创建者",
    "isActive": true
  }')

if [ $? -eq 0 ]; then
    echo "✅ 用户创建完成"
else
    echo "⚠️ 用户创建可能失败，继续执行..."
fi

# 创建路线
echo "🗺️  创建路线..."
route_response=$(curl -s -X POST "${BASE_URL}/routes" \
  -H "Content-Type: application/json" \
  -d '{
    "id": "route_001",
    "name": "黄山经典徒步路线",
    "description": "这条路线带您游览黄山最著名的景点，包括迎客松、光明顶和西海大峡谷。沿途可欣赏壮丽的云海和奇特的松树。",
    "region": "黄山风景区",
    "distance": 15.5,
    "duration": 8,
    "latitude": 30.1234,
    "longitude": 118.1567,
    "altitude": 630.0,
    "elevationGain": 1360,
    "elevationLoss": 1170,
    "difficulty": 1,
    "routeType": 0,
    "status": 1,
    "popularity": 1250,
    "createdBy": "user_001",
    "coverUrl": "https://images.unsplash.com/photo-1551632811-561732d1e306?ixlib=rb-1.2.1&auto=format&fit=crop&w=1000&q=80",
    "mapDataId": "map_001",
    "defaultMapId": "map_001"
  }')

if [ $? -eq 0 ]; then
    echo "✅ 路线创建完成"
else
    echo "❌ 路线创建失败"
    echo "响应: $route_response"
fi

# 验证数据
echo "🔍 验证导入结果..."
echo "用户列表:"
if command -v python3 &> /dev/null; then
    curl -s "${BASE_URL}/users?page=0&size=5" | python3 -m json.tool
elif command -v jq &> /dev/null; then
    curl -s "${BASE_URL}/users?page=0&size=5" | jq .
else
    curl -s "${BASE_URL}/users?page=0&size=5"
fi

echo ""
echo "路线列表:"
if command -v python3 &> /dev/null; then
    curl -s "${BASE_URL}/routes?page=0&size=5" | python3 -m json.tool
elif command -v jq &> /dev/null; then
    curl -s "${BASE_URL}/routes?page=0&size=5" | jq .
else
    curl -s "${BASE_URL}/routes?page=0&size=5"
fi

echo ""
echo "🎉 导入完成！"