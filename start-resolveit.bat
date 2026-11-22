@echo off
REM ResolveIT one-click start: backend (port 8080) and frontend (port 5173) in their own windows, then opens the site.
REM PostgreSQL must be running (the Windows service "postgresql-x64-18" starts automatically with Windows).
cd /d "%~dp0"
sc query postgresql-x64-18 | find "RUNNING" >nul || echo WARNING: PostgreSQL service is not running. Start it from Services (services.msc).
if not exist "frontend\node_modules" (
  echo Installing frontend packages the first time...
  call npm --prefix frontend install
)
start "ResolveIT backend" cmd /k "cd /d %~dp0backend && mvnw.cmd spring-boot:run"
start "ResolveIT frontend" cmd /k "cd /d %~dp0frontend && npm run dev"
echo Waiting for the backend to start...
:wait
timeout /t 3 /nobreak >nul
powershell -NoProfile -Command "try { Invoke-WebRequest -UseBasicParsing http://localhost:8080/api/health -TimeoutSec 2 | Out-Null; exit 0 } catch { exit 1 }"
if errorlevel 1 goto wait
start "" http://localhost:5173
echo ResolveIT is running. Close the two server windows to stop it.
