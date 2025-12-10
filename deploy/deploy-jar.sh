#!/bin/bash

# HeyTrip Supplier Integration JAR包一键部署脚本
# 用于直接部署已构建的JAR包到服务器
# 作者: Pax
# 版本: 1.0.0
# 
# 支持的容器编排工具:
#   - Docker Compose v2 (docker compose)
#   - Docker Compose v1 (docker-compose)
#   - Podman Compose (podman-compose)

set -euo pipefail

# 全局变量：将在 check_dependencies 中设置
COMPOSE_CMD=""           # 实际使用的 compose 命令
COMPOSE_TYPE=""          # 编排工具类型描述
SUPPORTS_PROFILE=true    # 是否支持 --profile 参数（podman-compose 不支持）

# 颜色定义
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
BLUE='\033[0;34m'
NC='\033[0m' # No Color

log_info() { echo -e "${BLUE}[INFO]${NC} $1"; }
log_success() { echo -e "${GREEN}[SUCCESS]${NC} $1"; }
log_warning() { echo -e "${YELLOW}[WARNING]${NC} $1"; }
log_error() { echo -e "${RED}[ERROR]${NC} $1"; }

show_help() {
    cat <<'EOF'
HeyTrip Supplier Integration 一键部署脚本

用法:
  deploy-jar.sh <jar文件路径> [选项]

参数:
  jar文件路径    要部署的JAR包文件路径（--type web时可以为空）

选项:
  --port PORT           指定后端应用端口 (默认: 19090)
  --web-port PORT       指定前端Web端口 (默认: 9091)
  --tag TAG             指定Docker镜像标签 (默认: 自动生成)
  --type TYPE           部署类型: backend|web|full (默认: backend)
                        backend: 仅部署后端JAR包
                        web: 仅部署前端Web
                        full: 部署前后端完整应用
  --compose TOOL        指定编排工具: dockerV1|dockerV2|podman
                        不指定则自动检测 (优先级: dockerV2 > dockerV1 > podman)
  --clean               清理旧的容器和镜像
  --help                显示此帮助信息

示例:
  # 部署后端JAR包（默认）
  ./deploy-jar.sh target/heytrip-supplier-1.0.0-SNAPSHOT.jar

  # 部署前端Web
  ./deploy-jar.sh dummy.jar --type web --web-port 8080

  # 部署完整应用（前后端）
  ./deploy-jar.sh target/heytrip-supplier-1.0.0-SNAPSHOT.jar --type full --clean

  # 自定义端口和标签
  ./deploy-jar.sh app.jar --port 8080 --web-port 3001 --tag v1.0.0 --type full

  # 强制使用 podman-compose
  ./deploy-jar.sh app.jar --compose podman

  # 强制使用 Docker Compose v2
  ./deploy-jar.sh app.jar --compose dockerV2

  # 强制使用 Docker Compose v1
  ./deploy-jar.sh app.jar --compose dockerV1
EOF
}

