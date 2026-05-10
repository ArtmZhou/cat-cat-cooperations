@echo off
chcp 65001 >nul

:: 猫猫多Agent协同系统 - 快速启动脚本

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

echo [1/2] 构建项目...
call mvn clean package -pl cat-standalone -am -DskipTests
if errorlevel 1 (
    echo ✗ 构建失败
    pause
    exit /b 1
)

echo.
echo [2/2] 启动服务...
start "Cat Agent Platform" java -jar cat-standalone/target/cat-standalone-1.0.0-SNAPSHOT.jar

timeout /t 5 /nobreak >nul

echo.
echo ==================================
echo 🐱 启动完成！
echo ==================================
echo.
echo 访问地址:
echo   API:      http://localhost:8080/api/v1
echo   前端界面: http://localhost:3000
echo   数据目录: ./data/
echo.
pause
