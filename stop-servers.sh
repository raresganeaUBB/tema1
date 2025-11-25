#!/bin/bash

# Script pentru a opri toate serverele
# Utilizare: ./stop-servers.sh

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

echo -e "${BLUE}=== Oprire Servere ===${NC}"
echo ""

# Oprește Tomcat
if [ -f "$CATALINA_HOME/bin/shutdown.sh" ]; then
    echo -e "${BLUE}Oprire Tomcat...${NC}"
    "$CATALINA_HOME/bin/shutdown.sh" >/dev/null 2>&1 || true
    echo -e "${GREEN}✓ Tomcat oprit${NC}"
fi

# Oprește Jetty
if [ -f "$JETTY_HOME/jetty.pid" ]; then
    JETTY_PID=$(cat "$JETTY_HOME/jetty.pid" 2>/dev/null)
    if [ -n "$JETTY_PID" ] && kill -0 "$JETTY_PID" 2>/dev/null; then
        echo -e "${BLUE}Oprire Jetty...${NC}"
        kill "$JETTY_PID" 2>/dev/null || true
        rm "$JETTY_HOME/jetty.pid" 2>/dev/null || true
        echo -e "${GREEN}✓ Jetty oprit${NC}"
    fi
fi

# Oprește WildFly
if [ -f "$JBOSS_HOME/wildfly.pid" ]; then
    WILDFLY_PID=$(cat "$JBOSS_HOME/wildfly.pid" 2>/dev/null)
    if [ -n "$WILDFLY_PID" ] && kill -0 "$WILDFLY_PID" 2>/dev/null; then
        echo -e "${BLUE}Oprire WildFly...${NC}"
        kill "$WILDFLY_PID" 2>/dev/null || true
        rm "$JBOSS_HOME/wildfly.pid" 2>/dev/null || true
        echo -e "${GREEN}✓ WildFly oprit${NC}"
    fi
fi

# Oprește prin jboss-cli dacă există
if [ -f "$JBOSS_HOME/bin/jboss-cli.sh" ]; then
    "$JBOSS_HOME/bin/jboss-cli.sh" --connect --command=":shutdown" >/dev/null 2>&1 || true
fi

echo ""
echo -e "${GREEN}=== Toate serverele au fost oprite ===${NC}"

