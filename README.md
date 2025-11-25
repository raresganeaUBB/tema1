# Event Ticketing System

A comprehensive event management and ticketing system built with Java servlets, Next.js frontend, and PostgreSQL database, deployed across multiple application servers (Tomcat, Jetty, WildFly).

## 🏗️ Architecture

### Backend (Java Servlets)

- **Event Management Servlet**: Manages events, venues, and ticket types
- **Booking & Payment Servlet**: Handles bookings, payments, and seat reservations
- **HTTP Communication**: Servlets communicate via REST APIs
- **Database**: PostgreSQL with HikariCP connection pooling

### Frontend (Next.js)

- **React 18** with TypeScript
- **Tailwind CSS** for styling
- **Framer Motion** for animations
- **Zod** for validation
- **React Hook Form** for form handling

### Deployment

- **Docker Compose** orchestration
- **Multiple Application Servers**: Tomcat, Jetty, WildFly
- **Nginx** load balancer (optional)
- **PostgreSQL** database

## 🚀 Quick Start

### Prerequisites

- Docker & Docker Compose
- Java 17+
- Maven 3.6+
- Node.js 18+ (for local development)

### Option 1: Using Deployment Scripts (Recommended)

#### Linux/macOS

```bash
# Make script executable
chmod +x deploy.sh

# Build and start everything
./deploy.sh start

# Or step by step
./deploy.sh build      # Build backend and frontend
./deploy.sh start      # Start all services
./deploy.sh status     # Check service status
./deploy.sh logs       # View logs
./deploy.sh stop       # Stop services
./deploy.sh cleanup    # Clean up everything
```

#### Windows

```cmd
# Build and start everything
deploy.bat start

# Or step by step
deploy.bat build       # Build backend and frontend
deploy.bat start       # Start all services
deploy.bat status      # Check service status
deploy.bat logs        # View logs
deploy.bat stop        # Stop services
deploy.bat cleanup     # Clean up everything
```

### Option 2: Manual Deployment

1. **Build Backend**

   ```bash
   cd backend
   mvn clean install
   cd ..
   ```

2. **Start Services**

   ```bash
   docker-compose up --build
   ```

3. **Access Application**
   - Frontend: http://localhost:3000
   - Event API: http://localhost:8080/event-servlet/api/events
   - Booking API: http://localhost:8081/booking-servlet/api/bookings

## 🌐 Service Endpoints

### Frontend

- **Main App**: http://localhost:3000
- **Events Page**: http://localhost:3000/events
- **Individual Event**: http://localhost:3000/events/[id]

### Backend APIs

#### Event Management Servlet

- **Tomcat**: http://localhost:8080/event-servlet/api/events
- **Jetty**: http://localhost:8081/event-servlet/api/events
- **WildFly**: http://localhost:8082/event-servlet/api/events

#### Booking & Payment Servlet

- **Tomcat**: http://localhost:8080/booking-servlet/api/bookings
- **Jetty**: http://localhost:8081/booking-servlet/api/bookings
- **WildFly**: http://localhost:8082/booking-servlet/api/bookings

### Database

- **PostgreSQL**: localhost:5432
- **Database**: eventticketing
- **Username**: eventuser
- **Password**: eventpass

## 🔧 Development

### Backend Development

```bash
cd backend
mvn clean install
mvn tomcat7:run  # For local development (requires Tomcat 10+)
```

### Frontend Development

```bash
cd frontend
yarn install
yarn dev
```

### Database Management

```bash
# Connect to database
docker exec -it event-ticketing-postgres psql -U eventuser -d eventticketing

# Run schema
psql -U eventuser -d eventticketing -f database/schema.sql

# Load sample data
psql -U eventuser -d eventticketing -f database/sample-data.sql
```

## 📁 Project Structure

