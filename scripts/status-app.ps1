# ==============================================================================
# NVL Platform - Check Application Status
# ==============================================================================

$conn = Get-NetTCPConnection -LocalPort 8080 -State Listen -ErrorAction SilentlyContinue
if ($conn) {
    $pidRunning = ($conn.OwningProcess | Select-Object -Unique)[0]
    $proc = Get-Process -Id $pidRunning -ErrorAction SilentlyContinue
    Write-Host "[NVL] Trang thai: DANG CHAY" -ForegroundColor Green
    Write-Host "[NVL] Port: 8080 (PID: $pidRunning - $($proc.ProcessName))" -ForegroundColor Cyan
    Write-Host "[NVL] URL : http://localhost:8080" -ForegroundColor Cyan
} else {
    Write-Host "[NVL] Trang thai: DANG TAT (Khong co tien trinh nao tren port 8080)." -ForegroundColor Gray
}
