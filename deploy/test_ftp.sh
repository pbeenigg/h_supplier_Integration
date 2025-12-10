#!/bin/bash

# FTP 连接测试脚本
# 用于快速验证 FTP 服务器连接和认证

HOST="43.205.50.170"
PORT="21"
USERNAME="staticdata"
PASSWORD="CQWdfki399kMFx8z"

echo "=========================================="
echo "FTP 连接测试"
echo "=========================================="
echo "服务器: $HOST:$PORT"
echo "用户名: $USERNAME"
echo "密码: ${PASSWORD:0:3}...${PASSWORD: -3}"
echo ""

# 测试 1: 端口连通性
echo "【测试 1】端口连通性测试..."
if command -v nc &> /dev/null; then
    timeout 5 nc -zv $HOST $PORT 2>&1
    if [ $? -eq 0 ]; then
        echo "✅ 端口 $PORT 可访问"
    else
        echo "❌ 端口 $PORT 不可访问"
        exit 1
    fi
else
    echo "⚠️  nc 命令不可用，跳过端口测试"
fi
echo ""

# 测试 2: FTP 服务器欢迎消息
echo "【测试 2】FTP 服务器欢迎消息..."
(echo "QUIT"; sleep 1) | telnet $HOST $PORT 2>&1 | grep -A 5 "220"
echo ""

# 测试 3: 使用 curl 测试登录
echo "【测试 3】使用 curl 测试 FTP 登录..."
if command -v curl &> /dev/null; then
    echo "执行命令: curl -v ftp://$USERNAME:***@$HOST:$PORT/"
    curl -v ftp://$USERNAME:$PASSWORD@$HOST:$PORT/ 2>&1 | grep -E "(Connected|Login|230|530|421)"
    CURL_EXIT=$?
    echo ""
    if [ $CURL_EXIT -eq 0 ]; then
        echo "✅ curl 测试通过"
    else
        echo "❌ curl 测试失败，退出码: $CURL_EXIT"
    fi
else
    echo "⚠️  curl 命令不可用"
fi
echo ""

# 测试 4: 使用 lftp 测试（如果可用）
echo "【测试 4】使用 lftp 测试..."
if command -v lftp &> /dev/null; then
    lftp -u $USERNAME,$PASSWORD $HOST -e "ls; quit" 2>&1 | head -20
    if [ $? -eq 0 ]; then
        echo "✅ lftp 测试通过"
    else
        echo "❌ lftp 测试失败"
    fi
else
    echo "⚠️  lftp 未安装，跳过测试"
    echo "   安装方法: brew install lftp"
fi
echo ""

# 测试 5: 密码特征检查
echo "【测试 5】密码特征检查..."
echo "密码长度: ${#PASSWORD}"
echo "首字符: ${PASSWORD:0:1}"
echo "尾字符: ${PASSWORD: -1}"
echo "是否包含空格: $(echo $PASSWORD | grep -q ' ' && echo '是' || echo '否')"
echo "是否包含特殊字符: $(echo $PASSWORD | grep -qE '[^a-zA-Z0-9]' && echo '是' || echo '否')"
echo ""

echo "=========================================="
echo "测试完成"
echo "=========================================="
echo ""
echo "📋 下一步建议："
echo "1. 如果 curl 测试成功，说明密码正确，问题可能在 Java 代码中"
echo "2. 如果 curl 测试失败（530），说明密码确实不对，需要联系管理员"
echo "3. 如果 curl 测试失败（421），说明连接数限制，等待后重试"
echo "4. 查看 Java 应用日志中的 '密码特征' 信息，对比密码是否一致"
