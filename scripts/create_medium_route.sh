#!/bin/bash

# 创建中等复杂度路线的Shell脚本
# 使用方法: ./create_medium_route.sh [route_name] [creator_id]

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
ROUTE_NAME=${1:-"华山险峰路线"}
CREATOR_ID=${2:-"3428ab9d-e840-4d1c-9589-da70e863497b"}

# 生成路线ID
ROUTE_ID=$(generate_uuid)

print_info "准备创建中等复杂度路线..."
print_info "路线ID: $ROUTE_ID"
print_info "路线名称: $ROUTE_NAME"
print_info "创建者ID: $CREATOR_ID"

# 构建中等复杂度的路线JSON数据
JSON_DATA=$(cat <<EOF
{
  "id": "$ROUTE_ID",
  "name": "$ROUTE_NAME",
  "description": "华山西峰登顶路线，以险峻著称，适合有经验的徒步者挑战。",
  "region": "陕西华山",
  "regionId": "region_huashan_001",
  "difficulty": 3,
  "routeType": 0,
  "status": 1,
  "coverUrl": "https://example.com/images/huashan_cover.jpg",
  "created_by": "$CREATOR_ID",

  "tags": ["山峰", "险峻", "挑战", "一日游"],

  "waypoints": [
    {
      "name": "华山北峰",
      "description": "华山北峰，海拔1614米",
      "latitude": 34.4889,
      "longitude": 110.0892,
      "elevation": 1614.0,
      "type": "start",
      "sequenceNumber": 1
    },
    {
      "name": "华山西峰",
      "description": "华山西峰，海拔2082米，华山最高峰",
      "latitude": 34.4856,
      "longitude": 110.0845,
      "elevation": 2082.0,
      "type": "end",
      "sequenceNumber": 2
    }
  ],

  "segments": [
    {
      "distance": 5.2,
      "elevation_gain": 468.0,
      "elevation_loss": 0.0,
      "estimated_time": 4.0,
      "difficulty": 3,
      "terrain": "mountain"
    }
  ],

  "images": [
    {
      "imageUrl": "https://example.com/images/huashan_cover.jpg",
      "isCover": true,
      "sequenceNumber": 1
    }
  ],

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
