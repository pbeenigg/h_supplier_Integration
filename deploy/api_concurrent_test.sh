#!/bin/bash

# API并发测试脚本 (适配Mac和Ubuntu)
# 用法: ./api_concurrent_test.sh [选项]
# 示例: ./api_concurrent_test.sh -c 10 -d 60 -i 0.5

#基本用法

#<BASH>
## 使用默认参数
#./api_concurrent_test.sh
## 自定义参数
#./api_concurrent_test.sh -c 10 -d 60 -i 0.5
## 静默模式运行
#./api_concurrent_test.sh -c 5 -d 30 -q
#参数说明
#-c, --concurrent NUM: 并发用户数 (默认: 5)
#-d, --duration SEC: 测试持续时间(秒) (默认: 30)
#-i, --interval SEC: 请求间隔(秒) (默认: 0.5)
#-t, --timeout SEC: 请求超时时间(秒) (默认: 60)
#-o, --output DIR: 输出目录 (默认: ./test_results)
#-q, --quiet: 静默模式，不显示详细进度
#-h, --help: 显示帮助信息

#示例
#<BASH>
## 10个并发用户，测试60秒
#./api_concurrent_test.sh -c 10 -d 60
## 5个并发用户，测试30秒，请求间隔1秒，超时30秒
#./api_concurrent_test.sh -c 5 -d 30 -i 1 -t 30
## 静默模式运行，结果保存到指定目录
#./api_concurrent_test.sh -c 3 -d 15 -o ./my_results -q



# 默认参数
DEFAULT_CONCURRENT_USERS=5
DEFAULT_TEST_DURATION=10
DEFAULT_INTERVAL=0.5
DEFAULT_TIMEOUT=60

# 全局变量
CONCURRENT_USERS=$DEFAULT_CONCURRENT_USERS
TEST_DURATION=$DEFAULT_TEST_DURATION
REQUEST_INTERVAL=$DEFAULT_INTERVAL
REQUEST_TIMEOUT=$DEFAULT_TIMEOUT
OUTPUT_DIR="./test_results"
LOG_FILE="$OUTPUT_DIR/test_log.txt"
SHOW_PROGRESS=true
DEBUG=false

# API配置
API_URL="http://colosseum.otrams.com:8087/ws/index.php"
QUERY_PARAMS=(
    "sel_currency=USD"
    "availableonly=1"
    "country_of_residence=1"
    "sel_country=1"
    "roomDetails=[{\"numberOfAdults\":2}]"
    "gzip=no"
    "limit_hotel_room_type=5"
    "chk_ratings=1.0,2.0,3.0,4.0,5.0"
    "checkout_date=16/12/2025"
    "timeout=60"
    "password=Welcome@@123"
    "checkin_date=15/12/2025"
    "hotel_ids=OT000003795,OT000003796,OT000003797,OT000003798,OT000003799,OT000003800"
    "action=hotel_search"
    "sel_nationality=1"
    "static_data=1"
    "username=Heytrip_Test"
    "number_of_rooms=1"
)

# 显示帮助信息
show_help() {
    echo "API并发测试脚本"
    echo "用法: $0 [选项]"
    echo ""
    echo "选项:"
    echo "  -c, --concurrent NUM    并发用户数 (默认: $DEFAULT_CONCURRENT_USERS)"
    echo "  -d, --duration SEC      测试持续时间(秒) (默认: $DEFAULT_TEST_DURATION)"
    echo "  -i, --interval SEC      请求间隔(秒) (默认: $DEFAULT_INTERVAL)"
    echo "  -t, --timeout SEC       请求超时时间(秒) (默认: $DEFAULT_TIMEOUT)"
    echo "  -o, --output DIR        输出目录 (默认: $OUTPUT_DIR)"
    echo "  --debug                 调试模式，显示详细信息"
    echo "  -q, --quiet             静默模式，不显示进度"
    echo "  -h, --help              显示此帮助信息"
    echo ""
    echo "示例:"
    echo "  $0                          # 使用默认参数"
    echo "  $0 -c 10 -d 60             # 10个并发用户，测试60秒"
    echo "  $0 -c 5 -d 30 -i 1         # 5个并发用户，测试30秒，请求间隔1秒"
}

