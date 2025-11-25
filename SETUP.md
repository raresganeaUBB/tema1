# Event Management & Ticketing System - Setup Guide

This guide will help you set up and run the complete Event Management & Ticketing System on your local machine.

## 🏗️ Project Architecture

```
┌─────────────────┐    ┌─────────────────┐    ┌─────────────────┐
│   Next.js App   │    │   Next.js App   │    │   Next.js App   │
│   (Frontend)    │    │   (Frontend)    │    │   (Frontend)    │
└─────────┬───────┘    └─────────┬───────┘    └─────────┬───────┘
          │                      │                      │
          └──────────────────────┼──────────────────────┘
                                 │
                    ┌────────────┴────────────┐
                    │      Load Balancer      │
                    │     (nginx/HAProxy)     │
                    └────────────┬────────────┘
                                 │
        ┌────────────────────────┼────────────────────────┐
        │                       │                        │
┌───────▼────────┐    ┌─────────▼────────┐    ┌─────────▼────────┐
│ Event Servlet  │    │ Booking Servlet  │    │  PostgreSQL DB   │
│   (Tomcat)     │◄──►│   (Jetty)        │    │   (Database)     │
│   Port: 8080   │    │   Port: 8081     │    │   Port: 5432     │
└────────────────┘    └──────────────────┘    └──────────────────┘
```

## 📋 Prerequisites

Before you begin, ensure you have the following installed:

- **Docker & Docker Compose** (Recommended for easy setup)
- **Node.js 18+** (for frontend development)
- **Java 17+** (for backend development)
- **Maven 3.8+** (for building Java projects)
- **Git** (for version control)

## 🚀 Quick Start with Docker (Recommended)

### 1. Clone the Repository

```bash
git clone <your-repo-url>
cd DataBaseProject
```

### 2. Start All Services

```bash
# Start all services (PostgreSQL, Tomcat, Jetty, WildFly, Frontend)
docker-compose up --build -d

# Check if all services are running
docker-compose ps
```

### 3. Access the Application

- **Frontend**: http://localhost:3000
- **Tomcat (Event Servlet)**: http://localhost:8080/event-servlet
- **Jetty (Booking Servlet)**: http://localhost:8081/booking-servlet
- **WildFly (Both Servlets)**: http://localhost:8082/event-servlet & http://localhost:8082/booking-servlet
- **PostgreSQL Database**: localhost:5432

### 4. Verify Installation

```bash
# Check service health
curl http://localhost:8080/event-servlet/health
curl http://localhost:8081/booking-servlet/health

# Check database connection
docker-compose exec postgres psql -U eventuser -d eventticketing -c "SELECT COUNT(*) FROM events;"
```

## 🛠️ Manual Setup (Development Mode)

### 1. Database Setup

```bash
# Start PostgreSQL with Docker
docker run --name event-postgres -e POSTGRES_PASSWORD=eventpass -e POSTGRES_DB=eventticketing -e POSTGRES_USER=eventuser -p 5432:5432 -d postgres:15

# Wait for PostgreSQL to start (about 10 seconds)
sleep 10

# Import database schema
PGPASSWORD=eventpass psql -h localhost -U eventuser -d eventticketing -f database/schema.sql
PGPASSWORD=eventpass psql -h localhost -U eventuser -d eventticketing -f database/sample-data.sql
```

### 2. Backend Setup

**Option A: Using Docker (Recommended - Easiest)**

```bash
# Start all services with Docker
docker-compose up -d

# This automatically starts:
# - PostgreSQL database
# - Tomcat with Event Servlet (port 8080)
# - Jetty with Booking Servlet (port 8081)
# - Frontend (port 3000)
```

**Option B: Manual Setup (For Development)**

**Important: Stop Docker containers first if they're running!**

```bash
# Stop Docker containers to free up ports
docker-compose stop jetty tomcat postgres
# OR stop all: docker-compose down

# 1. Build shared module first
cd backend/shared
mvn clean install

# 2. Start Event Servlet with Embedded Tomcat 10 (Terminal 1)
cd ../event-servlet
mvn clean package
mvn exec:java -Dexec.mainClass="com.eventticketing.event.servlet.EmbeddedTomcat"
# Servlet will be available at: http://localhost:8080/event-servlet
# Note: Uses embedded Tomcat 10 (supports Jakarta Servlet API)
# Alternative: Run EmbeddedTomcat.java directly from your IDE

# 3. Start Booking Servlet with Jetty (Terminal 2)
cd ../booking-servlet
mvn clean package
mvn jetty:run
# Servlet will be available at: http://localhost:8081/booking-servlet

# Important Notes:
# - Event Servlet uses Embedded Tomcat 10 (mvn exec:java) - Port 8080
# - Booking Servlet uses Jetty 11 (mvn jetty:run) - Port 8081
# - Do NOT run jetty:run in event-servlet directory (wrong server!)
# - Do NOT run exec:java in booking-servlet directory (wrong server!)
# - Stop Docker containers first: docker-compose stop tomcat jetty
# - No need to install Jetty/Tomcat separately - embedded versions are used
# - Tomcat 7 doesn't support Jakarta Servlet API - using Tomcat 10 embedded
# - For production: Use Docker (docker-compose up) - recommended!
```

### 3. Frontend Setup

```bash
# Install dependencies
cd frontend
yarn install

# Start development server
yarn dev
```

## 🔧 Configuration

### Environment Variables

#### Backend (Java)

