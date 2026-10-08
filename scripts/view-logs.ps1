# ==============================================================================
# NVL Platform - Live Log Viewer with Color Highlighting & Error Filtering
# ==============================================================================

param (
    [switch]$ErrorOnly,
    [int]$Lines = 60
)

$root = (Resolve-Path (Join-Path $PSScriptRoot "..")).Path
$logFile = Join-Path $root "app.log"

if (-not (Test-Path $logFile)) {
    Write-Host "[NVL] Chua tim thay file app.log. Hay chay 'make up' truoc!" -ForegroundColor Yellow
    exit 0
}

function Format-LogLine ($line) {
    if ($line -match "ERROR" -or $line -match "Exception" -or $line -match "Caused by:" -or $line -match "FATAL") {
        Write-Host $line -ForegroundColor Red
    } elseif ($line -match "WARN") {
        Write-Host $line -ForegroundColor Yellow
    } elseif ($line -match "INFO") {
        Write-Host $line -ForegroundColor White
    } elseif ($line -match "DEBUG") {
        Write-Host $line -ForegroundColor DarkGray
    } else {
        # Stack trace line or continuation
        if ($line -match "^\s+at\s+" -or $line -match "^\s+\.\.\.\s+") {
            Write-Host $line -ForegroundColor Magenta
        } else {
            Write-Host $line -ForegroundColor Gray
        }
    }
}

if ($ErrorOnly) {
    Write-Host "==============================================================================" -ForegroundColor Red
    Write-Host "  NVL Spring Boot - Danh sach cac loi (ERROR / Exception) trong log" -ForegroundColor Red
    Write-Host "==============================================================================" -ForegroundColor Red

    $errorLines = Get-Content $logFile | Where-Object {
        $_ -match "ERROR" -or $_ -match "Exception" -or $_ -match "Caused by:" -or $_ -match "^\s+at\s+"
    }

    if ($errorLines) {
        $errorLines | Select-Object -Last 100 | ForEach-Object { Format-LogLine $_ }
    } else {
        Write-Host "[NVL] Khong co loi ERROR hay Exception nao trong app.log!" -ForegroundColor Green
    }
    exit 0
}

Write-Host "==============================================================================" -ForegroundColor Cyan
Write-Host "  NVL Spring Boot - Theo doi log thoi gian thuc (Realtime Logs)" -ForegroundColor Cyan
Write-Host "  * Nhan Ctrl+C de thoat xem log (ung dung van chay ngam binh thuong)" -ForegroundColor Yellow
Write-Host "  * Go 'make logs-error' de chi xem cac dong loi" -ForegroundColor DarkCyan
Write-Host "==============================================================================" -ForegroundColor Cyan

# 1. In cac dong cuoi cung
$initial = Get-Content $logFile -Tail $Lines
foreach ($l in $initial) {
    Format-LogLine $l
}

# 2. Theo doi lien tuc cac dong moi (tuong tu tail -f)
try {
    Get-Content $logFile -Tail 0 -Wait | ForEach-Object {
        Format-LogLine $_
    }
} catch {
    # Nguoi dung an Ctrl+C
    Write-Host "`n[NVL] Da thoat che do xem log. Ung dung van dang chay." -ForegroundColor Cyan
}
