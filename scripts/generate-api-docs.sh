#!/bin/bash

# API文档生成脚本
# 用于自动生成和更新WalkBG项目的API文档

set -e

# 配置变量
PROJECT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
DOCS_DIR="$PROJECT_DIR/docs"
API_BASE_URL="http://localhost:8080/walkbg"
OPENAPI_JSON_URL="$API_BASE_URL/api-docs"
SWAGGER_UI_URL="$API_BASE_URL/swagger-ui/index.html"

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

# 检查应用是否运行
check_app_running() {
    log_info "检查应用是否运行..."

    if curl -s -f "$API_BASE_URL/actuator/health" > /dev/null; then
        log_success "应用正在运行"
        return 0
    else
        log_error "应用未运行，请先启动应用"
        return 1
    fi
}

# 启动应用
start_app() {
    log_info "启动Spring Boot应用..."

    cd "$PROJECT_DIR"

    # 检查是否已经有进程在运行
    if pgrep -f "walkbg" > /dev/null; then
        log_warning "检测到应用已在运行，跳过启动"
        return 0
    fi

    # 后台启动应用
    nohup mvn spring-boot:run > logs/app.log 2>&1 &
    APP_PID=$!

    log_info "应用启动中，PID: $APP_PID"

    # 等待应用启动
    local max_attempts=30
    local attempt=0

    while [ $attempt -lt $max_attempts ]; do
        if curl -s -f "$API_BASE_URL/actuator/health" > /dev/null; then
            log_success "应用启动成功"
            return 0
        fi

        sleep 2
        attempt=$((attempt + 1))
        log_info "等待应用启动... ($attempt/$max_attempts)"
    done

    log_error "应用启动超时"
    return 1
}

# 生成OpenAPI JSON
generate_openapi_json() {
    log_info "生成OpenAPI JSON规范..."

    # 确保docs目录存在
    mkdir -p "$DOCS_DIR"

    # 下载OpenAPI JSON
    if curl -s -f "$OPENAPI_JSON_URL" -o "$DOCS_DIR/openapi.json"; then
        log_success "OpenAPI JSON生成成功: $DOCS_DIR/openapi.json"

        # 格式化JSON（如果有jq工具）
        if command -v jq > /dev/null; then
            jq '.' "$DOCS_DIR/openapi.json" > "$DOCS_DIR/openapi.formatted.json"
            mv "$DOCS_DIR/openapi.formatted.json" "$DOCS_DIR/openapi.json"
            log_info "JSON已格式化"
        fi

        return 0
    else
        log_error "OpenAPI JSON生成失败"
        return 1
    fi
}

# 生成YAML格式
generate_openapi_yaml() {
    log_info "生成OpenAPI YAML规范..."

    # 尝试下载YAML格式
    if curl -s -f "$OPENAPI_JSON_URL.yaml" -o "$DOCS_DIR/openapi.yaml"; then
        log_success "OpenAPI YAML生成成功: $DOCS_DIR/openapi.yaml"
        return 0
    else
        log_warning "YAML格式不可用，跳过"
        return 0
    fi
}

# 生成静态HTML文档
generate_static_html() {
    log_info "生成静态HTML文档..."

    # 检查是否有redoc-cli
    if command -v redoc-cli > /dev/null; then
        log_info "使用Redoc生成HTML文档..."
        redoc-cli build "$DOCS_DIR/openapi.json" --output "$DOCS_DIR/redoc.html"
        log_success "Redoc HTML文档生成成功: $DOCS_DIR/redoc.html"
    elif command -v swagger-codegen > /dev/null; then
        log_info "使用swagger-codegen生成HTML文档..."
        swagger-codegen generate -i "$DOCS_DIR/openapi.json" -l html2 -o "$DOCS_DIR/swagger-html"
        log_success "Swagger HTML文档生成成功: $DOCS_DIR/swagger-html"
    else
        log_warning "未找到文档生成工具，跳过静态HTML生成"
        log_info "可以安装: npm install -g redoc-cli"
    fi
}

# 生成Postman Collection
generate_postman_collection() {
    log_info "生成Postman Collection..."

    if command -v openapi2postman > /dev/null; then
        openapi2postman -s "$DOCS_DIR/openapi.json" -o "$DOCS_DIR/WalkBG-API.postman_collection.json"
        log_success "Postman Collection生成成功: $DOCS_DIR/WalkBG-API.postman_collection.json"
    else
        log_warning "未找到openapi2postman工具，跳过Postman Collection生成"
        log_info "可以安装: npm install -g openapi-to-postman"
    fi
}

# 更新文档索引页面
update_index_page() {
    log_info "更新文档索引页面..."

    local current_time=$(date '+%Y-%m-%d %H:%M:%S')

    # 如果api-docs.html不存在，则已经在前面创建了
    if [ -f "$DOCS_DIR/api-docs.html" ]; then
        # 更新时间戳
        sed -i.bak "s/生成时间: [0-9-]*/生成时间: $(date '+%Y-%m-%d')/g" "$DOCS_DIR/api-docs.html"
        rm -f "$DOCS_DIR/api-docs.html.bak"
        log_success "文档索引页面已更新"
    fi
}

