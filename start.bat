@echo off
chcp 65001 >nul
setlocal enabledelayedexpansion

:: ============================================
::  MarkLive 一键启动脚本 (Windows)
::  自动启动 Java 后端 + Vue 前端，并打开浏览器
:: ============================================

title MarkLive - 一键启动

echo.
echo ========================================
echo    MarkLive 一键启动
echo ========================================
echo.

:: 获取脚本所在目录
set "SCRIPT_DIR=%~dp0"
set "SCRIPT_DIR=%SCRIPT_DIR:~0,-1%"

:: ==================== 1. 启动后端 ====================
echo [1/3] 启动 Java 后端 (Spring Boot :8080)...
cd /d "%SCRIPT_DIR%"

start "MarkLive-Backend" cmd /c "mvn spring-boot:run -q"
echo   ✓ 后端进程已在独立窗口启动，等待就绪...

:: 等待后端就绪（最多等待 30 秒）
set "count=0"
:wait_backend
timeout /t 1 /nobreak >nul
set /a count+=1
curl -s http://localhost:8080/api/markdown/scan?folderPath=%TEMP% >nul 2>&1
if %errorlevel% equ 0 goto backend_ready
if %count% lss 30 goto wait_backend
echo   ✗ 后端启动超时，请检查后端窗口的日志。
pause
exit /b 1

:backend_ready
echo   ✓ 后端已就绪！

:: ==================== 2. 启动前端 ====================
echo.
echo [2/3] 启动 Vue 前端 (Vite :3000)...
cd /d "%SCRIPT_DIR%\frontend"

if not exist "node_modules" (
    echo   ⏳ 检测到缺少 node_modules，正在自动安装依赖...
    call npm install
    echo   ✓ 依赖安装完成
)

start "MarkLive-Frontend" cmd /c "npx vite --host 0.0.0.0 --port 3000"
echo   ✓ 前端进程已在独立窗口启动

:: 等待前端就绪
timeout /t 4 /nobreak >nul

:: ==================== 3. 打开浏览器 ====================
echo.
echo [3/3] 打开浏览器...
start "" http://localhost:3000
echo   ✓ 浏览器已打开

echo.
echo ========================================
echo    MarkLive 启动完成！
echo    后端: http://localhost:8080
echo    前端: http://localhost:3000
echo    关闭此窗口不会停止服务
echo    请分别关闭 "MarkLive-Backend"
echo    和 "MarkLive-Frontend" 窗口来停止
echo ========================================

pause
