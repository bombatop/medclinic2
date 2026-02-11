#!/bin/bash
# Start all backend services (Eureka first, then others in background)
set -e
cd "$(dirname "$0")/../backend"

echo "Starting Eureka..."
mvn -pl eureka-server spring-boot:run &
EUREKA_PID=$!
sleep 25

echo "Starting API Gateway..."
mvn -pl api-gateway spring-boot:run &
sleep 10

echo "Starting main-service, auth-service, notification-service, document-service..."
mvn -pl main-service spring-boot:run &
mvn -pl auth-service spring-boot:run &
mvn -pl notification-service spring-boot:run &
mvn -pl document-service spring-boot:run &

echo "All services starting. Eureka PID: $EUREKA_PID"
echo "Health: curl http://localhost:8761/health"
echo "Stop: kill \$(jobs -p)"
