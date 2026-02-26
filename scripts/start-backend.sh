#!/bin/bash
# Start all backend services (Eureka first, then others in background)
set -e
cd "$(dirname "$0")/../backend"

echo "Starting Eureka..."
./gradlew :eureka-server:bootRun &
EUREKA_PID=$!
sleep 25

echo "Starting API Gateway..."
./gradlew :api-gateway:bootRun &
sleep 10

echo "Starting main-service, auth-service, notification-service, document-service..."
./gradlew :main-service:bootRun &
./gradlew :auth-service:bootRun &
./gradlew :notification-service:bootRun &
./gradlew :document-service:bootRun &

echo "All services starting. Eureka PID: $EUREKA_PID"
echo "Health: curl http://localhost:8761/health"
echo "Stop: kill \$(jobs -p)"
