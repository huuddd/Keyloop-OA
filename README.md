# Unified Service Scheduler

> **Keyloop Technical Assessment -- Scenario A (Domain: Ownership)**

A scheduling system for automotive dealerships that manages service appointments, technician assignments, service bay allocation, and real-time availability queries. The system prevents double-booking through pessimistic locking and enforces appointment lifecycle state transitions.

## Architecture

Clean Architecture with 4 layers, enforced by **9 ArchUnit rules** at build time:

```mermaid
graph LR
    API["api/<br/>Controllers<br/>Exception Handler<br/>Request DTOs"]
    APP["application/<br/>CQRS Handlers<br/>Validation Pipeline<br/>DTOs"]
    DOM["domain/<br/>Aggregates<br/>Value Objects<br/>Repository Interfaces<br/><i>Zero framework deps</i>"]
    INF["infrastructure/<br/>JPA Adapters<br/>Filters<br/>Event Publishing"]

    API --> APP
    APP --> DOM
    INF --> DOM
    API --> INF

    style DOM fill:#2d6a4f,stroke:#1b4332,color:#fff
    style APP fill:#264653,stroke:#1d3557,color:#fff
    style API fill:#457b9d,stroke:#1d3557,color:#fff
    style INF fill:#6c757d,stroke:#495057,color:#fff
```

**Dependency rule**: `domain` has zero framework dependencies -- `jakarta.persistence` and `org.springframework` are forbidden, enforced by ArchUnit at build time. `application` depends only on `domain`. `infrastructure` implements domain ports (repository interfaces). `api` orchestrates everything.

### Appointment Lifecycle (State Machine)

```mermaid
stateDiagram-v2
    [*] --> CONFIRMED : create()
    CONFIRMED --> IN_PROGRESS : checkIn()
    CONFIRMED --> CANCELLED : cancel()
    CONFIRMED --> NO_SHOW : markNoShow()
    IN_PROGRESS --> COMPLETED : complete()
    IN_PROGRESS --> CANCELLED : cancel()
    COMPLETED --> [*]
    CANCELLED --> [*]
    NO_SHOW --> [*]
```

### Request Flow (Create Appointment)

```mermaid
sequenceDiagram
    participant C as Client
    participant F as IdempotencyFilter
    participant Ctrl as AppointmentController
    participant H as CreateAppointmentCommandHandler
    participant V as CommandValidator
    participant D as Domain (Appointment)
    participant R as Repository (JPA)
    participant DB as PostgreSQL

    C->>F: POST /api/v1/appointments<br/>+ Idempotency-Key
    F->>F: Check idempotency store
    F->>Ctrl: Forward request
    Ctrl->>H: CreateAppointmentCommand
    H->>V: Validate command
    V-->>H: OK
    H->>R: findActiveByDealershipIdWithLock()
    R->>DB: SELECT ... FOR UPDATE<br/>(lock service bay rows)
    DB-->>R: Locked bays
    H->>H: Find available bay + technician
    H->>D: Appointment.create(...)
    D-->>H: Result.Success(appointment)
    H->>R: save(appointment)
    R->>DB: INSERT appointment
    H-->>Ctrl: AppointmentDto
    Ctrl-->>C: 201 Created
```

### Double-Booking Prevention (Concurrency Deep Dive)

```mermaid
sequenceDiagram
    participant T1 as Thread 1
    participant T2 as Thread 2
    participant DB as PostgreSQL

    T1->>DB: BEGIN
    T2->>DB: BEGIN
    T1->>DB: SELECT bays FOR UPDATE<br/>(locks bay rows)
    T2->>DB: SELECT bays FOR UPDATE<br/>(BLOCKED -- waits for T1)
    T1->>DB: Find bay available, INSERT appointment
    T1->>DB: COMMIT (releases lock)
    T2->>DB: Lock acquired, reads updated state
    T2->>DB: Bay now occupied -> 409 Conflict
    T2->>DB: ROLLBACK
```

