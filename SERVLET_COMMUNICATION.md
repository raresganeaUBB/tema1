# Comunicarea între Servleturi

## Prezentare Generală

Aplicația folosește **două servleturi separate** care comunică între ele prin **HTTP REST API**, similar cu o arhitectură de microservicii:

1. **Event Servlet** (event-servlet) - Gestionează evenimentele
2. **Booking Servlet** (booking-servlet) - Gestionează rezervările și plățile

## Servleturi și Configurație

### 1. Event Servlet (Event Management Servlet)

- **Server**: Apache Tomcat 10
- **Port**: 8080
- **Context Path**: `/event-servlet`
- **URL Base**: `http://localhost:8080/event-servlet/api/events`
- **Container Docker**: `event-ticketing-tomcat`
- **Funcționalități**:
  - Gestionează evenimente (create, read, update, delete)
  - Gestionează locații (venues)
  - Gestionează tipuri de bilete (ticket types)
  - Validare evenimente
  - Actualizare capacitate evenimente
  - Autentificare utilizatori (login, register)

### 2. Booking Servlet (Booking and Payment Servlet)

- **Server**: Eclipse Jetty 12
- **Port**: 8081
- **Context Path**: `/booking-servlet`
- **URL Base**: `http://localhost:8081/booking-servlet/api/bookings`
- **Container Docker**: `event-ticketing-jetty`
- **Funcționalități**:
  - Gestionează rezervările (bookings)
  - Gestionează plățile (payments)
  - Gestionează articolele de rezervare (booking items)
  - **Comunică cu Event Servlet** pentru validare și actualizare

## Comunicarea între Servleturi

### Clasa EventService (în Booking Servlet)

Booking Servlet folosește clasa `EventService` pentru a comunica cu Event Servlet prin **HTTP REST API**.

**Locație**: `backend/booking-servlet/src/main/java/com/eventticketing/booking/servlet/EventService.java`

### Metode de Comunicare

#### 1. Validare Eveniment (`validateEvent`)

- **Metodă HTTP**: `GET`
- **Endpoint Event Servlet**: `http://tomcat:8080/event-servlet/api/events/{eventId}`
- **Scop**: Verifică dacă un eveniment există și este disponibil pentru rezervare
- **Utilizare**: Când se creează o rezervare nouă
- **Răspuns**:
  - `200 OK` - Eveniment valid, returnează: `status`, `maxAttendees`, `basePrice`, etc.
  - `404 Not Found` - Eveniment nu există
  - `500 Internal Server Error` - Eroare la server

**Cod în BookingResource**:

```java
EventService.EventValidationResult eventValidation = eventService.validateEvent(request.getEventId());
if (!eventValidation.isValid()) {
    return Response.status(Response.Status.BAD_REQUEST)
            .entity("{\"error\": \"Event validation failed: " + eventValidation.getErrorMessage() + "\"}")
            .build();
}

// Check if event is active
if (!"ACTIVE".equals(eventValidation.getStatus())) {
    return Response.status(Response.Status.BAD_REQUEST)
            .entity("{\"error\": \"Event is not available for booking\"}")
            .build();
}
```

#### 2. Actualizare Capacitate Eveniment (`updateEventCapacity`)

- **Metodă HTTP**: `PATCH`
- **Endpoint Event Servlet**: `http://tomcat:8080/event-servlet/api/events/{eventId}/capacity`
- **Scop**: Actualizează capacitatea evenimentului după o rezervare
- **Utilizare**: După ce o rezervare este creată cu succes
- **Parametri**:
  ```json
  {
    "bookedSeats": 5
  }
  ```
- **Răspuns**:
  - `200 OK` - Capacitate actualizată cu succes
  - `404 Not Found` - Eveniment nu există
  - `400 Bad Request` - Date invalide

**Cod în BookingResource**:

```java
// Update event capacity
int totalQuantity = request.getItems() != null ?
    request.getItems().stream().mapToInt(BookingItemRequest::getQuantity).sum() : 1;
eventService.updateEventCapacity(request.getEventId(), totalQuantity);
```

#### 3. Obținere Detalii Eveniment (`getEventDetails`)

