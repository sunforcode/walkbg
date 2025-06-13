#!/bin/bash

# 简化版路线创建脚本
# 用于快速测试路线创建功能

BASE_URL="http://localhost:8080"
API_ENDPOINT="/api/routes"

echo "=== 创建简单路线测试 ==="

# 简单的路线数据
SIMPLE_ROUTE='{
  "name": "测试徒步路线",
  "description": "这是一条用于测试的简单徒步路线",
  "region": "北京",
  "distance": 5.0,
  "duration": 3,
  "latitude": 39.9042,
  "longitude": 116.4074,
  "altitude": 50.0,
  "elevation_gain": 10.0,
  "elevation_loss": 0.0,
  "difficulty": 1,
  "route_type": 0,
  "route_direction": 0,
  "status": 1,
  "created_by": "test_user_001",
  "tags": ["测试", "简单"],
  "seasons": ["春季", "夏季"],
  "waypoints": [
    {
      "name": "起点",
      "description": "路线起点",
      "latitude": 39.9042,
      "longitude": 116.4074,
      "elevation": 50.0,
      "type": "起点",
      "sequence_number": 1
    },
    {
      "name": "终点",
      "description": "路线终点",
      "latitude": 39.9142,
      "longitude": 116.4174,
      "elevation": 60.0,
      "type": "终点",
      "sequence_number": 2
    }
  ],
  "segments": [
    {
      "distance": 5.0,
      "elevation_gain": 10.0,
      "elevation_loss": 0.0,
      "estimated_time": 3.0,
      "difficulty": 1,
      "terrain": "平路",
      "surface_type": "土路"
    }
  ]
}'

echo "发送请求..."
curl -X POST \
  -H "Content-Type: application/json" \
  -H "Accept: application/json" \
  -d "$SIMPLE_ROUTE" \
  "$BASE_URL$API_ENDPOINT" \
  -w "\n状态码: %{http_code}\n" \
  -v

echo ""
echo "=== 测试完成 ==="