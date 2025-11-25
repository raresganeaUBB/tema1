package com.eventticketing.booking.servlet;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import jakarta.ws.rs.core.Response;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.Map;

/**
 * Service class for communicating with the Event Management Servlet
 */
public class EventService {
    
    private final HttpClient httpClient;
    private final ObjectMapper objectMapper;
    private final String eventServiceUrl;
    
    public EventService() {
        this.httpClient = HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(10))
                .build();
        this.objectMapper = new ObjectMapper();
        this.objectMapper.registerModule(new JavaTimeModule());
        
        // Get event service URL from environment or use default
        String eventServiceUrl = System.getenv("EVENT_SERVICE_URL");
        if (eventServiceUrl == null || eventServiceUrl.isEmpty()) {
            // Default to Tomcat event service
            eventServiceUrl = "http://localhost:8080/event-servlet/api/events";
        }
        this.eventServiceUrl = eventServiceUrl;
    }
    
    /**
     * Validates if an event exists and is available for booking
     */
    public EventValidationResult validateEvent(Long eventId) {
        try {
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(eventServiceUrl + "/" + eventId))
                    .header("Content-Type", "application/json")
                    .GET()
                    .build();
            
            HttpResponse<String> response = httpClient.send(request, 
                    HttpResponse.BodyHandlers.ofString());
            
            if (response.statusCode() == 200) {
                Map<String, Object> eventData = objectMapper.readValue(response.body(), Map.class);
                
                EventValidationResult result = new EventValidationResult();
                result.setValid(true);
                result.setEventId(eventId);
                result.setEventName((String) eventData.get("title"));
                // Try to get basePrice or ticketPrice
                Object priceObj = eventData.get("basePrice");
                if (priceObj == null) {
                    priceObj = eventData.get("ticketPrice");
                }
                if (priceObj != null) {
                    result.setTicketPrice(((Number) priceObj).doubleValue());
                }
                // Try to get maxAttendees or capacity
                Object capacityObj = eventData.get("maxAttendees");
                if (capacityObj == null) {
                    capacityObj = eventData.get("capacity");
                }
                if (capacityObj != null) {
                    result.setCapacity(((Number) capacityObj).intValue());
                }
                result.setStatus((String) eventData.get("status"));
                
                return result;
            } else {
                EventValidationResult result = new EventValidationResult();
                result.setValid(false);
                result.setErrorMessage("Event not found or not available");
                return result;
            }
            
        } catch (IOException | InterruptedException e) {
            EventValidationResult result = new EventValidationResult();
            result.setValid(false);
            result.setErrorMessage("Failed to validate event: " + e.getMessage());
            return result;
        }
    }
    
    /**
     * Updates event capacity after booking
     */
    public boolean updateEventCapacity(Long eventId, int bookedSeats) {
        try {
            Map<String, Object> updateData = Map.of(
                "id", eventId,
                "bookedSeats", bookedSeats
            );
            
            String requestBody = objectMapper.writeValueAsString(updateData);
            
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(eventServiceUrl + "/" + eventId + "/capacity"))
                    .header("Content-Type", "application/json")
                    .method("PATCH", HttpRequest.BodyPublishers.ofString(requestBody))
                    .build();
            
            HttpResponse<String> response = httpClient.send(request, 
                    HttpResponse.BodyHandlers.ofString());
            
            return response.statusCode() == 200;
            
        } catch (IOException | InterruptedException e) {
            System.err.println("Failed to update event capacity: " + e.getMessage());
            return false;
        }
    }
    
    /**
     * Gets event details for booking confirmation
     */
    public Map<String, Object> getEventDetails(Long eventId) {
        try {
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(eventServiceUrl + "/" + eventId))
                    .header("Content-Type", "application/json")
                    .GET()
                    .build();
            
            HttpResponse<String> response = httpClient.send(request, 
                    HttpResponse.BodyHandlers.ofString());
            
            if (response.statusCode() == 200) {
                return objectMapper.readValue(response.body(), Map.class);
            }
            
        } catch (IOException | InterruptedException e) {
            System.err.println("Failed to get event details: " + e.getMessage());
        }
        
        return null;
    }
    
    /**
     * Result class for event validation
     */
    public static class EventValidationResult {
        private boolean valid;
        private Long eventId;
        private String eventName;
        private double ticketPrice;
        private int capacity;
        private String status;
        private String errorMessage;
        
        // Getters and setters
        public boolean isValid() { return valid; }
        public void setValid(boolean valid) { this.valid = valid; }
        
        public Long getEventId() { return eventId; }
        public void setEventId(Long eventId) { this.eventId = eventId; }
        
        public String getEventName() { return eventName; }
        public void setEventName(String eventName) { this.eventName = eventName; }
        
        public double getTicketPrice() { return ticketPrice; }
        public void setTicketPrice(double ticketPrice) { this.ticketPrice = ticketPrice; }
        
        public int getCapacity() { return capacity; }
        public void setCapacity(int capacity) { this.capacity = capacity; }
        
        public String getStatus() { return status; }
        public void setStatus(String status) { this.status = status; }
        
        public String getErrorMessage() { return errorMessage; }
        public void setErrorMessage(String errorMessage) { this.errorMessage = errorMessage; }
    }
}
