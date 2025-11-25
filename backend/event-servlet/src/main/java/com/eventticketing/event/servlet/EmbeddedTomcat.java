package com.eventticketing.event.servlet;

import org.apache.catalina.Context;
import org.apache.catalina.LifecycleException;
import org.apache.catalina.startup.Tomcat;
import org.apache.catalina.webresources.DirResourceSet;
import org.apache.catalina.webresources.StandardRoot;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

/**
 * Embedded Tomcat 10 runner for development
 * Run with: mvn exec:java -Dexec.mainClass="com.eventticketing.event.servlet.EmbeddedTomcat"
 * Or run this class directly from your IDE
 * 
 * Prerequisites: Run 'mvn clean package' first to build the WAR file
 */
public class EmbeddedTomcat {
    public static void main(String[] args) throws Exception {
        Tomcat tomcat = new Tomcat();
        tomcat.setPort(8080);
        tomcat.getConnector();
        
        // Use exploded WAR directory (created by Maven during package)
        String baseDir = System.getProperty("user.dir");
        File explodedWarDir = new File(baseDir, "target/event-servlet");
        File webappDir = new File(baseDir, "src/main/webapp");
        
        Context ctx;
        if (explodedWarDir.exists() && new File(explodedWarDir, "WEB-INF").exists()) {
            // Deploy from exploded WAR directory (preferred)
            ctx = tomcat.addWebapp("/event-servlet", explodedWarDir.getAbsolutePath());
            System.out.println("Deploying from exploded WAR: " + explodedWarDir.getAbsolutePath());
        } else if (webappDir.exists()) {
            // Deploy from webapp directory (development mode)
            ctx = tomcat.addWebapp("/event-servlet", webappDir.getAbsolutePath());
            System.out.println("Deploying from webapp directory: " + webappDir.getAbsolutePath());
            
            // Add classes directory to WEB-INF/classes
            File classesDir = new File(baseDir, "target/classes");
            if (classesDir.exists()) {
                StandardRoot resources = new StandardRoot(ctx);
                resources.addPreResources(new DirResourceSet(resources, "/WEB-INF/classes",
                        classesDir.getAbsolutePath(), "/"));
                ctx.setResources(resources);
                System.out.println("Added classes directory: " + classesDir.getAbsolutePath());
            }
        } else {
            throw new RuntimeException("Cannot find exploded WAR or webapp directory. Run 'mvn clean package' first.");
        }
        
        // Set system properties for database connection
        System.setProperty("DATABASE_URL", 
            System.getenv("DATABASE_URL") != null ? System.getenv("DATABASE_URL") : "jdbc:postgresql://localhost:5432/eventticketing");
        System.setProperty("DATABASE_USER", 
            System.getenv("DATABASE_USER") != null ? System.getenv("DATABASE_USER") : "eventuser");
        System.setProperty("DATABASE_PASSWORD", 
            System.getenv("DATABASE_PASSWORD") != null ? System.getenv("DATABASE_PASSWORD") : "eventpass");
        
        tomcat.start();
        
        System.out.println("==========================================");
        System.out.println("Tomcat 10 started successfully!");
        System.out.println("Event Servlet: http://localhost:8080/event-servlet");
        System.out.println("Health Check: http://localhost:8080/event-servlet/health");
        System.out.println("API: http://localhost:8080/event-servlet/api/events");
        System.out.println("Press Ctrl+C to stop...");
        System.out.println("==========================================");
        
        tomcat.getServer().await();
    }
}

