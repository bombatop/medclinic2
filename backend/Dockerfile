FROM gradle:8.11-jdk17-alpine AS build
ARG SERVICE_NAME

WORKDIR /app

COPY build.gradle.kts settings.gradle.kts gradle.properties ./
COPY shared-lib/build.gradle.kts shared-lib/
COPY eureka-server/build.gradle.kts eureka-server/
COPY api-gateway/build.gradle.kts api-gateway/
COPY main-service/build.gradle.kts main-service/
COPY auth-service/build.gradle.kts auth-service/
COPY notification-service/build.gradle.kts notification-service/
COPY document-service/build.gradle.kts document-service/

RUN gradle :${SERVICE_NAME}:dependencies --no-daemon 2>/dev/null || true

COPY shared-lib/src shared-lib/src/
COPY ${SERVICE_NAME}/src ${SERVICE_NAME}/src/

RUN gradle :${SERVICE_NAME}:bootJar --no-daemon -x test \
    && cp ${SERVICE_NAME}/build/libs/*.jar /app/app.jar

FROM eclipse-temurin:17-jre-alpine

WORKDIR /app
COPY --from=build /app/app.jar app.jar

ENTRYPOINT ["java", "-jar", "app.jar"]
