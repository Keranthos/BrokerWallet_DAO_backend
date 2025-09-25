@echo off
chcp 65001 >nul
set JAVA_TOOL_OPTIONS=-Dfile.encoding=UTF-8 -Dconsole.encoding=UTF-8
echo ========================================
echo    🚀 BrokerWallet后端服务启动脚本
echo ========================================
echo.

:: 设置颜色
color 0A

:: 检查Java环境
echo 📋 正在检查Java环境...
java -version >nul 2>&1
if %errorlevel% neq 0 (
    echo ❌ 错误: 未找到Java环境，请确保已安装Java 17或更高版本
    echo 💡 下载地址: https://www.oracle.com/java/technologies/downloads/
    pause
    exit /b 1
)
echo ✅ Java环境检查通过

:: 检查Maven环境
echo 📋 正在检查Maven环境...
mvn -version >nul 2>&1
if %errorlevel% neq 0 (
    echo ❌ 错误: 未找到Maven环境，请确保已安装Maven 3.6或更高版本
    echo 💡 下载地址: https://maven.apache.org/download.cgi
    pause
    exit /b 1
)
echo ✅ Maven环境检查通过

:: 获取本机IP地址
echo 📋 正在获取本机IP地址...
for /f "tokens=2 delims=:" %%a in ('ipconfig ^| findstr /i "IPv4"') do (
    for /f "tokens=1" %%b in ("%%a") do (
        set LOCAL_IP=%%b
        goto :found_ip
    )
)
:found_ip
echo 🌐 本机IP地址: %LOCAL_IP%

:: 显示配置信息
echo.
echo ========================================
echo    📱 Android应用配置信息
echo ========================================
echo 请在Android项目的ServerConfig.java中设置：
echo SERVER_HOST = "%LOCAL_IP%"
echo SERVER_PORT = 5000
echo.
echo 配置文件位置：
echo app/src/main/java/com/example/brokerfi/config/ServerConfig.java
echo.

:: 提示用户检查数据库
echo ========================================
echo    🗄️ 数据库配置检查
echo ========================================
echo 请确保：
echo 1. MySQL服务已启动
echo 2. 已创建brokerwallet数据库
echo 3. 数据库密码已在application.yml中配置
echo.
echo 如需初始化数据库，请执行：
echo database/init.sql
echo.

:: 询问是否继续
set /p continue="是否继续启动服务？(Y/N): "
if /i "%continue%" neq "Y" (
    echo 👋 启动已取消
    pause
    exit /b 0
)

:: 创建日志目录
if not exist "logs" mkdir logs

:: 创建上传目录
if not exist "uploads" mkdir uploads
if not exist "uploads\proofs" mkdir uploads\proofs
if not exist "uploads\nft-images" mkdir uploads\nft-images
if not exist "uploads\thumbnails" mkdir uploads\thumbnails

echo.
echo ========================================
echo    🔥 正在启动BrokerWallet后端服务
echo ========================================
echo.

:: 启动Spring Boot应用
echo 🚀 启动中，请稍候...
echo 📝 日志将保存到: logs/brokerwallet-backend.log
echo 🌐 服务地址: http://localhost:5000
echo 🏥 健康检查: http://localhost:5000/api/health
echo 📱 手机访问: http://%LOCAL_IP%:5000
echo.

:: 使用Maven启动（设置UTF-8编码）
set MAVEN_OPTS=-Dfile.encoding=UTF-8 -Dconsole.encoding=UTF-8 -Djava.awt.headless=true
set JAVA_TOOL_OPTIONS=-Dfile.encoding=UTF-8 -Dconsole.encoding=UTF-8
mvn spring-boot:run

:: 如果Maven启动失败，提示用户
if %errorlevel% neq 0 (
    echo.
    echo ❌ 服务启动失败！
    echo.
    echo 🔍 可能的原因：
    echo 1. 端口5000已被占用
    echo 2. 数据库连接失败
    echo 3. Maven依赖下载失败
    echo.
    echo 💡 解决方案：
    echo 1. 检查端口占用: netstat -ano ^| findstr :5000
    echo 2. 检查数据库配置: src/main/resources/application.yml
    echo 3. 重新下载依赖: mvn clean install
    echo.
    echo 📋 查看详细日志: logs/brokerwallet-backend.log
    echo.
    pause
    exit /b 1
)

echo.
echo 👋 服务已停止
pause

