#!/bin/bash

# Script pentru deploy și execuție simultană pe Tomcat, Jetty și WildFly
# Utilizare: ./deploy-all-servers.sh [start|stop|restart|status]

# Încarcă variabilele de mediu din .zshrc sau .bashrc dacă există
# Folosește eval pentru a gestiona corect spațiile din căi
if [ -f ~/.zshrc ]; then
    while IFS= read -r line; do
        if [[ "$line" =~ ^export\ (CATALINA_HOME|JETTY_HOME|JBOSS_HOME)= ]]; then
            eval "$line" 2>/dev/null || true
        fi
    done < ~/.zshrc
fi
if [ -f ~/.bashrc ]; then
    while IFS= read -r line; do
        if [[ "$line" =~ ^export\ (CATALINA_HOME|JETTY_HOME|JBOSS_HOME)= ]]; then
            eval "$line" 2>/dev/null || true
        fi
    done < ~/.bashrc
fi

set -e

# Culori pentru output
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
BLUE='\033[0;34m'
NC='\033[0m' # No Color

# Configurare căi (folosește variabilele de mediu sau valorile implicite)
# Scriptul va folosi variabilele exportate din .zshrc/.bashrc
if [ -z "$CATALINA_HOME" ]; then
    CATALINA_HOME="/opt/tomcat"
fi
if [ -z "$JETTY_HOME" ]; then
    JETTY_HOME="/opt/jetty"
fi
if [ -z "$JBOSS_HOME" ]; then
    JBOSS_HOME="/opt/wildfly"
fi

# Debug: afișează căile folosite
if [ "${DEBUG:-0}" = "1" ]; then
    echo "CATALINA_HOME: $CATALINA_HOME"
    echo "JETTY_HOME: $JETTY_HOME"
    echo "JBOSS_HOME: $JBOSS_HOME"
fi

# Porturi
TOMCAT_PORT=8080
JETTY_PORT=8081
WILDFLY_PORT=8082

# Directoare proiect
PROJECT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
BACKEND_DIR="$PROJECT_DIR/backend"
TARGET_EVENT_WAR="$BACKEND_DIR/event-servlet/target/event-servlet.war"
TARGET_BOOKING_WAR="$BACKEND_DIR/booking-servlet/target/booking-servlet.war"
TARGET_BOOKING_EXPLODED="$BACKEND_DIR/booking-servlet/target/booking-servlet"

# Funcții utilitare
print_status() {
    echo -e "${BLUE}[INFO]${NC} $1"
}

print_success() {
    echo -e "${GREEN}[SUCCESS]${NC} $1"
}

print_warning() {
    echo -e "${YELLOW}[WARNING]${NC} $1"
}

print_error() {
    echo -e "${RED}[ERROR]${NC} $1"
}

