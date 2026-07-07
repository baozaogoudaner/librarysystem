@echo off
chcp 65001 >nul
title 图书馆智能服务系统 - 启动中

echo ============================================
echo    图书馆智能服务系统 - 一键启动脚本
echo ============================================
echo.

REM ---- 检查环境 ----
echo [1/8] 检查环境...
where java >nul 2>&1 || ( echo ❌ 未找到 Java，请安装 JDK 17 & pause & exit /b )
where mvn >nul 2>&1 || ( echo ❌ 未找到 Maven，请安装 Maven & pause & exit /b )
where node >nul 2>&1 || ( echo ❌ 未找到 Node.js，请安装 Node.js & pause & exit /b )
where redis-cli >nul 2>&1 || echo ⚠️ 未找到 Redis，请确保 Redis 服务已启动
echo ✅ 环境检查通过
echo.

REM ---- 安装父 POM + 公共模块 ----
echo [2/8] 编译公共模块...
call mvn install -N -q
call mvn install -pl common -q
if %errorlevel% neq 0 ( echo ❌ 编译失败 & pause & exit /b )
echo ✅ 编译完成
echo.

REM ---- 启动 Nacos ----
echo [3/8] 启动 Nacos...
start "Nacos" cmd /c "%USERPROFILE%\..\nacos\bin\startup.cmd -m standalone"
echo ⏳ 等待 Nacos 启动（约25秒）...
ping -n 26 127.0.0.1 >nul
echo ✅ Nacos 已启动
echo.

REM ---- 启动 RocketMQ ----
echo [4/8] 启动 RocketMQ...
start "RocketMQ-NameServer" cmd /c "%%ROCKETMQ_HOME%%\bin\mqnamesrv.cmd"
ping -n 8 127.0.0.1 >nul
start "RocketMQ-Broker" cmd /c "%%ROCKETMQ_HOME%%\bin\mqbroker.cmd -n localhost:9876"
echo ✅ RocketMQ 已启动
echo.

REM ---- 启动微服务 ----
echo [5/8] 启动 user-service（端口 8081）...
start "user-service" cmd /c "mvn spring-boot:run -pl user-service"
ping -n 35 127.0.0.1 >nul
echo ✅ user-service 已启动
echo.

echo [6/8] 启动 seat-service + book-service + reservation-service...
start "seat-service" cmd /c "mvn spring-boot:run -pl seat-service"
ping -n 5 127.0.0.1 >nul
start "book-service" cmd /c "mvn spring-boot:run -pl book-service"
ping -n 5 127.0.0.1 >nul
start "reservation-service" cmd /c "mvn spring-boot:run -pl reservation-service"
ping -n 30 127.0.0.1 >nul
echo ✅ 核心服务已启动
echo.

echo [7/8] 启动 borrowing-service + notification-service + gateway...
start "borrowing-service" cmd /c "mvn spring-boot:run -pl borrowing-service"
ping -n 3 127.0.0.1 >nul
start "notification-service" cmd /c "mvn spring-boot:run -pl notification-service"
ping -n 3 127.0.0.1 >nul
start "gateway-service" cmd /c "mvn spring-boot:run -pl gateway-service"
echo ✅ 网关已启动
echo.

REM ---- 启动前端 ----
echo [8/8] 安装前端依赖并启动...
cd frontend
call npm install --no-audit --no-fund
start "frontend" cmd /c "npx vite --host"
cd ..
echo ✅ 前端已启动
echo.

echo ============================================
echo    启动完成！
echo    登录地址：http://localhost:5173/login
echo    管理员：admin / 123456
echo    普通用户：zhangsan / 123456
echo ============================================
echo.
echo 各服务端口：
echo   Nacos:     8848
echo   Gateway:   8080
echo   User:      8081
echo   Seat:      8082
echo   Reservation: 8083
echo   Book:      8084
echo   Borrowing: 8085
echo   Notification: 8086
echo   Frontend:  5173
echo.
pause
