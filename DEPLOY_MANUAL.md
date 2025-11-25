# Deploy Manual pe Tomcat, Jetty și WildFly

Acest ghid explică cum să deploy și să rulezi aplicația pe toate cele trei servere (Tomcat, Jetty și WildFly) simultan, fără IDE.

## Prerechizite

1. **Java JDK 17** instalat și configurat
2. **Maven 3.6+** instalat
3. **PostgreSQL** rulând (sau Docker pentru PostgreSQL)
4. **Tomcat 10** instalat local
5. **Jetty 12** instalat local
6. **WildFly 30** instalat local

## Pasul 1: Build Proiectul

Construiește toate modulele și generează fișierele WAR:

```bash
cd backend
mvn clean package -DskipTests
```

Aceasta va genera:

- `backend/event-servlet/target/event-servlet.war`
- `backend/booking-servlet/target/booking-servlet.war`
- `backend/event-servlet/target/event-servlet/` (exploded WAR)
- `backend/booking-servlet/target/booking-servlet/` (exploded WAR)

## Pasul 2: Configurare Baza de Date

Asigură-te că PostgreSQL rulează și baza de date este creată:

```bash
# Dacă folosești Docker
docker run -d --name postgres-event \
  -e POSTGRES_DB=eventticketing \
  -e POSTGRES_USER=eventuser \
  -e POSTGRES_PASSWORD=eventpass \
  -p 5432:5432 \
  postgres:15

# Sau folosește docker-compose doar pentru PostgreSQL
docker-compose up -d postgres
```

Creează schema:

```bash
psql -U eventuser -d eventticketing -f database/schema.sql
psql -U eventuser -d eventticketing -f database/sample-data.sql
```

## Pasul 3: Deploy pe Tomcat (Port 8080)

### 3.1 Copiere WAR în Tomcat

```bash
# Copiază WAR-ul în directorul webapps al Tomcat
cp backend/event-servlet/target/event-servlet.war $CATALINA_HOME/webapps/

# Sau dacă nu ai CATALINA_HOME setat:
cp backend/event-servlet/target/event-servlet.war /path/to/tomcat/webapps/
```

### 3.2 Configurare Variabile de Mediu

Creează fișierul `$CATALINA_HOME/bin/setenv.sh` (sau `setenv.bat` pe Windows):

```bash
#!/bin/sh
export DATABASE_URL=jdbc:postgresql://localhost:5432/eventticketing
export DATABASE_USER=eventuser
export DATABASE_PASSWORD=eventpass
export SERVLET_PORT=8080
```

### 3.3 Pornire Tomcat

```bash
cd $CATALINA_HOME/bin
./startup.sh
# Sau pe Windows: startup.bat
```

### 3.4 Verificare

```bash
curl http://localhost:8080/event-servlet/health
curl http://localhost:8080/event-servlet/api/events
```

## Pasul 4: Deploy pe Jetty (Port 8081)

### 4.1 Copiere WAR în Jetty

```bash
# Copiază directorul exploded WAR în webapps
cp -r backend/booking-servlet/target/booking-servlet $JETTY_HOME/webapps/booking-servlet

# Sau dacă nu ai JETTY_HOME setat:
cp -r backend/booking-servlet/target/booking-servlet /path/to/jetty/webapps/booking-servlet
```

### 4.2 Configurare Variabile de Mediu

Creează fișierul `$JETTY_HOME/start.ini` sau adaugă în `start.d/jetty.ini`:

```ini
--module=deploy
--module=http
jetty.http.port=8081
```

Creează fișierul `$JETTY_HOME/start.d/database.ini`:

```ini
--exec
-DDATABASE_URL=jdbc:postgresql://localhost:5432/eventticketing
-DDATABASE_USER=eventuser
-DDATABASE_PASSWORD=eventpass
-DSERVLET_PORT=8081
-DEVENT_SERVICE_URL=http://localhost:8080/event-servlet/api/events
-DEVENT_SERVLET_URL=http://localhost:8080/event-servlet
```

### 4.3 Pornire Jetty

```bash
cd $JETTY_HOME
java -jar start.jar
```

### 4.4 Verificare

```bash
curl http://localhost:8081/booking-servlet/health
curl http://localhost:8081/booking-servlet/api/bookings/
```

