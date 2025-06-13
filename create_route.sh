#!/bin/bash

# WalkBG 路线创建脚本
# 用于调用POST API创建一个完整的路线数据

# 配置
BASE_URL="http://localhost:8080"
API_ENDPOINT="/api/routes"
CONTENT_TYPE="application/json"

# 颜色输出
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
BLUE='\033[0;34m'
NC='\033[0m' # No Color

echo -e "${BLUE}=== WalkBG 路线创建工具 ===${NC}"
echo ""

# 检查服务是否运行
echo -e "${YELLOW}检查服务状态...${NC}"
if ! curl -s "${BASE_URL}/actuator/health" > /dev/null 2>&1; then
    echo -e "${RED}❌ 服务未运行，请先启动 WalkBG 服务${NC}"
    echo "启动命令: mvn spring-boot:run"
    exit 1
fi
echo -e "${GREEN}✅ 服务运行正常${NC}"
echo ""

# 创建路线数据
echo -e "${YELLOW}创建路线数据...${NC}"

# 构建完整的路线JSON数据
ROUTE_DATA='{
  "name": "黄山天都峰徒步路线",
  "description": "黄山天都峰是黄山三大主峰之一，海拔1864米。这条路线从慈光阁出发，经过半山寺、玉屏楼，最终到达天都峰顶。路线风景秀丽，但难度较高，适合有经验的徒步爱好者。",
  "region": "安徽黄山",
  "region_id": "region_huangshan_001",
  "distance": 12.5,
  "duration": 8,
  "latitude": 30.1394,
  "longitude": 118.1558,
  "altitude": 800.0,
  "elevation_gain": 1064.0,
  "elevation_loss": 200.0,
  "difficulty": 2,
  "route_type": 0,
  "route_direction": 0,
  "status": 1,
  "cover_url": "https://example.com/images/huangshan_tiandufeng_cover.jpg",
  "map_data_id": "map_huangshan_001",
  "default_map_id": "default_map_001",
  "created_by": "user_admin_001",
  "tags": [
    "山峰",
    "风景名胜",
    "高难度",
    "一日游",
    "摄影"
  ],
  "seasons": [
    "春季",
    "夏季",
    "秋季"
  ],
  "waypoints": [
    {
      "name": "慈光阁",
      "description": "黄山南大门，徒步起点，海拔800米",
      "latitude": 30.1394,
      "longitude": 118.1558,
      "elevation": 800.0,
      "type": "起点",
      "icon_url": "https://example.com/icons/start_point.png",
      "image_url": "https://example.com/images/ciguangge.jpg",
      "sequence_number": 1
    },
    {
      "name": "半山寺",
      "description": "黄山著名古寺，徒步中转点，海拔1340米",
      "latitude": 30.1425,
      "longitude": 118.1612,
      "elevation": 1340.0,
      "type": "中转点",
      "icon_url": "https://example.com/icons/temple.png",
      "image_url": "https://example.com/images/banshan_temple.jpg",
      "sequence_number": 2
    },
    {
      "name": "玉屏楼",
      "description": "黄山著名景点，迎客松所在地，海拔1680米",
      "latitude": 30.1456,
      "longitude": 118.1634,
      "elevation": 1680.0,
      "type": "景点",
      "icon_url": "https://example.com/icons/scenic_spot.png",
      "image_url": "https://example.com/images/yupinglou.jpg",
      "sequence_number": 3
    },
    {
      "name": "天都峰顶",
      "description": "黄山三大主峰之一，海拔1864米，视野开阔",
      "latitude": 30.1478,
      "longitude": 118.1645,
      "elevation": 1864.0,
      "type": "终点",
      "icon_url": "https://example.com/icons/peak.png",
      "image_url": "https://example.com/images/tiandufeng_peak.jpg",
      "sequence_number": 4
    }
  ],
  "segments": [
    {
      "distance": 3.2,
      "elevation_gain": 540.0,
      "elevation_loss": 0.0,
      "estimated_time": 2.5,
      "difficulty": 1,
      "terrain": "石阶路",
      "surface_type": "石阶",
      "traffic_level": 2
    },
    {
      "distance": 2.8,
      "elevation_gain": 340.0,
      "elevation_loss": 0.0,
      "estimated_time": 2.0,
      "difficulty": 2,
      "terrain": "山路",
      "surface_type": "土路",
      "traffic_level": 1
    },
    {
      "distance": 1.5,
      "elevation_gain": 184.0,
      "elevation_loss": 0.0,
      "estimated_time": 1.5,
      "difficulty": 3,
      "terrain": "陡峭岩石",
      "surface_type": "岩石",
      "traffic_level": 0
    },
    {
      "distance": 5.0,
      "elevation_gain": 0.0,
      "elevation_loss": 200.0,
      "estimated_time": 2.0,
      "difficulty": 1,
      "terrain": "下山路",
      "surface_type": "石阶",
      "traffic_level": 1
    }
  ],
  "images": [
    {
      "imageUrl": "https://example.com/images/huangshan_tiandufeng_cover.jpg",
      "isCover": true,
      "sequenceNumber": 1
    },
    {
      "imageUrl": "https://example.com/images/huangshan_sunrise.jpg",
      "isCover": false,
      "sequenceNumber": 2
    },
    {
      "imageUrl": "https://example.com/images/huangshan_sea_of_clouds.jpg",
      "isCover": false,
      "sequenceNumber": 3
    }
  ]
}'