# 解析命令行参数
parse_arguments() {
    while [[ $# -gt 0 ]]; do
        case $1 in
            -c|--concurrent)
                CONCURRENT_USERS="$2"
                shift 2
                ;;
            -d|--duration)
                TEST_DURATION="$2"
                shift 2
                ;;
            -i|--interval)
                REQUEST_INTERVAL="$2"
                shift 2
                ;;
            -t|--timeout)
                REQUEST_TIMEOUT="$2"
                shift 2
                ;;
            -o|--output)
                OUTPUT_DIR="$2"
                LOG_FILE="$OUTPUT_DIR/test_log.txt"
                shift 2
                ;;
            --debug)
                DEBUG=true
                shift
                ;;
            -q|--quiet)
                SHOW_PROGRESS=false
                shift
                ;;
            -h|--help)
                show_help
                exit 0
                ;;
            *)
                echo "未知参数: $1"
                show_help
                exit 1
                ;;
        esac
    done

    # 验证参数
    if ! [[ "$CONCURRENT_USERS" =~ ^[0-9]+$ ]] || [ "$CONCURRENT_USERS" -le 0 ]; then
        echo "错误: 并发用户数必须是正整数"
        exit 1
    fi

    if ! [[ "$TEST_DURATION" =~ ^[0-9]+$ ]] || [ "$TEST_DURATION" -le 0 ]; then
        echo "错误: 测试持续时间必须是正整数"
        exit 1
    fi

    if ! [[ "$REQUEST_INTERVAL" =~ ^[0-9]+(\.[0-9]+)?$ ]] || (( $(echo "$REQUEST_INTERVAL <= 0" | bc -l 2>/dev/null || echo "1") )); then
        echo "错误: 请求间隔必须是正数"
        exit 1
    fi

    if ! [[ "$REQUEST_TIMEOUT" =~ ^[0-9]+$ ]] || [ "$REQUEST_TIMEOUT" -le 0 ]; then
        echo "错误: 超时时间必须是正整数"
        exit 1
    fi
}

# 检测操作系统类型并获取时间戳
get_timestamp() {
    if [[ "$OSTYPE" == "darwin"* ]]; then
        # Mac OS X
        if command -v gdate >/dev/null 2>&1; then
            echo $(gdate +%s%3N 2>/dev/null || echo $(($(date +%s) * 1000 + $(date +%N | cut -b1-3))))
        else
            echo $(($(date +%s) * 1000 + $(date +%N | cut -b1-3)))
        fi
    else
        # Linux (Ubuntu)
        if date +%s%3N >/dev/null 2>&1; then
            echo $(date +%s%3N)
        else
            echo $(($(date +%s) * 1000 + $(date +%N | cut -b1-3)))
        fi
    fi
}

# URL编码函数
urlencode() {
    local string="${1}"
    local strlen=${#string}
    local encoded=""
    local pos c o

    for (( pos=0 ; pos<strlen ; pos++ )); do
        c=${string:$pos:1}
        case "$c" in
            [-_.~a-zA-Z0-9] ) o="${c}" ;;
            * ) printf -v o '%%%02x' "'$c" ;;
        esac
        encoded+="${o}"
    done
    echo "${encoded}"
}

# 构建完整URL
build_full_url() {
    local base_url="$API_URL"
    local params=""

    for param in "${QUERY_PARAMS[@]}"; do
        if [[ $param == roomDetails=* ]]; then
            # 特殊处理roomDetails参数
            local key="roomDetails"
            local value="${param#roomDetails=}"
            local encoded_value=$(echo "$value" | sed 's/ /%20/g' | sed 's/"/%22/g' | sed "s/'/%27/g" | sed 's/{/%7B/g' | sed 's/}/%7D/g' | sed 's/:/%3A/g' | sed 's/,/%2C/g')
            params="${params}&${key}=${encoded_value}"
        else
            # 普通参数
            local key="${param%%=*}"
            local value="${param#*=}"
            params="${params}&${key}=${value}"
        fi
    done

    # 移除第一个&字符
    params="${params#&}"
    echo "${base_url}?${params}"
}

