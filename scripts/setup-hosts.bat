@echo off
chcp 65001 >nul
echo ============================================================
echo   CAU HINH TEN MIEN CHO HE THONG LMS EDULEARN
echo ============================================================
echo.

:: Kiem tra quyen Administrator
net session >nul 2>&1
if %errorlevel% neq 0 (
    echo [THONG BAO] Dang yeu cau quyen Administrator de chinh sua file hosts...
    powershell -Command "Start-Process cmd -ArgumentList '/c \"\"%~f0\"\"' -Verb RunAs"
    exit /b
)

set HOSTS_FILE=%WINDIR%\System32\drivers\etc\hosts

:: Kiem tra xem da co domain chua
findstr /i "LMS-EduLearn" "%HOSTS_FILE%" >nul
if %errorlevel% equ 0 (
    echo [OK] Ten mien LMS-EduLearn da duoc cau hinh trong file hosts!
) else (
    echo. >> "%HOSTS_FILE%"
    echo # ======================================== >> "%HOSTS_FILE%"
    echo # LMS EduLearn Local Domains >> "%HOSTS_FILE%"
    echo # ======================================== >> "%HOSTS_FILE%"
    echo 127.0.0.1  LMS-EduLearn >> "%HOSTS_FILE%"
    echo 127.0.0.1  lms-edulearn >> "%HOSTS_FILE%"
    echo 127.0.0.1  edulearn.local >> "%HOSTS_FILE%"
    echo 127.0.0.1  app.edulearn.local >> "%HOSTS_FILE%"
    echo 127.0.0.1  mail.edulearn.local >> "%HOSTS_FILE%"
    echo 127.0.0.1  api.edulearn.local >> "%HOSTS_FILE%"
    echo [THANH CONG] Da ghi them cac ten mien vao %HOSTS_FILE%
)

echo.
echo Dang xoa cache DNS...
ipconfig /flushdns >nul
echo.
echo ============================================================
echo [HOAN TAT] Ban co the truy cap ngay:
echo   - Tên miền mong muốn: http://LMS-EduLearn
echo   - Hoặc đường dẫn gốc: http://localhost:8088
echo ============================================================
pause
