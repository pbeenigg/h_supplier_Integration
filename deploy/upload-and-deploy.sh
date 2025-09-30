#!/bin/bash

set -euo pipefail

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}" )" && pwd)"
PROJECT_ROOT="$(cd "${SCRIPT_DIR}/.." && pwd)"
TARGET_JAR_NAME="heytrip-supplier-1.0.0-SNAPSHOT.jar"
LOCAL_JAR_PATH="${PROJECT_ROOT}/target/${TARGET_JAR_NAME}"
REMOTE_WORKDIR="/root/pax"  # 服务器上的默认部署目录，可通过参数覆盖
CONFIG_FILE="${SCRIPT_DIR}/upload-and-deploy.env"

usage() {
  cat <<'EOF'
用法：
  upload-and-deploy.sh --host <服务器IP> --user <用户名> [--key <私钥路径> | --password <密码>] \
      [--port <SSH端口>] [--remote-dir <远程目录>] [--extra-ssh "参数"]

提示：可在 deploy/upload-and-deploy.env 中设置默认的 HOST/USER/PORT 等参数。

示例：
  ./upload-and-deploy.sh --host 203.0.113.10 --user deployer --key ~/.ssh/id_rsa \
      --remote-dir /data/heytrip-supplier

  ./upload-and-deploy.sh --key ~/.ssh/id_rsa

  ./upload-and-deploy.sh --password '一次性密码'

入参说明：
  --host         目标服务器 IP 或域名（必填，可在 env 文件中预配置）
  --user         SSH 登录用户名（必填，可在 env 文件中预配置）
  --key          SSH 私钥路径，用于认证（与 --password 二选一）
  --password     SSH 登录密码（与 --key 二选一，脚本会使用 sshpass）
  --port         SSH 端口，默认 22（可在 env 文件中预配置）
  --remote-dir   Jar + 脚本的远程部署目录，默认 /root/pax
  --extra-ssh    追加到 ssh/scp/sftp 命令的参数，例如 "-o ProxyJump=bastion"
  --help         查看帮助
EOF
}

if [[ -f "$CONFIG_FILE" ]]; then
  # shellcheck disable=SC1090
  source "$CONFIG_FILE"
fi

HOST="${UPLOAD_HOST:-}"
SSH_USER="${UPLOAD_USER:-}"
SSH_KEY=""
SSH_PASSWORD="${UPLOAD_PASSWORD:-}"
SSH_PORT="${UPLOAD_PORT:-22}"
REMOTE_DIR="${UPLOAD_REMOTE_DIR:-$REMOTE_WORKDIR}"
EXTRA_SSH_OPTIONS="${UPLOAD_SSH_OPTIONS:-}"

while [[ $# -gt 0 ]]; do
  case "$1" in
    --host)
      HOST="$2"; shift 2;;
    --user)
      SSH_USER="$2"; shift 2;;
    --key)
      SSH_KEY="$2"; shift 2;;
    --password)
      SSH_PASSWORD="$2"; shift 2;;
    --port)
      SSH_PORT="$2"; shift 2;;
    --remote-dir)
      REMOTE_DIR="$2"; shift 2;;
    --extra-ssh)
      EXTRA_SSH_OPTIONS="$2"; shift 2;;
    --help|-h)
      usage; exit 0;;
    *)
      echo "[ERROR] 未识别的参数: $1" >&2
      usage
      exit 1;;
  esac
done

if [[ -z "$HOST" || -z "$SSH_USER" ]]; then
  echo "[ERROR] --host/--user 为必填参数，可在 ${CONFIG_FILE} 中预配置 host/user/port" >&2
  usage
  exit 1
fi

if [[ -z "$SSH_KEY" && -z "$SSH_PASSWORD" ]]; then
  read -rsp "请输入 SSH 密码: " SSH_PASSWORD
  echo
fi

if [[ -n "$SSH_KEY" && -n "$SSH_PASSWORD" ]]; then
  log "检测到同时提供私钥和密码，将优先使用私钥"
  SSH_PASSWORD=""
fi

