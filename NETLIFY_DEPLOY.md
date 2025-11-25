# Deploy Frontend pe Netlify

Acest ghid explică cum să deploy-ezi frontend-ul Next.js pe Netlify.

## Prerechizite

1. Cont Netlify (gratuit la [netlify.com](https://netlify.com))
2. Git repository (GitHub, GitLab, sau Bitbucket)
3. Backend-ul deploy-at și accesibil public (nu localhost)

## Pasul 1: Pregătire Frontend

### 1.1 Actualizează next.config.js pentru producție

Fișierul `next.config.js` este deja configurat să folosească variabilele de mediu:

- `NEXT_PUBLIC_API_URL` - pentru Event Servlet
- `NEXT_PUBLIC_BOOKING_API_URL` - pentru Booking Servlet

### 1.2 Build local (opțional, pentru testare)

```bash
cd frontend
npm install
npm run build
```

## Pasul 2: Deploy pe Netlify

### Metoda 1: Deploy prin Netlify Dashboard (Recomandat)

1. **Conectează repository-ul Git:**

   - Mergi pe [app.netlify.com](https://app.netlify.com)
   - Click pe "Add new site" → "Import an existing project"
   - Selectează Git provider-ul (GitHub, GitLab, Bitbucket)
   - Autorizează Netlify să acceseze repository-ul
   - Selectează repository-ul tău

2. **Configurează build settings:**

   - **Base directory:** `frontend`
   - **Build command:** `npm run build`
   - **Publish directory:** `frontend/.next`

3. **Adaugă variabilele de mediu:**

   - În secțiunea "Environment variables", adaugă:
     ```
     NEXT_PUBLIC_API_URL = https://your-backend-domain.com:8080
     NEXT_PUBLIC_BOOKING_API_URL = https://your-booking-backend-domain.com:8081
     ```
   - **IMPORTANT:** Înlocuiește `your-backend-domain.com` cu URL-ul real al backend-ului tău

4. **Deploy:**
   - Click pe "Deploy site"
   - Netlify va construi și deploy-a aplicația automat

### Metoda 2: Deploy prin Netlify CLI

1. **Instalează Netlify CLI:**

   ```bash
   npm install -g netlify-cli
   ```

2. **Login în Netlify:**

   ```bash
   netlify login
   ```

3. **Deploy:**

   ```bash
   cd frontend
   netlify deploy --prod
   ```

4. **Setează variabilele de mediu:**
   ```bash
   netlify env:set NEXT_PUBLIC_API_URL "https://your-backend-domain.com:8080"
   netlify env:set NEXT_PUBLIC_BOOKING_API_URL "https://your-booking-backend-domain.com:8081"
   ```

## Pasul 3: Configurare Backend pentru Acces Public

### Opțiuni pentru Backend:

#### Opțiunea 1: Deploy Backend pe Cloud (Recomandat)

**Platforme recomandate:**

- **Heroku** - pentru Tomcat/Jetty/WildFly
- **Railway** - suportă Docker
- **Render** - suportă Docker și Java
- **AWS EC2** - pentru control complet
- **DigitalOcean** - pentru control complet

**Exemplu pentru Heroku:**

```bash
# Creează Procfile
echo "web: java -jar target/event-servlet.war" > Procfile

# Deploy
heroku create your-app-name
git push heroku main
```

#### Opțiunea 2: Tunneling (pentru development)

Folosește **ngrok** sau **Cloudflare Tunnel** pentru a expune localhost public:

```bash
# Instalează ngrok
brew install ngrok  # macOS
# sau descarcă de la https://ngrok.com

# Pornește tunnel pentru Tomcat
ngrok http 8080

# Pornește tunnel pentru Jetty
ngrok http 8081
```

Apoi folosește URL-urile ngrok în variabilele de mediu Netlify.

#### Opțiunea 3: VPS cu IP Public

Dacă ai un VPS cu IP public:

1. Configurează firewall-ul să permită porturile 8080, 8081, 8082
2. Folosește IP-ul public în variabilele de mediu

## Pasul 4: Configurare CORS

Asigură-te că backend-ul permite request-uri de la domeniul Netlify:

### Pentru Tomcat (Event Servlet):

Verifică că `CORSFilter` permite origin-ul Netlify:

```java
// În CORSFilter.java
response.setHeader("Access-Control-Allow-Origin", "*");
// Sau pentru producție:
response.setHeader("Access-Control-Allow-Origin", "https://your-netlify-app.netlify.app");
```

### Pentru Jetty (Booking Servlet):

Similar, asigură-te că CORS este configurat corect.

## Pasul 5: Verificare

După deploy:

1. **Verifică build-ul:**

   - Mergi în Netlify Dashboard → "Deploys"
   - Verifică că build-ul a reușit

2. **Testează aplicația:**

   - Deschide URL-ul Netlify (ex: `https://your-app.netlify.app`)
   - Verifică că paginile se încarcă
   - Testează funcționalitățile (login, events, booking)

3. **Verifică console-ul browser:**
   - Deschide Developer Tools (F12)
   - Verifică erorile în Console și Network tabs
   - Asigură-te că request-urile API merg către backend-ul corect

## Troubleshooting

### Build eșuează

- Verifică că toate dependențele sunt în `package.json`
- Verifică log-urile de build în Netlify Dashboard
- Rulează `npm run build` local pentru a identifica erorile

### API calls eșuează

- Verifică că variabilele de mediu sunt setate corect în Netlify
- Verifică că backend-ul este accesibil public
- Verifică CORS settings în backend
- Verifică că URL-urile din variabilele de mediu sunt corecte

### Pagini 404

- Verifică că `netlify.toml` este configurat corect
- Verifică că rewrites din `next.config.js` funcționează
- Verifică că rutele Next.js sunt corecte

## Note Importante

1. **Variabilele de mediu** trebuie să înceapă cu `NEXT_PUBLIC_` pentru a fi accesibile în browser
2. **HTTPS este obligatoriu** pentru Netlify - backend-ul trebuie să suporte HTTPS sau să folosești tunneling
3. **CORS** trebuie configurat corect pentru a permite request-uri de la domeniul Netlify
4. **Backend-ul trebuie să fie accesibil public** - localhost nu va funcționa

## Exemplu Configurare Completă

### Netlify Environment Variables:

```
NEXT_PUBLIC_API_URL = https://your-tomcat-server.com:8080
NEXT_PUBLIC_BOOKING_API_URL = https://your-jetty-server.com:8081
```

### Backend URLs (exemplu):

- Tomcat: `https://event-api.yourdomain.com`
- Jetty: `https://booking-api.yourdomain.com`

### Netlify Site URL:

- Frontend: `https://event-ticketing.netlify.app`

## Resurse

- [Netlify Next.js Documentation](https://docs.netlify.com/integrations/frameworks/nextjs/)
- [Netlify Environment Variables](https://docs.netlify.com/environment-variables/overview/)
- [Next.js Deployment](https://nextjs.org/docs/deployment)
