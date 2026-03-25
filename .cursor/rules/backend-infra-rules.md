---
description: "Backend microservices layout, Gradle, Spring Boot, config, error handling, eventing for MedClinic 2"
alwaysApply: false
globs: ["backend/**/*.{java,kts,yml,yaml}"]
---

## Backend & infrastructure rules (non‑domain)

These rules describe **how** the backend is structured and how services should interact, without encoding business/domain logic.

### 1. Service layout

- Backend root: `backend/`
  - Each microservice is a Gradle subproject:
    - `auth-service` – authentication, JWT, users & roles
    - `main-service` – core clinic entities (patients/clients, employees, appointments)
    - `notification-service` – consumes RabbitMQ events (e.g. Telegram for reminders and admin alerts)
    - `document-service` – placeholder for reports/exports
    - `eureka-server` – service discovery
    - `api-gateway` – edge gateway & routing
    - `shared-lib` – shared DTOs and events (e.g. `AppointmentReminderEvent`, `AdminSecurityEvent`)

### 2. Configuration patterns

- Java: **17** (see `backend/build.gradle.kts`).
- Spring Boot: **3.x**, Spring Cloud for Gateway & Eureka.
- Configuration conventions:
  - Service names via `spring.application.name`.
  - DB connection strings:
    - In `application.yml` for defaults.
    - Overridable via env vars (e.g. `SPRING_DATASOURCE_URL`) in Docker.
  - Eureka:
    - Clients point to `http://eureka-server:8761/eureka/` inside Docker.
    - Avoid hard‑coding `localhost` when running in containers; prefer env overrides.

When adding settings:

- Prefer adding options to `application.yml` with sensible defaults.
- Use env var overrides in Docker for hostnames/ports/secrets.

### 3. Error handling

- Each service has a `GlobalExceptionHandler` that produces a simple `ErrorResponse`:
  - Shape: `{ status, message, timestamp }`.
  - Common exceptions:
    - `ResourceNotFoundException` → 404
    - `ConflictException` → 409
    - `AccessDeniedException` → 403 (where applicable)
    - `IllegalArgumentException` → 400
- When adding new controller/service methods:
  - Reuse existing exception types where possible.
  - If you introduce a new exception type, wire it into the global handler.

### 4. Eventing

- Shared event models live in `shared-lib` (e.g. `AppointmentReminderEvent`, `AdminSecurityEvent`).
- Publishers (e.g. in `main-service`) send events via Spring Cloud Stream `StreamBridge`.
- Consumers (e.g. in `notification-service`) should:
  - Depend only on **shared-lib** for event types.
  - Avoid importing internal main‑service entities.

When adding new events:

- Define new records in `shared-lib`.
- Keep event payloads **backend‑oriented**, not coupling directly to frontend DTOs.

### 5. After backend changes

- Rebuild: `docker compose up -d --build <service-name>`. Service mapping: `main-service`, `auth-service`, `api-gateway`.

