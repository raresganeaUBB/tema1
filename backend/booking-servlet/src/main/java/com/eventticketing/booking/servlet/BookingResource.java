package com.eventticketing.booking.servlet;

import com.eventticketing.shared.database.DatabaseConnection;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;

import java.math.BigDecimal;
import java.sql.*;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Path("/bookings")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
public class BookingResource {
    
    private final ObjectMapper objectMapper;
    private final EventService eventService;
    
    public BookingResource() {
        this.objectMapper = new ObjectMapper();
        this.objectMapper.registerModule(new JavaTimeModule());
        this.eventService = new EventService();
    }

    @GET
    @Path("/")
    public Response getAllBookings() {
        try (Connection conn = DatabaseConnection.getConnection()) {
            String sql = "SELECT b.*, e.title as event_title, e.event_date as event_date " +
                        "FROM bookings b " +
                        "JOIN events e ON b.event_id = e.id " +
                        "ORDER BY b.created_at DESC";
            
            try (PreparedStatement stmt = conn.prepareStatement(sql);
                 ResultSet rs = stmt.executeQuery()) {
                
                List<BookingResponse> bookings = new ArrayList<>();
                while (rs.next()) {
                    BookingResponse booking = mapResultSetToBookingResponse(rs);
                    bookings.add(booking);
                }
                
                return Response.ok(objectMapper.writeValueAsString(bookings)).build();
            }
        } catch (Exception e) {
            e.printStackTrace();
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR)
                    .entity("{\"error\": \"Failed to retrieve bookings: " + e.getMessage() + "\"}")
                    .build();
        }
    }

    @GET
    @Path("/{id}")
    public Response getBookingById(@PathParam("id") Long id) {
        try (Connection conn = DatabaseConnection.getConnection()) {
            String sql = "SELECT b.*, e.title as event_title, e.event_date as event_date " +
                        "FROM bookings b " +
                        "JOIN events e ON b.event_id = e.id " +
                        "WHERE b.id = ?";
            
            try (PreparedStatement stmt = conn.prepareStatement(sql)) {
                stmt.setLong(1, id);
                
                try (ResultSet rs = stmt.executeQuery()) {
                    if (rs.next()) {
                        BookingResponse booking = mapResultSetToBookingResponse(rs);
                        
                        // Get booking items
                        String itemsSql = "SELECT * FROM booking_items WHERE booking_id = ?";
                        try (PreparedStatement itemsStmt = conn.prepareStatement(itemsSql)) {
                            itemsStmt.setLong(1, id);
                            try (ResultSet itemsRs = itemsStmt.executeQuery()) {
                                List<BookingItemResponse> items = new ArrayList<>();
                                while (itemsRs.next()) {
                                    BookingItemResponse item = new BookingItemResponse();
                                    item.setId(itemsRs.getLong("id"));
                                    item.setTicketTypeId(itemsRs.getLong("ticket_type_id"));
                                    item.setSeatId(itemsRs.getLong("seat_id"));
                                    item.setQuantity(itemsRs.getInt("quantity"));
                                    item.setUnitPrice(itemsRs.getBigDecimal("unit_price"));
                                    item.setTotalPrice(itemsRs.getBigDecimal("total_price"));
                                    items.add(item);
                                }
                                booking.setItems(items);
                            }
                        }
                        
                        return Response.ok(objectMapper.writeValueAsString(booking)).build();
                    } else {
                        return Response.status(Response.Status.NOT_FOUND)
                                .entity("{\"error\": \"Booking not found\"}")
                                .build();
                    }
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR)
                    .entity("{\"error\": \"Failed to retrieve booking: " + e.getMessage() + "\"}")
                    .build();
        }
    }

    @POST
    @Path("/")
    public Response createBooking(BookingRequest request) {
        Connection conn = null;
        try {
            conn = DatabaseConnection.getConnection();
            conn.setAutoCommit(false);
            
            try {
                // Validate event
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
                
                // Generate booking reference
                String bookingReference = request.getBookingReference();
                if (bookingReference == null || bookingReference.trim().isEmpty()) {
                    bookingReference = "BK" + System.currentTimeMillis() + UUID.randomUUID().toString().substring(0, 8).toUpperCase();
                }
                
                // Create booking
                String insertBookingSql = "INSERT INTO bookings (user_id, event_id, booking_reference, total_amount, status, booking_date, created_at, updated_at) " +
                                         "VALUES (?, ?, ?, ?, 'PENDING', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP)";
                
                try (PreparedStatement bookingStmt = conn.prepareStatement(insertBookingSql, Statement.RETURN_GENERATED_KEYS)) {
                    bookingStmt.setLong(1, request.getUserId());
                    bookingStmt.setLong(2, request.getEventId());
                    bookingStmt.setString(3, bookingReference);
                    bookingStmt.setBigDecimal(4, request.getTotalAmount());
                    
                    int rowsAffected = bookingStmt.executeUpdate();
                    if (rowsAffected == 0) {
                        return Response.status(Response.Status.INTERNAL_SERVER_ERROR)
                                .entity("{\"error\": \"Failed to create booking\"}")
                                .build();
                    }
                    
                    Long bookingId;
                    try (ResultSet generatedKeys = bookingStmt.getGeneratedKeys()) {
                        if (generatedKeys.next()) {
                            bookingId = generatedKeys.getLong(1);
                        } else {
                            return Response.status(Response.Status.INTERNAL_SERVER_ERROR)
                                    .entity("{\"error\": \"Failed to create booking\"}")
                                    .build();
                        }
                    }
                    
                    // Create booking items
                    if (request.getItems() != null && !request.getItems().isEmpty()) {
                        String insertItemSql = "INSERT INTO booking_items (booking_id, ticket_type_id, seat_id, quantity, unit_price, total_price, created_at) " +
                                              "VALUES (?, ?, ?, ?, ?, ?, CURRENT_TIMESTAMP)";
                        
                        try (PreparedStatement itemStmt = conn.prepareStatement(insertItemSql)) {
                            for (BookingItemRequest item : request.getItems()) {
                                itemStmt.setLong(1, bookingId);
                                itemStmt.setObject(2, item.getTicketTypeId(), Types.BIGINT);
                                itemStmt.setObject(3, item.getSeatId(), Types.BIGINT);
                                itemStmt.setInt(4, item.getQuantity());
                                itemStmt.setBigDecimal(5, item.getUnitPrice());
                                itemStmt.setBigDecimal(6, item.getTotalPrice());
                                itemStmt.addBatch();
                            }
                            itemStmt.executeBatch();
                        }
                    }
                    
                    // Create payment record
                    if (request.getPaymentMethod() != null) {
                        String insertPaymentSql = "INSERT INTO payments (booking_id, amount, payment_method, payment_status, created_at, updated_at) " +
                                                 "VALUES (?, ?, ?, 'PENDING', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP)";
                        
                        try (PreparedStatement paymentStmt = conn.prepareStatement(insertPaymentSql)) {
                            paymentStmt.setLong(1, bookingId);
                            paymentStmt.setBigDecimal(2, request.getTotalAmount());
                            paymentStmt.setString(3, request.getPaymentMethod());
                            paymentStmt.executeUpdate();
                        }
                    }
                    
                    // Update event capacity
                    int totalQuantity = request.getItems() != null ? 
                        request.getItems().stream().mapToInt(BookingItemRequest::getQuantity).sum() : 1;
                    eventService.updateEventCapacity(request.getEventId(), totalQuantity);
                    
                    conn.commit();
                    
                    // Get the created booking
                    BookingResponse response = new BookingResponse();
                    response.setId(bookingId);
                    response.setUserId(request.getUserId());
                    response.setEventId(request.getEventId());
                    response.setBookingReference(bookingReference);
                    response.setTotalAmount(request.getTotalAmount());
                    response.setStatus("PENDING");
                    response.setCreatedAt(LocalDateTime.now());
                    response.setUpdatedAt(LocalDateTime.now());
                    
                    return Response.status(Response.Status.CREATED)
                            .entity(objectMapper.writeValueAsString(response))
                            .build();
                }
                
            } catch (Exception e) {
                if (conn != null) {
                    conn.rollback();
                }
                throw e;
            }
        } catch (Exception e) {
            e.printStackTrace();
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR)
                    .entity("{\"error\": \"Failed to create booking: " + e.getMessage() + "\"}")
                    .build();
        }
    }

    @PUT
    @Path("/{id}/payment")
    public Response processPayment(@PathParam("id") Long id, PaymentRequest request) {
        try (Connection conn = DatabaseConnection.getConnection()) {
            conn.setAutoCommit(false);
            
            try {
                // Update payment status
                String updatePaymentSql = "UPDATE payments SET payment_status = ?, payment_date = CURRENT_TIMESTAMP, updated_at = CURRENT_TIMESTAMP " +
                                         "WHERE booking_id = ?";
                
                try (PreparedStatement paymentStmt = conn.prepareStatement(updatePaymentSql)) {
                    paymentStmt.setString(1, request.getPaymentStatus());
                    paymentStmt.setLong(2, id);
                    
                    int rowsAffected = paymentStmt.executeUpdate();
                    if (rowsAffected == 0) {
                        return Response.status(Response.Status.NOT_FOUND)
                                .entity("{\"error\": \"Payment not found\"}")
                                .build();
                    }
                }
                
                // Update booking status based on payment status
                String updateBookingSql = "UPDATE bookings SET status = ?, updated_at = CURRENT_TIMESTAMP " +
                                         "WHERE id = ?";
                
                try (PreparedStatement bookingStmt = conn.prepareStatement(updateBookingSql)) {
                    String bookingStatus = "COMPLETED".equals(request.getPaymentStatus()) ? "CONFIRMED" : "PENDING";
                    bookingStmt.setString(1, bookingStatus);
                    bookingStmt.setLong(2, id);
                    bookingStmt.executeUpdate();
                }
                
                conn.commit();
                
                return Response.ok("{\"message\": \"Payment status updated successfully\"}").build();
                
            } catch (Exception e) {
                conn.rollback();
                throw e;
            }
        } catch (Exception e) {
            e.printStackTrace();
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR)
                    .entity("{\"error\": \"Failed to process payment: " + e.getMessage() + "\"}")
                    .build();
        }
    }

    @GET
    @Path("/user/{userId}")
    public Response getBookingsByUser(@PathParam("userId") Long userId) {
        try (Connection conn = DatabaseConnection.getConnection()) {
            String sql = "SELECT b.*, e.title as event_title, e.event_date as event_date " +
                        "FROM bookings b " +
                        "JOIN events e ON b.event_id = e.id " +
                        "WHERE b.user_id = ? " +
                        "ORDER BY b.created_at DESC";
            
            try (PreparedStatement stmt = conn.prepareStatement(sql)) {
                stmt.setLong(1, userId);
                
                try (ResultSet rs = stmt.executeQuery()) {
                    List<BookingResponse> bookings = new ArrayList<>();
                    while (rs.next()) {
                        BookingResponse booking = mapResultSetToBookingResponse(rs);
                        bookings.add(booking);
                    }
                    
                    return Response.ok(objectMapper.writeValueAsString(bookings)).build();
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR)
                    .entity("{\"error\": \"Failed to retrieve user bookings: " + e.getMessage() + "\"}")
                    .build();
        }
    }

    private BookingResponse mapResultSetToBookingResponse(ResultSet rs) throws SQLException {
        BookingResponse booking = new BookingResponse();
        booking.setId(rs.getLong("id"));
        booking.setUserId(rs.getLong("user_id"));
        booking.setEventId(rs.getLong("event_id"));
        booking.setBookingReference(rs.getString("booking_reference"));
        booking.setTotalAmount(rs.getBigDecimal("total_amount"));
        booking.setStatus(rs.getString("status"));
        
        Timestamp bookingDate = rs.getTimestamp("booking_date");
        if (bookingDate != null) {
            booking.setBookingDate(bookingDate.toLocalDateTime());
        }
        
        Timestamp createdAt = rs.getTimestamp("created_at");
        if (createdAt != null) {
            booking.setCreatedAt(createdAt.toLocalDateTime());
        }
        
        Timestamp updatedAt = rs.getTimestamp("updated_at");
        if (updatedAt != null) {
            booking.setUpdatedAt(updatedAt.toLocalDateTime());
        }
        
        // Get event title if available
        try {
            booking.setEventTitle(rs.getString("event_title"));
        } catch (SQLException e) {
            // Column might not be present in all queries
        }
        
        return booking;
    }

    // Request/Response DTOs
    public static class BookingRequest {
        private Long userId;
        private Long eventId;
        private String bookingReference;
        private BigDecimal totalAmount;
        private String paymentMethod;
        private List<BookingItemRequest> items;

        public Long getUserId() { return userId; }
        public void setUserId(Long userId) { this.userId = userId; }
        
        public Long getEventId() { return eventId; }
        public void setEventId(Long eventId) { this.eventId = eventId; }
        
        public String getBookingReference() { return bookingReference; }
        public void setBookingReference(String bookingReference) { this.bookingReference = bookingReference; }
        
        public BigDecimal getTotalAmount() { return totalAmount; }
        public void setTotalAmount(BigDecimal totalAmount) { this.totalAmount = totalAmount; }
        
        public String getPaymentMethod() { return paymentMethod; }
        public void setPaymentMethod(String paymentMethod) { this.paymentMethod = paymentMethod; }
        
        public List<BookingItemRequest> getItems() { return items; }
        public void setItems(List<BookingItemRequest> items) { this.items = items; }
    }

    public static class BookingItemRequest {
        private Long ticketTypeId;
        private Long seatId;
        private Integer quantity;
        private BigDecimal unitPrice;
        private BigDecimal totalPrice;

        public Long getTicketTypeId() { return ticketTypeId; }
        public void setTicketTypeId(Long ticketTypeId) { this.ticketTypeId = ticketTypeId; }
        
        public Long getSeatId() { return seatId; }
        public void setSeatId(Long seatId) { this.seatId = seatId; }
        
        public Integer getQuantity() { return quantity; }
        public void setQuantity(Integer quantity) { this.quantity = quantity; }
        
        public BigDecimal getUnitPrice() { return unitPrice; }
        public void setUnitPrice(BigDecimal unitPrice) { this.unitPrice = unitPrice; }
        
        public BigDecimal getTotalPrice() { return totalPrice; }
        public void setTotalPrice(BigDecimal totalPrice) { this.totalPrice = totalPrice; }
    }

    public static class BookingResponse {
        private Long id;
        private Long userId;
        private Long eventId;
        private String eventTitle;
        private String bookingReference;
        private BigDecimal totalAmount;
        private String status;
        private LocalDateTime bookingDate;
        private LocalDateTime createdAt;
        private LocalDateTime updatedAt;
        private List<BookingItemResponse> items;

        public Long getId() { return id; }
        public void setId(Long id) { this.id = id; }
        
        public Long getUserId() { return userId; }
        public void setUserId(Long userId) { this.userId = userId; }
        
        public Long getEventId() { return eventId; }
        public void setEventId(Long eventId) { this.eventId = eventId; }
        
        public String getEventTitle() { return eventTitle; }
        public void setEventTitle(String eventTitle) { this.eventTitle = eventTitle; }
        
        public String getBookingReference() { return bookingReference; }
        public void setBookingReference(String bookingReference) { this.bookingReference = bookingReference; }
        
        public BigDecimal getTotalAmount() { return totalAmount; }
        public void setTotalAmount(BigDecimal totalAmount) { this.totalAmount = totalAmount; }
        
        public String getStatus() { return status; }
        public void setStatus(String status) { this.status = status; }
        
        public LocalDateTime getBookingDate() { return bookingDate; }
        public void setBookingDate(LocalDateTime bookingDate) { this.bookingDate = bookingDate; }
        
        public LocalDateTime getCreatedAt() { return createdAt; }
        public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
        
        public LocalDateTime getUpdatedAt() { return updatedAt; }
        public void setUpdatedAt(LocalDateTime updatedAt) { this.updatedAt = updatedAt; }
        
        public List<BookingItemResponse> getItems() { return items; }
        public void setItems(List<BookingItemResponse> items) { this.items = items; }
    }

    public static class BookingItemResponse {
        private Long id;
        private Long ticketTypeId;
        private Long seatId;
        private Integer quantity;
        private BigDecimal unitPrice;
        private BigDecimal totalPrice;

        public Long getId() { return id; }
        public void setId(Long id) { this.id = id; }
        
        public Long getTicketTypeId() { return ticketTypeId; }
        public void setTicketTypeId(Long ticketTypeId) { this.ticketTypeId = ticketTypeId; }
        
        public Long getSeatId() { return seatId; }
        public void setSeatId(Long seatId) { this.seatId = seatId; }
        
        public Integer getQuantity() { return quantity; }
        public void setQuantity(Integer quantity) { this.quantity = quantity; }
        
        public BigDecimal getUnitPrice() { return unitPrice; }
        public void setUnitPrice(BigDecimal unitPrice) { this.unitPrice = unitPrice; }
        
        public BigDecimal getTotalPrice() { return totalPrice; }
        public void setTotalPrice(BigDecimal totalPrice) { this.totalPrice = totalPrice; }
    }

    public static class PaymentRequest {
        private String paymentStatus;
        private String paymentMethod;
        private String transactionId;

        public String getPaymentStatus() { return paymentStatus; }
        public void setPaymentStatus(String paymentStatus) { this.paymentStatus = paymentStatus; }
        
        public String getPaymentMethod() { return paymentMethod; }
        public void setPaymentMethod(String paymentMethod) { this.paymentMethod = paymentMethod; }
        
        public String getTransactionId() { return transactionId; }
        public void setTransactionId(String transactionId) { this.transactionId = transactionId; }
    }
}
