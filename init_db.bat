@echo off
chcp 65001 >nul
title 初始化数据库

echo ============================================
echo    初始化数据库
echo ============================================
echo.
echo 请确保 MySQL 已启动，密码为 123456
echo 如需修改密码，请编辑本文件
echo.

set MYSQL_PASSWORD=123456

echo 正在初始化数据库...
for %%f in (sql\*.sql) do (
    echo   执行 %%f ...
    "C:\Program Files\MySQL\MySQL Server 8.0\bin\mysql.exe" -u root -p%MYSQL_PASSWORD% --default-character-set=utf8mb4 < "%%f"
)

echo.
echo ✅ 数据库初始化完成！
pause