```bash
# Database Configuration
export DATABASE_URL="jdbc:postgresql://localhost:5432/eventticketing"
export DATABASE_USER="eventuser"
export DATABASE_PASSWORD="eventpass"

# Servlet Communication
export EVENT_SERVLET_URL="http://localhost:8080/event-servlet"
export BOOKING_SERVLET_URL="http://localhost:8081/booking-servlet"
```

#### Frontend (Next.js)

```bash
# API Endpoints
export NEXT_PUBLIC_API_URL="http://localhost:8080"
export NEXT_PUBLIC_BOOKING_API_URL="http://localhost:8081"
```

### Database Configuration

The system uses PostgreSQL 15 with the following default settings:

- **Host**: localhost
- **Port**: 5432
- **Database**: eventticketing
- **Username**: eventuser
- **Password**: eventpass

## 📊 Database Schema

The system includes the following main tables:

- `events` - Event information
- `venues` - Venue details
- `ticket_types` - Different ticket categories
- `seats` - Seat information
- `bookings` - Booking records
- `payments` - Payment transactions
- `users` - User accounts

## 🧪 Testing the System

### 1. Test Event Servlet

```bash
# Get all events
curl http://localhost:8080/event-servlet/api/events

# Get event by ID
curl http://localhost:8080/event-servlet/api/events/1

# Search events
curl "http://localhost:8080/event-servlet/api/events/search?q=music&category=Music"
```

### 2. Test Booking Servlet

```bash
# Create a booking
curl -X POST http://localhost:8081/booking-servlet/api/bookings \
  -H "Content-Type: application/json" \
  -d '{"userId": 1, "eventId": 1, "bookingReference": "BK2024001", "totalAmount": 150.00}'

# Get booking by ID
curl http://localhost:8081/booking-servlet/api/bookings/1
```

### 3. Test Frontend

1. Open http://localhost:3000
2. Browse events
3. Try the search functionality
4. Test responsive design on different screen sizes

## 🐛 Troubleshooting

### Common Issues

#### 1. Port Already in Use

```bash
# Find process using port
lsof -i :8080
lsof -i :8081
lsof -i :3000

# Kill process
kill -9 <PID>
```

#### 2. Database Connection Issues

```bash
# Check PostgreSQL status
docker-compose logs postgres

# Restart PostgreSQL
docker-compose restart postgres

# Check database exists
docker-compose exec postgres psql -U eventuser -d eventticketing -c "\l"
```

#### 3. Servlet Not Starting

```bash
# Check Java version
java -version

# Check Maven version
mvn -version

# Clean and rebuild
mvn clean package -DskipTests
```

#### 4. Frontend Build Issues

```bash
# Clear Next.js cache
rm -rf .next

# Clear node_modules
rm -rf node_modules yarn.lock
yarn install

# Check Node.js version
node -v
```

### Logs

#### View All Logs

```bash
docker-compose logs -f
```

#### View Specific Service Logs

```bash
docker-compose logs -f tomcat
docker-compose logs -f jetty
docker-compose logs -f wildfly
docker-compose logs -f frontend
docker-compose logs -f postgres
```

## 📈 Performance Optimization

### Database

- Connection pooling is configured with HikariCP
- Indexes are created on frequently queried columns
- Query optimization for large datasets

### Frontend

- Next.js with App Router for optimal performance
- Image optimization with Next.js Image component
- Code splitting and lazy loading
- React Query for efficient data fetching

### Backend

- Jersey (JAX-RS) for REST API
- Jackson for JSON processing
- Proper error handling and logging
- CORS configuration for cross-origin requests

## 🔒 Security Considerations

- Database credentials are configurable via environment variables
- CORS is properly configured
- Input validation on all API endpoints
- SQL injection prevention with prepared statements
- XSS protection in frontend

## 📝 Development Workflow

### 1. Making Changes

1. Make changes to the code
2. For backend: `mvn clean package`
3. For frontend: `npm run build`
4. Restart the affected services

### 2. Adding New Features

1. Create feature branch
2. Implement changes
3. Test thoroughly
4. Create pull request

### 3. Database Changes

1. Update schema.sql
2. Create migration script
3. Update sample-data.sql if needed
4. Test with fresh database

## 🚀 Deployment

### Production Deployment

1. Set up production database
2. Configure environment variables
3. Build and deploy WAR files to application servers
4. Deploy frontend to CDN or static hosting
5. Configure load balancer if needed

### Docker Production

```bash
# Build production images
docker-compose -f docker-compose.prod.yml build

# Deploy to production
docker-compose -f docker-compose.prod.yml up -d
```

## 📚 API Documentation

### Event Management API (Port 8080)

- `GET /api/events` - List all events
- `GET /api/events/{id}` - Get event details
- `POST /api/events` - Create event
- `PUT /api/events/{id}` - Update event
- `DELETE /api/events/{id}` - Delete event
- `GET /api/events/{id}/ticket-types` - Get ticket types
- `GET /api/events/{id}/seats` - Get available seats

### Booking API (Port 8081)

- `POST /api/bookings` - Create booking
- `GET /api/bookings/{id}` - Get booking details
- `POST /api/payments` - Process payment
- `PUT /api/bookings/{id}/confirm` - Confirm booking

## 🤝 Contributing

1. Fork the repository
2. Create a feature branch
3. Make your changes
4. Add tests if applicable
5. Submit a pull request

## 📄 License

This project is licensed under the MIT License - see the LICENSE file for details.

## 🆘 Support

If you encounter any issues:

1. Check the troubleshooting section
2. Review the logs
3. Create an issue on GitHub
4. Contact the development team

---

**Happy coding! 🎉**
