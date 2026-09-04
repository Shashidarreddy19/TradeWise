# ============================================================
# Trade Backend - Start Script
# Run this from E:\MAJOR PROJECT\backend\
# Usage: .\start.ps1
# ============================================================

$PORT = 8081

Write-Host "Checking for processes on port $PORT..." -ForegroundColor Cyan

# Find and kill any process occupying port 8081
$portInfo = netstat -ano | Select-String ":$PORT\s" | Select-String "LISTENING"
if ($portInfo) {
    $portInfo | ForEach-Object {
        $pid = ($_ -split '\s+')[-1]
        if ($pid -match '^\d+$') {
            Write-Host "Killing process PID $pid on port $PORT..." -ForegroundColor Yellow
            taskkill /PID $pid /F 2>$null
        }
    }
    Start-Sleep -Seconds 2
    Write-Host "Port $PORT is now free." -ForegroundColor Green
} else {
    Write-Host "Port $PORT is already free." -ForegroundColor Green
}

Write-Host ""
Write-Host "Starting Trade Backend on http://localhost:$PORT ..." -ForegroundColor Cyan
Write-Host "Press Ctrl+C to stop the server." -ForegroundColor Gray
Write-Host ""

# Start the Spring Boot application
.\mvnw.cmd spring-boot:run