The lock targets **service bay rows** (not appointment rows) because bay rows always exist. Locking appointment rows would fail on empty slots (nothing to lock = phantom read vulnerability).

See [`docs/02-design.md`](./docs/02-design.md) for the full system design document with additional diagrams, concurrency analysis, and the availability algorithm.

## Tech Stack

| Category | Technology | Justification |
|----------|-----------|---------------|
| Language | Java 21 (Virtual Threads) | Modern LTS, structured concurrency, pattern matching |
| Framework | Spring Boot 3.3.5 | Mature ecosystem, Actuator, auto-configuration |
| Database | PostgreSQL 16 | ACID guarantees, `FOR UPDATE` locking, JSONB support |
| Migrations | Flyway (V1-V6) | Version-controlled schema, repeatable migrations |
| Build | Maven | Standard Java build, dependency management |
| Unit Tests | JUnit 5 + AssertJ + Mockito | Industry standard, fluent assertions |
| Integration Tests | Testcontainers | Real PostgreSQL in tests, no mocking the database |
| Architecture Tests | ArchUnit | Compile-time layer dependency enforcement |
| API Docs | SpringDoc OpenAPI / Swagger UI | Auto-generated from annotations, interactive testing |
| Metrics | Micrometer + Prometheus | Dimensional metrics, Prometheus scraping |
| Dashboard | Grafana (auto-provisioned) | Pre-configured dashboard with 15 panels |
| Logging | SLF4J/Logback + CorrelationId MDC | Structured logging with request tracing |

See [`docs/03-tech-stack.md`](./docs/03-tech-stack.md) for detailed technology choices and justifications.

## Prerequisites

- Java 21 (Eclipse Temurin recommended)
- Docker + Docker Compose
- Maven 3.9+

## Quick Start

### Full Stack with Monitoring

```bash
docker compose up -d postgres           # Start PostgreSQL
mvn spring-boot:run                     # Start the app (port 8080)
docker compose up -d prometheus grafana # Start monitoring stack
```

| Service | URL | Credentials |
|---------|-----|-------------|
| Application | http://localhost:8080 | -- |
| Swagger UI | http://localhost:8080/swagger-ui.html | -- |
| Health Check | http://localhost:8080/actuator/health | -- |
| Prometheus | http://localhost:9090 | -- |
| Grafana Dashboard | http://localhost:3000/d/unified-scheduler | admin / admin |

### App Only (without monitoring)

```bash
docker compose up -d postgres   # Start PostgreSQL only
mvn spring-boot:run             # Start the app
```

Seed data (6 sample appointments across all statuses) is loaded automatically via Flyway V6.

## API Endpoints

| Method | Endpoint | Description | Success | Error Codes |
|--------|----------|-------------|---------|-------------|
| `POST` | `/api/v1/appointments` | Create appointment | 201 | 400, 409, 422 |
| `GET` | `/api/v1/appointments/{id}` | Get by ID | 200 | 404 |
| `DELETE` | `/api/v1/appointments/{id}` | Cancel | 204 | 404, 422 |
| `PATCH` | `/api/v1/appointments/{id}/check-in` | Check in customer | 204 | 404, 422 |
| `PATCH` | `/api/v1/appointments/{id}/complete` | Complete service | 204 | 404, 422 |
| `PATCH` | `/api/v1/appointments/{id}/no-show` | Mark no-show | 204 | 404, 422 |
| `GET` | `/api/v1/appointments` | Search (paginated) | 200 | -- |
| `GET` | `/api/v1/dealerships/{id}/availability` | Available slots | 200 | 400 |

All error responses follow **RFC 7807 Problem Detail**. `POST` requires an `Idempotency-Key` header (24h TTL).

### Error Mapping

