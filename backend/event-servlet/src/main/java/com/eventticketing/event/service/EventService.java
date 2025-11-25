package com.eventticketing.event.service;

import com.eventticketing.shared.model.Event;
import com.eventticketing.shared.model.TicketType;
import com.eventticketing.shared.model.Seat;
import com.eventticketing.shared.database.DatabaseConnection;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.sql.*;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

public class EventService {
    private static final Logger logger = LoggerFactory.getLogger(EventService.class);

    public List<Event> getAllEvents(String category, String status, int page, int size) throws SQLException {
        List<Event> events = new ArrayList<>();
        String sql = "SELECT * FROM events WHERE 1=1";
        List<Object> parameters = new ArrayList<>();
        
        if (category != null && !category.isEmpty()) {
            sql += " AND category = ?";
            parameters.add(category);
        }
        
        if (status != null && !status.isEmpty()) {
            sql += " AND status = ?";
            parameters.add(status);
        }
        
        sql += " ORDER BY event_date ASC LIMIT ? OFFSET ?";
        parameters.add(size);
        parameters.add(page * size);

        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            
            for (int i = 0; i < parameters.size(); i++) {
                stmt.setObject(i + 1, parameters.get(i));
            }
            
            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) {
                    events.add(mapResultSetToEvent(rs));
                }
            }
        }
        
        return events;
    }

    public Event getEventById(Long id) throws SQLException {
        String sql = "SELECT * FROM events WHERE id = ?";
        
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            
            stmt.setLong(1, id);
            
            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    return mapResultSetToEvent(rs);
                }
            }
        }
        
        return null;
    }

    public Event createEvent(Event event) throws SQLException {
        String sql = "INSERT INTO events (title, description, event_date, end_date, venue_id, organizer_id, " +
                    "category, status, max_attendees, ticket_sales_start, ticket_sales_end, images, tags, created_at) " +
                    "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)";
        
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            
            stmt.setString(1, event.getTitle());
            stmt.setString(2, event.getDescription());
            stmt.setTimestamp(3, Timestamp.valueOf(event.getEventDate()));
            stmt.setTimestamp(4, event.getEndDate() != null ? Timestamp.valueOf(event.getEndDate()) : null);
            stmt.setLong(5, event.getVenueId());
            stmt.setLong(6, event.getOrganizerId());
            stmt.setString(7, event.getCategory());
            stmt.setString(8, event.getStatus() != null ? event.getStatus() : "draft");
            stmt.setObject(9, event.getMaxAttendees());
            stmt.setTimestamp(10, event.getTicketSalesStart() != null ? Timestamp.valueOf(event.getTicketSalesStart()) : null);
            stmt.setTimestamp(11, event.getTicketSalesEnd() != null ? Timestamp.valueOf(event.getTicketSalesEnd()) : null);
            stmt.setString(12, event.getImages() != null ? String.join(",", event.getImages()) : null);
            stmt.setString(13, event.getTags() != null ? String.join(",", event.getTags()) : null);
            stmt.setTimestamp(14, Timestamp.valueOf(LocalDateTime.now()));
            
            int affectedRows = stmt.executeUpdate();
            if (affectedRows == 0) {
                throw new SQLException("Creating event failed, no rows affected.");
            }
            
            try (ResultSet generatedKeys = stmt.getGeneratedKeys()) {
                if (generatedKeys.next()) {
                    event.setId(generatedKeys.getLong(1));
                } else {
                    throw new SQLException("Creating event failed, no ID obtained.");
                }
            }
        }
        
        return event;
    }

    public Event updateEvent(Event event) throws SQLException {
        String sql = "UPDATE events SET title = ?, description = ?, event_date = ?, end_date = ?, " +
                    "venue_id = ?, organizer_id = ?, category = ?, status = ?, max_attendees = ?, " +
                    "ticket_sales_start = ?, ticket_sales_end = ?, images = ?, tags = ?, updated_at = ? " +
                    "WHERE id = ?";
        
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            
            stmt.setString(1, event.getTitle());
            stmt.setString(2, event.getDescription());
            stmt.setTimestamp(3, Timestamp.valueOf(event.getEventDate()));
            stmt.setTimestamp(4, event.getEndDate() != null ? Timestamp.valueOf(event.getEndDate()) : null);
            stmt.setLong(5, event.getVenueId());
            stmt.setLong(6, event.getOrganizerId());
            stmt.setString(7, event.getCategory());
            stmt.setString(8, event.getStatus());
            stmt.setObject(9, event.getMaxAttendees());
            stmt.setTimestamp(10, event.getTicketSalesStart() != null ? Timestamp.valueOf(event.getTicketSalesStart()) : null);
            stmt.setTimestamp(11, event.getTicketSalesEnd() != null ? Timestamp.valueOf(event.getTicketSalesEnd()) : null);
            stmt.setString(12, event.getImages() != null ? String.join(",", event.getImages()) : null);
            stmt.setString(13, event.getTags() != null ? String.join(",", event.getTags()) : null);
            stmt.setTimestamp(14, Timestamp.valueOf(LocalDateTime.now()));
            stmt.setLong(15, event.getId());
            
            int affectedRows = stmt.executeUpdate();
            if (affectedRows == 0) {
                return null; // Event not found
            }
        }
        
        return getEventById(event.getId());
    }

    public boolean deleteEvent(Long id) throws SQLException {
        String sql = "DELETE FROM events WHERE id = ?";
        
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            
            stmt.setLong(1, id);
            int affectedRows = stmt.executeUpdate();
            return affectedRows > 0;
        }
    }

    public List<TicketType> getTicketTypesByEventId(Long eventId) throws SQLException {
        List<TicketType> ticketTypes = new ArrayList<>();
        String sql = "SELECT * FROM ticket_types WHERE event_id = ? AND is_active = true ORDER BY price ASC";
        
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            
            stmt.setLong(1, eventId);
            
            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) {
                    ticketTypes.add(mapResultSetToTicketType(rs));
                }
            }
        }
        
        return ticketTypes;
    }

    public TicketType createTicketType(TicketType ticketType) throws SQLException {
        String sql = "INSERT INTO ticket_types (event_id, name, description, price, quantity_available, " +
                    "quantity_sold, sales_start, sales_end, is_active, created_at) " +
                    "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?)";
        
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            
            stmt.setLong(1, ticketType.getEventId());
            stmt.setString(2, ticketType.getName());
            stmt.setString(3, ticketType.getDescription());
            stmt.setBigDecimal(4, ticketType.getPrice());
            stmt.setInt(5, ticketType.getQuantityAvailable());
            stmt.setInt(6, ticketType.getQuantitySold() != null ? ticketType.getQuantitySold() : 0);
            stmt.setTimestamp(7, ticketType.getSalesStart() != null ? Timestamp.valueOf(ticketType.getSalesStart()) : null);
            stmt.setTimestamp(8, ticketType.getSalesEnd() != null ? Timestamp.valueOf(ticketType.getSalesEnd()) : null);
            stmt.setBoolean(9, ticketType.getIsActive() != null ? ticketType.getIsActive() : true);
            stmt.setTimestamp(10, Timestamp.valueOf(LocalDateTime.now()));
            
            int affectedRows = stmt.executeUpdate();
            if (affectedRows == 0) {
                throw new SQLException("Creating ticket type failed, no rows affected.");
            }
            
            try (ResultSet generatedKeys = stmt.getGeneratedKeys()) {
                if (generatedKeys.next()) {
                    ticketType.setId(generatedKeys.getLong(1));
                } else {
                    throw new SQLException("Creating ticket type failed, no ID obtained.");
                }
            }
        }
        
        return ticketType;
    }

    public boolean updateEventCapacity(Long eventId, int bookedSeats) throws SQLException {
        String sql = "UPDATE events SET booked_seats = booked_seats + ? WHERE id = ?";
        
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            
            stmt.setInt(1, bookedSeats);
            stmt.setLong(2, eventId);
            
            int affectedRows = stmt.executeUpdate();
            return affectedRows > 0;
        }
    }

    public List<Seat> getAvailableSeats(Long eventId) throws SQLException {
        List<Seat> seats = new ArrayList<>();
        String sql = "SELECT s.* FROM seats s " +
                    "JOIN events e ON s.venue_id = e.venue_id " +
                    "WHERE e.id = ? AND s.is_available = true " +
                    "ORDER BY s.section, s.row_number, s.seat_number";
        
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            
            stmt.setLong(1, eventId);
            
            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) {
                    seats.add(mapResultSetToSeat(rs));
                }
            }
        }
        
        return seats;
    }

    public boolean reserveSeat(Long eventId, Long seatId) throws SQLException {
        String sql = "UPDATE seats SET is_available = false WHERE id = ? AND is_available = true";
        
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            
            stmt.setLong(1, seatId);
            int affectedRows = stmt.executeUpdate();
            return affectedRows > 0;
        }
    }

    public boolean releaseSeat(Long eventId, Long seatId) throws SQLException {
        String sql = "UPDATE seats SET is_available = true WHERE id = ? AND is_available = false";
        
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            
            stmt.setLong(1, seatId);
            int affectedRows = stmt.executeUpdate();
            return affectedRows > 0;
        }
    }

    public List<Event> searchEvents(String query, String category, String dateFrom, String dateTo, 
                                   int page, int size) throws SQLException {
        List<Event> events = new ArrayList<>();
        StringBuilder sql = new StringBuilder("SELECT * FROM events WHERE 1=1");
        List<Object> parameters = new ArrayList<>();
        
        if (query != null && !query.isEmpty()) {
            sql.append(" AND (title LIKE ? OR description LIKE ?)");
            parameters.add("%" + query + "%");
            parameters.add("%" + query + "%");
        }
        
        if (category != null && !category.isEmpty()) {
            sql.append(" AND category = ?");
            parameters.add(category);
        }
        
        if (dateFrom != null && !dateFrom.isEmpty()) {
            sql.append(" AND event_date >= ?");
            parameters.add(Timestamp.valueOf(LocalDateTime.parse(dateFrom)));
        }
        
        if (dateTo != null && !dateTo.isEmpty()) {
            sql.append(" AND event_date <= ?");
            parameters.add(Timestamp.valueOf(LocalDateTime.parse(dateTo)));
        }
        
        sql.append(" ORDER BY event_date ASC LIMIT ? OFFSET ?");
        parameters.add(size);
        parameters.add(page * size);

        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql.toString())) {
            
            for (int i = 0; i < parameters.size(); i++) {
                stmt.setObject(i + 1, parameters.get(i));
            }
            
            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) {
                    events.add(mapResultSetToEvent(rs));
                }
            }
        }
        
        return events;
    }

    private Event mapResultSetToEvent(ResultSet rs) throws SQLException {
        Event event = new Event();
        event.setId(rs.getLong("id"));
        event.setTitle(rs.getString("title"));
        event.setDescription(rs.getString("description"));
        event.setEventDate(rs.getTimestamp("event_date").toLocalDateTime());
        
        // Map to available fields - The schema has: venue, city, country, capacity, available_seats
        // but the Event model expects: venueId, organizerId, category, maxAttendees
        // Create a simple hash from venue string for venueId
        event.setVenueId((long)rs.getString("venue").hashCode());
        event.setOrganizerId(1L); // Default organizer
        
        // Use city as category
        event.setCategory(rs.getString("city"));
        event.setStatus(rs.getString("status"));
        event.setMaxAttendees(rs.getInt("capacity"));
        
        // Map base_price from database
        java.math.BigDecimal basePrice = rs.getBigDecimal("base_price");
        if (basePrice != null) {
            event.setBasePrice(basePrice);
        }
        
        event.setCreatedAt(rs.getTimestamp("created_at").toLocalDateTime());
        Timestamp updatedAt = rs.getTimestamp("updated_at");
        if (updatedAt != null) {
            event.setUpdatedAt(updatedAt.toLocalDateTime());
        }
        
        return event;
    }

    private TicketType mapResultSetToTicketType(ResultSet rs) throws SQLException {
        TicketType ticketType = new TicketType();
        ticketType.setId(rs.getLong("id"));
        ticketType.setEventId(rs.getLong("event_id"));
        ticketType.setName(rs.getString("name"));
        ticketType.setDescription(rs.getString("description"));
        ticketType.setPrice(rs.getBigDecimal("price"));
        ticketType.setQuantityAvailable(rs.getInt("quantity_available"));
        ticketType.setQuantitySold(rs.getInt("quantity_sold"));
        
        Timestamp salesStart = rs.getTimestamp("sales_start");
        if (salesStart != null) {
            ticketType.setSalesStart(salesStart.toLocalDateTime());
        }
        
        Timestamp salesEnd = rs.getTimestamp("sales_end");
        if (salesEnd != null) {
            ticketType.setSalesEnd(salesEnd.toLocalDateTime());
        }
        
        ticketType.setIsActive(rs.getBoolean("is_active"));
        ticketType.setCreatedAt(rs.getTimestamp("created_at").toLocalDateTime());
        Timestamp updatedAt = rs.getTimestamp("updated_at");
        if (updatedAt != null) {
            ticketType.setUpdatedAt(updatedAt.toLocalDateTime());
        }
        
        return ticketType;
    }

    private Seat mapResultSetToSeat(ResultSet rs) throws SQLException {
        Seat seat = new Seat();
        seat.setId(rs.getLong("id"));
        seat.setVenueId(rs.getLong("venue_id"));
        seat.setSection(rs.getString("section"));
        seat.setRowNumber(rs.getString("row_number"));
        seat.setSeatNumber(rs.getString("seat_number"));
        seat.setSeatType(rs.getString("seat_type"));
        seat.setIsAvailable(rs.getBoolean("is_available"));
        seat.setCreatedAt(rs.getTimestamp("created_at").toLocalDateTime());
        return seat;
    }
}
