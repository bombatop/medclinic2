plugins {
    id("org.springframework.boot")
}

dependencies {
    implementation(project(":shared-lib"))
    implementation("org.springframework.boot:spring-boot-starter-web")
    implementation("org.springframework.cloud:spring-cloud-starter-netflix-eureka-client")
    implementation("org.springframework.cloud:spring-cloud-stream-binder-rabbit")
    implementation("org.springframework.boot:spring-boot-starter-actuator")
    implementation("io.micrometer:micrometer-tracing-bridge-brave")
    implementation("org.apache.poi:poi-ooxml:5.2.5")
    implementation("com.itextpdf:kernel:8.0.2")
    implementation("com.itextpdf:layout:8.0.2")
    runtimeOnly("org.springframework.boot:spring-boot-devtools")
    compileOnly("org.projectlombok:lombok")
    annotationProcessor("org.projectlombok:lombok")
}