```mermaid
graph LR
    A[Domain Result.Failure] --> B{Error Code}
    B -->|SlotNotAvailable| C[409 Conflict]
    B -->|NoBayAvailable| C
    B -->|NoTechnicianAvailable| C
    B -->|InvalidStateTransition| D[422 Unprocessable]
    B -->|NotFound| E[404 Not Found]

    F[Validation Error] --> G[400 Bad Request]
    H[Missing Idempotency-Key] --> I[422 Unprocessable]
    J[PessimisticLockingFailure] --> C

    style C fill:#e76f51,stroke:#c44536,color:#fff
    style D fill:#f4a261,stroke:#e76f51,color:#fff
    style E fill:#264653,stroke:#1d3557,color:#fff
    style G fill:#457b9d,stroke:#1d3557,color:#fff
    style I fill:#f4a261,stroke:#e76f51,color:#fff
```

## Testing

```bash
mvn test                                              # Full suite (requires Docker)
mvn test -Dtest="com.keyloop.scheduler.domain.**"     # Domain only (no Docker)
```

### Test Suite (98 tests)

| Category | Count | Scope |
|----------|-------|-------|
| Domain unit tests | 55 | Appointment aggregate, TimeSlot, AppointmentStatus state machine, Result\<T\>, entity hierarchy |
| Application handler tests | 18 | CQRS command/query handlers, validation pipeline (Mockito) |
| Infrastructure tests | 3 | CorrelationIdFilter behavior |
| Architecture tests | 9 | ArchUnit: 4 layer rules, 2 framework isolation, 3 naming conventions |
| Integration tests | 13 | Testcontainers + real PostgreSQL: full lifecycle, idempotency, search, concurrency |

### Test Pyramid

```mermaid
graph TB
    subgraph "Integration (13)"
        I1[Full API lifecycle]
        I2[Concurrency: 6 threads / 3 bays]
        I3[Idempotency + Search]
    end

    subgraph "Application (18)"
        A1[Command handlers]
        A2[Query handlers]
        A3[Validation pipeline]
    end

    subgraph "Domain (55)"
        D1[Appointment aggregate]
        D2[State transitions]
        D3[TimeSlot / Value objects]
        D4["Result&lt;T&gt; sealed types"]
    end

    subgraph "Architecture (9)"
        AR1[Layer dependency rules]
        AR2[Framework isolation]
        AR3[Naming conventions]
    end

    style I1 fill:#e76f51,stroke:#c44536,color:#fff
    style I2 fill:#e76f51,stroke:#c44536,color:#fff
    style I3 fill:#e76f51,stroke:#c44536,color:#fff
    style A1 fill:#f4a261,stroke:#e76f51,color:#000
    style A2 fill:#f4a261,stroke:#e76f51,color:#000
    style A3 fill:#f4a261,stroke:#e76f51,color:#000
    style D1 fill:#2a9d8f,stroke:#264653,color:#fff
    style D2 fill:#2a9d8f,stroke:#264653,color:#fff
    style D3 fill:#2a9d8f,stroke:#264653,color:#fff
    style D4 fill:#2a9d8f,stroke:#264653,color:#fff
    style AR1 fill:#264653,stroke:#1d3557,color:#fff
    style AR2 fill:#264653,stroke:#1d3557,color:#fff
    style AR3 fill:#264653,stroke:#1d3557,color:#fff
```

The concurrency integration test spawns **6 threads** competing for **3 service bays** simultaneously -- exactly 3 succeed (201) and 3 are rejected (409), validating the pessimistic locking strategy.

## Key Design Decisions

