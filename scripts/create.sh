#!/bin/bash

# 完整路线创建主脚本
# 使用方法: ./create.sh route_data.json [username] [email]

# 设置默认值
SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"

# 颜色定义
RED='\033[0;31m'
GREEN='\033[0;32m'
BLUE='\033[0;34m'
YELLOW='\033[1;33m'
NC='\033[0m'

# 打印带颜色的消息
print_info() { echo -e "${BLUE}[INFO]${NC} $1"; }
print_success() { echo -e "${GREEN}[SUCCESS]${NC} $1"; }
print_error() { echo -e "${RED}[ERROR]${NC} $1"; }
print_warning() { echo -e "${YELLOW}[WARNING]${NC} $1"; }

# 显示帮助信息
show_help() {
    echo "WalkBG 完整路线创建工具"
    echo ""
    echo "用法: $0 route_data.json [username] [email]"
    echo ""
    echo "参数:"
    echo "  route_data.json  必填，路线数据JSON文件路径"
    echo "  username         可选，用户名（默认从JSON中获取created_by字段作为用户名）"
    echo "  email            可选，邮箱（默认为username@walkbg.com）"
    echo ""
    echo "选项:"
    echo "  -h, --help      显示此帮助信息"
    echo ""
    echo "示例:"
    echo "  $0 my_route.json                    # 使用JSON中的created_by作为用户名"
    echo "  $0 my_route.json john_doe           # 指定用户名"
    echo "  $0 my_route.json john_doe john@example.com  # 指定用户名和邮箱"
    echo ""
    echo "流程:"
    echo "  0. 验证JSON数据格式"
    echo "  1. 查询用户是否存在，不存在则创建"
    echo "  2. 创建地图数据"
    echo "  3. 更新路线JSON中的用户ID和地图ID"
    echo "  4. 创建路线"
}

# 检查子脚本是否存在
check_scripts() {
    local scripts=("validate_route.sh" "create_user.sh" "create_map.sh" "create_route_simple.sh")

    for script in "${scripts[@]}"; do
        if [ ! -f "$SCRIPT_DIR/$script" ]; then
            print_error "找不到子脚本: $script"
            exit 1
        fi
        chmod +x "$SCRIPT_DIR/$script"
    done
}

# 查询用户是否存在
query_user_exists() {
    local username="$1"
    print_info "查询用户: $username" >&2

    local response=$(curl -s "http://localhost:8080/walkbg/api/user/username/$username")
    local user_id=$(echo "$response" | jq -r '.data.id // empty')

    if [ -n "$user_id" ] && [ "$user_id" != "null" ]; then
        echo "$user_id"
        return 0
    else
        return 1
    fi
}

# 解析命令行参数
if [[ "$1" == "-h" || "$1" == "--help" ]]; then
    show_help
    exit 0
fi

# 检查必需参数
if [ -z "$1" ]; then
    print_error "错误: 必须提供路线数据JSON文件"
    show_help
    exit 1
fi

ROUTE_FILE="$1"
USERNAME="$2"
EMAIL="$3"

# 检查文件是否存在
if [ ! -f "$ROUTE_FILE" ]; then
    print_error "错误: 路线数据文件不存在: $ROUTE_FILE"
    exit 1
fi

# 检查子脚本
check_scripts

print_info "=== WalkBG 完整路线创建开始 ==="
print_info "路线文件: $ROUTE_FILE"

# 步骤0: 验证JSON数据格式
print_info "步骤 0/5: 验证JSON数据格式..."

if ! "$SCRIPT_DIR/validate_route.sh" "$ROUTE_FILE"; then
    print_error "JSON数据验证失败，请修正后重试"
    exit 1
fi
print_success "JSON数据验证通过"

# 读取并验证JSON文件
JSON_DATA=$(cat "$ROUTE_FILE")
if ! echo "$JSON_DATA" | jq . >/dev/null 2>&1; then
    print_error "错误: 无效的JSON格式"
    exit 1
