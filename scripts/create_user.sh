#!/bin/bash

# 创建用户的Shell脚本
# 使用方法: ./create_user.sh [username] [email] [nickname] [phone]

# 设置默认值
BASE_URL="http://localhost:8080/walkbg"
API_ENDPOINT="/api/users"

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
    echo "  $0 <username> <email> [nickname] [phone]"
    echo ""
    echo "参数说明:"
    echo "  username  - 用户名 (必填)"
    echo "  email     - 邮箱地址 (必填)"
    echo "  nickname  - 昵称 (可选)"
    echo "  phone     - 手机号 (可选)"
    echo ""
    echo "示例:"
    echo "  $0 john_doe john@example.com \"John Doe\" \"13800138000\""
    echo "  $0 alice alice@example.com"
}

# 验证邮箱格式
validate_email() {
    local email=$1
    if [[ $email =~ ^[a-zA-Z0-9._%+-]+@[a-zA-Z0-9.-]+\.[a-zA-Z]{2,}$ ]]; then
        return 0
    else
        return 1
    fi
}

# 生成UUID
generate_uuid() {
    if command -v uuidgen &> /dev/null; then
        uuidgen | tr '[:upper:]' '[:lower:]'
    else
        # 如果没有uuidgen，使用简单的随机字符串
        echo "user-$(date +%s)-$(shuf -i 1000-9999 -n 1)"
    fi
}

# 检查参数
if [ $# -lt 2 ]; then
    print_error "参数不足"
    show_usage
    exit 1
fi

# 获取参数
USERNAME=$1
EMAIL=$2
NICKNAME=${3:-""}
PHONE=${4:-""}

# 验证必填参数
if [ -z "$USERNAME" ]; then
    print_error "用户名不能为空"
    exit 1
fi

if [ -z "$EMAIL" ]; then
    print_error "邮箱不能为空"
    exit 1
fi

# 验证邮箱格式
if ! validate_email "$EMAIL"; then
    print_error "邮箱格式不正确: $EMAIL"
    exit 1
fi

# 生成用户ID
USER_ID=$(generate_uuid)

print_info "准备创建用户..."
print_info "用户ID: $USER_ID"
print_info "用户名: $USERNAME"
print_info "邮箱: $EMAIL"
print_info "昵称: ${NICKNAME:-"(未设置)"}"
print_info "手机: ${PHONE:-"(未设置)"}"

# 构建JSON数据
JSON_DATA=$(cat <<EOF
{
  "id": "$USER_ID",
  "username": "$USERNAME",
  "email": "$EMAIL",
  "nickname": $(if [ -n "$NICKNAME" ]; then echo "\"$NICKNAME\""; else echo "\"$USERNAME\""; fi),
  "phone": $(if [ -n "$PHONE" ]; then echo "\"$PHONE\""; else echo "null"; fi)
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
        print_success "用户创建成功!"
        echo ""
        print_info "响应数据:"
        echo "$HTTP_BODY" | jq . 2>/dev/null || echo "$HTTP_BODY"

        # 尝试提取用户信息
        if command -v jq &> /dev/null; then
            CREATED_USER=$(echo "$HTTP_BODY" | jq -r '.data // empty')
            if [ -n "$CREATED_USER" ] && [ "$CREATED_USER" != "null" ]; then
                echo ""
                print_success "创建的用户信息:"
                echo "$CREATED_USER" | jq .
            fi
        fi
        ;;
    400)
        print_error "请求参数错误 (400)"
        echo "$HTTP_BODY" | jq -r '.message // .error // .' 2>/dev/null || echo "$HTTP_BODY"
        exit 1
        ;;
    409)
        print_error "用户已存在 (409)"
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
