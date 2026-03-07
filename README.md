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
docker compose version

./backend/gradlew --version
```

If Java or Docker are missing, install them:

```bash
sudo apt update
sudo apt install -y openjdk-17-jdk-headless docker.io docker-compose-v2
```

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
│   ├── gradle/               # Gradle wrapper
│   ├── gradlew, gradle.properties
│   ├── build.gradle.kts, settings.gradle.kts
│   ├── main-service/        # CRUD: patients, appointments, doctors
│   ├── auth-service/        # JWT authentication
│   ├── notification-service/# Email/SMS via RabbitMQ
│   ├── document-service/    # PDF/Excel reports
│   ├── eureka-server/       # Service discovery
│   ├── api-gateway/         # Routing + auth
│   └── shared-lib/          # Common DTOs, utils
├── frontend/
│   ├── src/
│   │   ├── api/             # Axios HTTP client
│   │   ├── assets/          # CSS
│   │   ├── components/      # Reusable Vue components
│   │   ├── layouts/         # Page layouts (AppLayout)
│   │   ├── router/          # Vue Router config
│   │   ├── stores/          # Pinia state management
│   │   ├── views/           # Page-level components
│   │   ├── App.vue          # Root component
│   │   └── main.ts          # App entry point
│   ├── .env                 # API base URL config
│   ├── package.json
│   └── vite.config.ts
├── infrastructure/
│   └── docker-compose.yml
├── scripts/
│   ├── start-infrastructure.sh
│   └── start-backend.sh
└── README.md
```

### Getting started

1. **Clone**: `git clone https://github.com/bombatop/medclinic2.git && cd medclinic2`

2. **Start infrastructure**:
   ```bash
   ./scripts/start-infrastructure.sh
   ```

3. **Build and run backend** (Eureka first, then others in separate terminals):
   ```bash
   cd backend && ./gradlew build -x test
   ./scripts/start-backend.sh    # all in background
   ```
   Or manually (from `backend/`), one per terminal:
   ```bash
   cd backend
   ./gradlew :eureka-server:bootRun          # 8761
   ./gradlew :api-gateway:bootRun            # 8080
   ./gradlew :main-service:bootRun           # 8081
   ./gradlew :auth-service:bootRun           # 8082
   ./gradlew :notification-service:bootRun   # 8083
   ./gradlew :document-service:bootRun       # 8084
   ```

4. **Start frontend**:
   ```bash
   cd frontend
   npm install
   npm run dev                               # 5173
   ```

### Service verification

After each service starts:

```bash
# 1. Verify backend build
cd backend && ./gradlew build -x test

# 2. Verify frontend build
cd frontend && npm run build

# 3. Health check (run while service is up)
curl http://localhost:8761/health   # eureka-server
curl http://localhost:8080/health   # api-gateway
curl http://localhost:8081/health   # main-service
curl http://localhost:8082/health   # auth-service
curl http://localhost:8083/health   # notification-service
curl http://localhost:8084/health   # document-service

# 4. Logs (from each service directory)
tail -f logs/eureka-server.log
tail -f logs/api-gateway.log
# etc.

# 5. Eureka dashboard
# Open http://localhost:8761 in browser

# 6. Frontend
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
