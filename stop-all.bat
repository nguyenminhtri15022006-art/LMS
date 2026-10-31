@echo off
chcp 65001 >nul
title Dừng toàn bộ Hệ thống LMS Microservices
echo ============================================================
echo   ĐANG DỪNG CÁC SERVICE BACKEND & WEB PORTAL (PORTS 8080-8086)
echo ============================================================
echo.

powershell -Command "Get-NetTCPConnection -State Listen -ErrorAction SilentlyContinue | Where-Object { $_.LocalPort -in 8080, 8081, 8082, 8086 } | ForEach-Object { Stop-Process -Id $_.OwningProcess -Force -ErrorAction SilentlyContinue; Write-Host ('[OK] Đã tắt tiến trình trên Port ' + $_.LocalPort) }"

echo.
echo ============================================================
echo   ĐÃ TẮT XONG CÁC SERVICES!
echo ============================================================
echo.
pause
