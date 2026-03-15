---
description: "Docker Compose, Dockerfiles, Postgres, RabbitMQ, and container setup for MedClinic 2"
alwaysApply: false
globs: ["docker-compose*.yml", "**/Dockerfile*", "backend/**/application*.yml"]
---

## Docker & infrastructure rules

These rules describe how to run MedClinic 2 via Docker and how to extend the infrastructure.

### 1. Overall model

- The **root** `docker-compose.yml` is the **single source of truth** for:
  - Postgres instances
  - RabbitMQ
  - All Spring Boot microservices
  - Frontend (Vue + Nginx)
- The `infrastructure/docker-compose.yml` is a **dev‑only** variant for running just Postgres + RabbitMQ.

When adding new services, prefer to **extend the root compose** rather than introducing new top‑level compose files.

### 2. Backend services (Spring Boot)

- All backend services are built from the shared multi‑stage `backend/Dockerfile`:
  - Stage 1: `gradle:8.11-jdk17-alpine` builds the selected subproject (`SERVICE_NAME` arg) and produces a Boot jar.
  - Stage 2: `eclipse-temurin:17-jre-alpine` runs `java -jar app.jar`.
- To add a new Spring Boot service:
  1. Create a new subproject under `backend/` and wire it in `settings.gradle.kts` and `build.gradle.kts`.
  2. Add a service block in `docker-compose.yml`:
     - `build.context: ./backend`
     - `build.args.SERVICE_NAME: <new-service-name>`
     - `environment` driven by existing patterns (e.g. `SPRING_DATASOURCE_URL`, `EUREKA_CLIENT_SERVICEURL_DEFAULTZONE`).
  3. Prefer overriding hostnames via env vars instead of hardcoding them in `application.yml`.

### 3. Datastores & messaging

- Postgres:
  - Main DB: `postgres-main` on `5432`, mapped to `main_service_db` with `main_user/main_pass`.
  - Auth DB: `postgres-auth` on `5433`, mapped to `auth_service_db` with `auth_user/auth_pass`.
  - Each has its own volume (`postgres_main_data`, `postgres_auth_data`).
- RabbitMQ:
  - Service: `rabbitmq` on `5672` (AMQP) and `15672` (management UI).
  - Volume: `rabbitmq_data`.

When adding new services:

- Prefer using **existing databases** unless there is a clear need for a separate schema.
- Use Docker service names (`postgres-main`, `postgres-auth`, `rabbitmq`) inside containers, not `localhost`.

### 4. Frontend (Vue + Vite + Nginx)

- `frontend/Dockerfile` is also multi‑stage:
  - Stage 1: `node:22-alpine` runs `npm ci` and `npm run build-only`.
  - Stage 2: `nginx:alpine` serves the built `dist/` folder.
- Nginx config (`frontend/nginx.conf`):
  - Serves SPA with `try_files ... /index.html`.
  - Proxies `/api/` to the API gateway service (`api-gateway:8080`).

When modifying frontend assets:

- Keep the dev experience (`npm run dev`) working outside Docker.
- Keep the Docker image serving **static assets** only; no dev server in containers.

### 5. New services and health checks

- For new services, add **healthchecks** where reasonable:
  - Spring Boot: probe `/actuator/health`.
  - Datastores: use built‑in health commands (e.g. `pg_isready` for Postgres).
- Use `depends_on` with `condition: service_healthy` sparingly:
  - Use it when a service truly cannot start without a dependency (e.g. API gateway depends on Eureka).

