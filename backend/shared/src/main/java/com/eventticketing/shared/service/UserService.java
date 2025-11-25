package com.eventticketing.shared.service;

import com.eventticketing.shared.model.User;
import com.eventticketing.shared.database.DatabaseConnection;
import org.mindrot.jbcrypt.BCrypt;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.sql.*;
import java.time.LocalDateTime;

public class UserService {
    private static final Logger logger = LoggerFactory.getLogger(UserService.class);
    private static final int BCRYPT_ROUNDS = 10;

    /**
     * Register a new user
     */
    public User registerUser(String username, String email, String password, String firstName, String lastName, String phone) throws SQLException {
        // Check if username or email already exists
        if (userExists(username, email)) {
            throw new IllegalArgumentException("Username or email already exists");
        }

        // Hash password
        String passwordHash = BCrypt.hashpw(password, BCrypt.gensalt(BCRYPT_ROUNDS));

        String sql = "INSERT INTO users (username, email, password_hash, first_name, last_name, phone, created_at, updated_at) " +
                     "VALUES (?, ?, ?, ?, ?, ?, ?, ?) RETURNING id";

        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            Timestamp now = Timestamp.valueOf(LocalDateTime.now());
            stmt.setString(1, username);
            stmt.setString(2, email);
            stmt.setString(3, passwordHash);
            stmt.setString(4, firstName);
            stmt.setString(5, lastName);
            stmt.setString(6, phone);
            stmt.setTimestamp(7, now);
            stmt.setTimestamp(8, now);

            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    Long id = rs.getLong("id");
                    User user = new User();
                    user.setId(id);
                    user.setUsername(username);
                    user.setEmail(email);
                    user.setFirstName(firstName);
                    user.setLastName(lastName);
                    user.setPhone(phone);
                    user.setCreatedAt(now.toLocalDateTime());
                    user.setUpdatedAt(now.toLocalDateTime());
                    user.setIsActive(true);
                    // Don't return password hash
                    user.setPasswordHash(null);
                    return user;
                }
            }
        }

        throw new SQLException("Failed to create user");
    }

    /**
     * Authenticate user by email/username and password
     */
    public User authenticateUser(String identifier, String password) throws SQLException {
        String sql = "SELECT id, username, email, password_hash, first_name, last_name, phone, created_at, updated_at " +
                     "FROM users WHERE (email = ? OR username = ?) LIMIT 1";

        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setString(1, identifier);
            stmt.setString(2, identifier);

            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    String storedHash = rs.getString("password_hash");
                    
                    // Verify password
                    if (BCrypt.checkpw(password, storedHash)) {
                        User user = new User();
                        user.setId(rs.getLong("id"));
                        user.setUsername(rs.getString("username"));
                        user.setEmail(rs.getString("email"));
                        user.setFirstName(rs.getString("first_name"));
                        user.setLastName(rs.getString("last_name"));
                        user.setPhone(rs.getString("phone"));
                        Timestamp createdAt = rs.getTimestamp("created_at");
                        if (createdAt != null) {
                            user.setCreatedAt(createdAt.toLocalDateTime());
                        }
                        Timestamp updatedAt = rs.getTimestamp("updated_at");
                        if (updatedAt != null) {
                            user.setUpdatedAt(updatedAt.toLocalDateTime());
                        }
                        user.setIsActive(true);
                        // Don't return password hash
                        user.setPasswordHash(null);
                        return user;
                    } else {
                        throw new IllegalArgumentException("Invalid password");
                    }
                } else {
                    throw new IllegalArgumentException("User not found");
                }
            }
        }
    }

    /**
     * Check if user exists by username or email
     */
    public boolean userExists(String username, String email) throws SQLException {
        String sql = "SELECT COUNT(*) FROM users WHERE username = ? OR email = ?";

        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setString(1, username);
            stmt.setString(2, email);

            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    return rs.getInt(1) > 0;
                }
            }
        }

        return false;
    }

    /**
     * Get user by ID
     */
    public User getUserById(Long id) throws SQLException {
        String sql = "SELECT id, username, email, first_name, last_name, phone, created_at, updated_at " +
                     "FROM users WHERE id = ?";

        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setLong(1, id);

            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    User user = new User();
                    user.setId(rs.getLong("id"));
                    user.setUsername(rs.getString("username"));
                    user.setEmail(rs.getString("email"));
                    user.setFirstName(rs.getString("first_name"));
                    user.setLastName(rs.getString("last_name"));
                    user.setPhone(rs.getString("phone"));
                    Timestamp createdAt = rs.getTimestamp("created_at");
                    if (createdAt != null) {
                        user.setCreatedAt(createdAt.toLocalDateTime());
                    }
                    Timestamp updatedAt = rs.getTimestamp("updated_at");
                    if (updatedAt != null) {
                        user.setUpdatedAt(updatedAt.toLocalDateTime());
                    }
                    user.setIsActive(true);
                    return user;
                }
            }
        }

        return null;
    }
}

