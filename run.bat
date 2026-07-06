@echo off
title AI Policy Guardian Stack Launcher
echo ===================================================
echo 🛡️ Starting AI Policy Guardian Full-Stack Services
echo ===================================================

echo Loading environment variables from .env ...
if exist .env (
    for /f "usebackq eol=# tokens=1* delims==" %%i in (".env") do (
        set "%%i=%%j"
    )
    echo Environment variables loaded successfully!
) else (
    echo [WARNING] .env file not found. Running with default/in-memory configuration.
)

echo.
echo [1/2] Starting Spring Boot Backend (Port 8080)...
start "AI Policy Guardian - Backend" cmd /k "cd backend && .\mvnw.cmd spring-boot:run"

echo [2/2] Starting Vite React Frontend (Port 5173)...
start "AI Policy Guardian - Frontend" cmd /k "cd frontend && npm run dev"

echo ===================================================
echo 🎉 Both services are launching in separate windows!
echo 🔗 Backend API URL: http://localhost:8080
echo 🔗 Frontend Portal: http://localhost:5173
echo ===================================================
pause
