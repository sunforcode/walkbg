#!/bin/bash

# 创建完整路线的Shell脚本
# 使用方法: ./create_complete_route.sh [route_name] [creator_id]

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
ROUTE_NAME=${1:-"华山西峰完整路线"}
CREATOR_ID=${2:-"3428ab9d-e840-4d1c-9589-da70e863497b"}

# 生成路线ID
ROUTE_ID=$(generate_uuid)

print_info "准备创建完整路线..."
print_info "路线ID: $ROUTE_ID"
print_info "路线名称: $ROUTE_NAME"
print_info "创建者ID: $CREATOR_ID"

# 生成地图数据ID
MAP_DATA_ID=$(generate_uuid)

print_info "步骤1: 创建RouteMapData..."

# 构建RouteMapData JSON数据
MAP_DATA_JSON=$(cat <<EOF
{
  "id": "$MAP_DATA_ID",
  "distance": 8.5,
  "duration": 6,
  "latitude": 34.4889,
  "longitude": 110.0892,
  "altitude": 1614.0,
  "elevationGain": 468.0,
  "elevationLoss": 50.0,
  "favoriteCount": 0,
  "completionCount": 0,
  "tripCount": 0
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

print_info "步骤2: 创建完整路线..."

# 构建完整的路线JSON数据
JSON_DATA=$(cat <<EOF
{
  "id": "$ROUTE_ID",
  "name": "$ROUTE_NAME",
  "description": "华山西峰登顶路线，以险峻著称，适合有经验的徒步者挑战。从北峰出发，经过苍龙岭，最终登顶西峰。",
  "region": "陕西华山",
  "region_id": "region_huashan_001",
  "difficulty": 4,
  "route_type": 0,
  "status": 1,
  "cover_url": "https://example.com/images/huashan_cover.jpg",
  "default_map_id": "$MAP_DATA_ID",
  "created_by": "$CREATOR_ID",

  "tags": ["山峰", "险峻", "挑战", "一日游", "华山"],

  "waypoints": [
    {
      "name": "华山北峰",
      "description": "华山北峰，海拔1614米，华山索道上站",
      "latitude": 34.4889,
      "longitude": 110.0892,
      "elevation": 1614.0,
      "type": "start",
      "sequence_number": 1
    },
    {
      "name": "苍龙岭",
      "description": "华山著名险道，两侧万丈深渊",
      "latitude": 34.4856,
      "longitude": 110.0845,
      "elevation": 1800.0,
      "type": "waypoint",
      "sequence_number": 2
    },
    {
      "name": "华山西峰",
      "description": "华山西峰，海拔2082米，华山最高峰",
      "latitude": 34.4823,
      "longitude": 110.0812,
      "elevation": 2082.0,
      "type": "end",
      "sequence_number": 3
    }
  ],

  "segments": [
    {
      "distance": 4.2,
      "elevation_gain": 186.0,
      "elevation_loss": 0.0,
      "estimated_time": 2.5,
      "difficulty": 3,
      "terrain": "mountain"
    },
    {
      "distance": 4.3,
      "elevation_gain": 282.0,
      "elevation_loss": 50.0,
      "estimated_time": 3.5,
      "difficulty": 4,
      "terrain": "mountain"
    }
  ],

  "images": [
    {
      "imageUrl": "https://example.com/images/huashan_cover.jpg",
      "isCover": true,
      "sequenceNumber": 1
    },
    {
      "imageUrl": "https://example.com/images/huashan_xifeng.jpg",
      "isCover": false,
      "sequenceNumber": 2
    }
  ],

  "markerPoints": [
    {
      "name": "苍龙岭险道",
      "description": "华山最险要路段，注意安全",
      "latitude": 34.4856,
      "longitude": 110.0845,
      "elevation": 1800.0,
      "marker_type": 1,
      "sequenceNumber": 1
    }
  ],

  "supplies": [
    {
      "name": "华山北峰补给站",
      "description": "提供水和简单食物",
      "latitude": 34.4889,
      "longitude": 110.0892,
      "elevation": 1614.0,
      "supply_type": 0,
      "last_verified": "3428ab9d-e840-4d1c-9589-da70e863497b",
      "updated_by": "3428ab9d-e840-4d1c-9589-da70e863497b"
    }
  ],
  "campsites": [
    {
      "name": "华山西峰营地",
      "description": "可搭帐篷的平台区域",
      "latitude": 34.4823,
      "longitude": 110.0812,
      "elevation": 2082.0,
      "campsite_type": 1,
      "notes": "风大，注意保暖"
    }
  ],
  "dailyPlans": [
    {
      "title": "华山一日登顶",
      "description": "从北峰到西峰的完整登山计划",
      "dayNumber": 1,
      "distance": 8.5,
      "elevation_gain": 468,
      "elevation_loss": 50.0,
      "estimated_time": 6.0,
      "notes": "早上6点出发，下午4点返回"
    }
  ],
  "waterSources": [
    {
      "name": "华山北峰水源",
      "description": "北峰附近的天然水源",
      "latitude": 34.4889,
      "longitude": 110.0892,
      "elevation": 1614.0,
      "water_type": 0,
      "water_quality": 1,
      "reliability": 0.8,
      "requires_treatment": false
    }
  ],
  "hitchhikeContacts": [
    {
      "name": "华山接送服务",
      "description": "提供华山景区接送服务",
      "phone": "13800138000",
      "price": 50.0,
      "verified": true
    }
  ]
}
EOF
)

print_info "发送请求到: $BASE_URL$API_ENDPOINT"
print_info "请求数据预览:"
echo "$JSON_DATA" | jq . 2>/dev/null | head -30 || echo "$JSON_DATA" | head -30
echo "..."

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
        print_success "完整路线创建成功!"
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
                echo "curl -X GET \"$BASE_URL$API_ENDPOINT/$ROUTE_ID_CREATED/details\" | jq ."
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
