#!/bin/bash

# Supply创建测试脚本
BASE_URL="http://localhost:8080"

echo "=== 测试Supply数据创建 ==="

# 根据提供的JSON格式创建Supply数据
SUPPLY_DATA='{
  "id": "sp_001",
  "name": "云谷寺商店",
  "description": "云谷寺入口处的综合商店，提供登山用品和食物",
  "route_id": "route_huangshan_001",
  "latitude": 30.1234,
  "longitude": 118.1567,
  "elevation": 630.0,
  "supply_type": 1,
  "last_verified": "2023-05-20T10:30:00Z",
  "updated_by": "user_001"
}'

echo "发送Supply创建请求..."
echo "数据: $SUPPLY_DATA"
echo ""

# 如果有Supply API端点，可以测试创建
# curl -X POST \
#   -H "Content-Type: application/json" \
#   -H "Accept: application/json" \
#   -d "$SUPPLY_DATA" \
#   "$BASE_URL/api/supplies" \
#   -w "\n状态码: %{http_code}\n" \
#   -v

# 测试获取路线详情（包含supplies）
echo "测试获取路线详情（包含supplies）..."
curl -X GET \
  -H "Accept: application/json" \
  "$BASE_URL/api/routes/route_huangshan_001/details" \
  -w "\n状态码: %{http_code}\n"

echo ""
echo "=== 测试完成 ==="