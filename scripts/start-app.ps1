# ==============================================================================
# NVL Platform - Start Application in Background Mode
# ==============================================================================

$root = (Resolve-Path (Join-Path $PSScriptRoot "..")).Path
$logFile = Join-Path $root "app.log"

# 1. Kiểm tra xem port 8080 đã có ứng dụng chạy chưa
$existing = Get-NetTCPConnection -LocalPort 8080 -State Listen -ErrorAction SilentlyContinue
if ($existing) {
    $pidRunning = ($existing.OwningProcess | Select-Object -Unique)[0]
    Write-Host "[NVL] Ung dung dang chay san tren port 8080 (PID: $pidRunning)." -ForegroundColor Yellow
    Write-Host "[NVL] Dia chi: http://localhost:8080" -ForegroundColor Cyan
    Write-Host "[NVL] Dung 'make restart' neu ban muon khoi dong lai."
    exit 0
}

Write-Host "[NVL] Dang khoi dong ung dung trong nen (Background Mode)..." -ForegroundColor Green

# 2. Làm mới file app.log
"" | Out-File -FilePath $logFile -Encoding utf8 -Force

# 3. Khởi chạy ngầm độc lập qua WMI Win32_Process (tách biệt 100% khỏi terminal caller)
$commandLine = "cmd.exe /c .\gradlew.bat bootRun > `"$logFile`" 2>&1"
$processClass = [wmiclass]"Win32_Process"
$spawnResult = $processClass.Create($commandLine, $root, $null)

if ($spawnResult.ReturnValue -ne 0) {
    Write-Host "[NVL] Loi khi khoi chay tien trinh (Code: $($spawnResult.ReturnValue))" -ForegroundColor Red
    exit 1
}

# 4. Chờ server sẵn sàng (tối đa 30 giây)
Write-Host "[NVL] Dang cho server san sang..." -NoNewline
$isReady = $false
$appPid = $null

for ($i = 0; $i -lt 30; $i++) {
    Start-Sleep -Seconds 1
    $conn = Get-NetTCPConnection -LocalPort 8080 -State Listen -ErrorAction SilentlyContinue
    if ($conn) {
        $isReady = $true
        $appPid = ($conn.OwningProcess | Select-Object -Unique)[0]
        break
    }
    Write-Host "." -NoNewline
}
Write-Host ""

if ($isReady) {
    Write-Host "[NVL] Khoi dong thanh cong! Ung dung da san sang tai http://localhost:8080/portal (PID: $appPid)" -ForegroundColor Green
} else {
    Write-Host "[NVL] Ung dung dang tiep tuc khoi dong trong nen." -ForegroundColor Yellow
    Write-Host "[NVL] Ban co the go 'make logs' de theo doi tien trinh hoac 'make status' de kiem tra." -ForegroundColor Yellow
}

Write-Host "[NVL] Da tra ve terminal. Ban co the tiep tuc nhap lenh moi!" -ForegroundColor Cyan
