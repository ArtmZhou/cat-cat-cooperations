#!/bin/bash

# 猫猫多Agent协同系统 - 快速启动脚本 (单机版)

echo ""
echo "🐱 猫猫多Agent协同系统 - 快速启动"
echo "=================================="
echo ""

# 检查Java
if ! command -v java &> /dev/null; then
    echo "✗ 未检测到Java，请先安装Java 17+"
    exit 1
fi

# 检查Maven
if ! command -v mvn &> /dev/null; then
    echo "✗ 未检测到Maven，请先安装Maven"
    exit 1
fi

echo "[1/3] 构建项目..."
mvn clean install -DskipTests -pl cat-common,cat-runtime,cat-agent,cat-task,cat-orchestration,cat-standalone -am
if [ $? -ne 0 ]; then
    echo "✗ 构建失败"
    exit 1
fi

echo ""
echo "[2/3] 启动服务..."
java -jar cat-standalone/target/cat-standalone-1.0.0-SNAPSHOT.jar &

echo "[3/3] 等待服务启动..."
sleep 5

echo ""
echo "=================================="
echo "🐱 启动完成！"
echo "=================================="
echo ""
echo "访问地址:"
echo "  API:      http://localhost:8080/api/v1"
echo "  H2控制台: http://localhost:8080/h2-console"
echo ""
echo "默认用户: admin / admin123"
echo "H2连接: jdbc:h2:./data/cat_agent (用户名: sa, 密码为空)"
echo ""