check_dependencies() {
    local manual_tool="${1:-}"  # 手动指定的编排工具（可选）
    
    log_info "检查系统依赖..."

    # 如果手动指定了编排工具
    if [[ -n "$manual_tool" ]]; then
        case "$manual_tool" in
            dockerV2)
                if docker compose version &> /dev/null; then
                    COMPOSE_CMD="docker compose"
                    COMPOSE_TYPE="Docker Compose v2"
                    SUPPORTS_PROFILE=true
                else
                    log_error "Docker Compose v2 不可用，请检查 Docker 安装"
                    exit 1
                fi
                ;;
            dockerV1)
                if command -v docker-compose &> /dev/null; then
                    COMPOSE_CMD="docker-compose"
                    COMPOSE_TYPE="Docker Compose v1"
                    SUPPORTS_PROFILE=true
                else
                    log_error "docker-compose 未安装，安装: pip install docker-compose"
                    exit 1
                fi
                ;;
            podman)
                if command -v podman-compose &> /dev/null; then
                    COMPOSE_CMD="podman-compose"
                    COMPOSE_TYPE="Podman Compose"
                    SUPPORTS_PROFILE=false  # podman-compose 不支持 --profile
                else
                    log_error "podman-compose 未安装，安装: pip install podman-compose"
                    exit 1
                fi
                ;;
            *)
                log_error "未知的编排工具: $manual_tool"
                echo "支持的选项: dockerV1, dockerV2, podman"
                exit 1
                ;;
        esac
        log_info "手动指定容器编排工具: ${COMPOSE_TYPE}"
    else
        # 自动检测（优先级: docker compose > docker-compose > podman-compose）
        if docker compose version &> /dev/null; then
            COMPOSE_CMD="docker compose"
            COMPOSE_TYPE="Docker Compose v2"
            SUPPORTS_PROFILE=true
        elif command -v docker-compose &> /dev/null; then
            COMPOSE_CMD="docker-compose"
            COMPOSE_TYPE="Docker Compose v1"
            SUPPORTS_PROFILE=true
        elif command -v podman-compose &> /dev/null; then
            COMPOSE_CMD="podman-compose"
            COMPOSE_TYPE="Podman Compose"
            SUPPORTS_PROFILE=false  # podman-compose 不支持 --profile
        else
            log_error "Compose 未安装，请先安装以下任一工具:"
            echo "  - Docker Compose v2: 内置于 Docker Desktop"
            echo "  - Docker Compose v1: pip install docker-compose"
            echo "  - Podman Compose: pip install podman-compose"
            exit 1
        fi
        log_info "自动检测到容器编排工具: ${COMPOSE_TYPE}"
    fi

    log_info "使用命令: ${COMPOSE_CMD}"
    if [[ "$SUPPORTS_PROFILE" == "false" ]]; then
        log_warning "当前编排工具不支持 --profile 参数，将使用服务名直接部署"
    fi

    if ! command -v curl &> /dev/null; then
        log_error "缺少依赖: curl"
        exit 1
    fi

    if ! command -v nc &> /dev/null; then
        log_error "缺少依赖: netcat (nc)"
        exit 1
    fi

    # 检查容器运行时（Docker 或 Podman）
    if command -v docker &> /dev/null; then
        if ! docker info &> /dev/null; then
            log_error "Docker 服务未启动，请启动 Docker 服务"
            exit 1
        fi
    elif command -v podman &> /dev/null; then
        log_info "使用 Podman 作为容器运行时"
    else
        log_error "未找到容器运行时（Docker 或 Podman），请先安装"
        exit 1
    fi

    log_success "系统依赖检查通过"
}

extract_version_from_jar() {
    local jar_file="$1"
    local jar_name
    jar_name=$(basename "$jar_file" .jar)

    if [[ "$jar_name" =~ -([0-9]+\.[0-9]+\.[0-9]+.*) ]]; then
        echo "${BASH_REMATCH[1]}"
    else
        local pom_version
        pom_version=$(grep -o '<version>[^<]*</version>' ../pom.xml | head -1 | sed 's/<version>\|<\/version>//g')
        echo "${pom_version:-1.0.0-SNAPSHOT}"
    fi
}

generate_image_tag() {
    local jar_file="$1"
    local custom_tag="$2"

    if [[ -n "$custom_tag" ]]; then
        echo "$custom_tag"
    else
        local version timestamp
        version=$(extract_version_from_jar "$jar_file")
        timestamp=$(date +"%Y%m%d-%H%M%S")
        echo "${version}-${timestamp}"
    fi
}

check_jar_file() {
    local jar_file="$1"

    if [[ -z "$jar_file" ]]; then
        log_error "请指定JAR文件路径"
        show_help
        exit 1
    fi

    if [[ ! -f "$jar_file" ]]; then
        log_error "JAR文件不存在: $jar_file"
        exit 1
    fi

    log_success "JAR文件检查通过: $jar_file"
}

prepare_environment() {
    local jar_file="$1"

    log_info "准备部署环境..."
    mkdir -p logs/app
    export JAR_FILE_PATH="$jar_file"
    log_success "环境准备完成"
}

