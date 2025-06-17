#!/bin/bash

# 简化版创建Route的Shell脚本
# 使用方法: ./create_route_simple.sh route_data.json 或 echo '{"name":"xxx",...}' | ./create_route_simple.sh

# 设置默认值
BASE_URL="http://localhost:8080/walkbg"
API_ENDPOINT="/api/routes"

# 颜色定义
RED='\033[0;31m'
GREEN='\033[0;32m'
BLUE='\033[0;34m'
NC='\033[0m'

# 打印带颜色的消息（输出到stderr）
print_info() { echo -e "${BLUE}[INFO]${NC} $1" >&2; }
print_success() { echo -e "${GREEN}[SUCCESS]${NC} $1" >&2; }
print_error() { echo -e "${RED}[ERROR]${NC} $1" >&2; }

# 显示帮助信息
show_help() {
    cat >&2 <<EOF
简化版路线创建脚本

用法: $0 route_data.json
      echo '{...}' | $0

示例:
  $0 my_route.json                     # 使用JSON文件
  echo '{"name":"xxx",...}' | $0      # 使用管道输入

选项:
  -h, --help    显示此帮助信息

注意: 此脚本不提供默认值，必须提供完整的JSON数据
EOF
}

# 处理帮助参数
if [[ "$1" == "-h" || "$1" == "--help" ]]; then
    show_help
    exit 0
fi

# 获取JSON数据
JSON_DATA=""

if [ -p /dev/stdin ]; then
    # 从管道读取
    JSON_DATA=$(cat)
    print_info "从管道读取路线数据"
elif [ -n "$1" ] && [ -f "$1" ]; then
    # 从文件读取
    JSON_DATA=$(cat "$1")
    print_info "从文件读取路线数据: $1"
elif [ -n "$1" ]; then
    # 直接作为JSON字符串
    JSON_DATA="$1"
    print_info "使用提供的JSON字符串"
else
    # 没有输入数据
    print_error "错误: 必须提供JSON数据"
    show_help
    exit 1
fi

# 检查是否有数据
if [ -z "$JSON_DATA" ]; then
    print_error "错误: JSON数据为空"
    exit 1
fi

# 验证JSON格式
if ! echo "$JSON_DATA" | jq . >/dev/null 2>&1; then
    print_error "错误: 无效的JSON格式"
    exit 1
fi

# 验证必需字段
REQUIRED_FIELDS=("name" "created_by" "default_map_id")
for field in "${REQUIRED_FIELDS[@]}"; do
    if ! echo "$JSON_DATA" | jq -e ".$field" >/dev/null 2>&1; then
        print_error "错误: 缺少必需字段: $field"
        exit 1
    fi
done

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
        print_success "路线创建成功!"

        # 提取路线ID并输出
        if command -v jq &> /dev/null; then
            ROUTE_ID_CREATED=$(echo "$HTTP_BODY" | jq -r '.data.id // empty')
            if [ -n "$ROUTE_ID_CREATED" ] && [ "$ROUTE_ID_CREATED" != "null" ]; then
                print_success "路线ID: $ROUTE_ID_CREATED"
                print_info "验证命令: curl -X GET \"$BASE_URL$API_ENDPOINT/$ROUTE_ID_CREATED/details\" | jq ."
                echo "$ROUTE_ID_CREATED"  # 只输出ID到stdout
                exit 0
            fi
        fi
        print_error "无法提取路线ID"
        exit 1
        ;;
    400)
        print_error "请求参数错误 (400)"
        echo "$HTTP_BODY" | jq -r '.message // .error // .' 2>/dev/null || echo "$HTTP_BODY" >&2
        exit 1
        ;;
    404)
        print_error "创建者用户不存在或地图数据不存在 (404)"
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
