package com.eventticketing.shared.database;

import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;
import org.postgresql.Driver;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;

public class DatabaseConnection {
    private static final Logger logger = LoggerFactory.getLogger(DatabaseConnection.class);
    private static HikariDataSource dataSource;
    private static final String DEFAULT_DB_URL = "jdbc:postgresql://localhost:5432/eventticketing";
    private static final String DEFAULT_DB_USER = "eventuser";
    private static final String DEFAULT_DB_PASSWORD = "eventpass";

    static {
        initializeDataSource();
    }

    private static void initializeDataSource() {
        try {
            // Register PostgreSQL driver explicitly
            try {
                DriverManager.registerDriver(new Driver());
            } catch (SQLException e) {
                logger.warn("PostgreSQL driver already registered or failed to register: {}", e.getMessage());
            }
            
            HikariConfig config = new HikariConfig();
            
            // Get database configuration from environment variables or use defaults
            String dbUrl = System.getenv("DATABASE_URL");
            String dbUser = System.getenv("DATABASE_USER");
            String dbPassword = System.getenv("DATABASE_PASSWORD");
            
            if (dbUrl == null) {
                dbUrl = System.getProperty("database.url", DEFAULT_DB_URL);
            }
            if (dbUser == null) {
                dbUser = System.getProperty("database.username", DEFAULT_DB_USER);
            }
            if (dbPassword == null) {
                dbPassword = System.getProperty("database.password", DEFAULT_DB_PASSWORD);
            }

            config.setJdbcUrl(dbUrl);
            config.setUsername(dbUser);
            config.setPassword(dbPassword);
            config.setDriverClassName("org.postgresql.Driver");
            
            // Connection pool settings
            config.setMaximumPoolSize(20);
            config.setMinimumIdle(5);
            config.setConnectionTimeout(30000);
            config.setIdleTimeout(600000);
            config.setMaxLifetime(1800000);
            config.setLeakDetectionThreshold(60000);
            
            // PostgreSQL specific settings
            config.addDataSourceProperty("cachePrepStmts", "true");
            config.addDataSourceProperty("prepStmtCacheSize", "250");
            config.addDataSourceProperty("prepStmtCacheSqlLimit", "2048");
            config.addDataSourceProperty("useServerPrepStmts", "true");
            config.addDataSourceProperty("application_name", "EventTicketingSystem");
            config.addDataSourceProperty("tcpKeepAlive", "true");
            config.addDataSourceProperty("socketTimeout", "30");
            
            // Connection validation
            config.setConnectionTestQuery("SELECT 1");
            config.setValidationTimeout(5000);
            
            dataSource = new HikariDataSource(config);
            logger.info("Database connection pool initialized successfully");
            logger.info("Database URL: {}", dbUrl);
            logger.info("Database User: {}", dbUser);
            
        } catch (Exception e) {
            logger.error("Failed to initialize database connection pool", e);
            throw new RuntimeException("Database initialization failed", e);
        }
    }

    public static Connection getConnection() throws SQLException {
        if (dataSource == null) {
            throw new SQLException("DataSource is not initialized");
        }
        return dataSource.getConnection();
    }

    public static DataSource getDataSource() {
        return dataSource;
    }

    public static void closeDataSource() {
        if (dataSource != null && !dataSource.isClosed()) {
            dataSource.close();
            logger.info("Database connection pool closed");
        }
    }

    public static boolean isHealthy() {
        try (Connection connection = getConnection()) {
            return connection.isValid(5);
        } catch (SQLException e) {
            logger.error("Database health check failed", e);
            return false;
        }
    }

    public static void logConnectionStats() {
        if (dataSource != null) {
            logger.info("Database Connection Pool Stats:");
            logger.info("  Active Connections: {}", dataSource.getHikariPoolMXBean().getActiveConnections());
            logger.info("  Idle Connections: {}", dataSource.getHikariPoolMXBean().getIdleConnections());
            logger.info("  Total Connections: {}", dataSource.getHikariPoolMXBean().getTotalConnections());
            logger.info("  Threads Awaiting Connection: {}", dataSource.getHikariPoolMXBean().getThreadsAwaitingConnection());
        }
    }
}
