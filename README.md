## MedClinic 2

Internal medical clinic management system built as a microservices-based portfolio project.

### System prerequisites

Run these commands on a fresh Ubuntu system to verify tools:

```bash
java -version
javac -version

node --version
npm --version

psql --version

docker --version
docker-compose --version

mvn --version
```

If Java, Maven, Docker, or docker-compose are missing, install them with:

```bash
sudo apt update
sudo apt install -y openjdk-17-jdk-headless maven docker.io docker-compose
```

### Project structure

```
medclinic2/
├── backend/
│   ├── main-service/         # CRUD: patients, appointments, doctors
│   ├── auth-service/         # JWT authentication
│   ├── notification-service/ # Email/SMS via RabbitMQ
│   ├── document-service/     # PDF/Excel reports
│   ├── eureka-server/        # Service discovery
│   ├── api-gateway/          # Routing + auth
│   └── shared-lib/           # Common DTOs, utils
├── infrastructure/
│   └── docker-compose.yml    # PostgreSQL, RabbitMQ
└── README.md
```

### Getting started

1. **Clone**: `git clone https://github.com/bombatop/medclinic2.git && cd medclinic2`

2. **Start infrastructure**:
   ```bash
   cd infrastructure && docker compose up -d
   ```

3. **Build and run backend** (start Eureka first, then other services):
   ```bash
   cd backend && mvn clean install
   mvn -pl eureka-server spring-boot:run          # port 8761
   mvn -pl api-gateway spring-boot:run             # port 8080
   mvn -pl main-service spring-boot:run            # port 8081
   mvn -pl auth-service spring-boot:run            # port 8082
   mvn -pl notification-service spring-boot:run    # port 8083
   mvn -pl document-service spring-boot:run        # port 8084
   ```

### Service verification

After each service starts:

```bash
# 1. Verify build
cd backend && mvn clean install -DskipTests

# 2. Health check (run while service is up)
curl http://localhost:8761/health   # eureka-server
curl http://localhost:8080/health   # api-gateway
curl http://localhost:8081/health   # main-service
curl http://localhost:8082/health   # auth-service
curl http://localhost:8083/health   # notification-service
curl http://localhost:8084/health   # document-service

# 3. Logs (from each service directory)
tail -f logs/eureka-server.log
tail -f logs/api-gateway.log
# etc.

# 4. Eureka dashboard
# Open http://localhost:8761 in browser
```
