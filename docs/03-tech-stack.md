# 03. Technology Choices & Justifications

---

## Table of Contents

- [1. Technology Stack](#1-technology-stack)
- [2. Justifications](#2-justifications)
- [3. Observability Stack](#3-observability-stack)
- [4. Project Structure](#4-project-structure)
- [5. Scalability Path (Future)](#5-scalability-path-future)

---

## 1. Technology Stack

| Category | Technology | Version |
|----------|-----------|---------|
| **Language** | Java | 21 (LTS) |
| **Framework** | Spring Boot | 3.3.5 |
| **Database** | PostgreSQL | 16 |
| **ORM** | Hibernate / Spring Data JPA | 6.x |
| **Migration** | Flyway | 10.x |
| **Build** | Maven | 3.9+ |
| **Testing** | JUnit 5 + AssertJ + Mockito | latest |
| **Integration Test** | Testcontainers (PostgreSQL) | latest |
| **Architecture Test** | ArchUnit | 1.3.0 |
| **Load Test** | Gatling (Java DSL) | 3.10.5 |
| **API Docs** | SpringDoc OpenAPI (Swagger UI) | 2.x |
| **Observability** | Micrometer + Spring Boot Actuator | auto |
| **Logging** | SLF4J + Logback (JSON) | auto |
| **Containerization** | Docker + Docker Compose | latest |

---

## 2. Justifications

### Java 21 + Spring Boot 3.3

- **Virtual Threads** (Project Loom) — stable in Java 21. Handles concurrent booking requests more efficiently than platform threads. Config: `spring.threads.virtual.enabled=true`
- **Record classes** — immutable DTOs, commands, value objects, domain events
- **Pattern matching** — cleaner domain logic (e.g., `Result<T>` sealed interface matching)
- Spring Boot 3.3.5 = latest stable, Hibernate 6, native Spring Data JPA support

### PostgreSQL (not H2 or MongoDB)

- **ACID transactions** — critical for booking consistency (pessimistic locking for double-booking prevention)
- **Row-level locking** (`SELECT ... FOR UPDATE`) — solves the double-booking problem deterministically
- **Mature, production-proven** — aligns with the "Build For the Future" requirement
- H2 (in-memory) does not support `FOR UPDATE` correctly — produces wrong behavior in concurrency tests

### Flyway (not Liquibase)

- Simpler: SQL-based migrations — easy to review, easy to understand
- Version-controlled schema evolution — reproducible database state
- Matches the assignment requirement for a "persistent database"
- 5 migrations (V1–V5): schema, seed data, appointment statuses, idempotency keys, audit/soft-delete columns

### Testcontainers

- Tests run against **real PostgreSQL** (Docker container) — not H2 fakes
- Concurrency tests (double-booking prevention) are only meaningful on a real database
- Integration tests are significantly more trustworthy

### ArchUnit

- Enforces Clean Architecture layer dependency rules at compile/test time
- Prevents accidental coupling (e.g., domain importing Spring annotations)
- 149 tests verify architectural constraints automatically

### SpringDoc OpenAPI

- Auto-generates Swagger UI — serves as the client mock layer (assignment: "mock or stub the client-side layer")
- Interactive API explorer = built-in test harness
- cURL examples generated automatically

---

## 3. Observability Stack

| Layer | Tool | Purpose |
|-------|------|---------|
| **Logging** | SLF4J + Logback | Structured JSON logs, correlation ID via MDC |
| **Correlation** | `CorrelationIdFilter` | UUID per request (`X-Correlation-ID`), propagated via MDC |
| **Idempotency** | `IdempotencyFilter` | `Idempotency-Key` header → cached response; 24h TTL with scheduled cleanup |
| **Metrics** | Micrometer + Actuator | `appointments.created`, `booking.conflicts`, API latency |
| **Health** | Spring Boot Actuator | `/actuator/health` — DB connectivity check |
| **Errors** | `GlobalExceptionHandler` | RFC 7807 ProblemDetail responses for all error types |

> Full distributed tracing (Jaeger/Zipkin) is out of scope but noted as a scalability path.

---

## 4. Project Structure

```
unified-scheduler-keyloop-oa/
├── docs/                              # System Design Documents
│   ├── 01-analysis.md                 # Requirements Analysis
│   ├── 02-design.md                   # HLD + Deep Dives
│   ├── 03-tech-stack.md               # Technology Choices (this file)
│   └── 04-genai-collaboration.md      # AI Collaboration Narrative
│
├── src/main/java/com/keyloop/scheduler/
│   ├── api/                           # API Layer (Controllers)
│   │   ├── controller/                # AppointmentController, AvailabilityController
│   │   ├── exception/                 # GlobalExceptionHandler (RFC 7807)
│   │   └── request/                   # CreateAppointmentRequest
│   │
│   ├── application/                   # Application Layer (CQRS)
│   │   ├── appointment/
│   │   │   ├── command/               # 5 commands + handlers
│   │   │   └── query/                 # SearchAppointments, GetById, AvailableSlots
│   │   ├── cqrs/                      # Command<R>, Query<R>, Handler interfaces
│   │   ├── dto/                       # AppointmentDto, AvailableSlotDto, PagedResult<T>
│   │   ├── event/                     # Domain event handlers
│   │   ├── port/                      # EventPublisher interface
│   │   └── validation/                # CommandValidator<T>, ValidatingService
│   │
│   ├── domain/                        # Domain Layer (package-by-aggregate, pure Java)
│   │   ├── appointment/               # Appointment aggregate root, status, errors, repository
│   │   │   └── event/                 # Domain events: Created, Cancelled, CheckedIn, Completed, NoShow
│   │   ├── servicetype/               # ServiceType entity, Specialization enum, repository
│   │   ├── customer/                  # Customer, Vehicle, typed IDs
│   │   ├── dealership/                # Dealership, ServiceBay, Technician, repositories
│   │   └── shared/                    # Entity hierarchy, Result<T>, Error, DomainEvent
│   │
│   └── infrastructure/                # Infrastructure Layer
│       ├── config/                    # ClockConfig, JpaAuditConfig
│       ├── event/                     # DomainEventPublisher
│       ├── filter/                    # CorrelationIdFilter, IdempotencyFilter
│       ├── job/                       # IdempotencyCleanupJob
│       └── persistence/
│           ├── converter/             # Strongly-typed ID ↔ Long converters
│           ├── entity/                # JPA entities (separate from domain)
│           └── repository/            # Spring Data repos + adapters
│
├── src/main/resources/
│   ├── application.yml
│   └── db/migration/                  # Flyway migrations
│       ├── V1__create_schema.sql
│       ├── V2__seed_data.sql
│       ├── V3__add_appointment_statuses.sql
│       ├── V4__create_idempotency_keys.sql
│       └── V5__add_audit_soft_delete_columns.sql
│
├── src/test/java/com/keyloop/scheduler/
│   ├── architecture/                  # ArchUnit tests
│   ├── domain/                        # Pure unit tests
│   ├── application/                   # Handler + validation tests
│   ├── infrastructure/                # Filter tests
│   ├── integration/                   # Testcontainers + full API tests
│   └── loadtest/                      # Gatling load simulations
│
├── docker-compose.yml                 # PostgreSQL + App
├── Dockerfile
├── pom.xml
└── README.md
```

---

## 5. Scalability Path (Future)

> Not implemented, but documented to demonstrate "Build For the Future" mindset.

| Current (Assessment) | Future (Production) |
|---------------------|-------------------|
| Single PostgreSQL | Read replicas + connection pooling (PgBouncer) |
| In-process locking (`FOR UPDATE`) | Distributed lock (Redis/Redisson) |
| Sync availability check | Pre-computed availability grid (Redis cache) |
| Monolith | Modular monolith → microservices if needed |
| Correlation ID (MDC) | Distributed tracing (OpenTelemetry → Jaeger) |
| JSON logs to stdout | ELK stack / Grafana Loki |
| Actuator metrics | Prometheus + Grafana dashboards |
| In-memory idempotency (DB) | Redis-backed idempotency store |

---

> **Navigation:** next → [`04-genai-collaboration.md`](./04-genai-collaboration.md) — AI Collaboration Narrative (assignment requirement).
