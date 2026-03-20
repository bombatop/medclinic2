---
description: "Docker Compose, Dockerfiles, Postgres, RabbitMQ, and container setup for MedClinic 2"
alwaysApply: false
globs: ["docker-compose*.yml", "**/Dockerfile*", "**/nginx.conf"]
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

- Shared multi-stage `backend/Dockerfile`: `SERVICE_NAME` arg, env vars for config.
- To add a new Spring Boot service:
  1. Create a new subproject under `backend/` and wire it in `settings.gradle.kts` and `build.gradle.kts`.
  2. Add a service block in `docker-compose.yml`:
     - `build.context: ./backend`
     - `build.args.SERVICE_NAME: <new-service-name>`
     - `environment` driven by existing patterns (e.g. `SPRING_DATASOURCE_URL`, `EUREKA_CLIENT_SERVICEURL_DEFAULTZONE`).
  3. Prefer overriding hostnames via env vars instead of hardcoding them in `application.yml`.

### 3. Datastores & messaging

- Postgres: `postgres-main` (main_service_db), `postgres-auth` (auth_service_db). RabbitMQ: `rabbitmq` on 5672/15672.
- Use Docker service names inside containers, not `localhost`.

### 4. Frontend (Vue + Vite + Nginx)

- Multi-stage: node build → nginx serves `dist/`. Nginx: SPA `try_files`, proxy `/api/` to gateway.
- Keep `npm run dev` working outside Docker; image serves static assets only.
- The **runtime** frontend container has no Node — for `type-check` / `lint` via Docker, use the Dockerfile `--target build` and `docker run … npm run …` (see [`frontend-rules.md`](frontend-rules.md) §1).

### 5. New services and health checks

- Add healthchecks (Spring Boot: `/actuator/health`; Postgres: `pg_isready`).
- `depends_on` with `condition: service_healthy` only when truly required.

After backend changes: `docker compose up -d --build <service-name>`. Frontend: see `frontend-rules.md`.

