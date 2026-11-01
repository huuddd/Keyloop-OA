# T01 — Project Scaffold: Spring Boot + PostgreSQL + Maven

**Type:** Setup | **Priority:** P0
**Depends on:** None
**Blocks:** T02, T03

---

## Description

Initialize a Spring Boot 3.3.5 project with Java 21, PostgreSQL driver, Flyway, JPA, Validation, Actuator, OpenAPI (Swagger UI), Testcontainers, ArchUnit, and Gatling. Configure `application.yml` with virtual threads, ProblemDetail support, and Hibernate validation mode. Create the main application class with `@EnableScheduling`.

## Acceptance Criteria

- [ ] `pom.xml` — all dependencies present with correct versions
- [ ] `SchedulerApplication.java` — `@SpringBootApplication` + `@EnableScheduling`
- [ ] `application.yml` — PostgreSQL datasource, Flyway, virtual threads, ProblemDetail, Actuator
- [ ] `mvn compile` succeeds (no DB needed at this stage)

## Implementation Details

### `pom.xml`

```xml
<?xml version="1.0" encoding="UTF-8"?>
<project xmlns="http://maven.apache.org/POM/4.0.0"
         xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
         xsi:schemaLocation="http://maven.apache.org/POM/4.0.0 https://maven.apache.org/xsd/maven-4.0.0.xsd">
    <modelVersion>4.0.0</modelVersion>

    <parent>
        <groupId>org.springframework.boot</groupId>
        <artifactId>spring-boot-starter-parent</artifactId>
        <version>3.3.5</version>
        <relativePath/>
    </parent>

    <groupId>com.keyloop</groupId>
    <artifactId>unified-scheduler</artifactId>
    <version>0.0.1-SNAPSHOT</version>
    <name>Unified Service Scheduler</name>
    <description>Keyloop Technical Assessment — Scenario A: Appointment Scheduler</description>

    <properties>
        <java.version>21</java.version>
        <springdoc.version>2.6.0</springdoc.version>
        <testcontainers.version>1.20.4</testcontainers.version>
        <gatling.version>3.10.5</gatling.version>
        <gatling-maven-plugin.version>4.8.2</gatling-maven-plugin.version>
    </properties>

    <dependencyManagement>
        <dependencies>
            <dependency>
                <groupId>org.testcontainers</groupId>
                <artifactId>testcontainers-bom</artifactId>
                <version>${testcontainers.version}</version>
                <type>pom</type>
                <scope>import</scope>
            </dependency>
        </dependencies>
    </dependencyManagement>

    <dependencies>
        <!-- Web -->
        <dependency>
            <groupId>org.springframework.boot</groupId>
            <artifactId>spring-boot-starter-web</artifactId>
        </dependency>

        <!-- JPA + PostgreSQL -->
        <dependency>
            <groupId>org.springframework.boot</groupId>
            <artifactId>spring-boot-starter-data-jpa</artifactId>
        </dependency>
        <dependency>
            <groupId>org.postgresql</groupId>
            <artifactId>postgresql</artifactId>
            <scope>runtime</scope>
        </dependency>

        <!-- Flyway -->
        <dependency>
            <groupId>org.flywaydb</groupId>
            <artifactId>flyway-core</artifactId>
        </dependency>
        <dependency>
            <groupId>org.flywaydb</groupId>
            <artifactId>flyway-database-postgresql</artifactId>
        </dependency>

        <!-- Validation -->
        <dependency>
            <groupId>org.springframework.boot</groupId>
            <artifactId>spring-boot-starter-validation</artifactId>
        </dependency>

        <!-- Actuator (observability) -->
        <dependency>
            <groupId>org.springframework.boot</groupId>
            <artifactId>spring-boot-starter-actuator</artifactId>
        </dependency>

        <!-- OpenAPI / Swagger UI -->
        <dependency>
            <groupId>org.springdoc</groupId>
            <artifactId>springdoc-openapi-starter-webmvc-ui</artifactId>
            <version>${springdoc.version}</version>
        </dependency>

        <!-- Test -->
        <dependency>
            <groupId>org.springframework.boot</groupId>
            <artifactId>spring-boot-starter-test</artifactId>
            <scope>test</scope>
        </dependency>
        <dependency>
            <groupId>org.testcontainers</groupId>
            <artifactId>postgresql</artifactId>
            <scope>test</scope>
        </dependency>
        <dependency>
            <groupId>org.testcontainers</groupId>
            <artifactId>junit-jupiter</artifactId>
            <scope>test</scope>
        </dependency>

        <!-- ArchUnit — architecture tests -->
        <dependency>
            <groupId>com.tngtech.archunit</groupId>
            <artifactId>archunit-junit5</artifactId>
            <version>1.3.0</version>
            <scope>test</scope>
        </dependency>

        <!-- Gatling — load testing -->
        <dependency>
            <groupId>io.gatling.highcharts</groupId>
            <artifactId>gatling-charts-highcharts</artifactId>
            <version>${gatling.version}</version>
            <scope>test</scope>
        </dependency>
    </dependencies>

    <build>
        <plugins>
            <plugin>
                <groupId>org.springframework.boot</groupId>
                <artifactId>spring-boot-maven-plugin</artifactId>
            </plugin>
            <plugin>
                <groupId>io.gatling</groupId>
                <artifactId>gatling-maven-plugin</artifactId>
                <version>${gatling-maven-plugin.version}</version>
                <configuration>
                    <simulationClass>com.keyloop.scheduler.loadtest.AppointmentLoadSimulation</simulationClass>
                </configuration>
            </plugin>
        </plugins>
    </build>
</project>
```

### `src/main/java/com/keyloop/scheduler/SchedulerApplication.java`

```java
package com.keyloop.scheduler;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling
public class SchedulerApplication {

    public static void main(String[] args) {
        SpringApplication.run(SchedulerApplication.class, args);
    }
}
```

> `@EnableScheduling` is required for the `IdempotencyCleanupJob` scheduled task (see T13).

### `src/main/resources/application.yml`

```yaml
spring:
  application:
    name: unified-scheduler

  mvc:
    problemdetails:
      enabled: true

  datasource:
    url: jdbc:postgresql://localhost:5432/scheduler_db
    username: scheduler
    password: scheduler

  jpa:
    hibernate:
      ddl-auto: validate
    show-sql: false
    properties:
      hibernate:
        format_sql: true

  flyway:
    enabled: true
    locations: classpath:db/migration

  threads:
    virtual:
      enabled: true

server:
  port: 8080

management:
  endpoints:
    web:
      exposure:
        include: health,info,metrics
```

**Key configuration choices:**
- `spring.mvc.problemdetails.enabled: true` — enables RFC 7807 ProblemDetail responses for Spring validation errors
- `spring.jpa.hibernate.ddl-auto: validate` — Hibernate validates the schema against entities but never modifies it (Flyway owns all DDL)
- `spring.threads.virtual.enabled: true` — Java 21 Virtual Threads (Project Loom) for all request handling
- `management.endpoints.web.exposure.include` — exposes health, info, and metrics actuator endpoints

## Commit message

```
feat: scaffold Spring Boot 3.3.5 project with all dependencies
```
