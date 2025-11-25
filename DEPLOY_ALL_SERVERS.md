# Deploy și Execuție pe Tomcat, Jetty și WildFly Simultan

Acest ghid explică cum să deploy și să rulezi aplicația pe toate cele trei servere (Tomcat, Jetty și WildFly) simultan, fără IDE.

## Prerechizite

1. **Java JDK 17** instalat și configurat
2. **Maven 3.6+** instalat
3. **PostgreSQL** rulând pe localhost:5432
4. **Tomcat 10** instalat local
5. **Jetty 12** instalat local
6. **WildFly 30** instalat local

## Instalare Servere

### Tomcat 10

```bash
# Descarcă Tomcat 10
wget https://archive.apache.org/dist/tomcat/tomcat-10/v10.1.20/bin/apache-tomcat-10.1.20.tar.gz
tar -xzf apache-tomcat-10.1.20.tar.gz
sudo mv apache-tomcat-10.1.20 /opt/tomcat
export CATALINA_HOME=/opt/tomcat
```

### Jetty 12

```bash
# Descarcă Jetty 12
wget https://repo1.maven.org/maven2/org/eclipse/jetty/jetty-home/12.0.5/jetty-home-12.0.5.tar.gz
tar -xzf jetty-home-12.0.5.tar.gz
sudo mv jetty-home-12.0.5 /opt/jetty
export JETTY_HOME=/opt/jetty
```

### WildFly 30

```bash
# Descarcă WildFly 30
wget https://github.com/wildfly/wildfly/releases/download/30.0.1.Final/wildfly-30.0.1.Final.tar.gz
tar -xzf wildfly-30.0.1.Final.tar.gz
sudo mv wildfly-30.0.1.Final /opt/wildfly
export JBOSS_HOME=/opt/wildfly
```

## Configurare Variabile de Mediu

Adaugă în `~/.bashrc` sau `~/.zshrc`:

```bash
export CATALINA_HOME=/opt/tomcat
export JETTY_HOME=/opt/jetty
export JBOSS_HOME=/opt/wildfly
export PATH=$PATH:$CATALINA_HOME/bin:$JETTY_HOME/bin:$JBOSS_HOME/bin
```

## Utilizare Script Automatizat

### Metoda 1: Script Automatizat (Recomandat)

Scriptul `deploy-all-servers.sh` automatizează tot procesul:

```bash
# Setează variabilele de mediu
export CATALINA_HOME=/opt/tomcat
export JETTY_HOME=/opt/jetty
export JBOSS_HOME=/opt/wildfly

# Build, deploy și pornire toate serverele
./deploy-all-servers.sh start

# Verificare status
./deploy-all-servers.sh status

# Oprire toate serverele
./deploy-all-servers.sh stop

# Restart
./deploy-all-servers.sh restart

# Doar deploy (fără pornire)
./deploy-all-servers.sh deploy
```

### Metoda 2: Deploy Manual

#### Pasul 1: Build Proiectul

```bash
cd backend
mvn clean package -DskipTests
```

Aceasta va genera:

- `backend/event-servlet/target/event-servlet.war`
- `backend/booking-servlet/target/booking-servlet.war`
- `backend/booking-servlet/target/booking-servlet/` (exploded WAR)

#### Pasul 2: Configurare PostgreSQL

Asigură-te că PostgreSQL rulează:

```bash
# Verificare
psql -U eventuser -d eventticketing -c "SELECT 1;"

# Dacă nu există, creează baza de date
createdb -U postgres eventticketing
psql -U eventuser -d eventticketing -f database/schema.sql
psql -U eventuser -d eventticketing -f database/sample-data.sql
```

#### Pasul 3: Deploy pe Tomcat (Port 8080)

```bash
# Copiere WAR
cp backend/event-servlet/target/event-servlet.war $CATALINA_HOME/webapps/

# Configurare variabile de mediu
cat > $CATALINA_HOME/bin/setenv.sh << 'EOF'
#!/bin/sh
export DATABASE_URL=jdbc:postgresql://localhost:5432/eventticketing
export DATABASE_USER=eventuser
export DATABASE_PASSWORD=eventpass
export SERVLET_PORT=8080
EOF
chmod +x $CATALINA_HOME/bin/setenv.sh

# Pornire Tomcat
$CATALINA_HOME/bin/startup.sh

# Verificare
curl http://localhost:8080/event-servlet/health
```

#### Pasul 4: Deploy pe Jetty (Port 8081)

```bash
# Copiere exploded WAR
cp -r backend/booking-servlet/target/booking-servlet $JETTY_HOME/webapps/

# Configurare
mkdir -p $JETTY_HOME/start.d
cat > $JETTY_HOME/start.d/database.ini << EOF
--exec
-DDATABASE_URL=jdbc:postgresql://localhost:5432/eventticketing
-DDATABASE_USER=eventuser
-DDATABASE_PASSWORD=eventpass
-DSERVLET_PORT=8081
-DEVENT_SERVICE_URL=http://localhost:8080/event-servlet/api/events
-DEVENT_SERVLET_URL=http://localhost:8080/event-servlet
EOF

# Asigură-te că modulul deploy este activat
echo "--module=deploy" >> $JETTY_HOME/start.ini

# Pornire Jetty
cd $JETTY_HOME
java -jar start.jar > logs/jetty.log 2>&1 &

# Verificare
sleep 5
curl http://localhost:8081/booking-servlet/health
```

