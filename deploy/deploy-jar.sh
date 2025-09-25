#!/bin/bash

# HeyTrip Supplier Integration JAR包一键部署脚本
# 用于直接部署已构建的JAR包到服务器
# 作者: Pax
# 版本: 1.0.0

set -e

# 颜色定义
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

# 显示使用帮助
show_help() {
    echo "HeyTrip Supplier Integration JAR包部署脚本"
    echo ""
    echo "用法:"
    echo "  $0 <jar文件路径> [选项]"
    echo ""
    echo "参数:"
    echo "  jar文件路径    要部署的JAR包文件路径"
    echo ""
    echo "选项:"
    echo "  --with-nginx   同时启动Nginx反向代理"
    echo "  --port PORT    指定应用端口 (默认: 9090)"
    echo "  --tag TAG      指定Docker镜像标签 (默认: 自动生成)"
    echo "  --clean        清理旧的容器和镜像"
    echo "  --help         显示此帮助信息"
    echo ""
    echo "示例:"
    echo "  $0 target/heytrip-supplier-1.0.0-SNAPSHOT.jar"
    echo "  $0 app.jar --with-nginx --port 9090"
    echo "  $0 app.jar --tag v1.0.0 --clean"
}

# 检查依赖
check_dependencies() {
    log_info "检查系统依赖..."
    
    if ! command -v docker &> /dev/null; then
        log_error "Docker 未安装，请先安装 Docker"
        exit 1
    fi
    
    if ! command -v docker compose &> /dev/null; then
        log_error "Docker Compose 未安装，请先安装 Docker Compose"
        exit 1
    fi
    
    if ! docker info &> /dev/null; then
        log_error "Docker 服务未启动，请启动 Docker 服务"
        exit 1
    fi
    
    log_success "系统依赖检查通过"
}

# 从JAR文件名提取版本号
extract_version_from_jar() {
    local jar_file="$1"
    local jar_name=$(basename "$jar_file" .jar)
    
    # 尝试从文件名提取版本号 (例如: heytrip-supplier-1.0.0-SNAPSHOT.jar)
    if [[ "$jar_name" =~ -([0-9]+\.[0-9]+\.[0-9]+.*) ]]; then
        echo "${BASH_REMATCH[1]}"
    else
        # 从pom.xml提取版本号
        local pom_version=$(grep -o '<version>[^<]*</version>' ../pom.xml | head -1 | sed 's/<version>\|<\/version>//g')
        echo "${pom_version:-1.0.0-SNAPSHOT}"
    fi
}

# 生成镜像标签
generate_image_tag() {
    local jar_file="$1"
    local custom_tag="$2"
    
    if [ -n "$custom_tag" ]; then
        echo "$custom_tag"
    else
        local version=$(extract_version_from_jar "$jar_file")
        local timestamp=$(date +"%Y%m%d-%H%M%S")
        echo "${version}-${timestamp}"
    fi
}

# 检查JAR文件
check_jar_file() {
    local jar_file="$1"
    
    if [ -z "$jar_file" ]; then
        log_error "请指定JAR文件路径"
        show_help
        exit 1
    fi
    
    if [ ! -f "$jar_file" ]; then
        log_error "JAR文件不存在: $jar_file"
        exit 1
    fi
    
    # 检查是否为JAR文件
    if [[ ! "$jar_file" =~ \.jar$ ]]; then
        log_error "指定的文件不是JAR文件: $jar_file"
        exit 1
    fi
    
    log_success "JAR文件检查通过: $jar_file"
}

# 准备部署环境
prepare_environment() {
    local jar_file="$1"
    
    log_info "准备部署环境..."
    
    # 创建必要目录
    mkdir -p logs/app
    mkdir -p logs/nginx
    
    # 设置JAR文件路径环境变量供Docker使用
    export JAR_FILE_PATH="$jar_file"
    
    log_success "环境准备完成"
}

