#!/bin/bash

# 批量创建测试用户的Shell脚本

# 设置默认值
BASE_URL="http://localhost:8080/walkbg"
SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
CREATE_USER_SCRIPT="$SCRIPT_DIR/create_user.sh"

# 颜色定义
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
BLUE='\033[0;34m'
NC='\033[0m' # No Color

print_info() {
    echo -e "${BLUE}[INFO]${NC} $1"
}

print_success() {
    echo -e "${GREEN}[SUCCESS]${NC} $1"
}

print_error() {
    echo -e "${RED}[ERROR]${NC} $1"
}

# 检查create_user.sh脚本是否存在
if [ ! -f "$CREATE_USER_SCRIPT" ]; then
    print_error "找不到create_user.sh脚本: $CREATE_USER_SCRIPT"
    exit 1
fi

# 检查脚本是否可执行
if [ ! -x "$CREATE_USER_SCRIPT" ]; then
    print_error "create_user.sh脚本没有执行权限"
    print_info "请运行: chmod +x $CREATE_USER_SCRIPT"
    exit 1
fi

print_info "开始批量创建测试用户..."
print_info "使用脚本: $CREATE_USER_SCRIPT"
print_info "API服务器: $BASE_URL"
echo ""

# 定义测试用户数据
declare -a TEST_USERS=(
    "admin admin@walkbg.com 系统管理员 13000130000"
    "hiking_expert expert@walkbg.com 徒步专家 13100131000"
    "mountain_guide guide@walkbg.com 山地向导 13200132000"
    "john_doe john@example.com John_Doe 13300133000"
    "alice alice@example.com Alice_Smith 13400134000"
    "bob bob@example.com Bob_Wilson 13500135000"
    "local_guide local@walkbg.com 本地向导 13600136000"
    "route_creator creator@walkbg.com 路线创建者 13700137000"
)

# 统计变量
TOTAL_USERS=${#TEST_USERS[@]}
SUCCESS_COUNT=0
FAILED_COUNT=0

print_info "准备创建 $TOTAL_USERS 个测试用户"
echo ""

# 遍历创建用户
for i in "${!TEST_USERS[@]}"; do
    USER_DATA="${TEST_USERS[$i]}"
    USER_NUM=$((i + 1))

    print_info "[$USER_NUM/$TOTAL_USERS] 创建用户: $USER_DATA"

    # 调用create_user.sh脚本
    if $CREATE_USER_SCRIPT $USER_DATA; then
        SUCCESS_COUNT=$((SUCCESS_COUNT + 1))
        print_success "用户创建成功"
    else
        FAILED_COUNT=$((FAILED_COUNT + 1))
        print_error "用户创建失败"
    fi

    echo ""

    # 添加短暂延迟，避免请求过于频繁
    sleep 0.5
done

# 输出统计结果
echo "========================================"
print_info "批量创建用户完成"
print_success "成功创建: $SUCCESS_COUNT 个用户"
if [ $FAILED_COUNT -gt 0 ]; then
    print_error "创建失败: $FAILED_COUNT 个用户"
else
    print_success "所有用户创建成功!"
fi
echo "========================================"

# 如果有成功创建的用户，显示验证命令
if [ $SUCCESS_COUNT -gt 0 ]; then
    echo ""
    print_info "验证创建的用户:"
    echo "curl -X GET \"$BASE_URL/api/users\" | jq ."
    echo ""
    print_info "获取用户统计:"
    echo "curl -X GET \"$BASE_URL/api/users/statistics\" | jq ."
fi
