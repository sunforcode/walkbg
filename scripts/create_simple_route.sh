#!/bin/bash

# 创建简单路线的Shell脚本
# 使用方法: ./create_simple_route.sh [route_name] [creator_id]

# 设置默认值
BASE_URL="http://localhost:8080/walkbg"
API_ENDPOINT="/api/routes"

# 颜色定义
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
BLUE='\033[0;34m'
NC='\033[0m' # No Color

# 打印带颜色的消息
print_info() {
    echo -e "${BLUE}[INFO]${NC} $1"
}

print_success() {
    echo -e "${GREEN}[SUCCESS]${NC} $1"
}

print_error() {
    echo -e "${RED}[ERROR]${NC} $1"
}

# 生成UUID
generate_uuid() {
    if command -v uuidgen &> /dev/null; then
        uuidgen | tr '[:upper:]' '[:lower:]'
    else
        echo "route-$(date +%s)-$(shuf -i 1000-9999 -n 1)"
    fi
}

# 获取参数
ROUTE_NAME=${1:-"黄山天都峰徒步路线"}
CREATOR_ID=${2:-"3428ab9d-e840-4d1c-9589-da70e863497b"}

# 生成路线ID
ROUTE_ID=$(generate_uuid)

print_info "准备创建简单路线..."
print_info "路线ID: $ROUTE_ID"
print_info "路线名称: $ROUTE_NAME"
print_info "创建者ID: $CREATOR_ID"

# 生成地图数据ID
MAP_DATA_ID=$(generate_uuid)

print_info "步骤1: 创建RouteMapData..."

# 构建RouteMapData JSON数据（最简版本）
MAP_DATA_JSON=$(cat <<EOF
{
  "id": "$MAP_DATA_ID"
}
EOF
)

# 先创建RouteMapData
print_info "创建地图数据ID: $MAP_DATA_ID"

# 发送创建RouteMapData的HTTP请求
MAP_DATA_RESPONSE=$(curl -s -w "\n%{http_code}" \
  -X POST \
  -H "Content-Type: application/json" \
  -H "Accept: application/json" \
  -d "$MAP_DATA_JSON" \
  "$BASE_URL/api/route-map-data")

# 分离响应体和状态码
MAP_DATA_HTTP_CODE=$(echo "$MAP_DATA_RESPONSE" | tail -n 1)
MAP_DATA_HTTP_BODY=$(echo "$MAP_DATA_RESPONSE" | sed '$d')

print_info "地图数据创建HTTP状态码: $MAP_DATA_HTTP_CODE"

if [ "$MAP_DATA_HTTP_CODE" != "200" ] && [ "$MAP_DATA_HTTP_CODE" != "201" ]; then
    print_error "创建地图数据失败，HTTP状态码: $MAP_DATA_HTTP_CODE"
    echo "$MAP_DATA_HTTP_BODY"
    exit 1
else
    print_success "地图数据创建成功!"
fi

print_info "步骤2: 创建路线..."

# 构建简单的路线JSON数据（不使用defaultMapId）
JSON_DATA=$(cat <<EOF
{
  "id": "$ROUTE_ID",
  "name": "$ROUTE_NAME",
  "description": "黄山三大主峰之一天都峰的经典徒步路线，风景秀丽，挑战性强。",
  "region": "安徽黄山",
  "regionId": "region_huangshan_001",
  "difficulty": 2,
  "routeType": 0,
  "status": 1,
  "coverUrl": "https://example.com/images/huangshan_cover.jpg",
  "default_map_id": "$MAP_DATA_ID",
  "created_by": "$CREATOR_ID",
  "tags": ["山峰", "风景名胜", "一日游"],
  "waypoints": [],
  "segments": [],
  "images": [],
  "supplies": [],
  "campsites": [],
  "markerPoints": [],
  "dailyPlans": []
}
EOF
)

print_info "发送请求到: $BASE_URL$API_ENDPOINT"
print_info "请求数据:"
echo "$JSON_DATA" | jq . 2>/dev/null || echo "$JSON_DATA"

# 发送HTTP请求
RESPONSE=$(curl -s -w "\n%{http_code}" \
  -X POST \
  -H "Content-Type: application/json" \
  -H "Accept: application/json" \
  -d "$JSON_DATA" \
  "$BASE_URL$API_ENDPOINT")

# 分离响应体和状态码
HTTP_CODE=$(echo "$RESPONSE" | tail -n 1)
HTTP_BODY=$(echo "$RESPONSE" | sed '$d')

print_info "HTTP状态码: $HTTP_CODE"

# 处理响应
case $HTTP_CODE in
    200|201)
        print_success "路线创建成功!"
        echo ""
        print_info "响应数据:"
        echo "$HTTP_BODY" | jq . 2>/dev/null || echo "$HTTP_BODY"
        ;;
    400)
        print_error "请求参数错误 (400)"
        echo "$HTTP_BODY" | jq -r '.message // .error // .' 2>/dev/null || echo "$HTTP_BODY"
        exit 1
        ;;
    404)
        print_error "创建者用户不存在 (404)"
        echo "$HTTP_BODY" | jq -r '.message // .error // .' 2>/dev/null || echo "$HTTP_BODY"
        exit 1
        ;;
    500)
        print_error "服务器内部错误 (500)"
        echo "$HTTP_BODY" | jq -r '.message // .error // .' 2>/dev/null || echo "$HTTP_BODY"
        exit 1
        ;;
    *)
        print_error "请求失败，HTTP状态码: $HTTP_CODE"
        echo "$HTTP_BODY"
        exit 1
        ;;
esac

print_success "脚本执行完成!"
