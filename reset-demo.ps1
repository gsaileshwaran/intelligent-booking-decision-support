#!/usr/bin/env pwsh
#Requires -Version 5.1
#
# PVK CINEMAS - Demo Reset Script (IMP-005)
# One-click demo environment reset
# Usage: .\reset-demo.ps1
#

param(
    [switch]$SkipConfirm,
    [switch]$SkipDataReset,
    [switch]$StartServices
)

$ErrorActionPreference = "Continue"
$ROOT = Split-Path -Parent $MyInvocation.MyCommand.Path

Write-Host ""
Write-Host "==========================================" -ForegroundColor Cyan
Write-Host "  PVK CINEMAS -- DEMO RESET TOOL" -ForegroundColor Cyan  
Write-Host "==========================================" -ForegroundColor Cyan
Write-Host ""

if (-not $SkipConfirm) {
    $confirm = Read-Host "This will reset the database to demo state. Continue? [y/N]"
    if ($confirm -notin @('y','Y','yes','Yes')) {
        Write-Host "Reset cancelled." -ForegroundColor Yellow
        exit 0
    }
}

# ---- Step 1: Stop all services ----
Write-Host ""
Write-Host "[1/5] Stopping services..." -ForegroundColor Yellow

$ports = @(8080, 8001, 3000, 5173)
foreach ($port in $ports) {
    try {
        $conns = Get-NetTCPConnection -LocalPort $port -State Listen -ErrorAction SilentlyContinue
        foreach ($conn in $conns) {
            $p = $conn.OwningProcess
            if ($p -and $p -gt 0) {
                Stop-Process -Id $p -Force -ErrorAction SilentlyContinue
                Write-Host "  [+] Stopped PID $p on port $port" -ForegroundColor Gray
            }
        }
    } catch {}
}

Start-Sleep -Seconds 2
Write-Host "  [+] Services stopped" -ForegroundColor Green

# ---- Step 2: Safe demo data reset (Phase 1 master data) ----
if (-not $SkipDataReset) {
    Write-Host ""
    Write-Host "[2/5] Resetting demo data (Phase 1 deterministic reset)..." -ForegroundColor Yellow
    
    $pyScript = Join-Path $ROOT "database\phase1_reset.py"
    if (Test-Path $pyScript) {
        $envFile = Join-Path $ROOT ".env"
        $res = python $pyScript --env-file "$envFile" 2>&1
        if ($LASTEXITCODE -eq 0) {
            Write-Host "  [+] Demo master data safely restored without touching Flyway history" -ForegroundColor Green
        } else {
            Write-Host "  [!] phase1_reset.py returned exit code $LASTEXITCODE" -ForegroundColor Yellow
            Write-Host "$res" -ForegroundColor Gray
        }
    } else {
        Write-Host "  [!] phase1_reset.py not found at $pyScript" -ForegroundColor Yellow
    }
} else {
    Write-Host ""
    Write-Host "[2/5] Skipping data reset (--SkipDataReset)" -ForegroundColor Gray
}

# ---- Step 3: Start backend (Spring Boot + Flyway V3) ----
Write-Host ""
Write-Host "[3/5] Starting backend (Spring Boot on :8080)..." -ForegroundColor Yellow

$backendPath = Join-Path $ROOT "backend"
$backendJob = Start-Process -FilePath "cmd.exe" -ArgumentList "/c mvn spring-boot:run" -WorkingDirectory $backendPath -PassThru -WindowStyle Minimized
Write-Host "  [+] Backend process started (PID: $($backendJob.Id)) -- Flyway V3 will run on startup" -ForegroundColor Green

Write-Host "  Waiting 25s for backend to initialize..." -ForegroundColor Gray
Start-Sleep -Seconds 25

# Check if backend is responsive
try {
    $health = Invoke-RestMethod -Uri "http://localhost:8080/api/v1/movies" -TimeoutSec 5 -ErrorAction Stop
    Write-Host "  [+] Backend is responsive" -ForegroundColor Green
} catch {
    Write-Host "  [!] Backend not yet ready - it may still be loading" -ForegroundColor Yellow
}

# ---- Step 4: Start Python search microservice ----
Write-Host ""
Write-Host "[4/5] Starting Python search service (FastAPI on :8001)..." -ForegroundColor Yellow

$searchPath = Join-Path $ROOT "search-service"
if (Test-Path $searchPath) {
    $searchJob = Start-Process -FilePath "cmd.exe" -ArgumentList "/c python -m uvicorn app.main:app --host 127.0.0.1 --port 8001" -WorkingDirectory $searchPath -PassThru -WindowStyle Minimized
    Write-Host "  [+] Search service started (PID: $($searchJob.Id))" -ForegroundColor Green
} else {
    Write-Host "  [!] search-service directory not found, skipping" -ForegroundColor Yellow
}

Start-Sleep -Seconds 5

# Trigger search index derivation from MySQL
try {
    $reindex = Invoke-RestMethod -Uri "http://localhost:8001/internal/v1/index" -Method Post -TimeoutSec 5 -ErrorAction Stop
    Write-Host "  [+] Search index rebuilt from authoritative MySQL" -ForegroundColor Green
} catch {
    Write-Host "  [!] Search index rebuild deferred" -ForegroundColor Gray
}

# ---- Step 5: Start frontend ----
Write-Host ""
Write-Host "[5/5] Starting frontend (Vite dev on :3000)..." -ForegroundColor Yellow

$frontendPath = Join-Path $ROOT "frontend"
$frontendJob = Start-Process -FilePath "cmd.exe" -ArgumentList "/c npm run dev" -WorkingDirectory $frontendPath -PassThru -WindowStyle Minimized
Write-Host "  [+] Frontend started (PID: $($frontendJob.Id))" -ForegroundColor Green

# ---- Summary ----
Write-Host ""
Write-Host "==========================================" -ForegroundColor Cyan
Write-Host "  DEMO RESET COMPLETE" -ForegroundColor Green
Write-Host "==========================================" -ForegroundColor Cyan
Write-Host ""
Write-Host "  Frontend:  http://localhost:3000" -ForegroundColor White
Write-Host "  Backend:   http://localhost:8080" -ForegroundColor White
Write-Host "  Search AI: http://localhost:8001" -ForegroundColor White
Write-Host ""
Write-Host "  Demo Credentials:" -ForegroundColor Yellow
Write-Host "  Super Admin   : admin@pvkcinemas.com    / Password123!" -ForegroundColor White
Write-Host "  Theatre Mgr   : manager@pvkcinemas.com  / Password123!" -ForegroundColor White
Write-Host "  Customer      : customer@pvkcinemas.com / Password123!" -ForegroundColor White
Write-Host ""
Write-Host "  Movies: Interstellar, Avengers Endgame, Inception," -ForegroundColor Gray
Write-Host "          The Dark Knight, RRR, KGF 2, Pushpa 2," -ForegroundColor Gray
Write-Host "          Kalki 2898 AD, Dune Part Two, Oppenheimer" -ForegroundColor Gray
Write-Host ""
Write-Host "  Wait ~30s after restart for Flyway V3 seed to complete" -ForegroundColor Yellow
Write-Host ""