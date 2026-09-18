#!/usr/bin/env pwsh
# ==============================================================================
# PVK CINEMAS — Clean Development Startup Script
# ==============================================================================
# Launches all required services in background terminals:
#   1. MySQL Database connectivity check (Port 3306)
#   2. Python FastAPI Hybrid Search Microservice (Port 8001)
#   3. Spring Boot Backend API (Port 8080)
#   4. React + Vite Frontend Web UI (Port 3000)
# ==============================================================================

$ROOT_DIR = $PSScriptRoot
if (-not $ROOT_DIR) { $ROOT_DIR = Get-Location }

Write-Host "============================================================" -ForegroundColor Cyan
Write-Host "       PVK CINEMAS — AI DECISION & BOOKING PLATFORM         " -ForegroundColor Cyan
Write-Host "============================================================" -ForegroundColor Cyan

# ------------------------------------------------------------------------------
# 1. Database Prerequisite Check
# ------------------------------------------------------------------------------
Write-Host "`n[1/4] Checking MySQL Database connection on port 3306..." -ForegroundColor Yellow
$mysqlActive = $false
try {
    $tcp = Test-NetConnection -ComputerName 127.0.0.1 -Port 3306 -WarningAction SilentlyContinue
    $mysqlActive = $tcp.TcpTestSucceeded
} catch {
    $mysqlActive = $false
}

if ($mysqlActive) {
    Write-Host "  [+] MySQL is reachable on 127.0.0.1:3306" -ForegroundColor Green
} else {
    Write-Host "  [!] WARNING: MySQL is not listening on 127.0.0.1:3306!" -ForegroundColor Red
    Write-Host "      Please ensure MySQL Server 8.0 service is started." -ForegroundColor Yellow
    Write-Host "      Example: Start-Service MySQL80" -ForegroundColor Yellow
}

# ------------------------------------------------------------------------------
# 2. Start Python Hybrid Search Service
# ------------------------------------------------------------------------------
Write-Host "[2/4] Launching Python Hybrid Search Service (Port 8001)..." -ForegroundColor Yellow
$searchDir = Join-Path $ROOT_DIR "search-service"
Start-Process powershell -ArgumentList "-NoExit", "-Command", "Set-Location '$searchDir'; python -m uvicorn app.main:app --host 127.0.0.1 --port 8001 --reload"

# ------------------------------------------------------------------------------
# 3. Start Spring Boot Backend API
# ------------------------------------------------------------------------------
Write-Host "[3/4] Launching Spring Boot Backend API (Port 8080)..." -ForegroundColor Yellow
$backendDir = Join-Path $ROOT_DIR "backend"
Start-Process powershell -ArgumentList "-NoExit", "-Command", "Set-Location '$backendDir'; mvn spring-boot:run"

# ------------------------------------------------------------------------------
# 4. Start React + Vite Frontend UI
# ------------------------------------------------------------------------------
Write-Host "[4/4] Launching React Vite Frontend UI (Port 3000)..." -ForegroundColor Yellow
$frontendDir = Join-Path $ROOT_DIR "frontend"
Start-Process powershell -ArgumentList "-NoExit", "-Command", "Set-Location '$frontendDir'; npm run dev"

Write-Host "`n============================================================" -ForegroundColor Green
Write-Host "   PVK Cinemas Services Successfully Launched!             " -ForegroundColor Green
Write-Host "------------------------------------------------------------" -ForegroundColor Green
Write-Host "   Frontend Web UI:   http://localhost:3000                 " -ForegroundColor White
Write-Host "   Backend REST API:  http://127.0.0.1:8080/api/v1          " -ForegroundColor White
Write-Host "   Search OpenAPI:    http://127.0.0.1:8001/docs            " -ForegroundColor White
Write-Host "============================================================`n" -ForegroundColor Green