check_prerequisites() {
    print_status "Verificare prerechizite..."
    
    local missing=()
    
    if [ ! -d "$CATALINA_HOME" ]; then
        missing+=("Tomcat nu este instalat la $CATALINA_HOME")
    fi
    
    if [ ! -d "$JETTY_HOME" ]; then
        missing+=("Jetty nu este instalat la $JETTY_HOME")
    fi
    
    if [ ! -d "$JBOSS_HOME" ]; then
        missing+=("WildFly nu este instalat la $JBOSS_HOME")
    fi
    
    if ! command -v mvn &> /dev/null; then
        missing+=("Maven nu este instalat")
    fi
    
    if ! command -v java &> /dev/null; then
        missing+=("Java nu este instalat")
    fi
    
    if [ ${#missing[@]} -gt 0 ]; then
        print_error "Lipsesc următoarele prerechizite:"
        for item in "${missing[@]}"; do
            echo "  - $item"
        done
        echo ""
        echo "Setează variabilele de mediu:"
        echo "  export CATALINA_HOME=/path/to/tomcat"
        echo "  export JETTY_HOME=/path/to/jetty"
        echo "  export JBOSS_HOME=/path/to/wildfly"
        exit 1
    fi
    
    print_success "Toate prerechizitele sunt îndeplinite"
}

build_project() {
    print_status "Build proiect..."
    
    cd "$BACKEND_DIR"
    
    if ! mvn clean package -DskipTests; then
        print_error "Build eșuat!"
        exit 1
    fi
    
    if [ ! -f "$TARGET_EVENT_WAR" ]; then
        print_error "event-servlet.war nu a fost generat!"
        exit 1
    fi
    
    if [ ! -f "$TARGET_BOOKING_WAR" ]; then
        print_error "booking-servlet.war nu a fost generat!"
        exit 1
    fi
    
    print_success "Build completat cu succes"
}

deploy_tomcat() {
    print_status "Deploy pe Tomcat (port $TOMCAT_PORT)..."
    
    # Copiere WAR
    cp "$TARGET_EVENT_WAR" "$CATALINA_HOME/webapps/"
    print_success "WAR copiat în $CATALINA_HOME/webapps/"
    
    # Configurare variabile de mediu
    if [ ! -f "$CATALINA_HOME/bin/setenv.sh" ]; then
        cat > "$CATALINA_HOME/bin/setenv.sh" << 'EOF'
#!/bin/sh
export DATABASE_URL=jdbc:postgresql://localhost:5432/eventticketing
export DATABASE_USER=eventuser
export DATABASE_PASSWORD=eventpass
export SERVLET_PORT=8080
EOF
        chmod +x "$CATALINA_HOME/bin/setenv.sh"
        print_success "setenv.sh creat"
    fi
}

start_tomcat() {
    print_status "Pornire Tomcat..."
    
    if [ -f "$CATALINA_HOME/bin/catalina.sh" ]; then
        "$CATALINA_HOME/bin/catalina.sh" start
        print_success "Tomcat pornit"
    else
        print_error "catalina.sh nu a fost găsit!"
        exit 1
    fi
}

stop_tomcat() {
    print_status "Oprire Tomcat..."
    
    if [ -f "$CATALINA_HOME/bin/catalina.sh" ]; then
        "$CATALINA_HOME/bin/catalina.sh" stop
        print_success "Tomcat oprit"
    fi
}

deploy_jetty() {
    print_status "Deploy pe Jetty (port $JETTY_PORT)..."
    
    # Copiere exploded WAR
    if [ -d "$TARGET_BOOKING_EXPLODED" ]; then
        rm -rf "$JETTY_HOME/webapps/booking-servlet"
        cp -r "$TARGET_BOOKING_EXPLODED" "$JETTY_HOME/webapps/"
        print_success "Exploded WAR copiat în $JETTY_HOME/webapps/"
    else
        print_error "Exploded WAR nu există! Rulează 'mvn clean package'"
        exit 1
    fi
    
    # Configurare start.ini
    if [ ! -f "$JETTY_HOME/start.d/database.ini" ]; then
        mkdir -p "$JETTY_HOME/start.d"
        cat > "$JETTY_HOME/start.d/database.ini" << EOF
--exec
-DDATABASE_URL=jdbc:postgresql://localhost:5432/eventticketing
-DDATABASE_USER=eventuser
-DDATABASE_PASSWORD=eventpass
-DSERVLET_PORT=$JETTY_PORT
-DEVENT_SERVICE_URL=http://localhost:$TOMCAT_PORT/event-servlet/api/events
-DEVENT_SERVLET_URL=http://localhost:$TOMCAT_PORT/event-servlet
EOF
        print_success "database.ini creat"
    fi
    
    # Asigură-te că modulul http și webapp sunt activate (pentru Jetty 12)
    if ! grep -q "jetty.http.port" "$JETTY_HOME/start.ini" 2>/dev/null; then
        echo "jetty.http.port=$JETTY_PORT" >> "$JETTY_HOME/start.ini"
    fi
}

start_jetty() {
    print_status "Pornire Jetty..."
    
    # Creează directorul logs dacă nu există
    mkdir -p "$JETTY_HOME/logs"
    
    cd "$JETTY_HOME"
    # Jetty 12 folosește modulul http și webapp, nu deploy
    nohup java -jar start.jar jetty.http.port=$JETTY_PORT > "$JETTY_HOME/logs/jetty.log" 2>&1 &
    JETTY_PID=$!
    echo $JETTY_PID > "$JETTY_HOME/jetty.pid"
    print_success "Jetty pornit (PID: $JETTY_PID)"
}

stop_jetty() {
    print_status "Oprire Jetty..."
    
    if [ -f "$JETTY_HOME/jetty.pid" ]; then
        JETTY_PID=$(cat "$JETTY_HOME/jetty.pid")
        if kill -0 "$JETTY_PID" 2>/dev/null; then
            kill "$JETTY_PID"
            rm "$JETTY_HOME/jetty.pid"
            print_success "Jetty oprit"
        else
            print_warning "Jetty nu rulează"
            rm "$JETTY_HOME/jetty.pid"
        fi
    else
        print_warning "PID file nu există"
    fi
}

deploy_wildfly() {
    print_status "Deploy pe WildFly (port $WILDFLY_PORT)..."
    
    # Copiere WAR-uri
    cp "$TARGET_EVENT_WAR" "$JBOSS_HOME/standalone/deployments/"
    cp "$TARGET_BOOKING_WAR" "$JBOSS_HOME/standalone/deployments/"
    print_success "WAR-uri copiate în $JBOSS_HOME/standalone/deployments/"
    
    # Configurare variabile de mediu
    if [ -f "$JBOSS_HOME/bin/standalone.conf" ]; then
        if ! grep -q "DATABASE_URL" "$JBOSS_HOME/bin/standalone.conf"; then
            cat >> "$JBOSS_HOME/bin/standalone.conf" << EOF

# Event Ticketing System Configuration
JAVA_OPTS="\$JAVA_OPTS -DDATABASE_URL=jdbc:postgresql://localhost:5432/eventticketing"
JAVA_OPTS="\$JAVA_OPTS -DDATABASE_USER=eventuser"
JAVA_OPTS="\$JAVA_OPTS -DDATABASE_PASSWORD=eventpass"
JAVA_OPTS="\$JAVA_OPTS -DSERVLET_PORT=$WILDFLY_PORT"
JAVA_OPTS="\$JAVA_OPTS -DEVENT_SERVICE_URL=http://localhost:$TOMCAT_PORT/event-servlet/api/events"
EOF
            print_success "standalone.conf actualizat"
        fi
    fi
}

start_wildfly() {
    print_status "Pornire WildFly..."
    
    if [ -f "$JBOSS_HOME/bin/standalone.sh" ]; then
        cd "$JBOSS_HOME/bin"
        nohup ./standalone.sh > "$JBOSS_HOME/standalone/log/server.log" 2>&1 &
        WILDFLY_PID=$!
        echo $WILDFLY_PID > "$JBOSS_HOME/wildfly.pid"
        print_success "WildFly pornit (PID: $WILDFLY_PID)"
    else
        print_error "standalone.sh nu a fost găsit!"
        exit 1
    fi
}

stop_wildfly() {
    print_status "Oprire WildFly..."
    
    if [ -f "$JBOSS_HOME/wildfly.pid" ]; then
        WILDFLY_PID=$(cat "$JBOSS_HOME/wildfly.pid")
        if kill -0 "$WILDFLY_PID" 2>/dev/null; then
            kill "$WILDFLY_PID"
            rm "$JBOSS_HOME/wildfly.pid"
            print_success "WildFly oprit"
        else
            print_warning "WildFly nu rulează"
            rm "$JBOSS_HOME/wildfly.pid"
        fi
    else
        # Încearcă să oprească prin jboss-cli
        if [ -f "$JBOSS_HOME/bin/jboss-cli.sh" ]; then
            "$JBOSS_HOME/bin/jboss-cli.sh" --connect --command=":shutdown" 2>/dev/null || true
        fi
    fi
}

check_status() {
    print_status "Verificare status servere..."
    
    # Tomcat
    if curl -s -f "http://localhost:$TOMCAT_PORT/event-servlet/health" > /dev/null 2>&1; then
        print_success "Tomcat: RUNNING (http://localhost:$TOMCAT_PORT)"
    else
        print_warning "Tomcat: NOT RUNNING"
    fi
    
    # Jetty
    if curl -s -f "http://localhost:$JETTY_PORT/booking-servlet/health" > /dev/null 2>&1; then
        print_success "Jetty: RUNNING (http://localhost:$JETTY_PORT)"
    else
        print_warning "Jetty: NOT RUNNING"
    fi
    
    # WildFly
    if curl -s -f "http://localhost:$WILDFLY_PORT/event-servlet/health" > /dev/null 2>&1; then
        print_success "WildFly: RUNNING (http://localhost:$WILDFLY_PORT)"
    else
        print_warning "WildFly: NOT RUNNING"
    fi
}

wait_for_servers() {
    print_status "Așteptare pornire servere (max 60 secunde)..."
    
    local max_wait=60
    local waited=0
    
    while [ $waited -lt $max_wait ]; do
        local all_up=true
        
        if ! curl -s -f "http://localhost:$TOMCAT_PORT/event-servlet/health" > /dev/null 2>&1; then
            all_up=false
        fi
        
        if ! curl -s -f "http://localhost:$JETTY_PORT/booking-servlet/health" > /dev/null 2>&1; then
            all_up=false
        fi
        
        if ! curl -s -f "http://localhost:$WILDFLY_PORT/event-servlet/health" > /dev/null 2>&1; then
            all_up=false
        fi
        
        if [ "$all_up" = true ]; then
            print_success "Toate serverele sunt pornite!"
            return 0
        fi
        
        sleep 2
        waited=$((waited + 2))
        echo -n "."
    done
    
    echo ""
    print_warning "Timeout - unele servere nu s-au pornit complet"
    check_status
}

# Funcție principală
main() {
    local command="${1:-start}"
    
    case "$command" in
        start)
            check_prerequisites
            build_project
            deploy_tomcat
            deploy_jetty
            deploy_wildfly
            start_tomcat
            sleep 5
            start_jetty
            sleep 5
            start_wildfly
            wait_for_servers
            check_status
            echo ""
            print_success "Deploy completat!"
            echo ""
            echo "URL-uri:"
            echo "  Tomcat:  http://localhost:$TOMCAT_PORT/event-servlet"
            echo "  Jetty:   http://localhost:$JETTY_PORT/booking-servlet"
            echo "  WildFly: http://localhost:$WILDFLY_PORT/event-servlet"
            echo "  WildFly: http://localhost:$WILDFLY_PORT/booking-servlet"
            ;;
        stop)
            stop_tomcat
            stop_jetty
            stop_wildfly
            print_success "Toate serverele au fost oprite"
            ;;
        restart)
            $0 stop
            sleep 5
            $0 start
            ;;
        status)
            check_status
            ;;
        deploy)
            check_prerequisites
            build_project
            deploy_tomcat
            deploy_jetty
            deploy_wildfly
            print_success "Deploy completat! Pornește serverele manual sau folosește 'start'"
            ;;
        *)
            echo "Utilizare: $0 [start|stop|restart|status|deploy]"
            echo ""
            echo "Comenzi:"
            echo "  start   - Build, deploy și pornire toate serverele"
            echo "  stop    - Oprește toate serverele"
            echo "  restart - Oprește și repornește toate serverele"
            echo "  status  - Verifică statusul serverelor"
            echo "  deploy  - Doar build și deploy (fără pornire)"
            exit 1
            ;;
    esac
}

main "$@"


