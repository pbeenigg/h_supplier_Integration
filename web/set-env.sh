#!/bin/bash

# 环境配置切换脚本
# 用法: ./set-env.sh [dev|prod]

set -e

ENV_TYPE=${1:-dev}
PROJECT_ROOT="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"

case $ENV_TYPE in
  "dev"|"development")
    echo "🔧 切换到开发环境配置..."
    cp "$PROJECT_ROOT/.env.development" "$PROJECT_ROOT/.env.local"
    echo "✅ 已设置为开发环境"
    echo "   - API_BASE_URL: http://localhost:9090"
    echo "   - PORT: 9091"
    ;;
  "prod"|"production")
    echo "🚀 切换到生产环境配置..."
    cp "$PROJECT_ROOT/.env.production" "$PROJECT_ROOT/.env.local"
    echo "✅ 已设置为生产环境"
    echo "   - API_BASE_URL: http://47.76.191.223:9090"
    echo "   - PORT: 9091"
    ;;
  *)
    echo "❌ 无效的环境类型: $ENV_TYPE"
    echo "用法: $0 [dev|prod]"
    echo "  dev  - 开发环境"
    echo "  prod - 生产环境"
    exit 1
    ;;
esac

echo ""
echo "💡 提示: 重启开发服务器以应用新配置"
echo "   pnpm dev  # 启动开发服务器"
echo "   pnpm prod # 启动生产服务器"