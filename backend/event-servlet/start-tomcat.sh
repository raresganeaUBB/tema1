#!/bin/bash

# Start Event Servlet with Embedded Tomcat
# Make sure you've built the project first: mvn clean package

cd "$(dirname "$0")"

echo "Building event-servlet..."
mvn clean package -DskipTests

if [ $? -ne 0 ]; then
    echo "Build failed! Please fix the errors above."
    exit 1
fi

echo ""
echo "Starting Embedded Tomcat..."
echo "Event Servlet will be available at: http://localhost:8080/event-servlet"
echo "Health Check: http://localhost:8080/event-servlet/health"
echo "API: http://localhost:8080/event-servlet/api/events"
echo ""
echo "Press Ctrl+C to stop..."
echo ""

mvn exec:java -Dexec.mainClass="com.eventticketing.event.servlet.EmbeddedTomcat"