```
event-ticketing-system/
├── backend/
│   ├── shared/                 # Shared Java components
│   ├── event-servlet/          # Event Management Servlet
│   └── booking-servlet/        # Booking & Payment Servlet
├── frontend/
│   ├── src/
│   │   ├── app/               # Next.js App Router
│   │   ├── components/        # React components
│   │   └── lib/               # Utilities
│   └── public/                # Static assets
├── database/
│   ├── schema.sql             # Database schema
│   └── sample-data.sql        # Sample data
├── nginx/
│   └── nginx.conf             # Load balancer config
├── docker-compose.yml         # Docker orchestration
├── deploy.sh                  # Linux/macOS deployment script
├── deploy.bat                 # Windows deployment script
└── README.md                  # This file
```

## 🔄 Servlet Communication

The system implements microservices-style communication between servlets:

1. **Event Validation**: Booking servlet validates events with Event servlet
2. **Capacity Updates**: Booking servlet notifies Event servlet of capacity changes
3. **HTTP REST APIs**: All communication via HTTP REST endpoints
4. **Environment Configuration**: Service URLs configurable via environment variables

## 🐳 Docker Services

| Service  | Port | Description                |
| -------- | ---- | -------------------------- |
| postgres | 5432 | PostgreSQL database        |
| tomcat   | 8080 | Tomcat application server  |
| jetty    | 8081 | Jetty application server   |
| wildfly  | 8082 | WildFly application server |
| frontend | 3000 | Next.js frontend           |
| nginx    | 80   | Load balancer (optional)   |

## 🛠️ Troubleshooting

### Common Issues

1. **Port Already in Use**

   ```bash
   # Stop conflicting services
   docker-compose down
   # Or kill processes using ports
   lsof -ti:3000 | xargs kill -9
   ```

2. **Build Failures**

   ```bash
   # Clean and rebuild
   ./deploy.sh cleanup
   ./deploy.sh build
   ```

3. **Database Connection Issues**

   ```bash
   # Check database health
   docker-compose ps
   # View database logs
   docker-compose logs postgres
   ```

4. **Frontend Not Loading**
   ```bash
   # Check frontend logs
   docker-compose logs frontend
   # Rebuild frontend
   ./deploy.sh build-frontend
   ```

### Health Checks

```bash
# Check all services
./deploy.sh health

# Check specific service logs
./deploy.sh logs postgres
./deploy.sh logs frontend
./deploy.sh logs tomcat
```

## 📊 API Documentation

### Event Management API

#### Get All Events

```http
GET /api/events/
```

#### Get Event by ID

```http
GET /api/events/{id}
```

#### Create Event

```http
POST /api/events/
Content-Type: application/json

{
  "name": "Concert",
  "description": "Amazing concert",
  "eventDate": "2024-12-31T20:00:00",
  "venue": "Madison Square Garden",
  "city": "New York",
  "country": "USA",
  "capacity": 20000,
  "ticketPrice": 150.00,
  "category": "Music",
  "status": "ACTIVE"
}
```

### Booking API

#### Get All Bookings

```http
GET /api/bookings/
```

#### Create Booking

```http
POST /api/bookings/
Content-Type: application/json

{
  "eventId": 1,
  "userId": 1,
  "seatId": 1,
  "quantity": 2
}
```

## 🎯 Features

### ✅ Implemented

- [x] Two communicating Java servlets
- [x] Deployment on Tomcat, Jetty, WildFly
- [x] PostgreSQL database with complete schema
- [x] Next.js frontend with React, TypeScript
- [x] Docker orchestration
- [x] REST API endpoints
- [x] Event browsing and booking
- [x] HTTP communication between servlets
- [x] Deployment scripts
- [x] Health checks and monitoring

### 🔄 Servlet Communication Features

- [x] Event validation before booking
- [x] Real-time capacity updates
- [x] Cross-service error handling
- [x] Environment-based service discovery

## 📝 License

This project is created for educational purposes as part of a database and servlet project.

## 🤝 Contributing

This is a project for academic purposes. For questions or issues, please refer to the project documentation or contact the development team.

---

**Event Ticketing System** - Your gateway to amazing events! 🎫
