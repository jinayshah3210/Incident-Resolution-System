@echo off
REM Recreates the ResolveIT database (DESTROYS its data). Requires psql on PATH and the postgres superuser password.
setlocal
if "%DB_NAME%"=="" set DB_NAME=resolveit
if "%DB_USER%"=="" set DB_USER=resolveit
set /p answer=This drops and recreates database %DB_NAME%. Continue? [y/N] 
if /i not "%answer%"=="y" exit /b 1
psql -h localhost -U postgres -c "DROP DATABASE IF EXISTS %DB_NAME%;" -c "CREATE DATABASE %DB_NAME% OWNER %DB_USER%;"
echo Database recreated. Start the backend to apply migrations and demo accounts.