#### Pasul 5: Deploy pe WildFly (Port 8082)

```bash
# Copiere WAR-uri
cp backend/event-servlet/target/event-servlet.war $JBOSS_HOME/standalone/deployments/
cp backend/booking-servlet/target/booking-servlet.war $JBOSS_HOME/standalone/deployments/

# Configurare variabile de mediu
cat >> $JBOSS_HOME/bin/standalone.conf << EOF

# Event Ticketing System Configuration
JAVA_OPTS="\$JAVA_OPTS -DDATABASE_URL=jdbc:postgresql://localhost:5432/eventticketing"
JAVA_OPTS="\$JAVA_OPTS -DDATABASE_USER=eventuser"
JAVA_OPTS="\$JAVA_OPTS -DDATABASE_PASSWORD=eventpass"
JAVA_OPTS="\$JAVA_OPTS -DSERVLET_PORT=8082"
JAVA_OPTS="\$JAVA_OPTS -DEVENT_SERVICE_URL=http://localhost:8080/event-servlet/api/events"
EOF

# Pornire WildFly
cd $JBOSS_HOME/bin
./standalone.sh > ../standalone/log/server.log 2>&1 &

# Verificare
sleep 10
curl http://localhost:8082/event-servlet/health
curl http://localhost:8082/booking-servlet/health
```

## Verificare Deploy Simultan

Verifică că toate cele trei servere rulează:

```bash
# Tomcat (port 8080)
curl http://localhost:8080/event-servlet/health
curl http://localhost:8080/event-servlet/api/events

# Jetty (port 8081)
curl http://localhost:8081/booking-servlet/health
curl http://localhost:8081/booking-servlet/api/bookings/

# WildFly (port 8082)
curl http://localhost:8082/event-servlet/health
curl http://localhost:8082/event-servlet/api/events
curl http://localhost:8082/booking-servlet/health
curl http://localhost:8082/booking-servlet/api/bookings/
```

## Oprire Servere

### Oprire Manuală

```bash
# Tomcat
$CATALINA_HOME/bin/shutdown.sh

# Jetty
pkill -f "jetty.*start.jar"

# WildFly
$JBOSS_HOME/bin/jboss-cli.sh --connect --command=":shutdown"
```

### Oprire cu Script

```bash
./deploy-all-servers.sh stop
```

## Configurare WildFly Datasource (Opțional)

Pentru o configurare mai avansată în WildFly, poți configura datasource-ul prin CLI:

```bash
cd $JBOSS_HOME/bin
./jboss-cli.sh --connect

# În CLI:
module add --name=org.postgresql \
  --resources=/path/to/postgresql-42.6.0.jar \
  --dependencies=javax.api,javax.transaction.api

data-source add \
  --name=EventTicketingDS \
  --jndi-name=java:jboss/datasources/EventTicketingDS \
  --driver-name=postgresql \
  --connection-url=jdbc:postgresql://localhost:5432/eventticketing \
  --user-name=eventuser \
  --password=eventpass \
  --enabled=true
```

## Porturi și URL-uri

| Server  | Port | Event Servlet                       | Booking Servlet                       |
| ------- | ---- | ----------------------------------- | ------------------------------------- |
| Tomcat  | 8080 | http://localhost:8080/event-servlet | -                                     |
| Jetty   | 8081 | -                                   | http://localhost:8081/booking-servlet |
| WildFly | 8082 | http://localhost:8082/event-servlet | http://localhost:8082/booking-servlet |

## Troubleshooting

### Tomcat nu pornește

```bash
# Verifică log-urile
tail -f $CATALINA_HOME/logs/catalina.out

# Verifică dacă portul este ocupat
lsof -i :8080

# Verifică permisiuni
ls -la $CATALINA_HOME/webapps/
```

### Jetty nu pornește

```bash
# Verifică log-urile
tail -f $JETTY_HOME/logs/jetty.log

# Verifică configurarea
cat $JETTY_HOME/start.ini
cat $JETTY_HOME/start.d/database.ini
```

### WildFly nu pornește

```bash
# Verifică log-urile
tail -f $JBOSS_HOME/standalone/log/server.log

# Verifică configurarea
cat $JBOSS_HOME/bin/standalone.conf | grep DATABASE
```

### Probleme cu conexiunea la baza de date

```bash
# Verifică că PostgreSQL rulează
pg_isready -U eventuser -d eventticketing

# Testează conexiunea
psql -U eventuser -d eventticketing -c "SELECT 1;"
```

## Note Importante

1. **Ordinea de pornire**: Tomcat trebuie să pornească primul, apoi Jetty (care depinde de Tomcat), apoi WildFly
2. **Porturi**: Asigură-te că porturile 8080, 8081, 8082 nu sunt ocupate
3. **Variabile de mediu**: Toate serverele trebuie să aibă acces la variabilele de mediu pentru baza de date
4. **Comunicare între servleturi**: Booking Servlet (Jetty) comunică cu Event Servlet (Tomcat) prin HTTP
5. **WildFly**: Deploy-ează ambele servleturi, dar comunicarea între ele se face prin Tomcat

## Concluzie

Aplicația poate fi deploy-ată și rulată simultan pe toate cele trei servere. Scriptul `deploy-all-servers.sh` automatizează procesul, dar poți folosi și metodele manuale pentru mai mult control.