# 改进的HTTP请求函数
make_http_request() {
    local url="$1"
    local timeout="$2"

    # 使用临时文件存储响应体
    local temp_response=$(mktemp)
    local temp_header=$(mktemp)

    # 发送HTTP请求，分别获取响应码、响应时间和响应体
    local curl_result=$(curl -s \
        -w "%{http_code}|%{time_total}|%{size_download}" \
        -D "$temp_header" \
        -o "$temp_response" \
        --connect-timeout "$timeout" \
        --max-time $((timeout + 10)) \
        "$url")

    local response_code=$(echo "$curl_result" | cut -d'|' -f1)
    local time_total=$(echo "$curl_result" | cut -d'|' -f2)
    local size_download=$(echo "$curl_result" | cut -d'|' -f3)

    # 清理临时文件
    rm -f "$temp_response" "$temp_header"

    # 如果响应码为空或无效，设置默认值
    if [ -z "$response_code" ] || [ "$response_code" = "" ]; then
        response_code="000"
    fi

    # 返回结果
    echo "$response_code|$time_total|$size_download"
}

# 单次请求函数
make_request() {
    local thread_id=$1
    local count=0
    local success_count=0
    local fail_count=0
    local full_url=$(build_full_url)

    if [ "$SHOW_PROGRESS" = true ]; then
        echo "线程 $thread_id 启动..."
    fi

    while [ $count -lt $TEST_DURATION ]; do
        # 获取开始时间
        local start_time=$(get_timestamp)

        # 使用改进的HTTP请求函数
        local response_data=$(make_http_request "$full_url" "$REQUEST_TIMEOUT")
        local response_code=$(echo "$response_data" | cut -d'|' -f1)
        local time_total=$(echo "$response_data" | cut -d'|' -f2)
        local size_download=$(echo "$response_data" | cut -d'|' -f3)

        # 调试输出
        if [ "$DEBUG" = true ]; then
            echo "DEBUG - Thread $thread_id Request $((count+1)):"
            echo "  URL: $full_url"
            echo "  Response Code: $response_code"
            echo "  Time Total: $time_total"
            echo "  Size Download: $size_download"
            echo "  Response Data: $response_data"
            echo "---"
        fi

        # 计算响应时间(毫秒)
        local response_time_ms
        if [ -n "$time_total" ] && [ "$time_total" != "" ]; then
            response_time_ms=$(echo "$time_total * 1000" | bc -l 2>/dev/null | cut -d'.' -f1 2>/dev/null || echo "0")
        else
            response_time_ms="0"
        fi

        if [ -z "$response_time_ms" ]; then
            response_time_ms="0"
        fi

        # 获取结束时间
        local end_time=$(get_timestamp)

        # 计算持续时间
        local duration
        if [ "$end_time" -ge "$start_time" ]; then
            duration=$((end_time - start_time))
        else
            duration=$response_time_ms
        fi

        # 如果curl响应时间计算失败，使用实际测量的时间
        if [ "$response_time_ms" = "0" ] || [ "$response_time_ms" = "" ]; then
            duration=$((end_time - start_time))
            response_time_ms=$duration
        fi

        # 记录结果 (包含URL)
        echo "$(date '+%Y-%m-%d %H:%M:%S'),线程$thread_id,响应码:$response_code,耗时:${response_time_ms}ms,URL:${full_url}" >> "$LOG_FILE"

        # 统计成功/失败次数
        if [ "$response_code" = "200" ]; then
            ((success_count++))
        else
            ((fail_count++))
        fi

        # 显示进度
        if [ "$SHOW_PROGRESS" = true ]; then
            echo "线程 $thread_id: 请求 $((count+1))/$TEST_DURATION - 响应码:$response_code - 耗时:${response_time_ms}ms"
        fi

        # 增加计数器
        count=$((count + 1))

        # 请求间隔
        if (( $(echo "$REQUEST_INTERVAL > 0" | bc -l 2>/dev/null || echo "0") )); then
            sleep $REQUEST_INTERVAL
        fi
    done

    if [ "$SHOW_PROGRESS" = true ]; then
        echo "线程 $thread_id 完成 - 成功:$success_count, 失败:$fail_count"
    fi
}

