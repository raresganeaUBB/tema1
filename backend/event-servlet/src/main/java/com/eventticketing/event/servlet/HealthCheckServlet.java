package com.eventticketing.event.servlet;

import com.eventticketing.shared.database.DatabaseConnection;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.io.PrintWriter;

public class HealthCheckServlet extends HttpServlet {
    private static final Logger logger = LoggerFactory.getLogger(HealthCheckServlet.class);

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response) 
            throws ServletException, IOException {
        
        response.setContentType("application/json");
        response.setCharacterEncoding("UTF-8");
        
        PrintWriter out = response.getWriter();
        
        try {
            // Check database connectivity
            boolean dbHealthy = DatabaseConnection.isHealthy();
            
            if (dbHealthy) {
                response.setStatus(HttpServletResponse.SC_OK);
                out.println("{\"status\": \"UP\", \"service\": \"event-servlet\", \"database\": \"UP\"}");
                logger.info("Health check passed - Event Servlet is healthy");
            } else {
                response.setStatus(HttpServletResponse.SC_SERVICE_UNAVAILABLE);
                out.println("{\"status\": \"DOWN\", \"service\": \"event-servlet\", \"database\": \"DOWN\"}");
                logger.warn("Health check failed - Database is not accessible");
            }
            
        } catch (Exception e) {
            response.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
            out.println("{\"status\": \"DOWN\", \"service\": \"event-servlet\", \"error\": \"" + e.getMessage() + "\"}");
            logger.error("Health check failed with exception", e);
        } finally {
            out.close();
        }
    }
}
