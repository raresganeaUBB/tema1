#!/bin/bash

# Script simplu pentru a porni toate serverele standalone
# Utilizare: ./start-servers.sh

set -e

# Culori
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
BLUE='\033[0;34m'
NC='\033[0m'

# Încarcă variabilele de mediu
if [ -f ~/.zshrc ]; then
    while IFS= read -r line; do
        if [[ "$line" =~ ^export\ (CATALINA_HOME|JETTY_HOME|JBOSS_HOME)= ]]; then
            eval "$line" 2>/dev/null || true
        fi
    done < ~/.zshrc
fi

# Verifică variabilele
if [ -z "$CATALINA_HOME" ] || [ -z "$JETTY_HOME" ] || [ -z "$JBOSS_HOME" ]; then
    echo "Eroare: Variabilele CATALINA_HOME, JETTY_HOME, JBOSS_HOME nu sunt setate!"
    exit 1
fi

echo -e "${BLUE}=== Pornire Servere Standalone ===${NC}"
echo ""

# 1. Verifică și pornește PostgreSQL
echo -e "${BLUE}[1/4]${NC} Verificare PostgreSQL..."
if ! pg_isready -U eventuser -d eventticketing -h localhost >/dev/null 2>&1; then
    echo -e "${YELLOW}PostgreSQL nu rulează. Încearcă să pornească prin Docker...${NC}"
    cd "$(dirname "$0")"
    docker-compose up -d postgres 2>/dev/null || echo -e "${YELLOW}Docker Compose nu a funcționat. Pornește PostgreSQL manual!${NC}"
    sleep 5
else
    echo -e "${GREEN}✓ PostgreSQL rulează${NC}"
fi

# 2. Pornește Tomcat
echo -e "${BLUE}[2/4]${NC} Pornire Tomcat..."
if ! lsof -i :8080 >/dev/null 2>&1; then
    if [ -f "$CATALINA_HOME/bin/startup.sh" ]; then
        "$CATALINA_HOME/bin/startup.sh" >/dev/null 2>&1
        echo -e "${GREEN}✓ Tomcat pornit${NC}"
        sleep 5
    else
        echo -e "${YELLOW}✗ Tomcat nu a putut fi pornit (startup.sh nu există)${NC}"
    fi
else
    echo -e "${GREEN}✓ Tomcat deja rulează${NC}"
fi

# 3. Pornește Jetty
echo -e "${BLUE}[3/4]${NC} Pornire Jetty..."
if ! lsof -i :8081 >/dev/null 2>&1; then
    mkdir -p "$JETTY_HOME/logs"
    cd "$JETTY_HOME"
    nohup java -jar start.jar jetty.http.port=8081 > logs/jetty.log 2>&1 &
    JETTY_PID=$!
    echo $JETTY_PID > jetty.pid
    echo -e "${GREEN}✓ Jetty pornit (PID: $JETTY_PID)${NC}"
    sleep 5
else
    echo -e "${GREEN}✓ Jetty deja rulează${NC}"
fi

# 4. Pornește WildFly
echo -e "${BLUE}[4/4]${NC} Pornire WildFly..."
if ! lsof -i :8082 >/dev/null 2>&1; then
    if [ -f "$JBOSS_HOME/bin/standalone.sh" ]; then
        cd "$JBOSS_HOME/bin"
        nohup ./standalone.sh > ../standalone/log/server.log 2>&1 &
        WILDFLY_PID=$!
        echo $WILDFLY_PID > ../wildfly.pid
        echo -e "${GREEN}✓ WildFly pornit (PID: $WILDFLY_PID)${NC}"
        sleep 10
    else
        echo -e "${YELLOW}✗ WildFly nu a putut fi pornit (standalone.sh nu există)${NC}"
    fi
else
    echo -e "${GREEN}✓ WildFly deja rulează${NC}"
fi

echo ""
echo -e "${GREEN}=== Toate serverele sunt pornite! ===${NC}"
echo ""
echo "URL-uri:"
echo "  Tomcat:  http://localhost:8080/event-servlet"
echo "  Jetty:   http://localhost:8081/booking-servlet"
echo "  WildFly: http://localhost:8082/event-servlet"
echo "  WildFly: http://localhost:8082/booking-servlet"
echo ""
echo "Pentru a opri serverele, rulează: ./stop-servers.sh"

