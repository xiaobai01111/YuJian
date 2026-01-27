plugins {
    java
    id("org.springframework.boot") version "3.4.1"
    id("io.spring.dependency-management") version "1.1.7"
    jacoco
}

group = "com.campus"
version = "1.0.0-SNAPSHOT"

java {
    toolchain {
        languageVersion.set(JavaLanguageVersion.of(21))
    }
}

configurations {
    compileOnly {
        extendsFrom(configurations.annotationProcessor.get())
    }
}

repositories {
    maven {
        url = uri("https://maven.aliyun.com/repository/public")
    }
    maven {
        url = uri("https://maven.aliyun.com/repository/spring")
    }
    mavenCentral()
}

val saTokenVersion = "1.38.0"
val mybatisPlusVersion = "3.5.5"
val springdocVersion = "2.8.4"
val jqwikVersion = "1.8.4"
val hutoolVersion = "5.8.26"
val minioVersion = "8.5.7"

dependencies {
    // Spring Boot Starters
    implementation("org.springframework.boot:spring-boot-starter-web")
    implementation("org.springframework.boot:spring-boot-starter-validation")
    implementation("org.springframework.boot:spring-boot-starter-data-redis")
    implementation("org.springframework.boot:spring-boot-starter-websocket")
    implementation("org.springframework.boot:spring-boot-starter-aop")
    implementation("org.springframework.boot:spring-boot-starter-mail")
    
    // DevTools (热加载)
    developmentOnly("org.springframework.boot:spring-boot-devtools")

    // Database
    implementation("org.postgresql:postgresql")
    implementation("com.baomidou:mybatis-plus-spring-boot3-starter:$mybatisPlusVersion")

    // Flyway
    implementation("org.flywaydb:flyway-core")
    implementation("org.flywaydb:flyway-database-postgresql")

    // Sa-Token
    implementation("cn.dev33:sa-token-spring-boot3-starter:$saTokenVersion")
    implementation("cn.dev33:sa-token-redis-jackson:$saTokenVersion")

    // Redis connection pool
    implementation("org.apache.commons:commons-pool2")

    // API Documentation (SpringDoc + Knife4j UI)
    implementation("org.springdoc:springdoc-openapi-starter-webmvc-ui:$springdocVersion")
    implementation("com.github.ben-manes.caffeine:caffeine:3.1.8")

    // MinIO (S3 协议文件存储)
    implementation("io.minio:minio:$minioVersion")

    // Environment Variables
    implementation("me.paulschwarz:spring-dotenv:4.0.0")

    // Utilities
    compileOnly("org.projectlombok:lombok")
    annotationProcessor("org.projectlombok:lombok")
    implementation("cn.hutool:hutool-all:$hutoolVersion")
    implementation("org.jsoup:jsoup:1.18.1")
    
    // POI for Excel import/export
    implementation("org.apache.poi:poi-ooxml:5.2.5")

    // Testing
    testImplementation("org.springframework.boot:spring-boot-starter-test")
    testImplementation("org.springframework.boot:spring-boot-testcontainers")
    testImplementation("net.jqwik:jqwik:$jqwikVersion")
    testImplementation("org.testcontainers:junit-jupiter")
    testImplementation("org.testcontainers:postgresql")
    testRuntimeOnly("org.junit.platform:junit-platform-launcher")
    
}

tasks.withType<Test> {
    useJUnitPlatform()
    finalizedBy(tasks.jacocoTestReport)
    environment("DOCKER_API_VERSION", "1.44")
}

tasks.jacocoTestReport {
    dependsOn(tasks.test)
    reports {
        xml.required.set(true)
        html.required.set(true)
        csv.required.set(false)
    }
}

tasks.bootJar {
    archiveFileName.set("campus-wall.jar")
}

tasks.withType<JavaCompile> {
    options.compilerArgs.add("-Xlint:deprecation")
}
