#!/usr/bin/env bash
#
# 本地手动部署入口（CI workflow 的兜底）。
# 与 .github/workflows/deploy.yml 的 deploy job 等价，但不写 .env——
# 服务器上的 .env 由 CI 从 GitHub Secrets 生成，或首次人工 vi 创建。
#
# 前提：本仓库 walkbg 与 kml-agent-service、walkadmin-react 在同一父目录下
# （即当前本地布局：walkbg/、kml-agent-service/、walkadmin-react/ 互为兄弟）。
#
# 用法：
#   WALK_SERVER=ubuntu@1.2.3.4 ./deploy.sh          # 同步代码并重建启动
#   WALK_SERVER=ubuntu@1.2.3.4 ./deploy.sh --logs   # 部署后跟随日志
set -euo pipefail

SERVER="${WALK_SERVER:?需设置 WALK_SERVER=user@host，如 export WALK_SERVER=ubuntu@1.2.3.4}"
REMOTE_DIR="${WALK_REMOTE_DIR:-/home/ubuntu/walk}"

# 脚本所在目录的上一级即 walkbg 仓库根；再上一级即各兄弟仓库的共同父目录。
SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
WALKBG_REPO="$(cd "$SCRIPT_DIR/.." && pwd)"
SIBLING_ROOT="$(cd "$WALKBG_REPO/.." && pwd)"

# 校验兄弟仓库齐备：早失败优于在服务器上才发现缺上下文。
for sib in kml-agent-service walkadmin-react; do
    [ -d "$SIBLING_ROOT/$sib" ] || { echo "错误: 缺少兄弟仓库 $SIBLING_ROOT/$sib"; exit 1; }
done
[ -f "$WALKBG_REPO/Dockerfile" ] || { echo "错误: 缺少 walkbg/Dockerfile"; exit 1; }

# 服务器上必须有 .env（CI 写入或人工创建），否则 compose 里的必填变量会导致启动失败。
# 这里提前检查并给出明确提示，而不是等到 compose 报错。
if ! ssh "$SERVER" "test -f $REMOTE_DIR/deploy/.env"; then
    echo "错误: 服务器上缺少 $REMOTE_DIR/deploy/.env"
    echo "首次需人工创建："
    echo "  ssh $SERVER 'cp $REMOTE_DIR/deploy/.env.example $REMOTE_DIR/deploy/.env && vi $REMOTE_DIR/deploy/.env'"
    echo "之后由 CI 从 GitHub Secrets 生成，本地不再维护。"
    exit 1
fi

echo "==> 目标服务器: $SERVER"
echo "==> 远端目录:   $REMOTE_DIR"

echo "==> 同步代码到服务器"
# 排除本地构建产物与虚拟环境：这些是 macOS/arm64 产物，同步过去无用且会拖慢传输、干扰容器内构建。
# --delete 保证服务器上的目录与本地一致，避免残留旧文件。
# --exclude '/deploy' 防止把 walkbg 仓库内的 deploy/ 嵌套同步上去：
#   deploy/ 由 $SCRIPT_DIR 作为独立平级目录单独传输，避免出现 walkbg/deploy/ 与 deploy/ 两份。
# --exclude '.env' 保护服务器上已有的 deploy/.env 不被本地（本地无此文件）删除。
rsync -az --delete \
    --exclude '.git' \
    --exclude 'node_modules' \
    --exclude 'dist' \
    --exclude 'target' \
    --exclude '.venv' \
    --exclude '__pycache__' \
    --exclude '*.log' \
    --exclude 'logs' \
    --exclude '.DS_Store' \
    --exclude '/deploy' \
    --exclude '.env' \
    "$WALKBG_REPO" \
    "$SIBLING_ROOT/kml-agent-service" \
    "$SIBLING_ROOT/walkadmin-react" \
    "$SCRIPT_DIR" \
    "$SERVER:$REMOTE_DIR/"

echo "==> 在服务器上构建并启动"
# --remove-orphans 清理已从编排中删除的旧容器
ssh "$SERVER" "cd $REMOTE_DIR/deploy && docker compose build && docker compose up -d --remove-orphans"

echo "==> 当前状态"
ssh "$SERVER" "cd $REMOTE_DIR/deploy && docker compose ps"

if [[ "${1:-}" == "--logs" ]]; then
    echo "==> 跟随日志（Ctrl-C 退出）"
    ssh "$SERVER" "cd $REMOTE_DIR/deploy && docker compose logs -f --tail=50"
fi

echo "==> 完成"