# 初始化测试环境
init_test() {
    # 创建输出目录
    mkdir -p "$OUTPUT_DIR"

    # 初始化日志文件 (包含URL列)
    echo "时间,线程,响应码,耗时(ms),URL" > "$LOG_FILE"

    echo "开始API并发测试..."
    echo "测试URL: $(build_full_url)"
    echo "并发用户数: $CONCURRENT_USERS"
    echo "测试持续时间: $TEST_DURATION 秒"
    echo "请求间隔: $REQUEST_INTERVAL 秒"
    echo "请求超时: $REQUEST_TIMEOUT 秒"
    echo "输出目录: $OUTPUT_DIR"
    echo "调试模式: $DEBUG"
    echo "操作系统: $OSTYPE"
    echo "========================"
}

# 运行并发测试
run_test() {
    echo "启动 $CONCURRENT_USERS 个并发用户..."

    for i in $(seq 1 $CONCURRENT_USERS); do
        make_request $i &
        sleep 0.1  # 启动间隔
    done

    # 等待所有后台任务完成
    wait

    echo "所有测试完成!"
    echo "结果保存在: $LOG_FILE"
}

# 生成统计报告
generate_report() {
    echo "========================"
    echo "测试统计报告:"
    echo "========================"

    # 检查日志文件是否存在且有内容
    if [ ! -f "$LOG_FILE" ] || [ $(wc -l < "$LOG_FILE" 2>/dev/null || echo "0") -le 1 ]; then
        echo "未生成有效测试数据"
        return 1
    fi

    # 统计总请求数
    local total_requests=$(tail -n +2 "$LOG_FILE" | wc -l | tr -d ' ')
    echo "总请求数: $total_requests"

    # 统计各响应码数量
    echo "响应码统计:"
    tail -n +2 "$LOG_FILE" | awk -F',' '{print $3}' | sort | uniq -c | while read count code; do
        echo "  $code: $count 次"
    done

    # 统计成功请求数(200响应码)
    local success_requests=$(grep ",响应码:200," "$LOG_FILE" 2>/dev/null | wc -l | tr -d ' ')
    echo "成功请求数(200): $success_requests"

    # 计算成功率
    if [ "$total_requests" -gt 0 ]; then
        if command -v bc >/dev/null 2>&1; then
            local success_rate=$(echo "scale=2; $success_requests * 100 / $total_requests" | bc 2>/dev/null || echo "0")
        else
            # 如果没有bc，使用简单的整数计算
            local success_rate=$((success_requests * 100 / total_requests))
        fi
        echo "成功率: $success_rate%"
    fi

    # 计算平均响应时间(只计算成功的请求)
    if [ "$success_requests" -gt 0 ]; then
        local avg_time=$(grep ",响应码:200," "$LOG_FILE" | awk -F',' '{sum+=$4; count++} END {if(count>0) printf "%.2f", sum/count; else print "0"}')
        echo "成功请求平均响应时间: ${avg_time}ms"
    else
        echo "成功请求平均响应时间: 0ms"
    fi

    # 找出最大响应时间
    local max_time=$(tail -n +2 "$LOG_FILE" | awk -F',' '{print $4}' | grep -E '^[0-9]+$' | sort -n | tail -1)
    if [ -z "$max_time" ]; then
        max_time="0"
    fi
    echo "最大响应时间: ${max_time}ms"

    # 找出最小响应时间
    local min_time=$(tail -n +2 "$LOG_FILE" | awk -F',' '{print $4}' | grep -E '^[0-9]+$' | grep -v "^0$" | sort -n | head -1)
    if [ -z "$min_time" ]; then
        min_time="0"
    fi
    echo "最小响应时间: ${min_time}ms"

    echo "========================"
    echo "测试完成!详细结果请查看 $LOG_FILE"
}

# 主函数
main() {
    # 解析命令行参数
    parse_arguments "$@"

    # 初始化测试
    init_test

    # 运行测试
    run_test

    # 生成报告
    generate_report
}

# 如果直接运行脚本，则执行主函数
if [[ "${BASH_SOURCE[0]}" == "${0}" ]]; then
    main "$@"
fi
