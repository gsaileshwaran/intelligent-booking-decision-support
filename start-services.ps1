# PowerShell Startup Script for PVK Cinemas Platform
Write-Host "============================================================" -ForegroundColor Cyan
Write-Host "       Starting PVK Cinemas Intelligent Booking System      " -ForegroundColor Cyan
Write-Host "============================================================" -ForegroundColor Cyan

$ROOT_DIR = Get-Location

# 1. Start Python Search Service
Write-Host "`n[1/3] Starting Python Hybrid Search Service (Port 8001)..." -ForegroundColor Yellow
Start-Process powershell -ArgumentList "-NoExit", "-Command", "cd '$ROOT_DIR\search-service'; python -m uvicorn app.main:app --host 127.0.0.1 --port 8001 --reload"

# 2. Start Spring Boot Backend
Write-Host "[2/3] Starting Spring Boot Backend API (Port 8080)..." -ForegroundColor Yellow
Start-Process powershell -ArgumentList "-NoExit", "-Command", "cd '$ROOT_DIR\backend'; mvn spring-boot:run"

# 3. Start Frontend UI
Write-Host "[3/3] Starting Frontend Web UI (Port 3000)..." -ForegroundColor Yellow
Start-Process powershell -ArgumentList "-NoExit", "-Command", "cd '$ROOT_DIR\frontend'; npm run dev"

Write-Host "`n============================================================" -ForegroundColor Green
Write-Host "   All services launched! Access the UI at:                 " -ForegroundColor Green
Write-Host "   http://localhost:3000                                    " -ForegroundColor White -BackgroundColor DarkGreen
Write-Host "============================================================" -ForegroundColor Green
