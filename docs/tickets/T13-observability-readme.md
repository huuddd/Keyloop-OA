# T13 — Observability Config + README + Dockerfile

**Type:** Chore | **Priority:** P2
**Depends on:** T12
**Blocks:** None

---

## Description

Finalize cross-cutting concerns: structured logging configuration, Dockerfile for containerized deployment, docker-compose with the app service, idempotency key cleanup job, and a comprehensive README.

## Acceptance Criteria

- [ ] `logback-spring.xml` — structured logging with `correlationId` in every log line
- [ ] `Dockerfile` — multi-stage build (Maven → JRE 21)
- [ ] `docker-compose.yml` — app service depending on postgres (healthcheck)
- [ ] `IdempotencyCleanupJob` — scheduled cleanup of expired keys (24h TTL)
- [ ] `ClockConfig` — injectable `Clock` for deterministic time in tests
- [ ] `DomainEventPublisher` — adapter implementing `EventPublisher` port via Spring `ApplicationEventPublisher`
- [ ] `README.md` — build/run/test instructions + architecture overview
- [ ] `docker compose up` → app + postgres running, `/actuator/health` returns UP

## Implementation Details

### `src/main/resources/logback-spring.xml`

```xml
<configuration>
    <appender name="CONSOLE" class="ch.qos.logback.core.ConsoleAppender">
        <encoder class="net.logstash.logback.encoder.LogstashEncoder">
            <includeMdcKeyName>correlationId</includeMdcKeyName>
        </encoder>
    </appender>

    <!-- Fallback: simple pattern when logstash encoder not available -->
    <appender name="CONSOLE_SIMPLE" class="ch.qos.logback.core.ConsoleAppender">
        <encoder>
            <pattern>%d{HH:mm:ss.SSS} [%thread] [%X{correlationId}] %-5level %logger{36} - %msg%n</pattern>
        </encoder>
    </appender>

    <root level="INFO">
        <appender-ref ref="CONSOLE_SIMPLE" />
    </root>

    <logger name="com.keyloop.scheduler" level="DEBUG"/>
    <logger name="org.hibernate.SQL" level="DEBUG"/>
</configuration>
```

### `infrastructure/config/ClockConfig.java`

```java
@Configuration
public class ClockConfig {
    @Bean
    public Clock clock() {
        return Clock.systemUTC();
    }
}
```

All domain methods that need the current time accept `Clock` as a parameter, making them deterministic in tests (pass `Clock.fixed(...)` to control time).

### `infrastructure/event/DomainEventPublisher.java`

```java
@Component
public class DomainEventPublisher implements EventPublisher {

    private final ApplicationEventPublisher springPublisher;

    public DomainEventPublisher(ApplicationEventPublisher springPublisher) {
        this.springPublisher = springPublisher;
    }

    @Override
    public void publishEventsFor(AggregateRoot<?> aggregate) {
        aggregate.getDomainEvents().forEach(springPublisher::publishEvent);
        aggregate.clearDomainEvents();
    }
}
```

### `infrastructure/job/IdempotencyCleanupJob.java`

```java
@Component
public class IdempotencyCleanupJob {

    private static final Logger log = LoggerFactory.getLogger(IdempotencyCleanupJob.class);
    private final SpringDataIdempotencyKeyRepository repository;

    public IdempotencyCleanupJob(SpringDataIdempotencyKeyRepository repository) {
        this.repository = repository;
    }

    @Scheduled(fixedRate = 3600000) // every hour
    @Transactional
    public void cleanupExpiredKeys() {
        var cutoff = LocalDateTime.now().minusHours(24);
        var deleted = repository.deleteByCreatedAtBefore(cutoff);
        if (deleted > 0) {
            log.info("Cleaned up {} expired idempotency keys (older than {})",
                deleted, cutoff);
        }
    }
}
```

### `Dockerfile`

```dockerfile
# Stage 1: Build
FROM eclipse-temurin:21-jdk-alpine AS build
WORKDIR /app
COPY pom.xml .
COPY src ./src
RUN apk add --no-cache maven \
    && mvn clean package -DskipTests

# Stage 2: Run
FROM eclipse-temurin:21-jre-alpine
WORKDIR /app
COPY --from=build /app/target/*.jar app.jar

EXPOSE 8080
ENTRYPOINT ["java", "-jar", "app.jar"]
```

### `docker-compose.yml`

```yaml
services:
  postgres:
    image: postgres:16-alpine
    container_name: scheduler-postgres
    environment:
      POSTGRES_DB: scheduler_db
      POSTGRES_USER: scheduler
      POSTGRES_PASSWORD: scheduler
    ports:
      - "5432:5432"
    volumes:
      - postgres_data:/var/lib/postgresql/data
    healthcheck:
      test: ["CMD-SHELL", "pg_isready -U scheduler -d scheduler_db"]
      interval: 5s
      timeout: 5s
      retries: 5

  app:
    build: .
    container_name: scheduler-app
    ports:
      - "8080:8080"
    environment:
      SPRING_DATASOURCE_URL: jdbc:postgresql://postgres:5432/scheduler_db
      SPRING_DATASOURCE_USERNAME: scheduler
      SPRING_DATASOURCE_PASSWORD: scheduler
    depends_on:
      postgres:
        condition: service_healthy

volumes:
  postgres_data:
```

### `README.md` structure

```markdown
# Unified Service Scheduler — Keyloop Technical Assessment

## Scenario
Scenario A: The Unified Service Scheduler (Domain: Ownership)

## Architecture
See `docs/02-design.md` for full architecture, sequence diagrams, and deep dives.

## Tech Stack
See `docs/03-tech-stack.md` for technology choices and justifications.

## Prerequisites
- Java 21
- Docker + Docker Compose
- Maven 3.9+

## Quick Start

### Option 1: Docker Compose (recommended)
docker compose up --build
# App: http://localhost:8080
# Swagger: http://localhost:8080/swagger-ui.html
# Health: http://localhost:8080/actuator/health

### Option 2: Local Development
docker compose up -d postgres   # Start DB only
mvn spring-boot:run             # Start app locally

## Running Tests
mvn test                        # Unit + integration tests (needs Docker for Testcontainers)

## API Endpoints
| Method | Endpoint | Description |
|--------|----------|-------------|
| POST   | /api/v1/appointments | Create appointment (requires Idempotency-Key header) |
| GET    | /api/v1/appointments/{id} | Get appointment by ID |
| DELETE | /api/v1/appointments/{id} | Cancel appointment |
| PATCH  | /api/v1/appointments/{id}/check-in | Check in |
| PATCH  | /api/v1/appointments/{id}/complete | Complete |
| PATCH  | /api/v1/appointments/{id}/no-show | Mark no-show |
| GET    | /api/v1/appointments | Search (paginated) |
| GET    | /api/v1/dealerships/{id}/availability | Available slots (paginated) |

## AI Collaboration Narrative
See `docs/04-genai-collaboration.md`
```

## Commit message

```
chore: add observability config, Dockerfile, and README
```
