# T02 — Docker Compose + Dockerfile

**Type:** Setup | **Priority:** P0
**Depends on:** T01
**Blocks:** T03

---

## Description

Create `docker-compose.yml` with PostgreSQL 16 and the application service, plus a multi-stage `Dockerfile` for building and running the app.

## Acceptance Criteria

- [x] `docker-compose.yml` — PostgreSQL 16 with healthcheck + app service
- [x] `Dockerfile` — multi-stage build (JDK 21 build → JRE 21 runtime)
- [ ] `docker compose up -d postgres` starts a healthy database
- [ ] `docker compose up --build` starts both app + database
- [ ] App connects to `scheduler_db` on startup

## Implementation Details

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

**Design notes:**
- `postgres:16-alpine` — lightweight image, matches production version
- Healthcheck ensures the app waits for PostgreSQL to be ready before starting
- `depends_on.condition: service_healthy` prevents startup race conditions
- Environment variables override `application.yml` defaults for the container network (`postgres:5432` instead of `localhost:5432`)
- Named volume `postgres_data` persists data across container restarts

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

**Design notes:**
- Multi-stage build — build stage uses JDK, runtime stage uses JRE only (smaller image)
- `eclipse-temurin:21` — production-grade OpenJDK distribution
- `-DskipTests` in Docker build — tests run separately in CI, not during image construction
- Alpine-based images for minimal footprint

### Usage

```bash
# Start database only (local development)
docker compose up -d postgres

# Start everything (full stack)
docker compose up --build

# Verify
curl http://localhost:8080/actuator/health
# → {"status":"UP"}
```

## Commit message

```
chore: add Docker Compose and multi-stage Dockerfile
```
