# Unified Service Scheduler -- Keyloop Technical Assessment

## Scenario

**Scenario A: The Unified Service Scheduler (Domain: Ownership)**

A scheduling system for automotive dealerships that manages service appointments, technician assignments, service bay allocation, and availability queries. The system prevents double-booking of service bays and technicians, enforces appointment lifecycle state transitions (SCHEDULED -> CHECKED_IN -> IN_PROGRESS -> COMPLETED, with CANCELLED and NO_SHOW paths), and supports priority-based scheduling.

## Architecture

Clean Architecture with 4 layers:

```
api/             --> Controllers, exception handling, request DTOs
application/     --> CQRS command/query handlers, validation, DTOs
domain/          --> Pure Java aggregates, value objects, repository interfaces
infrastructure/  --> JPA adapters, filters, config, event publishing
```

**Dependency rule**: `domain` has zero framework dependencies. `application` depends only on `domain`. `infrastructure` implements domain ports. `api` orchestrates everything.

See [`docs/02-design.md`](./docs/02-design.md) for full architecture diagrams, sequence diagrams, concurrency deep-dives, and the availability algorithm.

## Tech Stack

- **Java 21** with Spring Boot 3.3.5
- **PostgreSQL 16** with Flyway migrations
- **Maven** build system
- **Testing**: JUnit 5 + AssertJ + Mockito (unit), Testcontainers (integration), ArchUnit (architecture)
- **API docs**: SpringDoc OpenAPI / Swagger UI
- **Observability**: Micrometer + Actuator, SLF4J/Logback with correlationId MDC

See [`docs/03-tech-stack.md`](./docs/03-tech-stack.md) for technology choices and justifications.

## Prerequisites

- Java 21 (Eclipse Temurin recommended)
- Docker + Docker Compose
- Maven 3.9+ (or use the included Maven wrapper `./mvnw`)

## Quick Start

### Option 1: Docker Compose (recommended)

```bash
docker compose up --build
```

- App: http://localhost:8080
- Swagger UI: http://localhost:8080/swagger-ui.html
- Health check: http://localhost:8080/actuator/health

### Option 2: Local Development

```bash
docker compose up -d postgres   # Start PostgreSQL only
./mvnw spring-boot:run          # Start the app locally
```

## Running Tests

```bash
# Full test suite (requires Docker for Testcontainers)
./mvnw test

# Domain unit tests only (no Docker needed)
./mvnw test -Dtest="com.keyloop.scheduler.domain.**"
```

The test suite includes:
- **Domain unit tests** -- pure Java, no Spring context
- **Application handler tests** -- Mockito-based, testing CQRS handlers and validation
- **Infrastructure tests** -- filter behavior (CorrelationId, Idempotency)
- **Architecture tests** -- ArchUnit enforcing layer dependency rules
- **Integration tests** -- Testcontainers with full Spring context, including concurrency tests for double-booking prevention

## API Endpoints

| Method | Endpoint | Description |
|--------|----------|-------------|
| POST   | /api/v1/appointments | Create appointment (requires `Idempotency-Key` header) |
| GET    | /api/v1/appointments/{id} | Get appointment by ID |
| DELETE | /api/v1/appointments/{id} | Cancel appointment |
| PATCH  | /api/v1/appointments/{id}/check-in | Check in customer |
| PATCH  | /api/v1/appointments/{id}/complete | Complete appointment |
| PATCH  | /api/v1/appointments/{id}/no-show | Mark as no-show |
| GET    | /api/v1/appointments | Search appointments (paginated, filterable) |
| GET    | /api/v1/dealerships/{id}/availability | Query available slots (paginated) |

All error responses follow **RFC 7807 Problem Detail** format. The `POST` endpoint requires an `Idempotency-Key` header for safe retries (24-hour TTL).

## Key Design Decisions

- **Double-booking prevention**: Uses `SELECT ... FOR UPDATE` pessimistic locking on service bays and technicians during appointment creation (see FR2/BR2).
- **CQRS**: Every use case is modeled as either a Command (write) or Query (read) with dedicated handlers.
- **Result\<T\> for domain outcomes**: Business-rule failures (e.g., invalid state transitions, slot conflicts) return `Result.Failure` instead of throwing exceptions. Application-boundary errors (not found, invalid input) use exceptions mapped to RFC 7807.
- **Strongly-typed IDs**: Domain methods never accept raw `Long`/`UUID`; handlers convert at the boundary.
- **Clock injection**: All time-dependent logic accepts `java.time.Clock` for deterministic testing.

## AI Collaboration Narrative

This project was built with AI assistance (Claude) under human direction and review, following a **design-first, test-first** workflow.

### Strategy

- **Design phase**: Requirements analysis, architecture decisions, and tech stack selection were done collaboratively, with the human making all architectural decisions. See [`docs/04-genai-collaboration.md`](./docs/04-genai-collaboration.md) for the full design-phase narrative.
- **Implementation phase**: Followed a strict ticket-driven, TDD workflow (T01-T13). For each ticket, tests were written first (red), then implementation (green), with human review at each step.
- **AI role**: "Fast but fallible pair junior" -- delegated execution against an existing spec, never decisions on open business rules or architecture.

### Verification Process

- Every code change ships with a test, run for real before reporting completion.
- ArchUnit enforces layer dependency rules automatically on every test run.
- Integration tests use Testcontainers for realistic PostgreSQL testing, including concurrency scenarios.
- The human reviewed all AI-generated code, especially business rules (state transitions, double-booking, priority scheduling).

### Concrete Examples

Implementation commit examples with AI-generated vs. human-reviewed attribution are recorded in the git history. Each commit message follows the format:

```
feat(domain): add Appointment aggregate with create() factory

AI-generated: aggregate skeleton + happy-path test from ticket T06.
Human-reviewed: added invariant "endTime must be after startTime".
```

## Project Structure

```
docs/
  01-analysis.md          # Requirements analysis (FR1-FR8, business rules, assumptions)
  02-design.md            # Architecture, patterns, sequence diagrams, deep dives
  03-tech-stack.md        # Technology choices with justifications
  04-genai-collaboration.md  # AI collaboration narrative (design phase)
  tickets/                # T01-T13 implementation tickets with dependency graph

src/main/java/com/keyloop/scheduler/
  api/                    # Controllers, exception handler, request DTOs
  application/            # CQRS handlers, validation, DTOs, ports
  domain/                 # Pure Java: aggregates, value objects, events, repositories
  infrastructure/         # JPA entities, repository adapters, filters, config

src/main/resources/
  application.yml         # Spring configuration
  db/migration/           # Flyway V1-V5 migrations

src/test/java/com/keyloop/scheduler/
  architecture/           # ArchUnit layer dependency tests
  domain/                 # Pure unit tests
  application/            # Handler + validation tests (Mockito)
  infrastructure/         # Filter tests
  integration/            # Testcontainers integration tests
```
