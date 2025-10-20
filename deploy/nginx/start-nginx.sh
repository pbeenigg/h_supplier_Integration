#!/bin/bash

# HeyTrip Nginx Proxy 一键启动脚本
# 功能：
# 1. 检查 Docker / Docker Compose 依赖
# 2. 创建日志目录并设置权限
# 3. 启动独立的 Nginx 反向代理服务（连接 heytrip-supplier_default 网络）

set -euo pipefail

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
COMPOSE_FILE="$SCRIPT_DIR/docker-compose.yml"
LOG_DIR="$SCRIPT_DIR/logs"

usage() {
  cat <<'EOF'
用法：
  start-nginx.sh [--recreate]

选项：
  --recreate   重新创建容器（等同于 docker compose up --force-recreate）
  --help       显示帮助
EOF
}

RECREATE=false
while [[ $# -gt 0 ]]; do
  case "$1" in
    --recreate)
      RECREATE=true; shift ;;
    --help|-h)
      usage; exit 0 ;;
    *)
      echo "[ERROR] 未知参数: $1" >&2
      usage
      exit 1
      ;;
  esac
done

command -v docker >/dev/null 2>&1 || { echo "[ERROR] 未检测到 docker，请先安装 Docker" >&2; exit 1; }
command -v docker compose >/dev/null 2>&1 || { echo "[ERROR] 未检测到 docker compose 命令，请安装 Docker Compose" >&2; exit 1; }

mkdir -p "$LOG_DIR"
chmod 755 "$LOG_DIR"

echo "[INFO] 启动 Nginx 反向代理..."
cd "$SCRIPT_DIR"
if [[ "$RECREATE" = true ]]; then
  docker compose -f "$COMPOSE_FILE" up -d --force-recreate
else
  docker compose -f "$COMPOSE_FILE" up -d
fi

echo "[SUCCESS] Nginx 已启动，可通过 http://47.76.191.223 访问。"