if [[ -n "$SSH_KEY" && ! -f "$SSH_KEY" ]]; then
  echo "[ERROR] 指定的私钥文件不存在: $SSH_KEY" >&2
  exit 1
fi

log() {
  echo -e "[INFO] $1"
}

log_connection_context() {
  local masked_password="${SSH_PASSWORD:+******}"
  echo "----------------------------------------"
  echo -e "[INFO] date: $(date '+%Y-%m-%d %H:%M:%S')"
  echo -e "[INFO] host: $HOST"
  echo -e "[INFO] user: $SSH_USER"
  echo -e "[INFO] port: $SSH_PORT"
  echo -e "[INFO] remote-dir: $REMOTE_DIR"
  echo -e "[INFO] key: $SSH_KEY"
  echo -e "[INFO] password: $masked_password"
  if [[ -n "$EXTRA_SSH_OPTIONS" ]]; then
    echo -e "[INFO] extra-ssh-options: $EXTRA_SSH_OPTIONS"
  fi
}

die() {
  echo -e "[ERROR] $1" >&2
  exit 1
}

ensure_command() {
  if ! command -v "$1" >/dev/null 2>&1; then
    die "缺少依赖命令: $1"
  fi
}

run_in_project() {
  (cd "$PROJECT_ROOT" && "$@")
}

get_file_size() {
  local file_path="$1"
  if stat -f%z "$file_path" >/dev/null 2>&1; then
    stat -f%z "$file_path"
  else
    stat -c%s "$file_path"
  fi
}

format_bytes() {
  local bytes="$1"
  awk -v b="$bytes" 'BEGIN {
    split("B KB MB GB TB PB", units);
    value = b;
    i = 1;
    while (value >= 1024 && i < 6) {
      value /= 1024;
      i++;
    }
    printf("%.2f %s", value, units[i]);
  }'
}

format_speed() {
  local bytes="$1"
  local seconds="$2"
  awk -v b="$bytes" -v s="$seconds" 'BEGIN {
    if (s <= 0) { s = 1 }
    speed = b / s;
    split("B/s KB/s MB/s GB/s TB/s", units);
    value = speed;
    i = 1;
    while (value >= 1024 && i < 5) {
      value /= 1024;
      i++;
    }
    printf("%.2f %s", value, units[i]);
  }'
}

transfer_with_scp() {
  local source_path="$1"
  local destination="$2"
  local label="$3"
  local start_ts end_ts duration bytes human_size speed

  start_ts=$(date +%s)
  if ! default_scp "$source_path" "$destination"; then
    return 1
  fi
  end_ts=$(date +%s)
  duration=$(( end_ts - start_ts ))
  if (( duration < 1 )); then
    duration=1
  fi

  bytes=$(get_file_size "$source_path")
  human_size=$(format_bytes "$bytes")
  speed=$(format_speed "$bytes" "$duration")

  echo -e "[INFO] 上传完成: ${label} (${human_size}) -> ${destination}，耗时 ${duration}s，平均速率 ${speed}"
  return 0
}

ensure_command mvnd
ensure_command ssh
ensure_command scp
if [[ -n "$SSH_PASSWORD" ]]; then
  if ! command -v sshpass >/dev/null 2>&1; then
    cat <<'EOF' >&2
[ERROR] 缺少依赖命令: sshpass
Mac 用户可通过 Homebrew 安装：
  brew install hudochenkov/sshpass/sshpass
或改用 SSH 私钥方式：
  ./upload-and-deploy.sh --key ~/.ssh/id_rsa
EOF
    exit 1
  fi
fi

SSH_COMMON_OPTS=(-p "$SSH_PORT")
SCP_COMMON_OPTS=(-P "$SSH_PORT")

if [[ -n "$SSH_KEY" ]]; then
  SSH_COMMON_OPTS+=(-i "$SSH_KEY")
  SCP_COMMON_OPTS+=(-i "$SSH_KEY")
fi


