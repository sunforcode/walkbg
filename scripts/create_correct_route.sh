#!/bin/bash

# 创建正确格式路线的Shell脚本
# 使用方法: ./create_correct_route.sh [route_name] [creator_id]

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
ROUTE_NAME=${1:-"正确格式测试路线"}
CREATOR_ID=${2:-"3428ab9d-e840-4d1c-9589-da70e863497b"}

# 生成路线ID
ROUTE_ID=$(generate_uuid)

print_info "准备创建正确格式路线..."
print_info "路线ID: $ROUTE_ID"
print_info "路线名称: $ROUTE_NAME"
print_info "创建者ID: $CREATOR_ID"

# 构建正确格式的路线JSON数据
JSON_DATA=$(cat <<EOF
{
  "name": "$ROUTE_NAME",
  "description": "这是一个使用正确JSON格式的测试路线，包含基本的路点和路段信息。",
  "region": "测试区域",
  "difficulty": 2,
  "routeType": 0,
  "status": 1,
  "created_by": "$CREATOR_ID",

  "tags": ["测试", "正确格式"],

  "waypoints": [
    {
      "name": "起点",
      "description": "路线起点",
      "latitude": 39.9042,
      "longitude": 116.4074,
      "elevation": 100.0,
      "type": "start",
      "sequenceNumber": 1
    },
    {
      "name": "终点",
      "description": "路线终点",
      "latitude": 39.9142,
      "longitude": 116.4174,
      "elevation": 200.0,
      "type": "end",
      "sequenceNumber": 2
    }
  ],

  "segments": [
    {
      "distance": 2.5,
      "elevation_gain": 100.0,
      "elevation_loss": 0.0,
      "estimated_time": 1.5,
      "difficulty": 2,
      "terrain": "urban"
    }
  ],

  "images": [
    {
      "imageUrl": "https://example.com/test-image.jpg",
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

        # 提取路线ID并提供验证命令
        if command -v jq &> /dev/null; then
            ROUTE_ID_CREATED=$(echo "$HTTP_BODY" | jq -r '.data.id // empty')
            if [ -n "$ROUTE_ID_CREATED" ] && [ "$ROUTE_ID_CREATED" != "null" ]; then
                echo ""
                print_success "路线创建成功，ID: $ROUTE_ID_CREATED"
                print_info "验证路线详情:"
                echo "curl -X GET \"$BASE_URL$API_ENDPOINT/$ROUTE_ID_CREATED\" | jq ."
            fi
        fi
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