- **Metodă HTTP**: `GET`
- **Endpoint Event Servlet**: `http://tomcat:8080/event-servlet/api/events/{eventId}`
- **Scop**: Obține detalii complete despre un eveniment
- **Utilizare**: Pentru confirmarea rezervării și afișare informații
- **Răspuns**: Detalii complete despre eveniment (JSON)

### Implementare Tehnică

#### HTTP Client

Booking Servlet folosește **Java HttpClient** (Java 11+) pentru a face requesturi HTTP către Event Servlet:

```java
private final HttpClient httpClient;
private final ObjectMapper objectMapper;
private final String eventServiceUrl;

public EventService() {
    this.httpClient = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(10))
            .build();
    this.objectMapper = new ObjectMapper();
    this.objectMapper.registerModule(new JavaTimeModule());

    // URL-ul servletului de evenimente (configurat prin variabilă de mediu)
    String eventServiceUrl = System.getenv("EVENT_SERVICE_URL");
    if (eventServiceUrl == null || eventServiceUrl.isEmpty()) {
        eventServiceUrl = "http://localhost:8080/event-servlet/api/events";
    }
    this.eventServiceUrl = eventServiceUrl;
}
```

#### Exemplu de Request HTTP

```java
HttpRequest request = HttpRequest.newBuilder()
        .uri(URI.create(eventServiceUrl + "/" + eventId))
        .header("Content-Type", "application/json")
        .GET()
        .build();

HttpResponse<String> response = httpClient.send(request,
        HttpResponse.BodyHandlers.ofString());

if (response.statusCode() == 200) {
    Map<String, Object> eventData = objectMapper.readValue(response.body(), Map.class);
    // Process event data
}
```

### Configurare Docker

Comunicarea între servleturi este configurată în `docker-compose.yml`:

```yaml
jetty:
  environment:
    - EVENT_SERVICE_URL=http://tomcat:8080/event-servlet/api/events
    - EVENT_SERVLET_URL=http://tomcat:8080/event-servlet
  depends_on:
    tomcat:
      condition: service_healthy
  networks:
    - event-network
```

**Observații importante**:

- Ambele servleturi sunt pe același network Docker (`event-network`)
- Booking Servlet folosește numele containerului (`tomcat`) pentru a accesa Event Servlet
- Event Servlet trebuie să fie healthy înainte ca Booking Servlet să pornească
- Comunicarea se face prin HTTP între containere Docker

## Fluxul de Comunicare

### Exemplu: Creare Rezervare

1. **Client** face request `POST /api/bookings` către Booking Servlet (port 8081)
2. **Booking Servlet** primește requestul
3. **Booking Servlet** face request `GET /api/events/{eventId}` către Event Servlet (port 8080) pentru validare
4. **Event Servlet** răspunde cu detaliile evenimentului (status, capacity, price)
5. **Booking Servlet** verifică dacă evenimentul este valid și activ
6. **Booking Servlet** creează rezervarea în baza de date (tabele: `bookings`, `booking_items`, `payments`)
7. **Booking Servlet** face request `PATCH /api/events/{eventId}/capacity` către Event Servlet pentru actualizare capacitate
8. **Event Servlet** actualizează capacitatea evenimentului în baza de date
9. **Booking Servlet** răspunde clientului cu rezervarea creată

### Diagramă de Secvență

```
Client                    Booking Servlet              Event Servlet
  |                              |                            |
  |  1. POST /api/bookings       |                            |
  |----------------------------->|                            |
  |                              |                            |
  |                              |  2. GET /api/events/{id}   |
  |                              |--------------------------->|
  |                              |                            |
  |                              |  3. Event Details (200 OK) |
  |                              |<---------------------------|
  |                              |                            |
  |                              |  4. Validate Event         |
  |                              |   (status, capacity)       |
  |                              |                            |
  |                              |  5. Create Booking         |
  |                              |   (in database)            |
  |                              |                            |
  |                              |  6. PATCH /events/{id}/capacity |
  |                              |--------------------------->|
  |                              |                            |
  |                              |  7. Capacity Updated (200) |
  |                              |<---------------------------|
  |                              |                            |
  |  8. Booking Created (201)    |                            |
  |<-----------------------------|                            |
```