default_ssh() {
  local cmd=(ssh "${SSH_COMMON_OPTS[@]}")
  if [[ -n "$EXTRA_SSH_OPTIONS" ]]; then
    # shellcheck disable=SC2206
    local extra_opts=($EXTRA_SSH_OPTIONS)
    cmd+=("${extra_opts[@]}")
  fi
  cmd+=("${SSH_USER}@${HOST}")
  if [[ $# -gt 0 ]]; then
    cmd+=("$@")
  fi
  if [[ -n "$SSH_PASSWORD" ]]; then
    sshpass -p "$SSH_PASSWORD" "${cmd[@]}"
  else
    "${cmd[@]}"
  fi
}

default_scp() {
  local cmd=(scp "${SCP_COMMON_OPTS[@]}")
  if [[ -n "$EXTRA_SSH_OPTIONS" ]]; then
    # shellcheck disable=SC2206
    local extra_opts=($EXTRA_SSH_OPTIONS)
    cmd+=("${extra_opts[@]}")
  fi
  if [[ -n "$SSH_PASSWORD" ]]; then
    cmd+=(-oBatchMode=no)
  fi
  cmd+=("$@")
  if [[ -n "$SSH_PASSWORD" ]]; then
    sshpass -p "$SSH_PASSWORD" "${cmd[@]}"
  else
    "${cmd[@]}"
  fi
}

log "步骤 1/5: Maven 打包项目"
run_in_project mvnd clean package -Dmaven.test.skip=true

if [[ ! -f "$LOCAL_JAR_PATH" ]]; then
  die "未找到 Jar 文件: $LOCAL_JAR_PATH"
fi

TMP_REMOTE_SCRIPT="/tmp/upload-deploy-$RANDOM.sh"
REMOTE_JAR_PATH="${REMOTE_DIR}/deploy/${TARGET_JAR_NAME}"
REMOTE_DEPLOY_SCRIPT="${REMOTE_DIR}/deploy/deploy-jar.sh"

log "步骤 2/5: 通过 SSH 创建远程目录"
log_connection_context
default_ssh "mkdir -p '${REMOTE_DIR}/deploy'"

log "步骤 3/5: 上传 Jar 到服务器 -> ${REMOTE_JAR_PATH}"
if ! transfer_with_scp "$LOCAL_JAR_PATH" "${SSH_USER}@${HOST}:${REMOTE_DIR}/deploy" "$TARGET_JAR_NAME"; then
  die "上传 Jar 失败"
fi

log "步骤 4/5: 同步 deploy 核心文件"
DEFAULT_DEPLOY_DIR="${PROJECT_ROOT}/deploy"
default_ssh "mkdir -p '${REMOTE_DIR}/deploy'"
DEPLOY_FILES=("deploy-jar.sh" "docker-compose.yml" "Dockerfile" "version-manager.sh" ".env")
for file in "${DEPLOY_FILES[@]}"; do
  LOCAL_DEPLOY_FILE="${DEFAULT_DEPLOY_DIR}/${file}"
  if [[ ! -f "$LOCAL_DEPLOY_FILE" ]]; then
    echo -e "[WARN] 本地缺少 deploy/${file}，已跳过"
    continue
  fi
  log "同步 deploy/${file}"
  if ! transfer_with_scp "$LOCAL_DEPLOY_FILE" "${SSH_USER}@${HOST}:${REMOTE_DIR}/deploy/" "deploy/${file}"; then
    die "上传 deploy/${file} 失败"
  fi
done

default_ssh "chmod +x '${REMOTE_DIR}/deploy/deploy-jar.sh'"

default_ssh "cat > '${TMP_REMOTE_SCRIPT}' <<'EOS'
#!/bin/bash
set -e
cd '${REMOTE_DIR}/deploy'
./deploy-jar.sh './${TARGET_JAR_NAME}' --clean
EOS
chmod +x '${TMP_REMOTE_SCRIPT}'"

log "步骤 5/5: 触发远程部署"
default_ssh "${TMP_REMOTE_SCRIPT}"

default_ssh "rm -f '${TMP_REMOTE_SCRIPT}'"

log "部署完成！可登录 ${SSH_USER}@${HOST} 查看服务状态"
