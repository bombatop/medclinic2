## MedClinic 2

Internal medical clinic management system built as a microservices-based portfolio project.

### System prerequisites

Only **Docker** is required. No Java, Gradle, or Node needed on the host.

```bash
docker --version
docker compose version
```

**Windows**: Install [Docker Desktop](https://www.docker.com/products/docker-desktop/) with WSL 2 backend.

### Project structure

```
medclinic2/
├── frontend/
│   ├── Dockerfile              # Multi-stage build (Node + Nginx)
│   ├── nginx.conf              # SPA routing + API proxy
│   └── src/                    # Vue 3 + PrimeVue
├── backend/
│   ├── Dockerfile              # Multi-stage build (Gradle + JRE)
│   ├── build.gradle.kts, settings.gradle.kts
│   ├── main-service/           # CRUD: patients, appointments, doctors
│   ├── auth-service/           # JWT authentication
│   ├── notification-service/   # Telegram via RabbitMQ (reminders + admin alerts)
│   ├── document-service/       # PDF/Excel reports
│   ├── eureka-server/          # Service discovery
│   ├── api-gateway/            # Routing + auth
│   └── shared-lib/             # Common DTOs, utils
├── infrastructure/
│   └── docker-compose.yml      # DB + RabbitMQ only (for local dev)
├── docker-compose.yml          # Full stack
└── README.md
```

### Getting started

Create a **`.env`** file in the project root (it is gitignored). Docker Compose reads it for **`JWT_SECRET`**, which must match between **api-gateway** and **auth-service**:

```bash
git clone https://github.com/bombatop/medclinic2.git && cd medclinic2
cp infrastructure/.env.example .env
# Edit .env: set JWT_SECRET to a long random string (never commit .env)
docker compose up -d --build
```

Open http://localhost:3000 in your browser.

### Services

| Service              | Port  | URL                          |
|----------------------|-------|------------------------------|
| Frontend             | 3000  | http://localhost:3000         |
| API Gateway          | 8080  | http://localhost:8080         |
| Eureka Dashboard     | 8761  | http://localhost:8761         |
| Main Service         | 8081  | http://localhost:8081         |
| Auth Service         | 8082  | http://localhost:8082         |
| Notification Service | 8083  | http://localhost:8083         |
| Document Service     | 8084  | http://localhost:8084         |
| PostgreSQL (main)    | 5432  | main_user/main_pass           |
| PostgreSQL (auth)    | 5433  | auth_user/auth_pass           |
| RabbitMQ             | 5672  | guest/guest                   |
| RabbitMQ UI          | 15672 | http://localhost:15672         |

### Common commands

```bash
# Start everything
docker compose up -d --build

# Stop everything
docker compose down

# View logs for a service
docker compose logs -f main-service

# Rebuild a single service after code changes
docker compose up -d --build main-service

# Start only infrastructure (databases + RabbitMQ)
docker compose -f infrastructure/docker-compose.yml up -d
```

### Telegram notifications (optional)

The **notification-service** consumes RabbitMQ events and calls the Telegram Bot API when configured.

1. Create a bot with [@BotFather](https://t.me/BotFather) and copy the **bot token**.
2. Choose where **admin/security** messages go: a private chat with the bot, a group, or a channel. Send a message there, then read the numeric **chat id** (e.g. forward a message to [@userinfobot](https://t.me/userinfobot) or use `getUpdates` on your bot).
3. Add to your root **`.env`** (used by Docker Compose):

   - `TELEGRAM_BOT_TOKEN` — bot token (never commit it).
   - `TELEGRAM_ADMIN_CHAT_ID` — chat id for RBAC alerts (new user, user roles changed, role permissions changed).

**Patient reminders (24h / 1h):** In the UI, edit a patient: enable **Telegram appointment reminders** and set **Telegram chat id** (the patient must start your bot first so Telegram assigns an id). **main-service** schedules reminders and publishes `appointment-reminders` events; times use the JVM default timezone (see `medclinic.reminders` in [main-service application.yml](backend/main-service/src/main/resources/application.yml)).

### Default credentials

For development, the auth-service seeds an admin user:

- **Username:** `admin`
- **Password:** `admin`

Change the password after first login.

### Frontend tech stack

| Layer            | Choice            |
|------------------|-------------------|
| Framework        | Vue 3             |
| Language         | TypeScript        |
| Build tool       | Vite              |
| Routing          | Vue Router        |
| State management | Pinia             |
| HTTP client      | Axios             |
| UI components    | PrimeVue (Aura)   |