# 清理旧的部署
clean_old_deployment() {
    log_info "清理旧的部署..."
    
    # 停止并删除容器
    docker compose -f docker-compose.yml down --remove-orphans 2>/dev/null || true
    
    # 删除旧镜像（包括所有相关标签）
    docker images --format "table {{.Repository}}:{{.Tag}}" | grep "heytrip/supplier-integration" | awk '{print $1}' | xargs -r docker rmi 2>/dev/null || true
    
    # 清理悬空镜像
    docker image prune -f 2>/dev/null || true
    
    log_success "清理完成"
}

# 构建和启动服务
deploy_service() {
    local with_nginx="$1"
    local port="$2"
    local jar_file="$3"
    local image_tag="$4"
    
    log_info "构建应用镜像..."
    
    # 获取JAR文件的绝对路径
    local absolute_jar_path=$(cd "$(dirname "$jar_file")" && pwd)/$(basename "$jar_file")
    
    # 获取项目根目录的绝对路径（deploy目录的上级目录）
    local project_root=$(cd .. && pwd)
    
    # 计算相对路径
    local relative_jar_path
    if [[ "$absolute_jar_path" == "$project_root"/* ]]; then
        # JAR文件在项目目录内，计算相对路径
        relative_jar_path="${absolute_jar_path#$project_root/}"
    else
        # JAR文件在项目目录外，复制到项目根目录
        log_info "JAR文件在项目外，复制到项目根目录..."
        cp "$jar_file" "$project_root/app.jar"
        relative_jar_path="app.jar"
    fi
    
    # 提取项目版本号
    local project_version=$(extract_version_from_jar "$jar_file")
    
    log_info "使用JAR文件路径: $relative_jar_path"
    log_info "项目版本: $project_version"
    log_info "镜像标签: $image_tag"
    
    # 设置环境变量
    export HOST_PORT="$port"
    export JAR_FILE_PATH="$relative_jar_path"
    export IMAGE_TAG="$image_tag"
    export PROJECT_VERSION="$project_version"
    
    # 构建镜像
    docker compose -f docker-compose.yml build --no-cache heytrip-supplier
    
    log_info "启动服务..."
    
    if [ "$with_nginx" = true ]; then
        # 启动应用和Nginx
        docker compose -f docker-compose.yml --profile nginx up -d
        log_success "应用服务和Nginx代理启动完成"
    else
        # 仅启动应用
        docker compose -f docker-compose.yml up -d heytrip-supplier
        log_success "应用服务启动完成"
    fi
}

# 检查服务状态
check_service_status() {
    local port="$1"
    
    log_info "检查服务状态..."
    
    # 等待容器启动
    sleep 5
    
    # 检查容器是否运行（使用更可靠的检测方法）
    local container_running=false
    local attempts=0
    local max_attempts=30
    
    while [ $attempts -lt $max_attempts ]; do
        # 使用docker ps检查容器是否运行
        if docker ps --filter "name=heytrip-supplier" --filter "status=running" --format "{{.Names}}" | grep -q "heytrip-supplier"; then
            container_running=true
            break
        fi
        
        # 检查容器是否存在但未运行
        if docker ps -a --filter "name=heytrip-supplier" --format "{{.Names}}" | grep -q "heytrip-supplier"; then
            local status=$(docker ps -a --filter "name=heytrip-supplier" --format "{{.Status}}")
            log_info "容器状态: $status"
            
            # 如果容器退出了，显示日志
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
    echo ""
    
    if [ "$container_running" = false ]; then
        log_error "容器启动超时"
        docker compose -f docker-compose.yml logs heytrip-supplier
        exit 1
    fi
    
    log_success "容器已成功启动"
    
    # 检查应用健康状态（更智能的检查）
    log_info "等待应用启动完成..."
    local health_check_passed=false
    
    for i in {1..60}; do
        # 首先检查容器是否还在运行
        if ! docker ps --filter "name=heytrip-supplier" --filter "status=running" --format "{{.Names}}" | grep -q "heytrip-supplier"; then
            log_error "容器已停止运行，查看日志:"
            docker logs heytrip-supplier --tail=20 2>/dev/null || docker compose -f docker-compose.yml logs --tail=20 heytrip-supplier
            exit 1
        fi
        
        # 检查端口是否可访问
        if nc -z localhost "$port" 2>/dev/null; then
            log_info "端口 $port 已可访问"
            
            # 检查健康检查端点
            local health_response=$(curl -s -w "%{http_code}" "http://localhost:$port/actuator/health" -o /dev/null 2>/dev/null)
            if [ "$health_response" = "200" ]; then
                log_success "应用健康检查通过"
                health_check_passed=true
                break
            elif [ $i -gt 30 ]; then
                # 30次后如果端口可访问但健康检查失败，检查基本连通性
                local basic_response=$(curl -s -w "%{http_code}" "http://localhost:$port/" -o /dev/null 2>/dev/null)
                if [ "$basic_response" != "000" ]; then
                    log_warning "健康检查端点不可用(HTTP $health_response)，但应用端口已启动(HTTP $basic_response)"
                    health_check_passed=true
                    break
                fi
            fi
        fi
        
        # 显示进度
        if [ $((i % 5)) -eq 0 ]; then
            echo -n " [$i/60] "
        else
            echo -n "."
        fi
        sleep 2
    done
    echo ""
    
    if [ "$health_check_passed" = false ]; then
        log_error "应用启动超时，查看最近日志:"
        docker compose -f docker-compose.yml logs --tail=50 heytrip-supplier
        exit 1
    fi
}

# 显示部署信息
show_deployment_info() {
    local with_nginx="$1"
    local port="$2"
    local image_tag="$3"
    
    log_info "部署信息:"
    echo "=================================="
    echo "应用服务: http://localhost:$port"
    echo "健康检查: http://localhost:$port/actuator/health"
    echo "API文档: http://localhost:$port/swagger-ui.html"
    echo "镜像信息: heytrip/supplier-integration:$image_tag"
    
    if [ "$with_nginx" = true ]; then
        echo "Nginx代理: http://localhost:80"
    fi
    
    echo "=================================="
    
    log_info "Docker镜像列表:"
    docker images --filter "reference=heytrip/supplier-integration*" --format "table {{.Repository}}\t{{.Tag}}\t{{.ID}}\t{{.CreatedSince}}\t{{.Size}}"
    
    echo ""
    log_info "管理命令:"
    echo "docker compose -f docker-compose.yml logs -f    # 查看日志"
    echo "docker compose -f docker-compose.yml stop       # 停止服务"
    echo "docker compose -f docker-compose.yml start      # 启动服务"
    echo "docker compose -f docker-compose.yml down       # 停止并删除容器"
}

# 主函数
main() {
    echo "========================================"
    echo "HeyTrip Supplier Integration JAR包部署"
    echo "========================================"
    
    # 解析命令行参数
    JAR_FILE=""
    WITH_NGINX=false
    PORT=9090
    CLEAN=false
    IMAGE_TAG=""
    
    while [[ $# -gt 0 ]]; do
        case $1 in
            --with-nginx)
                WITH_NGINX=true
                shift
                ;;
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
                if [ -z "$JAR_FILE" ]; then
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
    
    # 验证参数
    if [ -z "$JAR_FILE" ]; then
        log_error "请指定JAR文件路径"
        show_help
        exit 1
    fi
    
    # 生成镜像标签
    if [ -z "$IMAGE_TAG" ]; then
        IMAGE_TAG=$(generate_image_tag "$JAR_FILE" "")
    fi
    
    # 执行部署步骤
    check_dependencies
    check_jar_file "$JAR_FILE"
    
    if [ "$CLEAN" = true ]; then
        clean_old_deployment
    fi
    
    prepare_environment "$JAR_FILE"
    deploy_service "$WITH_NGINX" "$PORT" "$JAR_FILE" "$IMAGE_TAG"
    check_service_status "$PORT"
    show_deployment_info "$WITH_NGINX" "$PORT" "$IMAGE_TAG"
    
    log_success "JAR包部署完成！镜像标签: $IMAGE_TAG"
}

# 错误处理
trap 'log_error "部署过程中发生错误，请检查日志"; exit 1' ERR

# 执行主函数
main "$@"
