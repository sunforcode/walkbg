#!/bin/bash

# 创建RouteMapData的Shell脚本
# 使用方法: ./create_map.sh [map_data.json] 或 echo '{"id":"xxx",...}' | ./create_map.sh

# 设置默认值
BASE_URL="http://localhost:8080/walkbg"
API_ENDPOINT="/api/route-map-data"

# 颜色定义
RED='\033[0;31m'
GREEN='\033[0;32m'
BLUE='\033[0;34m'
NC='\033[0m'

# 打印带颜色的消息（输出到stderr）
print_info() { echo -e "${BLUE}[INFO]${NC} $1" >&2; }
print_success() { echo -e "${GREEN}[SUCCESS]${NC} $1" >&2; }
print_error() { echo -e "${RED}[ERROR]${NC} $1" >&2; }

# 生成UUID
generate_uuid() {
    uuidgen 2>/dev/null || echo "$(date +%s)-$(shuf -i 1000-9999 -n 1)"
}

# 显示帮助信息
show_help() {
    cat >&2 <<EOF
用法: $0 [map_data.json]
      echo '{...}' | $0

示例:
  $0                           # 使用默认地图数据
  $0 my_map.json              # 使用JSON文件
  echo '{"id":"xxx",...}' | $0  # 使用管道输入

选项:
  -h, --help    显示此帮助信息
EOF
}

# 处理帮助参数
if [[ "$1" == "-h" || "$1" == "--help" ]]; then
    show_help
    exit 0
fi

# 获取JSON数据
if [ -p /dev/stdin ]; then
    # 从管道读取
    JSON_DATA=$(cat)
    print_info "从管道读取地图数据"
elif [ -n "$1" ] && [ -f "$1" ]; then
    # 从文件读取
    JSON_DATA=$(cat "$1")
    print_info "从文件读取地图数据: $1"
elif [ -n "$1" ]; then
    # 直接作为JSON字符串
    JSON_DATA="$1"
    print_info "使用提供的JSON字符串"
else
    # 使用默认数据
    MAP_DATA_ID=$(generate_uuid)
    JSON_DATA=$(cat <<EOF
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
    print_info "使用默认地图数据，ID: $MAP_DATA_ID"
fi

# 验证JSON格式
if ! echo "$JSON_DATA" | jq . >/dev/null 2>&1; then
    print_error "无效的JSON格式"
    exit 1
fi

# 发送HTTP请求
print_info "发送请求到: $BASE_URL$API_ENDPOINT"

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
        print_success "地图数据创建成功!"

        # 提取并输出地图数据ID（供其他脚本使用）
        if command -v jq &> /dev/null; then
            MAP_ID=$(echo "$HTTP_BODY" | jq -r '.data.id // empty')
            if [ -n "$MAP_ID" ] && [ "$MAP_ID" != "null" ]; then
                echo "$MAP_ID"  # 只输出ID到stdout
                exit 0
            fi
        fi
        print_error "无法提取地图数据ID"
        exit 1
        ;;
    400)
        print_error "请求参数错误 (400)"
        echo "$HTTP_BODY" | jq -r '.message // .error // .' 2>/dev/null || echo "$HTTP_BODY" >&2
        exit 1
        ;;
    500)
        print_error "服务器内部错误 (500)"
        echo "$HTTP_BODY" | jq -r '.message // .error // .' 2>/dev/null || echo "$HTTP_BODY" >&2
        exit 1
        ;;
    *)
        print_error "请求失败，HTTP状态码: $HTTP_CODE"
        echo "$HTTP_BODY" >&2
        exit 1
        ;;
esac
