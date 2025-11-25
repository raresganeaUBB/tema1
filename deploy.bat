@echo off
REM Event Ticketing System - Windows Deployment Script
REM This script helps deploy and manage the Event Ticketing System on Windows

setlocal enabledelayedexpansion

REM Project configuration
set PROJECT_NAME=event-ticketing
set BACKEND_DIR=backend
set FRONTEND_DIR=frontend
set DOCKER_COMPOSE_FILE=docker-compose.yml

REM Function to print colored output (simplified for Windows)
:print_status
echo [INFO] %~1
goto :eof

:print_success
echo [SUCCESS] %~1
goto :eof

:print_warning
echo [WARNING] %~1
goto :eof

:print_error
echo [ERROR] %~1
goto :eof

REM Function to check if command exists
:command_exists
where %1 >nul 2>&1
if %errorlevel% equ 0 (
    exit /b 0
) else (
    exit /b 1
)

REM Function to check prerequisites
:check_prerequisites
call :print_status "Checking prerequisites..."

set missing_deps=

where docker >nul 2>&1
if %errorlevel% neq 0 (
    set missing_deps=%missing_deps% docker
)

where docker-compose >nul 2>&1
if %errorlevel% neq 0 (
    set missing_deps=%missing_deps% docker-compose
)

where mvn >nul 2>&1
if %errorlevel% neq 0 (
    set missing_deps=%missing_deps% maven
)

where java >nul 2>&1
if %errorlevel% neq 0 (
    set missing_deps=%missing_deps% java
)

if not "%missing_deps%"=="" (
    call :print_error "Missing dependencies:%missing_deps%"
    call :print_error "Please install the missing dependencies and try again."
    exit /b 1
)

call :print_success "All prerequisites are installed"
goto :eof

REM Function to build backend
:build_backend
call :print_status "Building backend servlets..."

if not exist "%BACKEND_DIR%" (
    call :print_error "Backend directory not found!"
    exit /b 1
)

cd "%BACKEND_DIR%"

call :print_status "Running Maven clean install..."
call mvn clean install

if %errorlevel% equ 0 (
    call :print_success "Backend build completed successfully"
) else (
    call :print_error "Backend build failed!"
    exit /b 1
)

cd ..
goto :eof

REM Function to build frontend
:build_frontend
call :print_status "Building frontend..."

if not exist "%FRONTEND_DIR%" (
    call :print_error "Frontend directory not found!"
    exit /b 1
)

cd "%FRONTEND_DIR%"

if not exist "package.json" (
    call :print_error "package.json not found in frontend directory!"
    exit /b 1
)

call :print_status "Installing dependencies..."
where yarn >nul 2>&1
if %errorlevel% equ 0 (
    call yarn install
) else (
    call npm install
)

call :print_status "Building Next.js application..."
where yarn >nul 2>&1
if %errorlevel% equ 0 (
    call yarn build
) else (
    call npm run build
)

if %errorlevel% equ 0 (
    call :print_success "Frontend build completed successfully"
) else (
    call :print_error "Frontend build failed!"
    exit /b 1
)

cd ..
goto :eof

REM Function to start services
:start_services
call :print_status "Starting services with Docker Compose..."

if not exist "%DOCKER_COMPOSE_FILE%" (
    call :print_error "docker-compose.yml not found!"
    exit /b 1
)

call docker-compose up --build -d

if %errorlevel% equ 0 (
    call :print_success "Services started successfully"
    call :print_status "Waiting for services to be ready..."
    timeout /t 10 /nobreak >nul
    
    REM Check service health
    call :check_service_health
) else (
    call :print_error "Failed to start services!"
    exit /b 1
)
goto :eof

REM Function to check service health
:check_service_health
call :print_status "Checking service health..."

set services=postgres tomcat jetty wildfly frontend
set healthy_services=0

for %%s in (%services%) do (
    docker-compose ps | findstr /i "%%s.*healthy\|%%s.*Up" >nul
    if !errorlevel! equ 0 (
        call :print_success "%%s is running"
        set /a healthy_services+=1
    ) else (
        call :print_warning "%%s is not healthy"
    )
)

call :print_status "Health check complete: !healthy_services!/5 services healthy"
goto :eof

REM Function to stop services
:stop_services
call :print_status "Stopping services..."

call docker-compose down

if %errorlevel% equ 0 (
    call :print_success "Services stopped successfully"
) else (
    call :print_error "Failed to stop services!"
    exit /b 1
)
goto :eof

REM Function to show logs
:show_logs
set service=%1
if "%service%"=="" (
    call :print_status "Showing logs for all services..."
    call docker-compose logs -f
) else (
    call :print_status "Showing logs for %service%..."
    call docker-compose logs -f %service%
)
goto :eof

REM Function to show service status
:show_status
call :print_status "Service status:"
call docker-compose ps
goto :eof

REM Function to clean up
:cleanup
call :print_status "Cleaning up..."

call :print_status "Stopping services..."
call docker-compose down -v

call :print_status "Removing unused Docker images..."
call docker image prune -f

call :print_status "Cleaning Maven build artifacts..."
if exist "%BACKEND_DIR%" (
    cd "%BACKEND_DIR%"
    call mvn clean
    cd ..
)

call :print_success "Cleanup completed"
goto :eof

REM Function to show help
:show_help
echo Event Ticketing System - Windows Deployment Script
echo.
echo Usage: %0 [COMMAND]
echo.
echo Commands:
echo   build       Build both backend and frontend
echo   build-backend   Build only backend servlets
echo   build-frontend  Build only frontend
echo   start        Start all services
echo   stop         Stop all services
echo   restart      Restart all services
echo   status       Show service status
echo   logs [service] Show logs (optionally for specific service)
echo   health       Check service health
echo   cleanup      Clean up all resources
echo   help         Show this help message
echo.
echo Examples:
echo   %0 build     # Build everything
echo   %0 start     # Start all services
echo   %0 logs frontend  # Show frontend logs
echo   %0 cleanup   # Clean up everything
goto :eof

REM Main script logic
:main
set command=%1
if "%command%"=="" set command=help

if "%command%"=="build" (
    call :check_prerequisites
    call :build_backend
    call :build_frontend
    call :print_success "Build completed successfully!"
    call :print_status "Run '%0 start' to start the services"
    goto :eof
)

if "%command%"=="build-backend" (
    call :check_prerequisites
    call :build_backend
    goto :eof
)

if "%command%"=="build-frontend" (
    call :check_prerequisites
    call :build_frontend
    goto :eof
)

if "%command%"=="start" (
    call :check_prerequisites
    call :build_backend
    call :start_services
    call :print_success "Event Ticketing System is now running!"
    call :print_status "Access the application at:"
    call :print_status "  Frontend: http://localhost:3000"
    call :print_status "  Event API: http://localhost:8080/event-servlet/api/events"
    call :print_status "  Booking API: http://localhost:8081/booking-servlet/api/bookings"
    goto :eof
)

if "%command%"=="stop" (
    call :stop_services
    goto :eof
)

if "%command%"=="restart" (
    call :stop_services
    timeout /t 2 /nobreak >nul
    call :start_services
    goto :eof
)

if "%command%"=="status" (
    call :show_status
    goto :eof
)

if "%command%"=="logs" (
    call :show_logs %2
    goto :eof
)

if "%command%"=="health" (
    call :check_service_health
    goto :eof
)

if "%command%"=="cleanup" (
    call :cleanup
    goto :eof
)

if "%command%"=="help" (
    call :show_help
    goto :eof
)

call :show_help
goto :eof

REM Run main function
call :main %*
