@echo off
chcp 65001 >nul
title Khởi động Hệ thống LMS Microservices
echo ============================================================
echo   KHỞI ĐỘNG HỆ THỐNG EDULEARN LMS (MICROSERVICES ARCHITECTURE)
echo ============================================================
echo.

cd /d "%~dp0"

echo [1/5] Kiểm tra và khởi động các container Docker (DB & Eureka)...
docker start lms-mysql lms-mongodb lms-redis lms-minio lms-mail-server lms-service-registry >nul 2>&1
timeout /t 3 /nobreak >nul

echo [2/5] Đang khởi động Identity Service (Port 8081)...
start "1. Identity Service [Port 8081]" cmd /k "chcp 65001 >nul && set MAIL_HOST=localhost&& set MAIL_PORT=1025&& set EUREKA_URL=http://localhost:8761/eureka&& java -jar backend\identity-service\target\identity-service-0.0.1-SNAPSHOT.jar"
timeout /t 5 /nobreak >nul

echo [3/5] Đang khởi động Course Service (Port 8082)...
start "2. Course Service [Port 8082]" cmd /k "chcp 65001 >nul && set EUREKA_URL=http://localhost:8761/eureka&& java -jar backend\course-service\target\course-service-0.0.1-SNAPSHOT.jar"
timeout /t 5 /nobreak >nul

echo [4/5] Đang khởi động API Gateway (Port 8080)...
start "3. API Gateway [Port 8080]" cmd /k "chcp 65001 >nul && set EUREKA_URL=http://localhost:8761/eureka&& java -jar backend\api-gateway\target\api-gateway-0.0.1-SNAPSHOT.jar"
timeout /t 5 /nobreak >nul

echo [5/5] Đang khởi động Web Portal (Port 8086)...
start "4. Web Portal [Port 8086]" cmd /k "chcp 65001 >nul && java -jar backend\web-portal\target\web-portal-0.0.1-SNAPSHOT.jar"
timeout /t 8 /nobreak >nul

echo.
echo ============================================================
echo   TẤT CẢ SERVICE ĐÃ ĐƯỢC KHỞI ĐỘNG THÀNH CÔNG!
echo ============================================================
echo   - Web Portal:       http://localhost:8086
echo   - API Gateway:      http://localhost:8080
echo   - Eureka Registry:  http://localhost:8761
echo   - Course Service:   http://localhost:8082/course
echo   - Identity Service: http://localhost:8081/identity
echo ============================================================
echo.
pause
