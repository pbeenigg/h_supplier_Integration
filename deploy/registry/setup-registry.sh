#!/bin/bash

# 一键初始化并启动安全 Docker Registry
# 功能：
# 1. 创建所需目录结构 (auth/certs/data)
# 2. 基于传入的用户名/密码生成 htpasswd 文件
# 3. 使用 openssl 生成自签名证书 (domain.crt / domain.key)
# 4. 通过 docker compose 启动 registry 服务
#
# 先决条件：Docker、Docker Compose、OpenSSL 可用（若本机未安装 htpasswd，将自动调用 httpd:2.4 镜像生成）

set -euo pipefail

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
PROJECT_ROOT="$(cd "${SCRIPT_DIR}/../.." && pwd)"
COMPOSE_FILE="${SCRIPT_DIR}/docker-compose.yml"
AUTH_DIR="${SCRIPT_DIR}/auth"
CERTS_DIR="${SCRIPT_DIR}/certs"
DATA_DIR="${SCRIPT_DIR}/data"
ENV_FILE="${SCRIPT_DIR}/.env"

usage() {
  cat <<'EOF'
用法：
  setup-registry.sh --domain <域名或IP> --user <用户名> --password <密码> [--days <证书有效天数>]

示例：
  ./setup-registry.sh --domain registry.local --user admin --password S3cret!

参数说明：
  --domain     自签证书的 Common Name（CN），同时用于访问 Registry
  --user       Registry 基本认证用户名
  --password   Registry 基本认证密码
  --days       证书有效天数，默认 365
  --help       查看帮助
EOF
}

# 默认值
CERT_DAYS=365
DOMAIN=""
REGISTRY_USER=""
REGISTRY_PASSWORD=""

# 参数解析
while [[ $# -gt 0 ]]; do
  case "$1" in
    --domain)
      DOMAIN="$2"; shift 2;;
    --user)
      REGISTRY_USER="$2"; shift 2;;
    --password)
      REGISTRY_PASSWORD="$2"; shift 2;;
    --days)
      CERT_DAYS="$2"; shift 2;;
    --help|-h)
      usage; exit 0;;
    *)
      echo "[ERROR] 未知参数: $1" >&2
      usage
      exit 1
      ;;
  esac
done

if [[ -z "$DOMAIN" || -z "$REGISTRY_USER" || -z "$REGISTRY_PASSWORD" ]]; then
  echo "[ERROR] --domain、--user、--password 为必填参数" >&2
  usage
  exit 1
fi

# 依赖检测
command -v docker >/dev/null 2>&1 || { echo "[ERROR] 未检测到 docker，请先安装 Docker" >&2; exit 1; }
command -v docker compose >/dev/null 2>&1 || { echo "[ERROR] 未检测到 docker compose 命令，请安装 Docker Compose" >&2; exit 1; }
command -v openssl >/dev/null 2>&1 || { echo "[ERROR] 未检测到 openssl，请安装 OpenSSL" >&2; exit 1; }

mkdir -p "$AUTH_DIR" "$CERTS_DIR" "$DATA_DIR"

echo "[INFO] 写入环境变量配置 (.env)..."
cat > "$ENV_FILE" <<EOF
REGISTRY_DOMAIN=${DOMAIN}
EOF
chmod 600 "$ENV_FILE"

echo "[INFO] 生成 htpasswd 文件..."
if command -v htpasswd >/dev/null 2>&1; then
  htpasswd -Bbc "$AUTH_DIR/htpasswd" "$REGISTRY_USER" "$REGISTRY_PASSWORD"
else
  docker run --rm --entrypoint htpasswd httpd:2.4 -Bbn "$REGISTRY_USER" "$REGISTRY_PASSWORD" > "$AUTH_DIR/htpasswd"
fi
chmod 640 "$AUTH_DIR/htpasswd"

echo "[INFO] 生成自签名证书 (CN=$DOMAIN, 有效期 ${CERT_DAYS} 天, 含 SAN)..."

SAN_ENTRY="DNS:${DOMAIN}"
if [[ "$DOMAIN" =~ ^([0-9]{1,3}\.){3}[0-9]{1,3}$ ]]; then
  SAN_ENTRY="IP:${DOMAIN}"
fi

SSL_CONF=$(mktemp)
cat > "$SSL_CONF" <<EOF
[req]
default_bits = 4096
prompt = no
default_md = sha256
distinguished_name = dn
req_extensions = req_ext

[dn]
CN = ${DOMAIN}

[req_ext]
subjectAltName = ${SAN_ENTRY}
EOF

openssl req \
  -newkey rsa:4096 \
  -nodes \
  -keyout "$CERTS_DIR/domain.key" \
  -x509 \
  -days "$CERT_DAYS" \
  -out "$CERTS_DIR/domain.crt" \
  -config "$SSL_CONF" >/dev/null 2>&1
rm -f "$SSL_CONF"
chmod 640 "$CERTS_DIR"/domain.{key,crt}

echo "[INFO] 证书与认证文件准备完成："
echo "  - htpasswd: $AUTH_DIR/htpasswd"
echo "  - 证书:    $CERTS_DIR/domain.crt"
echo "  - 私钥:    $CERTS_DIR/domain.key"

echo "[INFO] 启动 Docker Registry..."
cd "$SCRIPT_DIR"
docker compose -f "$COMPOSE_FILE" up -d --force-recreate

echo "[SUCCESS] Registry 已启动！"
echo "  Registry API: https://${DOMAIN}/registry"
echo "  Registry UI: https://${DOMAIN}/registry/ui/"
echo "  登录账号: $REGISTRY_USER"
echo "  登录密码: $REGISTRY_PASSWORD"
echo "  数据目录: $DATA_DIR"

echo "[INFO] 若需让 Docker 客户端信任自签证书，请把 $CERTS_DIR/domain.crt 拷贝到目标主机的 /etc/docker/certs.d/${DOMAIN}:5000/ca.crt 后重启 Docker。"
echo "[INFO] 通过网关访问需提供 Basic Auth 凭证： $REGISTRY_USER / <您设置的密码>"
