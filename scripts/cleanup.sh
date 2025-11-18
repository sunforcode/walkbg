#!/bin/bash
# 项目清理脚本
# 用于清理临时文件、日志文件和不必要的文件

set -e

# 颜色输出
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
BLUE='\033[0;34m'
NC='\033[0m' # No Color

# 日志函数
log_info() {
    echo -e "${BLUE}[INFO]${NC} $1"
}

log_success() {
    echo -e "${GREEN}[SUCCESS]${NC} $1"
}

log_warning() {
    echo -e "${YELLOW}[WARNING]${NC} $1"
}

log_error() {
    echo -e "${RED}[ERROR]${NC} $1"
}

# 获取项目根目录
PROJECT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
cd "$PROJECT_DIR"

echo "🧹 开始清理项目..."
echo "项目目录: $PROJECT_DIR"
echo ""

# 1. 删除 .DS_Store 文件
log_info "删除 .DS_Store 文件..."
DS_STORE_COUNT=$(find . -name ".DS_Store" -type f | wc -l | tr -d ' ')
if [ "$DS_STORE_COUNT" -gt 0 ]; then
    find . -name ".DS_Store" -type f -delete
    log_success "删除了 $DS_STORE_COUNT 个 .DS_Store 文件"
else
    log_info "没有找到 .DS_Store 文件"
fi
echo ""

# 2. 清理日志文件
log_info "清理日志文件..."
if [ -d "logs" ]; then
    LOG_COUNT=$(find logs -type f \( -name "*.log" -o -name "*.gz" \) | wc -l | tr -d ' ')
    if [ "$LOG_COUNT" -gt 0 ]; then
        rm -rf logs/*.log logs/*.gz 2>/dev/null || true
        log_success "删除了 $LOG_COUNT 个日志文件"
    else
        log_info "没有找到日志文件"
    fi
    
    # 确保 logs 目录存在
    if [ ! -f "logs/.gitkeep" ]; then
        touch logs/.gitkeep
        log_success "创建了 logs/.gitkeep"
    fi
else
    log_warning "logs 目录不存在"
fi
echo ""

# 3. 删除未使用的 PostgreSQL 配置
log_info "检查未使用的配置文件..."
POSTGRES_CONFIG="src/main/resources/application-postgres.yml"
if [ -f "$POSTGRES_CONFIG" ]; then
    log_warning "发现未使用的 PostgreSQL 配置文件"
    read -p "是否删除 $POSTGRES_CONFIG? (y/n) " -n 1 -r
    echo
    if [[ $REPLY =~ ^[Yy]$ ]]; then
        rm -f "$POSTGRES_CONFIG"
        log_success "已删除 $POSTGRES_CONFIG"
    else
        log_info "保留 $POSTGRES_CONFIG"
    fi
else
    log_info "没有找到未使用的配置文件"
fi
echo ""

# 4. 清理 Maven 编译输出
log_info "清理 Maven 编译输出..."
if [ -d "target" ]; then
    mvn clean -q
    log_success "Maven 编译输出已清理"
else
    log_info "target 目录不存在"
fi
echo ""

# 5. 更新 .gitignore
log_info "检查 .gitignore 配置..."
GITIGNORE_UPDATED=false

# 检查是否需要添加 .catpaw/rules-mdc/
if ! grep -q ".catpaw/rules-mdc/" .gitignore 2>/dev/null; then
    echo "" >> .gitignore
    echo "# CatPaw AI 规则库" >> .gitignore
    echo ".catpaw/rules-mdc/" >> .gitignore
    log_success "已添加 .catpaw/rules-mdc/ 到 .gitignore"
    GITIGNORE_UPDATED=true
fi

# 检查是否需要添加数据库文件
if ! grep -q "data/" .gitignore 2>/dev/null; then
    echo "" >> .gitignore
    echo "# 数据库文件" >> .gitignore
    echo "data/" >> .gitignore
    log_success "已添加 data/ 到 .gitignore"
    GITIGNORE_UPDATED=true
fi

if [ "$GITIGNORE_UPDATED" = false ]; then
    log_info ".gitignore 已是最新"
fi
echo ""

# 6. 清理临时文件
log_info "清理临时文件..."
TEMP_FILES=$(find . -type f \( -name "*.tmp" -o -name "*.bak" -o -name "*.swp" -o -name "*.swo" -o -name "*~" \) 2>/dev/null | wc -l | tr -d ' ')
if [ "$TEMP_FILES" -gt 0 ]; then
    find . -type f \( -name "*.tmp" -o -name "*.bak" -o -name "*.swp" -o -name "*.swo" -o -name "*~" \) -delete 2>/dev/null || true
    log_success "删除了 $TEMP_FILES 个临时文件"
else
    log_info "没有找到临时文件"
fi
echo ""

# 7. 显示清理统计
echo "📊 清理统计:"
echo "  - .DS_Store 文件: $DS_STORE_COUNT 个"
echo "  - 日志文件: ${LOG_COUNT:-0} 个"
echo "  - 临时文件: $TEMP_FILES 个"
echo ""

log_success "✅ 清理完成！"
echo ""
echo "💡 建议:"
echo "  1. 运行 'git status' 检查是否有不应该提交的文件"
echo "  2. 定期运行此脚本保持项目整洁"
echo "  3. 查看 docs/FILE_CLEANUP_REPORT.md 了解更多清理建议"
