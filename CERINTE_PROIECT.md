# Analiză Cerințe Proiect - Baze de Date

## Cerințe Proiect

1. ✅/❌ **Instalarea/configurarea PostgreSQL, MicrosoftSQL, OracleDB**
2. ❌ **EJB, JPA, Servleturi, eventual JSP**
3. ❌ **Cel puțin un tabel în fiecare DB (PostgreSQL, MicrosoftSQL, OracleDB)**
4. ✅ **Tabele diferite cu legături între ele**
5. ⚠️ **CRUD complet (Create, Read, Update, Delete)**
6. ✅ **Documentație aplicație**
7. ✅ **Interfață grafică (User Interface)**

## Analiză Detaliată

### ✅ Ce este implementat:

1. **Servleturi** ✅

   - Event Servlet (Tomcat, port 8080)
   - Booking Servlet (Jetty, port 8081)
   - Comunicare între servleturi prin HTTP REST API

2. **Bază de date PostgreSQL** ✅

   - Configurată și funcțională
   - Tabele: users, events, bookings, booking_items, payments, ticket_types, seats
   - Legături între tabele (foreign keys)

3. **CRUD Parțial** ⚠️

   - ✅ **Create**: Events, Bookings, Users (register)
   - ✅ **Read**: Events, Bookings, Users
   - ⚠️ **Update**: Events (parțial), Bookings (payment status)
   - ⚠️ **Delete**: Events (există endpoint), dar nu este complet testat

4. **Interfață grafică** ✅

   - Next.js/React frontend
   - Pagini: Events, Event Details, Cart, Checkout, Login, Signup
   - Interacțiune completă cu utilizatorul

5. **Documentație** ✅
   - SETUP.md - instrucțiuni de instalare și configurare
   - SERVLET_COMMUNICATION.md - documentație comunicare servleturi

### ❌ Ce lipsește:

1. **MicrosoftSQL și OracleDB** ❌

   - Proiectul folosește DOAR PostgreSQL
   - Trebuie adăugate MicrosoftSQL și OracleDB
   - Trebuie tabele diferite în fiecare bază de date

2. **JPA (Java Persistence API)** ❌

   - Proiectul folosește JDBC direct (PreparedStatement, ResultSet)
   - Trebuie migrat la JPA cu EntityManager
   - Trebuie Entity-uri JPA (@Entity, @Table, etc.)

3. **EJB (Enterprise JavaBeans)** ❌

   - Nu sunt folosite EJB-uri (@Stateless, @Stateful, @Singleton)
   - Trebuie adăugate EJB-uri pentru logica de business

4. **JSP (JavaServer Pages)** ❌
   - Proiectul folosește Next.js/React pentru frontend
   - Trebuie adăugate pagini JSP sau justificare pentru Next.js

## Recomandări pentru Încadrare în Cerințe

### Opțiunea 1: Adăugare Tehnologii Cerute (Recomandat)

#### 1. Adăugare JPA

- Migrează de la JDBC la JPA
- Creează Entity-uri JPA pentru toate tabelele
- Folosește EntityManager pentru operații CRUD

#### 2. Adăugare EJB

- Creează EJB-uri pentru serviciile de business
- Folosește @Stateless pentru servicii stateless
- Injectează EntityManager în EJB-uri

#### 3. Adăugare MicrosoftSQL și OracleDB

- Configurează conexiuni separate pentru fiecare bază de date
- Distribuie tabele între baze de date:
  - **PostgreSQL**: users, events
  - **MicrosoftSQL**: bookings, booking_items
  - **OracleDB**: payments, ticket_types
- Folosește JPA cu multiple persistence units

#### 4. Adăugare JSP (Opțional)

- Creează pagini JSP pentru interfața de administrare
- Păstrează Next.js pentru interfața publică
- Sau: justifică folosirea Next.js ca tehnologie modernă

### Opțiunea 2: Adaptare Proiect Actual

Dacă nu vrei să schimbi complet arhitectura, poți:

1. **Justifică folosirea Next.js în loc de JSP**:

   - Next.js este o tehnologie modernă și mai puternică decât JSP
   - Oferă o experiență utilizator mai bună
   - Este mai ușor de menținut

2. **Adaugă JSP pentru administrare**:

   - Creează pagini JSP pentru panoul de administrare
   - Folosește Next.js pentru interfața publică

3. **Adaugă JPA și EJB**:
   - Migrează serviciile la JPA + EJB
   - Păstrează servleturile pentru endpoint-uri REST

## Plan de Acțiune Recomandat

### Pasul 1: Adăugare JPA

1. Adaugă dependențe JPA în pom.xml
2. Creează persistence.xml
3. Migrează Entity-urile la JPA (@Entity, @Table, etc.)
4. Înlocuiește JDBC cu EntityManager

### Pasul 2: Adăugare EJB

1. Creează EJB-uri pentru servicii (@Stateless)
2. Injectează EntityManager în EJB-uri
3. Folosește EJB-uri în servleturi

### Pasul 3: Adăugare MicrosoftSQL și OracleDB

1. Configurează conexiuni pentru MicrosoftSQL și OracleDB
2. Creează scheme separate pentru fiecare bază de date
3. Configurează multiple persistence units în persistence.xml
4. Distribuie tabele între baze de date

### Pasul 4: Adăugare JSP (Opțional)

1. Creează pagini JSP pentru administrare
2. Sau: documentează de ce folosești Next.js

### Pasul 5: Completare CRUD

1. Verifică că toate operațiile CRUD sunt implementate
2. Adaugă teste pentru fiecare operație
3. Documentează endpoint-urile

## Concluzie

Proiectul actual are o bază solidă dar **NU se încadrează complet în cerințe** pentru că:

- ❌ Folosește doar PostgreSQL (lipsește MicrosoftSQL și OracleDB)
- ❌ Folosește JDBC direct (lipsește JPA)
- ❌ Nu folosește EJB
- ❌ Nu folosește JSP (folosește Next.js)

**Recomandare**: Adaugă JPA, EJB și suport pentru multiple baze de date pentru a satisface toate cerințele.