## Endpoint-uri Event Servlet folosite de Booking Servlet

### 1. GET /api/events/{id}

- **Scop**: Obține detalii despre un eveniment
- **Răspuns**:
  ```json
  {
    "id": 1,
    "title": "Summer Music Festival 2024",
    "status": "ACTIVE",
    "maxAttendees": 10000,
    "basePrice": 150.0,
    ...
  }
  ```

### 2. PATCH /api/events/{id}/capacity

- **Scop**: Actualizează capacitatea evenimentului
- **Request Body**:
  ```json
  {
    "bookedSeats": 5
  }
  ```
- **Răspuns**:
  ```json
  {
    "message": "Event capacity updated successfully"
  }
  ```

## Avantaje ale Acestei Arhitecturi

1. **Separare de Responsabilități**:

   - Event Servlet gestionează doar evenimentele
   - Booking Servlet gestionează doar rezervările și plățile

2. **Scalabilitate**:

   - Servleturile pot fi scalate independent
   - Pot rula pe servere diferite

3. **Independență**:

   - Modificări într-un servlet nu afectează direct celălalt
   - Fiecare servlet are propria bază de date (sau parte din bază)

4. **Testabilitate**:

   - Fiecare servlet poate fi testat independent
   - Comunicarea poate fi mock-uită pentru teste

5. **Flexibilitate**:

   - Servleturile pot fi deployate pe servere diferite
   - Pot folosi tehnologii diferite (Tomcat vs Jetty)

6. **Comunicare Standard**:

   - Folosește HTTP REST API, un protocol standard
   - Ușor de integrat cu alte sisteme

7. **Arhitectură Microservicii**:
   - Similară cu microserviciile moderne
   - Comunicare prin HTTP între servicii

## Dependențe și Biblioteci

### Booking Servlet

- **Java HttpClient** (inclus în Java 11+) - pentru requesturi HTTP
- **Jackson** - pentru serializare/deserializare JSON
- **Jersey (JAX-RS)** - pentru REST API endpoints

### Event Servlet

- **Jersey (JAX-RS)** - pentru REST API endpoints
- **Jackson** - pentru serializare/deserializare JSON

## Testare Comunicare

### Test Manual - Verificare Event Servlet

```bash
# Verifică dacă Event Servlet răspunde
curl http://localhost:8080/event-servlet/api/events/1

# Verifică endpoint-ul de capacitate
curl -X PATCH http://localhost:8080/event-servlet/api/events/1/capacity \
  -H "Content-Type: application/json" \
  -d '{"bookedSeats": 5}'
```

### Test Manual - Verificare Booking Servlet

```bash
# Verifică dacă Booking Servlet răspunde
curl http://localhost:8081/booking-servlet/api/bookings/

# Testează comunicarea între servleturi (creare rezervare)
curl -X POST http://localhost:8081/booking-servlet/api/bookings \
  -H "Content-Type: application/json" \
  -d '{
    "userId": 1,
    "eventId": 1,
    "totalAmount": 150.00,
    "items": [{
      "quantity": 1,
      "unitPrice": 150.00,
      "totalPrice": 150.00
    }],
    "paymentMethod": "CREDIT_CARD"
  }'
```

### Test Comunicare între Containere Docker

```bash
# Test din containerul Jetty către Tomcat
docker exec event-ticketing-jetty curl http://tomcat:8080/event-servlet/api/events/1
```

## Concluzie

Aplicația implementează cu succes o arhitectură de tip microservicii folosind două servleturi care comunică între ele prin HTTP REST API:

1. **Event Servlet** (Tomcat, port 8080) - Gestionează evenimentele
2. **Booking Servlet** (Jetty, port 8081) - Gestionează rezervările și comunică cu Event Servlet

Comunicarea se face prin:

- **HTTP GET** pentru validare evenimente
- **HTTP PATCH** pentru actualizare capacitate
- **Java HttpClient** pentru requesturi HTTP
- **JSON** pentru format de date
- **Docker Network** pentru comunicare între containere

Această abordare oferă flexibilitate, scalabilitate și separare clară de responsabilități, similar cu arhitectura de microservicii moderne.
