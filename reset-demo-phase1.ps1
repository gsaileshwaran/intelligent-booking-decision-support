#!/usr/bin/env pwsh
#Requires -Version 5.1
#
# PVK CINEMAS -- Phase 1 Demo Reset Script
# ========================================
# Dataset Version : 2026.10
# Deterministic Seed: PVK-PHASE1-SEED-42
#
# PURPOSE:
#   Safely resets PVK Cinemas Phase 1 demo data (master/reference data only).
#   - NEVER drops the database or tables.
#   - NEVER touches flyway_schema_history.
#   - Cleans operational, transactional, and ephemeral data.
#   - Restores exactly: 5 cities | 25 theatres | 127 screens | 16,386 seats
#   - Restores demo users and theatre manager assignment.
#   - Validates all Phase 1 requirements post-reset.
#   - Safe to execute repeatedly (idempotent).
#
# USAGE:
#   .\reset-demo-phase1.ps1 [-SkipConfirm] [-ValidateOnly]
#
# REQUIRES:
#   Python 3.8+ and PyMySQL:  pip install pymysql
#

param(
    [switch]$SkipConfirm,
    [switch]$ValidateOnly
)

$ErrorActionPreference = "Continue"
$ROOT   = Split-Path -Parent $MyInvocation.MyCommand.Path
$DBDIR  = Join-Path $ROOT "database"
$SCRIPT = Join-Path $DBDIR "phase1_reset.py"

Write-Host ""
Write-Host "============================================================" -ForegroundColor Cyan
Write-Host "  PVK CINEMAS -- PHASE 1A DEMO RESET" -ForegroundColor Cyan
Write-Host "  Dataset Version : 2026.10" -ForegroundColor Cyan
Write-Host "  Seed            : PVK-PHASE1-SEED-42" -ForegroundColor Cyan
Write-Host "  Phase           : 1A (Physical Cinema Realism)" -ForegroundColor Cyan
Write-Host "============================================================" -ForegroundColor Cyan
Write-Host ""

# ---- Confirm ----
if (-not $SkipConfirm -and -not $ValidateOnly) {
    Write-Host "This will reset Phase 1 demo data (cities, theatres, screens, seats," -ForegroundColor Yellow
    Write-Host "demo users) and clear all shows, bookings, and holds." -ForegroundColor Yellow
    Write-Host ""
    $confirm = Read-Host "Continue? [y/N]"
    if ($confirm -notin @('y', 'Y', 'yes', 'Yes')) {
        Write-Host "Reset cancelled." -ForegroundColor Yellow
        exit 0
    }
}

# ---- Pre-flight: check phase1_reset.py ----
if (-not (Test-Path $SCRIPT)) {
    Write-Host "[ERROR] Phase 1 reset script not found: $SCRIPT" -ForegroundColor Red
    Write-Host "        Make sure database\phase1_reset.py exists." -ForegroundColor Red
    exit 1
}

# ---- Pre-flight: check Python ----
$pyCmd = $null
foreach ($candidate in @("python", "python3", "py")) {
    try {
        $ver = & $candidate --version 2>&1
        if ($ver -match "Python 3\.") {
            $pyCmd = $candidate
            break
        }
    } catch {}
}

if (-not $pyCmd) {
    Write-Host "[ERROR] Python 3.x not found on PATH." -ForegroundColor Red
    Write-Host "        Install Python 3.8+ and ensure it is on PATH." -ForegroundColor Red
    exit 1
}

Write-Host "  [+] Python found: $pyCmd" -ForegroundColor Green

# ---- Pre-flight: check pymysql ----
$pymysqlCheck = & $pyCmd -c "import pymysql; print('OK')" 2>&1
if ($pymysqlCheck -ne "OK") {
    Write-Host "[WARN] PyMySQL not installed. Attempting pip install..." -ForegroundColor Yellow
    & $pyCmd -m pip install pymysql --quiet
    $pymysqlCheck = & $pyCmd -c "import pymysql; print('OK')" 2>&1
    if ($pymysqlCheck -ne "OK") {
        Write-Host "[ERROR] Could not install PyMySQL. Run: pip install pymysql" -ForegroundColor Red
        exit 1
    }
}

