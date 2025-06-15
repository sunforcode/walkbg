#!/bin/bash

# 创建路线的Shell脚本
# 使用方法: ./create_route.sh [route_name] [creator_id]

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

print_warning() {
    echo -e "${YELLOW}[WARNING]${NC} $1"
}

print_error() {
    echo -e "${RED}[ERROR]${NC} $1"
}

# 显示使用说明
show_usage() {
    echo "使用方法:"
    echo "  $0 [route_name] [creator_id]"
    echo ""
    echo "参数说明:"
    echo "  route_name - 路线名称 (可选，默认使用预设路线)"
    echo "  creator_id - 创建者用户ID (可选，默认使用admin用户)"
    echo ""
    echo "示例:"
    echo "  $0                                    # 使用默认参数创建黄山路线"
    echo "  $0 \"泰山登顶路线\" \"user_123\"         # 创建自定义路线"
    echo "  $0 \"华山险峰路线\"                    # 使用默认创建者"
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
CREATOR_ID=${2:-"admin"}

# 生成路线ID
ROUTE_ID=$(generate_uuid)

print_info "准备创建路线..."
print_info "路线ID: $ROUTE_ID"
print_info "路线名称: $ROUTE_NAME"
print_info "创建者ID: $CREATOR_ID"

# 构建完整的路线JSON数据
JSON_DATA=$(cat <<EOF
{
  "id": "$ROUTE_ID",
  "name": "$ROUTE_NAME",
  "description": "黄山三大主峰之一天都峰的经典徒步路线，风景秀丽，挑战性强。从慈光阁出发，经半山寺、玉屏楼，最终登顶天都峰，全程约7.5公里，海拔爬升1064米。路线沿途风景优美，包含古建筑、奇松怪石等黄山经典景观，是体验黄山精华的绝佳选择。",
  "region": "安徽黄山",
  "regionId": "region_huangshan_001",
  "difficulty": 2,
  "routeType": 0,
  "status": 1,
  "coverUrl": "https://example.com/images/huangshan_cover.jpg",
  "created_by": "$CREATOR_ID",

  "tags": ["山峰", "风景名胜", "高难度", "一日游", "摄影"],

  "waypoints": [
    {
      "id": "waypoint_001",
      "name": "慈光阁",
      "description": "黄山南大门，徒步起点，海拔800米",
      "latitude": 30.1394,
      "longitude": 118.1558,
      "elevation": 800.0,
      "type": "start",
      "iconUrl": "https://example.com/icons/start.png",
      "imageUrl": "https://example.com/images/ciguangge.jpg",
      "sequenceNumber": 1
    },
    {
      "id": "waypoint_002",
      "name": "半山寺",
      "description": "黄山著名古寺，徒步中转点，海拔1340米",
      "latitude": 30.1425,
      "longitude": 118.1612,
      "elevation": 1340.0,
      "type": "waypoint",
      "iconUrl": "https://example.com/icons/temple.png",
      "imageUrl": "https://example.com/images/banshan.jpg",
      "sequenceNumber": 2
    },
    {
      "id": "waypoint_003",
      "name": "玉屏楼",
      "description": "黄山著名景点，迎客松所在地，海拔1680米",
      "latitude": 30.1456,
      "longitude": 118.1634,
      "elevation": 1680.0,
      "type": "scenic",
      "iconUrl": "https://example.com/icons/scenic.png",
      "imageUrl": "https://example.com/images/yupinglou.jpg",
      "sequenceNumber": 3
    },
    {
      "id": "waypoint_004",
      "name": "天都峰顶",
      "description": "黄山三大主峰之一，海拔1864米，视野开阔",
      "latitude": 30.1478,
      "longitude": 118.1645,
      "elevation": 1864.0,
      "type": "end",
      "iconUrl": "https://example.com/icons/peak.png",
      "imageUrl": "https://example.com/images/tiandufeng.jpg",
      "sequenceNumber": 4
    }
  ],

  "segments": [
    {
      "id": "segment_001",
      "distance": 3.2,
      "elevation_gain": 540.0,
      "elevation_loss": 0.0,
      "estimated_time": 2.5,
      "difficulty": 1,
      "terrain": "mountain"
    },
    {
      "id": "segment_002",
      "distance": 2.8,
      "elevation_gain": 340.0,
      "elevation_loss": 0.0,
      "estimated_time": 2.0,
      "difficulty": 2,
      "terrain": "mountain"
    },
    {
      "id": "segment_003",
      "distance": 1.5,
      "elevation_gain": 184.0,
      "elevation_loss": 0.0,
      "estimated_time": 1.5,
      "difficulty": 3,
      "terrain": "mountain"
    }
  ],

  "images": [
    {
      "imageUrl": "https://example.com/images/huangshan_cover.jpg",
      "isCover": true,
      "sequenceNumber": 1
    },
    {
      "imageUrl": "https://example.com/images/huangshan_sunrise.jpg",
      "isCover": false,
      "sequenceNumber": 2
    },
    {
      "imageUrl": "https://example.com/images/yingkesong.jpg",
      "isCover": false,
      "sequenceNumber": 3
    }
  ],

  "supplies": [
    {
      "id": "supply_001",
      "name": "玉屏楼小卖部",
      "description": "提供基本食品和饮用水",
      "latitude": 30.1456,
      "longitude": 118.1634,
      "elevation": 1680.0,
      "supplyType": 0,
      "lastVerified": "2024-06-06",
      "updatedBy": "$CREATOR_ID"
    },
    {
      "id": "supply_002",
      "name": "半山寺茶水站",
      "description": "提供热水和简单茶点",
      "latitude": 30.1425,
      "longitude": 118.1612,
      "elevation": 1340.0,
      "supplyType": 1,
      "lastVerified": "2024-06-06",
      "updatedBy": "$CREATOR_ID"
    }
  ],

  "campsites": [
    {
      "id": "camp_001",
      "name": "玉屏楼露营区",
      "description": "官方指定露营区域，位于玉屏楼景区附近",
      "latitude": 30.1456,
      "longitude": 118.1634,
      "elevation": 1680.0,
      "campsiteType": 0,
      "notes": "位置开阔，视野良好，需要自备帐篷和装备。注意天气变化，做好防风措施。"
    }
  ],

  "markerPoints": [
    {
      "id": "marker_001",
      "name": "迎客松",
      "description": "黄山标志性景观，位于玉屏楼附近",
      "latitude": 30.1456,
      "longitude": 118.1634,
      "elevation": 1680.0,
      "marker_type": 0,
      "iconUrl": "https://example.com/icons/scenic_spot.png"
    },
    {
      "id": "marker_002",
      "name": "危险路段",
      "description": "天都峰攀登路段，雨天湿滑，注意安全",
      "latitude": 30.1470,
      "longitude": 118.1640,
      "elevation": 1750.0,
      "marker_type": 2,
      "iconUrl": "https://example.com/icons/danger.png"
    }
  ],

  "dailyPlans": [
    {
      "id": "daily_plan_001",
      "title": "黄山天都峰一日登顶",
      "description": "从慈光阁出发，经过半山寺、玉屏楼，最终登顶天都峰的一日徒步路线",
      "dayNumber": 1,
      "distance": 7.5,
      "elevation_gain": 1064,
      "elevation_loss": 200.0,
      "estimated_time": 6.0,
      "notes": "建议早上7点出发，携带足够的水和食物，注意天气变化"
    }
  ]
}
EOF
)

print_info "发送请求到: $BASE_URL$API_ENDPOINT"
print_info "请求数据预览:"
echo "$JSON_DATA" | jq . 2>/dev/null | head -20 || echo "$JSON_DATA" | head -20
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
        print_success "路线创建成功!"
        echo ""
        print_info "响应数据:"
        echo "$HTTP_BODY" | jq . 2>/dev/null || echo "$HTTP_BODY"

        # 尝试提取路线信息
        if command -v jq &> /dev/null; then
            CREATED_ROUTE=$(echo "$HTTP_BODY" | jq -r '.data // empty')
            if [ -n "$CREATED_ROUTE" ] && [ "$CREATED_ROUTE" != "null" ]; then
                echo ""
                print_success "创建的路线信息:"
                echo "$CREATED_ROUTE" | jq .

                # 提取路线ID用于后续验证
                ROUTE_ID_CREATED=$(echo "$CREATED_ROUTE" | jq -r '.id // empty')
                if [ -n "$ROUTE_ID_CREATED" ] && [ "$ROUTE_ID_CREATED" != "null" ]; then
                    echo ""
                    print_info "验证路线详情:"
                    echo "curl -X GET \"$BASE_URL$API_ENDPOINT/$ROUTE_ID_CREATED/details\" | jq ."
                fi
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
        print_warning "请确认创建者用户ID '$CREATOR_ID' 是否存在"
        exit 1
        ;;
    409)
        print_error "路线已存在或冲突 (409)"
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
