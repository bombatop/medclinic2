rootProject.name = "medclinic-backend"

include(
    "shared-lib",
    "eureka-server",
    "api-gateway",
    "main-service",
    "auth-service",
    "notification-service",
    "document-service"
)