# 生成README
generate_readme() {
    log_info "生成API文档README..."

    cat > "$DOCS_DIR/README.md" << EOF
# WalkBG API 文档

## 📖 文档访问

### 在线文档
- [Swagger UI (交互式)](http://localhost:8080/walkbg/swagger-ui/index.html) - 推荐用于API测试
- [API文档首页](./api-docs.html) - 文档概览和快速导航

### 文档文件
- [OpenAPI JSON规范](./openapi.json) - 标准的OpenAPI 3.0规范
- [OpenAPI YAML规范](./openapi.yaml) - YAML格式的规范文件
- [Postman Collection](./WalkBG-API.postman_collection.json) - 用于Postman的API集合

## 🚀 快速开始

### 1. 启动应用
\`\`\`bash
mvn spring-boot:run
\`\`\`

### 2. 访问文档
打开浏览器访问: http://localhost:8080/walkbg/swagger-ui/index.html

### 3. 测试API
在Swagger UI中可以直接测试所有API接口

## 📋 API概览

### 核心模块
- **路线管理** - 路线的CRUD操作、搜索、推荐
- **用户管理** - 用户注册、信息管理
- **水源管理** - 水源点信息管理
- **补给管理** - 补给点信息管理
- **营地管理** - 营地信息管理
- **联系人管理** - 向导、接送等联系人管理
- **行程管理** - 徒步行程规划和管理

### 路线管理API
- \`GET /api/v1/routes\` - 分页查询路线列表
- \`GET /api/v1/routes/{id}\` - 查询路线详情
- \`POST /api/v1/routes\` - 创建路线
- \`PUT /api/v1/routes/{id}\` - 更新路线
- \`DELETE /api/v1/routes/{id}\` - 删除路线
- \`GET /api/v1/routes/my\` - 查询我创建的路线
- \`GET /api/v1/routes/favorites\` - 查询我收藏的路线
- \`GET /api/v1/routes/completed\` - 查询我完成的路线
- \`GET /api/v1/routes/recommendations\` - 获取推荐路线
- \`GET /api/v1/routes/nearby\` - 获取附近的路线

## 🔧 开发信息

- **基础URL**: http://localhost:8080/walkbg
- **API版本**: v1
- **数据格式**: JSON
- **响应格式**: 统一的ApiResponse包装

## 📝 更新日志

- **$(date '+%Y-%m-%d')**: 文档自动生成和更新
- **2025-06-20**: 初始版本发布

---

*文档最后更新: $(date '+%Y-%m-%d %H:%M:%S')*
EOF

    log_success "README.md生成成功"
}

# 清理函数
cleanup() {
    if [ ! -z "$APP_PID" ] && [ "$STARTED_BY_SCRIPT" = "true" ]; then
        log_info "清理资源..."
        kill $APP_PID 2>/dev/null || true
    fi
}

# 主函数
main() {
    log_info "开始生成API文档..."
    log_info "项目目录: $PROJECT_DIR"
    log_info "文档目录: $DOCS_DIR"

    # 设置清理陷阱
    trap cleanup EXIT

    # 检查应用状态
    if ! check_app_running; then
        if ! start_app; then
            log_error "无法启动应用，退出"
            exit 1
        fi
        STARTED_BY_SCRIPT=true
    fi

    # 生成各种格式的文档
    generate_openapi_json || exit 1
    generate_openapi_yaml
    generate_static_html
    generate_postman_collection
    update_index_page
    generate_readme

    log_success "API文档生成完成！"
    log_info "文档位置: $DOCS_DIR"
    log_info "在线访问: $SWAGGER_UI_URL"

    # 显示生成的文件
    log_info "生成的文件:"
    ls -la "$DOCS_DIR" | grep -E '\.(json|yaml|html|md)$' | while read line; do
        echo "  $line"
    done
}

# 帮助信息
show_help() {
    cat << EOF
WalkBG API文档生成脚本

用法: $0 [选项]

选项:
  -h, --help     显示帮助信息
  -s, --start    强制启动应用
  -c, --clean    清理旧文档后重新生成

示例:
  $0              # 生成API文档
  $0 --start      # 启动应用并生成文档
  $0 --clean      # 清理后重新生成

EOF
}

# 解析命令行参数
while [[ $# -gt 0 ]]; do
    case $1 in
        -h|--help)
            show_help
            exit 0
            ;;
        -s|--start)
            FORCE_START=true
            shift
            ;;
        -c|--clean)
            CLEAN_DOCS=true
            shift
            ;;
        *)
            log_error "未知选项: $1"
            show_help
            exit 1
            ;;
    esac
done

# 清理旧文档
if [ "$CLEAN_DOCS" = "true" ]; then
    log_info "清理旧文档..."
    rm -rf "$DOCS_DIR"/*.json "$DOCS_DIR"/*.yaml "$DOCS_DIR"/*.html "$DOCS_DIR"/swagger-html
    log_success "旧文档已清理"
fi

# 执行主函数
main
