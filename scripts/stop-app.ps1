# ==============================================================================
# NVL Platform - Stop Application
# ==============================================================================

Write-Host "[NVL] Dang dung ung dung tren port 8080..." -ForegroundColor Yellow

$conn = Get-NetTCPConnection -LocalPort 8080 -State Listen -ErrorAction SilentlyContinue
if ($conn) {
    $pids = $conn.OwningProcess | Select-Object -Unique
    foreach ($p in $pids) {
        try {
            Stop-Process -Id $p -Force -ErrorAction SilentlyContinue
            Write-Host "[NVL] Da dung tien trinh port 8080 (PID: $p)." -ForegroundColor Green
        } catch {
            Write-Host "[NVL] Khong the dung tien trinh PID $p." -ForegroundColor Red
        }
    }
} else {
    Write-Host "[NVL] Khong co tien trinh nao dang lang nghe tren port 8080." -ForegroundColor Gray
}

# Dọn dẹp bất kỳ tiến trình bootRun nào còn chạy ngầm
$lingering = Get-CimInstance Win32_Process | Where-Object { $_.CommandLine -like "*bootRun*" }
foreach ($p in $lingering) {
    try {
        Stop-Process -Id $p.ProcessId -Force -ErrorAction SilentlyContinue
        Write-Host "[NVL] Da dung tien trinh bootRun cu (PID: $($p.ProcessId))." -ForegroundColor Green
    } catch {}
}

# Dừng Gradle Daemon
$root = Resolve-Path (Join-Path $PSScriptRoot "..")
Push-Location $root
try {
    & .\gradlew.bat --stop | Out-Null
} catch {}
Pop-Location

Write-Host "[NVL] Da hoan tat lenh dung ung dung." -ForegroundColor Green