## Pasul 5: Deploy pe WildFly (Port 8082)

### 5.1 Copiere WAR în WildFly

```bash
# Copiază ambele WAR-uri în deployments
cp backend/event-servlet/target/event-servlet.war $JBOSS_HOME/standalone/deployments/
cp backend/booking-servlet/target/booking-servlet.war $JBOSS_HOME/standalone/deployments/

# Sau dacă nu ai JBOSS_HOME setat:
cp backend/event-servlet/target/event-servlet.war /path/to/wildfly/standalone/deployments/
cp backend/booking-servlet/target/booking-servlet.war /path/to/wildfly/standalone/deployments/
```

### 5.2 Configurare WildFly

Editează `$JBOSS_HOME/standalone/configuration/standalone.xml` sau folosește CLI:

```bash
cd $JBOSS_HOME/bin
./jboss-cli.sh --connect
```

În CLI, configurează datasource-ul PostgreSQL:

```bash
# Adaugă driver PostgreSQL
module add --name=org.postgresql --resources=/path/to/postgresql-42.6.0.jar --dependencies=javax.api,javax.transaction.api

# Sau copiază JAR-ul în modules
# $JBOSS_HOME/modules/org/postgresql/main/postgresql-42.6.0.jar
# $JBOSS_HOME/modules/org/postgresql/main/module.xml

# Creează datasource
data-source add \
  --name=EventTicketingDS \
  --jndi-name=java:jboss/datasources/EventTicketingDS \
  --driver-name=postgresql \
  --connection-url=jdbc:postgresql://localhost:5432/eventticketing \
  --user-name=eventuser \
  --password=eventpass \
  --enabled=true

# Sau configurează prin variabile de mediu
```

### 5.3 Configurare Variabile de Mediu

Editează `$JBOSS_HOME/bin/standalone.conf` (sau `standalone.conf.bat` pe Windows):

```bash
JAVA_OPTS="$JAVA_OPTS -DDATABASE_URL=jdbc:postgresql://localhost:5432/eventticketing"
JAVA_OPTS="$JAVA_OPTS -DDATABASE_USER=eventuser"
JAVA_OPTS="$JAVA_OPTS -DDATABASE_PASSWORD=eventpass"
JAVA_OPTS="$JAVA_OPTS -DSERVLET_PORT=8082"
JAVA_OPTS="$JAVA_OPTS -DEVENT_SERVICE_URL=http://localhost:8080/event-servlet/api/events"
```

### 5.4 Pornire WildFly

```bash
cd $JBOSS_HOME/bin
./standalone.sh
# Sau pe Windows: standalone.bat
```

### 5.5 Verificare

```bash
curl http://localhost:8082/event-servlet/health
curl http://localhost:8082/event-servlet/api/events
curl http://localhost:8082/booking-servlet/health
curl http://localhost:8082/booking-servlet/api/bookings/
```

## Pasul 6: Verificare Deploy Simultan

Verifică că toate cele trei servere rulează:

```bash
# Tomcat (port 8080)
curl http://localhost:8080/event-servlet/health

# Jetty (port 8081)
curl http://localhost:8081/booking-servlet/health

# WildFly (port 8082)
curl http://localhost:8082/event-servlet/health
curl http://localhost:8082/booking-servlet/health
```

## Scripturi de Automatizare

### Script pentru Build și Deploy (deploy-all.sh)

```bash
#!/bin/bash

echo "Building project..."
cd backend
mvn clean package -DskipTests

if [ $? -ne 0 ]; then
    echo "Build failed!"
    exit 1
fi

echo "Deploying to Tomcat..."
cp event-servlet/target/event-servlet.war $CATALINA_HOME/webapps/
echo "Tomcat deployment: $CATALINA_HOME/webapps/event-servlet.war"

echo "Deploying to Jetty..."
rm -rf $JETTY_HOME/webapps/booking-servlet
cp -r booking-servlet/target/booking-servlet $JETTY_HOME/webapps/
echo "Jetty deployment: $JETTY_HOME/webapps/booking-servlet"

echo "Deploying to WildFly..."
cp event-servlet/target/event-servlet.war $JBOSS_HOME/standalone/deployments/
cp booking-servlet/target/booking-servlet.war $JBOSS_HOME/standalone/deployments/
echo "WildFly deployment: $JBOSS_HOME/standalone/deployments/"

echo "Deployment complete!"
echo "Start servers manually:"
echo "  Tomcat: $CATALINA_HOME/bin/startup.sh"
echo "  Jetty: cd $JETTY_HOME && java -jar start.jar"
echo "  WildFly: $JBOSS_HOME/bin/standalone.sh"
```

