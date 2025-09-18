@echo off
chcp 65001 >nul
echo ========================================
echo    🔍 BrokerWallet后端配置检查工具
echo ========================================
echo.

:: 设置颜色
color 0B

:: 检查项目文件
echo 📁 检查项目文件结构...
if exist "pom.xml" (
    echo ✅ pom.xml 存在
) else (
    echo ❌ pom.xml 不存在
)

if exist "src\main\resources\application.yml" (
    echo ✅ application.yml 存在
) else (
    echo ❌ application.yml 不存在
)

if exist "database\init.sql" (
    echo ✅ 数据库初始化脚本存在
) else (
    echo ❌ 数据库初始化脚本不存在
)

:: 检查Java环境
echo.
echo ☕ 检查Java环境...
java -version 2>&1 | findstr "version" >nul
if %errorlevel% equ 0 (
    echo ✅ Java环境已安装
    java -version 2>&1 | findstr "version"
) else (
    echo ❌ Java环境未安装或未配置PATH
)

:: 检查Maven环境
echo.
echo 🔧 检查Maven环境...
mvn -version >nul 2>&1
if %errorlevel% equ 0 (
    echo ✅ Maven环境已安装
    mvn -version 2>&1 | findstr "Apache Maven"
) else (
    echo ❌ Maven环境未安装或未配置PATH
)

:: 检查MySQL连接
echo.
echo 🗄️ 检查MySQL环境...
mysql --version >nul 2>&1
if %errorlevel% equ 0 (
    echo ✅ MySQL客户端已安装
    mysql --version 2>&1
) else (
    echo ❌ MySQL客户端未安装或未配置PATH
)

:: 检查端口占用
echo.
echo 🌐 检查端口5000占用情况...
netstat -ano | findstr :5000 >nul
if %errorlevel% equ 0 (
    echo ⚠️ 端口5000已被占用
    echo 占用详情：
    netstat -ano | findstr :5000
) else (
    echo ✅ 端口5000可用
)

:: 获取网络信息
echo.
echo 📡 网络配置信息...
echo 本机IP地址：
ipconfig | findstr "IPv4" | findstr /v "127.0.0.1"

:: 检查目录权限
echo.
echo 📂 检查目录权限...
if exist "uploads" (
    echo ✅ uploads目录存在
) else (
    echo ⚠️ uploads目录不存在，将在启动时创建
)

if exist "logs" (
    echo ✅ logs目录存在
) else (
    echo ⚠️ logs目录不存在，将在启动时创建
)

:: 检查配置文件内容
echo.
echo ⚙️ 检查配置文件...
if exist "src\main\resources\application.yml" (
    echo 数据库配置：
    findstr "url:" src\main\resources\application.yml
    findstr "username:" src\main\resources\application.yml
    echo 注意：请确保数据库密码已正确配置
) else (
    echo ❌ 配置文件不存在
)

:: 总结
echo.
echo ========================================
echo    📋 配置检查完成
echo ========================================
echo.
echo 💡 使用建议：
echo 1. 确保所有✅项目都正常
echo 2. 解决所有❌和⚠️的问题
echo 3. 运行 start-server.bat 启动服务
echo 4. 访问 http://localhost:5000/api/health 验证服务
echo.

:: 询问是否查看详细帮助
set /p help="是否查看详细帮助信息？(Y/N): "
if /i "%help%"=="Y" (
    echo.
    echo ========================================
    echo    🆘 详细帮助信息
    echo ========================================
    echo.
    echo Java安装：
    echo - 下载Java 17+: https://www.oracle.com/java/technologies/downloads/
    echo - 配置JAVA_HOME环境变量
    echo.
    echo Maven安装：
    echo - 下载Maven: https://maven.apache.org/download.cgi
    echo - 配置MAVEN_HOME和PATH环境变量
    echo.
    echo MySQL安装：
    echo - 下载MySQL: https://dev.mysql.com/downloads/mysql/
    echo - 创建数据库: CREATE DATABASE brokerwallet;
    echo - 执行初始化脚本: database/init.sql
    echo.
    echo 防火墙配置：
    echo - Windows防火墙允许端口5000
    echo - 路由器端口转发（如需要）
    echo.
    echo Android配置：
    echo - 修改ServerConfig.java中的SERVER_HOST
    echo - 确保手机和电脑在同一网络
    echo.
)

echo.
pause

