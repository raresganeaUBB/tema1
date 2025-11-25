#!/bin/bash

# Event Ticketing System - Deployment Script
# This script helps deploy and manage the Event Ticketing System

set -e

# Colors for output
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
BLUE='\033[0;34m'
NC='\033[0m' # No Color

# Project configuration
PROJECT_NAME="event-ticketing"
BACKEND_DIR="backend"
FRONTEND_DIR="frontend"
DOCKER_COMPOSE_FILE="docker-compose.yml"

# Function to print colored output
print_status() {
    echo -e "${BLUE}[INFO]${NC} $1"
}

print_success() {
    echo -e "${GREEN}[SUCCESS]${NC} $1"
}

print_warning() {
    echo -e "${YELLOW}[WARNING]${NC} $1"
}

print_error() {
    echo -e "${RED}[ERROR]${NC} $1"
}

# Function to check if command exists
command_exists() {
    command -v "$1" >/dev/null 2>&1
}

# Function to check prerequisites
check_prerequisites() {
    print_status "Checking prerequisites..."
    
    local missing_deps=()
    
    if ! command_exists docker; then
        missing_deps+=("docker")
    fi
    
    if ! command_exists docker-compose; then
        missing_deps+=("docker-compose")
    fi
    
    if ! command_exists mvn; then
        missing_deps+=("maven")
    fi
    
    if ! command_exists java; then
        missing_deps+=("java")
    fi
    
    if [ ${#missing_deps[@]} -ne 0 ]; then
        print_error "Missing dependencies: ${missing_deps[*]}"
        print_error "Please install the missing dependencies and try again."
        exit 1
    fi
    
    print_success "All prerequisites are installed"
}

# Function to build backend
build_backend() {
    print_status "Building backend servlets..."
    
    if [ ! -d "$BACKEND_DIR" ]; then
        print_error "Backend directory not found!"
        exit 1
    fi
    
    cd "$BACKEND_DIR"
    
    print_status "Running Maven clean install..."
    mvn clean install
    
    if [ $? -eq 0 ]; then
        print_success "Backend build completed successfully"
    else
        print_error "Backend build failed!"
        exit 1
    fi
    
    cd ..
}

# Function to build frontend
build_frontend() {
    print_status "Building frontend..."
    
    if [ ! -d "$FRONTEND_DIR" ]; then
        print_error "Frontend directory not found!"
        exit 1
    fi
    
    cd "$FRONTEND_DIR"
    
    if [ ! -f "package.json" ]; then
        print_error "package.json not found in frontend directory!"
        exit 1
    fi
    
    print_status "Installing dependencies..."
    if command_exists yarn; then
        yarn install
    else
        npm install
    fi
    
    print_status "Building Next.js application..."
    if command_exists yarn; then
        yarn build
    else
        npm run build
    fi
    
    if [ $? -eq 0 ]; then
        print_success "Frontend build completed successfully"
    else
        print_error "Frontend build failed!"
        exit 1
    fi
    
    cd ..
}

# Function to start services
start_services() {
    print_status "Starting services with Docker Compose..."
    
    if [ ! -f "$DOCKER_COMPOSE_FILE" ]; then
        print_error "docker-compose.yml not found!"
        exit 1
    fi
    
    docker-compose up --build -d
    
    if [ $? -eq 0 ]; then
        print_success "Services started successfully"
        print_status "Waiting for services to be ready..."
        sleep 10
        
        # Check service health
        check_service_health
    else
        print_error "Failed to start services!"
        exit 1
    fi
}

# Function to check service health
check_service_health() {
    print_status "Checking service health..."
    
    local services=("postgres" "tomcat" "jetty" "wildfly" "frontend")
    local healthy_services=0
    
    for service in "${services[@]}"; do
        if docker-compose ps | grep -q "$service.*healthy\|$service.*Up"; then
            print_success "$service is running"
            ((healthy_services++))
        else
            print_warning "$service is not healthy"
        fi
    done
    
    print_status "Health check complete: $healthy_services/${#services[@]} services healthy"
}

# Function to stop services
stop_services() {
    print_status "Stopping services..."
    
    docker-compose down
    
    if [ $? -eq 0 ]; then
        print_success "Services stopped successfully"
    else
        print_error "Failed to stop services!"
        exit 1
    fi
}

# Function to show logs
show_logs() {
    local service=${1:-""}
    
    if [ -n "$service" ]; then
        print_status "Showing logs for $service..."
        docker-compose logs -f "$service"
    else
        print_status "Showing logs for all services..."
        docker-compose logs -f
    fi
}

# Function to show service status
show_status() {
    print_status "Service status:"
    docker-compose ps
}

# Function to clean up
cleanup() {
    print_status "Cleaning up..."
    
    print_status "Stopping services..."
    docker-compose down -v
    
    print_status "Removing unused Docker images..."
    docker image prune -f
    
    print_status "Cleaning Maven build artifacts..."
    if [ -d "$BACKEND_DIR" ]; then
        cd "$BACKEND_DIR"
        mvn clean
        cd ..
    fi
    
    print_success "Cleanup completed"
}

# Function to show help
show_help() {
    echo "Event Ticketing System - Deployment Script"
    echo ""
    echo "Usage: $0 [COMMAND]"
    echo ""
    echo "Commands:"
    echo "  build       Build both backend and frontend"
    echo "  build-backend   Build only backend servlets"
    echo "  build-frontend  Build only frontend"
    echo "  start        Start all services"
    echo "  stop         Stop all services"
    echo "  restart      Restart all services"
    echo "  status       Show service status"
    echo "  logs [service] Show logs (optionally for specific service)"
    echo "  health       Check service health"
    echo "  cleanup      Clean up all resources"
    echo "  help         Show this help message"
    echo ""
    echo "Examples:"
    echo "  $0 build     # Build everything"
    echo "  $0 start     # Start all services"
    echo "  $0 logs frontend  # Show frontend logs"
    echo "  $0 cleanup   # Clean up everything"
}

# Main script logic
main() {
    case "${1:-help}" in
        "build")
            check_prerequisites
            build_backend
            build_frontend
            print_success "Build completed successfully!"
            print_status "Run '$0 start' to start the services"
            ;;
        "build-backend")
            check_prerequisites
            build_backend
            ;;
        "build-frontend")
            check_prerequisites
            build_frontend
            ;;
        "start")
            check_prerequisites
            build_backend
            start_services
            print_success "Event Ticketing System is now running!"
            print_status "Access the application at:"
            print_status "  Frontend: http://localhost:3000"
            print_status "  Event API: http://localhost:8080/event-servlet/api/events"
            print_status "  Booking API: http://localhost:8081/booking-servlet/api/bookings"
            ;;
        "stop")
            stop_services
            ;;
        "restart")
            stop_services
            sleep 2
            start_services
            ;;
        "status")
            show_status
            ;;
        "logs")
            show_logs "$2"
            ;;
        "health")
            check_service_health
            ;;
        "cleanup")
            cleanup
            ;;
        "help"|*)
            show_help
            ;;
    esac
}

# Run main function with all arguments
main "$@"