### Script pentru Windows (deploy-all.bat)

```batch
@echo off
echo Building project...
cd backend
call mvn clean package -DskipTests

if %ERRORLEVEL% NEQ 0 (
    echo Build failed!
    exit /b 1
)

echo Deploying to Tomcat...
copy event-servlet\target\event-servlet.war %CATALINA_HOME%\webapps\
echo Tomcat deployment: %CATALINA_HOME%\webapps\event-servlet.war

echo Deploying to Jetty...
if exist %JETTY_HOME%\webapps\booking-servlet rmdir /s /q %JETTY_HOME%\webapps\booking-servlet
xcopy /E /I booking-servlet\target\booking-servlet %JETTY_HOME%\webapps\booking-servlet
echo Jetty deployment: %JETTY_HOME%\webapps\booking-servlet

echo Deploying to WildFly...
copy event-servlet\target\event-servlet.war %JBOSS_HOME%\standalone\deployments\
copy booking-servlet\target\booking-servlet.war %JBOSS_HOME%\standalone\deployments\
echo WildFly deployment: %JBOSS_HOME%\standalone\deployments\

echo Deployment complete!
echo Start servers manually:
echo   Tomcat: %CATALINA_HOME%\bin\startup.bat
echo   Jetty: cd %JETTY_HOME% ^&^& java -jar start.jar
echo   WildFly: %JBOSS_HOME%\bin\standalone.bat
```

## Porturi Utilizate

- **8080**: Tomcat - Event Servlet
- **8081**: Jetty - Booking Servlet
- **8082**: WildFly - Event Servlet + Booking Servlet
- **5432**: PostgreSQL

## Troubleshooting

### Tomcat nu pornește

- Verifică că portul 8080 nu este ocupat: `lsof -i :8080` (Linux/Mac) sau `netstat -ano | findstr :8080` (Windows)
- Verifică logurile: `$CATALINA_HOME/logs/catalina.out`

### Jetty nu pornește

- Verifică că portul 8081 nu este ocupat
- Verifică configurația `start.ini`
- Verifică logurile în consolă

### WildFly nu pornește

- Verifică că portul 8082 nu este ocupat
- Verifică configurația datasource
- Verifică logurile: `$JBOSS_HOME/standalone/log/server.log`

### WAR-urile nu se deploy

- Verifică că fișierele WAR există în directoarele corecte
- Verifică permisiunile de fișiere
- Verifică logurile serverului pentru erori

### Probleme de conectivitate la baza de date

- Verifică că PostgreSQL rulează: `psql -U eventuser -d eventticketing`
- Verifică variabilele de mediu (DATABASE_URL, DATABASE_USER, DATABASE_PASSWORD)
- Verifică că driver-ul PostgreSQL este disponibil în classpath

## Note Importante

1. **WildFly** poate rula ambele servleturi simultan pe același port (8082), dar cu context paths diferite:

   - `/event-servlet` pentru Event Servlet
   - `/booking-servlet` pentru Booking Servlet

2. **Comunicarea între servleturi**: Booking Servlet trebuie să știe unde să găsească Event Servlet. Dacă rulezi pe servere diferite, actualizează `EVENT_SERVICE_URL` în configurația Jetty.

3. **Hot Deploy**:

   - **Tomcat**: Copiază noul WAR și șterge directorul vechi din webapps
   - **Jetty**: Actualizează fișierele în directorul exploded WAR
   - **WildFly**: Copiază noul WAR și WildFly va detecta automat schimbarea

4. **Undeploy**:
   - **Tomcat**: Șterge WAR-ul sau directorul din webapps
   - **Jetty**: Șterge directorul din webapps
   - **WildFly**: Șterge WAR-urile sau adaugă fișier `.undeployed`
