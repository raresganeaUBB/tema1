# Deploy Backend Standalone (fără terminal)

Acest ghid explică cum să configurezi backend-ul să ruleze standalone, fără să fie nevoie să rulezi comenzi în terminal.

## Opțiuni pentru Backend Standalone

### Opțiunea 1: Systemd Services (Linux/macOS)

Creează servicii systemd care pornesc automat serverele la boot.

#### Pentru Tomcat:

Creează `/etc/systemd/system/tomcat-event.service`:

```ini
[Unit]
Description=Apache Tomcat Event Servlet
After=network.target postgresql.service

[Service]
Type=forking
User=your-user
Group=your-group
Environment="CATALINA_HOME=/Users/raresganea/Documents/Master SDI/apache-tomcat-11.0.13"
Environment="CATALINA_BASE=/Users/raresganea/Documents/Master SDI/apache-tomcat-11.0.13"
Environment="DATABASE_URL=jdbc:postgresql://localhost:5432/eventticketing"
Environment="DATABASE_USER=eventuser"
Environment="DATABASE_PASSWORD=eventpass"
ExecStart=/Users/raresganea/Documents/Master\ SDI/apache-tomcat-11.0.13/bin/startup.sh
ExecStop=/Users/raresganea/Documents/Master\ SDI/apache-tomcat-11.0.13/bin/shutdown.sh
Restart=on-failure

[Install]
WantedBy=multi-user.target
```

Activează serviciul:

```bash
sudo systemctl daemon-reload
sudo systemctl enable tomcat-event
sudo systemctl start tomcat-event
```

#### Pentru Jetty:

Creează `/etc/systemd/system/jetty-booking.service`:

```ini
[Unit]
Description=Jetty Booking Servlet
After=network.target postgresql.service tomcat-event.service

[Service]
Type=simple
User=your-user
Group=your-group
WorkingDirectory=/Users/raresganea/Documents/Master SDI/jetty-home-12.1.3
Environment="JETTY_HOME=/Users/raresganea/Documents/Master SDI/jetty-home-12.1.3"
Environment="DATABASE_URL=jdbc:postgresql://localhost:5432/eventticketing"
Environment="DATABASE_USER=eventuser"
Environment="DATABASE_PASSWORD=eventpass"
Environment="SERVLET_PORT=8081"
Environment="EVENT_SERVICE_URL=http://localhost:8080/event-servlet/api/events"
ExecStart=/usr/bin/java -jar /Users/raresganea/Documents/Master\ SDI/jetty-home-12.1.3/start.jar jetty.http.port=8081
Restart=on-failure

[Install]
WantedBy=multi-user.target
```

#### Pentru WildFly:

Creează `/etc/systemd/system/wildfly.service`:

```ini
[Unit]
Description=WildFly Application Server
After=network.target postgresql.service

[Service]
Type=simple
User=your-user
Group=your-group
WorkingDirectory=/Users/raresganea/Documents/Master SDI/wildfly-38.0.0.Final/bin
Environment="JBOSS_HOME=/Users/raresganea/Documents/Master SDI/wildfly-38.0.0.Final"
Environment="DATABASE_URL=jdbc:postgresql://localhost:5432/eventticketing"
Environment="DATABASE_USER=eventuser"
Environment="DATABASE_PASSWORD=eventpass"
ExecStart=/Users/raresganea/Documents/Master\ SDI/wildfly-38.0.0.Final/bin/standalone.sh
Restart=on-failure

[Install]
WantedBy=multi-user.target
```

### Opțiunea 2: Launch Agents (macOS)

Creează Launch Agents pentru macOS care pornesc serverele automat.

#### Pentru Tomcat:

Creează `~/Library/LaunchAgents/com.eventticketing.tomcat.plist`:

```xml
<?xml version="1.0" encoding="UTF-8"?>
<!DOCTYPE plist PUBLIC "-//Apple//DTD PLIST 1.0//EN" "http://www.apple.com/DTDs/PropertyList-1.0.dtd">
<plist version="1.0">
<dict>
    <key>Label</key>
    <string>com.eventticketing.tomcat</string>
    <key>ProgramArguments</key>
    <array>
        <string>/Users/raresganea/Documents/Master SDI/apache-tomcat-11.0.13/bin/startup.sh</string>
    </array>
    <key>RunAtLoad</key>
    <true/>
    <key>KeepAlive</key>
    <true/>
    <key>EnvironmentVariables</key>
    <dict>
        <key>CATALINA_HOME</key>
        <string>/Users/raresganea/Documents/Master SDI/apache-tomcat-11.0.13</string>
        <key>DATABASE_URL</key>
        <string>jdbc:postgresql://localhost:5432/eventticketing</string>
        <key>DATABASE_USER</key>
        <string>eventuser</string>
        <key>DATABASE_PASSWORD</key>
        <string>eventpass</string>
    </dict>
</dict>
</plist>
```

Încarcă agentul:

```bash
launchctl load ~/Library/LaunchAgents/com.eventticketing.tomcat.plist
```

### Opțiunea 3: Docker Compose (Recomandat)

Folosește Docker Compose pentru a rula toate serviciile standalone.

```bash
# Pornește toate serviciile
docker-compose up -d

# Verifică status
docker-compose ps

# Oprește toate serviciile
docker-compose down
```

### Opțiunea 4: Script de Pornire Automată

Creează un script care pornește serverele la login.

#### Pentru macOS:

Creează `~/.zshrc` sau `~/.bash_profile`:

```bash
# Pornește serverele la login (doar dacă nu rulează deja)
if ! lsof -i :8080 >/dev/null 2>&1; then
    "$CATALINA_HOME/bin/startup.sh" >/dev/null 2>&1 &
fi

if ! lsof -i :8081 >/dev/null 2>&1; then
    cd "$JETTY_HOME" && nohup java -jar start.jar jetty.http.port=8081 > logs/jetty.log 2>&1 &
fi

if ! lsof -i :8082 >/dev/null 2>&1; then
    cd "$JBOSS_HOME/bin" && nohup ./standalone.sh > ../standalone/log/server.log 2>&1 &
fi
```

## Verificare

După configurare, verifică că serverele rulează:

```bash
# Verifică porturile
lsof -i :8080 -i :8081 -i :8082

# Testează endpoint-urile
curl http://localhost:8080/event-servlet/health
curl http://localhost:8081/booking-servlet/health
curl http://localhost:8082/event-servlet/health
```

## Oprire Servere

### Systemd:

```bash
sudo systemctl stop tomcat-event jetty-booking wildfly
```

### Launch Agents (macOS):

```bash
launchctl unload ~/Library/LaunchAgents/com.eventticketing.tomcat.plist
```

### Docker Compose:

```bash
docker-compose down
```
