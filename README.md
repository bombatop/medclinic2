## MedClinic 2

Internal medical clinic management system built as a microservices-based portfolio project.

### System prerequisites

Only **Docker** is required. No Java, Gradle, or Node needed on the host.

```bash
docker --version
docker compose version
```

**Windows**: Install [Docker Desktop](https://www.docker.com/products/docker-desktop/) with WSL 2 backend.

Node.js 20+ is required for the frontend. Install via nvm:

```bash
curl -o- https://raw.githubusercontent.com/nvm-sh/nvm/v0.40.1/install.sh | bash
source ~/.bashrc
nvm install 20
```

### Project structure

```
medclinic2/
├── backend/
│   ├── Dockerfile              # Multi-stage build (Gradle + JRE)
│   ├── build.gradle.kts, settings.gradle.kts
│   ├── main-service/           # CRUD: patients, appointments, doctors
│   ├── auth-service/           # JWT authentication
│   ├── notification-service/   # Email/SMS via RabbitMQ
│   ├── document-service/       # PDF/Excel reports
│   ├── eureka-server/          # Service discovery
│   ├── api-gateway/            # Routing + auth
│   └── shared-lib/             # Common DTOs, utils
├── frontend/
│   ├── src/
│   │   ├── api/                # Axios HTTP client
│   │   ├── assets/             # CSS
│   │   ├── components/         # Reusable Vue components
│   │   ├── layouts/            # Page layouts (AppLayout)
│   │   ├── router/             # Vue Router config
│   │   ├── stores/             # Pinia state management
│   │   ├── views/              # Page-level components
│   │   ├── App.vue             # Root component
│   │   └── main.ts             # App entry point
│   ├── .env                    # API base URL config
│   ├── package.json
│   └── vite.config.ts
├── infrastructure/
│   └── docker-compose.yml      # DB + RabbitMQ only (for local dev)
├── docker-compose.yml          # Full stack (infrastructure + all services)
└── README.md
```

### Getting started

```bash
git clone https://github.com/bombatop/medclinic2.git && cd medclinic2
docker compose up -d --build
```

4. **Start frontend**:
   ```bash
   cd frontend
   npm install
   npm run dev                               # 5173
   ```

### Services

| Service              | Port  | URL                          |
|----------------------|-------|------------------------------|
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

# Frontend
# Open http://localhost:5173 in browser
```

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