Write-Host "  [+] PyMySQL available" -ForegroundColor Green

# ---- Load DB config from .env ----
$envFile = Join-Path $ROOT ".env"
if (Test-Path $envFile) {
    Write-Host "  [+] Loading DB config from .env" -ForegroundColor Green
} else {
    Write-Host "  [!] No .env found -- using defaults (root/root, pvk_cinemas_db)" -ForegroundColor Yellow
}

# ---- Run Phase 1 Reset ----
Write-Host ""
if ($ValidateOnly) {
    Write-Host "[VALIDATE ONLY] Running Phase 1 validation checks..." -ForegroundColor Yellow
    $args_extra = "--validate-only"
} else {
    Write-Host "[RESET] Starting Phase 1 data reset..." -ForegroundColor Yellow
    $args_extra = ""
}

Write-Host ""

$proc = Start-Process -FilePath $pyCmd `
    -ArgumentList "$SCRIPT $args_extra --env-file `"$envFile`"" `
    -NoNewWindow -PassThru -Wait

$exitCode = $proc.ExitCode

Write-Host ""

if ($exitCode -eq 0) {
    if ($ValidateOnly) {
        Write-Host "============================================================" -ForegroundColor Green
        Write-Host "  PHASE 1A VALIDATION PASSED" -ForegroundColor Green
        Write-Host "============================================================" -ForegroundColor Green
    } else {
        Write-Host "============================================================" -ForegroundColor Green
        Write-Host "  PHASE 1A RESET COMPLETE" -ForegroundColor Green
        Write-Host "============================================================" -ForegroundColor Green
        Write-Host ""
        Write-Host "  Dataset   : PVK Demo 2026.10 (Phase 1A Realistic Ecosystem)" -ForegroundColor White
        Write-Host "  Cities    : 5  (Chennai, Bengaluru, Hyderabad, Mumbai, Delhi)" -ForegroundColor White
        Write-Host "  Theatres  : 25 (5 per city, varied 3-8 screens each)" -ForegroundColor White
        Write-Host "  Screens   : 127 (Varied formats: IMAX, 4DX, Dolby Atmos, 3D, 2D)" -ForegroundColor White
        Write-Host "  Seats     : 16,386 (Varied screen capacities 64-240 seats)" -ForegroundColor White
        Write-Host ""
        Write-Host "  Demo Credentials (Password: Password123!):" -ForegroundColor Yellow
        Write-Host "  Super Admin   : admin@pvkcinemas.com" -ForegroundColor White
        Write-Host "  Theatre Mgr   : manager@pvkcinemas.com  -> PVK Cineplex Chennai" -ForegroundColor White
        Write-Host "  Customer      : customer@pvkcinemas.com" -ForegroundColor White
        Write-Host ""
        Write-Host "  NOTE: No shows have been generated (Phase 2)." -ForegroundColor Gray
        Write-Host "        Run the application to start the backend: .\start-dev.ps1" -ForegroundColor Gray
    }
} elseif ($exitCode -eq 1) {
    Write-Host "============================================================" -ForegroundColor Red
    Write-Host "  PHASE 1A VALIDATION FAILED" -ForegroundColor Red
    Write-Host "  See output above for details." -ForegroundColor Red
    Write-Host "============================================================" -ForegroundColor Red
} elseif ($exitCode -eq 2) {
    Write-Host "============================================================" -ForegroundColor Red
    Write-Host "  DATABASE CONNECTION FAILED" -ForegroundColor Red
    Write-Host "  Ensure MySQL is running and .env credentials are correct." -ForegroundColor Red
    Write-Host "============================================================" -ForegroundColor Red
} else {
    Write-Host "============================================================" -ForegroundColor Red
    Write-Host "  RESET FAILED (exit code $exitCode)" -ForegroundColor Red
    Write-Host "  See output above for details." -ForegroundColor Red
    Write-Host "============================================================" -ForegroundColor Red
}

Write-Host ""
exit $exitCode
