#!/bin/bash

# HeyTrip Supplier Integration JAR包一键部署脚本
# 用于直接部署已构建的JAR包到服务器
# 作者: Pax
# 版本: 1.0.0

set -euo pipefail

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
HeyTrip Supplier Integration JAR包部署脚本

用法:
  deploy-jar.sh <jar文件路径> [选项]

参数:
  jar文件路径    要部署的JAR包文件路径

选项:
  --port PORT    指定应用端口 (默认: 9090)
  --tag TAG      指定Docker镜像标签 (默认: 自动生成)
  --clean        清理旧的容器和镜像
  --help         显示此帮助信息

示例:
  ./deploy-jar.sh target/heytrip-supplier-1.0.0-SNAPSHOT.jar
  ./deploy-jar.sh app.jar --port 8080 --tag v1.0.0 --clean
EOF
}

check_dependencies() {
    log_info "检查系统依赖..."

    if ! command -v docker &> /dev/null; then
        log_error "Docker 未安装，请先安装 Docker"
        exit 1
    fi

    if ! docker compose version &> /dev/null; then
        if ! command -v docker-compose &> /dev/null; then
            log_error "Docker Compose 未安装，请先安装 Docker Compose (v2 或 v1)"
            exit 1
        fi
    fi

    if ! command -v curl &> /dev/null; then
        log_error "缺少依赖: curl"
        exit 1
    fi

    if ! command -v nc &> /dev/null; then
        log_error "缺少依赖: netcat (nc)"
        exit 1
    fi

    if ! docker info &> /dev/null; then
        log_error "Docker 服务未启动，请启动 Docker 服务"
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
    docker compose -f docker-compose.yml down --remove-orphans 2>/dev/null || true

    local image_list
    image_list=$(docker images --format "{{.Repository}}:{{.Tag}}" | grep "^heytrip/supplier-integration" || true)
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

    log_info "构建应用镜像..."

    local absolute_jar_path project_root relative_jar_path project_version
    absolute_jar_path=$(cd "$(dirname "$jar_file")" && pwd)/$(basename "$jar_file")
    project_root=$(cd .. && pwd)

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

    docker compose -f docker-compose.yml build --no-cache  heytrip-supplier

    log_info "启动服务..."
    docker compose -f docker-compose.yml up -d heytrip-supplier --force-recreate
    log_success "应用服务启动完成"
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
        docker compose -f docker-compose.yml logs heytrip-supplier
        exit 1
    fi

    log_success "容器已成功启动"

    log_info "等待应用启动完成..."
    local health_check_passed=false

    for i in {1..60}; do
        if ! docker ps --filter "name=heytrip-supplier" --filter "status=running" --format "{{.Names}}" | grep -q "heytrip-supplier"; then
            log_error "容器已停止运行，查看日志:"
            docker logs heytrip-supplier --tail=20 2>/dev/null || docker compose -f docker-compose.yml logs --tail=20 heytrip-supplier
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
        docker compose -f docker-compose.yml logs --tail=50 heytrip-supplier
        exit 1
    fi
}

show_deployment_info() {
    local port="$1"
    local image_tag="$2"

    log_info "部署信息:"
    echo "=================================="
    echo "应用服务: http://localhost:$port"
    echo "健康检查: http://localhost:$port/actuator/health"
    echo "API文档: http://localhost:$port/swagger-ui.html"
    echo "镜像信息: heytrip/supplier-integration:$image_tag"
    echo "=================================="

    log_info "Docker镜像列表:"
    docker images --filter "reference=heytrip/supplier-integration*" --format "table {{.Repository}}\t{{.Tag}}\t{{.ID}}\t{{.CreatedSince}}\t{{.Size}}"

    echo
    log_info "管理命令:"
    echo "docker compose -f docker-compose.yml logs -f    # 查看日志"
    echo "docker compose -f docker-compose.yml stop       # 停止服务"
    echo "docker compose -f docker-compose.yml start      # 启动服务"
    echo "docker compose -f docker-compose.yml down       # 停止并删除容器"
}

main() {
    echo "========================================"
    echo "HeyTrip Supplier Integration JAR包部署"
    echo "========================================"

    local JAR_FILE=""
    local PORT=9090
    local CLEAN=false
    local IMAGE_TAG=""

    while [[ $# -gt 0 ]]; do
        case "$1" in
            --port)
                PORT="$2"
                shift 2
                ;;
            --tag)
                IMAGE_TAG="$2"
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

    if [[ -z "$JAR_FILE" ]]; then
        log_error "请指定JAR文件路径"
        show_help
        exit 1
    fi

    if [[ -z "$IMAGE_TAG" ]]; then
        IMAGE_TAG=$(generate_image_tag "$JAR_FILE" "")
    fi

    check_dependencies
    check_jar_file "$JAR_FILE"

    if [[ "$CLEAN" = true ]]; then
        clean_old_deployment
    fi

    prepare_environment "$JAR_FILE"
    deploy_service "$PORT" "$JAR_FILE" "$IMAGE_TAG"
    check_service_status "$PORT"
    show_deployment_info "$PORT" "$IMAGE_TAG"

    log_success "JAR包部署完成！镜像标签: $IMAGE_TAG"
}

trap 'log_error "部署过程中发生错误，请检查日志"; exit 1' ERR

main "$@"
