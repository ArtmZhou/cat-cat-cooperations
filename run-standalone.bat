@echo off
chcp 65001 >nul

:: 猫猫多Agent协同系统 - 快速启动脚本 (单机版)

echo.
echo 🐱 猫猫多Agent协同系统 - 快速启动
echo ==================================
echo.

:: 检查Java
java -version >nul 2>&1
if errorlevel 1 (
    echo ✗ 未检测到Java，请先安装Java 17+
    pause
    exit /b 1
)

:: 检查Maven
mvn -version >nul 2>&1
if errorlevel 1 (
    echo ✗ 未检测到Maven，请先安装Maven
    pause
    exit /b 1
)

echo [1/3] 构建项目...
call mvn clean install -DskipTests -pl cat-common,cat-runtime,cat-agent,cat-task,cat-orchestration,cat-standalone -am
if errorlevel 1 (
    echo ✗ 构建失败
    pause
    exit /b 1
)

echo.
echo [2/3] 启动服务...
echo.
start "Cat Agent Platform" java -jar cat-standalone/target/cat-standalone-1.0.0-SNAPSHOT.jar

echo [3/3] 等待服务启动...
timeout /t 5 /nobreak >nul

echo.
echo ==================================
echo 🐱 启动完成！
echo ==================================
echo.
echo 访问地址:
echo   API:      http://localhost:8080/api/v1
echo   H2控制台: http://localhost:8080/h2-console
echo.
echo 默认用户: admin / admin123
echo H2连接: jdbc:h2:./data/cat_agent (用户名: sa, 密码为空)
echo.
pause