fi

# 固定使用用户名 marthon
USERNAME="marthon"

# 设置默认邮箱
if [ -z "$EMAIL" ]; then
    EMAIL="marthon@walkbg.com"
fi

print_info "用户名: $USERNAME"
print_info "邮箱: $EMAIL"

# 步骤1: 查询或创建用户
print_info "步骤 1/5: 处理用户信息..."

USER_ID=$(query_user_exists "$USERNAME")
if [ $? -eq 0 ]; then
    print_success "用户已存在，ID: $USER_ID"
else
    print_info "用户不存在，创建新用户..."
    USER_ID=$("$SCRIPT_DIR/create_user.sh" "$USERNAME" "$EMAIL")
    if [ $? -ne 0 ] || [ -z "$USER_ID" ]; then
        print_error "用户创建失败"
        exit 1
    fi
    print_success "用户创建成功，ID: $USER_ID"
fi

# 步骤2: 创建地图数据
print_info "步骤 2/5: 创建地图数据..."

MAP_ID=$("$SCRIPT_DIR/create_map.sh")
if [ $? -ne 0 ] || [ -z "$MAP_ID" ]; then
    print_error "地图数据创建失败"
    exit 1
fi
print_success "地图数据创建成功，ID: $MAP_ID"

# 步骤3: 更新JSON中的用户ID和地图ID
print_info "步骤 3/5: 更新路线数据..."

UPDATED_JSON=$(echo "$JSON_DATA" | jq \
    --arg user_id "$USER_ID" \
    --arg map_id "$MAP_ID" \
    '.created_by = $user_id | .default_map_id = $map_id |
     if .supplies then .supplies = [.supplies[] | .last_verified = $user_id | .updated_by = $user_id] else . end')

if [ $? -ne 0 ]; then
    print_error "JSON更新失败"
    exit 1
fi

# 创建临时文件保存更新后的JSON
TEMP_ROUTE_FILE=$(mktemp)
echo "$UPDATED_JSON" > "$TEMP_ROUTE_FILE"

print_success "路线数据更新完成"

# 步骤4: 再次验证更新后的JSON数据
print_info "步骤 4/5: 验证更新后的JSON数据..."

if ! "$SCRIPT_DIR/validate_route.sh" "$TEMP_ROUTE_FILE" >/dev/null 2>&1; then
    print_warning "更新后的JSON数据验证失败，但继续创建路线..."
else
    print_success "更新后的JSON数据验证通过"
fi

# 步骤5: 创建路线
print_info "步骤 5/5: 创建路线..."

ROUTE_ID=$("$SCRIPT_DIR/create_route_simple.sh" "$TEMP_ROUTE_FILE")
if [ $? -ne 0 ] || [ -z "$ROUTE_ID" ]; then
    rm -f "$TEMP_ROUTE_FILE"
    print_error "路线创建失败"
    exit 1
fi

# 清理临时文件
rm -f "$TEMP_ROUTE_FILE"

print_success "路线创建成功，ID: $ROUTE_ID"

# 最终结果
echo ""
print_success "=== 完整路线创建完成 ==="
print_info "用户ID: $USER_ID"
print_info "地图数据ID: $MAP_ID"
print_info "路线ID: $ROUTE_ID"
echo ""
print_info "验证命令:"
echo "curl -X GET \"http://localhost:8080/walkbg/api/routes/$ROUTE_ID/details\" | jq ."
echo ""
print_info "查看路线统计:"
echo "curl -X GET \"http://localhost:8080/walkbg/api/routes/$ROUTE_ID/details\" | jq '.data | {segments: (.segments | length), tags: (.tags | length), images: (.image_urls | length), supplies: (.supplies | length), campsites: (.campsites | length), daily_plans: (.daily_plans | length), marker_points: (.marker_points | length), water_sources: (.water_sources | length), hitchhike_contacts: (.hitchhike_contacts | length)}'"
