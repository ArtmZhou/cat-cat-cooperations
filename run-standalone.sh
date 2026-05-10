#!/bin/bash

# 猫猫多Agent协同系统 - 快速启动脚本

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

echo "[1/2] 构建项目..."
mvn clean package -pl cat-standalone -am -DskipTests
if [ $? -ne 0 ]; then
    echo "✗ 构建失败"
    exit 1
fi

echo ""
echo "[2/2] 启动服务..."
java -jar cat-standalone/target/cat-standalone-1.0.0-SNAPSHOT.jar &

sleep 5

echo ""
echo "=================================="
echo "🐱 启动完成！"
echo "=================================="
echo ""
echo "访问地址:"
echo "  API:      http://localhost:8080/api/v1"
echo "  前端界面: http://localhost:3000"
echo "  数据目录: ./data/"
echo ""