# 发送POST请求
echo "发送请求到: ${BASE_URL}${API_ENDPOINT}"
echo ""

RESPONSE=$(curl -s -w "\n%{http_code}" \
  -X POST \
  -H "Content-Type: ${CONTENT_TYPE}" \
  -H "Accept: application/json" \
  -d "${ROUTE_DATA}" \
  "${BASE_URL}${API_ENDPOINT}")

# 分离响应体和状态码
HTTP_BODY=$(echo "$RESPONSE" | head -n -1)
HTTP_CODE=$(echo "$RESPONSE" | tail -n 1)

echo -e "${BLUE}HTTP状态码: ${HTTP_CODE}${NC}"
echo ""

# 根据状态码处理响应
case $HTTP_CODE in
    201)
        echo -e "${GREEN}✅ 路线创建成功！${NC}"
        echo ""
        echo -e "${YELLOW}创建的路线信息:${NC}"
        echo "$HTTP_BODY" | python3 -m json.tool 2>/dev/null || echo "$HTTP_BODY"
        ;;
    400)
        echo -e "${RED}❌ 请求数据有误${NC}"
        echo -e "${YELLOW}错误详情:${NC}"
        echo "$HTTP_BODY" | python3 -m json.tool 2>/dev/null || echo "$HTTP_BODY"
        ;;
    500)
        echo -e "${RED}❌ 服务器内部错误${NC}"
        echo -e "${YELLOW}错误详情:${NC}"
        echo "$HTTP_BODY"
        ;;
    *)
        echo -e "${RED}❌ 请求失败 (HTTP ${HTTP_CODE})${NC}"
        echo -e "${YELLOW}响应内容:${NC}"
        echo "$HTTP_BODY"
        ;;
esac

echo ""
echo -e "${BLUE}=== 脚本执行完成 ===${NC}"

# 提供后续操作建议
if [ "$HTTP_CODE" = "201" ]; then
    echo ""
    echo -e "${YELLOW}后续操作建议:${NC}"
    echo "1. 查看所有路线: curl '${BASE_URL}/api/routes?page=0&size=10'"
    echo "2. 查看路线详情: curl '${BASE_URL}/api/routes/{route_id}'"
    echo "3. 访问Swagger文档: ${BASE_URL}/swagger-ui.html"
fi