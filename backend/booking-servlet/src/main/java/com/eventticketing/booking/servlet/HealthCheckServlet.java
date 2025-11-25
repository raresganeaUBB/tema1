package com.eventticketing.booking.servlet;

import com.eventticketing.shared.database.DatabaseConnection;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import java.io.IOException;
import java.io.PrintWriter;
import java.sql.Connection;
import java.sql.SQLException;

// Note: Servlet mapping is defined in web.xml, not via annotation
// to avoid conflict with web.xml servlet mapping
public class HealthCheckServlet extends HttpServlet {

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        
        response.setContentType("application/json");
        response.setCharacterEncoding("UTF-8");
        
        PrintWriter out = response.getWriter();
        
        try {
            // Check database connection
            try (Connection conn = DatabaseConnection.getConnection()) {
                if (conn != null && !conn.isClosed()) {
                    response.setStatus(HttpServletResponse.SC_OK);
                    out.println("{\"status\": \"healthy\", \"service\": \"booking-servlet\", \"database\": \"connected\"}");
                } else {
                    response.setStatus(HttpServletResponse.SC_SERVICE_UNAVAILABLE);
                    out.println("{\"status\": \"unhealthy\", \"service\": \"booking-servlet\", \"database\": \"disconnected\"}");
                }
            }
        } catch (SQLException e) {
            response.setStatus(HttpServletResponse.SC_SERVICE_UNAVAILABLE);
            out.println("{\"status\": \"unhealthy\", \"service\": \"booking-servlet\", \"database\": \"error\", \"error\": \"" + e.getMessage() + "\"}");
        } catch (Exception e) {
            response.setStatus(HttpServletResponse.SC_SERVICE_UNAVAILABLE);
            out.println("{\"status\": \"unhealthy\", \"service\": \"booking-servlet\", \"error\": \"" + e.getMessage() + "\"}");
        }
    }
}
