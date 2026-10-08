# ==============================================================================
# NVL Lesson Plan Platform - Makefile
# ==============================================================================

ifeq ($(OS),Windows_NT)
	GRADLE = gradlew.bat
else
	GRADLE = ./gradlew
endif

.PHONY: help up up-nvl-devtools up-nvl-devtool start run down down-nvl-devtools down-nvl-devtool stop restart logs status build test clean

.DEFAULT_GOAL := help

help:
	@echo ==============================================================================
	@echo   NVL Lesson Plan Platform - Danh sach lenh
	@echo ==============================================================================
	@echo   make up (hoac make start) : Khoi dong du an trong nen (tra ve terminal ngay)
	@echo   make logs                 : Theo doi log Spring Boot truc tiep (to mau loi)
	@echo   make logs-error           : Chi xem cac loi ERROR hoac Exception de debug
	@echo   make logs-clear           : Don dep file log cu de debug phien moi
	@echo   make status               : Kiem tra trang thai ung dung (Port 8080, PID)
	@echo   make down (hoac make stop): Dung ung dung tren port 8080
	@echo   make restart              : Khoi dong lai du an (down -> up)
	@echo   make build                : Build file JAR (bo qua test)
	@echo   make test                 : Chay toan bo test
	@echo   make clean                : Don dep thu muc build
	@echo ==============================================================================

# Khởi động ứng dụng trong nền (Background mode - không chiếm terminal)
up: up-nvl-devtools
up-nvl-devtool: up-nvl-devtools
start: up-nvl-devtools
run: up-nvl-devtools

up-nvl-devtools:
ifeq ($(OS),Windows_NT)
	@powershell -NoProfile -ExecutionPolicy Bypass -File ./scripts/start-app.ps1
else
	@bash -c 'if lsof -Pi :8080 -sTCP:LISTEN -t >/dev/null ; then echo "[NVL] Ung dung dang chay tren port 8080."; else nohup $(GRADLE) bootRun > app.log 2>&1 & echo "[NVL] Da khoi dong trong nen (PID: $$!). Log: app.log"; fi'
endif

# Dừng ứng dụng
down: down-nvl-devtools
down-nvl-devtool: down-nvl-devtools
stop: down-nvl-devtools

down-nvl-devtools:
ifeq ($(OS),Windows_NT)
	@powershell -NoProfile -ExecutionPolicy Bypass -File ./scripts/stop-app.ps1
else
	@echo [NVL] Dang dung ung dung...
	@fuser -k 8080/tcp 2>/dev/null || lsof -ti:8080 | xargs kill -9 2>/dev/null || true
	@$(GRADLE) --stop
	@echo [NVL] Da hoan tat lenh dung ung dung.
endif

# Khởi động lại
restart: down up

# Xem log thời gian thực của Spring Boot (tô màu lỗi đỏ, cảnh báo vàng)
logs:
ifeq ($(OS),Windows_NT)
	@powershell -NoProfile -ExecutionPolicy Bypass -File ./scripts/view-logs.ps1
else
	@tail -f app.log
endif

# Chỉ xem các lỗi ERROR / Exception để debug nhanh
logs-error:
ifeq ($(OS),Windows_NT)
	@powershell -NoProfile -ExecutionPolicy Bypass -File ./scripts/view-logs.ps1 -ErrorOnly
else
	@grep -E "ERROR|Exception|Caused by:" app.log | tail -n 100
endif

logs-err: logs-error

# Xoá nội dung file log cũ để bắt đầu phiên debug mới
logs-clear:
ifeq ($(OS),Windows_NT)
	@powershell -NoProfile -Command "'' | Out-File -FilePath app.log -Encoding utf8 -Force; Write-Host '[NVL] Da lam moi file app.log thanh cong.' -ForegroundColor Green"
else
	@> app.log && echo "[NVL] Da lam moi file app.log thanh cong."
endif

# Kiểm tra trạng thái ứng dụng
status:
ifeq ($(OS),Windows_NT)
	@powershell -NoProfile -ExecutionPolicy Bypass -File ./scripts/status-app.ps1
else
	@bash -c 'if lsof -Pi :8080 -sTCP:LISTEN -t >/dev/null ; then echo "[NVL] Ung dung DANG CHAY tren port 8080"; else echo "[NVL] Ung dung DANG TAT"; fi'
endif

# Build, Test, Clean
build:
	@echo [NVL] Dang build file JAR...
	$(GRADLE) build -x test

test:
	@echo [NVL] Dang chay tests...
	$(GRADLE) test

clean:
	@echo [NVL] Dang don dep thu muc build...
	$(GRADLE) clean