clean_old_deployment() {
    log_info "清理旧的部署..."
    $COMPOSE_CMD -f docker-compose.yml down --remove-orphans 2>/dev/null || true

    local image_list
    image_list=$(docker images --format "{{.Repository}}:{{.Tag}}" | grep "^heytrip" || true)
    if [[ -n $image_list ]]; then
        echo "$image_list" | xargs -r docker rmi 2>/dev/null || true
    fi

    docker image prune -f 2>/dev/null || true
    log_success "清理完成"
}

deploy_service() {
    local port="$1"
    local jar_file="$2"
    local image_tag="$3"
    local deploy_type="${4:-backend}"

    log_info "构建后端应用镜像..."

    local absolute_jar_path project_root relative_jar_path project_version
    absolute_jar_path=$(cd "$(dirname "$jar_file")" && pwd)/$(basename "$jar_file")
    project_root=$(pwd)

    if [[ "$absolute_jar_path" == "$project_root"/* ]]; then
        relative_jar_path="${absolute_jar_path#$project_root/}"
    else
        log_info "JAR文件在项目外，复制到项目根目录..."
        cp "$jar_file" "$project_root/app.jar"
        relative_jar_path="app.jar"
    fi

    project_version=$(extract_version_from_jar "$jar_file")

    log_info "使用JAR文件路径: $relative_jar_path"
    log_info "项目版本: $project_version"
    log_info "镜像标签: $image_tag"

    export HOST_PORT="$port"
    export JAR_FILE_PATH="$relative_jar_path"
    export IMAGE_TAG="$image_tag"
    export PROJECT_VERSION="$project_version"

    if [[ "$SUPPORTS_PROFILE" == "true" ]]; then
        $COMPOSE_CMD -f docker-compose.yml --profile backend build --no-cache heytrip-supplier
    else
        # podman-compose 不支持 --profile，直接指定服务名
        $COMPOSE_CMD -f docker-compose.yml build --no-cache heytrip-supplier
    fi

    log_info "启动后端服务..."
    if [[ "$SUPPORTS_PROFILE" == "true" ]]; then
        $COMPOSE_CMD -f docker-compose.yml --profile backend up -d heytrip-supplier --force-recreate
    else
        $COMPOSE_CMD -f docker-compose.yml up -d heytrip-supplier --force-recreate
    fi
    log_success "后端服务启动完成"
}

deploy_web_service() {
    local web_port="$1"
    local image_tag="$2"

    log_info "构建前端Web镜像..."

    export WEB_HOST_PORT="$web_port"
    export WEB_IMAGE_TAG="$image_tag"
    export WEB_VERSION="1.0.0"

    # 检查web目录是否存在
    if [[ ! -d "./web" ]]; then
        log_error "未找到 web 目录，请确保前端代码已同步到服务器"
        exit 1
    fi

    if [[ "$SUPPORTS_PROFILE" == "true" ]]; then
        $COMPOSE_CMD -f docker-compose.yml --profile web build --no-cache heytrip-web
    else
        $COMPOSE_CMD -f docker-compose.yml build --no-cache heytrip-web
    fi

    log_info "启动前端Web服务..."
    if [[ "$SUPPORTS_PROFILE" == "true" ]]; then
        $COMPOSE_CMD -f docker-compose.yml --profile web up -d heytrip-web --force-recreate
    else
        $COMPOSE_CMD -f docker-compose.yml up -d heytrip-web --force-recreate
    fi
    log_success "前端Web服务启动完成"
}

deploy_full_service() {
    local port="$1"
    local web_port="$2"
    local jar_file="$3"
    local image_tag="$4"

    log_info "构建完整应用镜像（前后端）..."

    local absolute_jar_path project_root relative_jar_path project_version
    absolute_jar_path=$(cd "$(dirname "$jar_file")" && pwd)/$(basename "$jar_file")
    project_root=$(pwd)

    if [[ "$absolute_jar_path" == "$project_root"/* ]]; then
        relative_jar_path="${absolute_jar_path#$project_root/}"
    else
        log_info "JAR文件在项目外，复制到项目根目录..."
        cp "$jar_file" "$project_root/app.jar"
        relative_jar_path="app.jar"
    fi

    project_version=$(extract_version_from_jar "$jar_file")

    log_info "使用JAR文件路径: $relative_jar_path"
    log_info "项目版本: $project_version"
    log_info "镜像标签: $image_tag"

    export HOST_PORT="$port"
    export WEB_HOST_PORT="$web_port"
    export JAR_FILE_PATH="$relative_jar_path"
    export IMAGE_TAG="$image_tag"
    export PROJECT_VERSION="$project_version"
    export WEB_IMAGE_TAG="web-$image_tag"
    export WEB_VERSION="1.0.0"

    # 检查web目录是否存在
    if [[ ! -d "./web" ]]; then
        log_error "未找到 web 目录，请确保前端代码已同步到服务器"
        exit 1
    fi

    if [[ "$SUPPORTS_PROFILE" == "true" ]]; then
        $COMPOSE_CMD -f docker-compose.yml --profile full build --no-cache
    else
        # podman-compose: 构建所有服务
        $COMPOSE_CMD -f docker-compose.yml build --no-cache
    fi

    log_info "启动完整应用服务..."
    if [[ "$SUPPORTS_PROFILE" == "true" ]]; then
        $COMPOSE_CMD -f docker-compose.yml --profile full up -d --force-recreate
    else
        # podman-compose: 启动所有服务
        $COMPOSE_CMD -f docker-compose.yml up -d --force-recreate
    fi
    log_success "完整应用服务启动完成"
}

check_service_status() {
    local port="$1"

    log_info "检查服务状态..."
    sleep 5

    local container_running=false
    local attempts=0
    local max_attempts=30

    while [[ $attempts -lt $max_attempts ]]; do
        if docker ps --filter "name=heytrip-supplier" --filter "status=running" --format "{{.Names}}" | grep -q "heytrip-supplier"; then
            container_running=true
            break
        fi

        if docker ps -a --filter "name=heytrip-supplier" --format "{{.Names}}" | grep -q "heytrip-supplier"; then
            local status
            status=$(docker ps -a --filter "name=heytrip-supplier" --format "{{.Status}}")
            log_info "容器状态: $status"
            if [[ "$status" == *"Exited"* ]]; then
                log_error "容器已退出，查看日志:"
                docker logs heytrip-supplier --tail=20
                exit 1
            fi
        fi

        attempts=$((attempts + 1))
        sleep 2
        echo -n "."
    done
    echo

    if [[ "$container_running" = false ]]; then
        log_error "容器启动超时"
        $COMPOSE_CMD -f docker-compose.yml logs heytrip-supplier
        exit 1
    fi

    log_success "容器已成功启动"

    log_info "等待应用启动完成..."
    local health_check_passed=false

    for i in {1..60}; do
        if ! docker ps --filter "name=heytrip-supplier" --filter "status=running" --format "{{.Names}}" | grep -q "heytrip-supplier"; then
            log_error "容器已停止运行，查看日志:"
            docker logs heytrip-supplier --tail=20 2>/dev/null || $COMPOSE_CMD -f docker-compose.yml logs --tail=20 heytrip-supplier
            exit 1
        fi

        if nc -z localhost "$port" 2>/dev/null; then
            log_info "端口 $port 已可访问"

            local health_response
            health_response=$(curl -s -w "%{http_code}" "http://localhost:$port/actuator/health" -o /dev/null 2>/dev/null)
            if [[ "$health_response" == "200" ]]; then
                log_success "应用健康检查通过"
                health_check_passed=true
                break
            elif [[ $i -gt 30 ]]; then
                local basic_response
                basic_response=$(curl -s -w "%{http_code}" "http://localhost:$port/" -o /dev/null 2>/dev/null)
                if [[ "$basic_response" != "000" ]]; then
                    log_warning "健康检查端点不可用(HTTP $health_response)，但应用端口已启动(HTTP $basic_response)"
                    health_check_passed=true
                    break
                fi
            fi
        fi

        if [[ $((i % 5)) -eq 0 ]]; then
            echo -n " [$i/60] "
        else
            echo -n "."
        fi
        sleep 2
    done
    echo

    if [[ "$health_check_passed" = false ]]; then
        log_error "应用启动超时，查看最近日志:"
        $COMPOSE_CMD -f docker-compose.yml logs --tail=50 heytrip-supplier
        exit 1
    fi
}

check_web_service_status() {
    local web_port="$1"

    log_info "检查前端Web服务状态..."
    sleep 5

    local container_running=false
    local attempts=0
    local max_attempts=30

    while [[ $attempts -lt $max_attempts ]]; do
        if docker ps --filter "name=heytrip-web" --filter "status=running" --format "{{.Names}}" | grep -q "heytrip-web"; then
            container_running=true
            break
        fi

        if docker ps -a --filter "name=heytrip-web" --format "{{.Names}}" | grep -q "heytrip-web"; then
            local status
            status=$(docker ps -a --filter "name=heytrip-web" --format "{{.Status}}")
            log_info "Web容器状态: $status"
            if [[ "$status" == *"Exited"* ]]; then
                log_error "Web容器已退出，查看日志:"
                docker logs heytrip-web --tail=20
                exit 1
            fi
        fi

        attempts=$((attempts + 1))
        sleep 2
        echo -n "."
    done
    echo

    if [[ "$container_running" = false ]]; then
        log_error "Web容器启动超时"
        $COMPOSE_CMD -f docker-compose.yml logs heytrip-web
        exit 1
    fi

    log_success "Web容器已成功启动"

    log_info "等待Web应用启动完成..."
    local health_check_passed=false

    for i in {1..30}; do
        if ! docker ps --filter "name=heytrip-web" --filter "status=running" --format "{{.Names}}" | grep -q "heytrip-web"; then
            log_error "Web容器已停止运行，查看日志:"
            docker logs heytrip-web --tail=20 2>/dev/null || $COMPOSE_CMD -f docker-compose.yml logs --tail=20 heytrip-web
            exit 1
        fi

        if nc -z localhost "$web_port" 2>/dev/null; then
            log_info "Web端口 $web_port 已可访问"

            local health_response
            health_response=$(curl -s -w "%{http_code}" "http://localhost:$web_port/" -o /dev/null 2>/dev/null)
            if [[ "$health_response" == "200" ]]; then
                log_success "Web应用健康检查通过"
                health_check_passed=true
                break
            fi
        fi

        if [[ $((i % 5)) -eq 0 ]]; then
            echo -n " [$i/30] "
        else
            echo -n "."
        fi
        sleep 2
    done
    echo

    if [[ "$health_check_passed" = false ]]; then
        log_error "Web应用启动超时，查看最近日志:"
        $COMPOSE_CMD -f docker-compose.yml logs --tail=50 heytrip-web
        exit 1
    fi
}

check_full_service_status() {
    local port="$1"
    local web_port="$2"

    log_info "检查完整应用服务状态..."
    
    # 检查后端服务
    log_info "检查后端服务..."
    check_service_status "$port"
    
    # 检查前端服务
    log_info "检查前端服务..."
    check_web_service_status "$web_port"
    
    log_success "完整应用服务检查通过"
}

show_deployment_info() {
    local port="$1"
    local image_tag="$2"
    local deploy_type="${3:-backend}"
    local web_port="${4:-}"

    log_info "部署信息:"
    echo "=================================="
    
    case "$deploy_type" in
        "backend")
            echo "后端应用服务: http://localhost:$port"
            echo "健康检查: http://localhost:$port/actuator/health"
            echo "API文档: http://localhost:$port/swagger-ui.html"
            echo "后端镜像: heytrip/supplier:$image_tag"
            ;;
        "web")
            echo "前端Web应用: http://localhost:$port"
            echo "前端镜像: heytrip/web:$image_tag"
            ;;
        "full")
            echo "前端Web应用: http://localhost:$web_port"
            echo "后端API服务: http://localhost:$port"
            echo "健康检查: http://localhost:$port/actuator/health"
            echo "API文档: http://localhost:$port/swagger-ui.html"
            echo "后端镜像: heytrip/supplier:$image_tag"
            echo "前端镜像: heytrip/web:web-$image_tag"
            echo ""
            echo "提示: 前端应用已配置API代理，可直接通过前端地址访问后端API"
            ;;
    esac
    
    echo "=================================="

    log_info "Docker镜像列表:"
    case "$deploy_type" in
        "backend")
            docker images --filter "reference=heytrip/supplier*" --format "table {{.Repository}}\t{{.Tag}}\t{{.ID}}\t{{.CreatedSince}}\t{{.Size}}"
            ;;
        "web")
            docker images --filter "reference=heytrip/web*" --format "table {{.Repository}}\t{{.Tag}}\t{{.ID}}\t{{.CreatedSince}}\t{{.Size}}"
            ;;
        "full")
            docker images --filter "reference=heytrip/*" --format "table {{.Repository}}\t{{.Tag}}\t{{.ID}}\t{{.CreatedSince}}\t{{.Size}}"
            ;;
    esac

    echo
    log_info "管理命令:"
    
    if [[ "$SUPPORTS_PROFILE" == "true" ]]; then
        # Docker Compose v1/v2 支持 --profile
        case "$deploy_type" in
            "backend")
                echo "$COMPOSE_CMD -f docker-compose.yml --profile backend logs -f  # 查看后端日志"
                echo "$COMPOSE_CMD -f docker-compose.yml --profile backend stop     # 停止后端服务"
                echo "$COMPOSE_CMD -f docker-compose.yml --profile backend start    # 启动后端服务"
                echo "$COMPOSE_CMD -f docker-compose.yml --profile backend down     # 停止并删除后端容器"
                ;;
            "web")
                echo "$COMPOSE_CMD -f docker-compose.yml --profile web logs -f      # 查看前端日志"
                echo "$COMPOSE_CMD -f docker-compose.yml --profile web stop         # 停止前端服务"
                echo "$COMPOSE_CMD -f docker-compose.yml --profile web start        # 启动前端服务"
                echo "$COMPOSE_CMD -f docker-compose.yml --profile web down         # 停止并删除前端容器"
                ;;
            "full")
                echo "$COMPOSE_CMD -f docker-compose.yml --profile full logs -f     # 查看完整应用日志"
                echo "$COMPOSE_CMD -f docker-compose.yml --profile full stop        # 停止完整应用服务"
                echo "$COMPOSE_CMD -f docker-compose.yml --profile full start       # 启动完整应用服务"
                echo "$COMPOSE_CMD -f docker-compose.yml --profile full down        # 停止并删除完整应用容器"
                ;;
        esac
    else
        # podman-compose 不支持 --profile，使用服务名
        case "$deploy_type" in
            "backend")
                echo "$COMPOSE_CMD -f docker-compose.yml logs -f heytrip-supplier     # 查看后端日志"
                echo "$COMPOSE_CMD -f docker-compose.yml stop heytrip-supplier        # 停止后端服务"
                echo "$COMPOSE_CMD -f docker-compose.yml start heytrip-supplier       # 启动后端服务"
                echo "$COMPOSE_CMD -f docker-compose.yml down                         # 停止并删除容器"
                ;;
            "web")
                echo "$COMPOSE_CMD -f docker-compose.yml logs -f heytrip-web          # 查看前端日志"
                echo "$COMPOSE_CMD -f docker-compose.yml stop heytrip-web             # 停止前端服务"
                echo "$COMPOSE_CMD -f docker-compose.yml start heytrip-web            # 启动前端服务"
                echo "$COMPOSE_CMD -f docker-compose.yml down                         # 停止并删除容器"
                ;;
            "full")
                echo "$COMPOSE_CMD -f docker-compose.yml logs -f                      # 查看所有日志"
                echo "$COMPOSE_CMD -f docker-compose.yml stop                         # 停止所有服务"
                echo "$COMPOSE_CMD -f docker-compose.yml start                        # 启动所有服务"
                echo "$COMPOSE_CMD -f docker-compose.yml down                         # 停止并删除所有容器"
                ;;
        esac
    fi
}

main() {
    echo "========================================"
    echo "HeyTrip Supplier Integration 一键部署"
    echo "========================================"

    local JAR_FILE=""
    local PORT=19090
    local WEB_PORT=9091
    local CLEAN=false
    local IMAGE_TAG=""
    local DEPLOY_TYPE="backend"
    local COMPOSE_TOOL=""  # 手动指定的编排工具: dockerV1|dockerV2|podman

    while [[ $# -gt 0 ]]; do
        case "$1" in
            --port)
                PORT="$2"
                shift 2
                ;;
            --web-port)
                WEB_PORT="$2"
                shift 2
                ;;
            --tag)
                IMAGE_TAG="$2"
                shift 2
                ;;
            --type)
                DEPLOY_TYPE="$2"
                shift 2
                ;;
            --compose)
                COMPOSE_TOOL="$2"
                shift 2
                ;;
            --clean)
                CLEAN=true
                shift
                ;;
            --help|-h)
                show_help
                exit 0
                ;;
            -*)
                log_error "未知选项: $1"
                show_help
                exit 1
                ;;
            *)
                if [[ -z "$JAR_FILE" ]]; then
                    JAR_FILE="$1"
                else
                    log_error "只能指定一个JAR文件"
                    show_help
                    exit 1
                fi
                shift
                ;;
        esac
    done

    # 验证部署类型
    if [[ ! "$DEPLOY_TYPE" =~ ^(backend|web|full)$ ]]; then
        log_error "--type 必须为 backend, web 或 full"
        show_help
        exit 1
    fi

    # JAR文件检查（仅后端部署需要）
    if [[ "$DEPLOY_TYPE" == "backend" || "$DEPLOY_TYPE" == "full" ]]; then
        if [[ -z "$JAR_FILE" ]]; then
            log_error "后端部署需要指定JAR文件路径"
            show_help
            exit 1
        fi
        check_jar_file "$JAR_FILE"
    fi

    if [[ -z "$IMAGE_TAG" ]]; then
        if [[ "$DEPLOY_TYPE" == "web" ]]; then
            IMAGE_TAG="web-$(date +"%Y%m%d-%H%M%S")"
        else
            IMAGE_TAG=$(generate_image_tag "$JAR_FILE" "")
        fi
    fi

    check_dependencies "$COMPOSE_TOOL"

    if [[ "$CLEAN" = true ]]; then
        clean_old_deployment
    fi

    # 根据部署类型执行不同的部署流程
    case "$DEPLOY_TYPE" in
        "backend")
            log_info "开始部署后端应用..."
            prepare_environment "$JAR_FILE"
            deploy_service "$PORT" "$JAR_FILE" "$IMAGE_TAG" "backend"
            check_service_status "$PORT"
            show_deployment_info "$PORT" "$IMAGE_TAG" "backend"
            log_success "后端应用部署完成！镜像标签: $IMAGE_TAG"
            ;;
        "web")
            log_info "开始部署前端Web应用..."
            deploy_web_service "$WEB_PORT" "$IMAGE_TAG"
            check_web_service_status "$WEB_PORT"
            show_deployment_info "$WEB_PORT" "$IMAGE_TAG" "web"
            log_success "前端Web应用部署完成！镜像标签: $IMAGE_TAG"
            ;;
        "full")
            log_info "开始部署完整应用（前后端）..."
            prepare_environment "$JAR_FILE"
            deploy_full_service "$PORT" "$WEB_PORT" "$JAR_FILE" "$IMAGE_TAG"
            check_full_service_status "$PORT" "$WEB_PORT"
            show_deployment_info "$PORT" "$IMAGE_TAG" "full" "$WEB_PORT"
            log_success "完整应用部署完成！后端: $IMAGE_TAG"
            ;;
    esac
}

trap 'log_error "部署过程中发生错误，请检查日志"; exit 1' ERR

main "$@"
