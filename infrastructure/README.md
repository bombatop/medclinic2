# MedClinic Infrastructure

PostgreSQL and RabbitMQ for the backend services.

## Start

```bash
docker compose up -d
```

## Stop

```bash
docker compose down
```

## Services

| Service          | Port | Credentials   |
|------------------|------|---------------|
| PostgreSQL (main)| 5432 | main_user/main_pass, db: main_service_db |
| PostgreSQL (auth)| 5433 | auth_user/auth_pass, db: auth_service_db |
| RabbitMQ         | 5672 | guest/guest   |
| RabbitMQ UI     | 15672 | http://localhost:15672 |