| Decision | Approach | Rationale |
|----------|----------|-----------|
| Double-booking prevention | `SELECT ... FOR UPDATE` on **service bay rows** | Prevents phantom reads -- bay rows always exist, so the lock serializes even when no prior appointments exist for a slot |
| CQRS | Command + Handler / Query + Handler | Single-responsibility handlers, clear read/write separation |
| Result\<T\> | Sealed `Success`/`Failure` types | Business-rule failures are values, not exceptions; boundary errors (404, 400) still use exceptions mapped to RFC 7807 |
| Strongly-typed IDs | `CustomerId`, `VehicleId`, `AppointmentId` | Compile-time safety; anti-corruption boundary at the handler layer |
| Clock injection | `java.time.Clock` via constructor | Deterministic tests with `Clock.fixed(...)`, no hidden `now()` calls |
| Idempotency | `Idempotency-Key` header + DB store | Safe retries for POST, 24h TTL with scheduled cleanup |
| Audit + soft delete | `AuditableEntity` / `SoftDeletableEntity` | Consistent `createdAt`, `updatedAt`, `deleted` across all entities |
| Correlation ID | `CorrelationIdFilter` + MDC | UUID propagated through all log lines for end-to-end request tracing |

## Observability

```mermaid
graph LR
    APP["Spring Boot App<br/>/actuator/prometheus"] -->|scrape every 15s| PROM["Prometheus<br/>:9090"]
    PROM --> GRAF["Grafana<br/>:3000"]
    APP -->|structured logs| LOG["Logback<br/>+ correlationId MDC"]

    style APP fill:#2a9d8f,stroke:#264653,color:#fff
    style PROM fill:#e76f51,stroke:#c44536,color:#fff
    style GRAF fill:#f4a261,stroke:#e76f51,color:#000
    style LOG fill:#264653,stroke:#1d3557,color:#fff
```

### Metrics Pipeline

The application exposes metrics via **Micrometer + Prometheus registry**, scraped every 15 seconds by Prometheus. A pre-configured Grafana dashboard auto-provisions on startup.

### Grafana Dashboard (15 panels)

| Section | Panels |
|---------|--------|
| HTTP Overview | Request Rate, Response Time p95, Total Requests, Error Rate (5xx), Avg Response Time, 409 Conflicts |
| JVM | Heap Memory, Threads (live/daemon/peak), GC Pause Count, GC Pause Duration |
| Database (HikariCP) | Connection Pool (active/idle/total/pending), Connection Acquire Time |
| Business Metrics | Appointment API Calls by Endpoint, HTTP Status Distribution, Latency percentiles (p50/p95/p99) |

### Actuator Endpoints

| Endpoint | Description |
|----------|-------------|
| `/actuator/health` | Application health (DB, disk, detailed) |
| `/actuator/prometheus` | Prometheus-format metrics |
| `/actuator/metrics` | JSON metrics catalog |
| `/actuator/flyway` | Migration status |
| `/actuator/scheduledtasks` | Scheduled jobs (idempotency cleanup) |

### Structured Logging

All log output includes `correlationId` in MDC for request tracing. Log levels: `com.keyloop.scheduler` at DEBUG, `org.hibernate.SQL` at DEBUG for query visibility. JSON-format logging can be enabled for production deployments.

## AI Collaboration Narrative

This project was built with AI assistance (Claude Code) under human direction and review, following a **design-first, test-first** workflow.

### Workflow

```mermaid
graph LR
    DESIGN["Phase 1: Design<br/>Human-led architecture<br/>AI-assisted documentation"] --> TICKET["Phase 2: Tickets<br/>T01-T13 dependency graph<br/>Domain-first build order"]
    TICKET --> TDD["Phase 3: TDD per ticket<br/>Write tests (red)<br/>Implement (green)<br/>Human review"]
    TDD --> VERIFY["Phase 4: Verification<br/>Integration tests<br/>Concurrency testing<br/>Bug discovery + fix"]

    style DESIGN fill:#264653,stroke:#1d3557,color:#fff
    style TICKET fill:#2a9d8f,stroke:#264653,color:#fff
    style TDD fill:#f4a261,stroke:#e76f51,color:#000
    style VERIFY fill:#e76f51,stroke:#c44536,color:#fff
```

### Strategy

