@echo off
chcp 65001 >nul
title NVL Platform - Local Signing Agent (Ban Co Yeu Chinh Phu)
echo ===================================================================
echo   NVL PLATFORM - LOCAL SIGNING AGENT (BAN CƠ YẾU CHÍNH PHỦ - VGCA)
echo   Dịch vụ kết nối USB Token Ban Cơ yếu Chính phủ qua cổng 8989
echo ===================================================================
echo.
cd /d "%~dp0"
python local_signing_agent.py
if %ERRORLEVEL% NEQ 0 (
    echo.
    echo [!] Không tìm thấy Python hoặc có lỗi xảy ra.
    echo     Vui lòng đảm bảo đã cài đặt Python 3 từ https://www.python.org
    pause
)

