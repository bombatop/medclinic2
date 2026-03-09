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

That's it. Docker builds the JDK, compiles the code, and runs everything.

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
```