- **Design phase**: Requirements analysis ([`docs/01-analysis.md`](./docs/01-analysis.md)), architecture decisions ([`docs/02-design.md`](./docs/02-design.md)), and tech stack selection ([`docs/03-tech-stack.md`](./docs/03-tech-stack.md)) were done collaboratively, with the human making all architectural decisions and resolving every ambiguity. See [`docs/04-genai-collaboration.md`](./docs/04-genai-collaboration.md) for the full design-phase narrative.
- **Implementation phase**: Strict ticket-driven TDD workflow (T01-T13). Each ticket: read acceptance criteria, write tests first (red), implement (green), human review, commit. Dependency order enforced by the ticket graph ([`docs/tickets/README.md`](./docs/tickets/README.md)).
- **AI role**: "Fast but fallible pair junior" -- delegated execution against an existing spec, never decisions on open business rules or architecture.

### Verification Process

1. **Every code change ships with a test**, run for real before reporting completion -- never assumed to pass.
2. **ArchUnit** (9 rules) enforces layer dependency rules and naming conventions automatically on every test run. Written early (T04), not deferred to the end.
3. **Integration tests** (Testcontainers) verify the full stack with real PostgreSQL, including a **6-thread concurrency test** for double-booking prevention (3 bays, 6 simultaneous requests -- exactly 3 succeed, 3 get 409).
4. **Human review** of all AI-generated code, especially: state transition logic, pessimistic locking strategy, error-code mapping, and business rules.

### Bugs Discovered Through Human Verification (T12)

Integration testing uncovered 3 bugs in AI-generated code -- demonstrating the "AI generates, human verifies and corrects" workflow:

1. **Specialization type mismatch**: `SpringDataTechnicianRepository.findBySpecialization(String)` should have accepted `Specialization` enum -- caused every appointment creation to fail with 500. Caught during integration test execution.
2. **Phantom read in double-booking**: Original implementation locked appointment rows (`SELECT ... FOR UPDATE` on appointments table), but when no appointments exist for a slot, there's nothing to lock. **Human directed the fix**: lock service bay rows instead (they always exist), serializing concurrent access at the correct level.
3. **Error code mapping gap**: `"NoBayAvailable"` didn't match the substring check for `"NotAvailable"`, returning 400 instead of the expected 409. Identified during integration test review.

These examples illustrate why human verification is essential -- AI produces fast but fallible output; the subtle bugs that require domain understanding are caught through testing and review.

### Commit Convention

```
feat(domain): add Appointment aggregate with create() factory

AI-generated: aggregate skeleton + happy-path test from ticket T06.
Human-reviewed: added invariant "endTime must be after startTime".
```

## Project Structure

```
docs/
  01-analysis.md              # Requirements analysis (FR1-FR8, business rules, assumptions)
  02-design.md                # System design document (architecture, patterns, deep dives)
  03-tech-stack.md            # Technology choices with justifications
  04-genai-collaboration.md   # AI collaboration narrative (design phase)
  tickets/                    # T01-T13 implementation tickets with dependency graph

src/main/java/com/keyloop/scheduler/
  api/                        # Controllers, exception handler, request DTOs
  application/                # CQRS handlers, validation, DTOs, ports
  domain/                     # Pure Java: aggregates, value objects, events, repositories
  infrastructure/             # JPA entities, repository adapters, filters, config

src/main/resources/
  application.yml             # Spring configuration
  db/migration/               # Flyway V1-V6 (schema + seed data)

src/test/java/com/keyloop/scheduler/
  architecture/               # ArchUnit layer dependency tests (9)
  domain/                     # Pure unit tests (55)
  application/                # Handler + validation tests (18)
  infrastructure/             # Filter tests (3)
  integration/                # Testcontainers integration tests (13)

monitoring/
  prometheus/prometheus.yml   # Scrape configuration
  grafana/
    provisioning/             # Auto-provisioned datasource + dashboard provider
    dashboards/               # 15-panel Grafana dashboard JSON

docker-compose.yml            # PostgreSQL + Prometheus + Grafana
Dockerfile                    # Multi-stage build for production
```
